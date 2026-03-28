package com.geek.ratelimit4j.core.algorithm;

import com.geek.ratelimit4j.core.config.ModeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.BooleanUtils;

/**
 * 限流算法执行结果
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
public class RateLimitResult {

    private final boolean allowed;
    private final long waitTimeMs;
    private final long remainingPermits;
    private final String key;
    private final AlgorithmType algorithmType;
    private final ModeType modeType;
    private final long executionTimeMs;

    public static RateLimitResult allowed(long remainingPermits, String key,
                                          AlgorithmType algorithmType, ModeType modeType) {
        return new RateLimitResult(true, 0, remainingPermits, key,
                algorithmType, modeType, 0);
    }

    public static RateLimitResult allowed(long remainingPermits, String key,
                                          AlgorithmType algorithmType, ModeType modeType,
                                          long executionTimeMs) {
        return new RateLimitResult(true, 0, remainingPermits, key,
                algorithmType, modeType, executionTimeMs);
    }

    public static RateLimitResult rejected(long waitTimeMs, String key,
                                           AlgorithmType algorithmType, ModeType modeType) {
        return new RateLimitResult(false, waitTimeMs, 0, key,
                algorithmType, modeType, 0);
    }

    public static RateLimitResult rejected(long waitTimeMs, String key,
                                           AlgorithmType algorithmType, ModeType modeType,
                                           long executionTimeMs) {
        return new RateLimitResult(false, waitTimeMs, 0, key,
                algorithmType, modeType, executionTimeMs);
    }

    public boolean isRejected() {
        return BooleanUtils.isFalse(allowed);
    }

    public String getDescription() {
        if (allowed) {
            return String.format("Allowed by %s [%s], remaining: %d",
                    algorithmType.getCode(), modeType.getCode(), remainingPermits);
        }
        return String.format("Rejected by %s [%s], wait: %dms",
                algorithmType.getCode(), modeType.getCode(), waitTimeMs);
    }
}