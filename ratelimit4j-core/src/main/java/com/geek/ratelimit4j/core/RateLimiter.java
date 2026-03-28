package com.geek.ratelimit4j.core;

import com.geek.ratelimit4j.core.config.RateLimitConfig;

/**
 * 限流器核心接口
 * 所有限流算法实现必须遵循此接口契约
 * 提供统一的限流操作方法，支持单许可和多许可获取
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * RateLimiter limiter = registry.getRateLimiter("api-user");
 * if (limiter.tryAcquire()) {
 *     // 执行业务逻辑
 * } else {
 *     // 处理限流情况
 * }
 * }</pre>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public interface RateLimiter {

    /**
     * 尝试获取单个许可
     * 非阻塞操作，立即返回获取结果
     *
     * <p>此方法是限流判断的核心方法，内部根据配置的算法类型
     * 执行相应的限流计算逻辑。</p>
     *
     * @return true表示获取成功，请求可以继续执行；
     *         false表示被限流，请求应当被拒绝或降级
     */
    boolean tryAcquire();

    /**
     * 尝试获取指定数量的许可
     * 用于批量操作或权重不同的请求
     *
     * <p>适用场景：</p>
     * <ul>
     *   <li>批量API请求</li>
     *   <li>不同权重的请求处理</li>
     *   <li>资源消耗较大的操作</li>
     * </ul>
     *
     * @param permits 需要获取的许可数量，必须大于0
     * @return true表示获取成功，false表示被限流
     * @throws IllegalArgumentException 当permits小于等于0时抛出
     */
    boolean tryAcquire(int permits);

    /**
     * 获取当前限流器的配置信息
     * 包含限流速率、算法类型等关键配置
     *
     * <p>配置信息可用于：</p>
     * <ul>
     *   <li>监控和诊断</li>
     *   <li>动态调整限流策略</li>
     *   <li>问题排查</li>
     * </ul>
     *
     * @return 限流配置对象，不应为null
     */
    RateLimitConfig getConfig();

    /**
     * 获取当前可用的许可数量
     * 用于监控和调试，实时了解限流器状态
     *
     * <p>注意：</p>
     * <ul>
     *   <li>返回值可能不准确（分布式环境存在延迟）</li>
     *   <li>仅用于监控和诊断，不用于限流判断</li>
     *   <li>令牌桶算法返回当前令牌数</li>
     *   <li>窗口类算法返回剩余配额</li>
     * </ul>
     *
     * @return 当前可用许可数量，可能为0或正数
     */
    long getAvailablePermits();

    /**
     * 获取限流器的唯一标识名称
     * 用于在Registry中查找和管理限流器
     *
     * <p>命名规范：</p>
     * <ul>
     *   <li>推荐使用 "模块:功能:维度" 格式</li>
     *   <li>例如：api:user:login、web:ip:index</li>
     * </ul>
     *
     * @return 限流器名称，不应为空
     */
    String getName();

    /**
     * 判断限流器是否为分布式模式
     * 用于选择降级策略和监控分类
     *
     * @return true表示分布式模式，false表示本地模式
     */
    boolean isDistributed();
}