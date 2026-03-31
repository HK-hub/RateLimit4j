# External Integrations

**Analysis Date:** 2026-03-31

## Data Storage

**Redis (Distributed Rate Limiting):**
- Provider: Redis via Redisson client
- Module: `ratelimit4j-redis`
- Purpose: Distributed rate limiting across multiple application instances
- Configuration: Spring Boot auto-configuration via Redisson starter
- Connection: Configured via Spring Boot Redis properties
- Key implementation: `ratelimit4j-redis/src/main/java/com/geek/ratelimit4j/redis/storage/RedisStorageProvider.java`

**In-Memory Storage (Local Rate Limiting):**
- Provider: Java in-memory data structures
- Module: `ratelimit4j-local`
- Purpose: Single-instance rate limiting with no external dependencies
- Implementation: Uses ConcurrentHashMap and atomic counters

## Authentication & Identity

**Not applicable:**
- RateLimit4j is a library that provides rate limiting functionality
- Authentication is delegated to the consuming application
- No built-in authentication providers

## Observability & Telemetry

**OpenTelemetry Integration:**
- Package: `io.opentelemetry` (API and SDK)
- Version: 1.47.0
- Purpose: Metrics and tracing for rate limit operations
- Implementation: `ratelimit4j-spring-boot-starter/src/main/java/com/geek/ratelimit4j/starter/telemetry/OpenTelemetryRateLimitTelemetry.java`
- Configuration: `ratelimit4j-core/src/main/java/com/geek/ratelimit4j/core/telemetry/TelemetryConfig.java`
- Optional: Can be excluded if not needed

**Telemetry Events Tracked:**
- Rate limit hits and passes
- Rate limit rejections
- Algorithm execution times

## Web & HTTP Integration

**Spring Web Integration:**
- Framework: Spring MVC / WebFlux (via AOP)
- Purpose: Intercept HTTP requests for rate limiting
- Implementation: Spring AOP Aspect
- Key files:
  - `ratelimit4j-spring-boot-starter/src/main/java/com/geek/ratelimit4j/starter/aspect/AbstractRateLimitAspect.java`
  - `ratelimit4j-spring-boot-starter/src/main/java/com/geek/ratelimit4j/starter/aspect/DefaultRateLimitAspect.java`

**SPEL (Spring Expression Language) Support:**
- Purpose: Dynamic rate limit keys using Spring expressions
- Implementation: `ratelimit4j-spring-boot-starter/src/main/java/com/geek/ratelimit4j/starter/resolver/SpelRateLimitKeyResolver.java`

## Dimension Resolvers (Built-in)

The library provides built-in dimension resolvers for rate limiting:

| Resolver | Purpose | File |
|----------|---------|------|
| IP Address | Rate limit by client IP | `IpDimensionResolver.java` |
| User | Rate limit by authenticated user | `UserDimensionResolver.java` |
| Tenant | Rate limit by tenant ID | `TenantDimensionResolver.java` |
| Device | Rate limit by device identifier | `DeviceDimensionResolver.java` |
| Method | Rate limit by HTTP method | `MethodDimensionResolver.java` |

## Build & Repository

**Maven Repository:**
- Primary: Aliyun Maven Repository (https://maven.aliyun.com/repository/public)
- Dependencies resolved from Maven Central transitively

## Configuration Properties

**Rate Limit Configuration (Spring Boot):**
- Properties class: `RateLimitProperties.java`
- Located in: `ratelimit4j-spring-boot-starter/src/main/java/com/geek/ratelimit4j/starter/autoconfigure/`

**Typical configuration properties:**
- Engine type (local/redis)
- Algorithm selection
- Default limits (requests per time window)
- Dimension resolver preferences

## No External Integrations For:

- **Database**: Not used; rate limit state stored in Redis or memory
- **Message Queues**: Not used; synchronous rate limiting
- **API Gateways**: Library can be integrated into API gateways but no built-in support
- **CDN**: Not applicable
- **Email/Notifications**: Not used
- **Third-party Auth**: Not built-in; integrates with whatever auth the consuming app uses

---

*Integration audit: 2026-03-31*