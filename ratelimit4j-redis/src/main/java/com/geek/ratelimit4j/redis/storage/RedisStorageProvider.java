package com.geek.ratelimit4j.redis.storage;

import com.geek.ratelimit4j.core.storage.StorageProvider;
import com.geek.ratelimit4j.core.storage.StorageType;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redisson的Redis存储提供者实现
 * 使用Redisson客户端实现分布式存储操作
 *
 * <p>特点：</p>
 * <ul>
 *   <li>支持Lua脚本执行，保证原子性</li>
 *   <li>自动重连机制</li>
 *   <li>支持集群和哨兵模式</li>
 *   <li>连接池管理</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class RedisStorageProvider implements StorageProvider {

    /**
     * Redisson客户端
     */
    private final RedissonClient redissonClient;

    /**
     * Key前缀
     */
    private final String keyPrefix;

    /**
     * 构造Redis存储提供者
     *
     * @param redissonClient Redisson客户端
     */
    public RedisStorageProvider(RedissonClient redissonClient) {
        this(redissonClient, "ratelimit4j:");
    }

    /**
     * 构造Redis存储提供者（带Key前缀）
     *
     * @param redissonClient Redisson客户端
     * @param keyPrefix Key前缀
     */
    public RedisStorageProvider(RedissonClient redissonClient, String keyPrefix) {
        if (Objects.isNull(redissonClient)) {
            throw new IllegalArgumentException("RedissonClient cannot be null");
        }
        this.redissonClient = redissonClient;
        this.keyPrefix = Objects.nonNull(keyPrefix) ? keyPrefix : "ratelimit4j:";
    }

    /**
     * 获取存储类型
     */
    @Override
    public StorageType getStorageType() {
        return StorageType.REDIS;
    }

    /**
     * 执行原子操作（Lua脚本）
     *
     * @param script Lua脚本内容
     * @param keys 涉及的Key列表
     * @param args 脚本参数列表
     * @return 执行结果
     */
    @Override
    public <T> T executeAtomic(String script, List<String> keys, List<Object> args) {
        RScript rScript = redissonClient.getScript();

        List<Object> prefixedKeys = keys.stream()
                .map(this::buildKey)
                .map(k -> (Object) k)
                .toList();

        return rScript.eval(RScript.Mode.READ_WRITE, script, RScript.ReturnType.VALUE, prefixedKeys, args.toArray());
    }

    /**
     * 存储数值并设置过期时间
     *
     * @param key 存储Key
     * @param value 存储值
     * @param ttlSeconds 过期时间（秒）
     */
    @Override
    public void store(String key, long value, long ttlSeconds) {
        RBucket<Long> bucket = redissonClient.getBucket(buildKey(key));
        bucket.set(value, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 获取存储的数值
     *
     * @param key 存储Key
     * @return 存储的数值，不存在时返回null
     */
    @Override
    public Long get(String key) {
        RBucket<Long> bucket = redissonClient.getBucket(buildKey(key));
        return bucket.get();
    }

    /**
     * 删除存储的Key
     *
     * @param key 存储Key
     * @return true表示删除成功，false表示Key不存在
     */
    @Override
    public boolean delete(String key) {
        RBucket<?> bucket = redissonClient.getBucket(buildKey(key));
        return bucket.delete();
    }

    /**
     * 批量删除Key
     *
     * @param pattern Key模式
     * @return 删除的Key数量
     */
    @Override
    public long deleteByPattern(String pattern) {
        RKeys rKeys = redissonClient.getKeys();
        Iterable<String> keys = rKeys.getKeysByPattern(buildKey(pattern));
        long count = 0;
        for (String k : keys) {
            if (redissonClient.getBucket(k).delete()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 检查存储是否可用
     *
     * @return true表示可用
     */
    @Override
    public boolean isAvailable() {
        try {
            redissonClient.getNodesGroup().pingAll();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取存储健康状态
     *
     * @return 健康状态
     */
    @Override
    public StorageHealth getHealth() {
        try {
            long startTime = System.currentTimeMillis();
            boolean pingResult = redissonClient.getNodesGroup().pingAll();
            long latency = System.currentTimeMillis() - startTime;

            if (pingResult) {
                return new StorageHealth(true, "Redis is healthy", 
                        java.util.Map.of("latencyMs", latency));
            } else {
                return new StorageHealth(false, "Redis ping failed");
            }
        } catch (Exception e) {
            return new StorageHealth(false, "Redis health check failed: " + e.getMessage());
        }
    }

    /**
     * 增加计数器值
     *
     * @param key 存储Key
     * @param delta 增量值
     * @return 增加后的值
     */
    @Override
    public long increment(String key, long delta) {
        RBucket<Long> bucket = redissonClient.getBucket(buildKey(key));
        Long value = bucket.get();
        long newValue = (Objects.isNull(value) ? 0 : value) + delta;
        bucket.set(newValue);
        return newValue;
    }

    /**
     * 增加计数器值并设置过期时间
     *
     * @param key 存储Key
     * @param delta 增量值
     * @param ttlSeconds 过期时间（秒）
     * @return 增加后的值
     */
    @Override
    public long increment(String key, long delta, long ttlSeconds) {
        RBucket<Long> bucket = redissonClient.getBucket(buildKey(key));
        Long value = bucket.get();
        long newValue = (Objects.isNull(value) ? 0 : value) + delta;
        bucket.set(newValue, ttlSeconds, TimeUnit.SECONDS);
        return newValue;
    }

    /**
     * 设置Key过期时间
     *
     * @param key 存储Key
     * @param ttlSeconds 过期时间（秒）
     * @return true表示设置成功
     */
    @Override
    public boolean expire(String key, long ttlSeconds) {
        RBucket<?> bucket = redissonClient.getBucket(buildKey(key));
        return bucket.expire(ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 获取Key剩余过期时间
     *
     * @param key 存储Key
     * @return 剩余过期时间（秒），-1表示永不过期，-2表示不存在
     */
    @Override
    public long getTtl(String key) {
        RBucket<?> bucket = redissonClient.getBucket(buildKey(key));
        long remainTime = bucket.remainTimeToLive();
        return remainTime > 0 ? remainTime / 1000 : remainTime;
    }

    /**
     * 构建完整的Key（前缀 + 原始Key）
     *
     * @param key 原始Key
     * @return 完整Key
     */
    private String buildKey(String key) {
        return keyPrefix + key;
    }
}