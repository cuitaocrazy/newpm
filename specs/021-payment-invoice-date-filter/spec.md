# Feature Specification: 付款里程碑「开票日期」区间查询

**特性分支**: `feat/payment-invoice-date-filter`（worktree：`.claude/worktrees/payment-invoice-date-filter`）
**特性目录**: `021-payment-invoice-date-filter`
**关联 Issue**: #36
**基线提交**: `fd3a35f`
**创建日期**: 2026-08-07
**状态**: ✅ **已实现并实测通过**（2026-08-07）——
后端单测 297 全绿（本特性 13 + 15），本特性 e2e **28/28 通过零 skip**，既有回归 **72/72 通过**。
执行环境、命令、结果行与数据库取证见 [`bdd/coverage.md` §八 实测执行记录](./bdd/coverage.md)。
需求侧待确认项见 §八，均不阻塞实现。

> **证据口径**：本文中所有代码位置均为 worktree `feat/payment-invoice-date-filter`
> 在基线 `fd3a35f` 上的实测行号（`grep -n` / `sed -n` 取数，2026-08-07）。
> 未实测的内容一律标注「未验证」，不以推测填空。

---

## 一、业务描述

### 这块功能在业务上是干什么的

**付款里程碑管理**（前端 `ruoyi-ui/src/views/project/payment/index.vue`，路由 `/project/payment`，后端 `PaymentController` → `/project/payment/**`）是合同回款的执行台账。它以「合同 → 付款里程碑」一对多的形态展示：一份合同下挂若干个里程碑（首付款 / 验收款 / 质保金……），每个里程碑记录一条**金额 + 时间 + 状态**的三元组。

页面上与时间相关的字段有两个，业务含义完全不同：

| 展示列 | 物理列 | 业务含义 | 谁产生它 |
|---|---|---|---|
| **开票日期** | `pm_payment.submit_acceptance_date` | **我方向客户开出发票的日期** | 财务开票后回填 |
| 实际回款日期 | `pm_payment.actual_payment_date` | 客户的钱实际到账的日期 | 对账后回填 |

**⚠ 列名与业务名不一致，这是历史遗留而非笔误。** `submit_acceptance_date` 原义是「提交验收材料日期」，业务含义后来改为「开票日期」，但**物理列名与 Java 字段名未改**。有据可查的三处：

- DDL 注释已改：`pm-sql/init/00_tables_ddl.sql:714` — `` `submit_acceptance_date` date DEFAULT NULL COMMENT '开票日期' ``
- 列表展示已改：`payment/index.vue:254` — `label="开票日期" prop="submitAcceptanceDate"`
- 导出表头已改：`Payment.java:68` — `@Excel(name = "开票日期", sort = 23, ...)`，但**上一行 `:66` 的 Java 注释仍写着「提交验收材料日期」**

库里**不存在** `invoice_date` 之类的列（已核实 `pm_payment` 全部 18 列）。因此本特性**零 DDL 变更、零字典变更**。

### 谁在用、为什么需要按开票日期做期间核对

| 角色 | 场景 | 为什么非要「开票日期区间」不可 |
|---|---|---|
| **财务 / 合同管理岗** | 月度、季度、年度**开票台账核对**：把系统里某期间开出的发票，与财务系统 / 税控开票记录逐笔勾稽 | 勾稽的对账单是**按开票期间出的**。用「实际回款日期」筛出来的是另一批单据（开票与回款天然错期，账期常跨月甚至跨季），两者对不上 |
| **财务** | **收入确认与纳税申报期间划分**：确认某张发票落在哪个申报期 | 申报期以开票日期为准，与回款无关 |
| **业务部门负责人** | 排查「已开票但长期未回款」的挂账 | 需要先按开票期间圈定范围，再看回款状态。没有开票日期区间就只能全量导出到 Excel 里手工筛 |

### 现状：这个字段已经查得见，却筛不了

`submit_acceptance_date` 在系统里**已经被 select 出来、已经映射、已经展示、已经导出**，唯独**没有任何查询过滤**——它是一个只读不可筛的列。实测证据（基线 `fd3a35f`）：

| 环节 | 位置 | 现状 |
|---|---|---|
| resultMap 映射 | `ContractMapper.xml:72`、`PaymentMapper.xml:17` | ✅ 已有 |
| select 字段 | `ContractMapper.xml:376`、`PaymentMapper.xml:50/102`、`:43`（`selectPaymentVo`） | ✅ 已有 |
| insert / update | `PaymentMapper.xml:126/146`、`:172` | ✅ 已有 |
| 列表展示 | `payment/index.vue:254-256` | ✅ 已有 |
| 导出列 | `Payment.java:68` | ✅ 已有 |
| **查询过滤** | 三处 `<where>`：`ContractMapper.xml:420-421` 区、`:458-459` 区、`PaymentMapper.xml:87-92` 区 | ❌ **零匹配** |

`grep -rn "submitAcceptanceDate\|submit_acceptance_date"` 在三个 `<where>` 块内**零命中**——这就是本特性要补的全部内容。

### 期望的正确行为

查询区出现一个「开票日期」区间条件（开始日期 + 结束日期），**行为与它旁边的「实际回款日期」完全一致**：同样的控件形态、同样的参数上行方式、同样的闭区间语义，并且同样地作用于**列表、底部合计金额、导出**这三条数据出口。

---

## 二、参照物：「实际回款日期」的完整散点

本特性是一次**对称补齐**，不是新机制发明。参照实现的每一处都必须一一对齐，漏掉任一处就会产生「筛了一半」的缺陷。

### 2.1 后端散点（3 个文件、7 处）

| # | 位置 | 内容 |
|---|---|---|
| 1 | `PaymentController.java:60-73` `listWithContracts` | 一对 `@RequestParam(required=false)`（`:61-62`）→ 塞进 `contract.getParams()`（`:64-69`） |
| 2 | `PaymentController.java:80-91` `sumPaymentAmount` | 同上（`:81-82` / `:84-89`） |
| 3 | `PaymentController.java:99-131` `export` | 同上（`:100-101` / `:103-108`） |
| 4 | `ContractMapper.xml:420-421` `selectContractWithPaymentsList` | **裸列比较**：`and p.actual_payment_date &gt;= #{params.actualPaymentDateStart}` |
| 5 | `ContractMapper.xml:458-459` `sumPaymentAmount` | 同 4（同文件同风格） |
| 6 | `PaymentMapper.xml:87-92` `selectPaymentList` | **`date_format` 两侧归一化**：`date_format(p.actual_payment_date,'%Y-%m-%d') >= date_format(#{...},'%Y-%m-%d')` |
| 7 | `Payment.java` / `Contract.java` | **无字段新增** —— 区间值走 `BaseEntity.params`（`BaseEntity.java:43/107-116`），不是实体字段 |

> **⚠ 两种日期比较写法并存，且都要保留。** `ContractMapper.xml` 用裸列比较，`PaymentMapper.xml` 用 `date_format` 包两侧。两列都是 MySQL `date` 类型（`00_tables_ddl.sql:714/715`），语义上等价；差异是历史风格分化。**本特性沿用各自文件的既有风格，不在任一文件里引入第二种写法**——统一风格属于独立的重构议题（见 OUT-6）。

### 2.2 前端散点（1 个文件、6 处）

| # | 位置 | 内容 |
|---|---|---|
| 1 | `payment/index.vue:120-129` | 「更多」折叠区（`v-if="showMoreSearch"`，`:81` 起）内的 `el-date-picker type="daterange"`，`value-format="YYYY-MM-DD"`，`style="width: 240px"` |
| 2 | `:381` | `const actualPaymentDateRange = ref([])` —— 独立 ref，不在 `queryParams` 里 |
| 3 | `:431-432` | `queryParams` 里的 `actualPaymentDateStart: null` / `actualPaymentDateEnd: null` |
| 4 | `:442-449` | `getList()` **每次**从 ref 反向覆写起止值；`length !== 2` 时写 `null` |
| 5 | `:646` / `:668` | `saveSearchState` / `restoreSearchState` 存取该 ref |
| 6 | `:686` | `resetQuery` 清空该 ref |

导出无需第 7 处：`handleExport`（`:822-827`）是 `{ ...queryParams.value }` 整体展开，起止值只要进了 `queryParams` 就会自动随导出上行。

---

## 三、术语与口径

| 术语 | 定义 |
|---|---|
| **开票日期** | `pm_payment.submit_acceptance_date`，Java 字段 `Payment.submitAcceptanceDate`，MySQL `date` 类型（无时分秒） |
| **区间** | **闭区间** `[start, end]`，两端**含端点** |
| **日期格式** | `YYYY-MM-DD`（前端 `value-format`），字符串上行，后端不做类型转换、原样入 `params` |
| **三条数据出口** | ① 列表 `GET /project/payment/listWithContracts`；② 底部合计 `GET /project/payment/sumPaymentAmount`；③ 导出 `POST /project/payment/export` |
| **口径一致** | 同一组查询条件下，三条出口**看到的是同一个行集合**：合计金额 = 列表全量行的 `payment_amount` 之和；导出行集合 = 列表全量行集合 |

---

## 四、功能需求

### 查询条件与参数契约

- **FR-001**：付款里程碑查询区 MUST 新增「开票日期」区间条件（开始日期 + 结束日期）。控件形态 MUST 与「实际回款日期」一致：`el-date-picker` / `type="daterange"` / `value-format="YYYY-MM-DD"` / `style="width: 240px"`，并置于「更多」折叠区内。
- **FR-002**：前后端参数名 MUST 为 `submitAcceptanceDateStart` 与 `submitAcceptanceDateEnd`，类型 `string`（`YYYY-MM-DD`），二者**各自独立可选**。该命名是前后端契约，MUST NOT 改名（含大小写与缩写变体）。
- **FR-003**：区间语义 MUST 为**闭区间**——`submit_acceptance_date` 恰好等于 `start` 或恰好等于 `end` 的记录 MUST 出现在结果中。

### 三条数据出口（本特性的核心约束）

- **FR-004**：**列表** MUST 按该条件过滤。
- **FR-005**：**底部合计金额** MUST 按**同一条件**过滤，与列表口径一致。漏掉此处会产生「列表筛过了、底部合计还是全量」的错位——这是本特性最容易漏的一处。
- **FR-006**：**导出** MUST 按同一条件过滤，导出的行集合 MUST 等于列表在无分页下的全量行集合。
- **FR-007**：扁平款项列表 `selectPaymentList`（`GET /project/payment/list`）MUST 支持同名的 `params.submitAcceptanceDateStart/End` 过滤，与该语句现有的 `actualPaymentDate` 处理保持同构。
  > 说明：该端点不经 `@RequestParam` 显式接参，条件走 `BaseEntity.params` 的 `params[key]=value` 绑定（与现有 `actualPaymentDate` 在此语句上的形态完全相同）。付款里程碑页面本身不调用它，此项是**为口径一致而补**，不是页面功能。

### 边界行为

- **FR-008**：仅填**开始日期** → MUST 只施加下界（`>= start`），上界不限。
- **FR-009**：仅填**结束日期** → MUST 只施加上界（`<= end`），下界不限。
- **FR-010**：**起止相等**（`start == end`）→ MUST 精确筛出该单日开票的记录。
- **FR-011**：**起 > 止** → MUST 返回**空结果集**且 MUST NOT 报错/抛异常；合计金额 MUST 为 `0`。（UI 的 `daterange` 结构上不产生该输入，但 API 直连可以。）
- **FR-012**：`submit_acceptance_date` 为 **NULL** 的付款里程碑行，只要区间任一端有值，MUST NOT 出现在结果中。
- **FR-013**：**两端都为空**（未填 / 清空 / 未展开「更多」）→ MUST NOT 向 SQL 追加任何该字段的条件，结果 MUST 与本特性上线前逐条一致。

### 组合与页面状态

- **FR-014**：该条件与其余既有查询条件 MUST 为 **AND** 组合；与「实际回款日期」区间 MUST 可同时生效、互不干扰。
- **FR-015**：搜索状态缓存（`sessionStorage`，key `payment_search_state`）MUST 保存并还原该区间的**独立 ref**，而不仅仅是 `queryParams` 里的起止值。
  > 依据：`payment/index.vue:442-449` 的 `getList()` **每次**都从 ref 反向覆写起止值，ref 为空就写 `null`。只还原 `queryParams` 的话，还原后第一次 `getList` 会**当场把刚还原的条件抹成 null**。该机制已由 `payment/index.vue:643-646` 的现场注释与 `tests/e2e-sort-state-persist.spec.js:485-488` 的断言双重记录。
- **FR-016**：「重置」MUST 清空该区间 ref，并 MUST 清掉搜索状态缓存条目（与既有 `resetQuery` 同一处理）。

---

## 五、不变式（本次改造不得破坏的既有行为）

> ⚠️ **回填校正（2026-08-07，实现后）**：本表的 INV-1~INV-7 与
> `PaymentInvoiceDateFilterSqlTest` 里 `@DisplayName` 用的 INV-1~INV-7 **同号不同义**
> （测试类那套讲的是自己的断言维度：形状 / 风格 / 位置 / 空串 / 单端 / 无参 / 共存）。
> 引用任何 INV-x 前先确认是哪一套；换算表见
> [`bdd/coverage.md` §四](./bdd/coverage.md) 与 §七 D-4。

| 编号 | 不变式 | 背景与依据 |
|---|---|---|
| INV-1 | 「实际回款日期」的既有行为 MUST **一行不动** —— 参数名、SQL 写法、控件、缓存、重置全部保持原样 | `tests/e2e-sort-state-persist.spec.js:457/485-488` 断言 `actualPaymentDateStart=2000-01-01`；改坏即红 |
| INV-2 | `ContractMapper.xml` 内 MUST 只有裸列比较一种日期写法；`PaymentMapper.xml` 内 MUST 只有 `date_format` 一种 | 同文件内两种风格并存是化石记录，会误导后续维护者（CLAUDE.md 已记录同类「不对称即缺陷」的判据） |
| INV-3 | 三条出口的 `<where>` 条件集合 MUST 保持**互为镜像** | `ContractMapper.xml:420-421` 与 `:458-459` 现在逐字相同；破坏对称就是 FR-005 失守的前兆 |
| INV-4 | `${params.dataScope}` 的位置与语义 MUST 不变（`ContractMapper.xml:422`、`PaymentMapper.xml:93`） | 数据权限是安全约束；新条件必须**追加在其之前**，MUST NOT 挪动或包裹它。<br>⚠️ **回填校正**：`ContractMapper.sumPaymentAmount`（页脚合计）**本来就没有** `${params.dataScope}` —— 这是早于本特性的既有缺陷（合计不受部门数据权限约束，配对的列表却受约束），**已知未修、不在本特性范围**。它使「合计与列表口径一致」这个保证**仅对 admin 成立**。详见 [`bdd/coverage.md` §五 盲区 1](./bdd/coverage.md) |
| INV-5 | MUST NOT 修改 `pm_payment` 的任何 DDL、字典、`insert`/`update` 语句 | 本特性纯查询增量；`PaymentMapper.xml:172` 的 `submit_acceptance_date = #{submitAcceptanceDate}`（**无 `<if>` 守卫**，是 Issue #7/#10 治理后的正确形态）MUST 保持原样 |
| INV-6 | 分页与排序行为 MUST 不变 | `ContractMapper.xml:424` 的 `order by c.create_time desc, p.create_time asc` 与 `startPage()` 调用顺序均不动 |
| INV-7 | 付款里程碑的其余既有筛选（付款状态多选、季度多选、部门 ancestors 下钻等）MUST 不受影响 | `tests/payment-status-filter-regression.spec.js` 是现成探针 |

---

## 六、验收标准

| 编号 | 可验收判据 | 验证方式 |
|---|---|---|
| **SC-001** | 「更多」展开后，查询区可见「开票日期」区间控件，且与「实际回款日期」控件形态一致 | E2E（UI） |
| **SC-002** | 填入区间点「查询」后，上行请求 query 中含 `submitAcceptanceDateStart` 与 `submitAcceptanceDateEnd`，值为所填的 `YYYY-MM-DD` | E2E（拦截请求断言） |
| **SC-003** | 列表返回的**每一条**付款里程碑，其 `submitAcceptanceDate` 均落在 `[start, end]` 内 | E2E（API，逐行断言） |
| **SC-004** | 同一组条件下：`sumPaymentAmount` 的返回值 == 列表全量行 `paymentAmount` 之和 | E2E（API，两次调用比对） |
| **SC-005** | 同一组条件下：导出的数据行数 == 列表全量行数（口径一致，FR-006） | E2E（API，导出响应可解析行数则断言行数；否则至少断言 HTTP 200 且非空，并记录降级理由） |
| **SC-006** | **仅填开始**：结果中每行 `submitAcceptanceDate >= start`，且存在至少一条 `> start` 的行（证明上界确未被施加） | E2E（API） |
| **SC-007** | **仅填结束**：结果中每行 `submitAcceptanceDate <= end`，且存在至少一条 `< end` 的行 | E2E（API） |
| **SC-008** | **起止相等**（取库中真实存在的某个开票日期 D）：结果非空，且每行 `submitAcceptanceDate == D` | ~~E2E（API，先探测真实值再断言，不硬编码日期）~~ → **实现改为自造确定数据集**（`2026-03-15`），理由见 [`bdd/coverage.md` §七 D-2](./bdd/coverage.md) |
| **SC-009** | **含端点**：以库中真实的最小/最大开票日期作为 `start`/`end`，这两条边界记录 MUST 出现在结果中 | ~~E2E（API，探测库中真实极值）~~ → **实现改为自造边界行**（`2026-03-01` / `2026-03-31`），同 D-2 |
| **SC-010** | **起 > 止**：`code=200`、`rows` 为空数组、合计金额为 `0`，无异常 | E2E（API） |
| **SC-011** | **空值行不出现**：库中存在 `submitAcceptanceDate` 为 null 的付款里程碑，任一端有值时结果中 MUST 无该行 | ~~E2E（先探测数据前提，无此类数据则显式 skip）~~ → **实现自造该 NULL 行，故不再需要 skip**；另加一条反证用例（不带开票条件时该行必须可见），防止「造数没造出来 → 断言假绿」。见 D-2 |
| **SC-012** | **两端为空**：带空参数与完全不带参数两次查询，`total` 与首页行集合逐条一致（FR-013） | E2E（API） |
| **SC-013** | **组合生效**：开票日期区间 + 实际回款日期区间同时给出，结果同时满足两个区间 | E2E（API） |
| **SC-014** | **状态缓存**：填区间 → 查询 → 进详情/关页签再回列表 → 控件里值仍在，且重建后的列表请求仍带 `submitAcceptanceDateStart/End` | E2E（UI，照 `e2e-sort-state-persist.spec.js` 既有写法） |
| **SC-015** | **重置**：点「重置」后控件清空，且随后的列表请求不再带这两个参数 | E2E（UI） |
| **SC-016** | 「实际回款日期」既有行为零回归：`tests/e2e-sort-state-persist.spec.js` 全绿（INV-1） | Playwright |
| **SC-017** | 既有付款筛选零回归：`tests/payment-status-filter-regression.spec.js`、`tests/e2e-payment-crud.spec.js`、`tests/payment-clear-field-regression.spec.js` 全绿 | Playwright |
| **SC-018** | `mvn clean compile -pl ruoyi-project -am` 通过；`mvn test -pl ruoyi-project -am` 不因本次改动新增红灯 | 本地 / CI |
| **SC-019** | `npx vue-tsc --noEmit` 中 **`payment/index.vue` 零错误**（全局存量基线约 39 个错误不作门槛） | 本地 |
| **SC-020** | 三个 `<where>` 中新条件的写法与该文件既有日期写法**逐字同风格**（`ContractMapper` 裸列 / `PaymentMapper` `date_format`），且均位于 `${params.dataScope}` 之前（INV-2 / INV-4） | 代码评审 + diff 自检 |

> ✅ **实测结论（2026-08-07）**：SC-001 ~ SC-020 **逐条已验证通过**。
> 承载它们的用例与逐例耗时见 [`bdd/coverage.md` §二 / §三](./bdd/coverage.md)，
> 执行环境、命令、结果行（`28 passed (42.4s)` / `72 passed (1.6m)` / `Tests run: 297`）
> 与三条数据库取证 SQL 见 [`bdd/coverage.md` §八](./bdd/coverage.md)。
>
> **三条降级 / 限定必须与「通过」一起读，不得单独引用结论**：
> 1. **SC-005 的判据已降级**：不解析 Excel 行数，改用「空集 < 区间 < 全量」的**字节数单调性**。
>    能抓「导出根本没筛」，抓不到「筛错了但行数恰好也变了」。理由见 `bdd/coverage.md` §七 D-3。
> 2. **SC-004 的「合计 == 列表之和」仅对 admin 成立**：`ContractMapper.sumPaymentAmount` 没有
>    `${params.dataScope}`（既有缺陷，不在本特性范围，见 INV-4 的回填校正），
>    而测试账号写死为 admin（数据权限「全部」）。**本次 28/28 正是在 admin 视角下取得的。**
> 3. **SC-008 / SC-009 / SC-011 的数据来源已改为自造确定数据集**（不再探测库中真实极值），
>    因此 SC-011 **不再需要 skip**，本次也**确实没有触发任何 skip**。见 D-2。

---

## 七、非目标（Out of Scope）

- **OUT-1**：**不改任何 DDL**。`submit_acceptance_date` 列、类型、注释、索引全部不动；不新增 `invoice_date` 列，不做列改名。
- **OUT-2**：**不改任何字典**。本条件是自由日期区间，不涉及 `sys_jdgl` 等字典项，不需要刷 Redis 字典缓存。
- **OUT-3**：**不改「实际回款日期」的任何既有行为**（INV-1）。这是对称补齐，不是顺手重构。
- **OUT-4**：**不重命名** `submit_acceptance_date` / `submitAcceptanceDate`。列名与业务名的历史错位是**已知且被接受**的现状（改名会波及 resultMap、insert/update、导出注解、前端列与本次新增的过滤，收益为零、风险为全模块）。
- **OUT-5**：**不修正** `Payment.java:66` 那句仍写着「提交验收材料日期」的 Java 注释。它是 OUT-4 的同类项，属独立的文档订正（记为 OQ-2）。
- **OUT-6**：**不统一两个 mapper 的日期比较写法**。裸列 vs `date_format` 的风格分化是既有事实，统一它需要回归全部日期类筛选，属独立重构议题。
- **OUT-7**：**不新增接口、不新增实体字段**。区间值走 `BaseEntity.params`，与「实际回款日期」完全同构。
- **OUT-8**：**不改导出的列集合与表头**。「开票日期」导出列早已存在（`Payment.java:68`），本特性只改导出的**行过滤**。
- **OUT-9**：**不做跨字段联动校验**（如「开票日期不得晚于回款日期」）。区间之间无约束。
- **OUT-10**：**不改分页、排序、数据权限**（INV-4 / INV-6）。
- **OUT-11**：**不做时区/时分秒处理**。列是 MySQL `date`，无时分秒；不引入 `datetime` 语义。

---

## 八、假设与待确认

### 已确认的事实（作为约束写死）

| 编号 | 内容 | 证据 |
|---|---|---|
| D-A | 「开票日期」= `pm_payment.submit_acceptance_date`，库中**不存在** `invoice_date` 列 | `00_tables_ddl.sql:714`；`pm_payment` 全 18 列已核 |
| D-B | 参数名固定为 `submitAcceptanceDateStart` / `submitAcceptanceDateEnd` | Issue #36 契约约定 |
| D-C | 行为与「实际回款日期」完全一致（闭区间、三出口、`params` 承载） | §二 参照实现 |
| D-D | 本特性零 DDL、零字典变更 | D-A 推论 |

### 假设

1. **闭区间是业务期望的默认语义。** 依据：既有「实际回款日期」用的就是 `>=` / `<=`（`ContractMapper.xml:420-421`），财务按期间核对时「当月 1 日至当月末日」需要含两端。
2. **开票日期允许为空是正常业务状态**（尚未开票的里程碑），故 FR-012 的「空值行不出现」是正确行为而非缺陷 —— 与 SQL 中 `NULL` 参与比较恒为 `UNKNOWN` 的天然语义一致，无需额外写 `IS NOT NULL`。
3. **`GET /project/payment/list`（扁平列表）当前没有已知的前端调用方带该条件**，FR-007 是为口径一致而补。若将来有页面用它，行为已就位。

### 待确认（不阻塞实现）

| 编号 | 问题 | 暂定处置 |
|---|---|---|
| **OQ-1** | 「开票日期」筛选是否需要同时出现在**其他**用到付款里程碑的页面（如合同详情内嵌的里程碑列表）？ | **本次只做付款里程碑管理页**（Issue #36 原文范围）。其他页面若有需要，作为增量特性另提 |
| **OQ-2** | `Payment.java:66` 的过期 Java 注释（「提交验收材料日期」）是否顺手订正？ | **本次不改**（OUT-5）。建议单独提一个文档订正 Issue，避免本特性 diff 混入无关改动 |
| **OQ-3** | 是否需要把「开票日期」列也做成可排序列（`sortable="custom"`）？ | **本次不做**。Issue #36 只要求查询条件；排序需按 Issue #16 的「唯一次级排序键」规则另行设计 |
