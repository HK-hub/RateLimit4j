package com.geek.ratelimit4j.core.exception;

import com.geek.ratelimit4j.core.config.EngineType;

/**
 * 限流引擎不存在异常
 * 当注解指定了具体引擎，但当前Spring容器中没有对应的引擎实现时抛出
 *
 * <p>场景示例：</p>
 * <ul>
 *   <li>注解指定 engine = REDIS，但未配置Redis连接</li>
 *   <li>注解指定 engine = LOCAL，但未引入本地引擎模块</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class NoSuchRateLimitEngineException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 缺失的引擎类型
     */
    private final EngineType engineType;

    /**
     * 限流Key
     */
    private final String key;

    /**
     * 构造引擎不存在异常
     *
     * @param engineType 缺失的引擎类型
     */
    public NoSuchRateLimitEngineException(EngineType engineType) {
        super(String.format("Rate limit engine not found: %s. " +
                "Please ensure the corresponding engine module is configured.", engineType));
        this.engineType = engineType;
        this.key = null;
    }

    /**
     * 构造引擎不存在异常（带Key信息）
     *
     * @param engineType 缺失的引擎类型
     * @param key        限流Key
     */
    public NoSuchRateLimitEngineException(EngineType engineType, String key) {
        super(String.format("Rate limit engine not found: %s for key: %s. " +
                "Please ensure the corresponding engine module is configured.", engineType, key));
        this.engineType = engineType;
        this.key = key;
    }

    /**
     * 构造引擎不存在异常（带原因）
     *
     * @param engineType 缺失的引擎类型
     * @param message    详细消息
     * @param cause      原因
     */
    public NoSuchRateLimitEngineException(EngineType engineType, String message, Throwable cause) {
        super(message, cause);
        this.engineType = engineType;
        this.key = null;
    }

    /**
     * 获取缺失的引擎类型
     *
     * @return 引擎类型
     */
    public EngineType getEngineType() {
        return engineType;
    }

    /**
     * 获取限流Key
     *
     * @return 限流Key，可能为null
     */
    public String getKey() {
        return key;
    }
}