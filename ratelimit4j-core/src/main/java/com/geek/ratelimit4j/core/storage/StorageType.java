package com.geek.ratelimit4j.core.storage;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 存储类型枚举
 * 定义分布式限流使用的外部存储类型
 *
 * <p>存储对比：</p>
 * <ul>
 *   <li>Redis：高性能、广泛使用、支持Lua脚本</li>
 *   <li>Hazelcast：内存网格、低延迟（预留扩展）</li>
 *   <li>Zookeeper：强一致性、适合协调场景（预留扩展）</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public enum StorageType {

    /**
     * Redis存储
     * 
     * <p>特点：</p>
     * <ul>
     *   <li>高性能单线程模型</li>
     *   <li>支持Lua脚本保证原子性</li>
     *   <li>广泛使用、生态完善</li>
     *   <li>支持集群和哨兵模式</li>
     * </ul>
     * 
     * <p>推荐使用Redisson客户端，提供丰富的分布式对象。</p>
     */
    REDIS("redis"),

    /**
     * Hazelcast存储（预留扩展）
     * 
     * <p>特点：</p>
     * <ul>
     *   <li>内存数据网格</li>
     *   <li>超低延迟</li>
     *   <li>分布式Map和AtomicLong</li>
     * </ul>
     */
    HAZELCAST("hazelcast"),

    /**
     * Zookeeper存储（预留扩展）
     * 
     * <p>特点：</p>
     * <ul>
     *   <li>强一致性保证</li>
     *   <li>适合协调场景</li>
     *   <li>性能相对较低</li>
     * </ul>
     */
    ZOOKEEPER("zookeeper");

    /**
     * 存储类型代码标识
     */
    private final String code;

    /**
     * 构造存储类型枚举
     * 
     * @param code 存储类型代码标识
     */
    StorageType(String code) {
        this.code = code;
    }

    /**
     * 获取存储类型代码标识
     * 
     * @return 存储类型代码字符串
     */
    public String getCode() {
        return this.code;
    }

    /**
     * 根据代码标识获取存储类型
     * 
     * @param code 存储类型代码标识
     * @return 对应的存储类型，如果未找到则返回null
     */
    public static StorageType fromCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }

        String normalizedCode = code.toLowerCase().trim();
        for (StorageType type : values()) {
            if (Objects.equals(type.code, normalizedCode)) {
                return type;
            }
        }

        return null;
    }

    /**
     * 判断给定代码是否为有效的存储类型
     * 
     * @param code 存储类型代码标识
     * @return true表示有效，false表示无效
     */
    public static boolean isValidCode(String code) {
        return Objects.nonNull(fromCode(code));
    }
}