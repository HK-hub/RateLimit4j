package com.geek.ratelimit4j.starter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RateLimit容器注解
 * 支持在方法上使用多个@RateLimit注解
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @RateLimit(rate = 1, period = 3)
 * @RateLimit(rate = 10, period = 60)
 * public String apiMethod() {
 *     return "Success";
 * }
 * }</pre>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimits {
    
    /**
     * RateLimit注解数组
     *
     * @return RateLimit注解数组
     */
    RateLimit[] value();
}