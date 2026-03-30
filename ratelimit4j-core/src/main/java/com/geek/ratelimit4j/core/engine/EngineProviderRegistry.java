package com.geek.ratelimit4j.core.engine;

import com.geek.ratelimit4j.core.config.EngineType;
import java.util.List;

/**
 * 引擎提供者注册中心接口
 * 管理所有引擎提供者，支持按类型精确获取
 *
 * <p>设计说明：</p>
 * <ul>
 *   <li>接口定义核心契约，便于扩展</li>
 *   <li>支持用户自定义实现</li>
 *   <li>遵循开闭原则</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public interface EngineProviderRegistry {

    /**
     * 获取指定类型的引擎提供者
     *
     * @param engineType 引擎类型
     * @return 引擎提供者
     * @throws IllegalStateException 当引擎提供者未找到时抛出
     */
    RateLimitEngineProvider getProvider(EngineType engineType);

    /**
     * 检查指定引擎是否可用
     *
     * @param engineType 引擎类型
     * @return true表示可用
     */
    boolean isAvailable(EngineType engineType);

    /**
     * 获取所有引擎提供者
     *
     * @return 引擎提供者列表
     */
    List<RateLimitEngineProvider> getAllProviders();


    /**
     * 从作用域中提取引擎类型
     *
     * @param engine 引擎类型
     * @return 引擎类型
     */
    EngineType extractEngine(EngineType engine);
}