package com.geek.ratelimit4j.starter.resolver;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;

/**
 * 默认Key解析器
 * 基于方法签名生成默认限流Key
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class DefaultKeyResolver implements com.geek.ratelimit4j.core.resolver.KeyResolver {

    @Override
    public String resolve(Method method, Object[] args, String keyExpression, String keyPrefix) {
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        
        StringBuilder keyBuilder = new StringBuilder();
        
        if (StringUtils.isNotBlank(keyPrefix)) {
            keyBuilder.append(keyPrefix).append(":");
        }
        
        keyBuilder.append(className).append(":").append(methodName);
        
        return keyBuilder.toString();
    }

    @Override
    public String getName() {
        return "default";
    }
}