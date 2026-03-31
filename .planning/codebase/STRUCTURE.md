# Structure

**Analysis Date:** 2026-03-31

## Directory Layout

```
RateLimit4j/
├── pom.xml                           # Parent POM (modules definition)
├── ratelimit4j-core/                 # Core module (interfaces, annotations, configs)
│   ├── pom.xml
│   └── src/main/java/com/geek/ratelimit4j/core/
│       ├── RateLimiter.java
│       ├── algorithm/                # Algorithm interfaces and enums
│       │   ├── RateLimitAlgorithm.java
│       │   ├── RateLimitResult.java
│       │   ├── AlgorithmType.java
│       │   └── EngineAware.java
│       ├── annotation/                # Rate limiting annotations
│       │   ├── RateLimit.java
│       │   ├── RateLimits.java
│       │   ├── IpRateLimit.java
│       │   ├── UserRateLimit.java
│       │   ├── TenantRateLimit.java
│       │   └── DeviceRateLimit.java
│       ├── config/                    # Configuration classes and enums
│       │   ├── RateLimitConfig.java
│       │   ├── RateLimitContext.java
│       │   ├── AlgorithmType.java
│       │   ├── DimensionType.java
│       │   ├── EngineType.java
│       │   ├── ModeType.java
│       │   └── StorageType.java
│       ├── engine/                    # Engine provider interfaces
│       │   ├── RateLimitEngineProvider.java
│       │   ├── EngineProviderRegistry.java
│       │   ├── DefaultEngineProviderRegistry.java
│       │   └── AbstractEngineProviderRegistry.java
│       ├── exception/                 # Exception classes
│       │   ├── RateLimitException.java
│       │   ├── RateLimitFallbackException.java
│       │   └── NoSuchRateLimitEngineException.java
│       ├── handler/                   # Fallback handler interface
│       │   └── FallbackHandler.java
│       ├── registry/                   # Algorithm registry
│       │   ├── AlgorithmRegistry.java
│       │   └── AbstractAlgorithmRegistry.java
│       ├── resolver/                  # Key and dimension resolvers
│       │   ├── DimensionResolver.java
│       │   ├── DimensionResolveContext.java
│       │   ├── DimensionResolverRegistry.java
│       │   ├── KeyBuilder.java
│       │   ├── KeyResolver.java
│       │   └── RateLimitResolveContext.java
│       ├── storage/                   # Storage provider interface
│       │   ├── StorageProvider.java
│       │   └── StorageType.java
│       └── telemetry/                  # Telemetry interfaces
│           ├── RateLimitTelemetry.java
│           ├── TelemetryConfig.java
│           └── TelemetryEvent.java
│
├── ratelimit4j-local/                 # Local in-memory implementation
│   ├── pom.xml
│   └── src/
│       ├── main/java/.../local/
│       │   ├── algorithm/
│       │   │   ├── LocalTokenBucketAlgorithm.java
│       │   │   ├── LocalLeakyBucketAlgorithm.java
│       │   │   ├── LocalFixedWindowAlgorithm.java
│       │   │   ├── LocalSlidingWindowCounterAlgorithm.java
│       │   │   └── LocalSlidingWindowLogAlgorithm.java
│       │   ├── circuit/
│       │   │   └── CircuitBreaker.java
│       │   └── engine/
│       │       └── LocalEngineProvider.java
│       └── test/java/.../local/
│           └── algorithm/
│               └── LocalTokenBucketAlgorithmTest.java
│
├── ratelimit4j-redis/                 # Redis distributed implementation
│   ├── pom.xml
│   └── src/main/java/.../redis/
│       ├── algorithm/
│       │   ├── RedisTokenBucketAlgorithm.java
│       │   ├── RedisLeakyBucketAlgorithm.java
│       │   ├── RedisFixedWindowAlgorithm.java
│       │   ├── RedisSlidingWindowCounterAlgorithm.java
│       │   └── RedisSlidingWindowLogAlgorithm.java
│       ├── storage/
│       │   └── RedisStorageProvider.java
│       └── engine/
│           └── RedisEngineProvider.java
│
└── ratelimit4j-spring-boot-starter/   # Spring Boot starter
    ├── pom.xml
    └── src/main/java/.../starter/
        ├── aspect/
        │   ├── AbstractRateLimitAspect.java
        │   ├── DefaultRateLimitAspect.java
        │   └── RateLimitAspectSupport.java
        ├── autoconfigure/
        │   ├── RateLimitAutoConfiguration.java
        │   └── RateLimitProperties.java
        ├── registry/
        │   └── DefaultAlgorithmRegistry.java
        ├── resolver/
        │   ├── RateLimitKeyResolver.java
        │   ├── DefaultKeyResolver.java
        │   ├── SpelRateLimitKeyResolver.java
        │   ├── BuilderRateLimitKeyResolver.java
        │   ├── CompositeRateLimitKeyResolver.java
        │   ├── DimensionRateLimitKeyResolver.java
        │   ├── MethodRateLimitKeyResolver.java
        │   ├── DefaultDimensionResolveContext.java
        │   ├── DefaultRateLimitResolveContext.java
        │   ├── SpelKeyResolver.java
        │   ├── IpDimensionResolver.java
        │   ├── UserDimensionResolver.java
        │   ├── TenantDimensionResolver.java
        │   ├── DeviceDimensionResolver.java
        │   └── MethodDimensionResolver.java
        └── telemetry/
            ├── TelemetryAutoConfiguration.java
            └── OpenTelemetryRateLimitTelemetry.java
```

## Key Locations

### Entry Points

| Purpose | File Path |
|---------|-----------|
| Main Annotation | `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/annotation/RateLimit.java` |
| Container Annotation | `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/annotation/RateLimits.java` |
| Auto-Configuration | `ratelimit4j-spring-boot-starter/src/main/java/com/geek/ratelimit4j/starter/autoconfigure/RateLimitAutoConfiguration.java` |
| Properties Binding | `ratelimit4j-spring-boot-starter/src/main/java/com/geek/ratelimit4j/starter/autoconfigure/RateLimitProperties.java` |
| AOP Aspect | `ratelimit4j-spring-boot-starter/src/main/java/com/geek/ratelimit4j/starter/aspect/AbstractRateLimitAspect.java` |

### Core Interfaces

| Purpose | File Path |
|---------|-----------|
| Core Limiter | `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/RateLimiter.java` |
| Algorithm Interface | `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/algorithm/RateLimitAlgorithm.java` |
| Storage Provider | `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/storage/StorageProvider.java` |
| Engine Provider | `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/engine/RateLimitEngineProvider.java` |
| Algorithm Registry | `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/registry/AlgorithmRegistry.java` |
| Dimension Resolver | `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/resolver/DimensionResolver.java` |

### Algorithm Implementations

| Algorithm | Local Implementation | Redis Implementation |
|-----------|---------------------|---------------------|
| Token Bucket | `ratelimit4j-local/src/main/java/.../LocalTokenBucketAlgorithm.java` | `ratelimit4j-redis/src/main/java/.../RedisTokenBucketAlgorithm.java` |
| Leaky Bucket | `ratelimit4j-local/src/main/java/.../LocalLeakyBucketAlgorithm.java` | `ratelimit4j-redis/src/main/java/.../RedisLeakyBucketAlgorithm.java` |
| Fixed Window | `ratelimit4j-local/src/main/java/.../LocalFixedWindowAlgorithm.java` | `ratelimit4j-redis/src/main/java/.../RedisFixedWindowAlgorithm.java` |
| Sliding Window Counter | `ratelimit4j-local/src/main/java/.../LocalSlidingWindowCounterAlgorithm.java` | `ratelimit4j-redis/src/main/java/.../RedisSlidingWindowCounterAlgorithm.java` |
| Sliding Window Log | `ratelimit4j-local/src/main/java/.../LocalSlidingWindowLogAlgorithm.java` | `ratelimit4j-redis/src/main/java/.../RedisSlidingWindowLogAlgorithm.java` |

### Configuration Enums

| Enum | File Path |
|------|-----------|
| AlgorithmType | `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/algorithm/AlgorithmType.java` |
| EngineType | `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/config/EngineType.java` |
| DimensionType | `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/config/DimensionType.java` |
| ModeType | `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/config/ModeType.java` |
| StorageType | `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/storage/StorageType.java` |

## Naming Conventions

### Files

| Type | Convention | Example |
|------|------------|---------|
| Interfaces | PascalCase, ends with Interface suffix (optional) | `RateLimiter`, `RateLimitAlgorithm` |
| Annotations | PascalCase, ends with Limit or Limit(s) | `@RateLimit`, `@RateLimits` |
| Abstract Classes | PascalCase, starts with Abstract | `AbstractRateLimitAspect` |
| Default Implementations | PascalCase, starts with Default | `DefaultRateLimitAspect`, `DefaultAlgorithmRegistry` |
| Algorithm Classes | PascalCase, ends with Algorithm | `LocalTokenBucketAlgorithm`, `RedisTokenBucketAlgorithm` |
| Provider Classes | PascalCase, ends with Provider | `LocalEngineProvider`, `RedisEngineProvider` |
| Resolver Classes | PascalCase, ends with Resolver | `IpDimensionResolver`, `SpelRateLimitKeyResolver` |
| Properties Classes | PascalCase, ends with Properties | `RateLimitProperties` |
| Config Classes | PascalCase, ends with Config | `RateLimitConfig`, `TelemetryConfig` |

### Directories

| Directory | Purpose |
|-----------|---------|
| `algorithm/` | Algorithm interfaces and implementations |
| `annotation/` | Annotation definitions |
| `aspect/` | AOP aspect classes |
| `autoconfigure/` | Spring Boot auto-configuration |
| `config/` | Configuration classes and enums |
| `circuit/` | Circuit breaker implementation |
| `engine/` | Engine provider interfaces and implementations |
| `exception/` | Custom exception classes |
| `handler/` | Handler interfaces (e.g., FallbackHandler) |
| `registry/` | Registry implementations |
| `resolver/` | Resolver interfaces and implementations |
| `storage/` | Storage provider interfaces and implementations |
| `telemetry/` | Telemetry/monitoring components |

## Module Organization

### Multi-Module Structure

| Module | Purpose | Dependencies |
|--------|---------|--------------|
| `ratelimit4j-core` | Core interfaces, annotations, configs | None (pure Java) |
| `ratelimit4j-local` | Local in-memory algorithms | ratelimit4j-core |
| `ratelimit4j-redis` | Redis-based distributed algorithms | ratelimit4j-core, Redisson |
| `ratelimit4j-spring-boot-starter` | Spring Boot integration | All modules, Spring Boot |

### Adding New Components

**New Algorithm (Local):**
1. Create: `ratelimit4j-local/src/main/java/.../algorithm/Local[AlgorithmName]Algorithm.java`
2. Implement: `RateLimitAlgorithm` interface
3. Register: Add @Bean in `RateLimitAutoConfiguration`

**New Algorithm (Redis):**
1. Create: `ratelimit4j-redis/src/main/java/.../algorithm/Redis[AlgorithmName]Algorithm.java`
2. Implement: `RateLimitAlgorithm` interface with Lua scripts
3. Register: Add @Bean in `RateLimitAutoConfiguration`

**New Dimension Resolver:**
1. Create: `ratelimit4j-spring-boot-starter/.../resolver/[Dimension]DimensionResolver.java`
2. Implement: `DimensionResolver` interface
3. Register: Add @Bean in `RateLimitAutoConfiguration` or use `@Component`

**New Key Resolver:**
1. Create: `ratelimit4j-spring-boot-starter/.../resolver/[Name]RateLimitKeyResolver.java`
2. Implement: `RateLimitKeyResolver` interface
3. Register: Add @Bean in `RateLimitAutoConfiguration`

**New Engine Provider:**
1. Create: `ratelimit4j-[name]/src/main/java/.../engine/[Name]EngineProvider.java`
2. Implement: `RateLimitEngineProvider` interface
3. Register: Add @Bean in `RateLimitAutoConfiguration`

---

*Structure analysis: 2026-03-31*