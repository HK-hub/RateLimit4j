package com.geek.ratelimit4j.starter.resolver;

import com.geek.ratelimit4j.core.config.DimensionType;
import com.geek.ratelimit4j.core.resolver.DimensionResolveContext;
import com.geek.ratelimit4j.core.resolver.DimensionResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 设备维度解析器
 * 从请求头或方法参数中提取设备标识
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Slf4j
public class DeviceDimensionResolver implements DimensionResolver {

    protected static final String UNKNOWN = "unknown";

    @Override
    public DimensionType getType() {
        return DimensionType.DEVICE;
    }

    @Override
    public String resolve(DimensionResolveContext context) {
        // 1. 从Header获取
        String deviceId = context.getHeader("X-Device-Id");
        if (StringUtils.isBlank(deviceId)) {
            deviceId = context.getHeader("Device-Id");
        }
        if (StringUtils.isNotBlank(deviceId)) {
            return deviceId;
        }

        // 2. 从User-Agent生成设备标识
        String userAgent = context.getHeader("User-Agent");
        if (StringUtils.isNotBlank(userAgent)) {
            return String.valueOf(userAgent.hashCode());
        }

        // 3. 从方法参数获取
        deviceId = tryGetFromMethodArgs(context);
        if (StringUtils.isNotBlank(deviceId)) {
            return deviceId;
        }

        log.debug("[DeviceDimensionResolver] Device ID not found, returning 'unknown'");
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
            if (paramName.contains("device")) {
                if (arg instanceof String) {
                    return (String) arg;
                }
                return arg.toString();
            }
        }
        return null;
    }
}