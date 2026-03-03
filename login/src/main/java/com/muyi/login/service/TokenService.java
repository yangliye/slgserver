package com.muyi.login.service;

import com.muyi.common.redis.RedisManager;
import com.muyi.shared.dto.TokenInfo;
import com.muyi.shared.token.TokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Token 管理服务
 * <p>
 * 负责 token 生成，通过 Redis 存储供 Gate 直接验证。
 *
 * @author muyi
 */
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private RedisManager redis;
    private long tokenTtlSeconds = 300;

    public void setRedis(RedisManager redis) {
        this.redis = redis;
    }

    public void setTokenTtlSeconds(long ttlSeconds) {
        this.tokenTtlSeconds = ttlSeconds;
    }

    /**
     * 生成 token 并写入 Redis
     *
     * @return 生成的 token 字符串
     */
    public String generateToken(long uid, String account, int gameServerId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        TokenInfo info = new TokenInfo(uid, account, gameServerId);
        TokenStore.save(redis, token, info, tokenTtlSeconds);
        log.info("Token generated for player[{}], account={}, ttl={}s", uid, account, tokenTtlSeconds);
        return token;
    }

    /**
     * 移除 token（管理接口或强制踢人时使用）
     */
    public void removeToken(String token) {
        TokenStore.remove(redis, token);
    }
}
