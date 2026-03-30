package com.geek.ratelimit4j.starter.resolver;

import com.geek.ratelimit4j.core.config.DimensionType;
import com.geek.ratelimit4j.core.resolver.DimensionResolverRegistry;
import com.geek.ratelimit4j.core.resolver.RateLimitResolveContext;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 维度Key解析器
 * 使用预定义维度（IP、USER、TENANT、DEVICE等）解析限流Key
 *
 * <p>优先级：3</p>
 * <p>触发条件：rateLimit.dimension() != DimensionType.METHOD</p>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class DimensionRateLimitKeyResolver implements RateLimitKeyResolver {

    private final DimensionResolverRegistry dimensionResolverRegistry;

    public DimensionRateLimitKeyResolver(DimensionResolverRegistry dimensionResolverRegistry) {
        this.dimensionResolverRegistry = Objects.requireNonNull(dimensionResolverRegistry, 
                "dimensionResolverRegistry must not be null");
    }

    @Override
    public boolean supports(RateLimitResolveContext context) {
        return context.hasValidDimension();
    }

    @Override
    public String resolve(RateLimitResolveContext context) {
        DimensionType dimension = context.getDimensionType();
        String keyPrefix = context.getKeyPrefix();

        DefaultDimensionResolveContext dimensionContext = new DefaultDimensionResolveContext(
                context.getNativeJoinPoint());

        String dimensionKey = dimensionResolverRegistry.resolve(dimension, dimensionContext);

        if (StringUtils.isBlank(dimensionKey)) {
            throw new IllegalStateException(
                    "Dimension resolver returned empty key for dimension: " + dimension.getCode());
        }

        StringBuilder fullKey = new StringBuilder();

        if (StringUtils.isNotBlank(keyPrefix)) {
            fullKey.append(keyPrefix).append(":");
        }

        fullKey.append(dimension.getCode()).append(":");
        fullKey.append(dimensionKey);

        return fullKey.toString();
    }

    @Override
    public int getOrder() {
        return 3;
    }
}