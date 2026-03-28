package com.geek.ratelimit4j.core.config;

import lombok.Getter;

/**
 * 限流维度类型枚举
 * 定义限流Key的提取维度
 *
 * <p>维度类型：</p>
 * <ul>
 *   <li>METHOD - 方法全限定名（默认）</li>
 *   <li>IP - 客户端IP地址</li>
 *   <li>USER - 用户ID</li>
 *   <li>TENANT - 租户ID</li>
 *   <li>DEVICE - 设备ID</li>
 *   <li>CUSTOM - 自定义维度</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Getter
public enum DimensionType {

    /**
     * 方法全限定名维度
     * 格式：com.xxx.xxx.ClassName#methodName
     * 默认维度，用于方法级别的限流
     */
    METHOD("method", "方法全限定名"),

    /**
     * IP地址维度
     * 从请求中提取客户端IP地址
     * 用于基于IP的限流
     */
    IP("ip", "IP地址"),

    /**
     * 用户ID维度
     * 从请求上下文中提取用户ID
     * 用于基于用户的限流
     */
    USER("user", "用户ID"),

    /**
     * 租户ID维度
     * 从请求上下文中提取租户ID
     * 用于多租户场景的限流
     */
    TENANT("tenant", "租户ID"),

    /**
     * 设备ID维度
     * 从请求中提取设备标识
     * 用于基于设备的限流
     */
    DEVICE("device", "设备ID"),

    /**
     * 自定义维度
     * 使用keys属性中定义的SpEL表达式
     */
    CUSTOM("custom", "自定义");

    /**
     * 维度编码
     */
    private final String code;

    /**
     * 维度描述
     */
    private final String description;

    /**
     * 构造函数
     *
     * @param code        维度编码
     * @param description 维度描述
     */
    DimensionType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据编码获取维度类型
     *
     * @param code 维度编码
     * @return 维度类型，未找到时返回METHOD
     */
    public static DimensionType fromCode(String code) {
        // 遍历所有维度类型
        for (DimensionType type : values()) {
            // 匹配编码（忽略大小写）
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        // 未找到时返回METHOD（默认）
        return METHOD;
    }
}