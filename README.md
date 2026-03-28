# RateLimit4j Boot Starter

一个开箱即用的Java限流工具箱，同时支持本地和分布式限流。

## 功能特性

- 支持本地限流（基于内存）
- 支持分布式限流（基于Redis）
- 多种限流算法：令牌桶、漏桶、固定窗口、滑动窗口
- Spring Boot自动配置
- 注解驱动的限流方式
- 可扩展的自定义限流策略
- 高可用设计，支持集群环境

## 架构设计

### 核心组件

1. **RateLimiter接口** - 限流器抽象接口
2. **LocalRateLimiter** - 本地限流实现
3. **DistributedRateLimiter** - 分布式限流实现
4. **AlgorithmFactory** - 限流算法工厂
5. **RateLimitAspect** - 切面处理注解限流
6. **ConfigurationProperties** - 配置属性绑定

### 模块划分

```
ratelimit4j-core           // 核心接口和抽象类
ratelimit4j-local          // 本地限流实现
ratelimit4j-redis          // Redis分布式限流实现
ratelimit4j-spring-boot-starter // Spring Boot Starter
```

## 核心算法设计

### 1. 令牌桶算法 (Token Bucket)

- 以固定速率向桶中添加令牌
- 请求需要获取令牌才能通过
- 桶满时丢弃多余令牌
- 支持突发流量处理

### 2. 漏桶算法 (Leaky Bucket)

- 固定速率流出请求
- 平滑输出请求流量
- 缓冲区满时拒绝新请求

### 3. 固定窗口计数器 (Fixed Window Counter)

- 将时间划分为固定大小窗口
- 在每个窗口内统计请求数量
- 超过阈值则拒绝请求

### 4. 滑动窗口日志 (Sliding Window Log)

- 记录每次请求的时间戳
- 统计最近时间窗口内的请求数
- 精确控制请求速率

### 5. 滑动窗口计数器 (Sliding Window Counter)

- 结合多个固定窗口
- 使用加权平均计算当前窗口请求数
- 平衡精度与性能

## 高可用设计

### 容错机制

- 本地缓存兜底：Redis不可用时自动降级到本地限流
- 熔断保护：防止因限流服务异常影响主业务流程
- 异常隔离：限流异常不影响业务逻辑执行

### 集群支持

- Redis集群模式支持
- 数据分片存储
- 节点故障自动转移

### 性能优化

- 最小化网络IO操作
- 连接池管理
- 批量操作支持
- 异步处理非关键路径

## 功能需求分析

### 基础功能

- [ ] 支持多种限流算法
- [ ] 支持本地和分布式两种模式
- [ ] 提供编程式和声明式(注解)使用方式
- [ ] 支持自定义限流规则
- [ ] 提供详细的监控指标

### 高级功能

- [ ] 动态配置更新
- [ ] 多维度限流（用户、IP、接口等）
- [ ] 黑白名单机制
- [ ] 流量整形
- [ ] 自适应限流

### 非功能性需求

- [ ] 高性能低延迟
- [ ] 高可用性和容错能力
- [ ] 易于集成和使用
- [ ] 完善的文档和示例

## 使用指南

### Maven依赖

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>ratelimit4j-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 配置文件

```yaml
ratelimit4j:
  # 启用限流
  enabled: true
  # 默认限流模式: local | redis
  default-mode: local
  # Redis配置（分布式限流时需要）
  redis:
    host: localhost
    port: 6379
    timeout: 2000
  # 默认限流规则
  default-rule:
    # 限流算法: token_bucket | leaky_bucket | fixed_window | sliding_window
    algorithm: token_bucket
    # 限流速率（每秒允许的请求数）
    rate: 100
    # 限流周期（秒）
    period: 1
```

### 编程式使用

```java
@Autowired
private RateLimiter rateLimiter;

public void someMethod() {
    if (rateLimiter.tryAcquire()) {
        // 执行业务逻辑
    } else {
        // 处理限流情况
        throw new RateLimitException("Too many requests");
    }
}
```

### 注解式使用

```java
@RateLimit(rate = 100, period = 1, algorithm = "token_bucket")
public String apiEndpoint() {
    return "Hello World";
}
```

## 开发计划

### 第一阶段：核心功能实现

- [ ] 完成核心接口设计
- [ ] 实现本地限流功能
- [ ] 实现Redis分布式限流
- [ ] 完成Spring Boot Starter集成

### 第二阶段：算法完善

- [ ] 实现所有限流算法
- [ ] 添加算法性能测试
- [ ] 优化算法实现细节

### 第三阶段：高级特性

- [ ] 实现动态配置更新
- [ ] 添加监控指标暴露
- [ ] 完善异常处理机制

### 第四阶段：质量保证

- [ ] 编写单元测试
- [ ] 进行性能测试
- [ ] 完善文档和示例

## 贡献指南

欢迎提交Issue和Pull Request来改进这个项目。

## 许可证

Apache License 2.0
