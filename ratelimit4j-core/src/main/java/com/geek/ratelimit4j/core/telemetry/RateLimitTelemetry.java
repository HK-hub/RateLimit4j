package com.geek.ratelimit4j.core.telemetry;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;

/**
 * 限流监控追踪接口
 * 定义限流事件的记录和上报方法
 *
 * <p>实现要求：</p>
 * <ul>
 *   <li>实现类必须线程安全</li>
 *   <li>监控异常不应影响限流功能</li>
 *   <li>支持异步上报以提高性能</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * RateLimitTelemetry telemetry = new OpenTelemetryRateLimitTelemetry(config);
 * telemetry.recordAllowed("api:user", AlgorithmType.TOKEN_BUCKET, 1, 99);
 * }</pre>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public interface RateLimitTelemetry {

    /**
     * 记录限流事件
     *
     * @param event Telemetry事件
     */
    void recordEvent(TelemetryEvent event);

    /**
     * 记录请求通过事件
     *
     * @param key 限流Key
     * @param algorithmType 算法类型
     * @param permits 获取的许可数
     * @param remaining 剩余许可数
     */
    void recordAllowed(String key, AlgorithmType algorithmType,
                       int permits, long remaining);

    /**
     * 记录请求被限流事件
     *
     * @param key 限流Key
     * @param algorithmType 算法类型
     * @param permits 请求的许可数
     * @param waitTimeMs 等待时间
     */
    void recordRejected(String key, AlgorithmType algorithmType,
                        int permits, long waitTimeMs);

    /**
     * 记录算法执行延迟
     *
     * @param algorithmType 算法类型
     * @param latencyMs 执行延迟（毫秒）
     */
    void recordLatency(AlgorithmType algorithmType, long latencyMs);

    /**
     * 记录熔断事件
     *
     * @param key 限流Key
     * @param algorithmType 算法类型
     * @param open true表示熔断打开，false表示熔断恢复
     */
    void recordCircuitBreaker(String key, AlgorithmType algorithmType, boolean open);

    /**
     * 记录存储不可用事件
     *
     * @param storageType 存储类型
     * @param errorMessage 错误信息
     */
    void recordStorageUnavailable(String storageType, String errorMessage);

    /**
     * 获取Telemetry配置
     *
     * @return Telemetry配置对象
     */
    TelemetryConfig getConfig();

    /**
     * 判断Telemetry是否启用
     *
     * @return true表示启用
     */
    boolean isEnabled();

    /**
     * 关闭Telemetry，释放资源
     */
    void shutdown();
}