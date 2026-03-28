package com.geek.ratelimit4j.starter.telemetry;

import com.geek.ratelimit4j.core.telemetry.RateLimitTelemetry;
import com.geek.ratelimit4j.core.telemetry.TelemetryConfig;
import com.geek.ratelimit4j.starter.autoconfigure.RateLimitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Telemetry自动配置
 * 配置OpenTelemetry监控集成
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass(name = "io.opentelemetry.api.OpenTelemetry")
@ConditionalOnProperty(prefix = "ratelimit4j.telemetry", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TelemetryAutoConfiguration {

    /**
     * 注册OpenTelemetry监控
     *
     * @param properties 配置属性
     * @return 监控实现
     */
    @Bean
    public RateLimitTelemetry rateLimitTelemetry(RateLimitProperties properties) {
        TelemetryConfig config = properties.getTelemetry().toTelemetryConfig();
        return new OpenTelemetryRateLimitTelemetry(config);
    }
}