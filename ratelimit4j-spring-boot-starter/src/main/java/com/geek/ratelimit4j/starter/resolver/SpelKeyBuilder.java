package com.geek.ratelimit4j.starter.resolver;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;

/**
 * SpEL表达式Key构建器
 * 支持使用SpEL表达式从方法参数中提取限流Key
 *
 * <p>表达式示例：</p>
 * <ul>
 *   <li>#user.id - 提取用户ID</li>
 *   <li>#request.remoteAddr - 提取IP地址</li>
 *   <li>#request.getHeader('X-Token') - 提取Header</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Component
public class SpelKeyBuilder implements KeyBuilder {

    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Override
    public String build(ProceedingJoinPoint joinPoint, String expression) {
        if (StringUtils.isBlank(expression)) {
            return buildDefaultKey(joinPoint);
        }

        try {
            EvaluationContext context = createEvaluationContext(joinPoint);
            Expression exp = parser.parseExpression(expression);
            Object value = exp.getValue(context);

            return Objects.nonNull(value) ? value.toString() : "null";
        } catch (Exception e) {
            return buildDefaultKey(joinPoint);
        }
    }

    @Override
    public String getName() {
        return "spel";
    }

    /**
     * 构建默认Key（基于方法签名）
     */
    private String buildDefaultKey(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        return className + ":" + methodName;
    }

    /**
     * 创建SpEL评估上下文
     */
    private EvaluationContext createEvaluationContext(ProceedingJoinPoint joinPoint) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = method.getParameters();

        if (Objects.nonNull(args) && Objects.nonNull(parameters)) {
            for (int i = 0; i < args.length && i < parameters.length; i++) {
                context.setVariable(parameters[i].getName(), args[i]);
            }
        }

        context.setVariable("method", method.getName());
        context.setVariable("className", method.getDeclaringClass().getSimpleName());
        context.setVariable("args", args);

        return context;
    }
}