package com.geek.ratelimit4j.core.algorithm;

import com.geek.ratelimit4j.core.config.ModeType;
import com.geek.ratelimit4j.core.config.RateLimitConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 限流算法执行上下文
 * 封装限流判断所需的所有输入信息
 *
 * <p>上下文包含：</p>
 * <ul>
 *   <li>限流Key：标识限流维度（用户ID、IP、接口等）</li>
 *   <li>限流配置：算法类型、速率、周期等</li>
 *   <li>执行模式：本地或分布式</li>
 *   <li>附加属性：用于扩展的自定义属性</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class RateLimitContext {

    /**
     * 限流Key，用于标识限流维度
     * 例如：用户ID、IP地址、接口名、组合Key等
     */
    private final String key;

    /**
     * 限流配置
     */
    private final RateLimitConfig config;

    /**
     * 请求时间戳（毫秒）
     */
    private final long requestTimestamp;

    /**
     * 执行模式：本地或分布式
     */
    private final ModeType modeType;

    /**
     * 附加属性
     * 用于存储扩展信息，如用户ID、IP地址等
     */
    private final Map<String, Object> attributes;

    /**
     * 构造限流上下文
     *
     * @param key 限流Key
     * @param config 限流配置
     * @param requestTimestamp 请求时间戳（毫秒）
     * @param modeType 执行模式
     * @param attributes 附加属性
     */
    public RateLimitContext(String key, RateLimitConfig config, long requestTimestamp,
                            ModeType modeType, Map<String, Object> attributes) {
        this.key = key;
        this.config = config;
        this.requestTimestamp = requestTimestamp;
        this.modeType = modeType;
        this.attributes = Objects.nonNull(attributes) ? attributes : new HashMap<>();
    }

    /**
     * 创建默认上下文（使用当前时间戳）
     *
     * @param key 限流Key
     * @param config 限流配置
     * @param modeType 执行模式
     * @return 限流上下文对象
     */
    public static RateLimitContext of(String key, RateLimitConfig config, ModeType modeType) {
        return new RateLimitContext(key, config, System.currentTimeMillis(),
                                    modeType, new HashMap<>());
    }

    /**
     * 创建带附加属性的上下文
     *
     * @param key 限流Key
     * @param config 限流配置
     * @param modeType 执行模式
     * @param attributes 附加属性
     * @return 限流上下文对象
     */
    public static RateLimitContext of(String key, RateLimitConfig config,
                                       ModeType modeType, Map<String, Object> attributes) {
        return new RateLimitContext(key, config, System.currentTimeMillis(),
                                    modeType, attributes);
    }

    /**
     * 创建Builder用于灵活构建上下文
     *
     * @return Builder对象
     */
    public static Builder builder() {
        return new Builder();
    }

    // Getter方法

    public String getKey() {
        return this.key;
    }

    public RateLimitConfig getConfig() {
        return this.config;
    }

    public long getRequestTimestamp() {
        return this.requestTimestamp;
    }

    public ModeType getModeType() {
        return this.modeType;
    }

    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    /**
     * 获取附加属性值
     *
     * @param name 属性名称
     * @return 属性值，不存在时返回null
     */
    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }

    /**
     * 获取附加属性值（带类型转换）
     *
     * @param name 属性名称
     * @param type 目标类型
     * @return 属性值，不存在或类型不匹配时返回null
     */
    public <T> T getAttribute(String name, Class<T> type) {
        Object value = this.attributes.get(name);
        if (Objects.isNull(value)) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    /**
     * 获取限流速率
     *
     * @return 限流速率，配置为空时返回0
     */
    public int getRate() {
        if (Objects.isNull(config)) {
            return 0;
        }
        return config.getRate();
    }

    /**
     * 获取限流周期
     *
     * @return 限流周期（秒），配置为空时返回1
     */
    public int getPeriod() {
        if (Objects.isNull(config)) {
            return 1;
        }
        return config.getPeriod();
    }

    /**
     * 判断是否为分布式模式
     *
     * @return true表示分布式模式，false表示本地模式
     */
    public boolean isDistributed() {
        return Objects.equals(modeType, ModeType.DISTRIBUTED);
    }

    /**
     * Builder类
     */
    public static class Builder {
        private String key;
        private RateLimitConfig config;
        private long requestTimestamp = System.currentTimeMillis();
        private ModeType modeType = ModeType.LOCAL;
        private Map<String, Object> attributes = new HashMap<>();

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder config(RateLimitConfig config) {
            this.config = config;
            return this;
        }

        public Builder requestTimestamp(long timestamp) {
            this.requestTimestamp = timestamp;
            return this;
        }

        public Builder modeType(ModeType modeType) {
            this.modeType = modeType;
            return this;
        }

        public Builder attribute(String name, Object value) {
            this.attributes.put(name, value);
            return this;
        }

        public Builder attributes(Map<String, Object> attributes) {
            this.attributes.putAll(attributes);
            return this;
        }

        public RateLimitContext build() {
            return new RateLimitContext(key, config, requestTimestamp, modeType, attributes);
        }
    }
}