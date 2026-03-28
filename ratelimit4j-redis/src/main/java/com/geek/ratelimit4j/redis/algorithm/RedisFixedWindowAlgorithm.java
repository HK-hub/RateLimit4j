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
 * Redis固定窗口计数器算法实现
 * 基于Lua脚本实现原子性的固定窗口限流
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
 *   <li>使用Lua脚本保证原子性</li>
 *   <li>支持分布式环境</li>
 *   <li>实现简单高效</li>
 *   <li>存在临界时刻双倍流量问题</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Slf4j
@Getter
public class RedisFixedWindowAlgorithm implements RateLimitAlgorithm {

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
     * 固定窗口Lua脚本
     * KEYS[1]: 限流Key
     * ARGV[1]: 窗口大小（毫秒）
     * ARGV[2]: 最大请求数
     * ARGV[3]: 当前时间戳
     * 返回: 1表示允许，0表示拒绝
     */
    private static final String FIXED_WINDOW_SCRIPT = 
            // 获取当前计数
            "local current = redis.call('GET', KEYS[1]);" +
            // 如果计数不存在或已过期
            "if current == false then " +
            // 设置新计数为1
            "    redis.call('SET', KEYS[1], 1, 'PX', ARGV[1]);" +
            // 返回允许（1表示允许，剩余数）
            "    return {1, ARGV[2] - 1};" +
            "end;" +
            // 计数存在，检查是否超过限制
            "if tonumber(current) >= tonumber(ARGV[2]) then " +
            // 超过限制，返回拒绝（0表示拒绝，等待时间）
            "    local ttl = redis.call('PTTL', KEYS[1]);" +
            "    return {0, ttl};" +
            "end;" +
            // 未超过限制，递增计数
            "redis.call('INCR', KEYS[1]);" +
            // 返回允许
            "return {1, ARGV[2] - tonumber(current) - 1};";

    // ==================== 构造函数 ====================

    /**
     * 构造Redis固定窗口算法
     *
     * @param redissonClient Redisson客户端
     * @param config         默认限流配置
     */
    public RedisFixedWindowAlgorithm(RedissonClient redissonClient, RateLimitConfig config) {
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
        // 获取限流Key
        String key = "ratelimit4j:fixed_window:" + context.getKey();
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
                    FIXED_WINDOW_SCRIPT,
                    RScript.ReturnType.MULTI,
                    keys,
                    args.toArray()
            );
            
            // 转换结果
            @SuppressWarnings("unchecked")
            List<Long> result = (List<Long>) resultObj;

            // 计算执行耗时
            long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;

            // 解析结果
            if (Objects.nonNull(result) && result.size() >= 2) {
                long allowed = result.get(0);
                long remaining = result.get(1);

                if (allowed == 1) {
                    // 允许通过
                    return RateLimitResult.allowed(remaining, key,
                            AlgorithmType.FIXED_WINDOW, ModeType.DISTRIBUTED, executionTimeMs);
                } else {
                    // 被拒绝
                    return RateLimitResult.rejected(remaining, key,
                            AlgorithmType.FIXED_WINDOW, ModeType.DISTRIBUTED, executionTimeMs);
                }
            }

            // 结果解析失败，默认允许
            return RateLimitResult.allowed(config.getRate(), key,
                    AlgorithmType.FIXED_WINDOW, ModeType.DISTRIBUTED, executionTimeMs);

        } catch (Exception e) {
            // 发生异常，记录错误日志
            log.error("[RedisFixedWindow] Rate limit evaluation failed for key: {}", key, e);
            // 计算执行耗时
            long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            // 异常情况下允许通过（降级策略）
            return RateLimitResult.allowed(config.getRate(), key,
                    AlgorithmType.FIXED_WINDOW, ModeType.DISTRIBUTED, executionTimeMs);
        }
    }

    // ==================== 算法接口方法 ====================

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.FIXED_WINDOW;
    }

    @Override
    public void reset(String key) {
        if (Objects.isNull(key)) {
            return;
        }
        try {
            // 删除Redis中的计数器
            String redisKey = "ratelimit4j:fixed_window:" + key;
            redissonClient.getBucket(redisKey).delete();
            log.debug("[RedisFixedWindow] Reset rate limiter: key={}", key);
        } catch (Exception e) {
            log.error("[RedisFixedWindow] Failed to reset rate limiter: key={}", key, e);
        }
    }

    @Override
    public String getStatusDescription(String key) {
        try {
            String redisKey = "ratelimit4j:fixed_window:" + key;
            Long count = (Long) redissonClient.getBucket(redisKey).get();
            long ttl = redissonClient.getBucket(redisKey).remainTimeToLive();
            
            return String.format("RedisFixedWindow[key=%s, count=%d, ttl=%dms]",
                    key, Objects.nonNull(count) ? count : 0, ttl);
        } catch (Exception e) {
            return "RedisFixedWindow[key=" + key + ", error=" + e.getMessage() + "]";
        }
    }
}