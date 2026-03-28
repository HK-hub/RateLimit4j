package com.geek.ratelimit4j.core.telemetry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * Telemetry配置类
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryConfig {

    private boolean enabled = true;
    private String serviceName = "RateLimit4j";
    private String endpoint = "http://localhost:4317";
    private boolean detailedLogging = false;
    private double samplingRate = 1.0;
    private boolean metricsEnabled = true;
    private boolean tracingEnabled = true;

    public static TelemetryConfig defaultConfig() {
        return TelemetryConfig.builder().build();
    }

    public static TelemetryConfig disabled() {
        return TelemetryConfig.builder().enabled(false).build();
    }

    public boolean isValid() {
        return StringUtils.isNotBlank(serviceName);
    }
}