package com.geek.ratelimit4j.redis.algorithm;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import com.geek.ratelimit4j.core.algorithm.RateLimitAlgorithm;
import com.geek.ratelimit4j.core.algorithm.RateLimitResult;
import com.geek.ratelimit4j.core.config.ModeType;
import com.geek.ratelimit4j.core.config.RateLimitConfig;
import com.geek.ratelimit4j.core.config.RateLimitContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Redis令牌桶算法实现
 * 基于Redisson的RRateLimiter实现分布式令牌桶限流
 *
 * <p>特点：</p>
 * <ul>
 *   <li>使用Redisson内置的RRateLimiter</li>
 *   <li>支持分布式环境下的精确限流</li>
 *   <li>支持突发流量处理</li>
 *   <li>高可用，支持Redis集群</li>
 * </ul>
 *
 * <p>Redisson RRateLimiter特点：</p>
 * <ul>
 *   <li>基于Redis的令牌桶算法实现</li>
 *   <li>使用Lua脚本保证原子性</li>
 *   <li>支持设置速率和容量</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Slf4j
@Getter
public class RedisTokenBucketAlgorithm implements RateLimitAlgorithm {

    // ==================== 成员变量 ====================

    /**
     * Redisson客户端
     * 用于操作Redis
     */
    private final RedissonClient redissonClient;

    /**
     * 默认限流配置
     */
    private final RateLimitConfig defaultConfig;

    /**
     * 限流器缓存
     * Key: 限流Key, Value: RRateLimiter实例
     */
    private final ConcurrentHashMap<String, RRateLimiter> rateLimiterCache = new ConcurrentHashMap<>();

    // ==================== 构造函数 ====================

    /**
     * 构造Redis令牌桶算法
     *
     * @param redissonClient Redisson客户端
     * @param config         默认限流配置
     */
    public RedisTokenBucketAlgorithm(RedissonClient redissonClient, RateLimitConfig config) {
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
     *
     * @param context 限流上下文
     * @return 限流结果
     */
    @Override
    public RateLimitResult evaluate(RateLimitContext context) {
        // 默认获取1个许可
        return evaluate(context, 1);
    }

    /**
     * 执行限流判断（多许可）
     *
     * @param context 限流上下文
     * @param permits 请求许可数
     * @return 限流结果
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

        // 记录开始时间（用于计算执行耗时）
        long startTime = System.nanoTime();
        // 获取限流Key
        String key = context.getKey();
        // 获取限流配置
        RateLimitConfig config = Objects.nonNull(context.getConfig()) ? context.getConfig() : defaultConfig;

        try {
            // 获取或创建限流器
            RRateLimiter rateLimiter = getOrCreateRateLimiter(key, config);

            // 尝试获取许可（非阻塞）
            boolean acquired = rateLimiter.tryAcquire(permits);

            // 计算执行耗时
            long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;

            if (acquired) {
                // 获取成功，返回允许结果
                // 注意：Redisson不直接提供剩余令牌数，这里用配置的值估算
                long remaining = config.getRate() - permits;
                return RateLimitResult.allowed(remaining, key, 
                        AlgorithmType.TOKEN_BUCKET, ModeType.DISTRIBUTED, executionTimeMs);
            } else {
                // 获取失败，返回拒绝结果
                // 计算等待时间（估算：许可数 / 速率 * 周期）
                long waitTimeMs = calculateWaitTime(config, permits);
                return RateLimitResult.rejected(waitTimeMs, key, 
                        AlgorithmType.TOKEN_BUCKET, ModeType.DISTRIBUTED, executionTimeMs);
            }
        } catch (Exception e) {
            // 发生异常，记录错误日志
            log.error("[RedisTokenBucket] Rate limit evaluation failed for key: {}", key, e);
            // 计算执行耗时
            long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            // 异常情况下允许通过（降级策略）
            return RateLimitResult.allowed(config.getRate(), key, 
                    AlgorithmType.TOKEN_BUCKET, ModeType.DISTRIBUTED, executionTimeMs);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取或创建限流器
     * 使用缓存避免重复创建
     *
     * @param key    限流Key
     * @param config 限流配置
     * @return RRateLimiter实例
     */
    private RRateLimiter getOrCreateRateLimiter(String key, RateLimitConfig config) {
        // 从缓存获取
        return rateLimiterCache.computeIfAbsent(key, k -> {
            // 创建新的限流器
            RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
            
            // 设置速率和容量
            // RateType.OVERALL: 全局限流（所有客户端共享）
            // rate: 每周期产生的令牌数
            // period: 周期时间
            // unit: 时间单位
            long rate = config.getRate();
            long period = config.getPeriod();
            int maxBurst = config.getMaxBurst() > 0 ? config.getMaxBurst() : config.getRate();

            // 设置限流器参数
            // rate: 速率（每周期产生令牌数）
            // rateInterval: 周期间隔
            // rateIntervalUnit: 周期单位
            rateLimiter.trySetRate(RateType.OVERALL, rate, period, RateIntervalUnit.SECONDS);

            log.debug("[RedisTokenBucket] Created rate limiter: key={}, rate={}/{}s, maxBurst={}", 
                      key, rate, period, maxBurst);
            
            return rateLimiter;
        });
    }

    /**
     * 计算等待时间
     * 估算需要等待多久才能获取足够的令牌
     *
     * @param config  限流配置
     * @param permits 需要的许可数
     * @return 等待时间（毫秒）
     */
    private long calculateWaitTime(RateLimitConfig config, int permits) {
        // 获取速率（每秒令牌数）
        int rate = config.getRate();
        // 获取周期（秒）
        int period = config.getPeriod();
        
        // 计算每毫秒产生的令牌数
        double tokensPerMs = (double) rate / (period * 1000.0);
        
        // 计算等待时间
        if (tokensPerMs > 0) {
            return (long) Math.ceil(permits / tokensPerMs);
        }
        
        // 默认等待一个周期
        return period * 1000L;
    }

    // ==================== 算法接口方法 ====================

    /**
     * 获取算法类型
     *
     * @return 令牌桶算法类型
     */
    @Override
    public AlgorithmType getType() {
        return AlgorithmType.TOKEN_BUCKET;
    }

    /**
     * 重置指定Key的限流状态
     * 删除限流器缓存
     *
     * @param key 限流Key
     */
    @Override
    public void reset(String key) {
        // 参数校验
        if (Objects.isNull(key)) {
            return;
        }
        
        try {
            // 从缓存移除
            RRateLimiter rateLimiter = rateLimiterCache.remove(key);
            // 删除Redis中的限流器
            if (Objects.nonNull(rateLimiter)) {
                rateLimiter.delete();
            }
            log.debug("[RedisTokenBucket] Reset rate limiter: key={}", key);
        } catch (Exception e) {
            log.error("[RedisTokenBucket] Failed to reset rate limiter: key={}", key, e);
        }
    }

    /**
     * 获取状态描述
     *
     * @param key 限流Key
     * @return 状态描述字符串
     */
    @Override
    public String getStatusDescription(String key) {
        try {
            // 获取限流器
            RRateLimiter rateLimiter = rateLimiterCache.get(key);
            
            if (Objects.isNull(rateLimiter)) {
                return "No rate limiter for key: " + key;
            }
            
            // 获取限流器配置
            var rateConfig = rateLimiter.getConfig();
            if (Objects.nonNull(rateConfig)) {
                return String.format("RedisTokenBucket[key=%s, rate=%d, period=%ds]", 
                        key, rateConfig.getRate(), rateConfig.getRateInterval());
            }
            
            return "RedisTokenBucket[key=" + key + ", status=unknown]";
        } catch (Exception e) {
            return "RedisTokenBucket[key=" + key + ", error=" + e.getMessage() + "]";
        }
    }
}