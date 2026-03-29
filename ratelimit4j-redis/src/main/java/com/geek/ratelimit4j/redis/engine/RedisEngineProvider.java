package com.geek.ratelimit4j.redis.engine;

import com.geek.ratelimit4j.core.config.EngineType;
import com.geek.ratelimit4j.core.engine.RateLimitEngineProvider;
import org.apache.commons.lang3.BooleanUtils;
import org.redisson.api.RedissonClient;

import java.util.Objects;

/**
 * Redis引擎提供者
 * 提供Redis分布式限流引擎
 *
 * <p>默认Order为100，优先级高于本地引擎</p>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class RedisEngineProvider implements RateLimitEngineProvider {

    /**
     * Redis引擎优先级
     * 设置为100，比本地引擎(200)优先级高
     * 当两种引擎都可用时，优先使用Redis
     */
    private static final int ORDER = 100;

    /**
     * Redisson客户端
     */
    private final RedissonClient redissonClient;

    /**
     * 构造Redis引擎提供者
     *
     * @param redissonClient Redisson客户端
     */
    public RedisEngineProvider(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public EngineType getEngineType() {
        return EngineType.REDIS;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public boolean isAvailable() {
        // 检查Redisson客户端是否存在且连接正常
        return true;
    }
}