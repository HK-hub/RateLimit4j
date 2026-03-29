package com.geek.ratelimit4j.starter.telemetry;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import com.geek.ratelimit4j.core.telemetry.RateLimitTelemetry;
import com.geek.ratelimit4j.core.telemetry.TelemetryConfig;
import com.geek.ratelimit4j.core.telemetry.TelemetryEvent;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import lombok.Getter;

import java.util.Objects;

/**
 * OpenTelemetry限流监控实现
 * 提供Metrics指标和Tracing追踪功能
 *
 * <p>Metrics指标：</p>
 * <ul>
 *   <li>ratelimit.requests.total - 总请求计数</li>
 *   <li>ratelimit.requests.allowed - 允许通过数</li>
 *   <li>ratelimit.requests.rejected - 被限流数</li>
 *   <li>ratelimit.wait.time - 等待时间</li>
 *   <li>ratelimit.algorithm.latency - 算法执行延迟</li>
 * </ul>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
@Getter
public class OpenTelemetryRateLimitTelemetry implements RateLimitTelemetry {

    private static final String METER_NAME = "ratelimit4j";
    private static final String TRACER_NAME = "ratelimit4j";

    private final TelemetryConfig config;
    private final Meter meter;
    private final Tracer tracer;

    private final LongCounter requestCounter;
    private final LongCounter allowedCounter;
    private final LongCounter rejectedCounter;

    /**
     * 构造OpenTelemetry监控
     *
     * @param config 监控配置
     */
    public OpenTelemetryRateLimitTelemetry(TelemetryConfig config) {
        this.config = config;

        if (config.isEnabled()) {
            var openTelemetry = GlobalOpenTelemetry.get();
            this.meter = openTelemetry.getMeter(METER_NAME);
            this.tracer = openTelemetry.getTracer(TRACER_NAME);
        } else {
            this.meter = null;
            this.tracer = null;
        }

        if (Objects.nonNull(meter)) {
            this.requestCounter = meter.counterBuilder("ratelimit.requests.total")
                    .setDescription("Total rate limit requests")
                    .build();

            this.allowedCounter = meter.counterBuilder("ratelimit.requests.allowed")
                    .setDescription("Allowed requests")
                    .build();

            this.rejectedCounter = meter.counterBuilder("ratelimit.requests.rejected")
                    .setDescription("Rejected requests")
                    .build();
        } else {
            this.requestCounter = null;
            this.allowedCounter = null;
            this.rejectedCounter = null;
        }
    }

    @Override
    public void recordEvent(TelemetryEvent event) {
        if (!isEnabled() || Objects.isNull(meter)) {
            return;
        }

        Attributes attributes = Attributes.of(
                AttributeKey.stringKey("key"), event.getKey(),
                AttributeKey.stringKey("algorithm"), 
                        Objects.nonNull(event.getAlgorithmType()) ? event.getAlgorithmType().getCode() : "unknown",
                AttributeKey.stringKey("mode"), event.getModeType()
        );

        if (Objects.nonNull(requestCounter)) {
            requestCounter.add(1, attributes);
        }

        if (event.isRejected()) {
            if (Objects.nonNull(rejectedCounter)) {
                rejectedCounter.add(1, attributes);
            }
        } else {
            if (Objects.nonNull(allowedCounter)) {
                allowedCounter.add(1, attributes);
            }
        }

        if (config.isTracingEnabled() && Objects.nonNull(tracer)) {
            recordSpan(event);
        }
    }

    @Override
    public void recordAllowed(String key, AlgorithmType algorithmType, int permits, long remaining) {
        TelemetryEvent event = TelemetryEvent.allowed(key, algorithmType, permits, remaining);
        recordEvent(event);
    }

    @Override
    public void recordRejected(String key, AlgorithmType algorithmType, int permits, long waitTimeMs) {
        TelemetryEvent event = TelemetryEvent.rejected(key, algorithmType, waitTimeMs, permits);
        recordEvent(event);
    }

    @Override
    public void recordLatency(AlgorithmType algorithmType, long latencyMs) {
        if (!isEnabled() || Objects.isNull(meter)) {
            return;
        }

        meter.histogramBuilder("ratelimit.algorithm.latency")
                .setDescription("Algorithm execution latency")
                .setUnit("ms")
                .build()
                .record(latencyMs, Attributes.of(
                        AttributeKey.stringKey("algorithm"), algorithmType.getCode()
                ));
    }

    @Override
    public void recordCircuitBreaker(String key, AlgorithmType algorithmType, boolean open) {
        TelemetryEvent event = open 
                ? TelemetryEvent.circuitOpen(key, algorithmType)
                : TelemetryEvent.circuitClosed(key, algorithmType);
        recordEvent(event);
    }

    @Override
    public void recordStorageUnavailable(String storageType, String errorMessage) {
        if (!isEnabled() || Objects.isNull(meter)) {
            return;
        }

        meter.counterBuilder("ratelimit.storage.unavailable")
                .setDescription("Storage unavailable events")
                .build()
                .add(1, Attributes.of(
                        AttributeKey.stringKey("storage_type"), storageType,
                        AttributeKey.stringKey("error"), errorMessage
                ));
    }

    @Override
    public boolean isEnabled() {
        return Objects.nonNull(config) && config.isEnabled();
    }

    @Override
    public void shutdown() {
    }

    /**
     * 记录Span追踪
     */
    private void recordSpan(TelemetryEvent event) {
        if (Objects.isNull(tracer)) {
            return;
        }

        Span span = tracer.spanBuilder("RateLimit." + event.getEventType().getCode())
                .startSpan();

        try {
            span.setAttribute("ratelimit.key", event.getKey());
            span.setAttribute("ratelimit.algorithm", 
                    Objects.nonNull(event.getAlgorithmType()) ? event.getAlgorithmType().getCode() : "unknown");
            span.setAttribute("ratelimit.permits", event.getRequestedPermits());
            span.setAttribute("ratelimit.mode", event.getModeType());

            if (event.isRejected()) {
                span.setAttribute("ratelimit.wait_time_ms", event.getWaitTimeMs());
            } else {
                span.setAttribute("ratelimit.remaining", event.getRemainingPermits());
            }

            span.setAttribute("ratelimit.execution_time_ms", event.getExecutionTimeMs());
        } finally {
            span.end();
        }
    }
}