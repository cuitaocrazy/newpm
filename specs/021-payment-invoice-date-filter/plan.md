# Implementation Plan: 付款里程碑「开票日期」区间查询

**特性目录**: `021-payment-invoice-date-filter` | **分支**: `feat/payment-invoice-date-filter` | **基线**: `fd3a35f` | **日期**: 2026-08-07
**Spec**: [spec.md](./spec.md) | **Issue**: #36

## Summary

一句话：**把「实际回款日期」已经跑通的那一整条散点，原样再铺一遍到 `submit_acceptance_date`（业务名「开票日期」）上——一处不多，一处不少。**

改动面：**后端 3 个文件 9 处、前端 1 个文件 7 处**。**零 DDL、零字典、零新接口、零新实体字段、零新依赖、零新组件。**

技术上没有新机制，全部风险来自「漏一处」与「多改一处」。四个必须提前知道的事实：

1. **列名 ≠ 业务名，且不改名。** 「开票日期」的物理列是 `pm_payment.submit_acceptance_date`（`00_tables_ddl.sql:714` 注释已是「开票日期」）。库里**不存在** `invoice_date`。
2. **必须改三处 `<where>`，不是一处。** 页面的三条数据出口（列表 / 底部合计 / 导出）落在**两条 SQL**上，再加扁平列表共三条。只改列表 → 「列表筛过了、底部合计还是全量」。
3. **两个 mapper 的日期写法不同，且都要保留。** `ContractMapper.xml` 裸列比较，`PaymentMapper.xml` `date_format` 包两侧。**沿用各自文件的既有风格**，不在任一文件内引入第二种。
4. **前端的区间是独立 `ref`，不在 `queryParams` 里。** `getList()` 每次从 `ref` 反向覆写起止值——搜索状态缓存**必须存这个 `ref`**，否则还原后第一次 `getList` 当场把条件抹成 `null`（`payment/index.vue:643-646` 现场注释 + `tests/e2e-sort-state-persist.spec.js:485-488` 断言双重记录）。

---

## Technical Context

| 项 | 值 |
|---|---|
| **语言/版本** | Java 17 / TypeScript 5.6 |
| **主要依赖** | Spring Boot 3.5.8、MyBatis、Vue 3.5、Element Plus 2.13（**均为既有，零新增**） |
| **存储** | MySQL 8.x (`ry-vue`)。涉及表：**仅 `pm_payment`（只读）**。**无 DDL 变更** |
| **涉及列** | `pm_payment.submit_acceptance_date`（`date`，可空，`00_tables_ddl.sql:714`）。**该列上无索引**（`pm_payment` 的 KEY 只有 `contract_id`/`payment_status`/`expected_quarter`/`actual_quarter`/`create_time`，`:723-727`）——与 `actual_payment_date` 同等待遇，表规模下不构成约束 |
| **参数承载** | `BaseEntity.params`（`ruoyi-common/.../BaseEntity.java:43`，`getParams()` 懒初始化 `:107-111`），**不新增实体字段** |
| **测试** | JUnit 5（`ruoyi-project`，MyBatis `XMLMapperBuilder` 渲染断言，不连库）、Playwright（根 `tests/`） |
| **构建校验** | `mvn clean compile -pl ruoyi-project -am`；`mvn test -pl ruoyi-project -am`；`cd ruoyi-ui && npx vue-tsc --noEmit`（存量基线约 39 错，判据是 `payment/index.vue` 零错误） |
| **目标平台** | 既有 K3s（namespace `newpm`），无变化 |

---

## Constitution Check

*GATE：设计前必须通过，设计后复查。依据 `.specify/memory/constitution.md`。*

| 原则 | 相关要求 | 本特性符合性 |
|---|---|---|
| **I. 业务完整性优先** | mutating 接口 MUST 加 `@Log` | ⚪ **不新增 mutating 接口**。`export` 的既有 `@Log(title="款项管理", businessType=EXPORT)`（`PaymentController.java:97`）不动 |
| | 软删是默认策略 | ⚪ 不适用（纯查询）。三条语句的 `p.del_flag = '0'` / `c.del_flag = '0'` 一行不动 |
| **II. 权限驱动访问控制** | Controller MUST 有 `@PreAuthorize` | ✅ 无新增 Controller 方法；三个既有方法的 `@PreAuthorize('project:payment:list' / ':export')`（`:58/:78/:96`）不动 |
| | 部门隔离用 `@DataScope` | ✅ **`${params.dataScope}` 位置与语义完全不动**（`ContractMapper.xml:422`、`PaymentMapper.xml:93`）。新条件**追加在它之前**，不挪动、不包裹、不加 `<if>` 条件绕过它 |
| **III. API 与代码一致性** | 前端响应取值 `res.rows` / `res.data` | ✅ 复用既有 `listPaymentWithContracts` / `sumPaymentAmount`（`api/project/payment.js`），取值方式不变 |
| | 参数命名一致 | ✅ `submitAcceptanceDateStart/End` 与实体字段 `submitAcceptanceDate` 同名根，与 `actualPaymentDateStart/End` 同构 |
| **IV. 任务与项目解耦** | 禁止向 `pm_project` 加任务字段 | ⚪ 不适用（只碰 `pm_payment` 的查询） |
| **V. 数据库规范** | 跨字符集 JOIN 加 `COLLATE` | ⚪ **不适用**：新条件是 `pm_payment` **单表单列**与字符串常量的比较，不引入任何 JOIN，既有 JOIN 的 `COLLATE`（`ContractMapper.xml:390-394`）不动 |
| | Schema 变更策略 | ✅ **无 schema 变更**（OUT-1） |
| **VI. 前端组件与字典规范** | 字典用 `<dict-select>` | ⚪ 不适用：日期区间是自由输入，非字典项。控件用 `el-date-picker`，与「实际回款日期」（`payment/index.vue:121-128`）逐属性同构 |

**结论**：全部通过，无 ⚠️、无需 Complexity Tracking。本特性是**纯对称补齐**，不引入任何新的设计张力。

---

## 一、前后端参数契约

| 参数名 | 方向 | 类型 | 必填 | 格式 | 前端来源 | 上行方式 | 后端接收 | 落到 SQL |
|---|---|---|---|---|---|---|---|---|
| `submitAcceptanceDateStart` | 前 → 后 | `string` | **否** | `YYYY-MM-DD` | `submitAcceptanceDateRange.value[0]`（未选/清空时为 `null`） | 列表/合计：GET query；导出：POST form-urlencoded（`tansParams`） | `@RequestParam(required=false) String`，塞进 `contract.getParams()` | `params.submitAcceptanceDateStart` → `submit_acceptance_date >= ?` |
| `submitAcceptanceDateEnd` | 前 → 后 | `string` | **否** | `YYYY-MM-DD` | `submitAcceptanceDateRange.value[1]` | 同上 | 同上 | `params.submitAcceptanceDateEnd` → `submit_acceptance_date <= ?` |

**契约细则（每一条都对应一个易错点）**

1. **参数名不得改动。** `submitAcceptanceDateStart` / `submitAcceptanceDateEnd` 是 Issue #36 定死的前后端契约，含大小写。改名会让前端筛选静默失效（后端拿不到 key → `<if>` 不成立 → 无条件全量返回，**不报错**）。
2. **`null` 不上行。** `tansParams`（`ruoyi-ui/src/utils/ruoyi.ts`）对 `null`/`undefined` 一视同仁地不拼进 query（`payment/index.vue:610-612` 现场注释已实证），因此「未填」天然表现为「参数缺失」，后端 `<if>` 的 `!= null` 分支不成立 → FR-013 自动成立。
3. **`""` 也必须被挡住。** 三处 `<if>` MUST 同时写 `!= null and != ''`，与既有 `actualPaymentDate` 完全一致 —— 空串若漏挡会渲染成 `>= ''`，MySQL 隐式转换后把**所有非空日期**都判为真，等于静默改变结果集。
4. **后端不做日期类型转换。** 字符串原样入 `params`，由 MySQL 做隐式转换（与 `actualPaymentDate` 现状一致）。这也是为什么 `PaymentMapper` 侧要 `date_format(#{...},'%Y-%m-%d')` 包一层——保持该文件既有风格。
5. **导出走 POST form 而非 GET。** `proxy.download`（`ruoyi-ui/src/utils/request.ts:127-133`）是 `service.post` + `transformRequest: tansParams` + `Content-Type: application/x-www-form-urlencoded`，`@RequestParam` 从 form body 取值。**前端导出无需任何额外改动**——`handleExport`（`payment/index.vue:822-827`）是 `{ ...queryParams.value }` 整体展开，起止值进了 `queryParams` 就自动随导出上行。

---

## 二、改动清单（精确到 file:line）

> 行号均为基线 `fd3a35f` 实测值。**按下表从后往前改**（大行号先改），可避免前面的插入把后面的行号顶偏。

### 2.1 后端：`PaymentController.java`（3 处，各 2 个参数 + 2 个 put）

文件：`ruoyi-project/src/main/java/com/ruoyi/project/controller/PaymentController.java`

| # | 方法 | 现状行号 | 改动 |
|---|---|---|---|
| **B1** | `listWithContracts`（`:60-73`） | 形参 `:61-62`；put 块 `:64-69` | 形参列表在 `:62` 之后追加两个 `@RequestParam(required = false) String submitAcceptanceDateStart, submitAcceptanceDateEnd`；`:69` 之后追加两个 `if (xxx != null) contract.getParams().put("xxx", xxx);` —— **必须在 `startPage()`（`:70`）之前** |
| **B2** | `sumPaymentAmount`（`:80-91`） | 形参 `:81-82`；put 块 `:84-89` | 同 B1（`:82` 之后 / `:89` 之后，`:90` 的 `return` 之前） |
| **B3** | `export`（`:99-131`） | 形参 `:100-101`；put 块 `:103-108` | 同 B1（`:101` 之后 / `:108` 之后，`:110` 的全量查询之前） |

改动形状（逐字对齐既有写法，`listWithContracts` 为例）：

```java
public TableDataInfo listWithContracts(Contract contract,
        @RequestParam(required = false) String actualPaymentDateStart,
        @RequestParam(required = false) String actualPaymentDateEnd,
        @RequestParam(required = false) String submitAcceptanceDateStart,   // 新增
        @RequestParam(required = false) String submitAcceptanceDateEnd)     // 新增
{
    if (actualPaymentDateStart != null) { contract.getParams().put("actualPaymentDateStart", actualPaymentDateStart); }
    if (actualPaymentDateEnd != null)   { contract.getParams().put("actualPaymentDateEnd", actualPaymentDateEnd); }
    // 新增：开票日期(pm_payment.submit_acceptance_date，列名为历史命名，业务含义是开票日期)
    if (submitAcceptanceDateStart != null) { contract.getParams().put("submitAcceptanceDateStart", submitAcceptanceDateStart); }
    if (submitAcceptanceDateEnd != null)   { contract.getParams().put("submitAcceptanceDateEnd", submitAcceptanceDateEnd); }
    startPage();
    ...
}
```

> **`if (xxx != null)` 的守卫要照抄，不要「优化」成无条件 put。** 无条件 put 会把 `null` 写进 `params` map，`<if test="... != null">` 仍然不成立，行为上等价——但会让 `params` 里多出两个 `null` 值的 key，影响后续任何对 `params` 做遍历/序列化的逻辑。保持与既有三处逐字一致。

### 2.2 后端：`ContractMapper.xml`（2 处，裸列写法）

文件：`ruoyi-project/src/main/resources/mapper/project/ContractMapper.xml`

| # | 语句 | 插入位置 | 改动 |
|---|---|---|---|
| **B4** | `selectContractWithPaymentsList`（`:355-425`） | `:421` 之后、`:422` 的 `${params.dataScope}` **之前** | 追加两行 `<if>` |
| **B5** | `sumPaymentAmount`（`:428-461`） | `:459` 之后、`:460` 的 `</where>` 之前 | 追加同样两行 |

```xml
<if test="params.submitAcceptanceDateStart != null and params.submitAcceptanceDateStart != ''"> and p.submit_acceptance_date &gt;= #{params.submitAcceptanceDateStart}</if>
<if test="params.submitAcceptanceDateEnd != null and params.submitAcceptanceDateEnd != ''"> and p.submit_acceptance_date &lt;= #{params.submitAcceptanceDateEnd}</if>
```

- **写法必须是裸列比较**，与同文件 `:420-421` / `:458-459` 逐字同风格（INV-2）。**不要**在本文件里写 `date_format`。
- `&gt;` / `&lt;` 是 XML 转义，不能写裸 `>` `<`。
- B4 与 B5 的两行**必须逐字相同**（INV-3）：这两条语句的 `<where>` 现在就是互为镜像的，任何不对称都是 FR-005 失守的前兆。

### 2.3 后端：`PaymentMapper.xml`（1 处，`date_format` 写法）

文件：`ruoyi-project/src/main/resources/mapper/project/PaymentMapper.xml`

| # | 语句 | 插入位置 | 改动 |
|---|---|---|---|
| **B6** | `selectPaymentList`（`:46-96`） | `:92` 的 `</if>` 之后、`:93` 的 `${params.dataScope}` **之前** | 追加两块 `<if>` |

```xml
<if test="params.submitAcceptanceDateStart != null and params.submitAcceptanceDateStart != ''">
    and date_format(p.submit_acceptance_date,'%Y-%m-%d') &gt;= date_format(#{params.submitAcceptanceDateStart},'%Y-%m-%d')
</if>
<if test="params.submitAcceptanceDateEnd != null and params.submitAcceptanceDateEnd != ''">
    and date_format(p.submit_acceptance_date,'%Y-%m-%d') &lt;= date_format(#{params.submitAcceptanceDateEnd},'%Y-%m-%d')
</if>
```

- **写法必须是 `date_format` 包两侧**，与同文件 `:87-92` 逐字同风格（INV-2）。
- 该语句服务 `GET /project/payment/list`，**Controller 侧无 `@RequestParam` plumbing**（`PaymentController.java:48-53` 只绑 `Payment payment`）——条件走 `params[submitAcceptanceDateStart]=...` 的 Map 绑定，与该语句现有 `actualPaymentDate` 的形态完全相同。**这是 FR-007 的既定形态，不要顺手给 `/list` 加 `@RequestParam`**（那会改变一个本特性范围外的接口签名）。

### 2.4 后端「禁止改动」清单

| 位置 | 为什么禁止 |
|---|---|
| `ContractMapper.xml:420-421`、`:458-459`、`PaymentMapper.xml:87-92` | 「实际回款日期」既有条件，INV-1。`e2e-sort-state-persist.spec.js:457/485` 直接断言它 |
| `ContractMapper.xml:422`、`PaymentMapper.xml:93` 的 `${params.dataScope}` | 数据权限边界，INV-4。新条件只能**追加在它之前** |
| `PaymentMapper.xml:172` 的 `submit_acceptance_date = #{submitAcceptanceDate},` | 这是 Issue #7/#10 治理后的**正确形态（无 `<if>` 守卫）**。本特性不碰 update；看到「没有守卫」不要以为是遗漏而去补 —— 补上就是把已修复的缺陷改回去（INV-5） |
| `ContractMapper.xml:424` 的 `order by` | INV-6 |
| `Payment.java` / `Contract.java` | 不新增字段（OUT-7）；`Payment.java:66` 的过期注释本次也不改（OUT-5） |
| `pm-sql/init/00_tables_ddl.sql` | 零 DDL（OUT-1） |

### 2.5 前端：`payment/index.vue`（7 处）

文件：`ruoyi-ui/src/views/project/payment/index.vue`

| # | 现状行号 | 改动 | 对应参照 |
|---|---|---|---|
| **F1** | `:129` 之后（`:130` 的 `</template>` 之前） | 新增 `el-form-item label="开票日期" prop="submitAcceptanceDateRange"` + `el-date-picker type="daterange"` `value-format="YYYY-MM-DD"` `range-separator="-"` `start-placeholder="开始日期"` `end-placeholder="结束日期"` `style="width: 240px"` | `:120-129` |
| **F2** | `:381` 之后 | `const submitAcceptanceDateRange = ref([])` | `:381` |
| **F3** | `:432` 之后 | `queryParams` 里加 `submitAcceptanceDateStart: null,` `submitAcceptanceDateEnd: null,` | `:431-432` |
| **F4** | `:449` 之后（仍在 `getList()` 内、`listPaymentWithContracts` 调用之前） | 镜像一段 `if (range && range.length === 2) {...} else { 两个都置 null }` | `:442-449` |
| **F5** | `:646` | `saveSearchState` 的 `state` 对象里加 `submitAcceptanceDateRange: submitAcceptanceDateRange.value` | `:646` |
| **F6** | `:668` | `restoreSearchState` 里加 `submitAcceptanceDateRange.value = state.submitAcceptanceDateRange \|\| []` | `:668` |
| **F7** | `:686` 之后 | `resetQuery` 里加 `submitAcceptanceDateRange.value = []` | `:686` |

**放置位置的决定：新控件放在「实际回款日期」之后（F1 追加在 `:129` 后），不放在它之前。**

- 理由 ①：追加式编辑，diff 只有新增行、不移动任何既有行，回归面最小；
- 理由 ②：两个日期区间控件相邻，视觉上自成一组；
- 理由 ③：「查询区顺序应与表格列顺序一致」不是本页现状——「里程碑确认年份」在「更多」区排第一（`:82`），在 `columns` 里却排倒数第四（`:408`）。因此不存在被破坏的既有约定。
- 影响评估：`e2e-sort-state-persist.spec.js:135-136` 的 `formItem` 是 `page.locator('.el-form-item').filter({ hasText: label }).first()`，**按 label 文本定位、与顺序无关**，前后放置都不影响既有用例。

**F4 的具体形状**（与 `:442-449` 逐段对称）：

```js
// 处理日期范围
if (actualPaymentDateRange.value && actualPaymentDateRange.value.length === 2) { ... } else { ... }   // 既有，不动

// 开票日期区间（pm_payment.submit_acceptance_date）
if (submitAcceptanceDateRange.value && submitAcceptanceDateRange.value.length === 2) {
  queryParams.value.submitAcceptanceDateStart = submitAcceptanceDateRange.value[0]
  queryParams.value.submitAcceptanceDateEnd = submitAcceptanceDateRange.value[1]
} else {
  queryParams.value.submitAcceptanceDateStart = null
  queryParams.value.submitAcceptanceDateEnd = null
}
```

> `else` 分支的两个 `null` **不能省**。省掉的话，用户清空控件后 `queryParams` 里仍留着上一次的值，表现为「界面上条件已清空、结果却还被筛着」。既有代码正是这样写的（`:446-449`），照抄即可。

### 2.6 前端「零改动」清单

| 位置 | 为什么零改动 |
|---|---|
| `handleExport`（`:822-827`） | `{ ...queryParams.value }` 整体展开，F3 一落地导出就自动带上（见 §一 细则 5） |
| `api/project/payment.js` | `listPaymentWithContracts` / `sumPaymentAmount` 都是 `params: query` 透传，签名已够用 |
| `columns` 配置（`:404`）与表格列（`:254-256`） | 「开票日期」列早已存在，本特性只加筛选不加列 |
| `handleQuery` / `handleSortChange` / `onMounted` | 时序约束（`:833-840`）不变：`restoreSearchState()` 仍在 `getList()` 之前 |

---

## 三、关键决策

### D1：为什么必须改三处 `<where>`，而不是「改一处 SQL 就够了」

页面的三条数据出口落在两条 SQL 上，再加扁平列表共三条：

| 出口 | 前端调用 | Controller | SQL 语句 |
|---|---|---|---|
| 列表 | `listPaymentWithContracts`（`:452`） | `listWithContracts`（`:60`） | `ContractMapper.selectContractWithPaymentsList`（`:355`） |
| **底部合计** | `sumPaymentAmount`（`:463`） | `sumPaymentAmount`（`:80`） | **`ContractMapper.sumPaymentAmount`（`:428`）—— 独立的第二条 SQL** |
| 导出 | `proxy.download`（`:824`） | `export`（`:99`） | 复用 `selectContractWithPaymentsList`（`:110`，不分页） |
| （口径一致）扁平列表 | — | `list`（`:48`） | `PaymentMapper.selectPaymentList`（`:46`） |

**合计金额走的是一条完全独立的 SQL**，它自带一份**逐字复制**的 `<where>`（`:433-459` 与 `:394-421` 互为镜像）。只改列表不改它 → 用户筛出 3 条记录，底部却显示全库合计。这是本特性**最容易漏且最难自查**的一处：列表页面看起来完全正常，只有把合计数字和列表金额加总比对才发现对不上。**SC-004 就是专门守它的。**

导出复用列表 SQL，因此 SQL 侧不需要第三份 `<where>`——但 **Controller 侧的 `export` 方法必须单独加参数**（B3），它有自己的一套 `@RequestParam`。

### D2：为什么两个 mapper 保持两种日期写法

| | `ContractMapper.xml` | `PaymentMapper.xml` |
|---|---|---|
| 既有写法 | `p.actual_payment_date >= #{...}`（裸列） | `date_format(p.actual_payment_date,'%Y-%m-%d') >= date_format(#{...},'%Y-%m-%d')` |
| 本次新增 | 裸列（B4/B5） | `date_format`（B6） |

两列都是 MySQL `date` 类型（`00_tables_ddl.sql:714/715`，**无时分秒**），因此两种写法**语义等价**——`date_format` 那层在 `date` 列上是恒等变换。风格分化是历史遗留。

**为什么不趁机统一**：统一是一次跨语句的行为改动，需要回归全部日期类筛选（合同签订日期、投产日期、项目周期……），收益是「代码好看」，风险是「改坏一个说不清的边界」。本特性是对称补齐，**范围内不做重构**（OUT-6）。

**为什么必须同文件内统一**：同一个 `<where>` 里「一半裸列、一半 `date_format`」是缺陷的化石记录（CLAUDE.md 已把「同一 update 里一半有守卫一半没有」列为必查信号，同理适用）。后来者会以为差异有意义而去猜原因。INV-2 就是这条。

### D3：为什么不新增实体字段，而用 `params`

`Contract` / `Payment` 都继承 `BaseEntity`，其 `params`（`BaseEntity.java:43`）是 RuoYi 承载「非持久化查询条件」的既定位置。日期区间不是实体属性（一条付款里程碑没有「开票日期开始」这个属性），放进实体会：

- 让 `resultMap`、`insert`、`update` 面临「要不要映射它」的无谓问题；
- 与「实际回款日期」的既有形态分裂，破坏对称。

`Payment` 上确实有几个非持久化的多值筛选字段（`deptIds` / `paymentStatuses` / `expectedQuarters` / `actualQuarters`，CLAUDE.md 记录），但那是**数组型**参数、必须走 `<foreach>`、无法用 `params` 的 Map 表达；日期区间是标量，没有这个约束。**跟随 `actualPaymentDate` 的既有选择。**

### D4：`${params.dataScope}` 必须留在最后

三条语句的 `<where>` 末尾都是 `${params.dataScope}`（`ContractMapper.xml:422`、`PaymentMapper.xml:93`；`sumPaymentAmount` 无此片段）。新条件一律**追加在它之前**。

理由不是风格：`${}` 是**字符串拼接**（非 `#{}` 预编译），由 `DataScopeAspect` 注入形如 ` AND (d.dept_id IN (...))` 的片段。把新条件插到它之后，会让 `<where>` 的 `TrimSqlNode` 剥前导 `AND` 的行为落在错误的片段上，在「dataScope 为空串」的场景下可能拼出语法错误的 SQL。**位置是正确性问题，不是审美问题。** Issue #24 的教训（数据权限片段被条件性跳过 → 全库越权读）已经用 `DailyReportProjectScopeSqlTest` 锁死了同类不变式。

### D5：空值行为的三条 SQL 语义（不写额外代码）

| 场景 | SQL 表现 | 结果 | 对应 FR |
|---|---|---|---|
| 两端都不传 | 两个 `<if>` 都不成立，不追加任何条件 | 与上线前逐条一致 | FR-013 |
| `submit_acceptance_date IS NULL` 的行，任一端有值 | `NULL >= '2026-01-01'` → `UNKNOWN` → `WHERE` 不通过 | 该行不出现 | FR-012 |
| `start > end` | `col >= A AND col <= B` 且 `A > B` → 恒假 | 空结果；`sumPaymentAmount` 因 `COALESCE(sum(...), 0)`（`ContractMapper.xml:429`）返回 `0` | FR-011 |

**三条都是 SQL 的天然语义，不需要写任何额外判断。** 不要「贴心地」加 `IS NOT NULL` 或在 Java 里校验 `start <= end`——前者改变语义（会让 `<if>` 不成立时也过滤掉空值行），后者把一个静默正确的行为变成一个要维护的错误分支。

---

## 四、测试策略

### 4.1 TDD（后端单测，先红后绿）

**判据：必须先看到失败输出再写实现。** 基线先跑一次 `mvn test -pl ruoyi-project -am` 记录当前通过数，避免把既有红灯误记成本次引入。

**载体**：新增 `ruoyi-project/src/test/java/com/ruoyi/project/mapper/PaymentInvoiceDateFilterSqlTest.java`，照抄 `ruoyi-project/src/test/java/com/ruoyi/project/mapper/DailyReportProjectScopeSqlTest.java`（同仓库既有范式）的做法：用 MyBatis `XMLMapperBuilder` 解析 mapper XML，取 `MappedStatement#getBoundSql(param).getSql()` 断言**渲染出的 SQL 文本**。

> **不连库、不起 Spring、毫秒级。** 这是本特性唯一能做出真「红→绿」的单测层——特性本体就是三段 `<if>`，行为完全体现在 SQL 渲染上。

必须照搬的四个坑（前三个已由参照测试的注释记录，第四个是本特性特有）：

1. `configuration.getTypeAliasRegistry().registerAliases("com.ruoyi.project.domain")` —— 否则 `parameterType="Contract"` / `"Payment"` 报 ClassNotFound。
2. `XMLMapperBuilder` 必须用 **4 参构造**并传 `configuration.getSqlFragments()`，否则 `<include refid>` 解析不动。
3. `params` 里若没有 `dataScope` 这个 key，`${params.dataScope}` 渲染成空串 —— 断言「哨兵仍在」时必须显式放哨兵值，否则测的是别的东西。
4. **⚠️ 本特性特有的假绿陷阱：`submit_acceptance_date` 本来就出现在 select 字段列表里**（`ContractMapper.xml:376`、`PaymentMapper.xml:50`）。因此 `assertTrue(sql.contains("submit_acceptance_date"))` **恒真**，是一条永远绿的废断言。断言必须落在**比较运算的完整形状**上，且先把 SQL 的空白折叠：

   ```java
   /** 渲染出的 SQL 保留 XML 里的换行与缩进，直接 contains 会因空白差异假红 */
   private static String norm(String sql) { return sql.replaceAll("\\s+", " "); }

   // ContractMapper 侧（裸列写法）
   assertTrue(norm(sql).contains("p.submit_acceptance_date >= ?"));
   // PaymentMapper 侧（date_format 写法）
   assertTrue(norm(sql).contains(
       "date_format(p.submit_acceptance_date,'%Y-%m-%d') >= date_format(?,'%Y-%m-%d')"));
   ```

> ⚠️ **回填校正（2026-08-07，实现后）**：下表的 `T1`~`T8` 是**设计编号，实现里没有采用**。
> 实际落地的 `PaymentInvoiceDateFilterSqlTest` 有 **13 个方法**，按不变式命名
> （`inv1_`~`inv7_` + `trapRegistry_`）；「T8 = 对三条语句各跑一遍」不是独立编号，
> 而是在每个方法内部对三条语句循环断言。换算表见
> [`bdd/coverage.md` §二 / §七 D-1](./bdd/coverage.md)。

| 循环 | 红点（断言） | 覆盖 |
|---|---|---|
| **T1** | 三条语句在 `params` 两端都不传时，渲染 SQL **不含** `submit_acceptance_date >=` / `<=` 片段 | FR-013 |
| **T2** | 只传 `start` → 含 `>=` 片段、**不含** `<=` 片段 | FR-008 |
| **T3** | 只传 `end` → 含 `<=` 片段、**不含** `>=` 片段 | FR-009 |
| **T4** | 两端都传 → 两个片段**都在** | FR-003 |
| **T5** | 传空串 `""` → **不含**任何片段（`!= ''` 分支生效） | §一 细则 3 |
| **T6** | 放入 dataScope 哨兵后：哨兵仍在渲染结果中，且新片段的下标 **小于** 哨兵下标 | INV-4 / D4 |
| **T7** | 同时传 `actualPaymentDate*` 与 `submitAcceptanceDate*` → **四个**片段都在，互不吞没 | FR-014 / INV-1 |
| **T8** | T1~T7 对 `selectContractWithPaymentsList` / `sumPaymentAmount` / `selectPaymentList` **三条语句各跑一遍**（`@ParameterizedTest` 或三份 `@Nested`），且 `ContractMapper` 两条断言裸列形状、`PaymentMapper` 断言 `date_format` 形状 | INV-2 / INV-3 / D1 |

**可选（推荐但非门槛）**：`PaymentControllerTest` —— Mockito mock `IContractService`，用 `ArgumentCaptor<Contract>` 断言三个方法各自把两个参数塞进了 `getParams()`，且传 `null` 时**不放 key**。它守的是 B1~B3 的 plumbing；若不写，该层由 E2E 的 SC-002/003 兜住。

> ⚠️ **回填校正**：这一层**已写且不是 4 例，是 15 例**（三个出口 × 写入 / null 不写 / 空串写入 /
> 单端 / 双区间共存 / 既有 `actualPaymentDate` 行为不变）。另注意 Controller 的判据是
> `if (x != null)`，所以**空串会被放进 `params`** —— 真正挡住空串的是 mapper `<if>` 的 `!= ''`，
> 两层职责不要合并理解。

### 4.2 BDD

产出 `specs/021-payment-invoice-date-filter/bdd/payment-invoice-date-filter.feature`（中文 Gherkin，**评审用，不直接执行**——本仓库无 Cucumber 工具链）+ `bdd/coverage.md`（场景 → JUnit/Playwright 用例映射矩阵），格式照 `specs/017-contract-code-sort/bdd/`。

**BDD 测的是「业务能不能说清」，不是「代码对不对」** —— 场景必须用财务/合同管理岗的语言写，出现的名词只能是「开票日期」，不能是 `submit_acceptance_date`。

核心场景（每条都对应 §五 的一个 SC）：

| 场景 | 判据 |
|---|---|
| 财务按月核对开票台账 | 给定开票日期区间为某月首末日，列表只出现该月开出的里程碑 |
| **合计金额与列表口径一致** | 底部合计 == 列表全量行金额之和（**本特性的核心业务判据**） |
| 导出与列表看到同一批数据 | 导出行数 == 列表全量行数 |
| 只填开始日期 / 只填结束日期 | 单边界生效，另一边不限 |
| 起止同一天 | 精确筛出当天开票的记录 |
| 尚未开票的里程碑不出现在开票期间结果里 | 空值行被排除 |
| 不填开票日期时结果与从前一致 | 无条件回归 |
| 开票期间 + 回款期间同时使用 | 两个区间 AND 生效，互不干扰 |
| 离开列表再回来，筛选条件还在 | 状态缓存 |

### 4.3 E2E（Playwright）

**新增** `tests/e2e-payment-invoice-date-filter.spec.js`。

**API 段（主体）** —— 用 `tests/helpers/api-client.js` 的 `setupApi()`，`afterAll` 里 `await api.dispose()`。

> **硬要求：数据驱动，不硬编码任何日期。** 照 `tests/payment-status-filter-regression.spec.js` 的既有风格——先**探测**库里真实的开票日期分布（`GET /project/payment/list?pageNum=1&pageSize=10000`，收集非空 `submitAcceptanceDate`，算出 min / max / 出现次数最多的那个 D），再据此构造区间。硬编码日期在数据漂移后必红，且红得毫无信息量。
>
> **数据前提不满足时必须显式 `skip` 并打印原因，不得静默通过。** 例如 SC-011 需要库里存在 `submitAcceptanceDate` 为 null 的里程碑——探测不到就 skip 并打印「本库无空开票日期数据，SC-011 未验证」。**「跳过」要报成跳过，不能报成通过。**
>
> ⚠️ **回填校正（2026-08-07，实现 + 实测后）**：上面这段「探测式 + 探测不到就 skip」的设计**已被实现替换**，
> 改为 `beforeAll` **自造确定数据集**（1 个唯一标记合同 + 6 条里程碑，日期硬编码
> `2026-03-01` / `03-15` / `03-31` / `04-01` + 一条 NULL，金额取 2 的幂），`afterAll` 逐条清理。
> 理由与收益（精确集合断言、合计可反推命中集合、NULL 行不再依赖运气、跨环境可重复）见
> [`bdd/coverage.md` §七 D-2](./bdd/coverage.md)——**那一节是本段的替代口径，以它为准。**
>
> **因此 SC-011 不再需要 skip**，并额外加了一条反证用例（不带开票条件时该 NULL 行必须可见），
> 防止「造数没造出来 → 断言假绿」。
>
> ✅ **实测（2026-08-07）**：全套件 `28 passed (42.4s)`，**零 skip** ——
> 代码里仅存的两条 `test.skip`（`sys_fkzt` 字典取不到值 / 自造合同无 `deptId`）**均未触发**，
> 两条用例真实执行并通过（111ms / 88ms）。
> **但「跳过要报成跳过」这条底线不变**：skip 分支仍在代码里，换个环境依然可能触发，
> 判据是 `--reporter=list` 里该行是 `✓` 而非 `-`。

| 用例 | 判据 | SC |
|---|---|---|
| 列表逐行落在区间内 | 对 `listWithContracts` 返回的每个 `paymentList` 项断言 `submitAcceptanceDate` ∈ `[start, end]` | SC-003 |
| **合计 == 列表全量之和** | 同一组条件下拉全量列表（大 `pageSize`）求和，与 `sumPaymentAmount` 的 `res.data` 比对（`toBeCloseTo`，两位小数） | **SC-004** |
| 导出行数 == 列表行数 | `POST /project/payment/export` 返回 200 且非空；能解析行数则断言相等，否则记录降级理由 | SC-005 |
| 只填开始 / 只填结束 | 除边界断言外，还要断言「存在至少一条严格越过另一端的行」——证明另一端**确实没被施加** | SC-006 / SC-007 |
| 起止相等 | 取探测到的真实日期 D，结果非空且每行 == D | SC-008 |
| 含端点 | 以 min / max 为端点，这两条边界记录必须出现 | SC-009 |
| 起 > 止 | `code=200`、`rows` 为空、合计为 0，无异常 | SC-010 |
| 空值行不出现 | 先探测确认存在空开票日期的里程碑，再断言其不在结果中 | SC-011 |
| 两端为空 | 带空参数 vs 完全不带参数，`total` 与首页行集合逐条一致 | SC-012 |
| 组合 | 开票区间 + 回款区间同时给，结果同时满足两者 | SC-013 |

**UI 段** —— 照 `tests/e2e-sort-state-persist.spec.js:407-495` 的既有写法（`formItem` / `trackRequests` / `paramOf` / `leaveCloseTagAndReturn` 都是现成工具，`PAYMENT` 常量 `:40-45` 已含 `listUrl` 与 `storageKey`）：

| 用例 | 判据 | SC |
|---|---|---|
| 控件可见且形态一致 | 点「更多」后 `formItem(page, '开票日期')` 可见，含两个 `.el-range-input` | SC-001 |
| 参数上行 | 填 `2000-01-01` / `2099-12-31` 后点查询，`paramOf(listReqs.last(), 'submitAcceptanceDateStart')` === `'2000-01-01'` | SC-002 |
| **状态缓存** | 离开列表再返回后：① 控件值仍在（`toHaveValue`）② **重建后的请求仍带这两个参数** —— ②才是真判据，只断言①会漏掉「ref 没存 → getList 抹成 null」这条主缺陷 | **SC-014** |
| 重置 | 点「重置」后控件为空，且随后的列表请求 `paramOf(...)` 为 `null` | SC-015 |

**必须保持绿的回归套件**（每一条都是某个设计约束的探针）：

| 套件 | 守的是什么 |
|---|---|
| `tests/e2e-sort-state-persist.spec.js` | INV-1：「实际回款日期」的参数、缓存、排序一行没动（`:457/:485-488` 直接断言其值） |
| `tests/payment-status-filter-regression.spec.js` | INV-7：付款状态多选筛选未被新条件干扰 |
| `tests/e2e-payment-crud.spec.js` | 付款里程碑 CRUD 主流程 |
| `tests/payment-clear-field-regression.spec.js` | INV-5：`PaymentMapper` 的 update 语句没被顺手动过（清空语义仍成立） |
| `tests/clear-field-guards-regression.spec.js`（付款段） | 同上，跨模块版本 |

### 4.4 静态检查

- `mvn clean compile -pl ruoyi-project -am`
- `mvn test -pl ruoyi-project -am` —— 判据：**不因本次改动新增红灯**（与基线数比对）
- `cd ruoyi-ui && npx vue-tsc --noEmit` —— 判据：**`payment/index.vue` 零错误**（全局存量基线约 39 错不作门槛）

### 4.5 验收对照

spec §六 的 SC-001 ~ SC-020 逐条勾稽。SC-020（三处写法同风格 + 位于 dataScope 之前）由 T6/T8 单测 + diff 自检双重覆盖。

> ✅ **实测结果（2026-08-07，本地 worktree 环境）**：
>
> | 层 | 命令 | 结果行 |
> |---|---|---|
> | 后端全模块单测 | `mvn test -pl ruoyi-project -am` | `Tests run: 297, Failures: 0, Errors: 0, Skipped: 0` |
> | 本特性 e2e | `E2E_BASE_URL=http://localhost:5174 npx playwright test tests/e2e-payment-invoice-date-filter.spec.js --reporter=list` | `28 passed (42.4s)`（零 skip） |
> | 既有回归（5 文件） | 同上 `--reporter=list` | `72 passed (1.6m)` |
>
> SC-001 ~ SC-020 逐条通过；**三条降级 / 限定**（SC-005 判据降级、SC-004 仅 admin 成立、
> SC-008/009/011 改自造数据集）见 [spec.md §六 的实测结论块](./spec.md) 与
> [`bdd/coverage.md` §五 / §七](./bdd/coverage.md)。
> 完整执行记录（环境 / 逐例耗时 / 数据库取证 SQL）见 [`bdd/coverage.md` §八](./bdd/coverage.md)。
>
> ⚠️ **`T1`~`T8` 这套编号只存在于本 plan**；实现里的方法名是 `inv1_`~`inv7_` + `trapRegistry_`（13 例），
> 换算关系见 `bdd/coverage.md` §七 D-1。

---

## 五、风险与规避

| ID | 风险 | 证据 | 规避 |
|---|---|---|---|
| **R-001** | **搜索状态缓存漏存 ref** → 从详情返回列表后，控件里看着有值、结果却是全量（或反过来）。表现极具迷惑性：界面显示条件、请求里却没有参数 | `payment/index.vue:442-449` 的 `getList()` **每次**从 ref 反向覆写起止值，ref 为空就写 `null`；`:643-646` 现场注释与 `tests/e2e-sort-state-persist.spec.js:485-488` 的断言已把该机制写死 | F5/F6 列为独立改动项；**SC-014 的判据必须是「重建后的请求仍带参数」而不只是「控件里有值」** —— 只断言控件值会漏掉这条主缺陷 |
| **R-002** | **合计金额那处 SQL 漏改** → 「列表筛过了、底部合计还是全量」。列表页面看起来完全正常，不比对数字发现不了 | `sumPaymentAmount` 是 `ContractMapper.xml:428-461` 一条**完全独立**的 SQL，自带一份逐字复制的 `<where>` | B5 单列一行；T8 强制三条语句各跑一遍；**SC-004 用「合计 == 列表全量之和」直接勾稽**，这是唯一能发现该缺陷的判据 |
| **R-003** | **日期写法风格串台** —— 在 `ContractMapper` 里写了 `date_format`，或在 `PaymentMapper` 里写了裸列 | 两文件既有风格实测不同（`ContractMapper.xml:420` vs `PaymentMapper.xml:88`） | INV-2 + D2 明确「沿用各自文件风格」；T8 的断言**按文件分别断言形状**（裸列 / `date_format`），写串了直接红 |
| **R-004** | **新条件插到 `${params.dataScope}` 之后** → `<where>` 的 `TrimSqlNode` 剥前导 `AND` 的行为落在错误片段上，dataScope 为空时可能拼出语法错误 SQL | Issue #24 的同类教训已由 `DailyReportProjectScopeSqlTest` 锁死 | D4 明确位置；**T6 断言「新片段下标 < 哨兵下标」**，插错位置直接红 |
| **R-005** | **只改列表 SQL，忘了 Controller 的 `export` 方法** → 导出仍是全量。导出是异步下载，用户不打开 Excel 发现不了 | `export`（`:99-131`）有自己独立的三对 `@RequestParam` | B3 单列一行；SC-005 直接断言导出行数 |
| **R-006** | **`<if>` 漏写 `!= ''`** → 空串渲染成 `>= ''`，MySQL 隐式转换后把所有非空日期判为真，**静默改变结果集且不报错** | 既有三处均写了 `!= null and != ''` | §一 细则 3；**T5 专门守它** |
| **R-007** | 「顺手」把 `PaymentMapper.xml:172` 的 `submit_acceptance_date = #{...}` 加回 `<if>` 守卫（看到「别的字段有守卫、它没有」而误以为是遗漏） | 该字段**无守卫是正确形态**，是 Issue #7/#10 治理的结果；CLAUDE.md 把该缺陷记为「已复发四次」 | INV-5 + §2.4 显式标「禁止改动」；`payment-clear-field-regression.spec.js` 是现成防线 |
| **R-008** | 「顺手」把两个 mapper 的日期写法统一了 | 这是本次最诱人的「改进」方向 | OUT-6 + INV-2；diff 自检：本次 diff 内**不得出现对既有 `actualPaymentDate` 行的任何修改** |
| **R-009** | 单测写成 `assertTrue(sql.contains("submit_acceptance_date"))` → **恒绿废断言**，因为该列本来就在 select 字段列表里 | `ContractMapper.xml:376`、`PaymentMapper.xml:50` | §4.1 坑 4：断言必须落在完整比较形状上，并先 `norm()` 折叠空白；**TDD 的「先红」这一步就能暴露废断言——写完断言先跑一次，如果一开始就是绿的，说明断言无效** |
| **R-010** | E2E 硬编码日期 → 数据漂移后必红，且红得没有信息量 | 合同/付款类 E2E 的历史红灯多源于抽样既有数据 | §4.3 硬要求：先探测真实分布再构造区间；数据前提不满足显式 `skip` 并打印原因 |
| **R-011** | 前端 `else` 分支漏写两个 `null` → 用户清空控件后条件仍生效 | 既有代码 `:446-449` 正是靠 `else` 置 `null` 兜住 | §2.5 F4 给出完整形状；SC-015 守它 |
| **R-012** | 改动落到主工作区而非 worktree（主工作区有 96 个未提交文件） | — | 全程用绝对路径 `.claude/worktrees/payment-invoice-date-filter/...`；提交前 `git status` 确认；**禁止 `git add -A`**，只 add 本特性 4 个源文件 + specs/tests |
| **R-013** | 纯文档提交也会触发约 6 分钟的生产空转部署 | CI `paths-ignore` 的 `'*.md'` 只匹配仓库根目录（glob 的 `*` 不跨 `/`），改 `specs/**` 照常触发 | 已知，接受；本特性最终会带代码一起提交 |

---

## 六、回滚方案

| 层 | 回滚动作 | 影响 |
|---|---|---|
| **全部** | `git revert` 本特性提交 → push `main` → 自动构建部署（约 6 分钟） | 查询条件消失，回到现状 |
| **数据库** | **无需回滚** —— 零 DDL 变更（OUT-1） | — |
| **数据** | **无需回滚** —— 纯查询特性，不写任何数据 | — |

**这是本仓库回滚成本最低的一类特性**：无 schema、无数据、无接口签名破坏性变更（新增的 `@RequestParam` 全部 `required=false`，老客户端不传即行为不变）。

---

## 七、Project Structure

### Documentation

```
specs/021-payment-invoice-date-filter/
├── spec.md          # 需求（已产出）
├── plan.md          # 本文件
├── bdd/
│   ├── payment-invoice-date-filter.feature   # 中文 Gherkin（后续产出）
│   └── coverage.md                           # 场景 → JUnit/Playwright 映射（后续产出）
└── tasks.md         # TDD 红绿任务清单（后续产出）
```

### Source Code

```
ruoyi-project/src/main/
├── java/com/ruoyi/project/controller/PaymentController.java   # B1~B3：三个方法各 +2 参数 +2 put
└── resources/mapper/project/
    ├── ContractMapper.xml                                     # B4 :421后、B5 :459后（裸列写法）
    └── PaymentMapper.xml                                      # B6 :92后（date_format 写法）

ruoyi-project/src/test/java/com/ruoyi/project/mapper/
└── PaymentInvoiceDateFilterSqlTest.java                       # 实际 13 例 inv1_~inv7_ + trapRegistry_（XMLMapperBuilder 渲染断言）

ruoyi-project/src/test/java/com/ruoyi/project/controller/
└── PaymentControllerTest.java                                 # 实际 15 例（Mockito + ArgumentCaptor<Contract>）

ruoyi-ui/src/views/project/payment/
└── index.vue                                                  # F1~F7

tests/
└── e2e-payment-invoice-date-filter.spec.js                    # 新增（API 段数据驱动 + UI 段）
```

**不在改动范围内的文件**：`pm-sql/**`（零 DDL）、`Payment.java` / `Contract.java`（零新字段）、`api/project/payment.js`（签名已够用）、任何字典数据。

---

## 八、设计后复查

- **零 DDL、零字典、零新接口、零新实体字段、零新依赖、零新前端组件** —— 与 Summary 一致
- **三条数据出口全部覆盖**（列表 / 合计 / 导出），合计那条独立 SQL 已单列为 B5 并配专属判据 SC-004
- **两种日期写法的边界已明确**（D2 / INV-2），并由 T8 按文件分别断言形状
- **`${params.dataScope}` 的位置作为正确性约束登记**（D4 / INV-4），由 T6 守护
- **搜索状态缓存的真判据已修正**为「重建后的请求仍带参数」而非「控件里有值」（R-001 / SC-014）
- **三处「禁止改动」**（既有 `actualPaymentDate` 条件、`${params.dataScope}`、`PaymentMapper.xml:172` 的无守卫 update）已写进 §2.4

**Constitution 复查：仍全部通过。**
