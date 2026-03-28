package com.geek.ratelimit4j.core.config;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 限流模式类型枚举
 * 定义限流器的运行模式，用于配置限流策略的执行方式
 *
 * <p>模式对比：</p>
 * <ul>
 *   <li>本地模式：低延迟、高吞吐，适合单机应用</li>
 *   <li>分布式模式：集群全局限流，适合分布式系统</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public enum ModeType {

    /**
     * 本地限流模式 (Local)
     * 
     * <p>特点：</p>
     * <ul>
     *   <li>使用本地内存进行限流计算</li>
     *   <li>不依赖外部存储</li>
     *   <li>低延迟（通常小于1ms）</li>
     *   <li>高吞吐（可达百万QPS）</li>
     * </ul>
     * 
     * <p>适用场景：</p>
     * <ul>
     *   <li>单机应用</li>
     *   <li>分布式环境的兜底方案</li>
     *   <li>不需要全局限流的场景</li>
     * </ul>
     */
    LOCAL("local"),

    /**
     * 分布式限流模式 (Distributed)
     * 
     * <p>特点：</p>
     * <ul>
     *   <li>使用外部存储（如Redis）进行限流计算</li>
     *   <li>保证集群全局一致性</li>
     *   <li>存在网络延迟（通常小于10ms）</li>
     *   <li>吞吐受限于外部存储性能</li>
     * </ul>
     * 
     * <p>适用场景：</p>
     * <ul>
     *   <li>分布式集群应用</li>
     *   <li>需要全局限流的场景</li>
     *   <li>多服务共享限流配额</li>
     * </ul>
     */
    DISTRIBUTED("distributed");

    /**
     * 模式代码标识
     */
    private final String code;

    /**
     * 构造模式类型枚举
     * 
     * @param code 模式代码标识
     */
    ModeType(String code) {
        this.code = code;
    }

    /**
     * 获取模式代码标识
     * 
     * @return 模式代码字符串
     */
    public String getCode() {
        return this.code;
    }

    /**
     * 根据代码标识获取模式类型
     * 
     * @param code 模式代码标识（如 "local" 或 "distributed"）
     * @return 对应的模式类型，如果未找到则返回null
     */
    public static ModeType fromCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }

        String normalizedCode = code.toLowerCase().trim();
        for (ModeType type : values()) {
            if (Objects.equals(type.code, normalizedCode)) {
                return type;
            }
        }

        try {
            return valueOf(code.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 判断给定代码是否为有效的模式类型
     * 
     * @param code 模式代码标识
     * @return true表示有效，false表示无效
     */
    public static boolean isValidCode(String code) {
        return Objects.nonNull(fromCode(code));
    }
}