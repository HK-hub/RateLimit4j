package com.geek.ratelimit4j.starter.resolver;

import com.geek.ratelimit4j.core.resolver.DimensionResolveContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 默认维度解析上下文
 * 基于AOP切点和HttpServletRequest实现
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class DefaultDimensionResolveContext implements DimensionResolveContext {

    @Getter
    private final HttpServletRequest request;

    private final ProceedingJoinPoint joinPoint;

    private final MethodSignature signature;

    private final Map<String, Object> attributes = new HashMap<>();

    public DefaultDimensionResolveContext(ProceedingJoinPoint joinPoint) {
        this.joinPoint = joinPoint;
        this.signature = (MethodSignature) joinPoint.getSignature();
        this.request = findRequest(joinPoint);
    }

    @Override
    public String getHeader(String name) {
        return Objects.nonNull(request) ? request.getHeader(name) : null;
    }

    @Override
    public String getRemoteAddr() {
        return Objects.nonNull(request) ? request.getRemoteAddr() : null;
    }

    @Override
    public Object[] getMethodArgs() {
        return joinPoint.getArgs();
    }

    @Override
    public String[] getMethodParameterNames() {
        Parameter[] parameters = signature.getMethod().getParameters();
        String[] names = new String[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            names[i] = parameters[i].getName();
        }
        return names;
    }

    @Override
    public Class<?>[] getMethodParameterTypes() {
        return signature.getMethod().getParameterTypes();
    }

    @Override
    public String getDeclaringClassName() {
        return signature.getMethod().getDeclaringClass().getName();
    }

    @Override
    public String getMethodName() {
        return signature.getMethod().getName();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getNativeRequest() {
        return (T) request;
    }

    @Override
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    @Override
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    private HttpServletRequest findRequest(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (Objects.isNull(args)) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof HttpServletRequest) {
                return (HttpServletRequest) arg;
            }
        }
        return null;
    }
}