package com.geek.ratelimit4j.core.telemetry;

import java.util.Objects;

/**
 * Telemetry配置类
 * 定义监控追踪的配置参数
 *
 * <p>配置项说明：</p>
 * <ul>
 *   <li>enabled：是否启用监控</li>
 *   <li>serviceName：服务名称，用于Tracing标识</li>
 *   <li>endpoint：OpenTelemetry Collector端点</li>
 *   <li>detailedLogging：是否记录详细日志</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class TelemetryConfig {

    /**
     * 是否启用监控
     */
    private final boolean enabled;

    /**
     * 服务名称，用于Tracing标识
     */
    private final String serviceName;

    /**
     * OpenTelemetry Collector端点
     */
    private final String endpoint;

    /**
     * 是否记录详细日志
     */
    private final boolean detailedLogging;

    /**
     * 采样率（0.0-1.0）
     */
    private final double samplingRate;

    /**
     * 是否启用Metrics
     */
    private final boolean metricsEnabled;

    /**
     * 是否启用Tracing
     */
    private final boolean tracingEnabled;

    /**
     * 构造Telemetry配置
     *
     * @param enabled 是否启用
     * @param serviceName 服务名称
     * @param endpoint 端点地址
     * @param detailedLogging 是否详细日志
     */
    public TelemetryConfig(boolean enabled, String serviceName, String endpoint, boolean detailedLogging) {
        this.enabled = enabled;
        this.serviceName = serviceName;
        this.endpoint = endpoint;
        this.detailedLogging = detailedLogging;
        this.samplingRate = 1.0;
        this.metricsEnabled = true;
        this.tracingEnabled = true;
    }

    /**
     * 完整构造函数
     */
    public TelemetryConfig(boolean enabled, String serviceName, String endpoint,
                           boolean detailedLogging, double samplingRate,
                           boolean metricsEnabled, boolean tracingEnabled) {
        this.enabled = enabled;
        this.serviceName = serviceName;
        this.endpoint = endpoint;
        this.detailedLogging = detailedLogging;
        this.samplingRate = samplingRate;
        this.metricsEnabled = metricsEnabled;
        this.tracingEnabled = tracingEnabled;
    }

    /**
     * 创建默认配置
     *
     * @return 默认配置对象
     */
    public static TelemetryConfig defaultConfig() {
        return new TelemetryConfig(true, "RateLimit4j",
                                   "http://localhost:4317", false);
    }

    /**
     * 创建禁用配置
     *
     * @return 禁用的配置对象
     */
    public static TelemetryConfig disabled() {
        return new TelemetryConfig(false, null, null, false);
    }

    /**
     * 创建Builder
     *
     * @return Builder对象
     */
    public static Builder builder() {
        return new Builder();
    }

    // Getter方法

    public boolean isEnabled() {
        return this.enabled;
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public String getEndpoint() {
        return this.endpoint;
    }

    public boolean isDetailedLogging() {
        return this.detailedLogging;
    }

    public double getSamplingRate() {
        return this.samplingRate;
    }

    public boolean isMetricsEnabled() {
        return this.metricsEnabled;
    }

    public boolean isTracingEnabled() {
        return this.tracingEnabled;
    }

    /**
     * 判断配置是否有效
     *
     * @return true表示有效
     */
    public boolean isValid() {
        return Objects.nonNull(serviceName) && !serviceName.isEmpty();
    }

    /**
     * Builder类
     */
    public static class Builder {
        private boolean enabled = true;
        private String serviceName = "RateLimit4j";
        private String endpoint = "http://localhost:4317";
        private boolean detailedLogging = false;
        private double samplingRate = 1.0;
        private boolean metricsEnabled = true;
        private boolean tracingEnabled = true;

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder detailedLogging(boolean detailedLogging) {
            this.detailedLogging = detailedLogging;
            return this;
        }

        public Builder samplingRate(double samplingRate) {
            this.samplingRate = samplingRate;
            return this;
        }

        public Builder metricsEnabled(boolean metricsEnabled) {
            this.metricsEnabled = metricsEnabled;
            return this;
        }

        public Builder tracingEnabled(boolean tracingEnabled) {
            this.tracingEnabled = tracingEnabled;
            return this;
        }

        public TelemetryConfig build() {
            return new TelemetryConfig(enabled, serviceName, endpoint,
                                       detailedLogging, samplingRate,
                                       metricsEnabled, tracingEnabled);
        }
    }
}