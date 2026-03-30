package com.geek.ratelimit4j.starter.aspect;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import com.geek.ratelimit4j.core.algorithm.RateLimitAlgorithm;
import com.geek.ratelimit4j.core.algorithm.RateLimitResult;
import com.geek.ratelimit4j.core.config.EngineType;
import com.geek.ratelimit4j.core.config.ModeType;
import com.geek.ratelimit4j.core.config.RateLimitConfig;
import com.geek.ratelimit4j.core.config.RateLimitContext;
import com.geek.ratelimit4j.core.engine.EngineProviderRegistry;
import com.geek.ratelimit4j.core.exception.NoSuchRateLimitEngineException;
import com.geek.ratelimit4j.core.exception.RateLimitException;
import com.geek.ratelimit4j.core.registry.AlgorithmRegistry;
import com.geek.ratelimit4j.core.resolver.RateLimitResolveContext;
import com.geek.ratelimit4j.core.telemetry.RateLimitTelemetry;
import com.geek.ratelimit4j.core.annotation.RateLimit;
import com.geek.ratelimit4j.core.annotation.RateLimits;
import com.geek.ratelimit4j.core.handler.FallbackHandler;
import com.geek.ratelimit4j.starter.resolver.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 抽象限流切面基类
 *
 * <p>该类是限流功能的核心实现骨架，采用模板方法设计模式，定义了限流处理的完整流程。
 * 具体子类可以通过继承此类来扩展或定制特定的限流行为。</p>
 *
 * <h2>核心职责</h2>
 * <ul>
 *   <li>拦截带有 {@link RateLimit} 或 {@link RateLimits} 注解的方法</li>
 *   <li>解析限流键（Key）、引擎类型、算法类型等配置</li>
 *   <li>调用限流算法进行流量评估</li>
 *   <li>处理限流拒绝场景（抛异常或执行降级处理）</li>
 *   <li>记录遥测数据用于监控和统计</li>
 * </ul>
 *
 * <h2>处理流程</h2>
 * <pre>
 * 方法拦截 → 配置解析 → 键解析 → 算法获取 → 流量评估 → 结果处理
 *     ↓           ↓         ↓         ↓          ↓
 * @RateLimit   Engine    Key      Algorithm   Allowed/Rejected
 *                                              ↓
 *                                    Allowed: 执行原方法
 *                                    Rejected: 降级/异常
 * </pre>
 *
 * <h2>设计模式</h2>
 * <ul>
 *   <li>模板方法模式：定义处理骨架，子类可扩展具体步骤</li>
 *   <li>策略模式：通过 {@link AlgorithmRegistry} 支持多种限流算法</li>
 *   <li>依赖注入：通过 {@link ApplicationContext} 获取降级处理器</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 * @see RateLimit
 * @see RateLimits
 * @see RateLimitAlgorithm
 * @see FallbackHandler
 */
@Slf4j
@org.aspectj.lang.annotation.Aspect
@Getter
public abstract class AbstractRateLimitAspect implements RateLimitAspectSupport {

    /**
     * 限流算法注册表
     *
     * <p>持有所有已注册的限流算法实例，根据算法类型和引擎类型进行查找。
     * 支持的算法包括：固定窗口、滑动窗口、令牌桶、漏桶等。</p>
     *
     * <p>查找逻辑：algorithmRegistry.getAlgorithm(algorithmType, engineType)</p>
     */
    protected final AlgorithmRegistry algorithmRegistry;

    /**
     * Spring 应用上下文
     *
     * <p>用于从 Spring 容器中获取降级处理器（FallbackHandler）实例。
     * 降级处理器必须是 Spring Bean，才能通过此上下文进行依赖注入。</p>
     */
    protected final ApplicationContext applicationContext;

    /**
     * 遥测组件（可选）
     *
     * <p>用于记录限流相关的监控数据，包括：
     * <ul>
     *   <li>允许通过的请求数量和剩余配额</li>
     *   <li>被拒绝的请求数量和等待时间</li>
     * </ul>
     * </p>
     *
     * <p>当 telemetry 为 null 或未启用时，遥测记录将被跳过。</p>
     */
    protected final RateLimitTelemetry telemetry;

    /**
     * 限流键解析器
     *
     * <p>负责将 {@link RateLimit} 注解和当前方法调用上下文解析为具体的限流键。
     * 限流键用于标识不同的限流维度（如：用户ID、IP地址、API路径等）。</p>
     *
     * <p>解析流程：
     * <pre>
     * RateLimit 注解配置 + 方法签名 + 参数 → RateLimitKeyResolver → String key
     * </pre>
     * </p>
     */
    protected final RateLimitKeyResolver keyResolver;

    /**
     * 引擎提供者注册表
     *
     * <p>负责管理和获取限流引擎实例。支持的引擎类型：
     * <ul>
     *   <li>{@link EngineType#LOCAL}：本地内存引擎（单机限流）</li>
     *   <li>{@link EngineType#REDIS}：Redis 引擎（分布式限流）</li>
     *   <li>{@link EngineType#AUTO}：自动选择（根据配置决定）</li>
     * </ul>
     * </p>
     */
    protected final EngineProviderRegistry engineProviderRegistry;

    /**
     * 构造函数
     *
     * <p>初始化限流切面所需的所有核心组件。所有必需参数都会进行非空校验，
     * 确保切面实例创建后处于可用状态。</p>
     *
     * <p>参数说明：
     * <ul>
     *   <li>algorithmRegistry：必需，用于获取限流算法</li>
     *   <li>applicationContext：必需，用于获取降级处理器 Bean</li>
     *   <li>telemetry：可选，为 null 时跳过遥测记录</li>
     *   <li>keyResolver：必需，用于解析限流键</li>
     *   <li>engineProviderRegistry：必需，用于解析和获取引擎</li>
     * </ul>
     * </p>
     *
     * @param algorithmRegistry        限流算法注册表，不能为 null
     * @param applicationContext       Spring 应用上下文，不能为 null
     * @param telemetry                遥测组件，可为 null（表示不记录遥测）
     * @param keyResolver              限流键解析器，不能为 null
     * @param engineProviderRegistry   引擎提供者注册表，不能为 null
     * @throws NullPointerException 当必需参数为 null 时抛出
     */
    protected AbstractRateLimitAspect(
            AlgorithmRegistry algorithmRegistry,
            ApplicationContext applicationContext,
            RateLimitTelemetry telemetry,
            RateLimitKeyResolver keyResolver,
            EngineProviderRegistry engineProviderRegistry) {
        // 校验并赋值算法注册表，null 时抛出 NPE
        this.algorithmRegistry = Objects.requireNonNull(algorithmRegistry, "algorithmRegistry must not be null");
        // 校验并赋值应用上下文，用于后续获取降级处理器 Bean
        this.applicationContext = Objects.requireNonNull(applicationContext, "applicationContext must not be null");
        // 遥测组件可选赋值，null 表示不启用遥测功能
        this.telemetry = telemetry;
        // 校验并赋值键解析器，用于生成限流维度标识
        this.keyResolver = Objects.requireNonNull(keyResolver, "keyResolver must not be null");
        // 校验并赋值引擎注册表，用于获取限流引擎实例
        this.engineProviderRegistry = Objects.requireNonNull(engineProviderRegistry, "engineProviderRegistry must not be null");
    }

    /**
     * 单注解拦截切入点
     *
     * <p>拦截标注了单个 {@link RateLimit} 注解的方法，执行限流逻辑。
     * 通过 Spring AOP 的 @Around 通知实现方法环绕拦截。</p>
     *
     * <p>拦截流程：
     * <pre>
     * 方法调用 → aroundSingle 拦截 → processRateLimits 处理 → 结果返回
     * </pre>
     * </p>
     *
     * <p>注解绑定机制：AspectJ 通过参数名匹配，将方法上的注解实例自动绑定到参数。</p>
     *
     * @param joinPoint   AOP 连接点，包含被拦截方法的信息和参数
     * @param rateLimit   方法上标注的限流注解实例，由 AspectJ 自动绑定
     * @return 方法执行结果，或降级处理结果（当被限流时）
     * @throws Throwable 方法执行异常，或限流拒绝异常（无降级处理器时）
     */
    @Around("@annotation(rateLimit)")
    public Object aroundSingle(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 将单个注解包装为列表，统一调用 processRateLimits 处理
        return processRateLimits(joinPoint, Collections.singletonList(rateLimit));
    }

    /**
     * 多注解拦截切入点
     *
     * <p>拦截标注了 {@link RateLimits} 组合注解的方法，执行多层级限流逻辑。
     * {@link RateLimits} 注解可包含多个 {@link RateLimit}，实现多维度限流。</p>
     *
     * <p>典型场景：同时限制用户级和 IP 级的请求频率
     * <pre>
     * &#064;RateLimits({
     *   &#064;RateLimit(key = "user", rate = 100),   // 用户级：100次/秒
     *   &#064;RateLimit(key = "ip", rate = 1000)    // IP级：1000次/秒
     * })
     * </pre>
     * </p>
     *
     * <p>处理顺序：按注解数组顺序依次检查，任一限流不通过即拒绝。</p>
     *
     * @param joinPoint    AOP 连接点，包含被拦截方法的信息和参数
     * @param rateLimits   方法上标注的组合注解实例，由 AspectJ 自动绑定
     * @return 方法执行结果，或降级处理结果（当被限流时）
     * @throws Throwable 方法执行异常，或限流拒绝异常（无降级处理器时）
     */
    @Around("@annotation(rateLimits)")
    public Object aroundMultiple(ProceedingJoinPoint joinPoint, RateLimits rateLimits) throws Throwable {
        // 将组合注解中的多个 RateLimit 转为列表，统一处理
        return processRateLimits(joinPoint, Arrays.asList(rateLimits.value()));
    }

    /**
     * 限流处理核心模板方法
     *
     * <p>这是整个限流逻辑的核心实现，采用模板方法模式定义了完整的处理流程。
     * 对每个限流注解依次执行：启用检查 → 键解析 → 引擎解析 → 算法获取 → 配置构建 →
     * 流量评估 → 遥测记录 → 结果处理。</p>
     *
     * <h3>处理流程详解</h3>
     * <ol>
     *   <li><b>迭代限流规则</b>：遍历所有 RateLimit 注解，按顺序检查</li>
     *   <li><b>启用检查</b>：跳过 enabled=false 的规则，不做限流</li>
     *   <li><b>键解析</b>：根据注解配置和方法上下文生成限流键</li>
     *   <li><b>引擎解析</b>：确定使用本地引擎还是分布式引擎</li>
     *   <li><b>算法获取</b>：从注册表获取对应的限流算法实例</li>
     *   <li><b>配置构建</b>：将注解属性转换为算法可用的配置对象</li>
     *   <li><b>流量评估</b>：调用算法判断当前请求是否允许通过</li>
     *   <li><b>遥测记录</b>：记录允许/拒绝统计数据</li>
     *   <li><b>拒绝处理</b>：如被拒绝，执行降级或抛异常</li>
     *   <li><b>执行原方法</b>：所有规则通过后，执行被拦截的业务方法</li>
     * </ol>
     *
     * <h3>短路机制</h3>
     * <p>任一限流规则拒绝请求时，立即返回，不会继续检查后续规则或执行原方法。
     * 这保证了"严格匹配"原则——必须通过所有规则才能执行业务逻辑。</p>
     *
     * <h3>线程安全</h3>
     * <p>方法本身不维护共享状态，依赖的组件（算法、引擎）自行保证线程安全。
     * 本地算法使用 CAS/Atomic 类，分布式算法依赖 Redis 原子操作。</p>
     *
     * @param joinPoint    AOP 连接点，包含目标方法签名、参数、实例等信息
     * @param rateLimits   限流注解列表，可能包含一个或多个规则
     * @return 方法执行结果，或降级处理结果
     * @throws Throwable 业务方法执行异常，或限流拒绝异常（无降级配置时）
     */
    @Override
    public Object processRateLimits(ProceedingJoinPoint joinPoint, List<RateLimit> rateLimits) throws Throwable {
        // 遍历所有限流规则，按注解定义顺序依次检查
        // 任一规则拒绝请求即返回，形成"短路"效果
        for (RateLimit rateLimit : rateLimits) {
            // 检查当前规则是否启用
            // enabled=false 的规则直接跳过，不进行限流判断
            if (!isEnabled(rateLimit)) {
                continue;
            }

            // 解析限流键：根据注解的 key/keyExpression 和当前方法调用上下文生成唯一标识
            // 不同的键代表不同的限流维度（如：user:123、ip:192.168.1.1、api:/v1/users）
            String key = resolveKey(joinPoint, rateLimit);

            // 解析引擎类型：将注解配置的引擎类型转换为实际使用的引擎
            // AUTO 类型会根据配置自动选择 LOCAL 或 REDIS
            EngineType engine = resolveEngine(rateLimit.engine());

            // 获取限流算法实例：根据算法类型和引擎类型从注册表查找
            // 算法实例负责实际的限流计算逻辑
            RateLimitAlgorithm algorithm = getAlgorithm(rateLimit.algorithm(), engine);

            // 构建限流配置对象：将注解属性转换为 RateLimitConfig
            // 包含：速率、周期、最大突发量、键前缀等参数
            RateLimitConfig config = buildConfig(rateLimit);

            // 根据引擎类型确定运行模式
            // REDIS 引擎 → 分布式模式（多实例共享限流计数）
            // LOCAL/AUTO → 本地模式（单实例独立限流）
            ModeType modeType = engine == EngineType.REDIS ? ModeType.DISTRIBUTED : ModeType.LOCAL;

            // 构建限流上下文：封装键、配置、模式等评估所需的所有信息
            // 上下文是算法评估的输入参数
            RateLimitContext context = RateLimitContext.of(key, config, modeType);

            // 执行限流评估：调用算法判断当前请求是否允许通过
            // 返回结果包含：是否允许、剩余配额、等待时间（被拒绝时）
            RateLimitResult result = algorithm.evaluate(context);

            // 记录遥测数据：统计允许/拒绝的请求，用于监控和告警
            // 当 telemetry 为 null 或未启用时，此方法内部会跳过
            recordTelemetry(result, key, rateLimit.algorithm());

            // 判断请求是否被拒绝
            // isRejected=true 表示超出限流阈值，需要进行拒绝处理
            if (result.isRejected()) {
                // 记录调试日志：输出被拒绝的键和等待时间，便于问题排查
                log.debug("[RateLimit] Request rejected for key: {}, wait time: {}ms",
                        key, result.getWaitTimeMs());
                // 执行拒绝处理：创建异常、查找降级处理器、执行降级逻辑或抛异常
                // 短路返回：被拒绝的请求不会继续检查后续规则，也不会执行原方法
                return handleRejection(joinPoint, rateLimit, key, result);
            }
        }

        // 所有限流规则都通过（或全部被跳过），执行原业务方法
        // joinPoint.proceed() 会调用被拦截的方法，并返回其执行结果
        return joinPoint.proceed();
    }

    /**
     * 检查限流规则是否启用
     *
     * <p>判断 {@link RateLimit} 注解的 enabled 属性是否为 true。
     * 未启用的规则会被跳过，不做任何限流判断。</p>
     *
     * <p>使用场景：临时关闭某个限流规则，用于调试或特殊业务场景。</p>
     *
     * @param rateLimit 限流注解实例
     * @return true 表示启用，需执行限流；false 表示禁用，跳过该规则
     */
    protected boolean isEnabled(RateLimit rateLimit) {
        // 使用 BooleanUtils.isTrue 处理可能为 null 的 Boolean 值
        // null 或 false 都返回 false，只有显式 true 才返回 true
        return BooleanUtils.isTrue(rateLimit.enabled());
    }

    /**
     * 解析限流键
     *
     * <p>根据限流注解配置和当前方法调用上下文，生成唯一的限流标识键。
     * 不同的键代表不同的限流维度，互不影响。</p>
     *
     * <h3>键解析机制</h3>
     * <ol>
     *   <li>创建解析上下文：封装 JoinPoint（方法信息）和 RateLimit（注解配置）</li>
     *   <li>调用键解析器：解析器根据 key/keyExpression 字段计算最终键值</li>
     * </ol>
     *
     * <h3>键表达式支持</h3>
     * <p>支持 SpEL 表达式从方法参数中动态提取键值：
     * <pre>
     * &#064;RateLimit(keyExpression = "#userId")  // 从参数 userId 提取值
     * &#064;RateLimit(keyExpression = "#user.id") // 从参数 user 对象提取 id 属性
     * </pre>
     * </p>
     *
     * @param joinPoint  AOP 连接点，包含方法签名和参数值
     * @param rateLimit  限流注解，包含键配置信息
     * @return 解析后的限流键字符串，如："user:123" 或 "ip:192.168.1.1"
     */
    protected String resolveKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        // 创建解析上下文：将方法调用信息和注解配置封装在一起
        // DefaultRateLimitResolveContext 提供了获取方法签名、参数、注解属性的标准接口
        RateLimitResolveContext context = new DefaultRateLimitResolveContext(joinPoint, rateLimit);
        // 调用键解析器执行解析逻辑
        // 解析器会根据 key 字段（静态值）或 keyExpression 字段（SpEL 表达式）生成键
        return keyResolver.resolve(context);
    }

    /**
     * 解析限流引擎类型
     *
     * <p>将注解配置的引擎类型转换为实际使用的引擎实例。
     * 支持 LOCAL（本地）、REDIS（分布式）、AUTO（自动选择）三种类型。</p>
     *
     * <h3>解析逻辑</h3>
     * <ol>
     *   <li><b>显式指定</b>：如果注解指定了 LOCAL 或 REDIS，直接返回该类型</li>
     *   <li><b>自动选择</b>：如果指定 AUTO，由引擎提供者根据配置决定使用哪个引擎</li>
     *   <li><b>错误处理</b>：如果指定了不存在的引擎类型，抛出异常</li>
     * </ol>
     *
     * <h3>引擎选择策略</h3>
     * <p>AUTO 模式的选择逻辑（由 EngineProviderRegistry 实现）：
     * <ul>
     *   <li>如果配置了 Redis 连接，优先使用 REDIS 引擎</li>
     *   <li>否则使用 LOCAL 引擎（本地内存）</li>
     * </ul>
     * </p>
     *
     * @param engine 注解配置的引擎类型
     * @return 实际使用的引擎类型（LOCAL 或 REDIS）
     * @throws NoSuchRateLimitEngineException 当请求的引擎不存在时抛出
     */
    protected EngineType resolveEngine(EngineType engine) {
        // 调用引擎注册表的引擎提取方法
        // extractEngine 内部处理 AUTO → 实际引擎的转换逻辑
        // 以及引擎存在性校验（不存在时抛出 NoSuchRateLimitEngineException）
        return this.engineProviderRegistry.extractEngine(engine);
    }

    /**
     * 获取限流算法实例
     *
     * <p>根据算法类型和引擎类型，从算法注册表中获取对应的限流算法实现。</p>
     *
     * <h3>算法类型支持</h3>
     * <ul>
     *   <li>{@link AlgorithmType#FIXED_WINDOW}：固定窗口计数算法</li>
     *   <li>{@link AlgorithmType#SLIDING_WINDOW_COUNTER}：滑动窗口计数算法</li>
     *   <li>{@link AlgorithmType#TOKEN_BUCKET}：令牌桶算法（支持突发流量）</li>
     *   <li>{@link AlgorithmType#LEAKY_BUCKET}：漏桶算法（恒定速率输出）</li>
     * </ul>
     *
     * <h3>算法与引擎映射</h3>
     * <p>同一种算法在不同引擎上有不同实现：
     * <ul>
     *   <li>LOCAL 引擎：使用 Java Atomic/Semaphore 实现</li>
     *   <li>REDIS 引擎：使用 Redis Lua 脚本实现原子操作</li>
     * </ul>
     * </p>
     *
     * @param algorithmType 算法类型枚举
     * @param engine        引擎类型枚举
     * @return 对应的限流算法实例
     * @throws IllegalArgumentException 当找不到对应算法时抛出
     */
    protected RateLimitAlgorithm getAlgorithm(AlgorithmType algorithmType, EngineType engine) {
        // 从算法注册表查询算法实例
        // 注册表内部维护了 算法类型 × 引擎类型 的算法实例矩阵
        RateLimitAlgorithm algorithm = algorithmRegistry.getAlgorithm(algorithmType, engine);
        // 查询结果为 null 表示该算法类型在指定引擎上未实现
        // 抛出异常阻止继续执行，避免空指针问题
        if (algorithm == null) {
            throw new IllegalArgumentException(
                    String.format("No algorithm found for type=%s, engine=%s",
                            algorithmType.getCode(), engine.getCode()));
        }
        // 返回查询到的算法实例
        return algorithm;
    }

    /**
     * 构建限流配置对象
     *
     * <p>将 {@link RateLimit} 注解的属性转换为 {@link RateLimitConfig} 配置对象，
     * 供限流算法使用。配置对象包含算法运行所需的全部参数。</p>
     *
     * <h3>配置属性映射</h3>
     * <table border="1">
     *   <tr><th>注解属性</th><th>配置字段</th><th>说明</th></tr>
     *   <tr><td>algorithm</td><td>algorithmType</td><td>算法类型枚举</td></tr>
     *   <tr><td>rate</td><td>rate</td><td>速率（请求数/秒）</td></tr>
     *   <tr><td>period</td><td>period</td><td>时间窗口大小</td></tr>
     *   <tr><td>keyPrefix</td><td>keyPrefix</td><td>键前缀，用于区分不同应用</td></tr>
     *   <tr><td>maxBurst</td><td>maxBurst</td><td>最大突发量，默认等于 rate</td></tr>
     * </table>
     *
     * <h3>maxBurst 默认处理</h3>
     * <p>如果注解未设置 maxBurst（值为 0 或负数），则使用 rate 作为默认值。
     * 这保证了令牌桶算法在没有显式配置时，突发容量与速率一致。</p>
     *
     * @param rateLimit 限流注解实例
     * @return 构建完成的限流配置对象
     */
    protected RateLimitConfig buildConfig(RateLimit rateLimit) {
        // 使用 Builder 模式构建配置对象，确保不可变性和参数完整性
        return RateLimitConfig.builder()
                // 设置配置名称，标识来源于注解配置
                .name("annotation-based")
                // 设置算法类型：决定使用哪种限流算法
                .algorithmType(rateLimit.algorithm())
                // 设置速率：每秒允许的请求数量
                .rate(rateLimit.rate())
                // 设置时间窗口：限流计算的周期
                .period(rateLimit.period())
                // 设置键前缀：用于区分不同应用的限流计数
                .keyPrefix(rateLimit.keyPrefix())
                // 设置最大突发量：
                // 如果注解设置了正值，使用注解值
                // 如果注解未设置（0 或负数），使用 rate 作为默认值
                // 突发量用于令牌桶算法，表示可累积的最大令牌数
                .maxBurst(rateLimit.maxBurst() > 0 ? rateLimit.maxBurst() : rateLimit.rate())
                // 构建并返回配置对象
                .build();
    }

    /**
     * 处理限流拒绝场景
     *
     * <p>当请求被限流算法拒绝时，执行此方法进行后续处理。
     * 支持两种处理方式：</p>
     *
     * <ol>
     *   <li><b>抛出异常</b>：如果未配置降级处理器，直接抛出限流异常</li>
     *   <li><b>降级处理</b>：如果配置了降级处理器，调用处理器返回替代结果</li>
     * </ol>
     *
     * <h3>降级处理器机制</h3>
     * <p>降级处理器（FallbackHandler）允许业务代码自定义拒绝后的处理逻辑，
     * 例如：
     * <ul>
     *   <li>返回缓存数据</li>
     *   <li>返回默认值</li>
     *   <li>返回降级提示信息</li>
     *   <li>记录日志并重试</li>
     * </ul>
     * </p>
     *
     * <h3>异常创建逻辑</h3>
     * <p>首先创建限流异常对象（可自定义异常类型），然后将异常传递给降级处理器。
     * 降级处理器可根据异常信息决定如何处理。</p>
     *
     * @param joinPoint  AOP 连接点，包含被拦截方法的信息（降级处理器可能需要）
     * @param rateLimit  限流注解，包含降级处理器和异常类型配置
     * @param key        限流键，用于异常信息
     * @param result     限流评估结果，包含等待时间等信息
     * @return 降级处理器返回的结果
     * @throws RuntimeException 当未配置降级处理器时抛出
     */
    protected Object handleRejection(ProceedingJoinPoint joinPoint, RateLimit rateLimit,
                                     String key, RateLimitResult result) {
        // 创建限流异常对象：
        // 1. 如果注解指定了自定义异常类，尝试创建该类型异常
        // 2. 如果创建失败或未指定，使用默认的 RateLimitException
        RuntimeException exception = createRateLimitException(rateLimit, key, result);

        // 获取注解配置的降级处理器类型
        Class<? extends FallbackHandler> handlerClass = rateLimit.fallbackHandler();

        // 检查是否配置了有效的降级处理器：
        // 如果 handlerClass 等于 FallbackHandler.class（接口默认值），
        // 表示用户未指定具体实现类，不需要降级处理
        if (Objects.equals(handlerClass, FallbackHandler.class)) {
            // 未配置降级处理器，直接抛出异常终止请求
            throw exception;
        }

        // 从 Spring 容器获取降级处理器实例
        // 降级处理器必须是注册的 Spring Bean
        FallbackHandler fallbackHandler = getFallbackHandler(handlerClass);

        // 调用降级处理器执行降级逻辑：
        // 如果异常是 RateLimitException 类型，直接传递
        if (exception instanceof RateLimitException) {
            // 使用 RateLimitException 调用处理器，处理器可访问详细限流信息
            return fallbackHandler.handle(joinPoint, (RateLimitException) exception);
        }
        // 如果异常是其他类型（自定义异常），包装为 RateLimitException 后传递
        // 确保处理器接口统一接收 RateLimitException 参数
        return fallbackHandler.handle(joinPoint, new RateLimitException(exception.getMessage()));
    }

    /**
     * 获取降级处理器实例
     *
     * <p>从 Spring 应用上下文中获取指定类型的降级处理器 Bean。
     * 降级处理器必须提前注册为 Spring Bean，才能被此方法获取。</p>
     *
     * <h3>注册方式</h3>
     * <ul>
     *   <li>使用 @Component/@Service 注解标记处理器类</li>
     *   <li>在配置类中使用 @Bean 方法声明</li>
     *   <li>实现 FallbackHandler 接口并注册到容器</li>
     * </ul>
     *
     * <h3>异常处理</h3>
     * <p>如果容器中没有对应类型的 Bean，抛出 IllegalStateException，
     * 提示用户检查降级处理器是否正确注册。</p>
     *
     * @param handlerClass 降级处理器类型
     * @return 对应类型的降级处理器实例
     * @throws IllegalStateException 当处理器 Bean 不存在时抛出
     */
    protected FallbackHandler getFallbackHandler(Class<? extends FallbackHandler> handlerClass) {
        try {
            // 从 Spring 容器按类型获取 Bean
            // getBean 会自动匹配唯一实现类实例
            return applicationContext.getBean(handlerClass);
        } catch (BeansException e) {
            // Bean 不存在或获取失败时，抛出包装异常
            // 提示开发者降级处理器未正确注册到 Spring 容器
            throw new IllegalStateException("FallbackHandler not found: " + handlerClass.getName(), e);
        }
    }

    /**
     * 创建限流异常对象
     *
     * <p>根据限流注解配置创建合适的异常实例。支持自定义异常类型，
     * 允许业务代码定义专属的限流异常类。</p>
     *
     * <h3>异常创建策略</h3>
     * <ol>
     *   <li><b>默认异常</b>：如果未指定异常类型，创建 {@link RateLimitException}</li>
     *   <li><b>自定义异常</b>：如果指定了异常类，尝试通过 String 构造器创建实例</li>
     *   <li><b>创建失败</b>：如果自定义异常类没有 String 构造器，回退到默认异常</li>
     * </ol>
     *
     * <h3>自定义异常要求</h3>
     * <p>自定义异常类必须满足：
     * <ul>
     *   <li>继承 RuntimeException</li>
     *   <li>提供 public 构造器，接收 String 参数（异常消息）</li>
     * </ul>
     * </p>
     *
     * <h3>异常信息格式</h3>
     * <p>默认异常包含完整限流信息：键、算法、速率、等待时间。
     * 自定义异常只包含消息字符串。</p>
     *
     * @param rateLimit 限流注解，包含异常类型配置
     * @param key       限流键
     * @param result    限流评估结果，包含等待时间
     * @return 创建的异常实例
     */
    protected RuntimeException createRateLimitException(RateLimit rateLimit, String key,
                                                        RateLimitResult result) {
        // 获取注解配置的异常类型
        Class<? extends RuntimeException> exceptionClass = rateLimit.exceptionClass();

        // 检查是否使用默认异常类：
        // 如果 exceptionClass 等于 RateLimitException.class（默认值），
        // 直接创建包含完整限流信息的默认异常
        if (Objects.equals(exceptionClass, RateLimitException.class)) {
            // 创建默认限流异常，包含键、算法、速率、等待时间等详细信息
            return new RateLimitException(key, rateLimit.algorithm().getCode(),
                    rateLimit.rate(), result.getWaitTimeMs());
        }

        // 配置了自定义异常类，构建异常消息
        // 消息格式：标准化的限流拒绝信息，便于日志和监控
        String message = String.format("Rate limit exceeded: key=%s, algorithm=%s, rate=%d/s, wait=%dms",
                key, rateLimit.algorithm().getCode(), rateLimit.rate(), result.getWaitTimeMs());

        try {
            // 尝试使用 String 构造器创建自定义异常实例
            // 反射调用：exceptionClass.getConstructor(String.class).newInstance(message)
            return exceptionClass.getConstructor(String.class).newInstance(message);
        } catch (Exception e) {
            // 自定义异常类缺少 String 构造器，创建失败
            // 回退到默认异常，确保异常始终能被创建
            return new RateLimitException(key, rateLimit.algorithm().getCode(),
                    rateLimit.rate(), result.getWaitTimeMs());
        }
    }

    /**
     * 记录遥测数据
     *
     * <p>将限流评估结果记录到遥测系统，用于监控、统计和告警。
     * 遥测数据包括请求通过数量、剩余配额、拒绝数量、等待时间等。</p>
     *
     * <h3>遥测数据类型</h3>
     * <ul>
     *   <li><b>Allowed</b>：记录允许通过的请求、剩余配额</li>
     *   <li><b>Rejected</b>：记录被拒绝的请求、建议等待时间</li>
     * </ul>
     *
     * <h3>启用条件</h3>
     * <p>遥测记录需要满足：
     * <ul>
     *   <li>telemetry 实例不为 null（已注入遥测组件）</li>
     *   <li>telemetry.isEnabled() 返回 true（遥测功能开启）</li>
     * </ul>
     * 任一条件不满足，此方法直接返回，不执行记录。
     * </p>
     *
     * <h3>数据用途</h3>
     * <p>遥测数据可用于：
     * <ul>
     *   <li>实时监控限流状态（ Grafana/Prometheus 集成）</li>
     *   <li>统计 API 访问频率和拒绝率</li>
     *   <li>触发限流告警（拒绝率过高）</li>
     *   <li>分析用户行为模式</li>
     * </ul>
     * </p>
     *
     * @param result        限流评估结果
     * @param key           限流键
     * @param algorithmType 算法类型
     */
    protected void recordTelemetry(RateLimitResult result, String key, AlgorithmType algorithmType) {
        // 检查遥测组件是否可用：
        // 1. telemetry 为 null → 未注入遥测组件，跳过
        // 2. telemetry.isEnabled() 为 false → 遥测功能关闭，跳过
        if (telemetry == null || BooleanUtils.isFalse(telemetry.isEnabled())) {
            return;
        }

        // 根据评估结果类型分别记录：
        // Allowed：记录通过数量和剩余配额，用于容量监控
        if (result.isAllowed()) {
            telemetry.recordAllowed(key, algorithmType, 1, result.getRemainingPermits());
        } else {
            // Rejected：记录拒绝数量和建议等待时间，用于告警和重试策略
            telemetry.recordRejected(key, algorithmType, 1, result.getWaitTimeMs());
        }
    }
}