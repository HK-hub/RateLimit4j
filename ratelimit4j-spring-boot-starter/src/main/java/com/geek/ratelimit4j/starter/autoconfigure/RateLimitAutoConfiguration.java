package com.geek.ratelimit4j.starter.autoconfigure;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import com.geek.ratelimit4j.core.config.EngineType;
import com.geek.ratelimit4j.core.config.RateLimitConfig;
import com.geek.ratelimit4j.core.engine.RateLimitEngineProvider;
import com.geek.ratelimit4j.core.storage.StorageProvider;
import com.geek.ratelimit4j.core.telemetry.RateLimitTelemetry;
import com.geek.ratelimit4j.local.algorithm.*;
import com.geek.ratelimit4j.local.circuit.CircuitBreaker;
import com.geek.ratelimit4j.local.engine.LocalEngineProvider;
import com.geek.ratelimit4j.redis.algorithm.RedisFixedWindowAlgorithm;
import com.geek.ratelimit4j.redis.algorithm.RedisLeakyBucketAlgorithm;
import com.geek.ratelimit4j.redis.algorithm.RedisSlidingWindowCounterAlgorithm;
import com.geek.ratelimit4j.redis.algorithm.RedisSlidingWindowLogAlgorithm;
import com.geek.ratelimit4j.redis.algorithm.RedisTokenBucketAlgorithm;
import com.geek.ratelimit4j.redis.engine.RedisEngineProvider;
import com.geek.ratelimit4j.redis.storage.RedisStorageProvider;
import com.geek.ratelimit4j.starter.aspect.RateLimitAspect;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.Objects;

/**
 * RateLimit4j 自动配置类
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>支持本地和Redis两种限流引擎</li>
 *   <li>支持5种限流算法</li>
 *   <li>支持通过配置指定主引擎</li>
 *   <li>支持OpenTelemetry监控</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty(prefix = "ratelimit4j", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(com.geek.ratelimit4j.starter.telemetry.TelemetryAutoConfiguration.class)
@Getter
public class RateLimitAutoConfiguration {

    /**
     * 限流配置属性
     */
    private final RateLimitProperties properties;

    /**
     * 构造自动配置类
     *
     * @param properties 限流配置属性
     */
    public RateLimitAutoConfiguration(RateLimitProperties properties) {
        this.properties = properties;
    }

    // ==================== Redis存储和算法 ====================

    /**
     * 配置Redis存储提供者
     * 需要RedissonClient Bean存在
     *
     * @param redissonClient Redisson客户端
     * @return Redis存储提供者
     */
    @Bean
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "ratelimit4j.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public StorageProvider redisStorageProvider(RedissonClient redissonClient) {
        log.info("[RateLimit4j] Initializing Redis storage provider");
        // 创建Redis存储提供者
        return new RedisStorageProvider(redissonClient);
    }

    /**
     * 配置Redis令牌桶算法
     *
     * @param redissonClient Redisson客户端
     * @return Redis令牌桶算法
     */
    @Bean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "ratelimit4j.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedisTokenBucketAlgorithm redisTokenBucketAlgorithm(RedissonClient redissonClient) {
        log.info("[RateLimit4j] Initializing Redis token bucket algorithm");
        // 构建默认配置
        RateLimitConfig config = properties.getDefaultRule().toRateLimitConfig("redis-token-bucket");
        // 创建Redis令牌桶算法
        return new RedisTokenBucketAlgorithm(redissonClient, config);
    }

    /**
     * 配置Redis固定窗口算法
     *
     * @param redissonClient Redisson客户端
     * @return Redis固定窗口算法
     */
    @Bean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "ratelimit4j.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedisFixedWindowAlgorithm redisFixedWindowAlgorithm(RedissonClient redissonClient) {
        log.info("[RateLimit4j] Initializing Redis fixed window algorithm");
        // 构建默认配置
        RateLimitConfig config = properties.getDefaultRule().toRateLimitConfig("redis-fixed-window");
        // 创建Redis固定窗口算法
        return new RedisFixedWindowAlgorithm(redissonClient, config);
    }

    /**
     * 配置Redis滑动窗口日志算法
     *
     * @param redissonClient Redisson客户端
     * @return Redis滑动窗口日志算法
     */
    @Bean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "ratelimit4j.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedisSlidingWindowLogAlgorithm redisSlidingWindowLogAlgorithm(RedissonClient redissonClient) {
        log.info("[RateLimit4j] Initializing Redis sliding window log algorithm");
        // 构建默认配置
        RateLimitConfig config = properties.getDefaultRule().toRateLimitConfig("redis-sliding-window-log");
        // 创建Redis滑动窗口日志算法
        return new RedisSlidingWindowLogAlgorithm(redissonClient, config);
    }

    /**
     * 配置Redis滑动窗口计数器算法
     *
     * @param redissonClient Redisson客户端
     * @return Redis滑动窗口计数器算法
     */
    @Bean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "ratelimit4j.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedisSlidingWindowCounterAlgorithm redisSlidingWindowCounterAlgorithm(RedissonClient redissonClient) {
        log.info("[RateLimit4j] Initializing Redis sliding window counter algorithm");
        // 构建默认配置
        RateLimitConfig config = properties.getDefaultRule().toRateLimitConfig("redis-sliding-window-counter");
        // 创建Redis滑动窗口计数器算法
        return new RedisSlidingWindowCounterAlgorithm(redissonClient, config);
    }

    /**
     * 配置Redis漏桶算法
     *
     * @param redissonClient Redisson客户端
     * @return Redis漏桶算法
     */
    @Bean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "ratelimit4j.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedisLeakyBucketAlgorithm redisLeakyBucketAlgorithm(RedissonClient redissonClient) {
        log.info("[RateLimit4j] Initializing Redis leaky bucket algorithm");
        // 构建默认配置
        RateLimitConfig config = properties.getDefaultRule().toRateLimitConfig("redis-leaky-bucket");
        // 创建Redis漏桶算法
        return new RedisLeakyBucketAlgorithm(redissonClient, config);
    }

    // ==================== 本地算法 ====================

    /**
     * 配置本地令牌桶算法
     *
     * @return 本地令牌桶算法
     */
    @Bean
    public LocalTokenBucketAlgorithm localTokenBucketAlgorithm() {
        log.info("[RateLimit4j] Initializing local token bucket algorithm");
        // 构建默认配置
        RateLimitConfig config = properties.getDefaultRule().toRateLimitConfig("local-token-bucket");
        // 创建本地令牌桶算法
        return new LocalTokenBucketAlgorithm(config);
    }

    /**
     * 配置本地漏桶算法
     *
     * @return 本地漏桶算法
     */
    @Bean
    public LocalLeakyBucketAlgorithm localLeakyBucketAlgorithm() {
        log.info("[RateLimit4j] Initializing local leaky bucket algorithm");
        // 构建默认配置
        RateLimitConfig config = properties.getDefaultRule().toRateLimitConfig("local-leaky-bucket");
        // 创建本地漏桶算法
        return new LocalLeakyBucketAlgorithm(config);
    }

    /**
     * 配置本地固定窗口算法
     *
     * @return 本地固定窗口算法
     */
    @Bean
    public LocalFixedWindowAlgorithm localFixedWindowAlgorithm() {
        log.info("[RateLimit4j] Initializing local fixed window algorithm");
        // 构建默认配置
        RateLimitConfig config = properties.getDefaultRule().toRateLimitConfig("local-fixed-window");
        // 创建本地固定窗口算法
        return new LocalFixedWindowAlgorithm(config);
    }

    /**
     * 配置本地滑动窗口日志算法
     *
     * @return 本地滑动窗口日志算法
     */
    @Bean
    public LocalSlidingWindowLogAlgorithm localSlidingWindowLogAlgorithm() {
        log.info("[RateLimit4j] Initializing local sliding window log algorithm");
        // 构建默认配置
        RateLimitConfig config = properties.getDefaultRule().toRateLimitConfig("local-sliding-window-log");
        // 创建本地滑动窗口日志算法
        return new LocalSlidingWindowLogAlgorithm(config);
    }

    /**
     * 配置本地滑动窗口计数器算法
     *
     * @return 本地滑动窗口计数器算法
     */
    @Bean
    public LocalSlidingWindowCounterAlgorithm localSlidingWindowCounterAlgorithm() {
        log.info("[RateLimit4j] Initializing local sliding window counter algorithm");
        // 构建默认配置
        RateLimitConfig config = properties.getDefaultRule().toRateLimitConfig("local-sliding-window-counter");
        // 创建本地滑动窗口计数器算法
        return new LocalSlidingWindowCounterAlgorithm(config);
    }

    // ==================== 引擎提供者 ====================

    /**
     * 配置本地引擎提供者
     * Order为200，优先级低于Redis引擎
     *
     * @return 本地引擎提供者
     */
    @Bean
    public LocalEngineProvider localEngineProvider() {
        log.info("[RateLimit4j] Initializing local engine provider");
        // 创建本地引擎提供者
        return new LocalEngineProvider();
    }

    /**
     * 配置Redis引擎提供者
     * Order为100，优先级高于本地引擎
     *
     * @param redissonClient Redisson客户端（可选）
     * @return Redis引擎提供者
     */
    @Bean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "ratelimit4j.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedisEngineProvider redisEngineProvider(RedissonClient redissonClient) {
        log.info("[RateLimit4j] Initializing Redis engine provider");
        // 创建Redis引擎提供者
        return new RedisEngineProvider(redissonClient);
    }

    // ==================== 熔断器 ====================

    /**
     * 配置熔断器
     * 用于Redis不可用时的熔断保护
     *
     * @return 熔断器实例
     */
    @Bean
    public CircuitBreaker circuitBreaker() {
        log.info("[RateLimit4j] Initializing circuit breaker");
        // 获取降级配置
        RateLimitProperties.FallbackConfig fallbackConfig = properties.getFallback();
        // 创建熔断器
        return new CircuitBreaker(
                "default-circuit-breaker",
                fallbackConfig.getFailureThreshold(),
                fallbackConfig.getRecoveryTimeout()
        );
    }

    // ==================== 其他组件 ====================

    /**
     * 配置限流切面
     *
     * @param tokenBucket          本地令牌桶算法
     * @param leakyBucket          本地漏桶算法
     * @param fixedWindow          本地固定窗口算法
     * @param slidingWindowLog     本地滑动窗口日志算法
     * @param slidingWindowCounter 本地滑动窗口计数器算法
     * @param applicationContext   Spring应用上下文
     * @param telemetry            监控组件
     * @param redisTokenBucket     Redis令牌桶算法（可选）
     * @param redisFixedWindow     Redis固定窗口算法（可选）
     * @param redisSlidingWindowLog Redis滑动窗口日志算法（可选）
     * @param redisSlidingWindowCounter Redis滑动窗口计数器算法（可选）
     * @param redisLeakyBucket     Redis漏桶算法（可选）
     * @return 限流切面
     */
    @Bean
    public RateLimitAspect rateLimitAspect(
            LocalTokenBucketAlgorithm tokenBucket,
            LocalLeakyBucketAlgorithm leakyBucket,
            LocalFixedWindowAlgorithm fixedWindow,
            LocalSlidingWindowLogAlgorithm slidingWindowLog,
            LocalSlidingWindowCounterAlgorithm slidingWindowCounter,
            ApplicationContext applicationContext,
            @Autowired(required = false) RateLimitTelemetry telemetry,
            @Autowired(required = false) RedisTokenBucketAlgorithm redisTokenBucket,
            @Autowired(required = false) RedisFixedWindowAlgorithm redisFixedWindow,
            @Autowired(required = false) RedisSlidingWindowLogAlgorithm redisSlidingWindowLog,
            @Autowired(required = false) RedisSlidingWindowCounterAlgorithm redisSlidingWindowCounter,
            @Autowired(required = false) RedisLeakyBucketAlgorithm redisLeakyBucket) {

        log.info("[RateLimit4j] Initializing rate limit aspect with primary engine: {}", 
                 properties.getPrimaryEngine());

        // 创建限流切面
        RateLimitAspect aspect = new RateLimitAspect(
                tokenBucket, leakyBucket, fixedWindow,
                slidingWindowLog, slidingWindowCounter, applicationContext, telemetry);

        // 注册Redis算法（如果存在）
        if (Objects.nonNull(redisTokenBucket)) {
            log.info("[RateLimit4j] Registering Redis token bucket algorithm");
            aspect.registerRedisAlgorithm(AlgorithmType.TOKEN_BUCKET, redisTokenBucket);
        }
        if (Objects.nonNull(redisFixedWindow)) {
            log.info("[RateLimit4j] Registering Redis fixed window algorithm");
            aspect.registerRedisAlgorithm(AlgorithmType.FIXED_WINDOW, redisFixedWindow);
        }
        if (Objects.nonNull(redisSlidingWindowLog)) {
            log.info("[RateLimit4j] Registering Redis sliding window log algorithm");
            aspect.registerRedisAlgorithm(AlgorithmType.SLIDING_WINDOW_LOG, redisSlidingWindowLog);
        }
        if (Objects.nonNull(redisSlidingWindowCounter)) {
            log.info("[RateLimit4j] Registering Redis sliding window counter algorithm");
            aspect.registerRedisAlgorithm(AlgorithmType.SLIDING_WINDOW_COUNTER, redisSlidingWindowCounter);
        }
        if (Objects.nonNull(redisLeakyBucket)) {
            log.info("[RateLimit4j] Registering Redis leaky bucket algorithm");
            aspect.registerRedisAlgorithm(AlgorithmType.LEAKY_BUCKET, redisLeakyBucket);
        }

        return aspect;
    }
}