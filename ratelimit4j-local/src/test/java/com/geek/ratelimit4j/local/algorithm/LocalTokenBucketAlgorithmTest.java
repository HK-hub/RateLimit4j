package com.geek.ratelimit4j.local.algorithm;

import com.geek.ratelimit4j.core.algorithm.AlgorithmType;
import com.geek.ratelimit4j.core.config.RateLimitContext;
import com.geek.ratelimit4j.core.algorithm.RateLimitResult;
import com.geek.ratelimit4j.core.config.ModeType;
import com.geek.ratelimit4j.core.config.RateLimitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 本地令牌桶算法测试
 * 验证令牌桶算法的核心功能
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
class LocalTokenBucketAlgorithmTest {

    private LocalTokenBucketAlgorithm algorithm;
    private RateLimitConfig config;

    @BeforeEach
    void setUp() {
        config = RateLimitConfig.builder()
                .name("test-token-bucket")
                .algorithmType(AlgorithmType.TOKEN_BUCKET)
                .rate(10)
                .period(1)
                .maxBurst(10)
                .build();
        algorithm = new LocalTokenBucketAlgorithm(config);
    }

    @Test
    @DisplayName("初始状态应该能够获取令牌")
    void testInitialAcquire() {
        RateLimitContext context = RateLimitContext.of("test-key", config, ModeType.LOCAL);

        RateLimitResult result = algorithm.evaluate(context);

        assertTrue(Objects.nonNull(result));
        assertTrue(result.isAllowed());
        assertEquals(AlgorithmType.TOKEN_BUCKET, result.getAlgorithmType());
    }

    @Test
    @DisplayName("消耗所有令牌后应该被拒绝")
    void testExhaustTokens() {
        RateLimitContext context = RateLimitContext.of("test-key", config, ModeType.LOCAL);

        for (int i = 0; i < 10; i++) {
            RateLimitResult result = algorithm.evaluate(context);
            assertTrue(result.isAllowed(), "第" + (i + 1) + "次请求应该成功");
        }

        RateLimitResult result = algorithm.evaluate(context);
        assertFalse(result.isAllowed(), "令牌耗尽后应该被拒绝");
    }

    @Test
    @DisplayName("令牌应该按速率补充")
    void testTokenRefill() throws InterruptedException {
        RateLimitContext context = RateLimitContext.of("test-key", config, ModeType.LOCAL);

        for (int i = 0; i < 10; i++) {
            algorithm.evaluate(context);
        }

        Thread.sleep(150);

        RateLimitResult result = algorithm.evaluate(context);
        assertTrue(result.isAllowed(), "等待后应该有新令牌");
    }

    @Test
    @DisplayName("多许可获取应该正确判断")
    void testMultiplePermitsAcquire() {
        RateLimitContext context = RateLimitContext.of("test-key", config, ModeType.LOCAL);

        RateLimitResult result = algorithm.evaluate(context, 5);
        assertTrue(result.isAllowed());

        result = algorithm.evaluate(context, 6);
        assertFalse(result.isAllowed());
    }

    @Test
    @DisplayName("算法类型应该正确返回")
    void testAlgorithmType() {
        assertEquals(AlgorithmType.TOKEN_BUCKET, algorithm.getType());
    }

    @Test
    @DisplayName("重置应该清空令牌状态")
    void testReset() {
        RateLimitContext context = RateLimitContext.of("test-key", config, ModeType.LOCAL);

        for (int i = 0; i < 10; i++) {
            algorithm.evaluate(context);
        }

        algorithm.reset("test-key");

        RateLimitResult result = algorithm.evaluate(context);
        assertTrue(result.isAllowed());
    }

    @Test
    @DisplayName("不同Key应该独立计算")
    void testDifferentKeys() {
        RateLimitContext context1 = RateLimitContext.of("key1", config, ModeType.LOCAL);
        RateLimitContext context2 = RateLimitContext.of("key2", config, ModeType.LOCAL);

        for (int i = 0; i < 10; i++) {
            algorithm.evaluate(context1);
        }

        assertFalse(algorithm.evaluate(context1).isAllowed());
        assertTrue(algorithm.evaluate(context2).isAllowed());
    }

    @Test
    @DisplayName("构造函数不应接受null配置")
    void testNullConfig() {
        assertThrows(IllegalArgumentException.class, () -> {
            new LocalTokenBucketAlgorithm(null);
        });
    }
}