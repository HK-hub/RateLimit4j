package com.geek.ratelimit4j.core.algorithm;

import com.geek.ratelimit4j.core.config.EngineType;

/**
 * 引擎感知接口
 * 算法实现类可实现此接口来明确指定所属引擎类型
 *
 * <p>如果不实现此接口，AlgorithmRegistry将通过包路径自动推断引擎类型：</p>
 * <ul>
 *   <li>包路径包含 "local" -> LOCAL引擎</li>
 *   <li>包路径包含 "redis" -> REDIS引擎</li>
 * </ul>
 *
 * <p>用户自定义算法可实现此接口明确指定引擎类型</p>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public interface EngineAware {

    /**
     * 获取算法所属引擎类型
     *
     * @return 引擎类型
     */
    EngineType getEngineType();
}