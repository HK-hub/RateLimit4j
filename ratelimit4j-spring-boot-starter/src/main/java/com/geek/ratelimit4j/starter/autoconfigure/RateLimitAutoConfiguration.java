package com.geek.ratelimit4j.starter.autoconfigure;

import com.geek.ratelimit4j.core.config.RateLimitConfig;
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
import com.geek.ratelimit4j.core.registry.AlgorithmRegistry;
import com.geek.ratelimit4j.starter.registry.DefaultAlgorithmRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * RateLimit4j 自动配置类
 *
 * <p>设计改进：</p>
 * <ul>
 *   <li>基于AlgorithmRegistry自动发现算法，遵循开闭原则</li>
 *   <li>新增算法无需修改配置类</li>
 *   <li>所有算法Bean添加@ConditionalOnMissingBean，支持用户自定义扩展</li>
 * </ul>
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>支持本地和Redis两种限流引擎</li>
 *   <li>支持5种限流算法</li>
 *   <li>支持通过配置指定主引擎</li>
 *   <li>支持OpenTelemetry监控</li>
 *   <li>支持用户自定义算法替换</li>
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

    // ==================== 算法注册中心 ====================

    /**
     * 配置算法注册中心
     * 自动收集所有RateLimitAlgorithm Bean
     *
     * @param algorithmsProvider 算法列表提供者（Spring自动收集所有RateLimitAlgorithm Bean）
     * @return 算法注册中心
     */
    @Bean
    @ConditionalOnMissingBean
    public AlgorithmRegistry algorithmRegistry(
            ObjectProvider<java.util.List<com.geek.ratelimit4j.core.algorithm.RateLimitAlgorithm>> algorithmsProvider) {
        log.info("[RateLimit4j] Initializing AlgorithmRegistry");
        return new DefaultAlgorithmRegistry(algorithmsProvider);
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
    @ConditionalOnMissingBean
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "ratelimit4j.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public StorageProvider redisStorageProvider(RedissonClient redissonClient) {
        log.info("[RateLimit4j] Initializing Redis storage provider");
        return new RedisStorageProvider(redissonClient);
    }

    /**
     * 配置Redis令牌桶算法
     *
     * @param redissonClient Redisson客户端
     * @return Redis令牌桶算法
     */
    @Bean
    @ConditionalOnMissingBean(RedisTokenBucketAlgorithm.class)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "ratelimit4j.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedisTokenBucketAlgorithm redisTokenBucketAlgorithm(RedissonClient redissonClient) {
        log.info("[RateLimit4j] Initializing Redis token bucket algorithm");
        RateLimitConfig config = properties.getDefaultRule().toRateLimitConfig("redis-token-bucket");
        return new RedisTokenBucketAlgorithm(redissonClient, config);
    }

    /**
     * 配置Redis固定窗口算法
     *
     * @param redissonClient Redisson客户端
     * @return Redis固定窗口算法
     */
    @Bean
    @ConditionalOnMissingBean(RedisFixedWindowAlgorithm.class)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "ratelimit4j.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedisFixedWindowAlgorithm redisFixedWindowAlgorithm(RedissonClient redissonClient) {
        log.info("[RateLimit4j] Initializing Redis fixed window algorithm");
        RateLimitConfig config = properties.getDefaultRule().toRateLimitConfig("redis-fixed-window");
        return new RedisFixedWindowAlgorithm(redissonClient, config);
    }

    /**
     * 配置Redis滑动窗口日志算法
     *
     * @param redissonClient Redisson客户端
     * @return Redis滑动窗口日志算法
     */
    @Bean
    @ConditionalOnMissingBean(RedisSlidingWindowLogAlgorithm.class)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "ratelimit4j.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedisSlidingWindowLogAlgorithm redisSlidingWindowLogAlgorithm(RedissonClient redissonClient) {
        log.info("[RateLimit4j] Initializing Redis sliding window log algorithm");
        RateLimitConfig config = properties.getDefaultRule().toRateLimitConfig("redis-sliding-window-log");
        return new RedisSlidingWindowLogAlgorithm(redissonClient, config);
    }

    /**
     * 配置Redis滑动窗口计数器算法
     *
     * @param redissonClient Redisson客户端
     * @return Redis滑动窗口计数器算法
     */
    @Bean
    @ConditionalOnMissingBean(RedisSlidingWindowCounterAlgorithm.class)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "ratelimit4j.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedisSlidingWindowCounterAlgorithm redisSlidingWindowCounterAlgorithm(RedissonClient redissonClient) {
        log.info("[RateLimit4j] Initializing Redis sliding window counter algorithm");
        RateLimitConfig config = properties.getDefaultRule().toRateLimitConfig("redis-sliding-window-counter");
        return new RedisSlidingWindowCounterAlgorithm(redissonClient, config);
    }

    /**
     * 配置Redis漏桶算法
     *
     * @param redissonClient Redisson客户端
     * @return Redis漏桶算法
     */
    @Bean
    @ConditionalOnMissingBean(RedisLeakyBucketAlgorithm.class)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "ratelimit4j.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedisLeakyBucketAlgorithm redisLeakyBucketAlgorithm(RedissonClient redissonClient) {
        log.info("[RateLimit4j] Initializing Redis leaky bucket algorithm");
        RateLimitConfig config = properties.getDefaultRule().toRateLimitConfig("redis-leaky-bucket");
        return new RedisLeakyBucketAlgorithm(redissonClient, config);
    }

    // ==================== 本地算法 ====================

    /**
     * 配置本地令牌桶算法
     *
     * @return 本地令牌桶算法
     */
    @Bean
    @ConditionalOnMissingBean(LocalTokenBucketAlgorithm.class)
    public LocalTokenBucketAlgorithm localTokenBucketAlgorithm() {
        log.info("[RateLimit4j] Initializing local token bucket algorithm");
        RateLimitConfig config = properties.getDefaultRule().toRateLimitConfig("local-token-bucket");
        return new LocalTokenBucketAlgorithm(config);
    }

    /**
     * 配置本地漏桶算法
     *
     * @return 本地漏桶算法
     */
    @Bean
    @ConditionalOnMissingBean(LocalLeakyBucketAlgorithm.class)
    public LocalLeakyBucketAlgorithm localLeakyBucketAlgorithm() {
        log.info("[RateLimit4j] Initializing local leaky bucket algorithm");
        RateLimitConfig config = properties.getDefaultRule().toRateLimitConfig("local-leaky-bucket");
        return new LocalLeakyBucketAlgorithm(config);
    }

    /**
     * 配置本地固定窗口算法
     *
     * @return 本地固定窗口算法
     */
    @Bean
    @ConditionalOnMissingBean(LocalFixedWindowAlgorithm.class)
    public LocalFixedWindowAlgorithm localFixedWindowAlgorithm() {
        log.info("[RateLimit4j] Initializing local fixed window algorithm");
        RateLimitConfig config = properties.getDefaultRule().toRateLimitConfig("local-fixed-window");
        return new LocalFixedWindowAlgorithm(config);
    }

    /**
     * 配置本地滑动窗口日志算法
     *
     * @return 本地滑动窗口日志算法
     */
    @Bean
    @ConditionalOnMissingBean(LocalSlidingWindowLogAlgorithm.class)
    public LocalSlidingWindowLogAlgorithm localSlidingWindowLogAlgorithm() {
        log.info("[RateLimit4j] Initializing local sliding window log algorithm");
        RateLimitConfig config = properties.getDefaultRule().toRateLimitConfig("local-sliding-window-log");
        return new LocalSlidingWindowLogAlgorithm(config);
    }

    /**
     * 配置本地滑动窗口计数器算法
     *
     * @return 本地滑动窗口计数器算法
     */
    @Bean
    @ConditionalOnMissingBean(LocalSlidingWindowCounterAlgorithm.class)
    public LocalSlidingWindowCounterAlgorithm localSlidingWindowCounterAlgorithm() {
        log.info("[RateLimit4j] Initializing local sliding window counter algorithm");
        RateLimitConfig config = properties.getDefaultRule().toRateLimitConfig("local-sliding-window-counter");
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
    @ConditionalOnMissingBean
    public LocalEngineProvider localEngineProvider() {
        log.info("[RateLimit4j] Initializing local engine provider");
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
    @ConditionalOnMissingBean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "ratelimit4j.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedisEngineProvider redisEngineProvider(RedissonClient redissonClient) {
        log.info("[RateLimit4j] Initializing Redis engine provider");
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
    @ConditionalOnMissingBean
    public CircuitBreaker circuitBreaker() {
        log.info("[RateLimit4j] Initializing circuit breaker");
        RateLimitProperties.FallbackConfig fallbackConfig = properties.getFallback();
        return new CircuitBreaker(
                "default-circuit-breaker",
                fallbackConfig.getFailureThreshold(),
                fallbackConfig.getRecoveryTimeout()
        );
    }

    // ==================== 维度解析器 ====================

    /**
     * 配置维度解析器注册中心
     * 自动收集所有DimensionResolver Bean
     *
     * @param resolvers 所有维度解析器
     * @return 解析器注册中心
     */
    @Bean
    @ConditionalOnMissingBean
    public com.geek.ratelimit4j.core.resolver.DimensionResolverRegistry dimensionResolverRegistry(
            java.util.List<com.geek.ratelimit4j.core.resolver.DimensionResolver> resolvers) {
        log.info("[RateLimit4j] Initializing DimensionResolverRegistry with {} resolvers", resolvers.size());
        com.geek.ratelimit4j.core.resolver.DimensionResolverRegistry registry =
                new com.geek.ratelimit4j.core.resolver.DimensionResolverRegistry();
        for (com.geek.ratelimit4j.core.resolver.DimensionResolver resolver : resolvers) {
            registry.register(resolver);
        }
        return registry;
    }

    /**
     * 配置IP维度解析器
     */
    @Bean
    @ConditionalOnMissingBean
    public com.geek.ratelimit4j.core.resolver.DimensionResolver ipDimensionResolver() {
        log.info("[RateLimit4j] Initializing IpDimensionResolver");
        return new com.geek.ratelimit4j.starter.resolver.IpDimensionResolver();
    }

    /**
     * 配置用户维度解析器
     */
    @Bean
    @ConditionalOnMissingBean
    public com.geek.ratelimit4j.core.resolver.DimensionResolver userDimensionResolver() {
        log.info("[RateLimit4j] Initializing UserDimensionResolver");
        return new com.geek.ratelimit4j.starter.resolver.UserDimensionResolver();
    }

    /**
     * 配置租户维度解析器
     */
    @Bean
    @ConditionalOnMissingBean
    public com.geek.ratelimit4j.core.resolver.DimensionResolver tenantDimensionResolver() {
        log.info("[RateLimit4j] Initializing TenantDimensionResolver");
        return new com.geek.ratelimit4j.starter.resolver.TenantDimensionResolver();
    }

    /**
     * 配置设备维度解析器
     */
    @Bean
    @ConditionalOnMissingBean
    public com.geek.ratelimit4j.core.resolver.DimensionResolver deviceDimensionResolver() {
        log.info("[RateLimit4j] Initializing DeviceDimensionResolver");
        return new com.geek.ratelimit4j.starter.resolver.DeviceDimensionResolver();
    }

    /**
     * 配置方法维度解析器
     */
    @Bean
    @ConditionalOnMissingBean
    public com.geek.ratelimit4j.core.resolver.DimensionResolver methodDimensionResolver() {
        log.info("[RateLimit4j] Initializing MethodDimensionResolver");
        return new com.geek.ratelimit4j.starter.resolver.MethodDimensionResolver();
    }

    // ==================== 限流切面 ====================

    /**
     * 配置限流切面
     *
     * @param algorithmRegistry        算法注册中心
     * @param applicationContext       Spring应用上下文
     * @param dimensionResolverRegistry 维度解析器注册中心
     * @param telemetry                监控组件（可选）
     * @return 限流切面
     */
    @Bean
    @ConditionalOnMissingBean
    public RateLimitAspect rateLimitAspect(
            AlgorithmRegistry algorithmRegistry,
            ApplicationContext applicationContext,
            com.geek.ratelimit4j.core.resolver.DimensionResolverRegistry dimensionResolverRegistry,
            @Autowired(required = false) RateLimitTelemetry telemetry) {

        log.info("[RateLimit4j] Initializing rate limit aspect with primary engine: {}",
                 properties.getPrimaryEngine());

        return new RateLimitAspect(algorithmRegistry, applicationContext, properties, dimensionResolverRegistry, telemetry);
    }
}