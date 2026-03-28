package com.geek.ratelimit4j.core.config;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 限流配置类
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitConfig {

    private String name;

    private AlgorithmType algorithmType;

    private int rate;

    private int period;

    private String keyPrefix;

    private int maxBurst;

    public static RateLimitConfig defaultConfig(String name) {
        return RateLimitConfig.builder()
                .name(name)
                .algorithmType(AlgorithmType.TOKEN_BUCKET)
                .rate(100)
                .period(1)
                .keyPrefix("")
                .maxBurst(100)
                .build();
    }

    public String buildKey(String originalKey) {
        if (StringUtils.isBlank(keyPrefix)) {
            return originalKey;
        }
        return keyPrefix + ":" + originalKey;
    }

    public double getRatePerMs() {
        return (double) rate / (period * 1000.0);
    }

    public long getPeriodMs() {
        return period * 1000L;
    }
}