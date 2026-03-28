package com.geek.ratelimit4j.core.config;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;

import java.util.Objects;

/**
 * 限流配置类
 * 封装限流器的基本配置参数，支持Builder模式灵活构建
 *
 * <p>配置项说明：</p>
 * <ul>
 *   <li>name：限流器名称，用于标识和查找</li>
 *   <li>algorithmType：限流算法类型</li>
 *   <li>rate：每周期允许的请求数量</li>
 *   <li>period：限流周期（秒）</li>
 *   <li>keyPrefix：限流Key前缀</li>
 *   <li>maxBurst：最大突发容量（令牌桶算法）</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class RateLimitConfig {

    /**
     * 限流器名称，用于标识和查找
     */
    private final String name;

    /**
     * 限流算法类型
     */
    private final AlgorithmType algorithmType;

    /**
     * 每周期允许的请求数量（限流速率）
     * 例如：rate=100, period=1 表示每秒100个请求
     */
    private final int rate;

    /**
     * 限流周期（秒）
     */
    private final int period;

    /**
     * 限流Key前缀
     * 用于构建完整的限流Key，例如："user:api"
     */
    private final String keyPrefix;

    /**
     * 最大突发容量（仅令牌桶算法使用）
     * 表示桶的最大容量，允许的突发请求数量
     */
    private final int maxBurst;

    /**
     * 构造限流配置
     *
     * @param name 限流器名称
     * @param algorithmType 算法类型
     * @param rate 限流速率
     * @param period 限流周期（秒）
     * @param keyPrefix Key前缀
     * @param maxBurst 最大突发容量
     */
    public RateLimitConfig(String name, AlgorithmType algorithmType,
                           int rate, int period, String keyPrefix, int maxBurst) {
        this.name = name;
        this.algorithmType = algorithmType;
        this.rate = rate;
        this.period = period;
        this.keyPrefix = keyPrefix;
        this.maxBurst = maxBurst;
    }

    /**
     * 创建默认配置（令牌桶算法，100QPS）
     *
     * @param name 限流器名称
     * @return 默认配置对象
     */
    public static RateLimitConfig defaultConfig(String name) {
        return new RateLimitConfig(name, AlgorithmType.TOKEN_BUCKET,
                                   100, 1, "", 100);
    }

    /**
     * 创建Builder用于灵活配置
     *
     * @return Builder对象
     */
    public static Builder builder() {
        return new Builder();
    }

    // Getter方法

    public String getName() {
        return this.name;
    }

    public AlgorithmType getAlgorithmType() {
        return this.algorithmType;
    }

    public int getRate() {
        return this.rate;
    }

    public int getPeriod() {
        return this.period;
    }

    public String getKeyPrefix() {
        return this.keyPrefix;
    }

    public int getMaxBurst() {
        return this.maxBurst;
    }

    /**
     * 获取完整的限流Key（前缀 + 原始Key）
     *
     * @param originalKey 原始Key
     * @return 完整Key，前缀为空时返回原始Key
     */
    public String buildKey(String originalKey) {
        if (Objects.isNull(keyPrefix) || keyPrefix.isEmpty()) {
            return originalKey;
        }
        return keyPrefix + ":" + originalKey;
    }

    /**
     * 获取每毫秒的速率（用于令牌桶补充计算）
     *
     * @return 每毫秒速率
     */
    public double getRatePerMs() {
        return (double) rate / (period * 1000.0);
    }

    /**
     * 获取周期毫秒数
     *
     * @return 周期毫秒数
     */
    public long getPeriodMs() {
        return period * 1000L;
    }

    /**
     * Builder类
     */
    public static class Builder {
        private String name = "default";
        private AlgorithmType algorithmType = AlgorithmType.TOKEN_BUCKET;
        private int rate = 100;
        private int period = 1;
        private String keyPrefix = "";
        private int maxBurst = 100;

        /**
         * 设置限流器名称
         *
         * @param name 名称
         * @return Builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * 设置算法类型
         *
         * @param algorithmType 算法类型
         * @return Builder
         */
        public Builder algorithmType(AlgorithmType algorithmType) {
            this.algorithmType = algorithmType;
            return this;
        }

        /**
         * 设置限流速率
         *
         * @param rate 每周期请求数
         * @return Builder
         */
        public Builder rate(int rate) {
            this.rate = rate;
            return this;
        }

        /**
         * 设置限流周期
         *
         * @param period 周期（秒）
         * @return Builder
         */
        public Builder period(int period) {
            this.period = period;
            return this;
        }

        /**
         * 设置Key前缀
         *
         * @param keyPrefix Key前缀
         * @return Builder
         */
        public Builder keyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
            return this;
        }

        /**
         * 设置最大突发容量
         *
         * @param maxBurst 最大突发容量
         * @return Builder
         */
        public Builder maxBurst(int maxBurst) {
            this.maxBurst = maxBurst;
            return this;
        }

        /**
         * 构建配置对象
         *
         * @return RateLimitConfig
         */
        public RateLimitConfig build() {
            return new RateLimitConfig(name, algorithmType, rate, period, keyPrefix, maxBurst);
        }
    }
}