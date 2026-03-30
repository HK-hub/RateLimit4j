package com.geek.ratelimit4j.starter.resolver;

import com.geek.ratelimit4j.core.config.DimensionType;
import com.geek.ratelimit4j.core.resolver.DimensionResolveContext;
import com.geek.ratelimit4j.core.resolver.DimensionResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * IP维度解析器
 * 从请求中提取客户端真实IP地址
 *
 * <p>解析优先级：</p>
 * <ol>
 *   <li>X-Forwarded-For 请求头（代理场景）</li>
 *   <li>X-Real-IP 请求头</li>
 *   <li>RemoteAddr</li>
 * </ol>
 *
 * <p>自定义示例：</p>
 * <pre>
 * &#64;Bean
 * &#64;ConditionalOnMissingBean
 * public DimensionResolver ipDimensionResolver() {
 *     return new IpDimensionResolver() {
 *         &#64;Override
 *         public String resolve(DimensionResolveContext context) {
 *             // 从自定义请求头获取
 *             String ip = context.getHeader("X-Custom-Client-IP");
 *             if (StringUtils.isNotBlank(ip)) {
 *                 return normalizeIp(ip);
 *             }
 *             return super.resolve(context);
 *         }
 *     };
 * }
 * </pre>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Slf4j
public class IpDimensionResolver implements DimensionResolver {

    protected static final String UNKNOWN = "unknown";
    protected static final String IPV6_LOOPBACK_FULL = "0:0:0:0:0:0:0:1";
    protected static final String IPV6_LOOPBACK_SHORT = "::1";
    protected static final String IPV4_LOOPBACK = "127.0.0.1";

    @Override
    public DimensionType getType() {
        return DimensionType.IP;
    }

    @Override
    public String resolve(DimensionResolveContext context) {
        // 1. X-Forwarded-For（代理场景）
        String ip = context.getHeader("X-Forwarded-For");
        ip = normalizeIp(ip);
        if (!UNKNOWN.equals(ip)) {
            return ip;
        }

        // 2. X-Real-IP
        ip = context.getHeader("X-Real-IP");
        ip = normalizeIp(ip);
        if (!UNKNOWN.equals(ip)) {
            return ip;
        }

        // 3. RemoteAddr
        ip = context.getRemoteAddr();
        if (StringUtils.isNotBlank(ip)) {
            if (IPV6_LOOPBACK_FULL.equals(ip) || IPV6_LOOPBACK_SHORT.equals(ip)) {
                return IPV4_LOOPBACK;
            }
            return ip;
        }

        log.debug("[IpDimensionResolver] IP not found, returning 'unknown'");
        return UNKNOWN;
    }

    protected String normalizeIp(String ip) {
        if (StringUtils.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            return UNKNOWN;
        }
        int index = ip.indexOf(',');
        if (index > 0) {
            ip = ip.substring(0, index);
        }
        return ip.trim();
    }
}