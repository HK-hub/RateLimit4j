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
 * 设备维度限流注解
 * 基于设备ID进行限流，适用于移动端、IoT等场景
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 每个设备每秒最多10次请求
 * @DeviceRateLimit(rate = 10, period = 1)
 * public String api(@Header("X-Device-Id") String deviceId) { ... }
 *
 * // 每个设备每分钟最多100次请求
 * @DeviceRateLimit(rate = 100, period = 60)
 * public String api(@Header("X-Device-Id") String deviceId) { ... }
 * }</pre>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@RateLimit(dimension = DimensionType.DEVICE)
public @interface DeviceRateLimit {

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