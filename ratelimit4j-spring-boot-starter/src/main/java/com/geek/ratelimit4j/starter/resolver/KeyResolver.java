package com.geek.ratelimit4j.starter.resolver;

import java.lang.reflect.Method;

/**
 * 限流Key解析器接口
 * 用于解析限流Key的不同维度
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public interface KeyResolver {

    /**
     * 解析限流Key
     *
     * @param method 目标方法
     * @param args 方法参数
     * @param keyExpression Key表达式
     * @param keyPrefix Key前缀
     * @return 解析后的限流Key
     */
    String resolve(Method method, Object[] args, String keyExpression, String keyPrefix);

    /**
     * 获取解析器名称
     *
     * @return 解析器名称
     */
    String getName();
}