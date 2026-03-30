package com.geek.ratelimit4j.core.resolver;

/**
 * 维度解析上下文接口
 * 提供维度解析所需的请求信息
 *
 * <p>设计说明：</p>
 * <ul>
 *   <li>接口设计，不依赖具体框架（如Servlet）</li>
 *   <li>支持在不同环境中使用（Web、RPC等）</li>
 *   <li>用户可实现此接口适配不同的请求上下文</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public interface DimensionResolveContext {

    /**
     * 获取指定名称的请求头
     *
     * @param name 请求头名称
     * @return 请求头值，不存在时返回null
     */
    String getHeader(String name);

    /**
     * 获取远程地址（客户端IP）
     *
     * @return 远程地址
     */
    String getRemoteAddr();

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
     * 获取方法参数类型
     *
     * @return 方法参数类型数组
     */
    Class<?>[] getMethodParameterTypes();

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
     * 获取原生请求对象
     * 用于扩展场景，如获取HttpServletRequest的更多属性
     *
     * @param <T> 请求对象类型
     * @return 原生请求对象
     */
    <T> T getNativeRequest();

    /**
     * 获取属性
     * 用于传递额外信息
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
}