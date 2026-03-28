package com.geek.ratelimit4j.local.algorithm;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import com.geek.ratelimit4j.core.algorithm.RateLimitAlgorithm;
import com.geek.ratelimit4j.core.algorithm.RateLimitContext;
import com.geek.ratelimit4j.core.algorithm.RateLimitResult;
import com.geek.ratelimit4j.core.config.ModeType;
import com.geek.ratelimit4j.core.config.RateLimitConfig;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 本地固定窗口计数器算法实现
 *
 * <p>算法原理：</p>
 * <ul>
 *   <li>将时间划分为固定大小窗口</li>
 *   <li>在每个窗口内统计请求数量</li>
 *   <li>超过阈值则拒绝请求</li>
 *   <li>窗口结束后重置计数器</li>
 * </ul>
 *
 * <p>实现特点：</p>
 * <ul>
 *   <li>实现简单高效</li>
 *   <li>存在临界时刻双倍流量问题</li>
 *   <li>适合简单限流场景</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class LocalFixedWindowAlgorithm implements RateLimitAlgorithm {

    private final RateLimitConfig config;
    private final ConcurrentHashMap<String, FixedWindowState> windows;

    public LocalFixedWindowAlgorithm(RateLimitConfig config) {
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

        FixedWindowState state = windows.computeIfAbsent(key, k ->
                new FixedWindowState(config.getRate()));

        boolean acquired = state.tryAcquire(permits, windowSizeMs, config.getRate());
        long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;

        if (acquired) {
            return RateLimitResult.allowed(state.getRemaining(), key,
                    AlgorithmType.FIXED_WINDOW, ModeType.LOCAL, executionTimeMs);
        } else {
            long waitTimeMs = state.getWaitTimeMs();
            return RateLimitResult.rejected(waitTimeMs, key,
                    AlgorithmType.FIXED_WINDOW, ModeType.LOCAL, executionTimeMs);
        }
    }

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.FIXED_WINDOW;
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
        FixedWindowState state = windows.get(key);
        if (Objects.isNull(state)) {
            return "No window state for key: " + key;
        }
        return String.format("FixedWindow[key=%s, count=%d, limit=%d]",
                key, state.getCount(), config.getRate());
    }

    private static class FixedWindowState {
        private final AtomicLong count;
        private final AtomicLong windowStartTimestamp;

        FixedWindowState(int limit) {
            this.count = new AtomicLong(0);
            this.windowStartTimestamp = new AtomicLong(System.currentTimeMillis());
        }

        boolean tryAcquire(int permits, long windowSizeMs, int limit) {
            long now = System.currentTimeMillis();
            long windowStart = windowStartTimestamp.get();

            if (now - windowStart >= windowSizeMs) {
                if (windowStartTimestamp.compareAndSet(windowStart, now)) {
                    count.set(0);
                }
            }

            while (true) {
                long currentCount = count.get();
                if (currentCount + permits > limit) {
                    return false;
                }
                if (count.compareAndSet(currentCount, currentCount + permits)) {
                    return true;
                }
            }
        }

        long getCount() {
            return count.get();
        }

        long getRemaining() {
            return Math.max(0, count.get());
        }

        long getWaitTimeMs() {
            long windowStart = windowStartTimestamp.get();
            long elapsed = System.currentTimeMillis() - windowStart;
            long windowSizeMs = 1000;
            return Math.max(0, windowSizeMs - elapsed);
        }
    }
}