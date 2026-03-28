package com.geek.ratelimit4j.core.config;

import lombok.Getter;

/**
 * 限流引擎类型枚举
 * 定义支持的限流引擎类型
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Getter
public enum EngineType {

    /**
     * 本地引擎
     * 基于JVM内存实现，适用于单机部署
     * 特点：低延迟、无网络开销、但不支持集群共享
     */
    LOCAL("local", "本地限流引擎"),

    /**
     * Redis引擎
     * 基于Redis实现，适用于分布式部署
     * 特点：支持集群共享、但有一定网络延迟
     */
    REDIS("redis", "Redis分布式限流引擎"),

    /**
     * 自动选择
     * 根据配置自动选择可用的引擎
     * 优先使用配置文件中指定的primary引擎
     */
    AUTO("auto", "自动选择引擎");

    /**
     * 引擎编码
     */
    private final String code;

    /**
     * 引擎描述
     */
    private final String description;

    /**
     * 构造函数
     *
     * @param code        引擎编码
     * @param description 引擎描述
     */
    EngineType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据编码获取引擎类型
     *
     * @param code 引擎编码
     * @return 引擎类型，未找到时返回AUTO
     */
    public static EngineType fromCode(String code) {
        // 遍历所有引擎类型
        for (EngineType type : values()) {
            // 匹配编码（忽略大小写）
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        // 未找到时返回AUTO
        return AUTO;
    }
}