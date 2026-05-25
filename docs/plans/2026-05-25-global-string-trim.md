# 输入数据自动去除首尾空格 — 修改方案

> 日期：2026-05-25
> 范围：项目管理、合同管理、付款里程碑管理 —— 查询条件、新建、编辑提交时自动去除字符串首尾空格
> 方案选型：**后端全局**（一处配置，全模块生效，任何客户端都无法绕过）

## 1. 需求

「项目管理(Project)」「合同管理(Contract)」「付款里程碑管理(Payment)」三个模块中，用户在
查询条件、新建页面、编辑页面输入/录入/修改的数据，在**查询、保存、提交**时自动去掉首尾空格。

## 2. 现状分析（为什么要这么改）

入站请求有**两条完全不同的链路**，必须分别处理：

| 场景 | HTTP | Spring 绑定方式 | 现状 |
|---|---|---|---|
| 查询 `list(Entity)` | GET | `WebDataBinder`（普通对象绑定） | `BaseController.@InitBinder` 只转 Date，**不 trim String** |
| 新建/编辑 `add/edit(@RequestBody Entity)` | POST/PUT | Jackson 反序列化 | `ApplicationConfig` 的 Jackson 只配了时区，**无字段级 String trim** |

补充：现有的 `XssHttpServletRequestWrapper` 虽然会 trim，但
1. 它对 JSON 是**整体 trim 整串**，不是字段级；
2. 其 `urlPatterns` 仅含 `/system/*,/monitor/*,/tool/*`，**不覆盖 `/project/**`**。

因此目标三模块当前完全没有 trim 行为。

## 3. 方案

后端全局拦截，三处改动。三个目标模块的 Controller 均继承 `BaseController` 且走 Jackson，故自动覆盖；
其他模块同样受益（一致性更好）。

### 3.1 新增全局字符串反序列化器（JSON 体 / 新建·编辑·提交）

文件：`ruoyi-common/src/main/java/com/ruoyi/common/core/deserializer/TrimStringJsonDeserializer.java`

- 实现 `JsonDeserializer<String>` + `ContextualDeserializer`。
- `deserialize()`：对 String 值执行 `trim()`（null 原样返回）。
- `createContextual()`：在绑定阶段拿到字段名，对**密码类字段跳过 trim**，黑名单：
  `password / oldPassword / newPassword / confirmPassword`。
- 关键点：普通 `JsonDeserializer` 在 `deserialize()` 里拿不到字段名，必须借助
  `ContextualDeserializer` 才能按字段名排除。

```java
public class TrimStringJsonDeserializer extends JsonDeserializer<String> implements ContextualDeserializer {
    private static final Set<String> SKIP_FIELDS =
        Set.of("password", "oldPassword", "newPassword", "confirmPassword");
    private final boolean skip;
    public TrimStringJsonDeserializer() { this(false); }
    private TrimStringJsonDeserializer(boolean skip) { this.skip = skip; }

    @Override public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String v = p.getValueAsString();
        return (v == null || skip) ? v : v.trim();
    }
    @Override public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        return (property != null && SKIP_FIELDS.contains(property.getName()))
            ? new TrimStringJsonDeserializer(true) : this;
    }
}
```

### 3.2 注册到全局 Jackson

文件：`ruoyi-framework/src/main/java/com/ruoyi/framework/config/ApplicationConfig.java`

在已有的 `Jackson2ObjectMapperBuilderCustomizer` Bean 中追加注册：

```java
return jacksonObjectMapperBuilder -> {
    jacksonObjectMapperBuilder.timeZone(TimeZone.getDefault());
    jacksonObjectMapperBuilder.deserializerByType(String.class, new TrimStringJsonDeserializer());
};
```

### 3.3 GET 查询参数 trim

文件：`ruoyi-common/src/main/java/com/ruoyi/common/core/controller/BaseController.java`

在 `@InitBinder` 中追加：

```java
// emptyAsNull=false 仅 trim，不把空串转 null（与 MyBatis <if test="x!=null and x!=''"> 语义一致）
binder.registerCustomEditor(String.class, new StringTrimmerEditor(false));
```

## 4. 影响范围与风险

- **影响范围**：全系统所有 `@RequestBody` String 字段 + 所有 GET 对象绑定 String 参数。
- **密码安全**：已通过黑名单排除密码类字段，登录/改密不受影响。
- **多值数组字段**（如 Payment 的 `deptIds`/`paymentStatuses`）内 String 元素也会被 trim，
  均为 ID/状态码，无副作用。
- **空串语义**：`StringTrimmerEditor(false)` 只 trim 不转 null，纯空格查询条件 → 空串，
  MyBatis 的 `<if test="x!=null and x!=''">` 仍会跳过，行为不变。

## 5. 验证

1. 编译：`mvn clean compile -pl ruoyi-framework -am -Dmaven.test.skip=true` ✅ 已通过。
2. 手工验证（需后端 + 前端运行；本地后端端口 8085）：
   - 合同/项目/付款 **新建**：输入 `  测试  ` 保存 → 入库应为 `测试`（无首尾空格）。
   - **编辑**：同上。
   - **查询条件**：名称/编号前后加空格 → 仍能正常命中。
   - **登录**：密码含首尾空格仍可登录（验证黑名单生效）。

## 6. 变更文件清单

| 文件 | 类型 |
|---|---|
| `ruoyi-common/.../core/deserializer/TrimStringJsonDeserializer.java` | 新增 |
| `ruoyi-framework/.../config/ApplicationConfig.java` | 修改 |
| `ruoyi-common/.../core/controller/BaseController.java` | 修改 |
