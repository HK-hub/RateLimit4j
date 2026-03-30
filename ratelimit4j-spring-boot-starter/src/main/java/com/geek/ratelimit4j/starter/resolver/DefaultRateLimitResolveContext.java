package com.geek.ratelimit4j.starter.resolver;

import com.geek.ratelimit4j.core.config.DimensionType;
import com.geek.ratelimit4j.core.resolver.KeyBuilder;
import com.geek.ratelimit4j.core.resolver.RateLimitResolveContext;
import com.geek.ratelimit4j.core.annotation.RateLimit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 默认限流Key解析上下文实现
 * 基于AspectJ切点和RateLimit注解构建解析上下文
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class DefaultRateLimitResolveContext implements RateLimitResolveContext {

    private final ProceedingJoinPoint joinPoint;
    private final RateLimit rateLimit;
    private final Map<String, Object> attributes = new HashMap<>();

    public DefaultRateLimitResolveContext(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        this.joinPoint = Objects.requireNonNull(joinPoint, "joinPoint must not be null");
        this.rateLimit = Objects.requireNonNull(rateLimit, "rateLimit must not be null");
    }

    @Override
    public String getKeyPrefix() {
        return rateLimit.keyPrefix();
    }

    @Override
    public Method getMethod() {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod();
    }

    @Override
    public Object[] getMethodArgs() {
        return joinPoint.getArgs();
    }

    @Override
    public String[] getMethodParameterNames() {
        Method method = getMethod();
        Parameter[] parameters = method.getParameters();
        String[] names = new String[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            names[i] = parameters[i].getName();
        }
        return names;
    }

    @Override
    public String getDeclaringClassName() {
        return getMethod().getDeclaringClass().getSimpleName();
    }

    @Override
    public String getMethodName() {
        return getMethod().getName();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getNativeJoinPoint() {
        return (T) joinPoint;
    }

    @Override
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    @Override
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public DimensionType getDimensionType() {
        return rateLimit.dimension();
    }

    @Override
    public Class<?> getKeyBuilderClass() {
        Class<? extends KeyBuilder> clazz = rateLimit.keyBuilder();
        return (clazz == KeyBuilder.class) ? null : clazz;
    }

    @Override
    public String[] getKeys() {
        return rateLimit.keys();
    }

    /**
     * 获取RateLimit注解实例
     *
     * @return RateLimit注解实例
     */
    public RateLimit getRateLimit() {
        return rateLimit;
    }
}