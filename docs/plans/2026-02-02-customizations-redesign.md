# Customizations 结构化规格设计

## 问题

当前 `customizations` 只有 `列名 → description` 的扁平映射：

```yaml
customizations: {}
  # <列名>:
  #   description: "描述你想要的非标准 UI 效果"
```

存在三个问题：
1. **无法重现**：自然语言描述不够结构化，重新生成时阶段 5 无法稳定还原
2. **表达力不足**：只能描述列级别定制，无法表达页面级、表单级、工具栏等需求
3. **key 绑定列名**：定制需求本质是功能意图，可能影响多个字段或不涉及具体字段

## 方案

### 数据结构

以**功能标识**为 key 的结构化规格：

```yaml
customizations:
  <功能标识>:                    # 语义化命名，如 dept_tree_select
    intent: <string>             # 用户原始意图，保留上下文
    component:
      type: <string>             # 组件名(如 el-tree-select) 或 "custom"
      # --- 已有组件 ---
      props: { ... }             # 关键 props，写到接口级
      events: { ... }            # 关键事件绑定（可选）
      # --- 自建组件 (type: custom) ---
      spec: <string>             # 功能描述 + 关键约束
    bindTo: <string | string[]>  # 列名 | 列名数组 | toolbar | form | dialog | page（可选）
    service:                     # null 表示纯前端，不需要后端
      endpoint: <string>         # HTTP method + path
      source: <string>           # "复用 XxxService" 或 "new"
      returns: <string>          # 返回数据结构描述（可选）
      newApi: <bool>             # 是否需要新建接口
    notes: <string | null>       # 补充说明
```

### 字段规则

- `type` 非 `custom` 时写 `props`/`events`（已有组件接口已知）
- `type` 为 `custom` 时写 `spec`（自建组件只写意图+关键约束）
- `bindTo` 可选，不是所有定制都绑定到具体字段
- `service` 为 `null` 时表示纯前端改动

### 阶段分工

- **阶段 3**：确认标准字段配置（基本信息、字段信息、生成信息）
- **阶段 4**：CLI 生成标准 CRUD 代码并部署
- **阶段 5**：代码生成后，探查定制需求 → 固化到 yml → 基于已生成代码执行改造

### 阶段 5 交互策略

1. **主动识别**：扫描字段列表，找出可能需要定制的候选字段（外键关联、`_id` 结尾等），列出告知用户
2. **用户描述**：用户说需求，可能一次多个或逐个说，也可以补充 AI 未识别的
3. **逐个探查**：每个需求问关键决策点（数据量级、选择行为、现有接口），技术细节自行判断
4. **展示确认**：全部确认后展示完整 customizations yaml 汇总
5. **循环直到完成**：用户说 OK 或继续补充，不预设轮数
6. **持久化**：将确认后的 customizations 写入 yml，然后基于已生成代码执行改造

**原则**：问得少但问到点上，不问用户不需要关心的技术细节（debounce、renderAfterExpand 等由 AI 决定）。定制探查放在代码生成之后，因为改造需要基于已生成的代码结构来实施。

## 示例

```yaml
customizations:
  dept_tree_select:
    intent: "部门选择，树形结构，任意层级可选"
    component:
      type: el-tree-select
      props:
        data: "部门树数据"
        checkStrictly: true
        valueKey: "id"
        renderAfterExpand: false
    bindTo: dept_id
    service:
      endpoint: "GET /system/dept/treeselect"
      source: "复用 SysDeptService，RuoYi 自带"
      newApi: false
    notes: null

  customer_autocomplete:
    intent: "客户选择，按客户名称远程模糊搜索，数据量大"
    component:
      type: el-autocomplete
      props:
        fetchSuggestions: "后端远程搜索"
        valueKey: "customerName"
        debounce: 300
        triggerOnFocus: false
    bindTo: customer_id
    service:
      endpoint: "GET /project/customer/search?name={keyword}"
      source: "复用 PmCustomerService，需新增轻量搜索接口返回 id+name"
      returns: "[{ customerId, customerName }]"
      newApi: true
    notes: "提交值为 customerId，显示值为 customerName"

  manager_autocomplete:
    intent: "项目经理选择，按姓名搜索，数据量小前端过滤"
    component:
      type: el-autocomplete
      props:
        fetchSuggestions: "前端过滤"
        valueKey: "managerName"
    bindTo: project_manager
    service:
      endpoint: "GET /project/manager/listAll"
      source: "复用 PmManagerService，需新增返回全量 id+name 的接口"
      returns: "[{ managerId, managerName }]"
      newApi: true
    notes: "页面加载时一次性获取全量数据，前端 filter 匹配"
```
