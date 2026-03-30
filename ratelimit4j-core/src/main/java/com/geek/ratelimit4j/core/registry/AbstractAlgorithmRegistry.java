package com.geek.ratelimit4j.core.registry;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import com.geek.ratelimit4j.core.algorithm.EngineAware;
import com.geek.ratelimit4j.core.algorithm.RateLimitAlgorithm;
import com.geek.ratelimit4j.core.config.EngineType;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 算法注册中心抽象实现
 * 提供算法注册、查询和管理的通用逻辑
 *
 * <p>模板方法设计模式：</p>
 * <ul>
 *   <li>定义算法注册的核心流程</li>
 *   <li>子类可扩展特定行为（如日志记录、事件通知）</li>
 *   <li>提供引擎类型解析的扩展点</li>
 * </ul>
 *
 * <p>线程安全：</p>
 * <ul>
 *   <li>使用ConcurrentHashMap存储算法</li>
 *   <li>支持并发注册和查询</li>
 * </ul>
 *
 * <p>引擎类型解析优先级：</p>
 * <ol>
 *   <li>{@link #resolveEngineType(RateLimitAlgorithm)} 子类可覆盖</li>
 *   <li>EngineAware接口返回值</li>
 *   <li>类名/包名推断</li>
 *   <li>默认LOCAL</li>
 * </ol>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public abstract class AbstractAlgorithmRegistry implements AlgorithmRegistry {

    /**
     * 本地算法缓存
     */
    protected final Map<AlgorithmType, RateLimitAlgorithm> localAlgorithms = new ConcurrentHashMap<>();

    /**
     * Redis算法缓存
     */
    protected final Map<AlgorithmType, RateLimitAlgorithm> redisAlgorithms = new ConcurrentHashMap<>();

    @Override
    public void register(RateLimitAlgorithm algorithm) {
        if (Objects.isNull(algorithm)) {
            throw new IllegalArgumentException("Algorithm cannot be null");
        }

        AlgorithmType algorithmType = algorithm.getType();
        if (Objects.isNull(algorithmType)) {
            throw new IllegalArgumentException("Algorithm type cannot be null");
        }

        EngineType engineType = resolveEngineType(algorithm);
        Map<AlgorithmType, RateLimitAlgorithm> targetCache = getTargetCache(engineType);

        RateLimitAlgorithm existing = targetCache.get(algorithmType);
        if (Objects.nonNull(existing)) {
            onDuplicateAlgorithm(algorithmType, engineType, existing, algorithm);
            return;
        }

        targetCache.put(algorithmType, algorithm);
        onAlgorithmRegistered(algorithmType, engineType, algorithm);
    }

    @Override
    public RateLimitAlgorithm unregister(AlgorithmType algorithmType, EngineType engineType) {
        Map<AlgorithmType, RateLimitAlgorithm> cache = getTargetCache(engineType);
        RateLimitAlgorithm removed = cache.remove(algorithmType);
        if (Objects.nonNull(removed)) {
            onAlgorithmUnregistered(algorithmType, engineType, removed);
        }
        return removed;
    }

    @Override
    public RateLimitAlgorithm getAlgorithm(AlgorithmType algorithmType, EngineType engineType) {
        Map<AlgorithmType, RateLimitAlgorithm> cache = getTargetCache(engineType);
        return cache.get(algorithmType);
    }

    @Override
    public Map<AlgorithmType, RateLimitAlgorithm> getAlgorithms(EngineType engineType) {
        Map<AlgorithmType, RateLimitAlgorithm> cache = getTargetCache(engineType);
        return Collections.unmodifiableMap(cache);
    }

    @Override
    public boolean hasAlgorithm(AlgorithmType algorithmType, EngineType engineType) {
        return Objects.nonNull(getAlgorithm(algorithmType, engineType));
    }

    @Override
    public boolean hasEngineAlgorithms(EngineType engineType) {
        return !getTargetCache(engineType).isEmpty();
    }

    @Override
    public int getAlgorithmCount(EngineType engineType) {
        return getTargetCache(engineType).size();
    }

    @Override
    public void clear(EngineType engineType) {
        getTargetCache(engineType).clear();
        onCleared(engineType);
    }

    @Override
    public void clearAll() {
        localAlgorithms.clear();
        redisAlgorithms.clear();
        onCleared(null);
    }

    /**
     * 解析算法所属引擎类型
     * 子类可覆盖此方法提供自定义解析逻辑
     *
     * @param algorithm 算法实现
     * @return 引擎类型
     */
    protected EngineType resolveEngineType(RateLimitAlgorithm algorithm) {
        if (algorithm instanceof EngineAware) {
            EngineType engineType = ((EngineAware) algorithm).getEngineType();
            if (Objects.nonNull(engineType)) {
                return engineType;
            }
        }

        return inferEngineTypeByClassName(algorithm);
    }

    /**
     * 根据类名推断引擎类型
     * 默认实现：包路径或类名包含关键字推断
     *
     * @param algorithm 算法实现
     * @return 推断的引擎类型，默认LOCAL
     */
    protected EngineType inferEngineTypeByClassName(RateLimitAlgorithm algorithm) {
        String className = algorithm.getClass().getName();

        if (className.contains(".redis.") || className.contains("Redis")) {
            return EngineType.REDIS;
        }

        if (className.contains(".local.") || className.contains("Local")) {
            return EngineType.LOCAL;
        }

        return getDefaultEngineType();
    }

    /**
     * 获取默认引擎类型
     * 子类可覆盖以改变默认行为
     *
     * @return 默认引擎类型
     */
    protected EngineType getDefaultEngineType() {
        return EngineType.LOCAL;
    }

    /**
     * 获取目标算法缓存
     *
     * @param engineType 引擎类型
     * @return 对应的算法缓存Map
     */
    protected Map<AlgorithmType, RateLimitAlgorithm> getTargetCache(EngineType engineType) {
        if (engineType == EngineType.REDIS) {
            return redisAlgorithms;
        }
        return localAlgorithms;
    }

    /**
     * 算法注册成功回调
     * 子类可覆盖以添加日志、监控等
     *
     * @param algorithmType 算法类型
     * @param engineType    引擎类型
     * @param algorithm     算法实现
     */
    protected abstract void onAlgorithmRegistered(AlgorithmType algorithmType, EngineType engineType,
                                                   RateLimitAlgorithm algorithm);

    /**
     * 算法注销回调
     * 子类可覆盖以添加日志、监控等
     *
     * @param algorithmType 算法类型
     * @param engineType    引擎类型
     * @param algorithm     被移除的算法
     */
    protected abstract void onAlgorithmUnregistered(AlgorithmType algorithmType, EngineType engineType,
                                                     RateLimitAlgorithm algorithm);

    /**
     * 发现重复算法回调
     * 子类可覆盖以自定义处理逻辑（如覆盖或忽略）
     *
     * @param algorithmType 算法类型
     * @param engineType    引擎类型
     * @param existing      已存在的算法
     * @param newAlgorithm  新注册的算法
     */
    protected abstract void onDuplicateAlgorithm(AlgorithmType algorithmType, EngineType engineType,
                                                  RateLimitAlgorithm existing, RateLimitAlgorithm newAlgorithm);

    /**
     * 清空算法回调
     *
     * @param engineType 被清空的引擎类型，null表示清空所有
     */
    protected abstract void onCleared(EngineType engineType);
}