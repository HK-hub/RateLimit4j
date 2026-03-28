package com.geek.ratelimit4j.core.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 限流异常
 * 当请求被限流拒绝时抛出此异常
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public class RateLimitException extends RuntimeException {

    private final String key;
    private final String algorithmType;
    private final int rate;
    private final long waitTimeMs;

    public RateLimitException(String key) {
        super("Request rate limit exceeded for key: " + key);
        this.key = key;
        this.algorithmType = "unknown";
        this.rate = 0;
        this.waitTimeMs = 0;
    }

    public RateLimitException(String key, String algorithmType, int rate) {
        super(String.format("Rate limit exceeded: key=%s, algorithm=%s, rate=%d/s", key, algorithmType, rate));
        this.key = key;
        this.algorithmType = algorithmType;
        this.rate = rate;
        this.waitTimeMs = 0;
    }

    public String getDetailMessage() {
        return String.format("RateLimitException: key=%s, algorithm=%s, rate=%d/s, wait=%dms",
                key, algorithmType, rate, waitTimeMs);
    }
}