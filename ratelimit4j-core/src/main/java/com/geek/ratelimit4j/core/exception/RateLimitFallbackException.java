package com.geek.ratelimit4j.core.exception;

/**
 * 限流降级异常
 * 当降级处理失败时抛出此异常
 *
 * <p>降级失败场景：</p>
 * <ul>
 *   <li>降级方法不存在</li>
 *   <li>降级方法执行异常</li>
 *   <li>降级方法返回类型不匹配</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class RateLimitFallbackException extends RuntimeException {

    /**
     * 原始限流异常
     */
    private final RateLimitException originalException;

    /**
     * 降级方法名称
     */
    private final String fallbackMethod;

    /**
     * 构造降级异常
     *
     * @param message 异常消息
     * @param originalException 原始限流异常
     * @param fallbackMethod 降级方法名称
     */
    public RateLimitFallbackException(String message,
                                       RateLimitException originalException,
                                       String fallbackMethod) {
        super(message, originalException);
        this.originalException = originalException;
        this.fallbackMethod = fallbackMethod;
    }

    /**
     * 构造降级方法不存在异常
     *
     * @param fallbackMethod 降级方法名称
     * @param originalException 原始限流异常
     */
    public RateLimitFallbackException(String fallbackMethod,
                                       RateLimitException originalException) {
        this(String.format("Fallback method '%s' not found or execution failed",
                          fallbackMethod), originalException, fallbackMethod);
    }

    // Getter方法

    public RateLimitException getOriginalException() {
        return this.originalException;
    }

    public String getFallbackMethod() {
        return this.fallbackMethod;
    }

    /**
     * 获取原始限流Key
     *
     * @return 限流Key
     */
    public String getOriginalKey() {
        if (originalException != null) {
            return originalException.getKey();
        }
        return null;
    }
}