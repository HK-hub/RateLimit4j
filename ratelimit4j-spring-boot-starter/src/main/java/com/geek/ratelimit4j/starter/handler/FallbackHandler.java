package com.geek.ratelimit4j.starter.handler;

import com.geek.ratelimit4j.core.exception.RateLimitException;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * 限流降级处理器接口
 * 当请求被限流时，调用此处理器执行降级逻辑
 *
 * <p>实现类需要标注为Spring组件（@Component），框架会自动发现并注册。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Component
 * public class DefaultFallbackHandler implements FallbackHandler {
 *     @Override
 *     public Object handle(ProceedingJoinPoint joinPoint, RateLimitException exception) {
 *         // 返回降级响应
 *         return ResponseEntity.status(429).body("Rate limited");
 *     }
 * }
 * }</pre>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public interface FallbackHandler {

    /**
     * 处理限流降级逻辑
     *
     * @param joinPoint 切点信息，包含原方法和参数
     * @param exception 限流异常，包含限流详情
     * @return 降级返回值，应与原方法返回类型兼容
     */
    Object handle(ProceedingJoinPoint joinPoint, RateLimitException exception);

    /**
     * 获取降级处理器名称
     * 用于在注解中指定使用哪个处理器
     *
     * @return 处理器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}