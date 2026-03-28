package com.geek.ratelimit4j.core.exception;

import lombok.Getter;

/**
 * 限流降级异常
 * 当降级处理失败时抛出此异常
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Getter
public class RateLimitFallbackException extends RuntimeException {

    private final RateLimitException originalException;
    private final String fallbackMethod;

    public RateLimitFallbackException(String message, RateLimitException originalException, String fallbackMethod) {
        super(message, originalException);
        this.originalException = originalException;
        this.fallbackMethod = fallbackMethod;
    }

    public RateLimitFallbackException(String fallbackMethod, RateLimitException originalException) {
        super(String.format("Fallback method '%s' not found or execution failed", fallbackMethod), originalException);
        this.originalException = originalException;
        this.fallbackMethod = fallbackMethod;
    }

    public String getOriginalKey() {
        return originalException != null ? originalException.getKey() : null;
    }
}