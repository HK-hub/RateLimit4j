package com.geek.ratelimit4j.core.engine;

import com.geek.ratelimit4j.core.config.EngineType;

/**
 * 限流引擎提供者接口
 * 引擎实现类需实现此接口，并可通过getOrder()指定优先级
 *
 * <p>Order值越小，优先级越高</p>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public interface RateLimitEngineProvider {

    /**
     * 获取引擎类型
     *
     * @return 引擎类型
     */
    EngineType getEngineType();

    /**
     * 获取引擎优先级
     * Order值越小，优先级越高
     * 默认优先级为100
     *
     * @return 优先级值
     */
    default int getOrder() {
        return 100;
    }

    /**
     * 判断引擎是否可用
     * 用于健康检查或条件判断
     *
     * @return true表示可用
     */
    default boolean isAvailable() {
        return true;
    }
}