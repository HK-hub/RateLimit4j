package com.geek.ratelimit4j.core.registry;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import com.geek.ratelimit4j.core.algorithm.RateLimitAlgorithm;
import com.geek.ratelimit4j.core.config.EngineType;

import java.util.Map;

/**
 * 算法注册中心接口
 * 定义限流算法的注册、查询和管理能力
 *
 * <p>设计说明：</p>
 * <ul>
 *   <li>接口定义核心能力，不依赖具体实现框架</li>
 *   <li>支持多种引擎类型（LOCAL/REDIS等）的算法管理</li>
 *   <li>支持运行时动态注册算法</li>
 * </ul>
 *
 * <p>扩展点：</p>
 * <ul>
 *   <li>用户可实现此接口提供自定义注册中心</li>
 *   <li>可继承AbstractAlgorithmRegistry复用通用逻辑</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public interface AlgorithmRegistry {

    /**
     * 注册算法
     * 根据算法的引擎类型自动分类存储
     *
     * @param algorithm 算法实现
     * @throws IllegalArgumentException 当algorithm为null时抛出
     */
    void register(RateLimitAlgorithm algorithm);

    /**
     * 注销算法
     * 从注册中心移除指定算法
     *
     * @param algorithmType 算法类型
     * @param engineType    引擎类型
     * @return 被移除的算法，不存在时返回null
     */
    RateLimitAlgorithm unregister(AlgorithmType algorithmType, EngineType engineType);

    /**
     * 获取指定类型和引擎的算法
     *
     * @param algorithmType 算法类型
     * @param engineType    引擎类型
     * @return 算法实现，不存在时返回null
     */
    RateLimitAlgorithm getAlgorithm(AlgorithmType algorithmType, EngineType engineType);

    /**
     * 获取指定引擎的所有算法
     *
     * @param engineType 引擎类型
     * @return 算法映射（算法类型 -> 算法实现），返回不可变副本
     */
    Map<AlgorithmType, RateLimitAlgorithm> getAlgorithms(EngineType engineType);

    /**
     * 判断指定类型算法在指定引擎中是否存在
     *
     * @param algorithmType 算法类型
     * @param engineType    引擎类型
     * @return true表示存在
     */
    boolean hasAlgorithm(AlgorithmType algorithmType, EngineType engineType);

    /**
     * 判断指定引擎是否有可用算法
     *
     * @param engineType 引擎类型
     * @return true表示有可用算法
     */
    boolean hasEngineAlgorithms(EngineType engineType);

    /**
     * 获取指定引擎的算法数量
     *
     * @param engineType 引擎类型
     * @return 算法数量
     */
    int getAlgorithmCount(EngineType engineType);

    /**
     * 清空指定引擎的所有算法
     *
     * @param engineType 引擎类型
     */
    void clear(EngineType engineType);

    /**
     * 清空所有算法
     */
    void clearAll();
}