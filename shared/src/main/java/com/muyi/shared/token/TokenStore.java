package com.muyi.shared.token;

import com.muyi.common.redis.RedisManager;
import com.muyi.common.util.codec.JsonUtils;
import com.muyi.shared.dto.TokenInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Token 存储（基于 Redis）
 * <p>
 * Login 写入、Gate 读取验证，无需 RPC 回调。
 * 使用全局 Redis（{@code RedisManager.of("global")}）。
 * <p>
 * Redis Key: {@code token:{tokenString}} → JSON(TokenInfo)，带 TTL 自动过期。
 */
public final class TokenStore {

    private static final Logger log = LoggerFactory.getLogger(TokenStore.class);

    private static final String KEY_PREFIX = "token:";
    private static final long DEFAULT_TTL_SECONDS = 300;

    private TokenStore() {
    }

    /**
     * 存储 token（Login 调用）
     *
     * @param redis      全局 Redis 实例
     * @param token      token 字符串
     * @param info       玩家路由信息
     * @param ttlSeconds 过期时间（秒），0 表示使用默认值
     */
    public static void save(RedisManager redis, String token, TokenInfo info, long ttlSeconds) {
        long ttl = ttlSeconds > 0 ? ttlSeconds : DEFAULT_TTL_SECONDS;
        String key = KEY_PREFIX + token;
        String json = JsonUtils.toJson(info);
        redis.setex(key, ttl, json);
        log.debug("Token saved: uid={}, ttl={}s", info.getUid(), ttl);
    }

    public static void save(RedisManager redis, String token, TokenInfo info) {
        save(redis, token, info, DEFAULT_TTL_SECONDS);
    }

    /**
     * 验证 token 并返回玩家信息（Gate 调用）
     * <p>
     * 验证成功后自动删除 token（一次性消费），防止重复登录。
     *
     * @param redis 全局 Redis 实例
     * @param token token 字符串
     * @return TokenInfo，token 无效或已过期返回 null
     */
    public static TokenInfo verifyAndConsume(RedisManager redis, String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        String key = KEY_PREFIX + token;
        String json = redis.get(key);
        if (json == null) {
            log.debug("Token not found or expired: {}", token);
            return null;
        }
        redis.del(key);
        try {
            return JsonUtils.parse(json, TokenInfo.class);
        } catch (Exception e) {
            log.error("Failed to parse token data: {}", token, e);
            return null;
        }
    }

    /**
     * 验证 token 但不消费（仅查询）
     */
    public static TokenInfo verify(RedisManager redis, String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        String key = KEY_PREFIX + token;
        String json = redis.get(key);
        if (json == null) {
            return null;
        }
        try {
            return JsonUtils.parse(json, TokenInfo.class);
        } catch (Exception e) {
            log.error("Failed to parse token data: {}", token, e);
            return null;
        }
    }

    /**
     * 删除 token（登出时调用）
     */
    public static void remove(RedisManager redis, String token) {
        redis.del(KEY_PREFIX + token);
    }
}
