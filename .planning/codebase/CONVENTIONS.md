# Coding Conventions

**Analysis Date:** 2026-03-31

## Naming Patterns

**Files:**
- Classes and Interfaces: PascalCase (e.g., `RateLimiter.java`, `RateLimitAlgorithm.java`, `FallbackHandler.java`)
- Enums: PascalCase with UPPER_SNAKE_CASE values (e.g., `AlgorithmType.java` with `TOKEN_BUCKET`, `LEAKY_BUCKET`)
- Annotations: PascalCase (e.g., `RateLimit.java`, `RateLimits.java`)
- Test Classes: `{ClassName}Test.java` (e.g., `LocalTokenBucketAlgorithmTest.java`)

**Packages:**
- All lowercase with dots: `com.geek.ratelimit4j.core.algorithm`
- Module-scoped: `core`, `local`, `redis`, `starter`

**Functions:**
- camelCase: `tryAcquire()`, `getAvailablePermits()`, `buildKey()`, `evaluate()`

**Variables:**
- camelCase: `config`, `algorithm`, `buckets`, `tokensPerMs`
- Descriptive names: `rateLimitConfig`, `applicationContext`

**Types:**
- Enums use code strings: `AlgorithmType.TOKEN_BUCKET.getCode()` returns `"token_bucket"`

## Code Style

**Formatting:**
- Uses Lombok extensively to reduce boilerplate:
  - `@Data` for POJOs
  - `@Builder` for builder pattern
  - `@Getter` / `@Setter` for accessors
  - `@AllArgsConstructor`, `@NoArgsConstructor` for constructors

**Libraries Used:**
- Lombok for code generation
- SLF4J with `@Slf4j` for logging
- AspectJ for AOP (e.g., `@Aspect`, `@Around`, `@Before`)
- Spring Framework for dependency injection
- Apache Commons Lang3 (`StringUtils`, `BooleanUtils`)
- Redisson for Redis operations

**Import Organization:**
1. Java standard library
2. External dependencies (Spring, AspectJ, etc.)
3. Internal modules (com.geek.ratelimit4j.*)

## Documentation

**Javadoc Style:**
- Chinese language comments for main documentation
- English for technical terms
- Includes `@author`, `@since 1.0.0` tags
- Uses `<pre>{@code ...}</pre>` for code examples
- Detailed method documentation with `@param`, `@return`, `@throws`

**Example Pattern:**
```java
/**
 * 限流器核心接口
 * 所有限流算法实现必须遵循此接口契约
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * RateLimiter limiter = registry.getRateLimiter("api-user");
 * if (limiter.tryAcquire()) {
 *     // 执行业务逻辑
 * }
 * }</pre>
 *
 * @author RateLimit4j
 * @since 1.0.0
 */
```

## Error Handling

**Exception Strategy:**
- Custom exceptions extend `RuntimeException`
- `RateLimitException` - thrown when rate limit is exceeded
- `RateLimitFallbackException` - for fallback-related errors
- `NoSuchRateLimitEngineException` - for missing engine errors

**Validation Patterns:**
```java
// Constructor parameter validation
public LocalTokenBucketAlgorithm(RateLimitConfig config) {
    if (Objects.isNull(config)) {
        throw new IllegalArgumentException("RateLimitConfig cannot be null");
    }
    // ...
}

// Method parameter validation
if (Objects.isNull(context)) {
    throw new IllegalArgumentException("RateLimitContext cannot be null");
}
if (permits <= 0) {
    throw new IllegalArgumentException("Permits must be positive");
}
```

**Null Safety:**
- Uses `Objects.requireNonNull()` in constructors
- Uses `Objects.isNull()` and `Objects.nonNull()` for checks
- Optional fallback handling via `FallbackHandler` interface

## Design Patterns

**Builder Pattern:**
```java
RateLimitConfig config = RateLimitConfig.builder()
    .name("test-token-bucket")
    .algorithmType(AlgorithmType.TOKEN_BUCKET)
    .rate(10)
    .period(1)
    .maxBurst(10)
    .build();
```

**Template Method Pattern:**
- `AbstractRateLimitAspect` defines the processing skeleton
- Subclasses override specific steps via protected methods

**Strategy Pattern:**
- `AlgorithmRegistry` manages multiple algorithm implementations
- `RateLimitAlgorithm` interface allows algorithm swapping

**Registry Pattern:**
- `AlgorithmRegistry` for algorithm lookup by type + engine
- `EngineProviderRegistry` for engine provider management
- `DimensionResolverRegistry` for dimension resolvers

## Annotations

**Custom Annotations:**
- `@RateLimit` - Main rate limiting annotation for methods/classes
- `@RateLimits` - Container for multiple `@RateLimit` annotations (repeatable)
- Dimension-specific: `@IpRateLimit`, `@UserRateLimit`, `@TenantRateLimit`, `@DeviceRateLimit`

**Common Framework Annotations:**
- `@Component`, `@Service` - Spring Bean registration
- `@Aspect` - AspectJ aspect
- `@Around`, `@Before`, `@After` - AOP advice
- `@Target`, `@Retention` - Annotation metadata

---

*Convention analysis: 2026-03-31*