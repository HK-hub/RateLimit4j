<p align="center">
  <img src="docs/images/logo.png" alt="RateLimit4j Logo" width="200">
</p>

<h1 align="center">RateLimit4j</h1>

<p align="center">
  <strong>🚀 A powerful, extensible rate limiting framework for Java applications</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#quick-start">Quick Start</a> •
  <a href="#usage">Usage</a> •
  <a href="#configuration">Configuration</a> •
  <a href="#algorithms">Algorithms</a> •
  <a href="README_CN.md">中文文档</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17+-green?logo=java" alt="Java 17+">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?logo=spring" alt="Spring Boot 3.x">
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue" alt="License">
  <img src="https://img.shields.io/badge/Version-1.0.0-orange" alt="Version">
  <img src="https://img.shields.io/badge/Test%20Coverage-5%25-red" alt="Test Coverage">
</p>

---

## ✨ Features

### 🎯 Core Features

- **5 Rate Limiting Algorithms** - Token Bucket, Leaky Bucket, Fixed Window, Sliding Window Log, Sliding Window Counter
- **Dual Engine Support** - Local (JVM memory) and Redis (distributed) engines, **Redis is default**
- **Spring Boot Integration** - Auto-configuration with `@RateLimit` annotation
- **Multiple Dimensions** - Support IP, User, Tenant, Device, and custom dimensions
- **Flexible Key Resolution** - SpEL expressions, custom KeyBuilder, predefined dimensions
- **Fallback Handling** - Customizable fallback handlers for rejected requests
- **OpenTelemetry Integration** - Built-in metrics and tracing support
- **High Performance** - Optimized for high-concurrency scenarios

### 🔧 Advanced Features

| Feature | Description |
|---------|-------------|
| **Multi-rule Limiting** | Multiple `@RateLimit` annotations on a single method |
| **Engine Selection** | Choose between LOCAL, REDIS, or AUTO per annotation |
| **Primary Engine** | Configure default engine in `application.yml` (default: Redis) |
| **Circuit Breaker** | Built-in circuit breaker for Redis failures |
| **Custom Exceptions** | Define your own exception types |
| **Key Prefix** | Namespace isolation with key prefixes |

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Spring Boot 3.x
- Maven 3.6+ or Gradle 7+

### Installation

Add dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.geek.ratelimit4j</groupId>
    <artifactId>ratelimit4j-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

Or `build.gradle`:

```groovy
implementation 'com.geek.ratelimit4j:ratelimit4j-spring-boot-starter:1.0.0'
```

## 📝 Usage Examples

### 1. Basic Usage

```java
@RestController
public class ApiController {

    // Simple rate limit: 100 requests per second (using Redis by default)
    @RateLimit(rate = 100)
    public String basicApi() {
        return "Hello World";
    }
}
```

### 2. Multi-rule Limiting

```java
// Multiple rules: 3 seconds 1 time + 1 minute 10 times
@RateLimit(rate = 1, period = 3)
@RateLimit(rate = 10, period = 60)
public String multiRuleApi() {
    return "Multi-rule limited";
}
```

### 3. Dimension-based Limiting

```java
// IP-based limiting
@IpRateLimit(rate = 10, period = 1)
public String ipApi(HttpServletRequest request) {
    return "IP limited";
}

// User-based limiting
@UserRateLimit(rate = 100, period = 60)
public String userApi(@CurrentUser User user) {
    return "User limited";
}

// Tenant-based limiting (multi-tenant scenarios)
@TenantRateLimit(rate = 1000, period = 1)
public String tenantApi() {
    return "Tenant limited";
}

// Device-based limiting (mobile/IoT scenarios)
@DeviceRateLimit(rate = 10, period = 1)
public String deviceApi() {
    return "Device limited";
}
```

### 4. Custom Key with SpEL

```java
// Extract keys from method parameters using SpEL
@RateLimit(keys = {"#user.id", "#request.remoteAddr"}, rate = 50)
public String customKeyApi(User user, HttpServletRequest request) {
    return "Custom key limited";
}
```

### 5. Custom KeyBuilder

```java
@Component
public class UserIdKeyBuilder implements KeyBuilder {
    @Override
    public String build(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        // Custom logic to extract user ID
        return "user:" + extractUserId(args);
    }
}

// Use custom KeyBuilder
@RateLimit(keyBuilder = UserIdKeyBuilder.class, rate = 100)
public String customBuilderApi(User user) {
    return "Custom builder limited";
}
```

### 6. Local Engine (Single Instance)

```java
// Use local engine for single-instance scenarios
@RateLimit(rate = 100, engine = EngineType.LOCAL)
public String localApi() {
    return "Local limited";
}
```

### 7. Fallback Handler

```java
@Component
public class CustomFallbackHandler implements FallbackHandler {
    @Override
    public Object handle(ProceedingJoinPoint joinPoint, RateLimitException e) {
        // Custom fallback logic
        return ResponseEntity
            .status(429)
            .body("Too many requests, please try again later");
    }
}

// Use custom fallback handler
@RateLimit(rate = 10, fallbackHandler = CustomFallbackHandler.class)
public String fallbackApi() {
    return "With fallback";
}
```

## ⚙️ Configuration

### application.yml

```yaml
ratelimit4j:
  # Enable rate limiting (default: true)
  enabled: true
  
  # Primary engine: redis or local (default: redis)
  primary-engine: redis
  
  # Default rate limiting rule
  default-rule:
    algorithm: token_bucket
    rate: 100
    period: 1
    key-prefix: ""
    max-burst: 100
  
  # Redis configuration
  redis:
    enabled: true
    host: localhost
    port: 6379
    password: ""
    database: 0
    timeout: 2000
  
  # Fallback configuration
  fallback:
    enabled: true
    degrade-to-local: true
    failure-threshold: 5
    recovery-timeout: 30000
  
  # OpenTelemetry configuration
  telemetry:
    enabled: true
    service-name: RateLimit4j
    endpoint: http://localhost:4317
```

## 📊 Algorithms

### Algorithm Comparison

| Algorithm | Precision | Memory | Boundary Issue | Use Case |
|-----------|-----------|--------|----------------|----------|
| **Token Bucket** | High | Low | No | Burst traffic handling |
| **Leaky Bucket** | High | Low | No | Smooth traffic output |
| **Fixed Window** | Low | Lowest | Yes | Simple scenarios |
| **Sliding Window Log** | Highest | High | No | Precise limiting |
| **Sliding Window Counter** | Medium | Low | Minimal | Balanced solution |

### Algorithm Details

#### 1. Token Bucket

```
┌─────────────────────────────────────────┐
│              Token Bucket               │
│  ┌───────────────────────────────────┐  │
│  │  Tokens: [●][●][●][●][●]         │  │
│  │  Rate: 100 tokens/period         │  │
│  │  Max Burst: 100                  │  │
│  └───────────────────────────────────┘  │
│                                         │
│  Request → Consume Token → Allowed      │
│  Request → No Token → Rejected          │
└─────────────────────────────────────────┘
```

- Tokens are added at a fixed rate
- Requests consume tokens
- Allows burst traffic within bucket capacity
- Best for: API rate limiting with burst handling

#### 2. Leaky Bucket

```
┌─────────────────────────────────────────┐
│              Leaky Bucket               │
│  ┌───────────────────────────────────┐  │
│  │         │  Water Level            │  │
│  │    ▓▓▓▓▓│  [●][●][●]             │  │
│  │    ▓▓▓▓▓│                         │  │
│  │    ▓▓▓▓▓│  Leak Rate: 100/s       │  │
│  └───────────────────────────────────┘  │
│                                         │
│  Requests → Fill Bucket → Leak at rate  │
│  Bucket Full → Rejected                 │
└─────────────────────────────────────────┘
```

- Requests flow out at a constant rate
- Smooth traffic output
- No burst handling
- Best for: Traffic smoothing, API gateway

#### 3. Fixed Window

```
┌─────────────────────────────────────────┐
│            Fixed Window                 │
│                                         │
│  Window 1 (0-1s)    Window 2 (1-2s)    │
│  ┌────────────┐     ┌────────────┐     │
│  │ Count: 95  │     │ Count: 30  │     │
│  │ Limit: 100 │     │ Limit: 100 │     │
│  └────────────┘     └────────────┘     │
│                                         │
│  ⚠️ Boundary Issue: 200 requests at     │
│     window boundary (0.9s-1.1s)         │
└─────────────────────────────────────────┘
```

- Simple time window division
- Counter resets at window boundaries
- Boundary issue: possible 2x traffic at boundary
- Best for: Simple rate limiting scenarios

#### 4. Sliding Window Log

```
┌─────────────────────────────────────────┐
│         Sliding Window Log              │
│                                         │
│  Current Time: 1000ms                   │
│  Window: 1000ms                         │
│                                         │
│  Log: [100][200][300]...[900][1000]    │
│        ↑                            ↑   │
│        Expired                    Valid │
│                                         │
│  Count valid logs → Compare with limit  │
│  ✓ No boundary issue                    │
│  ✗ High memory usage                    │
└─────────────────────────────────────────┘
```

- Records each request timestamp
- Precise counting within time window
- No boundary issues
- Higher memory consumption
- Best for: Precise rate limiting

#### 5. Sliding Window Counter

```
┌─────────────────────────────────────────┐
│      Sliding Window Counter             │
│                                         │
│  Previous Window │ Current Window       │
│  ┌──────────────┐│┌──────────────┐     │
│  │ Count: 50    │││ Count: 30    │     │
│  │ Weight: 0.3  │││ Weight: 0.7  │     │
│  └──────────────┘│└──────────────┘     │
│                                         │
│  Weighted = 50 × 0.3 + 30 = 45         │
│  ✓ Low memory                           │
│  ✓ Minimal boundary issue               │
└─────────────────────────────────────────┘
```

- Combines previous and current window counts
- Weighted calculation based on time position
- Lower memory than log-based
- Minimal boundary issues
- Best for: Balanced solution

## 🏗️ Architecture

### Module Structure

```
RateLimit4j/
├── ratelimit4j-core/                    # Core module
│   ├── algorithm/                        # Algorithm interfaces
│   ├── config/                           # Configuration
│   ├── exception/                        # Exceptions
│   ├── storage/                          # Storage interfaces
│   └── telemetry/                        # Telemetry interfaces
│
├── ratelimit4j-local/                   # Local engine
│   ├── algorithm/                        # Local algorithms
│   └── circuit/                          # Circuit breaker
│
├── ratelimit4j-redis/                   # Redis engine (default)
│   ├── algorithm/                        # Redis algorithms
│   └── storage/                          # Redis storage
│
└── ratelimit4j-spring-boot-starter/     # Spring Boot starter
    ├── annotation/                       # Annotations
    ├── aspect/                           # AOP aspect
    ├── handler/                          # Fallback handlers
    ├── resolver/                         # Key resolvers
    └── autoconfigure/                    # Auto-configuration
```

### Key Resolution Priority

```
┌─────────────────────────────────────────────────────────────┐
│                    Key Resolution Flow                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. keys (SpEL) - Highest Priority                         │
│     ├── keys = {"#user.id"}                                │
│     └── Extract from method parameters                       │
│                                                             │
│  2. keyBuilder (Custom)                                     │
│     ├── keyBuilder = UserIdKeyBuilder.class                 │
│     └── Custom key building logic                           │
│                                                             │
│  3. dimension (Predefined)                                  │
│     ├── IP      → Extract from HttpServletRequest           │
│     ├── USER    → Extract from SecurityContext              │
│     ├── TENANT  → Extract from Header/Parameter             │
│     └── DEVICE  → Extract from Header/User-Agent            │
│                                                             │
│  4. Default (Method FQN) - Lowest Priority                  │
│     └── com.example.ApiController#methodName                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 📈 Project Status

### ✅ Implemented Features

| Module | Feature | Status | Description |
|--------|---------|--------|-------------|
| **Core** | Algorithm Types | ✅ Complete | 5 algorithms: Token Bucket, Leaky Bucket, Fixed Window, Sliding Window Log, Sliding Window Counter |
| **Core** | Dimension Types | ✅ Complete | 6 dimensions: METHOD, IP, USER, TENANT, DEVICE, CUSTOM |
| **Core** | @RateLimit Annotation | ✅ Complete | SpEL keys, KeyBuilder, dimension, multi-rule support |
| **Core** | Dimension Annotations | ✅ Complete | @IpRateLimit, @UserRateLimit, @TenantRateLimit, @DeviceRateLimit |
| **Core** | KeyBuilder/KeyResolver | ✅ Complete | Flexible key extraction interfaces |
| **Core** | FallbackHandler | ✅ Complete | Customizable fallback handling |
| **Core** | Telemetry | ✅ Complete | TelemetryEvent, TelemetryConfig interfaces |
| **Local** | Token Bucket | ✅ Complete | High-performance local token bucket |
| **Local** | Leaky Bucket | ✅ Complete | Smooth traffic output |
| **Local** | Fixed Window | ✅ Complete | Simple and efficient |
| **Local** | Sliding Window Log | ✅ Complete | Precise rate limiting |
| **Local** | Sliding Window Counter | ✅ Complete | Balanced solution |
| **Local** | Circuit Breaker | ✅ Complete | CLOSED/OPEN/HALF_OPEN states |
| **Redis** | Token Bucket | ✅ Complete | Redisson RRateLimiter based |
| **Redis** | Leaky Bucket | ✅ Complete | Distributed implementation |
| **Redis** | Fixed Window | ✅ Complete | Redis Lua scripts |
| **Redis** | Sliding Window Log | ✅ Complete | Distributed log algorithm |
| **Redis** | Sliding Window Counter | ✅ Complete | Distributed counter algorithm |
| **Starter** | Auto-Configuration | ✅ Complete | Spring Boot auto-config |
| **Starter** | AOP Aspect | ✅ Complete | @RateLimit interception |
| **Starter** | SpEL Key Resolver | ✅ Complete | Expression-based key extraction |
| **Starter** | Dimension Resolvers | ✅ Complete | IP, User, Tenant, Device, Method resolvers |
| **Starter** | OpenTelemetry | ✅ Complete | Metrics and tracing integration |

### 🔧 Needs Improvement

| Area | Issue | Priority | Description |
|------|-------|----------|-------------|
| **Testing** | Algorithm Tests | 🔴 High | Only LocalTokenBucketAlgorithmTest exists, other algorithms lack tests |
| **Testing** | Redis Engine Tests | 🔴 High | No test coverage for Redis algorithms |
| **Testing** | Starter Tests | 🔴 High | No integration tests for Spring Boot starter |
| **Testing** | Dimension Resolver Tests | 🔴 High | No tests for IP/User/Tenant/Device resolvers |
| **Testing** | Circuit Breaker Tests | 🔴 High | No unit tests for CircuitBreaker |
| **Testing** | Key Resolver Tests | 🟡 Medium | SpEL and composite resolver tests missing |
| **Docs** | Chinese README | 🟡 Medium | README_CN.md linked but not created |
| **Docs** | API Documentation | 🟡 Medium | Missing Javadoc for public APIs |
| **Docs** | Configuration Guide | 🟢 Low | Could add more configuration examples |
| **Docs** | Migration Guide | 🟢 Low | Missing version upgrade guide |

### ❌ Not Yet Implemented

| Feature | Priority | Description |
|---------|----------|-------------|
| **MySQL Storage Engine** | 🟢 Low | Database-backed rate limiting |
| **MongoDB Storage Engine** | 🟢 Low | NoSQL storage support |
| **In-memory Caffeine Cache** | 🟡 Medium | High-performance local caching layer |
| **Dynamic Configuration** | 🟡 Medium | Runtime rule updates without restart |
| **Config Center Integration** | 🟡 Medium | Nacos/Apollo/Spring Cloud Config support |
| **Prometheus Metrics** | 🟡 Medium | Native Prometheus exporter |
| **Micrometer Integration** | 🟡 Medium | Spring Boot metrics compatibility |
| **Health Check Endpoint** | 🟡 Medium | `/actuator/ratelimit` health indicator |
| **Admin Dashboard** | 🟢 Low | Visual rate limit management UI |
| **Rule Import/Export** | 🟢 Low | YAML/JSON rule configuration files |
| **Anti-Bot Protection** | 🟢 Low | Smart bot detection algorithms |
| **Blacklist/Whitelist** | 🟡 Medium | IP/User blocking functionality |
| **Rate Limit Dashboard API** | 🟢 Low | RESTful admin API |
| **Cluster-aware Local Engine** | 🟡 Medium | Hazelcast/Ignite distributed local |

### 💡 Extension Opportunities

| Extension | Potential | Description |
|-----------|-----------|-------------|
| **Algorithm Extensions** | ⭐⭐⭐⭐⭐ | Pluggable algorithm SPI for custom implementations |
| **Storage SPI** | ⭐⭐⭐⭐⭐ | Generic StorageProvider interface for new backends |
| **WebFlux Support** | ⭐⭐⭐⭐ | Reactive programming support for Spring WebFlux |
| **Quarkus/Micronaut** | ⭐⭐⭐ | Framework-agnostic core module |
| **Kubernetes HPA** | ⭐⭐⭐ | Horizontal Pod Autoscaler integration metrics |
| **Service Mesh** | ⭐⭐⭐ | Istio/Envoy rate limit adapter |
| **GraphQL Rate Limiting** | ⭐⭐⭐ | Field-level rate limiting for GraphQL APIs |
| **WebSocket Rate Limiting** | ⭐⭐⭐ | Connection and message rate limiting |
| **gRPC Interceptor** | ⭐⭐⭐ | Native gRPC rate limit interceptor |
| **Spring Security Integration** | ⭐⭐⭐⭐ | Security context-based rate limiting |

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Setup

```bash
# Clone the repository
git clone https://github.com/yourusername/RateLimit4j.git

# Navigate to project directory
cd RateLimit4j

# Build with Maven
mvn clean install

# Run tests
mvn test
```

### Testing Contributions Needed

The project currently has limited test coverage. We welcome contributions in:

```bash
# Run existing tests
mvn test -pl ratelimit4j-local

# Test areas that need contributions:
# - ratelimit4j-local/src/test/java/  (local algorithms)
# - ratelimit4j-redis/src/test/java/  (redis algorithms)  
# - ratelimit4j-spring-boot-starter/src/test/java/  (integration tests)
```

**Test Priority List:**
1. Algorithm unit tests (all 5 algorithms for both engines)
2. Dimension resolver tests (IP, User, Tenant, Device)
3. Circuit breaker tests
4. Key resolver tests (SpEL, Composite)
5. Spring Boot integration tests

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/yourusername">RateLimit4j Team</a>
</p>