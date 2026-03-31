# Technology Stack

**Analysis Date:** 2026-03-31

## Languages & Runtime

**Primary:**
- Java 17 - Used across all modules as the implementation language

**Secondary:**
- None

## Build System

**Build Tool:**
- Maven 3.x - Project build and dependency management

**Configuration:**
- Root `pom.xml` at `F:\JavaCode\RateLimit4j\pom.xml`
- Multi-module project with 4 sub-modules

**Build Plugins:**
- `maven-compiler-plugin` 3.11.0 - Java compilation
- `maven-surefire-plugin` 3.2.5 - Test execution
- `flatten-maven-plugin` 1.5.0 - Version property resolution
- `spring-boot-maven-plugin` - Spring Boot packaging

## Frameworks

**Core Frameworks:**
- Spring Boot 3.5.5 - Auto-configuration and starter module
- Spring AOP 6.x - Aspect-oriented programming for rate limit interceptors

**Testing:**
- JUnit Jupiter 5.x - Unit testing framework
- Mockito Core - Mocking framework for tests
- Embedded Redis 0.7.3 - For testing Redis-based implementations

**Storage:**
- Redisson 3.45.1 - Redis client for distributed rate limiting
- In-memory data structures (local module)

**Utilities:**
- Apache Commons Lang3 3.17.0 - Utility classes
- Guava 33.4.8-jre - Caching and concurrency utilities
- AspectJ Weaver - Runtime aspect weaving

**Observability:**
- OpenTelemetry 1.47.0 - Telemetry and metrics (optional integration)

## Dependencies

**Internal Modules:**
- `ratelimit4j-core` - Core interfaces and abstractions
- `ratelimit4j-local` - In-memory rate limiting algorithms
- `ratelimit4j-redis` - Distributed Redis-based rate limiting
- `ratelimit4j-spring-boot-starter` - Spring Boot auto-configuration

**Spring Boot Starters (Optional):**
- `spring-boot-starter` - Core Spring Boot
- `spring-boot-starter-aop` - AOP support
- `spring-boot-starter-web` - Web framework integration (optional)

**Key External Dependencies:**
| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.5.5 | Framework |
| Redisson | 3.45.1 | Redis client |
| Apache Commons Lang3 | 3.17.0 | Utilities |
| Guava | 33.4.8-jre | Caching/Concurrency |
| OpenTelemetry API/SDK | 1.47.0 | Telemetry (optional) |
| Lombok | Latest | Code generation |
| AspectJ Weaver | Latest | Aspect weaving |

## Configuration

**Maven Configuration:**
- Aliyun Maven repository configured for dependency resolution
- Version management via `${revision}` property
- Dependency management via `dependencyManagement` section

**Environment Configuration:**
- Uses Spring Boot configuration model (`RateLimitProperties`)
- Configuration classes: `ratelimit4j-spring-boot-starter/src/main/java/com/geek/ratelimit4j/starter/autoconfigure/RateLimitProperties.java`
- Auto-configuration: `ratelimit4j-spring-boot-starter/src/main/java/com/geek/ratelimit4j/starter/autoconfigure/RateLimitAutoConfiguration.java`

**Project Version:**
- Current: 1.0.0

## Module Structure

```
RateLimit4j (parent)
├── ratelimit4j-core/          # Core interfaces and abstractions
├── ratelimit4j-local/         # In-memory rate limiting
├── ratelimit4j-redis/         # Distributed Redis rate limiting
└── ratelimit4j-spring-boot-starter/  # Spring Boot integration
```

---

*Stack analysis: 2026-03-31*