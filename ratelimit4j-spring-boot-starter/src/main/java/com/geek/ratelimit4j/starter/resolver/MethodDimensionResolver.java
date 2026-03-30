package com.geek.ratelimit4j.starter.resolver;

import com.geek.ratelimit4j.core.config.DimensionType;
import com.geek.ratelimit4j.core.resolver.DimensionResolveContext;
import com.geek.ratelimit4j.core.resolver.DimensionResolver;

/**
 * 方法维度解析器
 * 返回方法全限定名作为维度值
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class MethodDimensionResolver implements DimensionResolver {

    @Override
    public DimensionType getType() {
        return DimensionType.METHOD;
    }

    @Override
    public String resolve(DimensionResolveContext context) {
        String className = context.getDeclaringClassName();
        String methodName = context.getMethodName();
        return className + "#" + methodName;
    }
}