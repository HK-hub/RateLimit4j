package com.geek.ratelimit4j.local.algorithm;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import com.geek.ratelimit4j.core.algorithm.RateLimitAlgorithm;
import com.geek.ratelimit4j.core.config.RateLimitContext;
import com.geek.ratelimit4j.core.algorithm.RateLimitResult;
import com.geek.ratelimit4j.core.config.ModeType;
import com.geek.ratelimit4j.core.config.RateLimitConfig;
import lombok.Getter;

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
@Getter
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
                new SlidingWindowLogState(config.getRate()));

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
        // 最大时间戳容量（防止队列无限增长）
        // 设置为限流上限的2倍，确保极端情况下也能正常工作
        private static final int MAX_CAPACITY_MULTIPLIER = 2;
        
        private final ConcurrentLinkedQueue<Long> timestamps;
        private final AtomicInteger count;
        // 最大容量限制
        private final int maxCapacity;

        SlidingWindowLogState() {
            this.timestamps = new ConcurrentLinkedQueue<>();
            this.count = new AtomicInteger(0);
            // 默认最大容量为2000（假设限流上限1000的2倍）
            this.maxCapacity = 2000;
        }

        /**
         * 创建带容量限制的状态对象
         *
         * @param limit 限流上限
         */
        SlidingWindowLogState(int limit) {
            this.timestamps = new ConcurrentLinkedQueue<>();
            this.count = new AtomicInteger(0);
            this.maxCapacity = limit * MAX_CAPACITY_MULTIPLIER;
        }

        boolean tryAcquire(int permits, long windowSizeMs, int limit) {
            long now = System.currentTimeMillis();
            long windowStart = now - windowSizeMs;

            // 清理过期时间戳
            cleanupOldTimestamps(windowStart);

            // 检查容量限制，防止队列无限增长
            enforceCapacityLimit(limit);

            int currentCount = count.get();
            if (currentCount + permits > limit) {
                return false;
            }

            // 添加时间戳
            for (int i = 0; i < permits; i++) {
                timestamps.offer(now);
            }
            count.addAndGet(permits);
            return true;
        }

        /**
         * 清理过期时间戳
         *
         * @param windowStart 窗口起始时间
         */
        private void cleanupOldTimestamps(long windowStart) {
            // 最多清理1000个过期时间戳（避免长时间阻塞）
            int cleaned = 0;
            while (cleaned < 1000) {
                Long oldest = timestamps.peek();
                // 队列为空或最老的时间戳在窗口内，停止清理
                if (Objects.isNull(oldest) || oldest > windowStart) {
                    break;
                }
                // 移除过期时间戳
                if (Objects.nonNull(timestamps.poll())) {
                    count.decrementAndGet();
                    cleaned++;
                }
            }
        }

        /**
         * 强制执行容量限制
         * 当队列超过最大容量时，强制移除最老的时间戳
         *
         * @param limit 限流上限
         */
        private void enforceCapacityLimit(int limit) {
            // 当队列大小超过最大容量时，强制清理
            while (timestamps.size() > maxCapacity) {
                Long removed = timestamps.poll();
                if (Objects.nonNull(removed)) {
                    count.decrementAndGet();
                } else {
                    break;
                }
            }
        }

        int getCurrentCount() {
            // 清理过期时间戳（使用一个默认窗口大小）
            cleanupOldTimestamps(System.currentTimeMillis() - 60000);
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