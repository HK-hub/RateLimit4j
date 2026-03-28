package com.geek.ratelimit4j.starter.autoconfigure;

import com.geek.ratelimit4j.core.storage.StorageProvider;
import com.geek.ratelimit4j.core.telemetry.RateLimitTelemetry;
import com.geek.ratelimit4j.local.algorithm.*;
import com.geek.ratelimit4j.local.circuit.CircuitBreaker;
import com.geek.ratelimit4j.redis.storage.RedisStorageProvider;
import com.geek.ratelimit4j.starter.aspect.RateLimitAspect;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.Objects;

/**
 * RateLimit4j 自动配置类
 * 自动配置限流器相关Bean
 *
 * <p>配置示例：</p>
 * <pre>{@code
 * ratelimit4j:
 *   enabled: true
 *   default-mode: LOCAL
 * }</pre>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty(prefix = "ratelimit4j", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(com.geek.ratelimit4j.starter.telemetry.TelemetryAutoConfiguration.class)
public class RateLimitAutoConfiguration {

    private final RateLimitProperties properties;

    public RateLimitAutoConfiguration(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "ratelimit4j.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public StorageProvider redisStorageProvider(RedissonClient redissonClient) {
        return new RedisStorageProvider(redissonClient);
    }

    @Bean
    public LocalTokenBucketAlgorithm localTokenBucketAlgorithm() {
        return new LocalTokenBucketAlgorithm(
                properties.getDefaultRule().toRateLimitConfig("default-token-bucket"));
    }

    @Bean
    public LocalLeakyBucketAlgorithm localLeakyBucketAlgorithm() {
        return new LocalLeakyBucketAlgorithm(
                properties.getDefaultRule().toRateLimitConfig("default-leaky-bucket"));
    }

    @Bean
    public LocalFixedWindowAlgorithm localFixedWindowAlgorithm() {
        return new LocalFixedWindowAlgorithm(
                properties.getDefaultRule().toRateLimitConfig("default-fixed-window"));
    }

    @Bean
    public LocalSlidingWindowLogAlgorithm localSlidingWindowLogAlgorithm() {
        return new LocalSlidingWindowLogAlgorithm(
                properties.getDefaultRule().toRateLimitConfig("default-sliding-window-log"));
    }

    @Bean
    public LocalSlidingWindowCounterAlgorithm localSlidingWindowCounterAlgorithm() {
        return new LocalSlidingWindowCounterAlgorithm(
                properties.getDefaultRule().toRateLimitConfig("default-sliding-window-counter"));
    }

    @Bean
    public CircuitBreaker circuitBreaker() {
        RateLimitProperties.FallbackConfig fallbackConfig = properties.getFallback();
        return new CircuitBreaker(
                "default-circuit-breaker",
                fallbackConfig.getFailureThreshold(),
                fallbackConfig.getRecoveryTimeout());
    }

    @Bean
    public RateLimitAspect rateLimitAspect(
            LocalTokenBucketAlgorithm tokenBucket,
            LocalLeakyBucketAlgorithm leakyBucket,
            LocalFixedWindowAlgorithm fixedWindow,
            LocalSlidingWindowLogAlgorithm slidingWindowLog,
            LocalSlidingWindowCounterAlgorithm slidingWindowCounter,
            @Autowired(required = false) RateLimitTelemetry telemetry) {
        return new RateLimitAspect(
                tokenBucket, leakyBucket, fixedWindow,
                slidingWindowLog, slidingWindowCounter, telemetry);
    }
}