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
 * 本地漏桶算法实现
 *
 * <p>算法原理：</p>
 * <ul>
 *   <li>请求以恒定速率流出</li>
 *   <li>桶满时拒绝新请求</li>
 *   <li>平滑输出请求流量</li>
 * </ul>
 *
 * <p>实现特点：</p>
 * <ul>
 *   <li>使用队列模拟桶结构</li>
 *   <li>基于时间戳计算流出速率</li>
 *   <li>支持最大容量限制</li>
 *   <li>适合需要稳定流速的场景</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class LocalLeakyBucketAlgorithm implements RateLimitAlgorithm {

    private final RateLimitConfig config;
    private final double leakRatePerMs;
    private final ConcurrentHashMap<String, LeakyBucketState> buckets;

    public LocalLeakyBucketAlgorithm(RateLimitConfig config) {
        if (Objects.isNull(config)) {
            throw new IllegalArgumentException("RateLimitConfig cannot be null");
        }
        this.config = config;
        this.leakRatePerMs = (double) config.getRate() / (config.getPeriod() * 1000.0);
        this.buckets = new ConcurrentHashMap<>();
    }

    @Override
    public RateLimitResult evaluate(RateLimitContext context) {
        return evaluate(context, 1);
    }

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

        LeakyBucketState state = buckets.computeIfAbsent(key, k ->
                new LeakyBucketState(config.getMaxBurst()));

        boolean acquired = state.tryAcquire(permits, leakRatePerMs, config.getMaxBurst());
        long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;

        if (acquired) {
            return RateLimitResult.allowed(state.getCurrentWater(), key,
                    AlgorithmType.LEAKY_BUCKET, ModeType.LOCAL, executionTimeMs);
        } else {
            long waitTimeMs = calculateWaitTime(state, permits);
            return RateLimitResult.rejected(waitTimeMs, key,
                    AlgorithmType.LEAKY_BUCKET, ModeType.LOCAL, executionTimeMs);
        }
    }

    private long calculateWaitTime(LeakyBucketState state, int permits) {
        double waterNeeded = state.getCurrentWater() + permits - config.getMaxBurst();
        if (waterNeeded <= 0) {
            return 0;
        }
        return (long) Math.ceil(waterNeeded / leakRatePerMs);
    }

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.LEAKY_BUCKET;
    }

    @Override
    public void reset(String key) {
        if (Objects.isNull(key)) {
            return;
        }
        buckets.remove(key);
    }

    @Override
    public String getStatusDescription(String key) {
        LeakyBucketState state = buckets.get(key);
        if (Objects.isNull(state)) {
            return "No bucket state for key: " + key;
        }
        return String.format("LeakyBucket[key=%s, water=%d, capacity=%d]",
                key, state.getCurrentWater(), config.getMaxBurst());
    }

    private static class LeakyBucketState {
        private final AtomicLong water;
        private final AtomicLong lastLeakTimestamp;

        LeakyBucketState(int capacity) {
            this.water = new AtomicLong(0);
            this.lastLeakTimestamp = new AtomicLong(System.currentTimeMillis());
        }

        boolean tryAcquire(int permits, double leakRatePerMs, int capacity) {
            long now = System.currentTimeMillis();
            leakWater(now, leakRatePerMs);

            while (true) {
                long currentWater = water.get();
                if (currentWater + permits > capacity) {
                    return false;
                }
                if (water.compareAndSet(currentWater, currentWater + permits)) {
                    return true;
                }
            }
        }

        private void leakWater(long now, double leakRatePerMs) {
            while (true) {
                long lastLeak = lastLeakTimestamp.get();
                if (now <= lastLeak) {
                    break;
                }

                long elapsedMs = now - lastLeak;
                long leaked = (long) (elapsedMs * leakRatePerMs);
                long newWater = Math.max(0, water.get() - leaked);

                if (lastLeakTimestamp.compareAndSet(lastLeak, now)) {
                    water.set(newWater);
                    break;
                }
            }
        }

        long getCurrentWater() {
            return water.get();
        }
    }
}