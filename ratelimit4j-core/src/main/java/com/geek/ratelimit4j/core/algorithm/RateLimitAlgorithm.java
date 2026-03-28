package com.geek.ratelimit4j.core.algorithm;

import com.geek.ratelimit4j.core.config.RateLimitContext;

/**
 * 限流算法接口
 * 定义算法的核心执行方法，所有限流算法实现必须遵循此接口
 *
 * <p>设计说明：</p>
 * <ul>
 *   <li>算法接口与RateLimiter接口分离，便于算法复用和组合</li>
 *   <li>算法实现关注限流计算逻辑</li>
 *   <li>RateLimiter关注配置管理和状态维护</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public interface RateLimitAlgorithm {

    /**
     * 执行限流判断（单许可）
     * 核心方法，根据算法逻辑判断是否允许请求通过
     *
     * @param context 限流上下文，包含限流Key、配置、请求信息等
     * @return 限流结果，包含是否允许、等待时间等信息
     */
    RateLimitResult evaluate(RateLimitContext context);

    /**
     * 执行带许可数量的限流判断
     * 用于需要获取多个许可的场景
     *
     * @param context 限流上下文
     * @param permits 需要获取的许可数量，必须大于0
     * @return 限流结果
     * @throws IllegalArgumentException 当permits小于等于0时抛出
     */
    RateLimitResult evaluate(RateLimitContext context, int permits);

    /**
     * 获取算法类型
     * 用于算法识别和配置匹配
     *
     * @return 算法类型枚举值
     */
    AlgorithmType getType();

    /**
     * 重置算法状态
     * 用于配置变更或手动干预场景
     *
     * <p>重置后的行为：</p>
     * <ul>
     *   <li>令牌桶：重置令牌数为最大容量</li>
     *   <li>窗口类：清空计数器和时间戳</li>
     *   <li>分布式：清除Redis中的相关Key</li>
     * </ul>
     *
     * @param key 限流Key
     */
    void reset(String key);

    /**
     * 获取当前状态描述
     * 用于监控和诊断
     *
     * @param key 限流Key
     * @return 状态描述字符串
     */
    String getStatusDescription(String key);
}