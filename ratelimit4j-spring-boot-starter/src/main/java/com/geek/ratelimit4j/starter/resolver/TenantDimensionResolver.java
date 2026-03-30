package com.geek.ratelimit4j.starter.resolver;

import com.geek.ratelimit4j.core.config.DimensionType;
import com.geek.ratelimit4j.core.resolver.DimensionResolveContext;
import com.geek.ratelimit4j.core.resolver.DimensionResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 租户维度解析器
 * 从请求头或方法参数中提取租户标识
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Slf4j
public class TenantDimensionResolver implements DimensionResolver {

    protected static final String UNKNOWN = "unknown";

    @Override
    public DimensionType getType() {
        return DimensionType.TENANT;
    }

    @Override
    public String resolve(DimensionResolveContext context) {
        // 1. 从Header获取
        String tenantId = context.getHeader("X-Tenant-Id");
        if (StringUtils.isBlank(tenantId)) {
            tenantId = context.getHeader("Tenant-Id");
        }
        if (StringUtils.isNotBlank(tenantId)) {
            return tenantId;
        }

        // 2. 从方法参数获取
        tenantId = tryGetFromMethodArgs(context);
        if (StringUtils.isNotBlank(tenantId)) {
            return tenantId;
        }

        log.debug("[TenantDimensionResolver] Tenant ID not found, returning 'unknown'");
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

            String paramName = paramNames[i].toLowerCase();
            if (paramName.contains("tenant")) {
                if (arg instanceof String) {
                    return (String) arg;
                }
                return arg.toString();
            }
        }
        return null;
    }
}