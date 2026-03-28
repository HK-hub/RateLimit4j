package com.geek.ratelimit4j.core.exception;

/**
 * 限流异常
 * 当请求被限流拒绝时抛出此异常
 *
 * <p>异常包含限流详细信息：</p>
 * <ul>
 *   <li>key：限流Key</li>
 *   <li>algorithmType：算法类型</li>
 *   <li>rate：限流速率</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * try {
 *     if (!limiter.tryAcquire()) {
 *         throw new RateLimitException("api:user:login");
 *     }
 * } catch (RateLimitException e) {
 *     // 处理限流逻辑
 *     return ResponseEntity.status(429).body("Too many requests");
 * }
 * }</pre>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class RateLimitException extends RuntimeException {

    /**
     * 限流Key
     */
    private final String key;

    /**
     * 算法类型代码
     */
    private final String algorithmType;

    /**
     * 当前限流速率
     */
    private final int rate;

    /**
     * 等待时间（毫秒）
     */
    private final long waitTimeMs;

    /**
     * 构造限流异常
     *
     * @param message 异常消息
     * @param key 限流Key
     * @param algorithmType 算法类型
     * @param rate 限流速率
     */
    public RateLimitException(String message, String key, String algorithmType, int rate) {
        super(message);
        this.key = key;
        this.algorithmType = algorithmType;
        this.rate = rate;
        this.waitTimeMs = 0;
    }

    /**
     * 构造限流异常（带等待时间）
     *
     * @param message 异常消息
     * @param key 限流Key
     * @param algorithmType 算法类型
     * @param rate 限流速率
     * @param waitTimeMs 建议等待时间
     */
    public RateLimitException(String message, String key, String algorithmType,
                              int rate, long waitTimeMs) {
        super(message);
        this.key = key;
        this.algorithmType = algorithmType;
        this.rate = rate;
        this.waitTimeMs = waitTimeMs;
    }

    /**
     * 构造默认限流异常
     *
     * @param key 限流Key
     */
    public RateLimitException(String key) {
        this("Request rate limit exceeded for key: " + key, key, "unknown", 0);
    }

    /**
     * 构造带速率的限流异常
     *
     * @param key 限流Key
     * @param algorithmType 算法类型
     * @param rate 限流速率
     */
    public RateLimitException(String key, String algorithmType, int rate) {
        this(String.format("Rate limit exceeded: key=%s, algorithm=%s, rate=%d/s",
                          key, algorithmType, rate), key, algorithmType, rate);
    }

    // Getter方法

    public String getKey() {
        return this.key;
    }

    public String getAlgorithmType() {
        return this.algorithmType;
    }

    public int getRate() {
        return this.rate;
    }

    public long getWaitTimeMs() {
        return this.waitTimeMs;
    }

    /**
     * 获取详细错误描述
     *
     * @return 详细描述字符串
     */
    public String getDetailMessage() {
        return String.format("RateLimitException: key=%s, algorithm=%s, rate=%d/s, wait=%dms",
                             key, algorithmType, rate, waitTimeMs);
    }
}