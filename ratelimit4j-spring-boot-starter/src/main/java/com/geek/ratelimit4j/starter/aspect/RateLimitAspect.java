package com.geek.ratelimit4j.starter.aspect;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import com.geek.ratelimit4j.core.algorithm.RateLimitAlgorithm;
import com.geek.ratelimit4j.core.algorithm.RateLimitContext;
import com.geek.ratelimit4j.core.algorithm.RateLimitResult;
import com.geek.ratelimit4j.core.config.ModeType;
import com.geek.ratelimit4j.core.config.RateLimitConfig;
import com.geek.ratelimit4j.core.exception.RateLimitException;
import com.geek.ratelimit4j.core.exception.RateLimitFallbackException;
import com.geek.ratelimit4j.core.telemetry.RateLimitTelemetry;
import com.geek.ratelimit4j.local.algorithm.*;
import com.geek.ratelimit4j.starter.annotation.RateLimit;
import com.geek.ratelimit4j.starter.resolver.KeyResolver;
import com.geek.ratelimit4j.starter.resolver.SpelKeyResolver;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流切面
 * 拦截带有@RateLimit注解的方法，执行限流判断
 *
 * <p>功能：</p>
 * <ul>
 *   <li>解析@RateLimit注解配置</li>
 *   <li>使用SpEL解析限流Key</li>
 *   <li>调用限流算法执行限流判断</li>
 *   <li>处理限流结果（降级或抛出异常）</li>
 *   <li>记录监控指标</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Aspect
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    /**
     * 本地算法实例缓存
     */
    private final ConcurrentHashMap<AlgorithmType, RateLimitAlgorithm> algorithmCache = new ConcurrentHashMap<>();

    /**
     * Key解析器
     */
    private final KeyResolver keyResolver;

    /**
     * 监控追踪器（可选）
     */
    private final RateLimitTelemetry telemetry;

    /**
     * 构造限流切面
     *
     * @param tokenBucket 令牌桶算法
     * @param leakyBucket 漏桶算法
     * @param fixedWindow 固定窗口算法
     * @param slidingWindowLog 滑动窗口日志算法
     * @param slidingWindowCounter 滑动窗口计数器算法
     * @param telemetry 监控追踪器
     */
    public RateLimitAspect(
            LocalTokenBucketAlgorithm tokenBucket,
            LocalLeakyBucketAlgorithm leakyBucket,
            LocalFixedWindowAlgorithm fixedWindow,
            LocalSlidingWindowLogAlgorithm slidingWindowLog,
            LocalSlidingWindowCounterAlgorithm slidingWindowCounter,
            RateLimitTelemetry telemetry) {
        
        this.keyResolver = new SpelKeyResolver();
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

    /**
     * 简化构造函数（仅使用令牌桶）
     */
    public RateLimitAspect(LocalTokenBucketAlgorithm tokenBucket) {
        this(tokenBucket, null, null, null, null, null);
    }

    /**
     * 环绕通知：拦截@RateLimit注解的方法
     *
     * @param joinPoint 切点
     * @param rateLimit 限流注解
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        if (!rateLimit.enabled()) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        String key = resolveKey(method, args, rateLimit);

        RateLimitAlgorithm algorithm = getAlgorithm(rateLimit.algorithm());
        if (Objects.isNull(algorithm)) {
            log.warn("No algorithm found for type: {}, proceeding without rate limit", rateLimit.algorithm());
            return joinPoint.proceed();
        }

        RateLimitConfig config = buildConfig(rateLimit);
        RateLimitContext context = RateLimitContext.of(key, config, ModeType.LOCAL);

        RateLimitResult result = algorithm.evaluate(context);

        if (telemetry != null && telemetry.isEnabled()) {
            telemetry.recordEvent(createTelemetryEvent(result, key, rateLimit.algorithm()));
        }

        if (result.isAllowed()) {
            return joinPoint.proceed();
        } else {
            return handleRejection(joinPoint, rateLimit, key, result);
        }
    }

    /**
     * 解析限流Key
     */
    private String resolveKey(Method method, Object[] args, RateLimit rateLimit) {
        return keyResolver.resolve(method, args, rateLimit.keyExpression(), rateLimit.keyPrefix());
    }

    /**
     * 获取限流算法
     */
    private RateLimitAlgorithm getAlgorithm(AlgorithmType type) {
        return algorithmCache.get(type);
    }

    /**
     * 构建限流配置
     */
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

    /**
     * 处理限流拒绝
     */
    private Object handleRejection(ProceedingJoinPoint joinPoint, RateLimit rateLimit,
                                    String key, RateLimitResult result) throws Throwable {
        log.debug("Rate limit rejected for key: {}, wait time: {}ms", key, result.getWaitTimeMs());

        if (StringUtils.isNotBlank(rateLimit.fallbackMethod())) {
            return executeFallback(joinPoint, rateLimit, key);
        }

        throw createRateLimitException(rateLimit, key, result);
    }

    /**
     * 执行降级方法
     */
    private Object executeFallback(ProceedingJoinPoint joinPoint, RateLimit rateLimit,
                                    String key) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object target = joinPoint.getTarget();
        Object[] args = joinPoint.getArgs();

        try {
            Method fallbackMethod = findFallbackMethod(target, rateLimit.fallbackMethod(), method);
            if (Objects.nonNull(fallbackMethod)) {
                fallbackMethod.setAccessible(true);
                return fallbackMethod.invoke(target, args);
            } else {
                throw new RateLimitFallbackException(rateLimit.fallbackMethod(),
                        new RateLimitException(key));
            }
        } catch (Exception e) {
            throw new RateLimitFallbackException("Fallback execution failed: " + e.getMessage(),
                    new RateLimitException(key), rateLimit.fallbackMethod());
        }
    }

    /**
     * 查找降级方法
     */
    private Method findFallbackMethod(Object target, String fallbackMethodName, Method originalMethod) {
        Class<?> targetClass = target.getClass();
        Class<?>[] parameterTypes = originalMethod.getParameterTypes();

        try {
            return targetClass.getDeclaredMethod(fallbackMethodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * 创建限流异常
     */
    private RuntimeException createRateLimitException(RateLimit rateLimit, String key,
                                                         RateLimitResult result) {
        Class<? extends RuntimeException> exceptionClass = rateLimit.exceptionClass();
        
        if (Objects.equals(exceptionClass, RateLimitException.class)) {
            String message = String.format("Rate limit exceeded: key=%s, algorithm=%s, rate=%d/s, wait=%dms",
                    key, rateLimit.algorithm().getCode(), rateLimit.rate(), result.getWaitTimeMs());
            return new RateLimitException(message, key, rateLimit.algorithm().getCode(),
                    rateLimit.rate(), result.getWaitTimeMs());
        }

        try {
            return exceptionClass.getConstructor(String.class)
                    .newInstance("Rate limit exceeded for key: " + key);
        } catch (Exception e) {
            String message = String.format("Rate limit exceeded: key=%s, algorithm=%s, rate=%d/s, wait=%dms",
                    key, rateLimit.algorithm().getCode(), rateLimit.rate(), result.getWaitTimeMs());
            return new RateLimitException(message, key, rateLimit.algorithm().getCode(),
                    rateLimit.rate(), result.getWaitTimeMs());
        }
    }

    /**
     * 创建监控事件
     */
    private com.geek.ratelimit4j.core.telemetry.TelemetryEvent createTelemetryEvent(
            RateLimitResult result, String key, AlgorithmType algorithmType) {
        if (result.isAllowed()) {
            return com.geek.ratelimit4j.core.telemetry.TelemetryEvent.allowed(
                    key, algorithmType, 1, result.getRemainingPermits(),
                    result.getExecutionTimeMs(), "local");
        } else {
            return com.geek.ratelimit4j.core.telemetry.TelemetryEvent.rejected(
                    key, algorithmType, result.getWaitTimeMs(), 1,
                    result.getExecutionTimeMs(), "local");
        }
    }
}