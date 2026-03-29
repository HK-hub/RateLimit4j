<p align="center">
  <img src="docs/images/logo.png" alt="RateLimit4j Logo" width="200">
</p>

<h1 align="center">RateLimit4j</h1>

<p align="center">
  <strong>🔥 强大、可扩展的 Java 限流框架</strong>
</p>

<p align="center">
  <a href="#特性">特性</a> •
  <a href="#快速开始">快速开始</a> •
  <a href="#使用示例">使用示例</a> •
  <a href="#配置说明">配置说明</a> •
  <a href="#算法说明">算法说明</a> •
  <a href="README.md">English</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17+-green?logo=java" alt="Java 17+">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?logo=spring" alt="Spring Boot 3.x">
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue" alt="License">
  <img src="https://img.shields.io/badge/Version-1.0.0-orange" alt="Version">
</p>

---

## ✨ 特性

### 🎯 核心特性

- **5种限流算法** - 令牌桶、漏桶、固定窗口、滑动窗口日志、滑动窗口计数器
- **双引擎支持** - 本地（JVM内存）和 Redis（分布式）引擎，**默认使用 Redis**
- **Spring Boot 集成** - 基于 `@RateLimit` 注解的自动配置
- **多维度限流** - 支持 IP、用户、租户、设备等维度
- **灵活的Key解析** - SpEL表达式、自定义KeyBuilder、预定义维度
- **降级处理** - 可自定义拒绝请求的降级处理器
- **OpenTelemetry 集成** - 内置指标和追踪支持
- **高性能** - 针对高并发场景优化

### 🔧 高级特性

| 特性 | 描述 |
|------|------|
| **多规则限流** | 单个方法支持多个 `@RateLimit` 注解 |
| **引擎选择** | 每个注解可选择 LOCAL、REDIS 或 AUTO |
| **主引擎配置** | 在 `application.yml` 中配置默认引擎（默认：Redis） |
| **熔断器** | 内置熔断器，Redis 故障时自动降级 |
| **自定义异常** | 支持定义自己的异常类型 |
| **Key前缀** | 通过 key 前缀实现命名空间隔离 |

## 🚀 快速开始

### 环境要求

- Java 17+
- Spring Boot 3.x
- Maven 3.6+ 或 Gradle 7+

### 安装

在 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.geek.ratelimit4j</groupId>
    <artifactId>ratelimit4j-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

或 `build.gradle`：

```groovy
implementation 'com.geek.ratelimit4j:ratelimit4j-spring-boot-starter:1.0.0'
```

## 📝 使用示例

### 1. 基础使用

```java
@RestController
public class ApiController {

    // 简单限流：每秒100次请求（默认使用Redis）
    @RateLimit(rate = 100)
    public String basicApi() {
        return "Hello World";
    }
}
```

### 2. 多规则限流

```java
// 多规则：3秒1次 + 1分钟10次
@RateLimit(rate = 1, period = 3)
@RateLimit(rate = 10, period = 60)
public String multiRuleApi() {
    return "多规则限流";
}
```

### 3. 维度限流

```java
// IP限流
@IpRateLimit(rate = 10, period = 1)
public String ipApi(HttpServletRequest request) {
    return "IP限流";
}

// 用户限流
@UserRateLimit(rate = 100, period = 60)
public String userApi(@CurrentUser User user) {
    return "用户限流";
}

// 租户限流（多租户场景）
@TenantRateLimit(rate = 1000, period = 1)
public String tenantApi() {
    return "租户限流";
}

// 设备限流（移动端/IoT场景）
@DeviceRateLimit(rate = 10, period = 1)
public String deviceApi() {
    return "设备限流";
}
```

### 4. 自定义Key（SpEL表达式）

```java
// 从方法参数中提取Key
@RateLimit(keys = {"#user.id", "#request.remoteAddr"}, rate = 50)
public String customKeyApi(User user, HttpServletRequest request) {
    return "自定义Key限流";
}
```

### 5. 自定义KeyBuilder

```java
@Component
public class UserIdKeyBuilder implements KeyBuilder {
    @Override
    public String build(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        // 自定义逻辑提取用户ID
        return "user:" + extractUserId(args);
    }
}

// 使用自定义KeyBuilder
@RateLimit(keyBuilder = UserIdKeyBuilder.class, rate = 100)
public String customBuilderApi(User user) {
    return "自定义Builder限流";
}
```

### 6. 本地引擎（单机场景）

```java
// 使用本地引擎（单机场景）
@RateLimit(rate = 100, engine = EngineType.LOCAL)
public String localApi() {
    return "本地限流";
}
```

### 7. 降级处理器

```java
@Component
public class CustomFallbackHandler implements FallbackHandler {
    @Override
    public Object handle(ProceedingJoinPoint joinPoint, RateLimitException e) {
        // 自定义降级逻辑
        return ResponseEntity
            .status(429)
            .body("请求过于频繁，请稍后重试");
    }
}

// 使用自定义降级处理器
@RateLimit(rate = 10, fallbackHandler = CustomFallbackHandler.class)
public String fallbackApi() {
    return "带降级处理";
}
```

## ⚙️ 配置说明

### application.yml

```yaml
ratelimit4j:
  # 是否启用限流（默认：true）
  enabled: true
  
  # 主引擎：redis 或 local（默认：redis）
  primary-engine: redis
  
  # 默认限流规则
  default-rule:
    algorithm: token_bucket
    rate: 100
    period: 1
    key-prefix: ""
    max-burst: 100
  
  # Redis配置
  redis:
    enabled: true
    host: localhost
    port: 6379
    password: ""
    database: 0
    timeout: 2000
  
  # 降级配置
  fallback:
    enabled: true
    degrade-to-local: true
    failure-threshold: 5
    recovery-timeout: 30000
  
  # OpenTelemetry配置
  telemetry:
    enabled: true
    service-name: RateLimit4j
    endpoint: http://localhost:4317
```

## 📊 算法说明

### 算法对比

| 算法 | 精度 | 内存 | 边界问题 | 适用场景 |
|------|------|------|----------|----------|
| **令牌桶** | 高 | 低 | 无 | 突发流量处理 |
| **漏桶** | 高 | 低 | 无 | 流量平滑输出 |
| **固定窗口** | 低 | 最低 | 有 | 简单限流场景 |
| **滑动窗口日志** | 最高 | 高 | 无 | 精确限流 |
| **滑动窗口计数器** | 中 | 低 | 极小 | 平衡方案 |

### 算法详解

#### 1. 令牌桶算法

```
┌─────────────────────────────────────────┐
│              令牌桶算法                  │
│  ┌───────────────────────────────────┐  │
│  │  令牌: [●][●][●][●][●]           │  │
│  │  速率: 100 tokens/周期            │  │
│  │  最大突发: 100                    │  │
│  └───────────────────────────────────┘  │
│                                         │
│  请求 → 消耗令牌 → 允许通过              │
│  请求 → 无令牌 → 拒绝                    │
└─────────────────────────────────────────┘
```

- 按固定速率生成令牌
- 请求消耗令牌
- 支持突发流量（桶容量范围内）
- 适用场景：API限流、突发流量处理

#### 2. 漏桶算法

```
┌─────────────────────────────────────────┐
│              漏桶算法                    │
│  ┌───────────────────────────────────┐  │
│  │         │  水位                    │  │
│  │    ▓▓▓▓▓│  [●][●][●]             │  │
│  │    ▓▓▓▓▓│                         │  │
│  │    ▓▓▓▓▓│  漏出速率: 100/s         │  │
│  └───────────────────────────────────┘  │
│                                         │
│  请求 → 注入桶中 → 恒定速率流出          │
│  桶满 → 拒绝                            │
└─────────────────────────────────────────┘
```

- 请求以恒定速率流出
- 平滑流量输出
- 无突发处理
- 适用场景：流量整形、API网关

#### 3. 固定窗口算法

```
┌─────────────────────────────────────────┐
│            固定窗口算法                  │
│                                         │
│  窗口1 (0-1s)      窗口2 (1-2s)        │
│  ┌────────────┐     ┌────────────┐     │
│  │ 计数: 95   │     │ 计数: 30   │     │
│  │ 限制: 100  │     │ 限制: 100  │     │
│  └────────────┘     └────────────┘     │
│                                         │
│  ⚠️ 边界问题: 窗口边界可能产生2倍流量     │
│     (如0.9s-1.1s区间可能通过200次请求)   │
└─────────────────────────────────────────┘
```

- 简单的时间窗口划分
- 窗口边界计数器重置
- 存在边界问题
- 适用场景：简单限流场景

#### 4. 滑动窗口日志算法

```
┌─────────────────────────────────────────┐
│         滑动窗口日志算法                  │
│                                         │
│  当前时间: 1000ms                        │
│  窗口大小: 1000ms                        │
│                                         │
│  日志: [100][200][300]...[900][1000]   │
│        ↑                            ↑   │
│        已过期                      有效  │
│                                         │
│  统计有效日志数 → 与限流阈值比较          │
│  ✓ 无边界问题                            │
│  ✗ 内存开销较高                          │
└─────────────────────────────────────────┘
```

- 记录每次请求的时间戳
- 精确统计时间窗口内的请求数
- 无边界问题
- 内存开销较大
- 适用场景：精确限流

#### 5. 滑动窗口计数器算法

```
┌─────────────────────────────────────────┐
│      滑动窗口计数器算法                   │
│                                         │
│  前一个窗口       │ 当前窗口              │
│  ┌──────────────┐│┌──────────────┐     │
│  │ 计数: 50     │││ 计数: 30     │     │
│  │ 权重: 0.3    │││ 权重: 0.7    │     │
│  └──────────────┘│└──────────────┘     │
│                                         │
│  加权计数 = 50 × 0.3 + 30 = 45          │
│  ✓ 内存开销低                            │
│  ✓ 边界问题极小                          │
└─────────────────────────────────────────┘
```

- 结合前一窗口和当前窗口计数
- 根据时间位置加权计算
- 内存开销低于日志算法
- 边界问题极小
- 适用场景：平衡方案

## 🏗️ 架构设计

### 模块结构

```
RateLimit4j/
├── ratelimit4j-core/                    # 核心模块
│   ├── algorithm/                        # 算法接口
│   ├── config/                           # 配置定义
│   ├── exception/                        # 异常定义
│   ├── storage/                          # 存储接口
│   └── telemetry/                        # 监控接口
│
├── ratelimit4j-local/                   # 本地引擎
│   ├── algorithm/                        # 本地算法实现
│   └── circuit/                          # 熔断器
│
├── ratelimit4j-redis/                   # Redis引擎（默认）
│   ├── algorithm/                        # Redis算法实现
│   └── storage/                          # Redis存储
│
└── ratelimit4j-spring-boot-starter/     # Spring Boot Starter
    ├── annotation/                       # 注解定义
    ├── aspect/                           # AOP切面
    ├── handler/                          # 降级处理器
    ├── resolver/                         # Key解析器
    └── autoconfigure/                    # 自动配置
```

### Key提取优先级

```
┌─────────────────────────────────────────────────────────────┐
│                    Key提取优先级                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. keys（SpEL表达式）最高优先级                             │
│     └── keys = {"#user.id"}                                │
│                                                             │
│  2. keyBuilder（自定义构建器）                               │
│     └── keyBuilder = UserIdKeyBuilder.class                 │
│                                                             │
│  3. dimension（预定义维度）                                  │
│     ├── IP      → 从 HttpServletRequest 提取                │
│     ├── USER    → 从 SecurityContext 提取                   │
│     ├── TENANT  → 从 Header/参数 提取                       │
│     └── DEVICE  → 从 Header/User-Agent 提取                 │
│                                                             │
│  4. 默认（方法全限定名）最低优先级                            │
│     └── com.example.ApiController#methodName                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 🤝 参与贡献

欢迎贡献代码！请遵循以下步骤：

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

### 开发环境搭建

```bash
# 克隆仓库
git clone https://github.com/yourusername/RateLimit4j.git

# 进入项目目录
cd RateLimit4j

# Maven构建
mvn clean install

# 运行测试
mvn test
```

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 详见 [LICENSE](LICENSE) 文件。

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/yourusername">RateLimit4j Team</a>
</p>