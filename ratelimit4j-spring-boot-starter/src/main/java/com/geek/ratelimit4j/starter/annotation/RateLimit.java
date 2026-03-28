package com.geek.ratelimit4j.starter.annotation;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import com.geek.ratelimit4j.starter.handler.DefaultFallbackHandler;
import com.geek.ratelimit4j.starter.handler.FallbackHandler;
import com.geek.ratelimit4j.starter.resolver.KeyBuilder;
import com.geek.ratelimit4j.starter.resolver.SpelKeyBuilder;

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
 * // 基础使用
 * @RateLimit(rate = 100, period = 1)
 * public String apiEndpoint() {
 *     return "Hello World";
 * }
 *
 * // 多Key维度限流（支持SpEL表达式）
 * @RateLimit(keys = {"#user.id", "#request.remoteAddr"}, rate = 50)
 * public String userApi(User user, HttpServletRequest request) {
 *     return "Success";
 * }
 *
 * // 自定义Key构建器
 * @RateLimit(keyBuilder = UserKeyBuilder.class, rate = 100)
 * public String customKeyApi(User user) {
 *     return "Success";
 * }
 *
 * // 自定义降级处理器
 * @RateLimit(rate = 10, fallbackHandler = CustomFallbackHandler.class)
 * public String apiWithFallback() {
 *     return "Success";
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
     * 限流Key数组（支持SpEL表达式）
     * 多个Key会被组合使用，支持从方法参数中提取用户ID、IP等维度
     *
     * <p>表达式示例：</p>
     * <ul>
     *   <li>#user.id - 提取用户ID</li>
     *   <li>#request.remoteAddr - 提取IP地址</li>
     *   <li>#request.getHeader('X-Token') - 提取Header</li>
     * </ul>
     *
     * @return Key表达式数组
     */
    String[] keys() default {};

    /**
     * 限流Key前缀
     *
     * @return Key前缀
     */
    String keyPrefix() default "";

    /**
     * 自定义Key构建器
     * 实现KeyBuilder接口并标注为@Component
     *
     * @return Key构建器类型
     */
    Class<? extends KeyBuilder> keyBuilder() default SpelKeyBuilder.class;

    /**
     * 自定义降级处理器
     * 实现FallbackHandler接口并标注为@Component
     *
     * @return 降级处理器类型
     */
    Class<? extends FallbackHandler> fallbackHandler() default DefaultFallbackHandler.class;

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