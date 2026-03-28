package com.geek.ratelimit4j.local.algorithm;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import com.geek.ratelimit4j.core.algorithm.RateLimitAlgorithm;
import com.geek.ratelimit4j.core.config.RateLimitContext;
import com.geek.ratelimit4j.core.algorithm.RateLimitResult;
import com.geek.ratelimit4j.core.config.ModeType;
import com.geek.ratelimit4j.core.config.RateLimitConfig;

import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 本地滑动窗口日志算法实现
 *
 * <p>算法原理：</p>
 * <ul>
 *   <li>记录每次请求的时间戳</li>
 *   <li>统计最近时间窗口内的请求数</li>
 *   <li>精确控制请求速率</li>
 *   <li>不存在边界问题</li>
 * </ul>
 *
 * <p>实现特点：</p>
 * <ul>
 *   <li>精确控制，无边界问题</li>
 *   <li>内存开销较大（需存储时间戳）</li>
 *   <li>适合需要精确限流的场景</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class LocalSlidingWindowLogAlgorithm implements RateLimitAlgorithm {

    private final RateLimitConfig config;
    private final ConcurrentHashMap<String, SlidingWindowLogState> windows;

    public LocalSlidingWindowLogAlgorithm(RateLimitConfig config) {
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

        SlidingWindowLogState state = windows.computeIfAbsent(key, k ->
                new SlidingWindowLogState());

        boolean acquired = state.tryAcquire(permits, windowSizeMs, config.getRate());
        long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;

        if (acquired) {
            return RateLimitResult.allowed(config.getRate() - state.getCurrentCount(), key,
                    AlgorithmType.SLIDING_WINDOW_LOG, ModeType.LOCAL, executionTimeMs);
        } else {
            long waitTimeMs = state.getWaitTimeMs(windowSizeMs);
            return RateLimitResult.rejected(waitTimeMs, key,
                    AlgorithmType.SLIDING_WINDOW_LOG, ModeType.LOCAL, executionTimeMs);
        }
    }

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.SLIDING_WINDOW_LOG;
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
        SlidingWindowLogState state = windows.get(key);
        if (Objects.isNull(state)) {
            return "No window state for key: " + key;
        }
        return String.format("SlidingWindowLog[key=%s, count=%d, limit=%d]",
                key, state.getCurrentCount(), config.getRate());
    }

    private static class SlidingWindowLogState {
        private final ConcurrentLinkedQueue<Long> timestamps;
        private final AtomicInteger count;

        SlidingWindowLogState() {
            this.timestamps = new ConcurrentLinkedQueue<>();
            this.count = new AtomicInteger(0);
        }

        boolean tryAcquire(int permits, long windowSizeMs, int limit) {
            long now = System.currentTimeMillis();
            long windowStart = now - windowSizeMs;

            cleanupOldTimestamps(windowStart);

            int currentCount = count.get();
            if (currentCount + permits > limit) {
                return false;
            }

            for (int i = 0; i < permits; i++) {
                timestamps.offer(now);
            }
            count.addAndGet(permits);
            return true;
        }

        private void cleanupOldTimestamps(long windowStart) {
            while (true) {
                Long oldest = timestamps.peek();
                if (Objects.isNull(oldest) || oldest > windowStart) {
                    break;
                }
                if (Objects.nonNull(timestamps.poll())) {
                    count.decrementAndGet();
                }
            }
        }

        int getCurrentCount() {
            cleanupOldTimestamps(System.currentTimeMillis() - 1000);
            return count.get();
        }

        long getWaitTimeMs(long windowSizeMs) {
            Long oldest = timestamps.peek();
            if (Objects.isNull(oldest)) {
                return 0;
            }
            long waitUntil = oldest + windowSizeMs - System.currentTimeMillis();
            return Math.max(0, waitUntil);
        }
    }
}