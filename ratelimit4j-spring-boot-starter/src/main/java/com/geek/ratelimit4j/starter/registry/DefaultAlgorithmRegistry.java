package com.geek.ratelimit4j.starter.registry;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import com.geek.ratelimit4j.core.algorithm.RateLimitAlgorithm;
import com.geek.ratelimit4j.core.config.EngineType;
import com.geek.ratelimit4j.core.registry.AbstractAlgorithmRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Objects;

/**
 * 默认算法注册中心实现
 * 基于Spring自动装配，自动发现并注册所有限流算法Bean
 *
 * <p>特点：</p>
 * <ul>
 *   <li>使用ObjectProvider延迟获取算法列表，避免循环依赖</li>
 *   <li>首次访问时自动初始化</li>
 *   <li>支持重复注册检测（保留已存在的算法）</li>
 * </ul>
 *
 * <p>由RateLimitAutoConfiguration配置注入，支持用户自定义实现替换</p>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Slf4j
public class DefaultAlgorithmRegistry extends AbstractAlgorithmRegistry {

    /**
     * 算法列表提供者
     * 使用ObjectProvider延迟获取，避免循环依赖
     */
    private final ObjectProvider<List<RateLimitAlgorithm>> algorithmsProvider;

    /**
     * 是否已初始化
     */
    private volatile boolean initialized = false;

    /**
     * 构造默认算法注册中心
     *
     * @param algorithmsProvider 算法列表提供者
     */
    public DefaultAlgorithmRegistry(ObjectProvider<List<RateLimitAlgorithm>> algorithmsProvider) {
        this.algorithmsProvider = algorithmsProvider;
        log.info("[RateLimit4j] DefaultAlgorithmRegistry created, algorithms will be registered on first access");
    }

    /**
     * 确保算法已注册
     * 延迟初始化，首次访问时执行
     */
    private void ensureInitialized() {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            doInitialize();
            initialized = true;
        }
    }

    /**
     * 执行初始化
     * 从ObjectProvider获取算法列表并注册
     */
    private void doInitialize() {
        List<RateLimitAlgorithm> algorithms = algorithmsProvider.getIfAvailable();
        if (Objects.isNull(algorithms) || algorithms.isEmpty()) {
            log.warn("[RateLimit4j] No RateLimitAlgorithm beans found in container");
            return;
        }

        log.info("[RateLimit4j] DefaultAlgorithmRegistry initializing with {} algorithm beans", algorithms.size());

        for (RateLimitAlgorithm algorithm : algorithms) {
            register(algorithm);
        }

        log.info("[RateLimit4j] Registered {} local algorithms, {} redis algorithms",
                 getAlgorithmCount(EngineType.LOCAL), getAlgorithmCount(EngineType.REDIS));
    }

    @Override
    public RateLimitAlgorithm getAlgorithm(AlgorithmType algorithmType, EngineType engineType) {
        ensureInitialized();
        return super.getAlgorithm(algorithmType, engineType);
    }

    @Override
    public boolean hasEngineAlgorithms(EngineType engineType) {
        ensureInitialized();
        return super.hasEngineAlgorithms(engineType);
    }

    @Override
    protected void onAlgorithmRegistered(AlgorithmType algorithmType, EngineType engineType,
                                          RateLimitAlgorithm algorithm) {
        log.debug("[RateLimit4j] Registered algorithm: type={}, engine={}, class={}",
                  algorithmType.getCode(), engineType.getCode(),
                  algorithm.getClass().getSimpleName());
    }

    @Override
    protected void onAlgorithmUnregistered(AlgorithmType algorithmType, EngineType engineType,
                                            RateLimitAlgorithm algorithm) {
        log.debug("[RateLimit4j] Unregistered algorithm: type={}, engine={}, class={}",
                  algorithmType.getCode(), engineType.getCode(),
                  algorithm.getClass().getSimpleName());
    }

    @Override
    protected void onDuplicateAlgorithm(AlgorithmType algorithmType, EngineType engineType,
                                         RateLimitAlgorithm existing, RateLimitAlgorithm newAlgorithm) {
        log.warn("[RateLimit4j] Duplicate algorithm found: type={}, engine={}, " +
                 "existing={}, new={}, keeping existing",
                 algorithmType.getCode(), engineType.getCode(),
                 existing.getClass().getSimpleName(), newAlgorithm.getClass().getSimpleName());
    }

    @Override
    protected void onCleared(EngineType engineType) {
        if (Objects.isNull(engineType)) {
            log.info("[RateLimit4j] All algorithms cleared");
        } else {
            log.info("[RateLimit4j] Algorithms cleared for engine: {}", engineType.getCode());
        }
    }
}