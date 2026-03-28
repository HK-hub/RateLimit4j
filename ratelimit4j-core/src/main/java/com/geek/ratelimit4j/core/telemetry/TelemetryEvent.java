package com.geek.ratelimit4j.core.telemetry;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Telemetry事件
 * 封装限流事件的监控数据，用于记录和上报
 *
 * <p>事件类型：</p>
 * <ul>
 *   <li>ALLOWED：请求通过</li>
 *   <li>REJECTED：请求被限流</li>
 *   <li>FALLBACK_TRIGGERED：降级触发</li>
 *   <li>CIRCUIT_OPEN：熔断触发</li>
 *   <li>CIRCUIT_CLOSED：熔断恢复</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class TelemetryEvent {

    /**
     * 事件类型枚举
     */
    public enum EventType {
        /** 请求通过 */
        ALLOWED("allowed"),
        /** 请求被限流 */
        REJECTED("rejected"),
        /** 降级触发 */
        FALLBACK_TRIGGERED("fallback_triggered"),
        /** 熔断触发 */
        CIRCUIT_OPEN("circuit_open"),
        /** 熔断恢复 */
        CIRCUIT_CLOSED("circuit_closed"),
        /** 存储不可用 */
        STORAGE_UNAVAILABLE("storage_unavailable");

        private final String code;

        EventType(String code) {
            this.code = code;
        }

        public String getCode() {
            return this.code;
        }
    }

    /**
     * 事件类型
     */
    private final EventType eventType;

    /**
     * 限流Key
     */
    private final String key;

    /**
     * 算法类型
     */
    private final AlgorithmType algorithmType;

    /**
     * 事件时间戳
     */
    private final long timestamp;

    /**
     * 等待时间（毫秒）
     */
    private final long waitTimeMs;

    /**
     * 请求的许可数
     */
    private final int requestedPermits;

    /**
     * 剩余许可数
     */
    private final long remainingPermits;

    /**
     * 执行耗时（毫秒）
     */
    private final long executionTimeMs;

    /**
     * 执行模式
     */
    private final String modeType;

    /**
     * 附加属性
     */
    private final Map<String, Object> attributes;

    /**
     * 构造Telemetry事件
     */
    public TelemetryEvent(EventType eventType, String key, AlgorithmType algorithmType,
                          long timestamp, long waitTimeMs, int requestedPermits,
                          long remainingPermits, long executionTimeMs, String modeType) {
        this.eventType = eventType;
        this.key = key;
        this.algorithmType = algorithmType;
        this.timestamp = timestamp;
        this.waitTimeMs = waitTimeMs;
        this.requestedPermits = requestedPermits;
        this.remainingPermits = remainingPermits;
        this.executionTimeMs = executionTimeMs;
        this.modeType = modeType;
        this.attributes = new HashMap<>();
    }

    /**
     * 创建允许事件
     *
     * @param key 限流Key
     * @param algorithmType 算法类型
     * @param requestedPermits 请求许可数
     * @param remainingPermits 剩余许可数
     * @return 允许事件
     */
    public static TelemetryEvent allowed(String key, AlgorithmType algorithmType,
                                         int requestedPermits, long remainingPermits) {
        return new TelemetryEvent(EventType.ALLOWED, key, algorithmType,
                                  System.currentTimeMillis(), 0,
                                  requestedPermits, remainingPermits, 0, "local");
    }

    /**
     * 创建允许事件（带执行时间）
     *
     * @param key 限流Key
     * @param algorithmType 算法类型
     * @param requestedPermits 请求许可数
     * @param remainingPermits 剩余许可数
     * @param executionTimeMs 执行耗时
     * @param modeType 执行模式
     * @return 允许事件
     */
    public static TelemetryEvent allowed(String key, AlgorithmType algorithmType,
                                         int requestedPermits, long remainingPermits,
                                         long executionTimeMs, String modeType) {
        return new TelemetryEvent(EventType.ALLOWED, key, algorithmType,
                                  System.currentTimeMillis(), 0,
                                  requestedPermits, remainingPermits, executionTimeMs, modeType);
    }

    /**
     * 创建拒绝事件
     *
     * @param key 限流Key
     * @param algorithmType 算法类型
     * @param waitTimeMs 等待时间
     * @param requestedPermits 请求许可数
     * @return 拒绝事件
     */
    public static TelemetryEvent rejected(String key, AlgorithmType algorithmType,
                                          long waitTimeMs, int requestedPermits) {
        return new TelemetryEvent(EventType.REJECTED, key, algorithmType,
                                  System.currentTimeMillis(), waitTimeMs,
                                  requestedPermits, 0, 0, "local");
    }

    /**
     * 创建拒绝事件（带执行时间）
     *
     * @param key 限流Key
     * @param algorithmType 算法类型
     * @param waitTimeMs 等待时间
     * @param requestedPermits 请求许可数
     * @param executionTimeMs 执行耗时
     * @param modeType 执行模式
     * @return 拒绝事件
     */
    public static TelemetryEvent rejected(String key, AlgorithmType algorithmType,
                                          long waitTimeMs, int requestedPermits,
                                          long executionTimeMs, String modeType) {
        return new TelemetryEvent(EventType.REJECTED, key, algorithmType,
                                  System.currentTimeMillis(), waitTimeMs,
                                  requestedPermits, 0, executionTimeMs, modeType);
    }

    /**
     * 创建熔断事件
     *
     * @param key 限流Key
     * @param algorithmType 算法类型
     * @return 熔断事件
     */
    public static TelemetryEvent circuitOpen(String key, AlgorithmType algorithmType) {
        return new TelemetryEvent(EventType.CIRCUIT_OPEN, key, algorithmType,
                                  System.currentTimeMillis(), 0, 0, 0, 0, "distributed");
    }

    /**
     * 创建熔断恢复事件
     *
     * @param key 限流Key
     * @param algorithmType 算法类型
     * @return 熔断恢复事件
     */
    public static TelemetryEvent circuitClosed(String key, AlgorithmType algorithmType) {
        return new TelemetryEvent(EventType.CIRCUIT_CLOSED, key, algorithmType,
                                  System.currentTimeMillis(), 0, 0, 0, 0, "distributed");
    }

    // Getter方法

    public EventType getEventType() {
        return this.eventType;
    }

    public String getKey() {
        return this.key;
    }

    public AlgorithmType getAlgorithmType() {
        return this.algorithmType;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public long getWaitTimeMs() {
        return this.waitTimeMs;
    }

    public int getRequestedPermits() {
        return this.requestedPermits;
    }

    public long getRemainingPermits() {
        return this.remainingPermits;
    }

    public long getExecutionTimeMs() {
        return this.executionTimeMs;
    }

    public String getModeType() {
        return this.modeType;
    }

    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    /**
     * 添加附加属性
     *
     * @param name 属性名
     * @param value 属性值
     * @return 当前事件对象
     */
    public TelemetryEvent withAttribute(String name, Object value) {
        this.attributes.put(name, value);
        return this;
    }

    /**
     * 判断是否为限流拒绝事件
     *
     * @return true表示被限流
     */
    public boolean isRejected() {
        return Objects.equals(eventType, EventType.REJECTED);
    }

    /**
     * 获取事件描述
     *
     * @return 事件描述字符串
     */
    public String getDescription() {
        return String.format("TelemetryEvent[type=%s, key=%s, algorithm=%s, permits=%d]",
                             eventType.getCode(), key,
                             Objects.nonNull(algorithmType) ? algorithmType.getCode() : "null",
                             requestedPermits);
    }
}