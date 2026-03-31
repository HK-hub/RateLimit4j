# Architecture

**Analysis Date:** 2026-03-31

## Pattern Overview

**Overall:** Plugin-based Rate Limiting Framework with Strategy Pattern

The project follows a **modular plugin architecture** with clear separation of concerns. It uses:
- **Strategy Pattern**: Multiple rate limit algorithms (Token Bucket, Leaky Bucket, Fixed Window, Sliding Window) 
- **Provider Pattern**: Storage providers (Local, Redis) and engine providers
- **Template Method Pattern**: AbstractRateLimitAspect for AOP-based rate limiting
- **Registry Pattern**: AlgorithmRegistry, DimensionResolverRegistry for component management

## Layers

### 1. Annotation Layer
**Location:** `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/annotation/`

**Purpose:** Declarative rate limiting configuration via Java annotations

**Key Components:**
- `@RateLimit` - Single rate limit rule annotation
- `@RateLimits` - Container for multiple rate limit rules
- `@IpRateLimit`, `@UserRateLimit`, `@TenantRateLimit`, `@DeviceRateLimit` - Pre-defined dimension annotations

### 2. Core Interface Layer
**Location:** `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/`

**Purpose:** Define core abstractions and contracts

**Key Interfaces:**
- `RateLimiter` - Core limiter interface with tryAcquire() methods
- `RateLimitAlgorithm` - Algorithm abstraction with evaluate() method
- `StorageProvider` - Storage abstraction for distributed rate limiting
- `DimensionResolver` - Dimension value extraction
- `RateLimitEngineProvider` - Engine provider abstraction

### 3. Configuration Layer
**Location:** `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/config/`

**Purpose:** Configuration models and enums

**Key Components:**
- `RateLimitConfig` - Algorithm configuration (rate, period, maxBurst, etc.)
- `RateLimitContext` - Runtime context for algorithm evaluation
- `AlgorithmType` - Enum for algorithm types (TOKEN_BUCKET, LEAKY_BUCKET, etc.)
- `EngineType` - Enum for engine types (LOCAL, REDIS, AUTO)
- `DimensionType` - Enum for dimension types (IP, USER, TENANT, DEVICE, METHOD)
- `ModeType` - Enum for mode (LOCAL, DISTRIBUTED)

### 4. Registry/Resolution Layer
**Location:** `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/registry/` and `resolver/`

**Purpose:** Component lifecycle management and resolution

**Key Components:**
- `AlgorithmRegistry` - Algorithm registration and lookup
- `DimensionResolverRegistry` - Dimension resolver management
- `KeyBuilder`, `KeyResolver` - Key extraction strategies

### 5. Engine Implementation Layer
**Location:** 
- `ratelimit4j-local/` - Local in-memory implementations
- `ratelimit4j-redis/` - Redis-based distributed implementations

**Purpose:** Concrete algorithm and storage implementations

**Key Components:**
- Local: `LocalTokenBucketAlgorithm`, `LocalLeakyBucketAlgorithm`, `LocalFixedWindowAlgorithm`, `LocalSlidingWindowCounterAlgorithm`, `LocalSlidingWindowLogAlgorithm`
- Redis: `RedisTokenBucketAlgorithm`, `RedisLeakyBucketAlgorithm`, `RedisFixedWindowAlgorithm`, `RedisSlidingWindowCounterAlgorithm`, `RedisSlidingWindowLogAlgorithm`
- Storage: `RedisStorageProvider`

### 6. Spring Boot Integration Layer
**Location:** `ratelimit4j-spring-boot-starter/`

**Purpose:** Spring Boot auto-configuration and AOP integration

**Key Components:**
- `RateLimitAutoConfiguration` - Main auto-configuration class
- `AbstractRateLimitAspect` - AOP aspect for intercepting annotated methods
- `DefaultRateLimitAspect` - Concrete aspect implementation
- Various `*KeyResolver` classes for key extraction
- Various `*DimensionResolver` classes for dimension resolution
- `RateLimitProperties` - Configuration properties binding

### 7. Telemetry/Monitoring Layer
**Location:** 
- `ratelimit4j-core/.../telemetry/` - Core telemetry interfaces
- `ratelimit4j-spring-boot-starter/.../telemetry/` - OpenTelemetry integration

**Purpose:** Metrics and monitoring

## Data Flow

### Request Flow

```
1. Client Request
      ↓
2. @RateLimit / @RateLimits Annotation (on method)
      ↓
3. AbstractRateLimitAspect.aroundSingle/aroundMultiple (AOP)
      ↓
4. Key Resolution
      - RateLimitKeyResolver.resolve(context)
      - Uses SpEL expression, KeyBuilder, or DimensionResolver
      ↓
5. Engine Resolution
      - EngineProviderRegistry.extractEngine(engineType)
      - Selects LOCAL or REDIS engine
      ↓
6. Algorithm Lookup
      - AlgorithmRegistry.getAlgorithm(algorithmType, engineType)
      - Retrieves corresponding algorithm implementation
      ↓
7. Algorithm Evaluation
      - RateLimitAlgorithm.evaluate(context)
      - Returns RateLimitResult (allowed/rejected, wait time)
      ↓
8. Telemetry Recording (if enabled)
      - RateLimitTelemetry.recordAllowed/rejected
      ↓
9. Result Handling
      - Allowed: proceed with method execution
      - Rejected: execute FallbackHandler or throw exception
```

### Key Resolution Priority

1. **keys attribute** - SpEL expressions (highest priority)
2. **keyBuilder** - Custom KeyBuilder implementation
3. **dimension** - Pre-defined DimensionType (IP, USER, TENANT, DEVICE, METHOD)
4. **Default** - Method fully qualified name

### Algorithm × Engine Matrix

| Algorithm | LOCAL Engine | REDIS Engine |
|-----------|--------------|--------------|
| TOKEN_BUCKET | LocalTokenBucketAlgorithm | RedisTokenBucketAlgorithm |
| LEAKY_BUCKET | LocalLeakyBucketAlgorithm | RedisLeakyBucketAlgorithm |
| FIXED_WINDOW | LocalFixedWindowAlgorithm | RedisFixedWindowAlgorithm |
| SLIDING_WINDOW_COUNTER | LocalSlidingWindowCounterAlgorithm | RedisSlidingWindowCounterAlgorithm |
| SLIDING_WINDOW_LOG | LocalSlidingWindowLogAlgorithm | RedisSlidingWindowLogAlgorithm |

## Key Abstractions

### RateLimiter (Core Interface)
**File:** `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/RateLimiter.java`

```java
public interface RateLimiter {
    boolean tryAcquire();
    boolean tryAcquire(int permits);
    RateLimitConfig getConfig();
    long getAvailablePermits();
    String getName();
    boolean isDistributed();
}
```

### RateLimitAlgorithm (Algorithm Interface)
**File:** `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/algorithm/RateLimitAlgorithm.java`

```java
public interface RateLimitAlgorithm {
    RateLimitResult evaluate(RateLimitContext context);
    RateLimitResult evaluate(RateLimitContext context, int permits);
    AlgorithmType getType();
    void reset(String key);
    String getStatusDescription(String key);
}
```

### StorageProvider (Storage Interface)
**File:** `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/storage/StorageProvider.java`

```java
public interface StorageProvider {
    StorageType getStorageType();
    <T> T executeAtomic(String script, List<String> keys, List<Object> args);
    void store(String key, long value, long ttlSeconds);
    Long get(String key);
    boolean delete(String key);
    // ... more methods
}
```

### RateLimitEngineProvider (Engine Interface)
**File:** `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/engine/RateLimitEngineProvider.java`

```java
public interface RateLimitEngineProvider {
    EngineType getEngineType();
    default int getOrder() { return 100; }
    default boolean isAvailable() { return true; }
}
```

### DimensionResolver (Dimension Interface)
**File:** `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/resolver/DimensionResolver.java`

```java
public interface DimensionResolver {
    DimensionType getType();
    String resolve(DimensionResolveContext context);
}
```

### AlgorithmRegistry (Registry Interface)
**File:** `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/registry/AlgorithmRegistry.java`

```java
public interface AlgorithmRegistry {
    void register(RateLimitAlgorithm algorithm);
    RateLimitAlgorithm unregister(AlgorithmType algorithmType, EngineType engineType);
    RateLimitAlgorithm getAlgorithm(AlgorithmType algorithmType, EngineType engineType);
    Map<AlgorithmType, RateLimitAlgorithm> getAlgorithms(EngineType engineType);
    // ... more methods
}
```

## Entry Points

### Annotation Entry Points

**Primary:**
- `@RateLimit` - `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/annotation/RateLimit.java`
  - Target: METHOD, TYPE
  - Retention: RUNTIME
  - Key attributes: keys, keyBuilder, dimension, algorithm, engine, rate, period, maxBurst

**Secondary:**
- `@RateLimits` - Container annotation for multiple @RateLimit rules
- `@IpRateLimit`, `@UserRateLimit`, `@TenantRateLimit`, `@DeviceRateLimit` - Convenience annotations

### Configuration Entry Points

**Properties:**
- `RateLimitProperties` - `ratelimit4j-spring-boot-starter/src/main/java/com/geek/ratelimit4j/starter/autoconfigure/RateLimitProperties.java`
- YAML prefix: `ratelimit4j`

**Auto-Configuration:**
- `RateLimitAutoConfiguration` - `ratelimit4j-spring-boot-starter/src/main/java/com/geek/ratelimit4j/starter/autoconfigure/RateLimitAutoConfiguration.java`
- Auto-configured when: `ratelimit4j.enabled=true` (default)

### AOP Entry Point

- `AbstractRateLimitAspect` - Intercepts methods annotated with @RateLimit/@RateLimits
- Pointcuts:
  - `@annotation(rateLimit)` - Single annotation
  - `@annotation(rateLimits)` - Multiple annotations

## Error Handling

**Strategy:** Custom exception hierarchy with fallback handler support

**Exception Types:**
- `RateLimitException` - Base exception
- `RateLimitFallbackException` - For fallback scenarios
- `NoSuchRateLimitEngineException` - When engine not found

**Fallback Mechanism:**
1. If `@RateLimit(fallbackHandler=...)` is specified
2. Framework calls the FallbackHandler from Spring context
3. Handler returns alternative result or throws custom exception

---

*Architecture analysis: 2026-03-31*