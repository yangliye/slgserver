package com.muyi.common.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Redis 连接管理器，支持多实例
 *
 * <pre>
 * // 注册实例
 * RedisManager.register("main", "127.0.0.1", 6379);
 * RedisManager.register("cache", "127.0.0.1", 6380, "password", 2);
 *
 * // 从地址字符串注册（host:port:password:db，无密码用空：host:port::db）
 * RedisManager.register("global", "127.0.0.1:6379");
 * RedisManager.register("game-2", "127.0.0.1:6380::3");
 *
 * // 获取实例操作
 * RedisManager.of("main").set("key", "value");
 * RedisManager.of("cache").get("key");
 *
 * // 默认实例（第一个注册的）
 * RedisManager.main().set("key", "value");
 * </pre>
 *
 * @author muyi
 */
public class RedisManager {
    
    private static final Logger log = LoggerFactory.getLogger(RedisManager.class);
    
    private static final ConcurrentHashMap<String, RedisManager> INSTANCES = new ConcurrentHashMap<>();
    private static volatile String defaultName;
    
    private final String name;
    private final JedisPool pool;
    
    private RedisManager(String name, JedisPool pool) {
        this.name = name;
        this.pool = pool;
    }
    
    // ==================== 实例管理 ====================
    
    /**
     * 注册 Redis 实例
     */
    public static RedisManager register(String name, String host, int port) {
        return register(name, host, port, null, 0);
    }
    
    /**
     * 注册 Redis 实例
     */
    public static RedisManager register(String name, String host, int port, String password, int database) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(64);
        config.setMaxIdle(16);
        config.setMinIdle(4);
        config.setMaxWait(Duration.ofSeconds(3));
        config.setTestOnBorrow(true);
        
        boolean hasPassword = password != null && !password.isEmpty();
        JedisPool pool = new JedisPool(config, host, port, 3000,
                hasPassword ? password : null, database);
        
        RedisManager instance = new RedisManager(name, pool);
        RedisManager old = INSTANCES.putIfAbsent(name, instance);
        if (old != null) {
            pool.close();
            log.warn("Redis instance '{}' already registered, skip", name);
            return old;
        }
        
        if (defaultName == null) {
            defaultName = name;
        }
        
        log.info("Redis instance '{}' registered: {}:{}/{}", name, host, port, database);
        return instance;
    }
    
    /**
     * 从地址字符串注册
     * <p>格式：host:port 或 host:port:password:db</p>
     * <p>无密码指定db：host:port::db</p>
     */
    public static RedisManager register(String name, String address) {
        String[] parts = address.split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 6379;
        String password = parts.length > 2 ? parts[2] : null;
        int database = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;
        return register(name, host, port, password, database);
    }
    
    /**
     * 获取指定名称的实例
     */
    public static RedisManager of(String name) {
        RedisManager instance = INSTANCES.get(name);
        if (instance == null) {
            throw new IllegalStateException("Redis instance not found: " + name);
        }
        return instance;
    }
    
    /**
     * 获取默认实例（第一个注册的）
     */
    public static RedisManager main() {
        if (defaultName == null) {
            throw new IllegalStateException("No Redis instance registered");
        }
        return of(defaultName);
    }
    
    /**
     * 是否已注册指定实例
     */
    public static boolean hasInstance(String name) {
        return INSTANCES.containsKey(name);
    }
    
    /**
     * 关闭所有实例
     */
    public static void shutdownAll() {
        INSTANCES.forEach((name, instance) -> {
            try {
                instance.pool.close();
                log.info("Redis instance '{}' closed", name);
            } catch (Exception e) {
                log.error("Error closing Redis instance '{}'", name, e);
            }
        });
        INSTANCES.clear();
        defaultName = null;
    }
    
    /**
     * 关闭指定实例
     */
    public static void shutdown(String name) {
        RedisManager instance = INSTANCES.remove(name);
        if (instance != null) {
            instance.pool.close();
            log.info("Redis instance '{}' closed", name);
            if (name.equals(defaultName)) {
                defaultName = INSTANCES.isEmpty() ? null : INSTANCES.keys().nextElement();
            }
        }
    }
    
    // ==================== 通用执行 ====================
    
    public <T> T execute(Function<Jedis, T> action) {
        try (Jedis jedis = pool.getResource()) {
            return action.apply(jedis);
        }
    }
    
    public void run(Consumer<Jedis> action) {
        try (Jedis jedis = pool.getResource()) {
            action.accept(jedis);
        }
    }
    
    public String getName() {
        return name;
    }
    
    // ==================== String ====================
    
    public String get(String key) {
        return execute(j -> j.get(key));
    }
    
    public String set(String key, String value) {
        return execute(j -> j.set(key, value));
    }
    
    public String setex(String key, long seconds, String value) {
        return execute(j -> j.setex(key, seconds, value));
    }
    
    public String setnx(String key, long seconds, String value) {
        return execute(j -> j.set(key, value,
                redis.clients.jedis.params.SetParams.setParams().nx().ex(seconds)));
    }
    
    public long incr(String key) {
        return execute(j -> j.incr(key));
    }
    
    public long incrBy(String key, long increment) {
        return execute(j -> j.incrBy(key, increment));
    }
    
    public long decr(String key) {
        return execute(j -> j.decr(key));
    }
    
    // ==================== Key ====================
    
    public boolean exists(String key) {
        return execute(j -> j.exists(key));
    }
    
    public long del(String... keys) {
        return execute(j -> j.del(keys));
    }
    
    public long expire(String key, long seconds) {
        return execute(j -> j.expire(key, seconds));
    }
    
    public long ttl(String key) {
        return execute(j -> j.ttl(key));
    }
    
    // ==================== Hash ====================
    
    public String hget(String key, String field) {
        return execute(j -> j.hget(key, field));
    }
    
    public long hset(String key, String field, String value) {
        return execute(j -> j.hset(key, field, value));
    }
    
    public long hset(String key, Map<String, String> hash) {
        return execute(j -> j.hset(key, hash));
    }
    
    public Map<String, String> hgetAll(String key) {
        return execute(j -> j.hgetAll(key));
    }
    
    public long hdel(String key, String... fields) {
        return execute(j -> j.hdel(key, fields));
    }
    
    public boolean hexists(String key, String field) {
        return execute(j -> j.hexists(key, field));
    }
    
    public long hincrBy(String key, String field, long increment) {
        return execute(j -> j.hincrBy(key, field, increment));
    }
    
    // ==================== List ====================
    
    public long lpush(String key, String... values) {
        return execute(j -> j.lpush(key, values));
    }
    
    public long rpush(String key, String... values) {
        return execute(j -> j.rpush(key, values));
    }
    
    public String lpop(String key) {
        return execute(j -> j.lpop(key));
    }
    
    public String rpop(String key) {
        return execute(j -> j.rpop(key));
    }
    
    public List<String> lrange(String key, long start, long stop) {
        return execute(j -> j.lrange(key, start, stop));
    }
    
    public long llen(String key) {
        return execute(j -> j.llen(key));
    }
    
    // ==================== Set ====================
    
    public long sadd(String key, String... members) {
        return execute(j -> j.sadd(key, members));
    }
    
    public Set<String> smembers(String key) {
        return execute(j -> j.smembers(key));
    }
    
    public boolean sismember(String key, String member) {
        return execute(j -> j.sismember(key, member));
    }
    
    public long srem(String key, String... members) {
        return execute(j -> j.srem(key, members));
    }
    
    public long scard(String key) {
        return execute(j -> j.scard(key));
    }
    
    // ==================== Sorted Set ====================
    
    public long zadd(String key, double score, String member) {
        return execute(j -> j.zadd(key, score, member));
    }
    
    public long zadd(String key, Map<String, Double> scoreMembers) {
        return execute(j -> j.zadd(key, scoreMembers));
    }
    
    public List<String> zrange(String key, long start, long stop) {
        return execute(j -> j.zrange(key, start, stop));
    }
    
    public List<String> zrevrange(String key, long start, long stop) {
        return execute(j -> j.zrevrange(key, start, stop));
    }
    
    public Double zscore(String key, String member) {
        return execute(j -> j.zscore(key, member));
    }
    
    public Long zrank(String key, String member) {
        return execute(j -> j.zrank(key, member));
    }
    
    public Long zrevrank(String key, String member) {
        return execute(j -> j.zrevrank(key, member));
    }
    
    public long zrem(String key, String... members) {
        return execute(j -> j.zrem(key, members));
    }
    
    public long zcard(String key) {
        return execute(j -> j.zcard(key));
    }
    
    // ==================== Pub/Sub ====================
    
    public long publish(String channel, String message) {
        return execute(j -> j.publish(channel, message));
    }
}
