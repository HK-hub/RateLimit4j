package com.geek.ratelimit4j.core.resolver;

import com.geek.ratelimit4j.core.config.DimensionType;

import java.lang.reflect.Method;

/**
 * 限流Key解析上下文接口
 * 提供Key解析所需的请求信息
 *
 * <p>设计说明：</p>
 * <ul>
 *   <li>接口设计，不依赖具体框架</li>
 *   <li>支持在不同环境中使用</li>
 *   <li>用户可实现此接口适配不同的请求上下文</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public interface RateLimitResolveContext {

    /**
     * 获取Key前缀
     *
     * @return Key前缀，可能为空
     */
    String getKeyPrefix();

    /**
     * 获取方法对象
     *
     * @return 方法对象
     */
    Method getMethod();

    /**
     * 获取方法参数
     *
     * @return 方法参数数组
     */
    Object[] getMethodArgs();

    /**
     * 获取方法参数名
     *
     * @return 方法参数名数组
     */
    String[] getMethodParameterNames();

    /**
     * 获取声明方法的类名
     *
     * @return 类名
     */
    String getDeclaringClassName();

    /**
     * 获取方法名
     *
     * @return 方法名
     */
    String getMethodName();

    /**
     * 获取原生切点对象
     *
     * @param <T> 切点对象类型
     * @return 原生切点对象
     */
    <T> T getNativeJoinPoint();

    /**
     * 获取属性
     *
     * @param key 属性名
     * @return 属性值
     */
    Object getAttribute(String key);

    /**
     * 设置属性
     *
     * @param key   属性名
     * @param value 属性值
     */
    void setAttribute(String key, Object value);

    // ==================== 限流配置获取方法 ====================

    /**
     * 获取限流维度类型
     *
     * @return 维度类型
     */
    DimensionType getDimensionType();

    /**
     * 获取Key构建器类型
     *
     * @return Key构建器类型，未配置返回null
     */
    Class<?> getKeyBuilderClass();

    /**
     * 获取SpEL表达式Key数组
     *
     * @return SpEL表达式数组，未配置返回null或空数组
     */
    String[] getKeys();

    /**
     * 判断是否配置了有效的KeyBuilder
     *
     * @return true表示配置了有效的KeyBuilder
     */
    default boolean hasKeyBuilder() {
        Class<?> clazz = getKeyBuilderClass();
        return clazz != null && clazz != Void.class;
    }

    /**
     * 判断是否配置了Keys表达式
     *
     * @return true表示配置了Keys表达式
     */
    default boolean hasKeys() {
        String[] keys = getKeys();
        return keys != null && keys.length > 0;
    }

    /**
     * 判断是否配置了有效的维度（非METHOD）
     *
     * @return true表示配置了有效维度
     */
    default boolean hasValidDimension() {
        DimensionType type = getDimensionType();
        return type != null && type != DimensionType.METHOD;
    }
}