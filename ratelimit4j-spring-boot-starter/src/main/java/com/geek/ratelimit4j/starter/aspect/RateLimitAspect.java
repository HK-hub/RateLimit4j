package com.geek.ratelimit4j.starter.aspect;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import com.geek.ratelimit4j.core.algorithm.RateLimitAlgorithm;
import com.geek.ratelimit4j.core.config.RateLimitContext;
import com.geek.ratelimit4j.core.algorithm.RateLimitResult;
import com.geek.ratelimit4j.core.config.ModeType;
import com.geek.ratelimit4j.core.config.RateLimitConfig;
import com.geek.ratelimit4j.core.exception.RateLimitException;
import com.geek.ratelimit4j.core.telemetry.RateLimitTelemetry;
import com.geek.ratelimit4j.local.algorithm.*;
import com.geek.ratelimit4j.starter.annotation.RateLimit;
import com.geek.ratelimit4j.starter.handler.FallbackHandler;
import com.geek.ratelimit4j.starter.resolver.KeyBuilder;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流切面
 * 拦截带有@RateLimit注解的方法，执行限流判断
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Aspect
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private final Map<AlgorithmType, RateLimitAlgorithm> algorithmCache = new ConcurrentHashMap<>();
    private final ApplicationContext applicationContext;
    private final RateLimitTelemetry telemetry;

    public RateLimitAspect(
            LocalTokenBucketAlgorithm tokenBucket,
            LocalLeakyBucketAlgorithm leakyBucket,
            LocalFixedWindowAlgorithm fixedWindow,
            LocalSlidingWindowLogAlgorithm slidingWindowLog,
            LocalSlidingWindowCounterAlgorithm slidingWindowCounter,
            ApplicationContext applicationContext,
            RateLimitTelemetry telemetry) {

        this.applicationContext = applicationContext;
        this.telemetry = telemetry;

        if (Objects.nonNull(tokenBucket)) {
            algorithmCache.put(AlgorithmType.TOKEN_BUCKET, tokenBucket);
        }
        if (Objects.nonNull(leakyBucket)) {
            algorithmCache.put(AlgorithmType.LEAKY_BUCKET, leakyBucket);
        }
        if (Objects.nonNull(fixedWindow)) {
            algorithmCache.put(AlgorithmType.FIXED_WINDOW, fixedWindow);
        }
        if (Objects.nonNull(slidingWindowLog)) {
            algorithmCache.put(AlgorithmType.SLIDING_WINDOW_LOG, slidingWindowLog);
        }
        if (Objects.nonNull(slidingWindowCounter)) {
            algorithmCache.put(AlgorithmType.SLIDING_WINDOW_COUNTER, slidingWindowCounter);
        }
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        if (BooleanUtils.isFalse(rateLimit.enabled())) {
            return joinPoint.proceed();
        }

        String key = resolveKey(joinPoint, rateLimit);

        RateLimitAlgorithm algorithm = algorithmCache.get(rateLimit.algorithm());
        if (Objects.isNull(algorithm)) {
            log.warn("No algorithm found for type: {}", rateLimit.algorithm());
            return joinPoint.proceed();
        }

        RateLimitConfig config = buildConfig(rateLimit);
        RateLimitContext context = RateLimitContext.of(key, config, ModeType.LOCAL);

        RateLimitResult result = algorithm.evaluate(context);

        recordTelemetry(result, key, rateLimit.algorithm());

        if (result.isAllowed()) {
            return joinPoint.proceed();
        }

        return handleRejection(joinPoint, rateLimit, key, result);
    }

    private String resolveKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        KeyBuilder keyBuilder = getKeyBuilder(rateLimit.keyBuilder());
        String[] keys = rateLimit.keys();
        String keyPrefix = rateLimit.keyPrefix();

        if (Objects.isNull(keys) || keys.length == 0) {
            String defaultKey = keyBuilder.build(joinPoint, "");
            return StringUtils.isNotBlank(keyPrefix) ? keyPrefix + ":" + defaultKey : defaultKey;
        }

        StringBuilder combinedKey = new StringBuilder();
        if (StringUtils.isNotBlank(keyPrefix)) {
            combinedKey.append(keyPrefix).append(":");
        }

        for (int i = 0; i < keys.length; i++) {
            String resolvedKey = keyBuilder.build(joinPoint, keys[i]);
            combinedKey.append(resolvedKey);
            if (i < keys.length - 1) {
                combinedKey.append(":");
            }
        }

        return combinedKey.toString();
    }

    private KeyBuilder getKeyBuilder(Class<? extends KeyBuilder> keyBuilderClass) {
        try {
            Map<String, KeyBuilder> builders = applicationContext.getBeansOfType(KeyBuilder.class);
            for (KeyBuilder builder : builders.values()) {
                if (builder.getClass().equals(keyBuilderClass)) {
                    return builder;
                }
            }
            return builders.values().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No KeyBuilder found"));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get KeyBuilder: " + keyBuilderClass.getName(), e);
        }
    }

    private FallbackHandler getFallbackHandler(Class<? extends FallbackHandler> handlerClass) {
        try {
            Map<String, FallbackHandler> handlers = applicationContext.getBeansOfType(FallbackHandler.class);
            for (FallbackHandler handler : handlers.values()) {
                if (handler.getClass().equals(handlerClass)) {
                    return handler;
                }
            }
            return handlers.values().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No FallbackHandler found"));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get FallbackHandler: " + handlerClass.getName(), e);
        }
    }

    private RateLimitConfig buildConfig(RateLimit rateLimit) {
        return RateLimitConfig.builder()
                .name("annotation-based")
                .algorithmType(rateLimit.algorithm())
                .rate(rateLimit.rate())
                .period(rateLimit.period())
                .keyPrefix(rateLimit.keyPrefix())
                .maxBurst(rateLimit.maxBurst() > 0 ? rateLimit.maxBurst() : rateLimit.rate())
                .build();
    }

    private Object handleRejection(ProceedingJoinPoint joinPoint, RateLimit rateLimit,
                                    String key, RateLimitResult result) {
        log.debug("Rate limit rejected for key: {}, wait time: {}ms", key, result.getWaitTimeMs());

        RuntimeException exception = createRateLimitException(rateLimit, key, result);
        FallbackHandler fallbackHandler = getFallbackHandler(rateLimit.fallbackHandler());

        if (exception instanceof RateLimitException) {
            return fallbackHandler.handle(joinPoint, (RateLimitException) exception);
        }
        return fallbackHandler.handle(joinPoint, new RateLimitException(exception.getMessage()));
    }

    private RuntimeException createRateLimitException(RateLimit rateLimit, String key,
                                                         RateLimitResult result) {
        String message = String.format("Rate limit exceeded: key=%s, algorithm=%s, rate=%d/s, wait=%dms",
                key, rateLimit.algorithm().getCode(), rateLimit.rate(), result.getWaitTimeMs());

        Class<? extends RuntimeException> exceptionClass = rateLimit.exceptionClass();

        if (Objects.equals(exceptionClass, RateLimitException.class)) {
            return new RateLimitException(message, key, rateLimit.algorithm().getCode(),
                    rateLimit.rate(), result.getWaitTimeMs());
        }

        try {
            return exceptionClass.getConstructor(String.class)
                    .newInstance(message);
        } catch (Exception e) {
            return new RateLimitException(message, key, rateLimit.algorithm().getCode(),
                    rateLimit.rate(), result.getWaitTimeMs());
        }
    }

    private void recordTelemetry(RateLimitResult result, String key, AlgorithmType algorithmType) {
        if (Objects.isNull(telemetry) || BooleanUtils.isFalse(telemetry.isEnabled())) {
            return;
        }

        if (result.isAllowed()) {
            telemetry.recordAllowed(key, algorithmType, 1, result.getRemainingPermits());
        } else {
            telemetry.recordRejected(key, algorithmType, 1, result.getWaitTimeMs());
        }
    }
}