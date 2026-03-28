package com.geek.ratelimit4j.starter.resolver;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * 限流Key构建器接口
 * 用于自定义限流Key的构建逻辑
 *
 * <p>实现类需要标注为Spring组件（@Component），框架会自动发现并注册。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Component
 * public class UserIdKeyBuilder implements KeyBuilder {
 *     @Override
 *     public String build(ProceedingJoinPoint joinPoint, String expression) {
 *         // 从方法参数中提取用户ID
 *         Object[] args = joinPoint.getArgs();
 *         // ...
 *         return "user:" + userId;
 *     }
 * }
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
     * @param expression Key表达式（来自注解配置）
     * @return 构建后的限流Key
     */
    String build(ProceedingJoinPoint joinPoint, String expression);

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