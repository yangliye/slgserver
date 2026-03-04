package com.muyi.gate.tcp;

import com.muyi.common.redis.RedisManager;
import com.muyi.common.util.codec.Rc4Cipher;
import com.muyi.common.util.time.TimeUtils;
import com.muyi.gate.router.MessageRouter;
import com.muyi.gate.router.RouteResult;
import com.muyi.gate.session.Session;
import com.muyi.gate.session.SessionManager;
import com.muyi.proto.GamePacket;
import com.muyi.proto.MessageRegistry;
import com.muyi.proto.gate.GateAuthReq;
import com.muyi.proto.gate.GateAuthResp;
import com.muyi.proto.gate.GateHeartbeatResp;
import com.muyi.rpc.client.RpcProxyManager;
import com.muyi.shared.api.game.IGameService;
import com.muyi.shared.api.world.IWorldService;
import com.muyi.shared.dto.TokenInfo;
import com.muyi.shared.token.TokenStore;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Gate 客户端连接处理器
 * <p>
 * 状态机：CONNECTED → (GateAuthReq 成功) → AUTHENTICATED → 业务消息转发
 * <p>
 * 核心职责：
 * <ul>
 *   <li>未认证状态：仅接受 GateAuthReq (msgId=9001)，通过 Redis 验证 token</li>
 *   <li>已认证状态：根据 MessageRouter 路由消息到 Game/World/Gate 本地处理</li>
 *   <li>连接断开：通知 Game 清理 PlayerExecutor</li>
 * </ul>
 *
 * @author muyi
 */
public class GateChannelHandler extends SimpleChannelInboundHandler<GamePacket> {

    private static final Logger log = LoggerFactory.getLogger(GateChannelHandler.class);

    private static final int GATE_AUTH_REQ = com.muyi.proto.MsgId.GATE_AUTH_REQ_VALUE;
    private static final int GATE_AUTH_RESP = com.muyi.proto.MsgId.GATE_AUTH_RESP_VALUE;
    private static final int GATE_HEARTBEAT_REQ = com.muyi.proto.MsgId.GATE_HEARTBEAT_REQ_VALUE;
    private static final int GATE_HEARTBEAT_RESP = com.muyi.proto.MsgId.GATE_HEARTBEAT_RESP_VALUE;

    private final SessionManager sessionManager;
    private final RpcProxyManager rpcProxy;
    private final MessageRouter messageRouter;
    private final int gateServerId;
    private final int authTimeoutSeconds;
    private final RedisManager globalRedis;

    private Session session;
    private boolean authenticated;
    private long connectTime;
    private String authToken;

    public GateChannelHandler(SessionManager sessionManager,
                              RpcProxyManager rpcProxy,
                              MessageRouter messageRouter,
                              int gateServerId,
                              int authTimeoutSeconds,
                              RedisManager globalRedis) {
        this.sessionManager = sessionManager;
        this.rpcProxy = rpcProxy;
        this.messageRouter = messageRouter;
        this.gateServerId = gateServerId;
        this.authTimeoutSeconds = authTimeoutSeconds;
        this.globalRedis = globalRedis;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        String clientIp = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString();
        session = sessionManager.createSession(ctx.channel(), clientIp);
        authenticated = false;
        connectTime = TimeUtils.currentTimeMillis();
        log.debug("Client connected: {} from {}", session.getSessionId(), clientIp);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GamePacket packet) {
        if (!authenticated) {
            handleUnauthenticated(ctx, packet);
        } else {
            handleAuthenticated(ctx, packet);
        }
    }

    private void handleUnauthenticated(ChannelHandlerContext ctx, GamePacket packet) {
        if (packet.getMsgId() != GATE_AUTH_REQ) {
            log.warn("Session[{}] not authenticated, received msgId={}, closing",
                    session.getSessionId(), packet.getMsgId());
            ctx.close();
            return;
        }

        try {
            GateAuthReq req = GateAuthReq.parseFrom(packet.getPayload());
            TokenInfo tokenInfo = TokenStore.verifyAndConsume(globalRedis, req.getToken());

            if (tokenInfo == null) {
                sendAuthResp(ctx, false, 0, "token invalid or expired", null);
                ctx.close();
                return;
            }

            if (!session.authenticate(tokenInfo.getUid(), tokenInfo.getAccount())) {
                sendAuthResp(ctx, false, 0, "authenticate failed", null);
                ctx.close();
                return;
            }

            session.enterGame(tokenInfo.getGameServerId());
            sessionManager.bindPlayerId(session, tokenInfo.getUid());

            authenticated = true;
            authToken = req.getToken();

            // 生成加密密钥，先发明文响应（含 key），再启用加密
            byte[] masterKey = Rc4Cipher.generateKey();
            sendAuthResp(ctx, true, tokenInfo.getUid(), "ok", masterKey);

            ctx.channel().attr(CipherAttr.CIPHER).set(Rc4Cipher.forServer(masterKey));

            log.info("Player[{}] authenticated via session[{}], game={}, cipher=RC4",
                    tokenInfo.getUid(), session.getSessionId(),
                    tokenInfo.getGameServerId());

        } catch (Exception e) {
            log.error("Session[{}] auth error", session.getSessionId(), e);
            sendAuthResp(ctx, false, 0, "internal error", null);
            ctx.close();
        }
    }

    private void handleAuthenticated(ChannelHandlerContext ctx, GamePacket packet) {
        session.touch();
        int msgId = packet.getMsgId();

        if (msgId == GATE_HEARTBEAT_REQ) {
            handleHeartbeat(ctx, packet);
            return;
        }

        RouteResult route = messageRouter.route(session, msgId, null);
        if (!route.isSuccess()) {
            log.debug("Player[{}] route failed for msgId={}: {}",
                    session.getPlayerId(), msgId, route.getErrorMessage());
            return;
        }

        switch (route.getTarget()) {
            case GAME -> forwardToGame(packet);
            case WORLD -> forwardToWorld(packet);
            case GATE_LOCAL -> handleLocalMessage(ctx, packet);
            default -> log.warn("Player[{}] unsupported route target: {} for msgId={}",
                    session.getPlayerId(), route.getTarget(), msgId);
        }
    }

    private void forwardToGame(GamePacket packet) {
        try {
            IGameService game = rpcProxy.get(IGameService.class, session.getGameServerId());
            game.forwardMessage(
                    session.getPlayerId(), authToken, gateServerId,
                    packet.getMsgId(), packet.getMsgSeq(), packet.getPayload());
        } catch (Exception e) {
            log.error("Player[{}] forward to game failed, msgId={}",
                    session.getPlayerId(), packet.getMsgId(), e);
        }
    }

    private void forwardToWorld(GamePacket packet) {
        try {
            IWorldService world = rpcProxy.get(IWorldService.class);
            world.forwardMessage(
                    session.getPlayerId(), session.getGameServerId(), gateServerId,
                    packet.getMsgId(), packet.getMsgSeq(), packet.getPayload());
        } catch (Exception e) {
            log.error("Player[{}] forward to world failed, msgId={}",
                    session.getPlayerId(), packet.getMsgId(), e);
        }
    }

    private void handleLocalMessage(ChannelHandlerContext ctx, GamePacket packet) {
        switch (packet.getMsgId()) {
            case GATE_HEARTBEAT_REQ -> handleHeartbeat(ctx, packet);
            default -> log.debug("Player[{}] unhandled local msgId={}",
                    session.getPlayerId(), packet.getMsgId());
        }
    }

    private void handleHeartbeat(ChannelHandlerContext ctx, GamePacket packet) {
        GateHeartbeatResp resp = GateHeartbeatResp.newBuilder()
                .setServerTime(TimeUtils.currentTimeMillis())
                .build();
        GamePacket respPacket = new GamePacket(
                GATE_HEARTBEAT_RESP, packet.getMsgSeq(), MessageRegistry.toBytes(resp));
        ctx.writeAndFlush(respPacket);
    }

    private void sendAuthResp(ChannelHandlerContext ctx, boolean success, long uid,
                              String message, byte[] encryptKey) {
        GateAuthResp.Builder builder = GateAuthResp.newBuilder()
                .setSuccess(success)
                .setUid(uid)
                .setMessage(message);
        if (encryptKey != null) {
            builder.setEncryptKey(com.google.protobuf.ByteString.copyFrom(encryptKey));
        }
        GamePacket packet = new GamePacket(GATE_AUTH_RESP, 0, MessageRegistry.toBytes(builder.build()));
        ctx.writeAndFlush(packet);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (session == null) {
            return;
        }
        long playerId = session.getPlayerId();
        if (playerId > 0 && authenticated) {
            int gameServerId = session.getGameServerId();
            if (gameServerId > 0) {
                try {
                    IGameService game = rpcProxy.get(IGameService.class, gameServerId);
                    game.playerLogout(playerId);
                } catch (Exception e) {
                    log.error("Player[{}] notify game logout failed", playerId, e);
                }
            }
        }
        sessionManager.removeSession(session);
        log.debug("Session[{}] closed, player={}", session.getSessionId(), session.getPlayerId());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent idleEvent) {
            if (idleEvent.state() == IdleState.READER_IDLE) {
                if (!authenticated) {
                    long elapsed = TimeUtils.currentTimeMillis() - connectTime;
                    if (elapsed > authTimeoutSeconds * 1000L) {
                        log.info("Session[{}] auth timeout ({}ms), closing",
                                session.getSessionId(), elapsed);
                        ctx.close();
                    }
                } else {
                    log.info("Player[{}] idle timeout, closing", session.getPlayerId());
                    ctx.close();
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Session[{}] exception", session != null ? session.getSessionId() : "null", cause);
        ctx.close();
    }
}
