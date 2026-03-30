package com.geek.ratelimit4j.starter.aspect;

import com.geek.ratelimit4j.core.registry.AlgorithmRegistry;
import com.geek.ratelimit4j.core.telemetry.RateLimitTelemetry;
import com.geek.ratelimit4j.starter.autoconfigure.RateLimitProperties;
import com.geek.ratelimit4j.starter.resolver.RateLimitKeyResolver;
import com.geek.ratelimit4j.core.engine.EngineProviderRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.util.Objects;

/**
 * 默认限流切面实现
 * 提供完整的限流处理逻辑
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Slf4j
public class DefaultRateLimitAspect extends AbstractRateLimitAspect {

    private final RateLimitProperties properties;

    public DefaultRateLimitAspect(
            AlgorithmRegistry algorithmRegistry,
            ApplicationContext applicationContext,
            RateLimitTelemetry telemetry,
            RateLimitKeyResolver keyResolver,
            RateLimitProperties properties,
            EngineProviderRegistry engineProviderRegistry) {
        super(algorithmRegistry, applicationContext, telemetry, keyResolver, engineProviderRegistry);
        this.properties = Objects.requireNonNull(properties, "properties must not be null");

        log.info("[RateLimit4j] DefaultRateLimitAspect initialized");
    }
}