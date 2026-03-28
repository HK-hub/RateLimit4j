# RateLimit4j Boot Starter

一个开箱即用的Java限流工具框架，支持本地和分布式限流，提供完整的限流算法实现。

## 功能特性

- **多种限流算法**：令牌桶、漏桶、固定窗口、滑动窗口日志、滑动窗口计数器
- **双模式支持**：本地限流（基于内存）和分布式限流（基于Redis）
- **注解驱动**：通过`@RateLimit`注解实现声明式限流
- **SpEL表达式**：支持从方法参数中提取限流Key
- **熔断保护**：内置熔断器，防止分布式存储故障影响业务
- **OpenTelemetry集成**：完整的监控追踪支持
- **高可配置**：灵活的YAML配置，支持多规则定义
- **Spring Boot Starter**：自动配置，开箱即用

## 快速开始

### Maven依赖

```xml
<dependency>
    <groupId>com.geek.ratelimit</groupId>
    <artifactId>ratelimit4j-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 基础配置

```yaml
ratelimit4j:
  enabled: true
  default-mode: LOCAL
  default-rule:
    algorithm: token_bucket
    rate: 100
    period: 1
```

### 注解式使用

```java
@RestController
public class ApiController {

    @RateLimit(rate = 100, period = 1, algorithm = AlgorithmType.TOKEN_BUCKET)
    public String index() {
        return "Hello World";
    }

    @RateLimit(keyExpression = "#user.id", rate = 50, period = 60,
                algorithm = AlgorithmType.SLIDING_WINDOW_COUNTER)
    public String userApi(User user) {
        return "User API";
    }

    @RateLimit(rate = 10, fallbackMethod = "fallback")
    public String apiWithFallback() {
        return "Success";
    }

    public String fallback() {
        return "Rate limited, please try again later";
    }
}
```

## 限流算法

| 算法 | 特点 | 适用场景 |
|------|------|----------|
| 令牌桶 (TOKEN_BUCKET) | 允许突发流量，恒定补充令牌 | API接口、间歇性流量 |
| 漏桶 (LEAKY_BUCKET) | 恒定流出速率，平滑流量 | 需要稳定流速的场景 |
| 固定窗口 (FIXED_WINDOW) | 简单高效，有临界问题 | 简单限流、对精度要求不高 |
| 滑动窗口日志 (SLIDING_WINDOW_LOG) | 精确控制，内存开销大 | 需要精确限流的场景 |
| 滑动窗口计数器 (SLIDING_WINDOW_COUNTER) | 平衡精度与性能 | 一般限流场景 |

## 配置说明

### 完整配置示例

```yaml
ratelimit4j:
  enabled: true
  default-mode: LOCAL

  default-rule:
    algorithm: token_bucket
    rate: 100
    period: 1
    key-prefix: ""
    max-burst: 100

  rules:
    api-user:
      algorithm: sliding_window_counter
      rate: 50
      period: 60
      key-prefix: "user:api"
    api-ip:
      algorithm: fixed_window
      rate: 200
      period: 1
      key-prefix: "ip:api"

  redis:
    host: localhost
    port: 6379
    password: ""
    database: 0
    timeout: 2000

  fallback:
    enabled: true
    degrade-to-local: true
    failure-threshold: 5
    recovery-timeout: 30000

  telemetry:
    enabled: true
    service-name: "RateLimit4j"
    endpoint: "http://localhost:4317"
```

### 配置项说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `enabled` | 是否启用限流 | true |
| `default-mode` | 默认限流模式 (LOCAL/DISTRIBUTED) | LOCAL |
| `default-rule.algorithm` | 默认算法 | token_bucket |
| `default-rule.rate` | 每周期请求数 | 100 |
| `default-rule.period` | 周期（秒） | 1 |
| `default-rule.max-burst` | 最大突发容量 | 100 |
| `fallback.degrade-to-local` | Redis故障时降级到本地限流 | true |
| `fallback.failure-threshold` | 熔断失败次数阈值 | 5 |
| `fallback.recovery-timeout` | 熔断恢复时间（毫秒） | 30000 |

## @RateLimit注解参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `algorithm` | 限流算法 | TOKEN_BUCKET |
| `rate` | 每周期请求数 | 100 |
| `period` | 周期（秒） | 1 |
| `keyPrefix` | Key前缀 | "" |
| `keyExpression` | SpEL表达式 | "" |
| `fallbackMethod` | 降级方法名 | "" |
| `exceptionClass` | 异常类型 | RateLimitException |
| `maxBurst` | 最大突发容量 | 0(使用rate) |
| `enabled` | 是否启用 | true |

### SpEL表达式示例

```java
// 用户ID维度
@RateLimit(keyExpression = "#user.id", rate = 50)

// IP地址维度
@RateLimit(keyExpression = "#request.remoteAddr", rate = 100)

// Header维度
@RateLimit(keyExpression = "#request.getHeader('X-Token')", rate = 200)

// 组合Key
@RateLimit(keyExpression = "#user.id + ':' + #request.requestURI", rate = 30)
```

## 监控指标

OpenTelemetry集成后暴露以下指标：

| 指标名称 | 类型 | 说明 |
|----------|------|------|
| `ratelimit.requests.total` | Counter | 总请求计数 |
| `ratelimit.requests.allowed` | Counter | 允许通过数 |
| `ratelimit.requests.rejected` | Counter | 被限流数 |
| `ratelimit.algorithm.latency` | Histogram | 算法执行延迟 |

## 模块结构

```
RateLimit4j/
├── ratelimit4j-core           // 核心接口和抽象类
├── ratelimit4j-local          // 本地限流实现
├── ratelimit4j-redis          // Redis分布式限流实现
├── ratelimit4j-spring-boot-starter  // Spring Boot自动配置
```

## 技术栈

- Java 17+
- Spring Boot 3.x
- Redisson 3.45.x
- Apache Commons Lang3
- OpenTelemetry SDK

## 许可证

Apache License 2.0