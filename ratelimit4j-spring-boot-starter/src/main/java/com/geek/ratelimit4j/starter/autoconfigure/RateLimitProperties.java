package com.geek.ratelimit4j.starter.autoconfigure;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import com.geek.ratelimit4j.core.config.RateLimitConfig;
import com.geek.ratelimit4j.core.telemetry.TelemetryConfig;
import lombok.Data;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 限流配置属性
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "ratelimit4j")
public class RateLimitProperties {

    private boolean enabled = true;

    private com.geek.ratelimit4j.core.config.ModeType defaultMode = 
            com.geek.ratelimit4j.core.config.ModeType.LOCAL;

    private RuleConfig defaultRule = new RuleConfig();

    private Map<String, RuleConfig> rules = new HashMap<>();

    private RedisConfig redis = new RedisConfig();

    private FallbackConfig fallback = new FallbackConfig();

    private TelemetryConfigProperties telemetry = new TelemetryConfigProperties();

    public RuleConfig getRule(String name) {
        if (Objects.isNull(name) || name.isEmpty()) {
            return defaultRule;
        }
        return rules.getOrDefault(name, defaultRule);
    }

    @Data
    public static class RuleConfig {
        private String algorithm = "token_bucket";
        private int rate = 100;
        private int period = 1;
        private String keyPrefix = "";
        private int maxBurst = 100;

        public RateLimitConfig toRateLimitConfig(String name) {
            return RateLimitConfig.builder()
                    .name(name)
                    .algorithmType(AlgorithmType.fromCode(algorithm))
                    .rate(rate)
                    .period(period)
                    .keyPrefix(keyPrefix)
                    .maxBurst(maxBurst > 0 ? maxBurst : rate)
                    .build();
        }
    }

    @Data
    public static class RedisConfig {
        private String host = "localhost";
        private int port = 6379;
        private String password = "";
        private int database = 0;
        private int timeout = 2000;
    }

    @Data
    public static class FallbackConfig {
        private boolean enabled = true;
        private boolean degradeToLocal = true;
        private int failureThreshold = 5;
        private long recoveryTimeout = 30000;

        public boolean isDegradeEnabled() {
            return BooleanUtils.isFalse(enabled) ? false : degradeToLocal;
        }
    }

    @Data
    public static class TelemetryConfigProperties {
        private boolean enabled = true;
        private String serviceName = "RateLimit4j";
        private String endpoint = "http://localhost:4317";

        public TelemetryConfig toTelemetryConfig() {
            return TelemetryConfig.builder()
                    .enabled(enabled)
                    .serviceName(serviceName)
                    .endpoint(endpoint)
                    .build();
        }
    }
}