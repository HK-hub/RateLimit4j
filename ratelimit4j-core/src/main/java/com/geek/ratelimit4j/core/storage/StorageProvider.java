package com.geek.ratelimit4j.core.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 分布式存储提供者接口
 * 支持Redis、Hazelcast等扩展，保证限流操作的原子性
 *
 * <p>设计说明：</p>
 * <ul>
 *   <li>接口抽象存储操作，便于扩展不同的分布式存储</li>
 *   <li>executeAtomic方法是核心，用于执行Lua脚本等原子操作</li>
 *   <li>Redis实现使用Redisson，支持Lua脚本执行</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * StorageProvider provider = new RedisStorageProvider(redissonClient);
 * Long result = provider.executeAtomic(luaScript, keys, args);
 * }</pre>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public interface StorageProvider {

    /**
     * 获取存储类型
     *
     * @return 存储类型枚举值
     */
    StorageType getStorageType();

    /**
     * 执行原子操作（如Lua脚本）
     * 所有限流计算必须通过此方法保证原子性
     *
     * <p>参数说明：</p>
     * <ul>
     *   <li>script：Lua脚本内容或脚本SHA标识</li>
     *   <li>keys：涉及的Key列表，对应Lua脚本中的KEYS数组</li>
     *   <li>args：脚本参数列表，对应Lua脚本中的ARGV数组</li>
     * </ul>
     *
     * @param script 脚本内容或脚本标识
     * @param keys 涉及的Key列表
     * @param args 脚本参数列表
     * @return 执行结果，具体类型取决于脚本
     */
    <T> T executeAtomic(String script, List<String> keys, List<Object> args);

    /**
     * 存储数值并设置过期时间
     * 用于初始化限流计数器
     *
     * @param key 存储Key
     * @param value 存储值
     * @param ttlSeconds 过期时间（秒）
     */
    void store(String key, long value, long ttlSeconds);

    /**
     * 获取存储的数值
     *
     * @param key 存储Key
     * @return 存储的数值，不存在时返回null
     */
    Long get(String key);

    /**
     * 删除存储的Key
     * 用于重置限流器状态
     *
     * @param key 存储Key
     * @return true表示删除成功，false表示Key不存在
     */
    boolean delete(String key);

    /**
     * 批量删除Key
     * 用于清理限流数据
     *
     * @param pattern Key模式（如 "ratelimit:*"）
     * @return 删除的Key数量
     */
    long deleteByPattern(String pattern);

    /**
     * 检查存储是否可用
     * 用于熔断判断和降级决策
     *
     * @return true表示可用，false表示不可用
     */
    boolean isAvailable();

    /**
     * 获取存储的健康状态详情
     * 用于监控和诊断
     *
     * @return 健康状态信息
     */
    StorageHealth getHealth();

    /**
     * 增加计数器值（原子操作）
     *
     * @param key 存储Key
     * @param delta 增量值
     * @return 增加后的值
     */
    long increment(String key, long delta);

    /**
     * 增加计数器值并设置过期时间（原子操作）
     *
     * @param key 存储Key
     * @param delta 增量值
     * @param ttlSeconds 过期时间（秒）
     * @return 增加后的值
     */
    long increment(String key, long delta, long ttlSeconds);

    /**
     * 设置Key的过期时间
     *
     * @param key 存储Key
     * @param ttlSeconds 过期时间（秒）
     * @return true表示设置成功
     */
    boolean expire(String key, long ttlSeconds);

    /**
     * 获取Key的剩余过期时间
     *
     * @param key 存储Key
     * @return 剩余过期时间（秒），-1表示永不过期，-2表示不存在
     */
    long getTtl(String key);

    /**
     * 存储健康状态
     * 使用Lombok简化getter方法
     */
    @Getter
    class StorageHealth {
        private final boolean healthy;
        private final String message;
        private final long lastCheckTimestamp;
        private final Map<String, Object> details;

        public StorageHealth(boolean healthy, String message) {
            this.healthy = healthy;
            this.message = message;
            this.lastCheckTimestamp = System.currentTimeMillis();
            this.details = new java.util.HashMap<>();
        }

        public StorageHealth(boolean healthy, String message, Map<String, Object> details) {
            this.healthy = healthy;
            this.message = message;
            this.lastCheckTimestamp = System.currentTimeMillis();
            this.details = Objects.nonNull(details) ? details : new java.util.HashMap<>();
        }

        public Object getDetail(String key) {
            return this.details.get(key);
        }
    }
}