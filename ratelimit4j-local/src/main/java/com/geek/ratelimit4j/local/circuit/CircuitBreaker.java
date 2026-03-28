package com.geek.ratelimit4j.local.circuit;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 熔断器实现
 * 防止分布式限流异常影响主业务，提供熔断保护机制
 *
 * <p>熔断状态：</p>
 * <ul>
 *   <li>CLOSED：正常状态，允许请求通过</li>
 *   <li>OPEN：熔断状态，拒绝所有请求</li>
 *   <li>HALF_OPEN：半开状态，允许部分请求通过进行探测</li>
 * </ul>
 *
 * <p>工作原理：</p>
 * <ol>
 *   <li>连续失败次数达到阈值时触发熔断</li>
 *   <li>熔断持续一段时间后进入半开状态</li>
 *   <li>半开状态下成功请求恢复熔断器</li>
 *   <li>半开状态下失败请求重新触发熔断</li>
 * </ol>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
public class CircuitBreaker {

    /**
     * 熔断器状态枚举
     */
    public enum State {
        /** 正常状态，允许请求通过 */
        CLOSED,
        /** 熔断状态，拒绝所有请求 */
        OPEN,
        /** 半开状态，允许部分请求通过进行探测 */
        HALF_OPEN
    }

    /**
     * 熔断器名称
     */
    private final String name;

    /**
     * 连续失败次数阈值
     */
    private final int failureThreshold;

    /**
     * 熔断恢复尝试间隔（毫秒）
     */
    private final long recoveryTimeoutMs;

    /**
     * 半开状态下允许通过的请求数
     */
    private final int halfOpenMaxCalls;

    /**
     * 当前状态
     */
    private final AtomicReference<State> state;

    /**
     * 连续失败次数
     */
    private final AtomicInteger failureCount;

    /**
     * 成功次数（用于半开状态判断）
     */
    private final AtomicInteger successCount;

    /**
     * 最后一次失败时间
     */
    private final AtomicLong lastFailureTime;

    /**
     * 半开状态下的调用次数
     */
    private final AtomicInteger halfOpenCalls;

    /**
     * 构造熔断器
     *
     * @param name 熔断器名称
     * @param failureThreshold 失败次数阈值
     * @param recoveryTimeoutMs 恢复尝试间隔（毫秒）
     */
    public CircuitBreaker(String name, int failureThreshold, long recoveryTimeoutMs) {
        this(name, failureThreshold, recoveryTimeoutMs, 3);
    }

    /**
     * 完整构造函数
     *
     * @param name 熔断器名称
     * @param failureThreshold 失败次数阈值
     * @param recoveryTimeoutMs 恢复尝试间隔（毫秒）
     * @param halfOpenMaxCalls 半开状态最大调用次数
     */
    public CircuitBreaker(String name, int failureThreshold, long recoveryTimeoutMs, int halfOpenMaxCalls) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.recoveryTimeoutMs = recoveryTimeoutMs;
        this.halfOpenMaxCalls = halfOpenMaxCalls;
        this.state = new AtomicReference<>(State.CLOSED);
        this.failureCount = new AtomicInteger(0);
        this.successCount = new AtomicInteger(0);
        this.lastFailureTime = new AtomicLong(0);
        this.halfOpenCalls = new AtomicInteger(0);
    }

    /**
     * 判断是否允许请求通过
     *
     * @return true表示允许，false表示拒绝
     */
    public boolean allowRequest() {
        State currentState = state.get();

        switch (currentState) {
            case CLOSED:
                return true;

            case OPEN:
                if (shouldAttemptReset()) {
                    if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                        halfOpenCalls.set(0);
                        successCount.set(0);
                        return true;
                    }
                }
                return false;

            case HALF_OPEN:
                int calls = halfOpenCalls.incrementAndGet();
                return calls <= halfOpenMaxCalls;

            default:
                return false;
        }
    }

    /**
     * 记录成功请求
     * 用于恢复熔断器状态
     */
    public void recordSuccess() {
        State currentState = state.get();

        if (Objects.equals(currentState, State.HALF_OPEN)) {
            int success = successCount.incrementAndGet();
            if (success >= halfOpenMaxCalls) {
                state.compareAndSet(State.HALF_OPEN, State.CLOSED);
                failureCount.set(0);
                halfOpenCalls.set(0);
            }
        } else if (Objects.equals(currentState, State.CLOSED)) {
            failureCount.set(0);
        }
    }

    /**
     * 记录失败请求
     * 用于触发熔断
     */
    public void recordFailure() {
        failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());

        State currentState = state.get();

        if (Objects.equals(currentState, State.HALF_OPEN)) {
            state.compareAndSet(State.HALF_OPEN, State.OPEN);
        } else if (Objects.equals(currentState, State.CLOSED)) {
            if (failureCount.get() >= failureThreshold) {
                state.compareAndSet(State.CLOSED, State.OPEN);
            }
        }
    }

    /**
     * 判断是否应该尝试重置熔断器
     *
     * @return true表示应该尝试重置
     */
    private boolean shouldAttemptReset() {
        long lastFailure = lastFailureTime.get();
        return System.currentTimeMillis() - lastFailure >= recoveryTimeoutMs;
    }

    /**
     * 获取当前状态
     *
     * @return 熔断器状态
     */
    public State getState() {
        return state.get();
    }

    /**
     * 获取当前失败次数
     *
     * @return 失败次数
     */
    public int getFailureCount() {
        return failureCount.get();
    }

    /**
     * 获取熔断器名称
     *
     * @return 名称
     */
    public String getName() {
        return name;
    }

    /**
     * 手动重置熔断器
     */
    public void reset() {
        state.set(State.CLOSED);
        failureCount.set(0);
        successCount.set(0);
        halfOpenCalls.set(0);
    }

    /**
     * 手动打开熔断器
     */
    public void open() {
        state.set(State.OPEN);
        lastFailureTime.set(System.currentTimeMillis());
    }

    /**
     * 判断熔断器是否打开
     *
     * @return true表示打开状态
     */
    public boolean isOpen() {
        return Objects.equals(state.get(), State.OPEN);
    }

    /**
     * 判断熔断器是否关闭
     *
     * @return true表示关闭状态
     */
    public boolean isClosed() {
        return Objects.equals(state.get(), State.CLOSED);
    }

    /**
     * 获取状态描述
     *
     * @return 状态描述字符串
     */
    public String getStatusDescription() {
        return String.format("CircuitBreaker[name=%s, state=%s, failures=%d/%d]",
                             name, state.get(), failureCount.get(), failureThreshold);
    }
}