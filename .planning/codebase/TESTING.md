# Testing Patterns

**Analysis Date:** 2026-03-31

## Framework

**Test Runner:**
- JUnit Jupiter (JUnit 5) - `org.junit.jupiter:junit-jupiter`
- Version: Managed by Spring Boot parent BOM (`3.5.5`)

**Assertion Library:**
- JUnit 5 assertions (`org.junit.jupiter.api.Assertions.*`)
- Also available: Mockito for mocking

**Mocking Framework:**
- Mockito Core - `org.mockito:mockito-core`
- Managed by Spring Boot parent BOM

**Build Configuration:**
- Maven Surefire Plugin: `maven-surefire-plugin` version `3.2.5`
- Run tests: `mvn test`
- Run with coverage: `mvn test jacoco:report`

## Structure

**Test File Organization:**
- Location: `src/test/java/` alongside main source
- Package: Mirrors main source package structure
- Example: `ratelimit4j-local/src/test/java/com/geek/ratelimit4j/local/algorithm/LocalTokenBucketAlgorithmTest.java`

**Naming Convention:**
- `{ClassName}Test.java` - Test class follows the class being tested

**Test Suite Structure:**
```java
class LocalTokenBucketAlgorithmTest {

    private LocalTokenBucketAlgorithm algorithm;
    private RateLimitConfig config;

    @BeforeEach
    void setUp() {
        // Setup test fixtures
    }
    
    // Test methods...
}
```

## Patterns

**Test Setup:**
```java
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
```

**Basic Test Pattern:**
```java
@Test
@DisplayName("初始状态应该能够获取令牌")
void testInitialAcquire() {
    RateLimitContext context = RateLimitContext.of("test-key", config, ModeType.LOCAL);

    RateLimitResult result = algorithm.evaluate(context);

    assertTrue(Objects.nonNull(result));
    assertTrue(result.isAllowed());
    assertEquals(AlgorithmType.TOKEN_BUCKET, result.getAlgorithmType());
}
```

**Assertion Patterns:**
- `assertTrue(condition)` - Boolean assertions
- `assertFalse(condition)` - Negative boolean
- `assertEquals(expected, actual)` - Value equality
- `assertThrows(exception.class, lambda)` - Exception testing
- `Objects.nonNull(object)` - Null checks with JUnit

**Display Names:**
```java
@Test
@DisplayName("描述测试场景的中文名称")
void testMethodName() {
    // Test implementation
}
```

**Exception Testing:**
```java
@Test
@DisplayName("构造函数不应接受null配置")
void testNullConfig() {
    assertThrows(IllegalArgumentException.class, () -> {
        new LocalTokenBucketAlgorithm(null);
    });
}
```

**Common Test Scenarios:**
- Initial state verification
- Resource exhaustion testing
- Token refill/regeneration
- Multiple permit acquisition
- Reset functionality
- Key isolation (different keys independent)
- Null parameter validation
- Edge cases

## Coverage

**Current State:**
- Limited test coverage - only one test file found: `LocalTokenBucketAlgorithmTest.java`
- No other modules have test files
- Coverage is NOT currently enforced

**Coverage Reporting:**
- Not configured in pom.xml
- No JaCoCo plugin configured
- No coverage target percentage

**Recommendations:**
- Add tests for Redis algorithms (`RedisTokenBucketAlgorithm`, `RedisFixedWindowAlgorithm`, etc.)
- Add tests for Spring Boot auto-configuration
- Add integration tests for the aspect processing
- Add tests for dimension resolvers
- Consider adding JaCoCo for coverage enforcement

## Common Patterns in Existing Tests

**Testing Algorithm Behavior:**
```java
// Exhaust tokens then verify rejection
for (int i = 0; i < 10; i++) {
    RateLimitResult result = algorithm.evaluate(context);
    assertTrue(result.isAllowed(), "第" + (i + 1) + "次请求应该成功");
}
RateLimitResult result = algorithm.evaluate(context);
assertFalse(result.isAllowed(), "令牌耗尽后应该被拒绝");
```

**Testing Key Isolation:**
```java
// Different keys should have independent limits
RateLimitContext context1 = RateLimitContext.of("key1", config, ModeType.LOCAL);
RateLimitContext context2 = RateLimitContext.of("key2", config, ModeType.LOCAL);

// Exhaust key1
for (int i = 0; i < 10; i++) {
    algorithm.evaluate(context1);
}

// key1 should be rejected, key2 should still work
assertFalse(algorithm.evaluate(context1).isAllowed());
assertTrue(algorithm.evaluate(context2).isAllowed());
```

**Async/Timing Tests:**
```java
@Test
@DisplayName("令牌应该按速率补充")
void testTokenRefill() throws InterruptedException {
    // Exhaust tokens
    for (int i = 0; i < 10; i++) {
        algorithm.evaluate(context);
    }
    
    // Wait for refill
    Thread.sleep(150);
    
    // Verify new tokens available
    RateLimitResult result = algorithm.evaluate(context);
    assertTrue(result.isAllowed(), "等待后应该有新令牌");
}
```

## Running Tests

**Commands:**
```bash
mvn test                  # Run all tests
mvn test -Dtest=ClassName # Run specific test class
mvn verify               # Run full verify lifecycle
```

---

*Testing analysis: 2026-03-31*