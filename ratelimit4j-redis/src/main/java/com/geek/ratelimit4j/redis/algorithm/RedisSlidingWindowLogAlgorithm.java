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
 * Redis滑动窗口日志算法实现
 * 基于Lua脚本实现原子性的滑动窗口日志限流
 *
 * <p>算法原理：</p>
 * <ul>
 *   <li>记录每次请求的时间戳到有序集合</li>
 *   <li>每次请求时清理窗口外的时间戳</li>
 *   <li>统计窗口内时间戳数量判断是否限流</li>
 *   <li>精确控制，无边界问题</li>
 * </ul>
 *
 * <p>实现特点：</p>
 * <ul>
 *   <li>使用Redis Sorted Set存储时间戳（score为时间戳）</li>
 *   <li>Lua脚本保证原子性</li>
 *   <li>支持分布式环境</li>
 *   <li>精确限流，无临界问题</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Slf4j
@Getter
public class RedisSlidingWindowLogAlgorithm implements RateLimitAlgorithm {

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
     * 滑动窗口日志Lua脚本
     * 使用Sorted Set存储时间戳，利用score排序特性实现高效清理
     *
     * KEYS[1]: 限流Key
     * ARGV[1]: 窗口大小（毫秒）
     * ARGV[2]: 最大请求数
     * ARGV[3]: 当前时间戳
     * ARGV[4]: 过期时间（用于设置Key过期）
     *
     * 返回: {允许标志, 剩余数量}
     */
    private static final String SLIDING_WINDOW_LOG_SCRIPT =
            // 清理窗口外的时间戳（移除score小于窗口起始时间的成员）
            "redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[3] - ARGV[1] - 1);" +
            // 获取当前窗口内的请求数量
            "local current = redis.call('ZCARD', KEYS[1]);" +
            // 判断是否超过限流阈值
            "if current >= tonumber(ARGV[2]) then " +
            // 超过阈值，返回拒绝（0表示拒绝，等待时间）
            "    local oldest = redis.call('ZRANGE', KEYS[1], 0, 0, 'WITHSCORES');" +
            "    if #oldest > 0 then " +
            "        return {0, oldest[2] + ARGV[1] - ARGV[3]};" +
            "    end;" +
            "    return {0, ARGV[1]};" +
            "end;" +
            // 未超过阈值，添加当前时间戳
            "redis.call('ZADD', KEYS[1], ARGV[3], ARGV[3] .. ':' .. math.random());" +
            // 设置Key过期时间（窗口大小 + 1秒，确保清理）
            "redis.call('PEXPIRE', KEYS[1], ARGV[1] + 1000);" +
            // 返回允许（1表示允许，剩余数量）
            "return {1, ARGV[2] - current - 1};";

    // ==================== 构造函数 ====================

    /**
     * 构造Redis滑动窗口日志算法
     *
     * @param redissonClient Redisson客户端
     * @param config         默认限流配置
     */
    public RedisSlidingWindowLogAlgorithm(RedissonClient redissonClient, RateLimitConfig config) {
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
        String key = "ratelimit4j:sliding_log:" + context.getKey();
        // 获取限流配置
        RateLimitConfig config = Objects.nonNull(context.getConfig()) ? context.getConfig() : defaultConfig;

        try {
            // 准备Lua脚本参数
            List<Object> keys = new ArrayList<>();
            // Key
            keys.add(key);

            List<Object> args = new ArrayList<>();
            // 窗口大小（毫秒）
            args.add(config.getPeriodMs());
            // 最大请求数
            args.add((long) config.getRate());
            // 当前时间戳
            args.add(System.currentTimeMillis());
            // 过期时间
            args.add(config.getPeriodMs() + 1000L);

            // 执行Lua脚本
            RScript script = redissonClient.getScript();
            Object resultObj = script.eval(
                    RScript.Mode.READ_WRITE,
                    SLIDING_WINDOW_LOG_SCRIPT,
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
                                AlgorithmType.SLIDING_WINDOW_LOG, ModeType.DISTRIBUTED, executionTimeMs);
                    } else {
                        // 被拒绝，remaining为等待时间
                        return RateLimitResult.rejected(Math.max(0, remaining), key,
                                AlgorithmType.SLIDING_WINDOW_LOG, ModeType.DISTRIBUTED, executionTimeMs);
                    }
                }
            }

            // 结果解析失败，默认允许
            return RateLimitResult.allowed(config.getRate(), key,
                    AlgorithmType.SLIDING_WINDOW_LOG, ModeType.DISTRIBUTED, executionTimeMs);

        } catch (Exception e) {
            // 发生异常，记录错误日志
            log.error("[RedisSlidingWindowLog] Rate limit evaluation failed for key: {}", key, e);
            // 计算执行耗时
            long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            // 异常情况下允许通过（降级策略）
            return RateLimitResult.allowed(config.getRate(), key,
                    AlgorithmType.SLIDING_WINDOW_LOG, ModeType.DISTRIBUTED, executionTimeMs);
        }
    }

    // ==================== 算法接口方法 ====================

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.SLIDING_WINDOW_LOG;
    }

    @Override
    public void reset(String key) {
        if (Objects.isNull(key)) {
            return;
        }
        try {
            // 删除Redis中的时间戳集合
            String redisKey = "ratelimit4j:sliding_log:" + key;
            redissonClient.getBucket(redisKey).delete();
            log.debug("[RedisSlidingWindowLog] Reset rate limiter: key={}", key);
        } catch (Exception e) {
            log.error("[RedisSlidingWindowLog] Failed to reset rate limiter: key={}", key, e);
        }
    }

    @Override
    public String getStatusDescription(String key) {
        try {
            String redisKey = "ratelimit4j:sliding_log:" + key;
            // 获取集合大小
            long count = redissonClient.getScript().eval(
                    RScript.Mode.READ_ONLY,
                    "return redis.call('ZCARD', KEYS[1]);",
                    RScript.ReturnType.INTEGER,
                    List.of(redisKey)
            );
            return String.format("RedisSlidingWindowLog[key=%s, count=%d]",
                    key, count);
        } catch (Exception e) {
            return "RedisSlidingWindowLog[key=" + key + ", error=" + e.getMessage() + "]";
        }
    }
}