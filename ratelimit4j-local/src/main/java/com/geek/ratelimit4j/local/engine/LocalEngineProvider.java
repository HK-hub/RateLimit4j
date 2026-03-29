package com.geek.ratelimit4j.local.engine;

import com.geek.ratelimit4j.core.config.EngineType;
import com.geek.ratelimit4j.core.engine.RateLimitEngineProvider;

/**
 * 本地引擎提供者
 * 提供本地（JVM内存）限流引擎
 *
 * <p>默认Order为200，优先级低于Redis引擎</p>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class LocalEngineProvider implements RateLimitEngineProvider {

    /**
     * 本地引擎优先级
     * 设置为200，比Redis引擎(100)优先级低
     * 当两种引擎都可用时，优先使用Redis
     */
    private static final int ORDER = 200;

    @Override
    public EngineType getEngineType() {
        return EngineType.LOCAL;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public boolean isAvailable() {
        // 本地引擎始终可用
        return true;
    }
}