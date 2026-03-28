package com.geek.ratelimit4j.starter.handler;

import com.geek.ratelimit4j.core.exception.RateLimitException;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.Objects;

/**
 * 默认降级处理器
 * 提供基础的限流降级处理逻辑
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class DefaultFallbackHandler implements FallbackHandler {

    @Override
    public Object handle(ProceedingJoinPoint joinPoint, RateLimitException exception) {
        throw exception;
    }

    @Override
    public String getName() {
        return "default";
    }
}