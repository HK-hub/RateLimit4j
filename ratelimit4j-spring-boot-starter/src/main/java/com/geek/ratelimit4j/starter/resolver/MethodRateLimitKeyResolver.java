package com.geek.ratelimit4j.starter.resolver;

import com.geek.ratelimit4j.core.resolver.RateLimitResolveContext;
import org.apache.commons.lang3.StringUtils;

/**
 * 方法名Key解析器
 * 基于方法全限定名生成限流Key（默认兜底策略）
 *
 * <p>优先级：4（最低，默认兜底）</p>
 * <p>触发条件：始终支持（作为兜底解析器）</p>
 *
 * <p>职责说明：</p>
 * <ul>
 *   <li>当没有配置其他Key策略时提供默认Key</li>
 *   <li>基于类名和方法名构建唯一Key</li>
 *   <li>保证同一方法始终生成相同Key</li>
 * </ul>
 *
 * <p>Key格式：[keyPrefix:]ClassName:methodName</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 默认使用方法全限定名作为Key
 * &#64;RateLimit(rate = 100)
 * public String api() { ... }
 * // 生成的Key: MyService:api
 *
 * // 带前缀的方法Key
 * &#64;RateLimit(rate = 100, keyPrefix = "myapp")
 * public String api() { ... }
 * // 生成的Key: myapp:MyService:api
 * }</pre>
 *
 * @author RateLimit4j
 * @since 1.0.0
 * @see RateLimitKeyResolver
 */
public class MethodRateLimitKeyResolver implements RateLimitKeyResolver {

    // ==================== 接口方法实现 ====================

    /**
     * {@inheritDoc}
     * 始终返回true，作为默认兜底解析器
     *
     * <p>说明：此解析器作为最后的选择，处理所有未被其他解析器处理的情况</p>
     */
    @Override
    public boolean supports(RateLimitResolveContext context) {
        // 始终支持，作为兜底解析器
        return true;
    }

    /**
     * {@inheritDoc}
     * 基于方法全限定名构建Key
     *
     * <p>处理流程：</p>
     * <ol>
     *   <li>获取keyPrefix、className、methodName</li>
     *   <li>按格式组合：[keyPrefix:]className:methodName</li>
     * </ol>
     *
     * @return 限流Key，格式为 [keyPrefix:]ClassName:methodName
     */
    @Override
    public String resolve(RateLimitResolveContext context) {
        // 获取各组件
        String keyPrefix = context.getKeyPrefix();
        String className = context.getDeclaringClassName();
        String methodName = context.getMethodName();

        // 构建Key
        StringBuilder keyBuilder = new StringBuilder();

        // 添加keyPrefix前缀（如果有）
        if (StringUtils.isNotBlank(keyPrefix)) {
            keyBuilder.append(keyPrefix).append(":");
        }

        // 添加类名和方法名
        keyBuilder.append(className).append(":").append(methodName);

        return keyBuilder.toString();
    }

    /**
     * {@inheritDoc}
     * 返回优先级4（最低优先级，兜底策略）
     */
    @Override
    public int getOrder() {
        return 4;
    }
}