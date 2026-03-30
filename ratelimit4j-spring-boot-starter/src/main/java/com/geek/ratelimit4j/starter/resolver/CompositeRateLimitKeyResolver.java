package com.geek.ratelimit4j.starter.resolver;

import com.geek.ratelimit4j.core.resolver.RateLimitResolveContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/**
 * 组合Key解析器
 * 按固定优先级顺序调用各Key解析器，返回第一个支持的解析器结果
 *
 * <p>固定优先级顺序（不可修改）：</p>
 * <ol>
 *   <li>BuilderRateLimitKeyResolver - 自定义KeyBuilder</li>
 *   <li>SpelRateLimitKeyResolver - SpEL表达式</li>
 *   <li>DimensionRateLimitKeyResolver - 预定义维度</li>
 *   <li>MethodRateLimitKeyResolver - 方法全限定名（兜底）</li>
 * </ol>
 *
 * <p>设计说明：</p>
 * <ul>
 *   <li>使用固定顺序，不依赖order值排序，避免用户修改order导致解析错误</li>
 *   <li>每个解析器的supports方法判断是否支持当前上下文</li>
 *   <li>第一个支持的解析器处理并返回结果</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Slf4j
public class CompositeRateLimitKeyResolver implements RateLimitKeyResolver {

    // ==================== 固定优先级的解析器列表 ====================

    /**
     * KeyBuilder解析器（优先级1）
     */
    private final BuilderRateLimitKeyResolver builderResolver;

    /**
     * SpEL解析器（优先级2）
     */
    private final SpelRateLimitKeyResolver spelResolver;

    /**
     * 维度解析器（优先级3）
     */
    private final DimensionRateLimitKeyResolver dimensionResolver;

    /**
     * 方法名解析器（优先级4，兜底）
     */
    private final MethodRateLimitKeyResolver methodResolver;

    // ==================== 构造函数 ====================

    /**
     * 构造组合Key解析器
     *
     * <p>从解析器列表中按类型提取各解析器，确保固定顺序</p>
     *
     * @param resolvers 所有Key解析器列表
     * @throws IllegalArgumentException 当缺少必需的解析器时抛出
     */
    public CompositeRateLimitKeyResolver(List<RateLimitKeyResolver> resolvers) {
        Objects.requireNonNull(resolvers, "resolvers must not be null");

        // 按类型提取各解析器
        this.builderResolver = findResolver(resolvers, BuilderRateLimitKeyResolver.class);
        this.spelResolver = findResolver(resolvers, SpelRateLimitKeyResolver.class);
        this.dimensionResolver = findResolver(resolvers, DimensionRateLimitKeyResolver.class);
        this.methodResolver = findResolver(resolvers, MethodRateLimitKeyResolver.class);

        // 校验必需的解析器
        if (builderResolver == null) {
            throw new IllegalArgumentException("BuilderRateLimitKeyResolver not found");
        }
        if (spelResolver == null) {
            throw new IllegalArgumentException("SpelRateLimitKeyResolver not found");
        }
        if (dimensionResolver == null) {
            throw new IllegalArgumentException("DimensionRateLimitKeyResolver not found");
        }
        if (methodResolver == null) {
            throw new IllegalArgumentException("MethodRateLimitKeyResolver not found");
        }

        log.info("[RateLimit4j] CompositeRateLimitKeyResolver initialized with fixed order: "
                + "Builder -> SpEL -> Dimension -> Method");
    }

    // ==================== 接口方法实现 ====================

    /**
     * {@inheritDoc}
     * 始终返回true（因为有MethodRateLimitKeyResolver兜底）
     */
    @Override
    public boolean supports(RateLimitResolveContext context) {
        return true;
    }

    /**
     * {@inheritDoc}
     * 按固定优先级顺序解析Key
     *
     * <p>解析顺序：</p>
     * <ol>
     *   <li>检查keyBuilder是否配置，有则使用BuilderResolver</li>
     *   <li>检查keys是否配置，有则使用SpELResolver</li>
     *   <li>检查dimension是否非METHOD，有则使用DimensionResolver</li>
     *   <li>使用MethodResolver生成默认Key</li>
     * </ol>
     *
     * @param context 解析上下文
     * @return 解析后的Key
     */
    @Override
    public String resolve(RateLimitResolveContext context) {
        // 优先级1：keyBuilder
        if (builderResolver.supports(context)) {
            String key = builderResolver.resolve(context);
            log.debug("[RateLimit4j] Key resolved by BuilderResolver: {}", key);
            return key;
        }

        // 优先级2：keys（SpEL表达式）
        if (spelResolver.supports(context)) {
            String key = spelResolver.resolve(context);
            log.debug("[RateLimit4j] Key resolved by SpELResolver: {}", key);
            return key;
        }

        // 优先级3：dimension（维度）
        if (dimensionResolver.supports(context)) {
            String key = dimensionResolver.resolve(context);
            log.debug("[RateLimit4j] Key resolved by DimensionResolver: {}", key);
            return key;
        }

        // 优先级4：method（默认兜底）
        String key = methodResolver.resolve(context);
        log.debug("[RateLimit4j] Key resolved by MethodResolver: {}", key);
        return key;
    }

    /**
     * {@inheritDoc}
     * 返回0（组合解析器本身无优先级概念，使用固定顺序）
     */
    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getName() {
        return "CompositeRateLimitKeyResolver";
    }

    // ==================== 私有方法 ====================

    /**
     * 从解析器列表中查找指定类型的解析器
     *
     * @param resolvers  解析器列表
     * @param resolverClass 目标解析器类型
     * @param <T> 解析器类型
     * @return 找到的解析器，未找到返回null
     */
    @SuppressWarnings("unchecked")
    private <T extends RateLimitKeyResolver> T findResolver(
            List<RateLimitKeyResolver> resolvers, Class<T> resolverClass) {
        for (RateLimitKeyResolver resolver : resolvers) {
            if (resolverClass.isInstance(resolver)) {
                return (T) resolver;
            }
        }
        return null;
    }
}