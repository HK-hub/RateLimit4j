package com.geek.ratelimit4j.core.annotation;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import com.geek.ratelimit4j.core.config.DimensionType;
import com.geek.ratelimit4j.core.config.EngineType;
import com.geek.ratelimit4j.core.exception.RateLimitException;
import com.geek.ratelimit4j.core.handler.FallbackHandler;
import com.geek.ratelimit4j.core.resolver.KeyBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IP维度限流注解
 * 基于客户端IP地址进行限流
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 每秒最多10次请求（按IP限制）
 * @IpRateLimit(rate = 10, period = 1)
 * public String api(HttpServletRequest request) { ... }
 *
 * // 使用Redis分布式限流
 * @IpRateLimit(rate = 100, period = 1, engine = EngineType.REDIS)
 * public String distributedApi(HttpServletRequest request) { ... }
 * }</pre>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@RateLimit(dimension = DimensionType.IP)
public @interface IpRateLimit {

    /**
     * 每周期允许的请求数
     */
    int rate() default 100;

    /**
     * 限流周期（秒）
     */
    int period() default 1;

    /**
     * 限流算法类型
     */
    AlgorithmType algorithm() default AlgorithmType.TOKEN_BUCKET;

    /**
     * 限流引擎类型
     */
    EngineType engine() default EngineType.AUTO;

    /**
     * Key前缀
     */
    String keyPrefix() default "";

    /**
     * 最大突发容量
     */
    int maxBurst() default 0;

    /**
     * 自定义Key构建器
     */
    Class<? extends KeyBuilder> keyBuilder() default KeyBuilder.class;

    /**
     * 降级处理器
     */
    Class<? extends FallbackHandler> fallbackHandler() default FallbackHandler.class;

    /**
     * 异常类型
     */
    Class<? extends RuntimeException> exceptionClass() default RateLimitException.class;

    /**
     * 是否启用
     */
    boolean enabled() default true;
}