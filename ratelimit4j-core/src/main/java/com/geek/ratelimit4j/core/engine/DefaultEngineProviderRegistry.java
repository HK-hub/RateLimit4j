package com.geek.ratelimit4j.core.engine;

import com.geek.ratelimit4j.core.config.EngineType;

import java.util.List;

/**
 * 默认引擎提供者注册中心实现
 * 提供基本的引擎提供者管理功能
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class DefaultEngineProviderRegistry extends AbstractEngineProviderRegistry {

    /**
     * 构造函数
     *
     * @param providers     引擎提供者列表
     * @param primaryEngine 主引擎配置（用于AUTO情况）
     */
    public DefaultEngineProviderRegistry(List<RateLimitEngineProvider> providers, EngineType primaryEngine) {
        super(providers, primaryEngine);
    }
}