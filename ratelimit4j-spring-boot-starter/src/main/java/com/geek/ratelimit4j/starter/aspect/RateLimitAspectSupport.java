package com.geek.ratelimit4j.starter.aspect;

import com.geek.ratelimit4j.core.annotation.RateLimit;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.List;

/**
 * 限流切面支持接口
 * 定义限流切面的核心契约
 *
 * <p>设计说明：</p>
 * <ul>
 *   <li>接口定义核心方法，便于扩展</li>
 *   <li>支持用户自定义切面实现</li>
 *   <li>遵循开闭原则：对扩展开放，对修改关闭</li>
 * </ul>
 *
 * <p>实现类层次：</p>
 * <pre>
 * RateLimitAspectSupport（接口）
 * └── AbstractRateLimitAspect（抽象类）
 *     └── DefaultRateLimitAspect（具体实现）
 * </pre>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 自定义切面实现
 * public class CustomRateLimitAspect extends AbstractRateLimitAspect {
 *     // 实现自定义逻辑...
 * }
 * }</pre>
 *
 * @author RateLimit4j
 * @since 1.0.0
 * @see AbstractRateLimitAspect
 * @see DefaultRateLimitAspect
 */
public interface RateLimitAspectSupport {

    /**
     * 处理限流逻辑
     *
     * <p>核心职责：</p>
     * <ul>
     *   <li>解析限流注解</li>
     *   <li>执行限流判断</li>
     *   <li>处理限流结果（通过/拒绝）</li>
     * </ul>
     *
     * @param joinPoint AspectJ切点对象，包含被拦截方法的信息
     * @return 方法执行结果，可能是原方法返回值或降级处理结果
     * @throws Throwable 方法执行过程中抛出的任何异常
     */
    Object processRateLimits(ProceedingJoinPoint joinPoint, List<RateLimit> rateLimits) throws Throwable;
}