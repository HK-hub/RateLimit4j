package com.geek.ratelimit4j.starter.resolver;

import com.geek.ratelimit4j.core.resolver.RateLimitResolveContext;

/**
 * 限流Key解析器接口
 * 用于解析限流Key的不同维度
 *
 * <p>优先级顺序（order值越小优先级越高）：</p>
 * <ol>
 *   <li>BuilderRateLimitKeyResolver (order=1) - keyBuilder属性</li>
 *   <li>SpelRateLimitKeyResolver (order=2) - keys属性（SpEL表达式）</li>
 *   <li>DimensionRateLimitKeyResolver (order=3) - dimension属性</li>
 *   <li>MethodRateLimitKeyResolver (order=4) - 默认方法名</li>
 * </ol>
 *
 * <p>设计说明：</p>
 * <ul>
 *   <li>每个解析器负责一种Key解析策略</li>
 *   <li>通过supports方法判断是否支持当前上下文</li>
 *   <li>支持用户自定义扩展，通过@ConditionalOnMissingBean替换默认实现</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public interface RateLimitKeyResolver {

    /**
     * 判断是否支持当前上下文
     *
     * @param context 解析上下文
     * @return true表示支持，会调用resolve方法
     */
    boolean supports(RateLimitResolveContext context);

    /**
     * 解析限流Key
     *
     * @param context 解析上下文
     * @return 解析后的限流Key，不应返回null
     */
    String resolve(RateLimitResolveContext context);

    /**
     * 获取解析器优先级
     * 值越小优先级越高
     *
     * @return 优先级值
     */
    int getOrder();

    /**
     * 获取解析器名称
     *
     * @return 解析器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}