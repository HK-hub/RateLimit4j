package com.geek.ratelimit4j.core.telemetry;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Telemetry事件
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryEvent {

    private EventType eventType;
    private String key;
    private AlgorithmType algorithmType;
    private long timestamp;
    private long waitTimeMs;
    private int requestedPermits;
    private long remainingPermits;
    private long executionTimeMs;
    private String modeType;
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    public enum EventType {
        ALLOWED("allowed"),
        REJECTED("rejected"),
        FALLBACK_TRIGGERED("fallback_triggered"),
        CIRCUIT_OPEN("circuit_open"),
        CIRCUIT_CLOSED("circuit_closed"),
        STORAGE_UNAVAILABLE("storage_unavailable");

        private final String code;

        EventType(String code) {
            this.code = code;
        }

        public String getCode() {
            return this.code;
        }
    }

    public static TelemetryEvent allowed(String key, AlgorithmType algorithmType,
                                          int requestedPermits, long remainingPermits) {
        return TelemetryEvent.builder()
                .eventType(EventType.ALLOWED)
                .key(key)
                .algorithmType(algorithmType)
                .timestamp(System.currentTimeMillis())
                .requestedPermits(requestedPermits)
                .remainingPermits(remainingPermits)
                .modeType("local")
                .build();
    }

    public static TelemetryEvent allowed(String key, AlgorithmType algorithmType,
                                          int requestedPermits, long remainingPermits,
                                          long executionTimeMs, String modeType) {
        return TelemetryEvent.builder()
                .eventType(EventType.ALLOWED)
                .key(key)
                .algorithmType(algorithmType)
                .timestamp(System.currentTimeMillis())
                .requestedPermits(requestedPermits)
                .remainingPermits(remainingPermits)
                .executionTimeMs(executionTimeMs)
                .modeType(modeType)
                .build();
    }

    public static TelemetryEvent rejected(String key, AlgorithmType algorithmType,
                                           long waitTimeMs, int requestedPermits) {
        return TelemetryEvent.builder()
                .eventType(EventType.REJECTED)
                .key(key)
                .algorithmType(algorithmType)
                .timestamp(System.currentTimeMillis())
                .waitTimeMs(waitTimeMs)
                .requestedPermits(requestedPermits)
                .modeType("local")
                .build();
    }

    public static TelemetryEvent rejected(String key, AlgorithmType algorithmType,
                                           long waitTimeMs, int requestedPermits,
                                           long executionTimeMs, String modeType) {
        return TelemetryEvent.builder()
                .eventType(EventType.REJECTED)
                .key(key)
                .algorithmType(algorithmType)
                .timestamp(System.currentTimeMillis())
                .waitTimeMs(waitTimeMs)
                .requestedPermits(requestedPermits)
                .executionTimeMs(executionTimeMs)
                .modeType(modeType)
                .build();
    }

    public static TelemetryEvent circuitOpen(String key, AlgorithmType algorithmType) {
        return TelemetryEvent.builder()
                .eventType(EventType.CIRCUIT_OPEN)
                .key(key)
                .algorithmType(algorithmType)
                .timestamp(System.currentTimeMillis())
                .modeType("distributed")
                .build();
    }

    public static TelemetryEvent circuitClosed(String key, AlgorithmType algorithmType) {
        return TelemetryEvent.builder()
                .eventType(EventType.CIRCUIT_CLOSED)
                .key(key)
                .algorithmType(algorithmType)
                .timestamp(System.currentTimeMillis())
                .modeType("distributed")
                .build();
    }

    public TelemetryEvent withAttribute(String name, Object value) {
        if (Objects.isNull(this.attributes)) {
            this.attributes = new HashMap<>();
        }
        this.attributes.put(name, value);
        return this;
    }

    public boolean isRejected() {
        return Objects.equals(eventType, EventType.REJECTED);
    }

    public String getDescription() {
        return String.format("TelemetryEvent[type=%s, key=%s, algorithm=%s, permits=%d]",
                eventType.getCode(), key,
                Objects.nonNull(algorithmType) ? algorithmType.getCode() : "null",
                requestedPermits);
    }
}