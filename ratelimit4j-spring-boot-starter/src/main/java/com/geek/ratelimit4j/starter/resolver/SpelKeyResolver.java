package com.geek.ratelimit4j.starter.resolver;

import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * SpEL表达式Key解析器
 * 支持使用SpEL表达式从方法参数中提取限流Key
 *
 * <p>表达式示例：</p>
 * <ul>
 *   <li>#user.id - 提取用户ID</li>
 *   <li>#request.getRemoteAddr() - 提取IP地址</li>
 *   <li>#request.getHeader('X-Token') - 提取Header</li>
 *   <li>#user.id + ':' + #method - 组合Key</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class SpelKeyResolver implements KeyResolver {

    /**
     * SpEL表达式解析器
     */
    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * 默认Key解析器（表达式为空时使用）
     */
    private final KeyResolver defaultResolver = new DefaultKeyResolver();

    @Override
    public String resolve(Method method, Object[] args, String keyExpression, String keyPrefix) {
        if (StringUtils.isBlank(keyExpression)) {
            return defaultResolver.resolve(method, args, keyExpression, keyPrefix);
        }

        try {
            EvaluationContext context = createEvaluationContext(method, args);
            Expression expression = parser.parseExpression(keyExpression);
            Object value = expression.getValue(context);

            StringBuilder keyBuilder = new StringBuilder();

            if (StringUtils.isNotBlank(keyPrefix)) {
                keyBuilder.append(keyPrefix).append(":");
            }

            if (Objects.nonNull(value)) {
                keyBuilder.append(value.toString());
            } else {
                keyBuilder.append("null");
            }

            return keyBuilder.toString();
        } catch (Exception e) {
            return defaultResolver.resolve(method, args, keyExpression, keyPrefix);
        }
    }

    /**
     * 创建SpEL评估上下文
     *
     * @param method 目标方法
     * @param args 方法参数
     * @return 评估上下文
     */
    private EvaluationContext createEvaluationContext(Method method, Object[] args) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        String[] parameterNames = getParameterNames(method);

        if (Objects.nonNull(args) && Objects.nonNull(parameterNames)) {
            for (int i = 0; i < args.length && i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        context.setVariable("method", method.getName());
        context.setVariable("className", method.getDeclaringClass().getSimpleName());

        return context;
    }

    /**
     * 获取方法参数名称
     *
     * @param method 目标方法
     * @return 参数名称数组
     */
    private String[] getParameterNames(Method method) {
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        String[] names = new String[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            names[i] = parameters[i].getName();
        }
        return names;
    }

    @Override
    public String getName() {
        return "spel";
    }
}