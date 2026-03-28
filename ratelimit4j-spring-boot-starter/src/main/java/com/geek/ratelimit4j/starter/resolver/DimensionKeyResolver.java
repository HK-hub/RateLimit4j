package com.geek.ratelimit4j.starter.resolver;

import com.geek.ratelimit4j.core.config.DimensionType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;

/**
 * 维度Key解析器
 * 根据维度类型从请求上下文中提取限流Key
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Slf4j
public class DimensionKeyResolver {

    // ==================== 常量定义 ====================

    /**
     * IPv6本地回环地址（完整形式）
     */
    private static final String IPV6_LOOPBACK_FULL = "0:0:0:0:0:0:0:1";

    /**
     * IPv6本地回环地址（简化形式）
     */
    private static final String IPV6_LOOPBACK_SHORT = "::1";

    /**
     * IPv4本地回环地址
     */
    private static final String IPV4_LOOPBACK = "127.0.0.1";

    /**
     * 默认未知值
     */
    private static final String UNKNOWN = "unknown";

    /**
     * 解析维度Key
     *
     * @param joinPoint 切点信息
     * @param dimension  维度类型
     * @return 维度Key值
     */
    public String resolve(ProceedingJoinPoint joinPoint, DimensionType dimension) {
        // 根据维度类型选择解析方法
        switch (dimension) {
            case IP:
                // 解析IP地址
                return resolveIp(joinPoint);
            case USER:
                // 解析用户ID
                return resolveUser(joinPoint);
            case TENANT:
                // 解析租户ID
                return resolveTenant(joinPoint);
            case DEVICE:
                // 解析设备ID
                return resolveDevice(joinPoint);
            case METHOD:
            case CUSTOM:
            default:
                // 返回方法全限定名
                return resolveMethod(joinPoint);
        }
    }

    /**
     * 解析IP地址
     * 从HttpServletRequest中提取客户端真实IP
     */
    private String resolveIp(ProceedingJoinPoint joinPoint) {
        // 尝试从方法参数中获取HttpServletRequest
        HttpServletRequest request = findRequest(joinPoint);
        
        // 如果找不到request对象，返回unknown
        if (Objects.isNull(request)) {
            // 使用debug级别避免日志泛滥
            log.debug("[DimensionKeyResolver] HttpServletRequest not found, using 'unknown' for IP dimension");
            return UNKNOWN;
        }

        // 尝试从Header中获取真实IP（支持代理场景）
        String ip = request.getHeader("X-Forwarded-For");
        
        // X-Forwarded-For可能包含多个IP，取第一个
        if (StringUtils.isNotBlank(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
            int index = ip.indexOf(',');
            if (index > 0) {
                ip = ip.substring(0, index);
            }
            return ip.trim();
        }

        // 尝试其他Header
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.isNotBlank(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        // 使用RemoteAddr
        ip = request.getRemoteAddr();
        
        // 本地回环地址转换为真实IP（使用常量避免魔法值）
        if (IPV6_LOOPBACK_FULL.equals(ip) || IPV6_LOOPBACK_SHORT.equals(ip)) {
            ip = IPV4_LOOPBACK;
        }
        
        return ip;
    }

    /**
     * 解析用户ID
     * 从方法参数或SecurityContext中提取
     */
    private String resolveUser(ProceedingJoinPoint joinPoint) {
        // 获取方法参数
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();

        // 遍历参数查找用户相关对象
        if (Objects.nonNull(args) && Objects.nonNull(parameters)) {
            for (int i = 0; i < args.length && i < parameters.length; i++) {
                Object arg = args[i];
                if (Objects.isNull(arg)) {
                    continue;
                }

                // 检查参数名
                String paramName = parameters[i].getName().toLowerCase();
                if (paramName.contains("user") || paramName.contains("userid")) {
                    // 尝试获取ID
                    String userId = extractId(arg);
                    if (StringUtils.isNotBlank(userId)) {
                        return userId;
                    }
                }

                // 检查参数类型
                String className = arg.getClass().getSimpleName().toLowerCase();
                if (className.contains("user")) {
                    String userId = extractId(arg);
                    if (StringUtils.isNotBlank(userId)) {
                        return userId;
                    }
                }
            }
        }

        // 尝试从Spring Security上下文获取
        try {
            // 使用反射避免强依赖Spring Security
            Class<?> securityContextHolderClass = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            Object context = securityContextHolderClass.getMethod("getContext").invoke(null);
            Object authentication = context.getClass().getMethod("getAuthentication").invoke(context);
            
            if (Objects.nonNull(authentication)) {
                Object principal = authentication.getClass().getMethod("getPrincipal").invoke(authentication);
                if (Objects.nonNull(principal)) {
                    // 尝试获取用户名
                    if (principal instanceof String) {
                        return (String) principal;
                    }
                    // 尝试获取ID
                    String userId = extractId(principal);
                    if (StringUtils.isNotBlank(userId)) {
                        return userId;
                    }
                }
            }
        } catch (Exception e) {
            // Spring Security不可用，忽略
            log.debug("[DimensionKeyResolver] Spring Security not available: {}", e.getMessage());
        }

        log.debug("[DimensionKeyResolver] User ID not found, using 'unknown'");
        return UNKNOWN;
    }

    /**
     * 解析租户ID
     * 从方法参数或Header中提取
     */
    private String resolveTenant(ProceedingJoinPoint joinPoint) {
        // 尝试从Header获取租户ID
        HttpServletRequest request = findRequest(joinPoint);
        
        if (Objects.nonNull(request)) {
            // 尝试常见的租户Header
            String tenantId = request.getHeader("X-Tenant-Id");
            if (StringUtils.isNotBlank(tenantId)) {
                return tenantId;
            }
            
            tenantId = request.getHeader("Tenant-Id");
            if (StringUtils.isNotBlank(tenantId)) {
                return tenantId;
            }
        }

        // 尝试从方法参数获取
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();

        if (Objects.nonNull(args) && Objects.nonNull(parameters)) {
            for (int i = 0; i < args.length && i < parameters.length; i++) {
                Object arg = args[i];
                if (Objects.isNull(arg)) {
                    continue;
                }

                String paramName = parameters[i].getName().toLowerCase();
                if (paramName.contains("tenant") || paramName.contains("tenantid")) {
                    String tenantId = extractId(arg);
                    if (StringUtils.isNotBlank(tenantId)) {
                        return tenantId;
                    }
                    // 直接返回字符串值
                    return arg.toString();
                }
            }
        }

        log.debug("[DimensionKeyResolver] Tenant ID not found, using 'unknown'");
        return UNKNOWN;
    }

    /**
     * 解析设备ID
     * 从Header或方法参数中提取
     */
    private String resolveDevice(ProceedingJoinPoint joinPoint) {
        // 尝试从Header获取设备ID
        HttpServletRequest request = findRequest(joinPoint);
        
        if (Objects.nonNull(request)) {
            // 尝试常见的设备Header
            String deviceId = request.getHeader("X-Device-Id");
            if (StringUtils.isNotBlank(deviceId)) {
                return deviceId;
            }
            
            deviceId = request.getHeader("Device-Id");
            if (StringUtils.isNotBlank(deviceId)) {
                return deviceId;
            }
            
            // 尝试User-Agent作为设备标识
            String userAgent = request.getHeader("User-Agent");
            if (StringUtils.isNotBlank(userAgent)) {
                // 使用User-Agent的hash作为设备标识
                return String.valueOf(userAgent.hashCode());
            }
        }

        // 尝试从方法参数获取
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();

        if (Objects.nonNull(args) && Objects.nonNull(parameters)) {
            for (int i = 0; i < args.length && i < parameters.length; i++) {
                Object arg = args[i];
                if (Objects.isNull(arg)) {
                    continue;
                }

                String paramName = parameters[i].getName().toLowerCase();
                if (paramName.contains("device") || paramName.contains("deviceid")) {
                    String deviceId = extractId(arg);
                    if (StringUtils.isNotBlank(deviceId)) {
                        return deviceId;
                    }
                    return arg.toString();
                }
            }
        }

        log.debug("[DimensionKeyResolver] Device ID not found, using 'unknown'");
        return "unknown";
    }

    /**
     * 解析方法全限定名
     */
    private String resolveMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();
        return className + "#" + methodName;
    }

    /**
     * 从方法参数中查找HttpServletRequest
     */
    private HttpServletRequest findRequest(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        
        if (Objects.isNull(args)) {
            return null;
        }

        // 遍历参数查找HttpServletRequest类型
        for (Object arg : args) {
            if (arg instanceof HttpServletRequest) {
                return (HttpServletRequest) arg;
            }
        }

        return null;
    }

    /**
     * 从对象中提取ID
     * 尝试调用getId()方法
     */
    private String extractId(Object obj) {
        if (Objects.isNull(obj)) {
            return null;
        }

        // 如果是字符串或数字，直接返回
        if (obj instanceof String) {
            return (String) obj;
        }
        if (obj instanceof Number) {
            return obj.toString();
        }

        // 尝试调用getId()方法
        try {
            Method getIdMethod = obj.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(obj);
            if (Objects.nonNull(id)) {
                return id.toString();
            }
        } catch (NoSuchMethodException e) {
            // 没有getId方法，忽略
        } catch (Exception e) {
            log.debug("[DimensionKeyResolver] Failed to extract ID: {}", e.getMessage());
        }

        // 尝试调用getUserId()方法
        try {
            Method getUserIdMethod = obj.getClass().getMethod("getUserId");
            Object id = getUserIdMethod.invoke(obj);
            if (Objects.nonNull(id)) {
                return id.toString();
            }
        } catch (Exception e) {
            // 忽略
        }

        return null;
    }
}