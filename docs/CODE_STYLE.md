# RateLimit4j 代码开发规范

> 本文档定义了RateLimit4j项目的代码开发规范，所有代码变更必须遵循此规范。

## 一、注释规范

### 1.1 类注释

**必须包含：**
- 类的功能描述
- 职责说明（使用`<p>`标签）
- 使用示例（使用`<pre>{@code}`标签）
- `@author` 和 `@since` 标签
- 相关类的`@see`标签

**格式模板：**
```java
/**
 * 类功能简述
 * 类功能详细描述（可选）
 *
 * <p>职责说明：</p>
 * <ul>
 *   <li>职责1</li>
 *   <li>职责2</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 示例代码
 * }</pre>
 *
 * @author RateLimit4j
 * @since 1.0.0
 * @see RelatedClass
 */
public class MyClass {
```

### 1.2 方法注释

**必须包含：**
- 方法功能描述
- 参数说明（`@param`）
- 返回值说明（`@return`）
- 异常说明（`@throws`）
- 处理流程（复杂方法必须）

**格式模板：**
```java
/**
 * 方法功能描述
 *
 * <p>处理流程：</p>
 * <ol>
 *   <li>步骤1</li>
 *   <li>步骤2</li>
 * </ol>
 *
 * @param param1 参数1说明
 * @param param2 参数2说明
 * @return 返回值说明
 * @throws ExceptionType 异常说明
 */
public String myMethod(String param1, String param2) {
```

### 1.3 成员变量注释

**必须包含：**
- 变量功能描述
- 单行注释格式

**格式模板：**
```java
/**
 * 变量功能描述
 * 可包含更多细节
 */
private final String myField;

/** 简短描述 */
private int count;
```

### 1.4 语句注释

**要求：**
- 关键逻辑必须有注释说明
- 使用单行注释 `//` 或块注释 `/* */`
- 注释应说明"为什么"而非"是什么"

**示例：**
```java
// 检查是否配置了有效的KeyBuilder
if (keyBuilderClass != null && keyBuilderClass != KeyBuilder.class) {
    // 从Spring容器获取KeyBuilder实例
    Map<String, KeyBuilder> builders = applicationContext.getBeansOfType(KeyBuilder.class);
}
```

---

## 二、命名规范

### 2.1 类命名

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 接口 | 名词或形容词 | `RateLimitKeyResolver` |
| 抽象类 | Abstract前缀 | `AbstractRateLimitAspect` |
| 实现类 | 功能描述 | `DefaultRateLimitAspect` |
| 工具类 | Utils后缀 | `StringUtils` |
| 异常类 | Exception后缀 | `RateLimitException` |

### 2.2 方法命名

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 查询方法 | get/find/query | `getKey()`, `findResolver()` |
| 判断方法 | is/has/can | `isEnabled()`, `hasAlgorithm()` |
| 处理方法 | handle/process/resolve | `handleRejection()` |
| 构建方法 | build/create | `buildConfig()` |

### 2.3 变量命名

- 使用驼峰命名法
- 成员变量使用有意义的名称
- 避免单字母变量（循环变量除外）

---

## 三、代码结构规范

### 3.1 类结构顺序

```java
public class MyClass {
    // 1. 静态常量
    public static final String CONSTANT = "value";
    
    // 2. 成员变量
    private final String field;
    
    // 3. 构造函数
    public MyClass(String field) {
        this.field = field;
    }
    
    // 4. 公共方法
    public void publicMethod() { }
    
    // 5. 受保护方法
    protected void protectedMethod() { }
    
    // 6. 私有方法
    private void privateMethod() { }
    
    // 7. 内部类
    private static class InnerClass { }
}
```

### 3.2 方法长度

- 单个方法不超过 50 行
- 超过 30 行应考虑拆分
- 复杂逻辑应抽取私有方法

### 3.3 分隔注释

使用分隔注释划分代码区块：

```java
// ==================== 成员变量 ====================

// ==================== 构造函数 ====================

// ==================== 公共方法 ====================

// ==================== 私有方法 ====================
```

---

## 四、API设计规范

### 4.1 接口设计

- 单一职责原则
- 接口方法不超过 5 个
- 提供清晰的Javadoc

### 4.2 参数校验

```java
public void method(String param) {
    Objects.requireNonNull(param, "param must not be null");
    // 业务逻辑
}
```

### 4.3 异常处理

- 使用有意义的异常类型
- 异常消息包含上下文信息
- 避免捕获后忽略

```java
// 正确
if (algorithm == null) {
    throw new IllegalArgumentException(
        String.format("No algorithm found for type=%s", type));
}

// 错误
if (algorithm == null) {
    throw new RuntimeException("Error");
}
```

---

## 五、扩展性设计规范

### 5.1 条件注入

使用 `@ConditionalOnMissingBean` 支持用户扩展：

```java
@Bean
@ConditionalOnMissingBean
public RateLimitKeyResolver myKeyResolver() {
    return new DefaultKeyResolver();
}
```

### 5.2 策略模式

使用接口+实现类模式支持扩展：

```java
public interface RateLimitKeyResolver {
    boolean supports(RateLimitResolveContext context);
    String resolve(RateLimitResolveContext context);
    int getOrder();
}
```

### 5.3 模板方法

抽象类定义骨架，子类实现细节：

```java
public abstract class AbstractRateLimitAspect {
    // 模板方法（final）
    protected final Object processRateLimits(...) { }
    
    // 抽象方法（子类必须实现）
    protected abstract EngineType resolveDefaultEngine();
    
    // hook方法（子类可覆盖）
    protected RateLimitConfig buildConfig(RateLimit rateLimit) { }
}
```

---

## 六、代码审查清单

在提交代码前，请确认：

- [ ] 所有类都有完整的Javadoc注释
- [ ] 所有公共方法都有注释
- [ ] 关键逻辑有语句注释
- [ ] 变量命名清晰有意义
- [ ] 方法长度符合规范
- [ ] 使用了适当的分隔注释
- [ ] 参数有null检查
- [ ] 异常消息清晰有意义
- [ ] 使用了条件注入支持扩展
- [ ] 编译无错误无警告
- [ ] 单元测试通过

---

## 七、规范执行

### 7.1 自动检查

- 使用Checkstyle或SpotBugs进行静态代码检查
- 配置CI流程自动执行规范检查

### 7.2 代码审查

- PR必须经过代码审查
- 审查者应检查规范遵守情况
- 不符合规范的代码应要求修改

---

**最后更新：** 2026-03-30  
**维护者：** RateLimit4j Team