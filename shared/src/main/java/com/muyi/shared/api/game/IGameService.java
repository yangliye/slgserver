package com.muyi.shared.api.game;

/**
 * Game 模块 RPC 服务接口
 * <p>
 * 供 Gate 等模块通过 RPC 调用，管理玩家在 Game 服务上的生命周期。
 *
 * @author muyi
 */
public interface IGameService {

    /**
     * 玩家登录通知（gate 认证成功后调用）
     * <p>
     * Game 收到后创建 PlayerExecutor，绑定玩家会话。
     *
     * @param uid          玩家 ID
     * @param authToken    认证凭据（后续消息校验用）
     * @param gateServerId 玩家连接的 gate 服务器 ID
     * @return 是否成功
     */
    boolean playerLogin(long uid, String authToken, int gateServerId);

    /**
     * 玩家登出通知（gate 断线后调用）
     * <p>
     * Game 收到后解绑 PlayerExecutor，清理玩家状态。
     *
     * @param uid 玩家 ID
     * @return 是否成功
     */
    boolean playerLogout(long uid);

    /**
     * 玩家重连通知（gate 侧重连成功后调用）
     * <p>
     * 更新 PlayerExecutor 的 gate 路由信息和认证凭据。
     *
     * @param uid            玩家 ID
     * @param newAuthToken   新认证凭据
     * @param newGateServerId 新 gate 服务器 ID（可能换了 gate）
     * @return 是否成功
     */
    boolean playerReconnect(long uid, String newAuthToken, int newGateServerId);

    /**
     * 检查玩家是否在线
     *
     * @param uid 玩家 ID
     * @return 是否在线
     */
    boolean isOnline(long uid);

    /**
     * 获取在线玩家数
     */
    int getOnlineCount();

    /**
     * 转发客户端消息（Gate → Game）
     * <p>
     * Gate 不解析 protobuf body，直接转发原始字节给 Game。
     * Game 收到后如果该 uid 尚无 PlayerExecutor，则懒创建并绑定 token。
     *
     * @param uid          玩家 ID
     * @param authToken    认证凭据（首次消息时用于创建 PlayerExecutor）
     * @param gateServerId 玩家连接的 gate 服务器 ID
     * @param msgId        消息协议 ID
     * @param msgSeq       消息序号
     * @param payload      protobuf 序列化字节
     */
    void forwardMessage(long uid, String authToken, int gateServerId, int msgId, int msgSeq, byte[] payload);
}
