package com.geek.ratelimit4j.core.config;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 限流算法执行上下文
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitContext {

    private String key;

    private RateLimitConfig config;

    private long requestTimestamp;

    private ModeType modeType;

    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    public static RateLimitContext of(String key, RateLimitConfig config, ModeType modeType) {
        return RateLimitContext.builder()
                .key(key)
                .config(config)
                .requestTimestamp(System.currentTimeMillis())
                .modeType(modeType)
                .attributes(new HashMap<>())
                .build();
    }

    public Object getAttribute(String name) {
        return Objects.nonNull(attributes) ? attributes.get(name) : null;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String name, Class<T> type) {
        Object value = getAttribute(name);
        if (Objects.isNull(value)) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    public int getRate() {
        return Objects.nonNull(config) ? config.getRate() : 0;
    }

    public int getPeriod() {
        return Objects.nonNull(config) ? config.getPeriod() : 1;
    }

    public boolean isDistributed() {
        return Objects.equals(modeType, ModeType.DISTRIBUTED);
    }
}