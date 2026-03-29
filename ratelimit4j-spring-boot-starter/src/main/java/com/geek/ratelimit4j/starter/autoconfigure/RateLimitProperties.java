package com.geek.ratelimit4j.starter.autoconfigure;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import com.geek.ratelimit4j.core.config.EngineType;
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
 * <p>配置示例：</p>
 * <pre>
 * ratelimit4j:
 *   enabled: true
 *   primary-engine: redis  # 主引擎: redis/local（默认redis）
 *   default-rule:
 *     algorithm: token_bucket
 *     rate: 100
 *     period: 1
 *   redis:
 *     enabled: true
 *     host: localhost
 *     port: 6379
 *   telemetry:
 *     enabled: true
 * </pre>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "ratelimit4j")
public class RateLimitProperties {

    // ==================== 基础配置 ====================

    /**
     * 是否启用限流
     * 全局开关，设为false则完全禁用限流功能
     */
    private boolean enabled = true;

    /**
     * 主引擎类型
     * 当注解指定engine=AUTO时使用此引擎
     * 可选值: redis（默认）, local
     */
    private EngineType primaryEngine = EngineType.REDIS;

    /**
     * 默认限流规则
     * 未指定规则名时使用此规则
     */
    private RuleConfig defaultRule = new RuleConfig();

    /**
     * 命名规则映射
     * Key: 规则名, Value: 规则配置
     */
    private Map<String, RuleConfig> rules = new HashMap<>();

    /**
     * Redis配置
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

    // ==================== 方法 ====================

    /**
     * 获取指定名称的规则配置
     *
     * @param name 规则名称
     * @return 规则配置，未找到时返回默认规则
     */
    public RuleConfig getRule(String name) {
        // 检查名称是否为空
        if (Objects.isNull(name) || name.isEmpty()) {
            // 返回默认规则
            return defaultRule;
        }
        // 从规则映射中获取，未找到时返回默认规则
        return rules.getOrDefault(name, defaultRule);
    }

    // ==================== 内部配置类 ====================

    /**
     * 限流规则配置
     */
    @Data
    public static class RuleConfig {

        /**
         * 算法类型
         * 可选值: token_bucket, leaky_bucket, fixed_window, sliding_window_log, sliding_window_counter
         */
        private String algorithm = "token_bucket";

        /**
         * 每周期允许的请求数
         */
        private int rate = 100;

        /**
         * 限流周期（秒）
         */
        private int period = 1;

        /**
         * Key前缀
         */
        private String keyPrefix = "";

        /**
         * 最大突发容量（令牌桶算法）
         */
        private int maxBurst = 100;

        /**
         * 转换为RateLimitConfig对象
         *
         * @param name 规则名称
         * @return 限流配置对象
         */
        public RateLimitConfig toRateLimitConfig(String name) {
            return RateLimitConfig.builder()
                    // 设置规则名称
                    .name(name)
                    // 解析算法类型
                    .algorithmType(AlgorithmType.fromCode(algorithm))
                    // 设置速率
                    .rate(rate)
                    // 设置周期
                    .period(period)
                    // 设置Key前缀
                    .keyPrefix(keyPrefix)
                    // 设置最大突发（为0时使用rate值）
                    .maxBurst(maxBurst > 0 ? maxBurst : rate)
                    .build();
        }
    }

    /**
     * Redis配置
     */
    @Data
    public static class RedisConfig {

        /**
         * 是否启用Redis限流引擎
         */
        private boolean enabled = true;

        /**
         * Redis主机地址
         */
        private String host = "localhost";

        /**
         * Redis端口
         */
        private int port = 6379;

        /**
         * Redis密码
         */
        private String password = "";

        /**
         * Redis数据库索引
         */
        private int database = 0;

        /**
         * 连接超时时间（毫秒）
         */
        private int timeout = 2000;
    }

    /**
     * 降级配置
     */
    @Data
    public static class FallbackConfig {

        /**
         * 是否启用降级
         */
        private boolean enabled = true;

        /**
         * 是否降级到本地限流
         * Redis不可用时是否使用本地限流
         */
        private boolean degradeToLocal = true;

        /**
         * 失败次数阈值
         * 连续失败多少次后触发熔断
         */
        private int failureThreshold = 5;

        /**
         * 恢复超时时间（毫秒）
         * 熔断后多久尝试恢复
         */
        private long recoveryTimeout = 30000;

        /**
         * 是否启用降级到本地
         *
         * @return true表示启用
         */
        public boolean isDegradeEnabled() {
            // 降级启用且允许降级到本地时返回true
            return BooleanUtils.isFalse(enabled) ? false : degradeToLocal;
        }
    }

    /**
     * 监控配置
     */
    @Data
    public static class TelemetryConfigProperties {

        /**
         * 是否启用监控
         */
        private boolean enabled = true;

        /**
         * 服务名称
         */
        private String serviceName = "RateLimit4j";

        /**
         * OpenTelemetry端点地址
         */
        private String endpoint = "http://localhost:4317";

        /**
         * 转换为TelemetryConfig对象
         *
         * @return 监控配置对象
         */
        public TelemetryConfig toTelemetryConfig() {
            return TelemetryConfig.builder()
                    // 设置是否启用
                    .enabled(enabled)
                    // 设置服务名称
                    .serviceName(serviceName)
                    // 设置端点地址
                    .endpoint(endpoint)
                    .build();
        }
    }
}