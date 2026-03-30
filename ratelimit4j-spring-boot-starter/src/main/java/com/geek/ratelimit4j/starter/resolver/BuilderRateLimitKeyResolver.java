package com.geek.ratelimit4j.starter.resolver;

import com.geek.ratelimit4j.core.resolver.KeyBuilder;
import com.geek.ratelimit4j.core.resolver.RateLimitResolveContext;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.util.Objects;

/**
 * KeyBuilder方式Key解析器
 * 使用自定义KeyBuilder构建限流Key
 *
 * <p>优先级：1（最高）</p>
 * <p>触发条件：rateLimit.keyBuilder() != KeyBuilder.class</p>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class BuilderRateLimitKeyResolver implements RateLimitKeyResolver {

    private final ApplicationContext applicationContext;

    public BuilderRateLimitKeyResolver(ApplicationContext applicationContext) {
        this.applicationContext = Objects.requireNonNull(applicationContext, 
                "applicationContext must not be null");
    }

    @Override
    public boolean supports(RateLimitResolveContext context) {
        return context.hasKeyBuilder();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String resolve(RateLimitResolveContext context) {
        Class<?> keyBuilderClass = context.getKeyBuilderClass();
        String keyPrefix = context.getKeyPrefix();
        ProceedingJoinPoint joinPoint = context.getNativeJoinPoint();

        KeyBuilder builder;
        try {
            builder = (KeyBuilder) applicationContext.getBean(keyBuilderClass);
        } catch (BeansException e) {
            throw new IllegalStateException("KeyBuilder bean not found: " + keyBuilderClass.getName(), e);
        }

        String builtKey = builder.build(joinPoint);

        if (StringUtils.isBlank(builtKey)) {
            throw new IllegalStateException("KeyBuilder '" + builder.getClass().getName() + "' returned empty key");
        }

        if (StringUtils.isNotBlank(keyPrefix)) {
            return keyPrefix + ":" + builtKey;
        }
        return builtKey;
    }

    @Override
    public int getOrder() {
        return 1;
    }
}