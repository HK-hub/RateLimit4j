package com.geek.ratelimit4j.redis.algorithm;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import com.geek.ratelimit4j.core.algorithm.RateLimitAlgorithm;
import com.geek.ratelimit4j.core.algorithm.RateLimitResult;
import com.geek.ratelimit4j.core.config.ModeType;
import com.geek.ratelimit4j.core.config.RateLimitConfig;
import com.geek.ratelimit4j.core.config.RateLimitContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Redis滑动窗口计数器算法实现
 * 基于Lua脚本实现原子性的滑动窗口计数限流
 *
 * <p>算法原理：</p>
 * <ul>
 *   <li>维护当前窗口和前一个窗口的计数</li>
 *   <li>根据当前时间在窗口中的位置加权计算</li>
 *   <li>平衡精度与内存消耗</li>
 *   <li>临界问题比固定窗口小</li>
 * </ul>
 *
 * <p>实现特点：</p>
 * <ul>
 *   <li>使用Hash存储窗口计数</li>
 *   <li>Lua脚本保证原子性</li>
 *   <li>支持分布式环境</li>
 *   <li>比滑动窗口日志更节省内存</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Slf4j
@Getter
public class RedisSlidingWindowCounterAlgorithm implements RateLimitAlgorithm {

    // ==================== 成员变量 ====================

    /**
     * Redisson客户端
     */
    private final RedissonClient redissonClient;

    /**
     * 默认限流配置
     */
    private final RateLimitConfig defaultConfig;

    /**
     * 滑动窗口计数器Lua脚本
     * 使用Hash存储当前窗口和前一个窗口的计数
     *
     * KEYS[1]: 限流Key
     * ARGV[1]: 窗口大小（毫秒）
     * ARGV[2]: 最大请求数
     * ARGV[3]: 当前时间戳
     *
     * 返回: {允许标志, 剩余数量}
     */
    private static final String SLIDING_WINDOW_COUNTER_SCRIPT =
            // 获取当前窗口编号
            "local currentWindow = math.floor(ARGV[3] / ARGV[1]);" +
            // 获取前一个窗口编号
            "local prevWindow = currentWindow - 1;" +
            // 获取当前窗口计数
            "local currentCount = tonumber(redis.call('HGET', KEYS[1], currentWindow) or 0);" +
            // 获取前一个窗口计数
            "local prevCount = tonumber(redis.call('HGET', KEYS[1], prevWindow) or 0);" +
            // 计算当前时间在窗口中的位置（0-1）
            "local windowStart = currentWindow * ARGV[1];" +
            "local elapsed = ARGV[3] - windowStart;" +
            "local weight = elapsed / ARGV[1];" +
            // 计算加权请求数：前一个窗口计数 * (1 - 权重) + 当前窗口计数
            "local weightedCount = math.floor(prevCount * (1 - weight) + currentCount);" +
            // 判断是否超过限流阈值
            "if weightedCount >= tonumber(ARGV[2]) then " +
            // 超过阈值，返回拒绝（0表示拒绝，等待时间）
            "    return {0, ARGV[1] - elapsed};" +
            "end;" +
            // 未超过阈值，增加当前窗口计数
            "redis.call('HINCRBY', KEYS[1], currentWindow, 1);" +
            // 设置整个Hash的过期时间（2个窗口大小）
            "redis.call('PEXPIRE', KEYS[1], ARGV[1] * 2);" +
            // 返回允许（1表示允许，剩余数量）
            "return {1, ARGV[2] - weightedCount - 1};";

    // ==================== 构造函数 ====================

    /**
     * 构造Redis滑动窗口计数器算法
     *
     * @param redissonClient Redisson客户端
     * @param config         默认限流配置
     */
    public RedisSlidingWindowCounterAlgorithm(RedissonClient redissonClient, RateLimitConfig config) {
        // 参数校验
        if (Objects.isNull(redissonClient)) {
            throw new IllegalArgumentException("RedissonClient cannot be null");
        }
        // 初始化Redisson客户端
        this.redissonClient = redissonClient;
        // 初始化默认配置
        this.defaultConfig = config;
    }

    // ==================== 限流方法 ====================

    @Override
    public RateLimitResult evaluate(RateLimitContext context) {
        return evaluate(context, 1);
    }

    @Override
    public RateLimitResult evaluate(RateLimitContext context, int permits) {
        // 参数校验
        if (Objects.isNull(context)) {
            throw new IllegalArgumentException("RateLimitContext cannot be null");
        }
        // 许可数校验
        if (permits <= 0) {
            throw new IllegalArgumentException("Permits must be positive");
        }

        // 记录开始时间
        long startTime = System.nanoTime();
        // 获取限流Key（添加前缀）
        String key = "ratelimit4j:sliding_counter:" + context.getKey();
        // 获取限流配置
        RateLimitConfig config = Objects.nonNull(context.getConfig()) ? context.getConfig() : defaultConfig;

        try {
            // 准备Lua脚本参数
            List<Object> keys = new ArrayList<>();
            keys.add(key);

            List<Object> args = new ArrayList<>();
            // 窗口大小（毫秒）
            args.add(config.getPeriodMs());
            // 最大请求数
            args.add((long) config.getRate());
            // 当前时间戳
            args.add(System.currentTimeMillis());

            // 执行Lua脚本
            RScript script = redissonClient.getScript();
            Object resultObj = script.eval(
                    RScript.Mode.READ_WRITE,
                    SLIDING_WINDOW_COUNTER_SCRIPT,
                    RScript.ReturnType.MULTI,
                    keys,
                    args.toArray()
            );

            // 计算执行耗时
            long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;

            // 解析结果
            if (Objects.nonNull(resultObj)) {
                @SuppressWarnings("unchecked")
                List<Long> result = (List<Long>) resultObj;

                if (result.size() >= 2) {
                    long allowed = result.get(0);
                    long remaining = result.get(1);

                    if (allowed == 1) {
                        // 允许通过
                        return RateLimitResult.allowed(remaining, key,
                                AlgorithmType.SLIDING_WINDOW_COUNTER, ModeType.DISTRIBUTED, executionTimeMs);
                    } else {
                        // 被拒绝
                        return RateLimitResult.rejected(Math.max(0, remaining), key,
                                AlgorithmType.SLIDING_WINDOW_COUNTER, ModeType.DISTRIBUTED, executionTimeMs);
                    }
                }
            }

            // 结果解析失败，默认允许
            return RateLimitResult.allowed(config.getRate(), key,
                    AlgorithmType.SLIDING_WINDOW_COUNTER, ModeType.DISTRIBUTED, executionTimeMs);

        } catch (Exception e) {
            // 发生异常，记录错误日志
            log.error("[RedisSlidingWindowCounter] Rate limit evaluation failed for key: {}", key, e);
            // 计算执行耗时
            long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            // 异常情况下允许通过（降级策略）
            return RateLimitResult.allowed(config.getRate(), key,
                    AlgorithmType.SLIDING_WINDOW_COUNTER, ModeType.DISTRIBUTED, executionTimeMs);
        }
    }

    // ==================== 算法接口方法 ====================

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.SLIDING_WINDOW_COUNTER;
    }

    @Override
    public void reset(String key) {
        if (Objects.isNull(key)) {
            return;
        }
        try {
            // 删除Redis中的计数器Hash
            String redisKey = "ratelimit4j:sliding_counter:" + key;
            redissonClient.getBucket(redisKey).delete();
            log.debug("[RedisSlidingWindowCounter] Reset rate limiter: key={}", key);
        } catch (Exception e) {
            log.error("[RedisSlidingWindowCounter] Failed to reset rate limiter: key={}", key, e);
        }
    }

    @Override
    public String getStatusDescription(String key) {
        try {
            String redisKey = "ratelimit4j:sliding_counter:" + key;
            // 获取所有窗口计数
            Object result = redissonClient.getScript().eval(
                    RScript.Mode.READ_ONLY,
                    "return redis.call('HGETALL', KEYS[1]);",
                    RScript.ReturnType.MULTI,
                    List.of(redisKey)
            );
            return String.format("RedisSlidingWindowCounter[key=%s, data=%s]",
                    key, result);
        } catch (Exception e) {
            return "RedisSlidingWindowCounter[key=" + key + ", error=" + e.getMessage() + "]";
        }
    }
}