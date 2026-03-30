package com.geek.ratelimit4j.starter.resolver;

import com.geek.ratelimit4j.core.config.DimensionType;
import com.geek.ratelimit4j.core.resolver.DimensionResolveContext;
import com.geek.ratelimit4j.core.resolver.DimensionResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 用户维度解析器
 * 从请求上下文或安全框架中提取用户标识
 *
 * <p>解析优先级：</p>
 * <ol>
 *   <li>方法参数中包含"user"/"userId"关键字的参数</li>
 *   <li>参数类型名包含"User"的对象</li>
 *   <li>Spring Security上下文</li>
 * </ol>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Slf4j
public class UserDimensionResolver implements DimensionResolver {

    protected static final String UNKNOWN = "unknown";

    @Override
    public DimensionType getType() {
        return DimensionType.USER;
    }

    @Override
    public String resolve(DimensionResolveContext context) {
        // 1. 从方法参数获取
        String userId = tryGetFromMethodArgs(context);
        if (StringUtils.isNotBlank(userId)) {
            return userId;
        }

        // 2. 从Spring Security获取
        userId = tryGetFromSpringSecurity();
        if (StringUtils.isNotBlank(userId)) {
            return userId;
        }

        log.debug("[UserDimensionResolver] User ID not found, returning 'unknown'");
        return UNKNOWN;
    }

    protected String tryGetFromMethodArgs(DimensionResolveContext context) {
        Object[] args = context.getMethodArgs();
        String[] paramNames = context.getMethodParameterNames();

        if (Objects.isNull(args) || Objects.isNull(paramNames)) {
            return null;
        }

        for (int i = 0; i < args.length && i < paramNames.length; i++) {
            Object arg = args[i];
            if (Objects.isNull(arg)) {
                continue;
            }

            // 检查参数名
            String paramName = paramNames[i].toLowerCase();
            if (paramName.contains("user") || paramName.contains("userid")) {
                String id = extractId(arg);
                if (StringUtils.isNotBlank(id)) {
                    return id;
                }
            }

            // 检查类型名
            String className = arg.getClass().getSimpleName().toLowerCase();
            if (className.contains("user")) {
                String id = extractId(arg);
                if (StringUtils.isNotBlank(id)) {
                    return id;
                }
            }
        }
        return null;
    }

    protected String tryGetFromSpringSecurity() {
        try {
            Class<?> holderClass = Class.forName(
                    "org.springframework.security.core.context.SecurityContextHolder");
            Object secContext = holderClass.getMethod("getContext").invoke(null);
            Object auth = secContext.getClass().getMethod("getAuthentication").invoke(secContext);

            if (Objects.nonNull(auth)) {
                Object principal = auth.getClass().getMethod("getPrincipal").invoke(auth);
                if (Objects.nonNull(principal)) {
                    if (principal instanceof String) {
                        return (String) principal;
                    }
                    return extractId(principal);
                }
            }
        } catch (ClassNotFoundException e) {
            log.debug("[UserDimensionResolver] Spring Security not available");
        } catch (Exception e) {
            log.debug("[UserDimensionResolver] Failed to get from Spring Security: {}", e.getMessage());
        }
        return null;
    }

    protected String extractId(Object obj) {
        if (Objects.isNull(obj)) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        if (obj instanceof Number) {
            return obj.toString();
        }
        try {
            Method getId = obj.getClass().getMethod("getId");
            Object id = getId.invoke(obj);
            if (Objects.nonNull(id)) {
                return id.toString();
            }
        } catch (Exception e) {
            // 忽略
        }
        return null;
    }
}