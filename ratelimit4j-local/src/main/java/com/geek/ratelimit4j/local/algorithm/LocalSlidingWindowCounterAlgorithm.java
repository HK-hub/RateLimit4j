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
 * 本地滑动窗口计数器算法实现
 *
 * <p>算法原理：</p>
 * <ul>
 *   <li>结合多个固定窗口</li>
 *   <li>使用加权平均计算当前窗口请求数</li>
 *   <li>平衡精度与性能</li>
 *   <li>临界问题比固定窗口小</li>
 * </ul>
 *
 * <p>实现特点：</p>
 * <ul>
 *   <li>使用前一个窗口和当前窗口加权计算</li>
 *   <li>比滑动窗口日志更节省内存</li>
 *   <li>比固定窗口更精确</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class LocalSlidingWindowCounterAlgorithm implements RateLimitAlgorithm {

    private final RateLimitConfig config;
    private final ConcurrentHashMap<String, SlidingWindowCounterState> windows;

    public LocalSlidingWindowCounterAlgorithm(RateLimitConfig config) {
        if (Objects.isNull(config)) {
            throw new IllegalArgumentException("RateLimitConfig cannot be null");
        }
        this.config = config;
        this.windows = new ConcurrentHashMap<>();
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
        long windowSizeMs = config.getPeriodMs();

        SlidingWindowCounterState state = windows.computeIfAbsent(key, k ->
                new SlidingWindowCounterState());

        boolean acquired = state.tryAcquire(permits, windowSizeMs, config.getRate());
        long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;

        if (acquired) {
            return RateLimitResult.allowed(state.getRemainingCount(config.getRate(), windowSizeMs), key,
                    AlgorithmType.SLIDING_WINDOW_COUNTER, ModeType.LOCAL, executionTimeMs);
        } else {
            long waitTimeMs = state.getWaitTimeMs(windowSizeMs);
            return RateLimitResult.rejected(waitTimeMs, key,
                    AlgorithmType.SLIDING_WINDOW_COUNTER, ModeType.LOCAL, executionTimeMs);
        }
    }

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.SLIDING_WINDOW_COUNTER;
    }

    @Override
    public void reset(String key) {
        if (Objects.isNull(key)) {
            return;
        }
        windows.remove(key);
    }

    @Override
    public String getStatusDescription(String key) {
        SlidingWindowCounterState state = windows.get(key);
        if (Objects.isNull(state)) {
            return "No window state for key: " + key;
        }
        return String.format("SlidingWindowCounter[key=%s, prev=%d, curr=%d, limit=%d]",
                key, state.getPrevCount(), state.getCurrentCount(), config.getRate());
    }

    private static class SlidingWindowCounterState {
        private final AtomicLong prevCount;
        private final AtomicLong currentCount;
        private final AtomicLong windowStartTimestamp;

        SlidingWindowCounterState() {
            this.prevCount = new AtomicLong(0);
            this.currentCount = new AtomicLong(0);
            this.windowStartTimestamp = new AtomicLong(System.currentTimeMillis());
        }

        boolean tryAcquire(int permits, long windowSizeMs, int limit) {
            long now = System.currentTimeMillis();

            synchronized (this) {
                long windowStart = windowStartTimestamp.get();
                long elapsed = now - windowStart;

                if (elapsed >= windowSizeMs) {
                    if (elapsed >= windowSizeMs * 2) {
                        prevCount.set(0);
                    } else {
                        prevCount.set(currentCount.get());
                    }
                    currentCount.set(0);
                    windowStartTimestamp.set(now);
                }

                double weightedCount = calculateWeightedCount(windowSizeMs);
                if (weightedCount + permits > limit) {
                    return false;
                }

                currentCount.addAndGet(permits);
                return true;
            }
        }

        private double calculateWeightedCount(long windowSizeMs) {
            long now = System.currentTimeMillis();
            long windowStart = windowStartTimestamp.get();
            long elapsed = now - windowStart;

            double weight = (double) elapsed / windowSizeMs;
            return prevCount.get() * (1 - weight) + currentCount.get();
        }

        long getPrevCount() {
            return prevCount.get();
        }

        long getCurrentCount() {
            return currentCount.get();
        }

        long getRemainingCount(int limit, long windowSizeMs) {
            double weighted = calculateWeightedCount(windowSizeMs);
            return Math.max(0, (long) (limit - weighted));
        }

        long getWaitTimeMs(long windowSizeMs) {
            long windowStart = windowStartTimestamp.get();
            long elapsed = System.currentTimeMillis() - windowStart;
            return Math.max(0, windowSizeMs - elapsed);
        }
    }
}