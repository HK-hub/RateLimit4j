package com.geek.ratelimit4j.starter.aspect;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import com.geek.ratelimit4j.core.algorithm.RateLimitAlgorithm;
import com.geek.ratelimit4j.core.algorithm.RateLimitResult;
import com.geek.ratelimit4j.core.config.DimensionType;
import com.geek.ratelimit4j.core.config.EngineType;
import com.geek.ratelimit4j.core.config.ModeType;
import com.geek.ratelimit4j.core.config.RateLimitConfig;
import com.geek.ratelimit4j.core.config.RateLimitContext;
import com.geek.ratelimit4j.core.exception.RateLimitException;
import com.geek.ratelimit4j.core.telemetry.RateLimitTelemetry;
import com.geek.ratelimit4j.local.algorithm.*;
import com.geek.ratelimit4j.starter.annotation.DeviceRateLimit;
import com.geek.ratelimit4j.starter.annotation.IpRateLimit;
import com.geek.ratelimit4j.starter.annotation.RateLimit;
import com.geek.ratelimit4j.starter.annotation.RateLimits;
import com.geek.ratelimit4j.starter.annotation.TenantRateLimit;
import com.geek.ratelimit4j.starter.annotation.UserRateLimit;
import com.geek.ratelimit4j.starter.handler.FallbackHandler;
import com.geek.ratelimit4j.starter.resolver.DimensionKeyResolver;
import com.geek.ratelimit4j.starter.resolver.KeyBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流切面
 * 拦截带有@RateLimit注解的方法，执行限流判断
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>支持多个@RateLimit注解（多规则限流）</li>
 *   <li>支持SpEL表达式从方法参数提取Key</li>
 *   <li>支持引擎选择（LOCAL/REDIS）</li>
 *   <li>支持自定义降级处理器</li>
 *   <li>支持OpenTelemetry监控</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Getter
public class RateLimitAspect {

    // ==================== 成员变量 ====================

    /**
     * SpEL表达式解析器
     * 线程安全，全局单例即可
     */
    private final SpelExpressionParser spelParser = new SpelExpressionParser();

    /**
     * 本地算法缓存
     * Key: 算法类型, Value: 算法实现
     */
    private final Map<AlgorithmType, RateLimitAlgorithm> localAlgorithmCache = new ConcurrentHashMap<>();

    /**
     * Redis算法缓存
     * Key: 算法类型, Value: 算法实现
     */
    private final Map<AlgorithmType, RateLimitAlgorithm> redisAlgorithmCache = new ConcurrentHashMap<>();

    /**
     * Spring应用上下文
     * 用于获取FallbackHandler等Bean
     */
    private final org.springframework.context.ApplicationContext applicationContext;

    /**
     * 监控组件
     * 可选，用于记录限流指标
     */
    private final RateLimitTelemetry telemetry;

    /**
     * 主引擎类型
     * 当注解指定AUTO时使用此引擎
     */
    private final EngineType primaryEngine;

    /**
     * 维度Key解析器
     * 用于解析预定义维度的Key值
     */
    private final DimensionKeyResolver dimensionKeyResolver;

    // ==================== 构造函数 ====================

    /**
     * 构造限流切面
     *
     * @param tokenBucket        本地令牌桶算法
     * @param leakyBucket        本地漏桶算法
     * @param fixedWindow        本地固定窗口算法
     * @param slidingWindowLog   本地滑动窗口日志算法
     * @param slidingWindowCounter 本地滑动窗口计数器算法
     * @param applicationContext Spring应用上下文
     * @param telemetry          监控组件
     */
    public RateLimitAspect(
            LocalTokenBucketAlgorithm tokenBucket,
            LocalLeakyBucketAlgorithm leakyBucket,
            LocalFixedWindowAlgorithm fixedWindow,
            LocalSlidingWindowLogAlgorithm slidingWindowLog,
            LocalSlidingWindowCounterAlgorithm slidingWindowCounter,
            org.springframework.context.ApplicationContext applicationContext,
            RateLimitTelemetry telemetry) {
        // 初始化应用上下文
        this.applicationContext = applicationContext;
        // 初始化监控组件
        this.telemetry = telemetry;
        // 默认使用本地引擎
        this.primaryEngine = EngineType.LOCAL;
        // 初始化维度Key解析器
        this.dimensionKeyResolver = new DimensionKeyResolver();

        // 注册本地算法到缓存
        if (Objects.nonNull(tokenBucket)) {
            localAlgorithmCache.put(AlgorithmType.TOKEN_BUCKET, tokenBucket);
        }
        if (Objects.nonNull(leakyBucket)) {
            localAlgorithmCache.put(AlgorithmType.LEAKY_BUCKET, leakyBucket);
        }
        if (Objects.nonNull(fixedWindow)) {
            localAlgorithmCache.put(AlgorithmType.FIXED_WINDOW, fixedWindow);
        }
        if (Objects.nonNull(slidingWindowLog)) {
            localAlgorithmCache.put(AlgorithmType.SLIDING_WINDOW_LOG, slidingWindowLog);
        }
        if (Objects.nonNull(slidingWindowCounter)) {
            localAlgorithmCache.put(AlgorithmType.SLIDING_WINDOW_COUNTER, slidingWindowCounter);
        }
    }

    // ==================== 切面方法 ====================

    /**
     * 环绕通知：处理@RateLimit注解
     * 支持单个注解
     *
     * @param joinPoint 切点信息
     * @param rateLimit 限流注解
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(rateLimit)")
    public Object aroundSingle(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 创建单元素列表，统一处理
        List<RateLimit> rateLimits = new ArrayList<>();
        rateLimits.add(rateLimit);
        // 调用统一处理方法
        return processRateLimits(joinPoint, rateLimits);
    }

    /**
     * 环绕通知：处理@RateLimits容器注解
     * 支持多个注解
     *
     * @param joinPoint   切点信息
     * @param rateLimits 限流注解容器
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(rateLimits)")
    public Object aroundMultiple(ProceedingJoinPoint joinPoint, RateLimits rateLimits) throws Throwable {
        // 转换为数组列表
        List<RateLimit> rateLimitList = new ArrayList<>();
        for (RateLimit rl : rateLimits.value()) {
            rateLimitList.add(rl);
        }
        // 调用统一处理方法
        return processRateLimits(joinPoint, rateLimitList);
    }

    // ==================== 核心处理方法 ====================

    /**
     * 处理限流注解列表
     * 遍历所有注解，任一注解限流不通过则拒绝
     *
     * @param joinPoint  切点信息
     * @param rateLimits 限流注解列表
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    private Object processRateLimits(ProceedingJoinPoint joinPoint, List<RateLimit> rateLimits) throws Throwable {
        // 遍历所有限流注解
        for (RateLimit rateLimit : rateLimits) {
            // 检查是否启用限流
            if (BooleanUtils.isFalse(rateLimit.enabled())) {
                // 未启用，跳过此注解
                continue;
            }

            // 解析限流Key
            String key = resolveKey(joinPoint, rateLimit);
            // 选择限流引擎
            EngineType engine = resolveEngine(rateLimit.engine());
            // 获取限流算法
            RateLimitAlgorithm algorithm = getAlgorithm(rateLimit.algorithm(), engine);

            // 检查算法是否存在
            if (Objects.isNull(algorithm)) {
                log.warn("[RateLimit] No algorithm found for type: {}, engine: {}", 
                         rateLimit.algorithm(), engine);
                // 算法不存在，跳过限流，直接执行方法
                continue;
            }

            // 构建限流配置
            RateLimitConfig config = buildConfig(rateLimit);
            // 构建限流上下文
            ModeType modeType = engine == EngineType.REDIS ? ModeType.DISTRIBUTED : ModeType.LOCAL;
            RateLimitContext context = RateLimitContext.of(key, config, modeType);

            // 执行限流判断
            RateLimitResult result = algorithm.evaluate(context);

            // 记录监控指标
            recordTelemetry(result, key, rateLimit.algorithm());

            // 检查限流结果
            if (result.isRejected()) {
                // 限流不通过，执行降级处理
                log.debug("[RateLimit] Request rejected for key: {}, wait time: {}ms", 
                          key, result.getWaitTimeMs());
                return handleRejection(joinPoint, rateLimit, key, result);
            }
        }

        // 所有注解都通过限流，执行原方法
        return joinPoint.proceed();
    }

    // ==================== Key解析方法 ====================

    /**
     * 解析限流Key
     * 优先级：keys -> keyBuilder -> dimension -> 方法全限定名
     *
     * @param joinPoint  切点信息
     * @param rateLimit  限流注解
     * @return 解析后的Key
     */
    private String resolveKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        // 获取key前缀
        String keyPrefix = rateLimit.keyPrefix();

        // ========== 优先级1：keys属性（SpEL表达式）==========
        String[] keys = rateLimit.keys();
        if (Objects.nonNull(keys) && keys.length > 0) {
            // 使用SpEL解析keys
            return resolveKeysBySpEL(joinPoint, keys, keyPrefix);
        }

        // ========== 优先级2：keyBuilder（自定义Key构建器）==========
        Class<? extends KeyBuilder> keyBuilderClass = rateLimit.keyBuilder();
        if (Objects.nonNull(keyBuilderClass) && keyBuilderClass != KeyBuilder.class) {
            // 使用自定义Key构建器
            return resolveKeyByBuilder(joinPoint, keyBuilderClass, keyPrefix);
        }

        // ========== 优先级3：dimension（预定义维度）==========
        DimensionType dimension = rateLimit.dimension();
        if (dimension != DimensionType.METHOD) {
            // 使用维度解析器
            return resolveKeyByDimension(joinPoint, dimension, keyPrefix);
        }

        // ========== 优先级4：默认使用方法全限定名 ==========
        String methodKey = buildDefaultKey(joinPoint);
        // 添加前缀
        return StringUtils.isNotBlank(keyPrefix) ? keyPrefix + ":" + methodKey : methodKey;
    }

    /**
     * 使用SpEL解析keys
     */
    private String resolveKeysBySpEL(ProceedingJoinPoint joinPoint, String[] keys, String keyPrefix) {
        // 构建组合Key
        StringBuilder combinedKey = new StringBuilder();
        
        // 添加Key前缀
        if (StringUtils.isNotBlank(keyPrefix)) {
            combinedKey.append(keyPrefix).append(":");
        }

        // 遍历解析每个Key表达式
        for (int i = 0; i < keys.length; i++) {
            // 使用SpEL解析Key表达式
            String resolvedKey = evaluateSpelExpression(joinPoint, keys[i]);
            combinedKey.append(resolvedKey);
            
            // 多个Key之间用冒号分隔
            if (i < keys.length - 1) {
                combinedKey.append(":");
            }
        }

        return combinedKey.toString();
    }

    /**
     * 使用自定义Key构建器解析Key
     */
    private String resolveKeyByBuilder(ProceedingJoinPoint joinPoint, 
                                        Class<? extends KeyBuilder> keyBuilderClass, 
                                        String keyPrefix) {
        try {
            // 从Spring容器中获取KeyBuilder
            Map<String, KeyBuilder> builders = applicationContext.getBeansOfType(KeyBuilder.class);
            
            // 遍历查找匹配的构建器
            for (KeyBuilder builder : builders.values()) {
                // 使用isAssignableFrom判断类型匹配
                if (keyBuilderClass.isAssignableFrom(builder.getClass())) {
                    // 调用构建器构建Key
                    String builtKey = builder.build(joinPoint);
                    
                    // 校验Key是否为空
                    if (StringUtils.isBlank(builtKey)) {
                        throw new IllegalStateException(
                                "KeyBuilder '" + builder.getClass().getName() + "' returned empty key");
                    }
                    
                    // 构建完整Key
                    if (StringUtils.isNotBlank(keyPrefix)) {
                        return keyPrefix + ":" + builtKey;
                    }
                    return builtKey;
                }
            }
            
            // 未找到指定的KeyBuilder，抛出异常
            throw new IllegalStateException("KeyBuilder not found: " + keyBuilderClass.getName());
        } catch (IllegalStateException e) {
            // 重新抛出状态异常
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to get KeyBuilder: " + keyBuilderClass.getName(), e);
        }
    }

    /**
     * 根据维度解析Key
     */
    private String resolveKeyByDimension(ProceedingJoinPoint joinPoint, DimensionType dimension, String keyPrefix) {
        // 使用维度解析器解析Key
        String dimensionKey = dimensionKeyResolver.resolve(joinPoint, dimension);
        
        // 校验维度Key是否为空
        if (StringUtils.isBlank(dimensionKey)) {
            throw new IllegalStateException(
                    "Dimension resolver returned empty key for dimension: " + dimension.getCode());
        }

        // 构建完整Key
        StringBuilder fullKey = new StringBuilder();
        
        // 添加前缀
        if (StringUtils.isNotBlank(keyPrefix)) {
            fullKey.append(keyPrefix).append(":");
        }

        // 添加维度前缀
        fullKey.append(dimension.getCode()).append(":");
        // 添加维度值
        fullKey.append(dimensionKey);

        return fullKey.toString();
    }

    /**
     * 使用SpEL解析表达式
     *
     * @param joinPoint  切点信息
     * @param expression SpEL表达式
     * @return 解析结果字符串
     */
    private String evaluateSpelExpression(ProceedingJoinPoint joinPoint, String expression) {
        // 检查表达式是否为空
        if (StringUtils.isBlank(expression)) {
            return buildDefaultKey(joinPoint);
        }

        try {
            // 创建SpEL评估上下文
            EvaluationContext context = createEvaluationContext(joinPoint);
            // 解析表达式
            Expression exp = spelParser.parseExpression(expression);
            // 执行表达式求值
            Object value = exp.getValue(context);

            // 返回值的字符串形式
            return Objects.nonNull(value) ? value.toString() : "null";
        } catch (Exception e) {
            // SpEL解析失败，记录警告日志
            log.warn("[RateLimit] Failed to evaluate SpEL expression: {}, error: {}", 
                     expression, e.getMessage());
            // 返回默认Key
            return buildDefaultKey(joinPoint);
        }
    }

    /**
     * 创建SpEL评估上下文
     * 将方法参数绑定到SpEL变量
     *
     * @param joinPoint 切点信息
     * @return SpEL评估上下文
     */
    private EvaluationContext createEvaluationContext(ProceedingJoinPoint joinPoint) {
        // 创建标准评估上下文
        StandardEvaluationContext context = new StandardEvaluationContext();

        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 获取方法对象
        Method method = signature.getMethod();
        // 获取方法参数值
        Object[] args = joinPoint.getArgs();
        // 获取方法参数名
        Parameter[] parameters = method.getParameters();

        // 绑定参数到SpEL上下文
        if (Objects.nonNull(args) && Objects.nonNull(parameters)) {
            for (int i = 0; i < args.length && i < parameters.length; i++) {
                // 使用参数名作为变量名
                context.setVariable(parameters[i].getName(), args[i]);
                // 同时支持p0, p1... 和 a0, a1... 形式的参数访问
                context.setVariable("p" + i, args[i]);
                context.setVariable("a" + i, args[i]);
            }
        }

        // 添加额外变量：方法名
        context.setVariable("method", method.getName());
        // 添加额外变量：类名
        context.setVariable("className", method.getDeclaringClass().getSimpleName());
        // 添加额外变量：所有参数数组
        context.setVariable("args", args);

        return context;
    }

    /**
     * 构建默认Key（基于方法签名）
     *
     * @param joinPoint 切点信息
     * @return 默认Key字符串
     */
    private String buildDefaultKey(ProceedingJoinPoint joinPoint) {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 获取方法对象
        Method method = signature.getMethod();
        // 获取类名
        String className = method.getDeclaringClass().getSimpleName();
        // 获取方法名
        String methodName = method.getName();
        // 组合成Key
        return className + ":" + methodName;
    }

    // ==================== 引擎和算法选择方法 ====================

    /**
     * 解析限流引擎
     * AUTO时使用配置的primary引擎
     *
     * @param engine 注解指定的引擎类型
     * @return 实际使用的引擎类型
     */
    private EngineType resolveEngine(EngineType engine) {
        // 检查是否为AUTO
        if (engine == EngineType.AUTO) {
            // 使用配置的主引擎
            return this.primaryEngine;
        }
        // 返回指定的引擎
        return engine;
    }

    /**
     * 获取限流算法
     * 根据算法类型和引擎类型获取对应的算法实现
     *
     * @param algorithmType 算法类型
     * @param engine        引擎类型
     * @return 限流算法实现
     */
    private RateLimitAlgorithm getAlgorithm(AlgorithmType algorithmType, EngineType engine) {
        // 根据引擎类型选择算法缓存
        Map<AlgorithmType, RateLimitAlgorithm> cache = 
                engine == EngineType.REDIS ? redisAlgorithmCache : localAlgorithmCache;
        
        // 从缓存中获取算法
        return cache.get(algorithmType);
    }

    /**
     * 注册Redis算法
     * 由配置类调用，注入Redis算法实现
     *
     * @param algorithmType 算法类型
     * @param algorithm     算法实现
     */
    public void registerRedisAlgorithm(AlgorithmType algorithmType, RateLimitAlgorithm algorithm) {
        // 注册到Redis算法缓存
        redisAlgorithmCache.put(algorithmType, algorithm);
    }

    // ==================== 配置构建方法 ====================

    /**
     * 构建限流配置
     * 从注解属性构建配置对象
     *
     * @param rateLimit 限流注解
     * @return 限流配置
     */
    private RateLimitConfig buildConfig(RateLimit rateLimit) {
        return RateLimitConfig.builder()
                .name("annotation-based")
                .algorithmType(rateLimit.algorithm())
                .rate(rateLimit.rate())
                .period(rateLimit.period())
                .keyPrefix(rateLimit.keyPrefix())
                // maxBurst为0时使用rate值
                .maxBurst(rateLimit.maxBurst() > 0 ? rateLimit.maxBurst() : rateLimit.rate())
                .build();
    }

    // ==================== 降级处理方法 ====================

    /**
     * 处理限流拒绝
     * 调用降级处理器处理被拒绝的请求
     *
     * @param joinPoint 切点信息
     * @param rateLimit 限流注解
     * @param key       限流Key
     * @param result    限流结果
     * @return 降级处理结果
     */
    private Object handleRejection(ProceedingJoinPoint joinPoint, RateLimit rateLimit, String key, RateLimitResult result) {
        // 创建限流异常
        RuntimeException exception = createRateLimitException(rateLimit, key, result);
        // 获取降级处理器
        FallbackHandler fallbackHandler = getFallbackHandler(rateLimit.fallbackHandler());

        // 检查异常类型
        if (exception instanceof RateLimitException) {
            // 调用降级处理器处理
            return fallbackHandler.handle(joinPoint, (RateLimitException) exception);
        }
        // 其他异常类型，包装为RateLimitException
        return fallbackHandler.handle(joinPoint, new RateLimitException(exception.getMessage()));
    }

    /**
     * 获取降级处理器
     * 从Spring容器中查找指定类型的降级处理器
     *
     * @param handlerClass 降级处理器类型
     * @return 降级处理器实例
     */
    private FallbackHandler getFallbackHandler(Class<? extends FallbackHandler> handlerClass) {
        try {
            // 从容器中获取所有FallbackHandler Bean
            Map<String, FallbackHandler> handlers = applicationContext.getBeansOfType(FallbackHandler.class);
            
            // 遍历查找匹配的处理器
            for (FallbackHandler handler : handlers.values()) {
                if (handler.getClass().equals(handlerClass)) {
                    return handler;
                }
            }
            
            // 未找到指定类型，返回第一个可用的处理器
            return handlers.values().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No FallbackHandler found"));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get FallbackHandler: " + handlerClass.getName(), e);
        }
    }

    /**
     * 创建限流异常
     *
     * @param rateLimit 限流注解
     * @param key       限流Key
     * @param result    限流结果
     * @return 限流异常
     */
    private RuntimeException createRateLimitException(RateLimit rateLimit, String key,
                                                       RateLimitResult result) {
        // 获取异常类型
        Class<? extends RuntimeException> exceptionClass = rateLimit.exceptionClass();

        // 检查是否为RateLimitException类型
        if (Objects.equals(exceptionClass, RateLimitException.class)) {
            // 创建RateLimitException
            return new RateLimitException(key, rateLimit.algorithm().getCode(),
                    rateLimit.rate(), result.getWaitTimeMs());
        }

        // 构建异常消息
        String message = String.format("Rate limit exceeded: key=%s, algorithm=%s, rate=%d/s, wait=%dms",
                key, rateLimit.algorithm().getCode(), rateLimit.rate(), result.getWaitTimeMs());

        try {
            // 尝试使用String参数构造函数创建异常
            return exceptionClass.getConstructor(String.class).newInstance(message);
        } catch (Exception e) {
            // 创建失败，返回RateLimitException
            return new RateLimitException(key, rateLimit.algorithm().getCode(),
                    rateLimit.rate(), result.getWaitTimeMs());
        }
    }

    // ==================== 监控记录方法 ====================

    /**
     * 记录监控指标
     *
     * @param result        限流结果
     * @param key           限流Key
     * @param algorithmType 算法类型
     */
    private void recordTelemetry(RateLimitResult result, String key, AlgorithmType algorithmType) {
        // 检查监控组件是否启用
        if (Objects.isNull(telemetry) || BooleanUtils.isFalse(telemetry.isEnabled())) {
            return;
        }

        // 根据限流结果记录不同指标
        if (result.isAllowed()) {
            // 记录允许通过的请求
            telemetry.recordAllowed(key, algorithmType, 1, result.getRemainingPermits());
        } else {
            // 记录被拒绝的请求
            telemetry.recordRejected(key, algorithmType, 1, result.getWaitTimeMs());
        }
    }
}