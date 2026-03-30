package com.geek.ratelimit4j.starter.resolver;

import com.geek.ratelimit4j.core.resolver.RateLimitResolveContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * SpEL表达式Key解析器
 * 使用SpEL表达式从方法参数提取限流Key
 *
 * <p>优先级：2</p>
 * <p>触发条件：rateLimit.keys() 不为空</p>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class SpelRateLimitKeyResolver implements RateLimitKeyResolver {

    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Override
    public boolean supports(RateLimitResolveContext context) {
        return context.hasKeys();
    }

    @Override
    public String resolve(RateLimitResolveContext context) {
        String[] keys = context.getKeys();
        String keyPrefix = context.getKeyPrefix();

        StringBuilder combinedKey = new StringBuilder();

        if (StringUtils.isNotBlank(keyPrefix)) {
            combinedKey.append(keyPrefix).append(":");
        }

        for (int i = 0; i < keys.length; i++) {
            String resolvedKey = evaluateSpelExpression(context, keys[i]);
            combinedKey.append(resolvedKey);

            if (i < keys.length - 1) {
                combinedKey.append(":");
            }
        }

        return combinedKey.toString();
    }

    private String evaluateSpelExpression(RateLimitResolveContext context, String expression) {
        if (StringUtils.isBlank(expression)) {
            return buildDefaultKey(context);
        }

        try {
            EvaluationContext evalContext = createEvaluationContext(context);
            Expression exp = parser.parseExpression(expression);
            Object value = exp.getValue(evalContext);

            return Objects.nonNull(value) ? value.toString() : "null";
        } catch (Exception e) {
            return buildDefaultKey(context);
        }
    }

    private EvaluationContext createEvaluationContext(RateLimitResolveContext context) {
        StandardEvaluationContext evalContext = new StandardEvaluationContext();

        Method method = context.getMethod();
        Object[] args = context.getMethodArgs();
        String[] parameterNames = context.getMethodParameterNames();

        if (args != null && parameterNames != null) {
            for (int i = 0; i < args.length && i < parameterNames.length; i++) {
                evalContext.setVariable(parameterNames[i], args[i]);
                evalContext.setVariable("p" + i, args[i]);
                evalContext.setVariable("a" + i, args[i]);
            }
        }

        evalContext.setVariable("method", method.getName());
        evalContext.setVariable("className", method.getDeclaringClass().getSimpleName());
        evalContext.setVariable("args", args);

        return evalContext;
    }

    private String buildDefaultKey(RateLimitResolveContext context) {
        return context.getDeclaringClassName() + ":" + context.getMethodName();
    }

    @Override
    public int getOrder() {
        return 2;
    }
}