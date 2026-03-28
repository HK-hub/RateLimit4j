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
 * Redis漏桶算法实现
 * 基于Lua脚本实现原子性的漏桶限流
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
 *   <li>使用Hash存储桶状态（容量、当前水位、最后漏水时间）</li>
 *   <li>Lua脚本保证原子性</li>
 *   <li>支持分布式环境</li>
 *   <li>适合需要稳定流速的场景</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Slf4j
@Getter
public class RedisLeakyBucketAlgorithm implements RateLimitAlgorithm {

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
     * 漏桶Lua脚本
     *
     * KEYS[1]: 限流Key
     * ARGV[1]: 桶容量
     * ARGV[2]: 漏水速率（每毫秒漏出的请求数，浮点数）
     * ARGV[3]: 当前时间戳
     *
     * 返回: {允许标志, 等待时间}
     */
    private static final String LEAKY_BUCKET_SCRIPT =
            // 获取桶容量
            "local capacity = tonumber(ARGV[1]);" +
            // 获取漏水速率（每毫秒漏出请求数）
            "local rate = tonumber(ARGV[2]);" +
            // 获取当前时间
            "local now = tonumber(ARGV[3]);" +
            // 获取当前水位
            "local water = tonumber(redis.call('HGET', KEYS[1], 'water') or 0);" +
            // 获取上次漏水时间
            "local lastLeakTime = tonumber(redis.call('HGET', KEYS[1], 'lastLeakTime') or now);" +
            // 计算经过的时间（毫秒）
            "local elapsedTime = math.max(0, now - lastLeakTime);" +
            // 计算漏出的水量
            "local leaked = math.floor(elapsedTime * rate);" +
            // 更新水位（漏水）
            "water = math.max(0, water - leaked);" +
            // 更新最后漏水时间
            "lastLeakTime = now;" +
            // 判断桶是否还有空间
            "if water < capacity then " +
            // 有空间，增加水位
            "    water = water + 1;" +
            // 保存状态
            "    redis.call('HMSET', KEYS[1], 'water', water, 'lastLeakTime', lastLeakTime);" +
            // 设置过期时间（桶容量 / 漏水速率 + 1秒）
            "    redis.call('PEXPIRE', KEYS[1], math.ceil(capacity / rate) + 1000);" +
            // 返回允许（1表示允许，等待时间为0）
            "    return {1, 0};" +
            "else " +
            // 桶满，计算需要等待的时间
            // 等待时间 = (水位 - 容量 + 1) / 漏水速率
            "    local waitTime = math.ceil((water - capacity + 1) / rate);" +
            // 返回拒绝（0表示拒绝，等待时间）
            "    return {0, waitTime};" +
            "end;";

    // ==================== 构造函数 ====================

    /**
     * 构造Redis漏桶算法
     *
     * @param redissonClient Redisson客户端
     * @param config         默认限流配置
     */
    public RedisLeakyBucketAlgorithm(RedissonClient redissonClient, RateLimitConfig config) {
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
        String key = "ratelimit4j:leaky_bucket:" + context.getKey();
        // 获取限流配置
        RateLimitConfig config = Objects.nonNull(context.getConfig()) ? context.getConfig() : defaultConfig;

        try {
            // 准备Lua脚本参数
            List<Object> keys = new ArrayList<>();
            keys.add(key);

            List<Object> args = new ArrayList<>();
            // 桶容量
            args.add((long) config.getRate());
            // 漏水速率（每毫秒漏出请求数 = 每秒请求数 / 1000）
            args.add(config.getRate() / 1000.0);
            // 当前时间戳
            args.add(System.currentTimeMillis());

            // 执行Lua脚本
            RScript script = redissonClient.getScript();
            Object resultObj = script.eval(
                    RScript.Mode.READ_WRITE,
                    LEAKY_BUCKET_SCRIPT,
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
                    long waitTime = result.get(1);

                    if (allowed == 1) {
                        // 允许通过
                        return RateLimitResult.allowed(config.getRate(), key,
                                AlgorithmType.LEAKY_BUCKET, ModeType.DISTRIBUTED, executionTimeMs);
                    } else {
                        // 被拒绝
                        return RateLimitResult.rejected(waitTime, key,
                                AlgorithmType.LEAKY_BUCKET, ModeType.DISTRIBUTED, executionTimeMs);
                    }
                }
            }

            // 结果解析失败，默认允许
            return RateLimitResult.allowed(config.getRate(), key,
                    AlgorithmType.LEAKY_BUCKET, ModeType.DISTRIBUTED, executionTimeMs);

        } catch (Exception e) {
            // 发生异常，记录错误日志
            log.error("[RedisLeakyBucket] Rate limit evaluation failed for key: {}", key, e);
            // 计算执行耗时
            long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            // 异常情况下允许通过（降级策略）
            return RateLimitResult.allowed(config.getRate(), key,
                    AlgorithmType.LEAKY_BUCKET, ModeType.DISTRIBUTED, executionTimeMs);
        }
    }

    // ==================== 算法接口方法 ====================

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.LEAKY_BUCKET;
    }

    @Override
    public void reset(String key) {
        if (Objects.isNull(key)) {
            return;
        }
        try {
            // 删除Redis中的桶状态
            String redisKey = "ratelimit4j:leaky_bucket:" + key;
            redissonClient.getBucket(redisKey).delete();
            log.debug("[RedisLeakyBucket] Reset rate limiter: key={}", key);
        } catch (Exception e) {
            log.error("[RedisLeakyBucket] Failed to reset rate limiter: key={}", key, e);
        }
    }

    @Override
    public String getStatusDescription(String key) {
        try {
            String redisKey = "ratelimit4j:leaky_bucket:" + key;
            // 获取桶状态
            Object water = redissonClient.getScript().eval(
                    RScript.Mode.READ_ONLY,
                    "return redis.call('HGET', KEYS[1], 'water') or 0;",
                    RScript.ReturnType.INTEGER,
                    List.of(redisKey)
            );
            return String.format("RedisLeakyBucket[key=%s, water=%s]",
                    key, water);
        } catch (Exception e) {
            return "RedisLeakyBucket[key=" + key + ", error=" + e.getMessage() + "]";
        }
    }
}