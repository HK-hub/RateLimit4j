package com.geek.ratelimit4j.starter.autoconfigure;

import com.geek.ratelimit4j.core.config.ModeType;
import com.geek.ratelimit4j.core.config.RateLimitConfig;
import com.geek.ratelimit4j.core.telemetry.TelemetryConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 限流配置属性
 * 绑定 application.yml 中的 ratelimit4j 配置
 *
 * <p>配置示例：</p>
 * <pre>{@code
 * ratelimit4j:
 *   enabled: true
 *   default-mode: LOCAL
 *   default-rule:
 *     algorithm: token_bucket
 *     rate: 100
 *     period: 1
 *   rules:
 *     api-user:
 *       algorithm: sliding_window_counter
 *       rate: 50
 *       period: 60
 * }</pre>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "ratelimit4j")
public class RateLimitProperties {

    /**
     * 全局启用开关
     */
    private boolean enabled = true;

    /**
     * 默认限流模式
     */
    private ModeType defaultMode = ModeType.LOCAL;

    /**
     * 默认限流规则
     */
    private RuleConfig defaultRule = new RuleConfig();

    /**
     * 自定义限流规则
     */
    private Map<String, RuleConfig> rules = new HashMap<>();

    /**
     * Redis存储配置
     */
    private RedisConfig redis = new RedisConfig();

    /**
     * 降级配置
     */
    private FallbackConfig fallback = new FallbackConfig();

    /**
     * 监控配置
     */
    private TelemetryConfigProperties telemetry = new TelemetryConfigProperties();

    // Getter和Setter

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ModeType getDefaultMode() {
        return defaultMode;
    }

    public void setDefaultMode(ModeType defaultMode) {
        this.defaultMode = defaultMode;
    }

    public RuleConfig getDefaultRule() {
        return defaultRule;
    }

    public void setDefaultRule(RuleConfig defaultRule) {
        this.defaultRule = defaultRule;
    }

    public Map<String, RuleConfig> getRules() {
        return rules;
    }

    public void setRules(Map<String, RuleConfig> rules) {
        this.rules = rules;
    }

    public RedisConfig getRedis() {
        return redis;
    }

    public void setRedis(RedisConfig redis) {
        this.redis = redis;
    }

    public FallbackConfig getFallback() {
        return fallback;
    }

    public void setFallback(FallbackConfig fallback) {
        this.fallback = fallback;
    }

    public TelemetryConfigProperties getTelemetry() {
        return telemetry;
    }

    public void setTelemetry(TelemetryConfigProperties telemetry) {
        this.telemetry = telemetry;
    }

    /**
     * 获取指定名称的规则配置
     *
     * @param name 规则名称
     * @return 规则配置，不存在时返回默认规则
     */
    public RuleConfig getRule(String name) {
        if (Objects.isNull(name) || name.isEmpty()) {
            return defaultRule;
        }
        return rules.getOrDefault(name, defaultRule);
    }

    /**
     * 规则配置类
     */
    public static class RuleConfig {
        private String algorithm = "token_bucket";
        private int rate = 100;
        private int period = 1;
        private String keyPrefix = "";
        private int maxBurst = 100;

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        public int getRate() {
            return rate;
        }

        public void setRate(int rate) {
            this.rate = rate;
        }

        public int getPeriod() {
            return period;
        }

        public void setPeriod(int period) {
            this.period = period;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public int getMaxBurst() {
            return maxBurst;
        }

        public void setMaxBurst(int maxBurst) {
            this.maxBurst = maxBurst;
        }

        /**
         * 转换为RateLimitConfig
         *
         * @param name 规则名称
         * @return RateLimitConfig
         */
        public RateLimitConfig toRateLimitConfig(String name) {
            return RateLimitConfig.builder()
                    .name(name)
                    .algorithmType(com.geek.ratelimit4j.core.algorithm.AlgorithmType.fromCode(algorithm))
                    .rate(rate)
                    .period(period)
                    .keyPrefix(keyPrefix)
                    .maxBurst(maxBurst > 0 ? maxBurst : rate)
                    .build();
        }
    }

    /**
     * Redis配置类
     */
    public static class RedisConfig {
        private String host = "localhost";
        private int port = 6379;
        private String password = "";
        private int database = 0;
        private int timeout = 2000;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getDatabase() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }

    /**
     * 降级配置类
     */
    public static class FallbackConfig {
        private boolean enabled = true;
        private boolean degradeToLocal = true;
        private int failureThreshold = 5;
        private long recoveryTimeout = 30000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isDegradeToLocal() {
            return degradeToLocal;
        }

        public void setDegradeToLocal(boolean degradeToLocal) {
            this.degradeToLocal = degradeToLocal;
        }

        public int getFailureThreshold() {
            return failureThreshold;
        }

        public void setFailureThreshold(int failureThreshold) {
            this.failureThreshold = failureThreshold;
        }

        public long getRecoveryTimeout() {
            return recoveryTimeout;
        }

        public void setRecoveryTimeout(long recoveryTimeout) {
            this.recoveryTimeout = recoveryTimeout;
        }
    }

    /**
     * Telemetry配置类
     */
    public static class TelemetryConfigProperties {
        private boolean enabled = true;
        private String serviceName = "RateLimit4j";
        private String endpoint = "http://localhost:4317";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public TelemetryConfig toTelemetryConfig() {
            return TelemetryConfig.builder()
                    .enabled(enabled)
                    .serviceName(serviceName)
                    .endpoint(endpoint)
                    .build();
        }
    }
}