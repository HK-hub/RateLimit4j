package com.geek.ratelimit4j.local.algorithm;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import com.geek.ratelimit4j.core.algorithm.RateLimitAlgorithm;
import com.geek.ratelimit4j.core.config.RateLimitContext;
import com.geek.ratelimit4j.core.algorithm.RateLimitResult;
import com.geek.ratelimit4j.core.config.ModeType;
import com.geek.ratelimit4j.core.config.RateLimitConfig;
import lombok.Getter;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

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
@Getter
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
        // 使用细粒度锁替代synchronized(this)，提高并发性能
        private final ReentrantLock lock;
        private final AtomicLong prevCount;
        private final AtomicLong currentCount;
        private final AtomicLong windowStartTimestamp;

        SlidingWindowCounterState() {
            // 创建公平锁，避免线程饥饿
            this.lock = new ReentrantLock();
            this.prevCount = new AtomicLong(0);
            this.currentCount = new AtomicLong(0);
            this.windowStartTimestamp = new AtomicLong(System.currentTimeMillis());
        }

        boolean tryAcquire(int permits, long windowSizeMs, int limit) {
            long now = System.currentTimeMillis();

            // 使用tryLock避免死锁，最多等待10ms
            boolean acquired = false;
            try {
                if (lock.tryLock() || lock.tryLock(10, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    try {
                        long windowStart = windowStartTimestamp.get();
                        long elapsed = now - windowStart;

                        // 检查是否需要滑动窗口
                        if (elapsed >= windowSizeMs) {
                            // 如果超过两个窗口，重置计数
                            if (elapsed >= windowSizeMs * 2) {
                                prevCount.set(0);
                            } else {
                                // 否则将当前计数转移为前一个窗口计数
                                prevCount.set(currentCount.get());
                            }
                            // 重置当前窗口计数
                            currentCount.set(0);
                            windowStartTimestamp.set(now);
                        }

                        // 计算加权请求数
                        double weightedCount = calculateWeightedCountInternal(windowSizeMs, now);
                        // 检查是否超过限流阈值
                        if (weightedCount + permits > limit) {
                            return false;
                        }

                        // 增加计数
                        currentCount.addAndGet(permits);
                        return true;
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                // 获取锁被中断，返回false
                Thread.currentThread().interrupt();
                return false;
            }
            return acquired;
        }

        /**
         * 内部计算加权计数方法（需在锁内调用）
         */
        private double calculateWeightedCountInternal(long windowSizeMs, long now) {
            long windowStart = windowStartTimestamp.get();
            long elapsed = now - windowStart;

            // 计算当前窗口权重
            double weight = (double) elapsed / windowSizeMs;
            // 加权公式：前一个窗口计数 * (1 - 权重) + 当前窗口计数
            return prevCount.get() * (1 - weight) + currentCount.get();
        }

        private double calculateWeightedCount(long windowSizeMs) {
            long now = System.currentTimeMillis();
            // 尝试获取锁计算
            try {
                if (lock.tryLock(5, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    try {
                        return calculateWeightedCountInternal(windowSizeMs, now);
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // 获取锁失败，返回近似值
            return prevCount.get() + currentCount.get();
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