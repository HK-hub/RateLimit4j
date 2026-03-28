package com.geek.ratelimit4j.starter.annotation;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解
 * 用于标注需要限流的方法或类，支持声明式限流配置
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @RateLimit(rate = 100, period = 1, algorithm = AlgorithmType.TOKEN_BUCKET)
 * public String apiEndpoint() {
 *     return "Hello World";
 * }
 *
 * @RateLimit(keyExpression = "#user.id", rate = 50, fallbackMethod = "fallback")
 * public String userApi(User user) {
 *     return "Success";
 * }
 *
 * public String fallback(User user) {
 *     return "Rate limited";
 * }
 * }</pre>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流算法类型
     *
     * @return 算法类型，默认令牌桶
     */
    AlgorithmType algorithm() default AlgorithmType.TOKEN_BUCKET;

    /**
     * 每周期允许的请求数
     *
     * @return 限流速率，默认100
     */
    int rate() default 100;

    /**
     * 限流周期（秒）
     *
     * @return 周期秒数，默认1秒
     */
    int period() default 1;

    /**
     * 限流Key前缀
     *
     * @return Key前缀
     */
    String keyPrefix() default "";

    /**
     * 限流Key表达式（SpEL）
     * 支持从方法参数中提取用户ID、IP等维度
     *
     * <p>表达式示例：</p>
     * <ul>
     *   <li>#user.id - 提取用户ID</li>
     *   <li>#request.getRemoteAddr() - 提取IP地址</li>
     *   <li>#request.getHeader('X-Token') - 提取Header</li>
     *   <li>#user.id + ':' + #method - 组合Key</li>
     * </ul>
     *
     * @return SpEL表达式
     */
    String keyExpression() default "";

    /**
     * 降级方法名称
     * 被限流时执行此方法，方法签名需与原方法一致
     *
     * @return 降级方法名
     */
    String fallbackMethod() default "";

    /**
     * 自定义限流异常类型
     *
     * @return 异常类型
     */
    Class<? extends RuntimeException> exceptionClass() default 
            com.geek.ratelimit4j.core.exception.RateLimitException.class;

    /**
     * 最大突发容量（仅令牌桶算法）
     *
     * @return 最大突发数
     */
    int maxBurst() default 0;

    /**
     * 是否启用限流
     *
     * @return true表示启用
     */
    boolean enabled() default true;
}