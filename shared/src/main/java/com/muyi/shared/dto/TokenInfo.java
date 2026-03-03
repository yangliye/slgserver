package com.muyi.shared.dto;

/**
 * Token 验证结果
 * <p>
 * Login 服务验证 token 后返回给 Gate 的玩家路由信息。
 *
 * @author muyi
 */
public class TokenInfo {

    private long uid;
    private String account;
    private int gameServerId;

    public TokenInfo() {
    }

    public TokenInfo(long uid, String account, int gameServerId) {
        this.uid = uid;
        this.account = account;
        this.gameServerId = gameServerId;
    }

    public long getUid() {
        return uid;
    }

    public String getAccount() {
        return account;
    }

    public int getGameServerId() {
        return gameServerId;
    }

    @Override
    public String toString() {
        return "TokenInfo{uid=" + uid
                + ", account='" + account + '\''
                + ", gameServerId=" + gameServerId + '}';
    }
}
