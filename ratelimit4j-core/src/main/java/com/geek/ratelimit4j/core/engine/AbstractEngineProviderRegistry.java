package com.geek.ratelimit4j.core.engine;

import com.geek.ratelimit4j.core.config.EngineType;
import com.geek.ratelimit4j.core.exception.NoSuchRateLimitEngineException;
import org.apache.commons.lang3.BooleanUtils;

import java.util.*;

/**
 * 抽象引擎提供者注册中心
 * 提供引擎提供者管理的通用实现
 *
 * <p>设计说明：</p>
 * <ul>
 *   <li>使用List存储，不使用Map，避免Spring代理对象class匹配问题</li>
 *   <li>子类可覆盖任意方法进行扩展</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public abstract class AbstractEngineProviderRegistry implements EngineProviderRegistry {

    /**
     * 引擎提供者列表
     * 不使用Map存储，因为Spring代理对象的class可能与原始class不匹配
     */
    protected final List<RateLimitEngineProvider> providers;

    /**
     * 主引擎配置（用于AUTO情况）
     */
    protected final EngineType primaryEngine;

    /**
     * 构造函数
     *
     * @param providers     引擎提供者列表
     * @param primaryEngine 主引擎配置（用于AUTO情况）
     */
    protected AbstractEngineProviderRegistry(List<RateLimitEngineProvider> providers, EngineType primaryEngine) {
        if (Objects.nonNull(providers) && BooleanUtils.isFalse(providers.isEmpty())) {
            providers.sort(Comparator.comparingInt(RateLimitEngineProvider::getOrder));
        }
        this.providers = providers != null ? providers : Collections.emptyList();
        this.primaryEngine = primaryEngine;
    }

    @Override
    public RateLimitEngineProvider getProvider(EngineType engineType) {
        Objects.requireNonNull(engineType, "engineType must not be null");
        
        for (RateLimitEngineProvider provider : providers) {
            if (provider.getEngineType() == engineType) {
                return provider;
            }
        }
        
        throw new IllegalStateException("EngineProvider not found: " + engineType.name());
    }

    @Override
    public boolean isAvailable(EngineType engineType) {
        if (engineType == null) {
            return false;
        }
        
        for (RateLimitEngineProvider provider : providers) {
            if (provider.getEngineType() == engineType) {
                return provider.isAvailable();
            }
        }
        
        return false;
    }

    @Override
    public List<RateLimitEngineProvider> getAllProviders() {
        return Collections.unmodifiableList(providers);
    }


    /**
     * 根据引擎类型获取引擎提供者
     *
     * @param engine 引擎类型
     * @return 引擎提供者
     */
    @Override
    public EngineType extractEngine(EngineType engine) {

        if (engine != EngineType.AUTO) {
            if (!this.isAvailable(engine)) {
                throw new NoSuchRateLimitEngineException(engine);
            }
            return engine;
        }

        // 是否指定主引擎
        if (Objects.nonNull(this.primaryEngine) && BooleanUtils.isFalse(Objects.equals(this.primaryEngine, EngineType.AUTO))) {
            // 如果指定了主引擎
            return this.primaryEngine;
        }

        // 没有指定主引擎，查看可用引擎
        if (Objects.isNull(this.providers) || this.providers.isEmpty()) {
            throw new NoSuchRateLimitEngineException(EngineType.AUTO);
        }
        return this.providers.get(0).getEngineType();
    }
}