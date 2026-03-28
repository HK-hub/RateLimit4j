package com.geek.ratelimit4j.local.algorithm;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import com.geek.ratelimit4j.core.algorithm.RateLimitAlgorithm;
import com.geek.ratelimit4j.core.config.RateLimitContext;
import com.geek.ratelimit4j.core.algorithm.RateLimitResult;
import com.geek.ratelimit4j.core.config.ModeType;
import com.geek.ratelimit4j.core.config.RateLimitConfig;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 本地令牌桶算法实现
 *
 * <p>算法原理：</p>
 * <ul>
 *   <li>以固定速率向桶中添加令牌</li>
 *   <li>请求需要获取令牌才能通过</li>
 *   <li>桶满时丢弃多余令牌</li>
 *   <li>支持突发流量处理</li>
 * </ul>
 *
 * <p>实现特点：</p>
 * <ul>
 *   <li>使用AtomicLong保证线程安全</li>
 *   <li>基于时间戳计算令牌补充数量</li>
 *   <li>支持最大突发容量限制</li>
 *   <li>每个Key独立维护令牌桶状态</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class LocalTokenBucketAlgorithm implements RateLimitAlgorithm {

    /**
     * 限流配置
     */
    private final RateLimitConfig config;

    /**
     * 每毫秒补充的令牌数量
     * rate为每秒速率，除以1000得到每毫秒速率
     */
    private final double tokensPerMs;

    /**
     * 令牌桶状态存储
     * Key -> TokenBucketState
     */
    private final ConcurrentHashMap<String, TokenBucketState> buckets;

    /**
     * 构造令牌桶算法
     *
     * @param config 限流配置
     * @throws IllegalArgumentException 当config为null时抛出
     */
    public LocalTokenBucketAlgorithm(RateLimitConfig config) {
        if (Objects.isNull(config)) {
            throw new IllegalArgumentException("RateLimitConfig cannot be null");
        }

        this.config = config;
        this.tokensPerMs = config.getRatePerMs();
        this.buckets = new ConcurrentHashMap<>();
    }

    /**
     * 执行限流判断（单许可）
     */
    @Override
    public RateLimitResult evaluate(RateLimitContext context) {
        return evaluate(context, 1);
    }

    /**
     * 执行限流判断（多许可）
     */
    @Override
    public RateLimitResult evaluate(RateLimitContext context, int permits) {
        if (Objects.isNull(context)) {
            throw new IllegalArgumentException("RateLimitContext cannot be null");
        }

        if (permits <= 0) {
            throw new IllegalArgumentException("Permits must be positive");
        }

        String key = context.getKey();
        long startTime = System.nanoTime();

        TokenBucketState state = buckets.computeIfAbsent(key, k ->
                new TokenBucketState(config.getMaxBurst(), System.currentTimeMillis()));

        boolean acquired = state.tryAcquire(permits, tokensPerMs, config.getMaxBurst());
        long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;

        if (acquired) {
            return RateLimitResult.allowed(state.getCurrentTokens(), key,
                    AlgorithmType.TOKEN_BUCKET, ModeType.LOCAL, executionTimeMs);
        } else {
            long waitTimeMs = calculateWaitTime(state, permits);
            return RateLimitResult.rejected(waitTimeMs, key,
                    AlgorithmType.TOKEN_BUCKET, ModeType.LOCAL, executionTimeMs);
        }
    }

    /**
     * 计算等待时间
     * 基于当前令牌数和补充速率计算需要等待多久才能获取指定数量令牌
     *
     * @param state 令牌桶状态
     * @param permits 需要的许可数
     * @return 等待时间（毫秒）
     */
    private long calculateWaitTime(TokenBucketState state, int permits) {
        double deficit = permits - state.getCurrentTokens();
        if (deficit <= 0) {
            return 0;
        }
        return (long) Math.ceil(deficit / tokensPerMs);
    }

    /**
     * 获取算法类型
     */
    @Override
    public AlgorithmType getType() {
        return AlgorithmType.TOKEN_BUCKET;
    }

    /**
     * 重置指定Key的令牌桶状态
     *
     * @param key 限流Key
     */
    @Override
    public void reset(String key) {
        if (Objects.isNull(key)) {
            return;
        }
        buckets.remove(key);
    }

    /**
     * 获取状态描述
     *
     * @param key 限流Key
     * @return 状态描述字符串
     */
    @Override
    public String getStatusDescription(String key) {
        TokenBucketState state = buckets.get(key);
        if (Objects.isNull(state)) {
            return "No bucket state for key: " + key;
        }
        return String.format("TokenBucket[key=%s, tokens=%d, maxBurst=%d]",
                key, state.getCurrentTokens(), config.getMaxBurst());
    }

    /**
     * 令牌桶状态
     * 封装单个Key的令牌桶状态信息，使用CAS操作保证线程安全
     */
    private static class TokenBucketState {

        /**
         * 当前令牌数量
         */
        private final AtomicLong tokens;

        /**
         * 最后更新时间戳
         */
        private final AtomicLong lastRefillTimestamp;

        /**
         * 最大容量
         */
        private final int maxCapacity;

        /**
         * 构造令牌桶状态
         *
         * @param initialTokens 初始令牌数
         * @param timestamp 初始时间戳
         */
        TokenBucketState(int initialTokens, long timestamp) {
            this.tokens = new AtomicLong(initialTokens);
            this.lastRefillTimestamp = new AtomicLong(timestamp);
            this.maxCapacity = initialTokens;
        }

        /**
         * 尝试获取令牌
         * 使用CAS操作保证原子性，支持多线程并发访问
         *
         * @param permits 需要获取的令牌数
         * @param tokensPerMs 每毫秒补充速率
         * @param maxBurst 最大容量
         * @return true表示获取成功，false表示令牌不足
         */
        boolean tryAcquire(int permits, double tokensPerMs, int maxBurst) {
            long now = System.currentTimeMillis();

            // 补充令牌（CAS操作）
            refillTokens(now, tokensPerMs, maxBurst);

            // 尝试消费令牌
            while (true) {
                long currentTokens = tokens.get();
                if (currentTokens < permits) {
                    return false;
                }

                if (tokens.compareAndSet(currentTokens, currentTokens - permits)) {
                    return true;
                }
            }
        }

        /**
         * 补充令牌
         * 基于时间差计算应补充的令牌数量
         */
        private void refillTokens(long now, double tokensPerMs, int maxBurst) {
            while (true) {
                long lastRefill = lastRefillTimestamp.get();
                if (now <= lastRefill) {
                    break;
                }

                long elapsedMs = now - lastRefill;
                long newTokens = Math.min(
                        tokens.get() + (long) (elapsedMs * tokensPerMs),
                        maxBurst
                );

                if (lastRefillTimestamp.compareAndSet(lastRefill, now)) {
                    tokens.set(newTokens);
                    break;
                }
            }
        }

        /**
         * 获取当前令牌数（用于监控）
         *
         * @return 当前令牌数
         */
        long getCurrentTokens() {
            return tokens.get();
        }
    }
}