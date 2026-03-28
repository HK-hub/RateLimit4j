package com.geek.ratelimit4j.core.algorithm;

import com.geek.ratelimit4j.core.config.ModeType;

import java.util.Objects;

/**
 * 限流算法执行结果
 * 封装限流判断的返回信息，包含是否允许、等待时间、剩余配额等
 *
 * <p>结果类型：</p>
 * <ul>
 *   <li>允许(Allowed)：请求可以继续执行</li>
 *   <li>拒绝(Rejected)：请求被限流，需等待或降级</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class RateLimitResult {

    /**
     * 是否允许请求通过
     */
    private final boolean allowed;

    /**
     * 被拒绝时的建议等待时间（毫秒）
     * 为0表示立即拒绝，大于0表示需要等待的时间
     */
    private final long waitTimeMs;

    /**
     * 当前剩余许可数量
     * 用于监控和限流状态展示
     */
    private final long remainingPermits;

    /**
     * 限流Key
     * 用于日志记录和监控追踪
     */
    private final String key;

    /**
     * 算法类型
     * 用于结果分类和监控统计
     */
    private final AlgorithmType algorithmType;

    /**
     * 执行模式
     */
    private final ModeType modeType;

    /**
     * 执行耗时（毫秒）
     * 用于性能监控
     */
    private final long executionTimeMs;

    /**
     * 构造限流结果
     *
     * @param allowed 是否允许
     * @param waitTimeMs 等待时间（毫秒）
     * @param remainingPermits 剩余许可数量
     * @param key 限流Key
     * @param algorithmType 算法类型
     * @param modeType 执行模式
     * @param executionTimeMs 执行耗时（毫秒）
     */
    public RateLimitResult(boolean allowed, long waitTimeMs, long remainingPermits,
                           String key, AlgorithmType algorithmType, ModeType modeType,
                           long executionTimeMs) {
        this.allowed = allowed;
        this.waitTimeMs = waitTimeMs;
        this.remainingPermits = remainingPermits;
        this.key = key;
        this.algorithmType = algorithmType;
        this.modeType = modeType;
        this.executionTimeMs = executionTimeMs;
    }

    /**
     * 创建允许通过的结果
     *
     * @param remainingPermits 剩余许可数量
     * @param key 限流Key
     * @param algorithmType 算法类型
     * @param modeType 执行模式
     * @return 允许通过的限流结果
     */
    public static RateLimitResult allowed(long remainingPermits, String key,
                                          AlgorithmType algorithmType, ModeType modeType) {
        return new RateLimitResult(true, 0, remainingPermits, key,
                                   algorithmType, modeType, 0);
    }

    /**
     * 创建允许通过的结果（带执行时间）
     *
     * @param remainingPermits 剩余许可数量
     * @param key 限流Key
     * @param algorithmType 算法类型
     * @param modeType 执行模式
     * @param executionTimeMs 执行耗时
     * @return 允许通过的限流结果
     */
    public static RateLimitResult allowed(long remainingPermits, String key,
                                          AlgorithmType algorithmType, ModeType modeType,
                                          long executionTimeMs) {
        return new RateLimitResult(true, 0, remainingPermits, key,
                                   algorithmType, modeType, executionTimeMs);
    }

    /**
     * 创建拒绝的结果
     *
     * @param waitTimeMs 建议等待时间（毫秒）
     * @param key 限流Key
     * @param algorithmType 算法类型
     * @param modeType 执行模式
     * @return 被拒绝的限流结果
     */
    public static RateLimitResult rejected(long waitTimeMs, String key,
                                           AlgorithmType algorithmType, ModeType modeType) {
        return new RateLimitResult(false, waitTimeMs, 0, key,
                                   algorithmType, modeType, 0);
    }

    /**
     * 创建拒绝的结果（带执行时间）
     *
     * @param waitTimeMs 建议等待时间（毫秒）
     * @param key 限流Key
     * @param algorithmType 算法类型
     * @param modeType 执行模式
     * @param executionTimeMs 执行耗时
     * @return 被拒绝的限流结果
     */
    public static RateLimitResult rejected(long waitTimeMs, String key,
                                           AlgorithmType algorithmType, ModeType modeType,
                                           long executionTimeMs) {
        return new RateLimitResult(false, waitTimeMs, 0, key,
                                   algorithmType, modeType, executionTimeMs);
    }

    /**
     * 判断是否被限流
     * 
     * @return true表示被限流，false表示允许通过
     */
    public boolean isRejected() {
        return Objects.equals(this.allowed, false);
    }

    // Getter方法

    public boolean isAllowed() {
        return this.allowed;
    }

    public long getWaitTimeMs() {
        return this.waitTimeMs;
    }

    public long getRemainingPermits() {
        return this.remainingPermits;
    }

    public String getKey() {
        return this.key;
    }

    public AlgorithmType getAlgorithmType() {
        return this.algorithmType;
    }

    public ModeType getModeType() {
        return this.modeType;
    }

    public long getExecutionTimeMs() {
        return this.executionTimeMs;
    }

    /**
     * 获取结果描述
     * 用于日志和监控
     *
     * @return 结果描述字符串
     */
    public String getDescription() {
        if (allowed) {
            return String.format("Allowed by %s [%s], remaining: %d",
                                 algorithmType.getCode(), modeType.getCode(), remainingPermits);
        } else {
            return String.format("Rejected by %s [%s], wait: %dms",
                                 algorithmType.getCode(), modeType.getCode(), waitTimeMs);
        }
    }
}