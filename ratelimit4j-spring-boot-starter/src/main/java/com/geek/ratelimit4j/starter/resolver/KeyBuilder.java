package com.geek.ratelimit4j.starter.resolver;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * 限流Key构建器接口
 * 用于自定义限流Key的构建逻辑
 *
 * <p>Key提取优先级：</p>
 * <ol>
 *   <li>keys属性 - SpEL表达式</li>
 *   <li>keyBuilder - 自定义Key构建器</li>
 *   <li>dimension - 预定义维度</li>
 *   <li>默认 - 方法全限定名</li>
 * </ol>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Component
 * public class UserIdKeyBuilder implements KeyBuilder {
 *     @Override
 *     public String build(ProceedingJoinPoint joinPoint) {
 *         // 从方法参数中提取用户ID
 *         Object[] args = joinPoint.getArgs();
 *         // ...
 *         return "user:" + userId;
 *     }
 *
 *     @Override
 *     public String getName() {
 *         return "userId";
 *     }
 * }
 *
 * // 使用
 * @RateLimit(rate = 100, keyBuilder = UserIdKeyBuilder.class)
 * public String api(User user) { ... }
 * }</pre>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public interface KeyBuilder {

    /**
     * 构建限流Key
     *
     * @param joinPoint 切点信息
     * @return 构建后的限流Key
     */
    String build(ProceedingJoinPoint joinPoint);

    /**
     * 获取Key构建器名称
     * 用于在注解中指定使用哪个构建器
     *
     * @return 构建器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}