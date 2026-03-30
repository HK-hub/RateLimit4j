package com.geek.ratelimit4j.core.annotation;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import com.geek.ratelimit4j.core.config.DimensionType;
import com.geek.ratelimit4j.core.config.EngineType;
import com.geek.ratelimit4j.core.exception.RateLimitException;
import com.geek.ratelimit4j.core.handler.FallbackHandler;
import com.geek.ratelimit4j.core.resolver.KeyBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解
 * 用于标注需要限流的方法或类，支持声明式限流配置
 *
 * <p>Key提取优先级：</p>
 * <ol>
 *   <li>keys属性 - 使用SpEL表达式提取</li>
 *   <li>keyBuilder - 使用自定义Key构建器</li>
 *   <li>dimension - 使用预定义维度（IP、USER等）</li>
 *   <li>默认 - 使用方法全限定名</li>
 * </ol>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 基于方法全限定名限流（默认）
 * @RateLimit(rate = 100, period = 1)
 * public String api() { ... }
 *
 * // 自定义Key（SpEL表达式）- 最高优先级
 * @RateLimit(keys = {"#user.id"}, rate = 50)
 * public String api(User user) { ... }
 *
 * // 自定义Key构建器 - 第二优先级
 * @RateLimit(keyBuilder = UserKeyBuilder.class, rate = 100)
 * public String api(User user) { ... }
 *
 * // 基于IP维度 - 第三优先级
 * @RateLimit(rate = 10, period = 1, dimension = DimensionType.IP)
 * public String ipApi(HttpServletRequest request) { ... }
 *
 * // 多规则限流
 * @RateLimit(rate = 1, period = 3, dimension = DimensionType.USER)
 * @RateLimit(rate = 10, period = 60, dimension = DimensionType.USER)
 * public String multiApi() { ... }
 * }</pre>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(RateLimits.class)
public @interface RateLimit {

    /**
     * 限流Key数组（支持SpEL表达式）
     * 最高优先级，指定后忽略keyBuilder和dimension属性
     *
     * <p>表达式示例：</p>
     * <ul>
     *   <li>#user.id - 提取用户ID</li>
     *   <li>#request.remoteAddr - 提取IP地址</li>
     *   <li>#p0, #a0 - 提取第一个参数</li>
     * </ul>
     *
     * @return Key表达式数组，为空时使用keyBuilder属性
     */
    String[] keys() default {};

    /**
     * 自定义Key构建器
     * 第二优先级，keys为空时生效
     * 实现KeyBuilder接口并标注为@Component
     *
     * @return Key构建器类型，为空时使用dimension属性
     */
    Class<? extends KeyBuilder> keyBuilder() default KeyBuilder.class;

    /**
     * 限流维度
     * 第三优先级，keys和keyBuilder都为空时生效
     * 预定义的Key提取维度
     *
     * @return 维度类型，默认为METHOD（方法全限定名）
     */
    DimensionType dimension() default DimensionType.METHOD;

    /**
     * 限流Key前缀
     * 用于区分不同业务场景的限流Key
     *
     * @return Key前缀，默认为空
     */
    String keyPrefix() default "";

    /**
     * 限流算法类型
     *
     * @return 算法类型，默认令牌桶
     */
    AlgorithmType algorithm() default AlgorithmType.TOKEN_BUCKET;

    /**
     * 限流引擎类型
     * LOCAL：本地内存限流（单机）
     * REDIS：分布式限流（基于Redis）
     * AUTO：自动选择（优先使用配置的primary引擎）
     *
     * @return 引擎类型，默认AUTO
     */
    EngineType engine() default EngineType.AUTO;

    /**
     * 每周期允许的请求数
     *
     * @return 限流速率，默认100
     */
    int rate() default 100;

    /**
     * 限流周期（秒）
     *
     * @return 周期秒数，默认1秒
     */
    int period() default 1;

    /**
     * 自定义降级处理器
     * 实现FallbackHandler接口并标注为@Component
     * 为空时不执行降级处理，直接抛出异常
     *
     * @return 降级处理器类型，默认为空
     */
    Class<? extends FallbackHandler> fallbackHandler() default FallbackHandler.class;

    /**
     * 自定义限流异常类型
     *
     * @return 异常类型
     */
    Class<? extends RuntimeException> exceptionClass() default RateLimitException.class;

    /**
     * 最大突发容量（仅令牌桶算法）
     *
     * @return 最大突发数，默认为0表示使用rate值
     */
    int maxBurst() default 0;

    /**
     * 是否启用限流
     *
     * @return true表示启用，默认启用
     */
    boolean enabled() default true;
}