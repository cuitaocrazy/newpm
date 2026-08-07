# BDD 场景覆盖映射

**特性**: `021-payment-invoice-date-filter`（付款里程碑「开票日期」区间查询，Issue #36）
**场景文件**: [payment-invoice-date-filter.feature](./payment-invoice-date-filter.feature)
**基线提交**: `fd3a35f` | **设计日期**: 2026-08-07 | **产物回填**: 2026-08-07 | **实测回填**: 2026-08-07

**阶段**: ✅ **实测执行后回填版（第三版）**。演进轨迹如下，三版的可信度不是一个量级：

| 版本 | 内容 | 可信度 |
|---|---|---|
| 第一版（BDD 设计阶段，硬性流程第 3 步） | 状态列全是「🔴 待落地」，用例名全是设计时臆造的占位名 | ❌ **空头支票**（见 §七 D-0） |
| 第二版（实现后按产物回填） | 用例名逐个 `grep` 核对过，单测 28 例已实测全绿 + 8 变异体验证；**e2e 仅"能被枚举"，一次没跑** | ⚠️ 单测层可信，e2e 层零证据 |
| **第三版（本版，实测执行后回填）** | **e2e 28 例真跑，28/28 通过零 skip；既有回归 72/72 通过；后端单测 297 全绿**。每条状态附耗时，并附**数据库取证**证明执行确实发生 | ✅ 有可复现证据 |

所有与设计意图不符之处按「如实标注偏离」的要求写在 [§七 设计与实现的偏离登记](#七设计与实现的偏离登记)；
本次的执行环境、命令、结果行与取证 SQL 见 [§八 实测执行记录](#八实测执行记录2026-08-07)。

> ⚠️ **第二版回填时发现：第一版是一张空头支票。** 它声称的 `PaymentInvoiceDateFilterSqlTest`（T1~T8，约 21 例）
> 当时**文件根本不存在**，而它给出的验收命令带 `-Dsurefire.failIfNoSpecifiedTests=false`
> ——该 flag 会让「测试类不存在」也 BUILD SUCCESS。**跑 0 个用例照样绿，且没有任何征兆。**
> 详见 [§六](#六判定bdd-测试通过的命令) 的验收判据与 [§七 D-0](#七设计与实现的偏离登记)。

本项目**无 Cucumber 工具链**，`.feature` 不直接执行。本文件把每个 Gherkin 场景映射到实际可执行的
JUnit 单测与 Playwright e2e，作为「BDD 测试通过」的判定依据。

`.feature` 共 **19 个场景**（其中 2 个场景大纲，展开为 23 例）。

---

## 〇、当前状态速览（先看这个）

| 执行层 | 载体 | 实际用例数 | 状态 | 判据 |
|---|---|---|---|---|
| 🧪 JUnit（SQL 渲染） | `PaymentInvoiceDateFilterSqlTest.java` | **13** | ✅ **全绿，已变异验证** | `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0` |
| 🧪 JUnit（Controller plumbing） | `PaymentControllerTest.java` | **15** | ✅ **全绿，已变异验证** | `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0` |
| 🌐 Playwright API | `e2e-payment-invoice-date-filter.spec.js`（前 23 例） | **23** | ✅ **全部通过，零 skip** | 与 UI 段同批执行，结果行 `28 passed (42.4s)` |
| 🖱️ Playwright UI | 同文件 `describe('查询区交互')` | **5** | ✅ **全部通过** | 同上 |
| 🛡️ 既有回归套件 | 5 个文件 | **72** | ✅ **全绿，无新增红灯** | 结果行 `72 passed (1.6m)` |
| 🧪 后端全模块回归 | `mvn test -pl ruoyi-project -am` | **297** | ✅ **全绿** | `Tests run: 297, Failures: 0, Errors: 0, Skipped: 0` |

> ✅ **本次实测结论 + 证据（第三版，2026-08-07）**
>
> 上一版这里写的是「已就绪待执行」——e2e 文件已写完、能被 Playwright 正确枚举
> （`--list` 输出 `Total: 28 tests in 1 file`），但**一次都没真跑过**。
> 本次已补上执行，结论如下：
>
> | 项 | 上一版状态 | 本次实测 |
> |---|---|---|
> | 本特性 e2e 28 例 | 🟡 零执行证据 | ✅ `28 passed (42.4s)`，**零 skip** |
> | 4.2 / 4.3 两条曾预警"可能 skip" | ⚠️ 可能静默降级 | ✅ **两条都真实执行**（111ms / 88ms），预警排除 |
> | 5 个既有回归套件 | 🟡 待执行 | ✅ `72 passed (1.6m)` |
> | 后端全模块单测 | ✅ 已绿（13+15） | ✅ 全模块 297 全绿 |
>
> **数据库取证（这才是"真跑过"的判据，不是 agent 自述）** —— 三条 SQL 的实际输出见
> [§八](#八实测执行记录2026-08-07)：`pm_contract` 里留下了 `E2E开票日期合同_INV1786109928918`
> （`del_flag=1`，软删痕迹）；`sys_oper_log` 里有 4 条当日 `/project/payment/export` 记录；
> 自造的 6 条里程碑（436~441）已被 `afterAll` 清理干净（剩余 0 条）。
>
> ---
>
> 📖 **这段判据的方法论价值对未来读者仍然成立，请勿删除：**
>
> **「文件能被枚举」≠「用例会通过」。** 两者之间隔着一整个执行过程：后端要起得来、
> 前端要能连上、造数要真成功、断言要真跑到。任何一环断掉，都可能表现为
> 「文件在那儿、`--list` 数得出来、但零绿灯」。
>
> **因此判定「跑过没有」永远不要凭 agent 自述，要找可证伪的落地痕迹**：
> - `pm_contract` 里 `contract_name LIKE 'E2E开票日期合同_INV%'` 且 `del_flag='1'` 的软删行
>   （`cleanup()` 走 `DELETE /project/contract/{id}`，合同是软删，痕迹不会消失，是最稳的探针）；
> - `sys_oper_log` 里 `/project/payment/export` 的导出记录（`@Log(EXPORT)` 留痕，证明请求真打到了后端）。
>
> 这两处**都查不到 = 没跑过**，不管报告写得多漂亮。
> MEMORY 记过「agent 自述 7/7 红、实际 35 全 skip」的事故 —— 本次之所以敢把 🟡 改成 ✅，
> 靠的正是上面这两条痕迹都被 `SELECT` 出来了，而不是靠一句「跑过了」。

---

## 一、执行层选择原则

| 判据 | 执行层 | 理由 |
|---|---|---|
| **`<if>` 是否渲染、渲染成什么形状、排在哪个位置** | **JUnit**（`XMLMapperBuilder` 渲染 SQL 文本断言，不连库） | 本特性的本体就是三段 `<if>`，行为完全体现在 SQL 渲染上。毫秒级，是唯一能做出真「红→绿」的单测层 |
| **Controller 把两个入参塞没塞进 `params`** | **JUnit**（Mockito + `ArgumentCaptor<Contract>`） | 纯 Java plumbing，三处各自独立，最容易漏 `export` 那处 |
| **真实行集合 / 金额加总 / 端点包含 / 空值排除** | **Playwright API e2e** | 比较运算的真实语义（含 `NULL` 参与比较恒 `UNKNOWN`）只有真库能证；合计与列表口径是否一致更是只能实测比对 |
| **控件形态、参数上行、搜索状态缓存、重置、导出** | **Playwright UI e2e** | 前端无单测地基（无 vitest/jest，见 Issue #15） |

> ⚠️ **本特性最容易被误判的两点（回填后依然成立）**：
>
> 1. **单测全绿 ≠ 筛选生效。** 单测断言的是「渲染出的 SQL 文本里有没有这段 `>=`」，
>    **它证明不了 MySQL 拿这段 SQL 跑出来的行集合是对的**，更证明不了空值行被排除、合计与列表口径一致。
>    这些只有 API e2e 能证。
>    ✅ **本次 API e2e 已真跑（23/23 通过）**，所以现在的正确表述从
>    「SQL 形状已锁死，语义尚未实测」升级为「**SQL 形状已锁死，真实语义已在本地库上实测**」。
>    但这条方法论提醒仍然成立：**下次谁只跑了单测就来宣布"筛选生效"，判据依旧不成立。**
> 2. **`assertTrue(sql.contains("submit_acceptance_date"))` 是恒绿废断言。**
>    该列本来就在 select 字段列表里（`ContractMapper.xml:376`、`PaymentMapper.xml:50`），
>    这条断言在改动前就是绿的。断言必须落在**完整比较形状**上。
>    ✅ 这条已不再只是注释里的警告 —— 实现里用
>    `trapRegistry_bareColumnNameIsAlwaysPresent` **把它做成了一条可执行的证明用例**：
>    在完全不传参的前提下断言两条列表语句仍含裸列名，证明该断言恒真。

---

## 二、场景 → 可执行用例映射（✅ 已按实际产物回填）

图例：🧪 JUnit ｜ 🌐 Playwright API ｜ 🖱️ Playwright UI ｜ 🛡️ 既有回归（必须保绿，不得改）

**用例载体（实际）**
- 🧪 `ruoyi-project/src/test/java/com/ruoyi/project/mapper/PaymentInvoiceDateFilterSqlTest.java` —— **13 例**，方法名前缀 `inv1_`~`inv7_` + `trapRegistry_`
- 🧪 `ruoyi-project/src/test/java/com/ruoyi/project/controller/PaymentControllerTest.java` —— **15 例**，方法名前缀 `listWithContracts_` / `sumPaymentAmount_` / `export_`
- 🌐🖱️ `tests/e2e-payment-invoice-date-filter.spec.js` —— **28 例**（API 23 + UI 5，同一 `describe.serial` 下，UI 在嵌套 `describe('查询区交互')` 里）

> 📌 **用例名一律抄自实际文件，不是设计时的占位名。** 第一版声称的 `T1`~`T8`、
> `listWithContracts_putsInvoiceDateParamsIntoParams`、`[SC-001] …` 这类标题
> **没有一个与实际对得上**（实际 28 个 e2e 标题里含 `[SC-` 的为 **0** 个）。偏离清单见 §七。

### 一、查询区里出现「开票日期」

| # | 场景标题 | 覆盖 FR | 落地方式 | 实际用例名 | 状态 |
|---|---|---|---|---|---|
| 1.1 | 「开票日期」条件与「实际回款日期」并排出现在「更多」里 | FR-001 | 🖱️ 仅 UI e2e | `e2e-…spec.js:664` 「开票日期」与「实际回款日期」并排出现在「更多」折叠区里 | ✅ **实测通过（8.5s）** |
| 1.2 | 填好开票日期点查询，条件真的传到了后台 | FR-002 | 🖱️ UI e2e + 🧪 | `e2e-…spec.js:686` 填好开票日期点查询，条件按契约参数名上行到后台<br>后端一侧由 🧪 `PaymentControllerTest#listWithContracts_writesSubmitAcceptanceDateRangeIntoParams` 锁死 | ✅ **e2e 实测通过（4.3s）**<br>✅ 单测绿 |

> **1.1 / 1.2 为什么只能 UI e2e**：控件属性与「界面填了值 → 请求里真的带上了」这条链路，
> 全部活在 `payment/index.vue` 的模板与 `getList()` 里，后端与单测都看不见。
> **1.2 的判据是「请求 query 里有这个 key 且值一字不差」**——只断言"点了查询没报错"等于什么都没验
> （参数名写错时后端拿不到 key → `<if>` 不成立 → 静默返回全量，**不报错**）。

### 二、按开票期间圈定范围（核心）

| # | 场景标题 | 覆盖 FR | 落地方式 | 实际用例名 | 状态 |
|---|---|---|---|---|---|
| 2.1 | 财务按月核对：只看 2026 年 3 月开出的发票 | FR-003 / FR-004 | 🧪 + 🌐 | 🧪 `inv1_contractList_rendersBareColumnComparison`<br>🌐 `:354` 完整区间命中：2026-03-01 ~ 2026-03-31 只筛出 3 月开票的 A/B/C/F<br>🌐 `:365` 区间外的里程碑不命中：4 月 1 日开票的 D 被 3 月区间挡在外面 | ✅ 单测绿<br>✅ **e2e 实测通过（62ms / 116ms）** |
| 2.2a | 区间两端都算数 —— 等于**起始日**的被查出 | FR-003 | 🌐 仅 e2e | `:388` 区间两端都算数（恰好等于起始日）：A 必须被查出来 | ✅ **实测通过（60ms）** |
| 2.2b | 区间两端都算数 —— 等于**结束日**的被查出 | FR-003 | 🌐 仅 e2e | `:388` 区间两端都算数（恰好等于结束日）：C 必须被查出来 | ✅ **实测通过（65ms）** |
| 2.3 | 只填开始日期：只压下界 | FR-008 | 🧪 + 🌐 | 🧪 `inv5_startOnly_rendersOnlyLowerBound`（三条语句一并断言，含**不出现 `<=`**）<br>🌐 `:400` 只填开始日期：2026-03-31 起开出的发票全都要（C、D），往后不设限 | ✅ 单测绿<br>✅ **e2e 实测通过（80ms）** |
| 2.4 | 只填结束日期：只压上界 | FR-009 | 🧪 + 🌐 | 🧪 `inv5_endOnly_rendersOnlyUpperBound`（含**不出现 `>=`**）<br>🌐 `:410` 只填结束日期：2026-03-01 之前开出的发票全都要（A），往前不设限 | ✅ 单测绿<br>✅ **e2e 实测通过（85ms）** |
| 2.5 | 起止是同一天：精确筛出这一天开出的发票 | FR-010 | 🌐 仅 e2e | `:423` 起止同一天（2026-03-15）：精确筛出这天开票的 B 与 F | ✅ **实测通过（81ms）** |
| 2.6 | 尚未开票的里程碑，在任何开票期间查询里都不出现（4 例） | FR-012 | 🌐 仅 e2e | `:445` 尚未开票的里程碑不出现（完整区间 / 仅开始日期 / 仅结束日期 / 同一天）**共 4 例**<br>➕ `:457` 未开票的里程碑在"不带开票条件"时仍然可见（**造数自证反证例**） | ✅ **5 例全部实测通过**<br>（54 / 54 / 52 / 45ms + 反证例 119ms）<br>**未触发任何 skip**（见 §七 D-2） |
| 2.7 | 起始日期晚于结束日期：空结果且不报错 | FR-011 | 🌐 仅 e2e | `:465` 起始晚于结束（2026-04-01 ~ 2026-03-01）：返回空集且接口不报错 | ✅ **实测通过（89ms）** |

> **2.2 / 2.5 / 2.6 / 2.7 为什么单测够不到**：它们断言的是**比较运算在 MySQL 里的真实语义**——
> 闭区间含端点、`NULL >= '2026-01-01'` 得 `UNKNOWN` 因而不进结果、恒假条件下
> `COALESCE(sum(...),0)` 返回 `0`。单测断言的是 SQL 文本，**这四条行为一个字都验不到**。
> **它们曾是本特性覆盖度最薄的一段（设计上只有 e2e 能证，而 e2e 没跑）——
> ✅ 本次 e2e 已真跑，这 8 例（2.2 ×2 / 2.5 / 2.6 ×5 含反证 / 2.7）全部通过，
> 该段从"设计上有覆盖、事实上零证据"转为有实测证据。**
>
> **2.6 的 `:457` 反证例是必须的。** 只断言「未开票的行不出现」有一个致命的假绿路径：
> 造数如果压根没造出那条 NULL 行，断言照样绿。`:457` 在**不带开票条件**时断言 E 可见，
> 把「它确实存在」钉死，「不出现」才有意义。
>
> **2.3 / 2.4 的真判据是「另一端确实没被施加」。** 单测侧由 `inv5_*` 的
> `assertNotContains` 守（另一端片段不得出现）；e2e 侧由「C、D 都要」「只有 A」这种
> **跨越另一端的行必须在结果里**的集合断言守。

### 三、三条数据出口必须口径一致

| # | 场景标题 | 覆盖 FR | 落地方式 | 实际用例名 | 状态 |
|---|---|---|---|---|---|
| 3.1 | **底部合计金额跟着开票日期一起变，不再是全量** | **FR-005** | 🧪 + 🌐 | 🧪 `inv1_contractSum_rendersBareColumnComparison`<br>🧪 `inv3_contractSum_fragmentInsideWhereClause`<br>🌐 `:488` 底部合计金额跟着开票日期一起变，不是全量<br>🌐 **`:501` 合计与列表口径完全一致：命中集合的金额之和 == sumPaymentAmount** | ✅ 单测绿<br>✅ **e2e 实测通过（50ms / 285ms）**<br>⚠️ 仅 admin 视角，见 §五 盲区 1 |
| 3.2 | 导出拿到的和列表看到的是同一批数据 | FR-006 | 🧪 + 🌐 | 🧪 `PaymentControllerTest#export_writesSubmitAcceptanceDateRangeIntoParams`<br>🧪 `#export_nullSubmitAcceptanceDates_keysAbsentFromParams`<br>🧪 `#export_bothDateRangesCoexistInParams`<br>🧪 `#export_actualPaymentDateBehaviorUnchanged`<br>🌐 `:521` 导出口径与列表一致：接口不报错且受开票日期条件影响（**判据降级，见 §七 D-3**）<br>🖱️ `:810` 选了日期不点查询直接导出（**新增缺陷回归，见 §七 D-5**） | ✅ 单测绿<br>✅ **e2e 实测通过（2.9s / 5.6s）**<br>取证：`sys_oper_log` 当日 4 条 `/payment/export` |
| 3.3 | 系统里另一条款项清单出口也按同一口径过滤 | FR-007 | 🧪 + 🌐 | 🧪 `inv1_paymentList_rendersDateFormatComparison`<br>🧪 `inv2_paymentList_mustUseDateFormatStyle`<br>🌐 `:548` 扁平款项清单 /project/payment/list 也按同一口径过滤 | ✅ 单测绿<br>✅ **e2e 实测通过（225ms）**<br>🔑 **解除了一条"未验证"疑点，见下方注** |

> **3.1 是本特性的核心业务判据，也是最容易漏的一处。** 合计走的是
> `ContractMapper.sumPaymentAmount`（`ContractMapper.xml:434-465`）——一条**完全独立**的 SQL，
> 自带一份逐字复制的 `<where>`。只改列表不改它，页面看起来**完全正常**：列表确实筛过了，
> 底部数字却是全库合计。**唯一能发现它的判据就是「合计 == 列表全量之和」**（`:501`），
> 不是「合计有值」也不是「合计变小了」。
> 变异 **M5**（删掉 `sumPaymentAmount` 里 `submitAcceptanceDateEnd` 的整行 `<if>`）就是这条的探针，实测**红**。
> ✅ `:501` 本次实测通过（285ms），FR-005 从「SQL 里改了」升级为「口径真的一致」。
>
> ⚠️ **但 3.1 的保证有一个数据权限盲区，且本次 28/28 是在 admin 视角下取得的，
> 见 [§五 已知覆盖盲区](#五已知覆盖盲区必须与结论一起读)。**
>
> **3.2 的后端一侧单独存在。** `export` 有自己的四对 `@RequestParam`（`PaymentController.java:118-134`），
> SQL 复用列表那条，因此 SQL 侧不需要第三份 `<where>`，但 Controller 侧必须单独加参数。
> 漏了它导出仍是全量，而导出是异步下载，**不打开 Excel 发现不了**。
>
> **3.3 的形态是既定的**：`GET /project/payment/list` 无 `@RequestParam` plumbing，条件走
> `params[key]=value` 的 Map 绑定（与该语句现有 `actualPaymentDate` 完全同构）。
> 实现照此办理，**没有**给 `/list` 加 `@RequestParam`。
>
> 🔑 **3.3 曾登记过一条「未验证」疑点，本次已实测解除。**
>
> - **原疑点**（SQL 单测作者如实登记）：「`PaymentMapper.selectPaymentList` 加了两个 `<if>`，
>   但 `PaymentController` 的 `GET /list` 签名是 `list(Payment payment)`，
>   **没有**像另外三个方法那样显式加 `@RequestParam` 往 `params` 里塞值。
>   该路径要生效只能靠 Spring 对 `params[submitAcceptanceDateStart]=...` 的 Map 绑定，**未实测**。」
>   —— 这是一条合理的怀疑：`<if>` 写对了、但 `params` 里压根没值，SQL 片段就永远不渲染，
>   **表现是"静默返回全量"，不报错**，纯单测（只渲染 SQL、不走 Spring 绑定）照不到。
> - **本次验证方式**：e2e 第 19 例 `:548`「扁平款项清单 `/project/payment/list` 也按同一口径过滤」
>   用真实 HTTP 请求带 `params[submitAcceptanceDateStart]` / `params[submitAcceptanceDateEnd]`
>   打到运行中的后端，断言返回行集合确实被收窄。
> - **结论**：✅ **实测通过（225ms）** —— Spring 的 `params` Map 绑定确实工作，
>   **FR-007 是真实现，不是纸面实现**。该条从「未验证」更新为「已实测通过」。

### 四、与其它查询条件是「并且」的关系

| # | 场景标题 | 覆盖 FR | 落地方式 | 实际用例名 | 状态 |
|---|---|---|---|---|---|
| 4.1 | 开票期间 + 回款期间同时填，两个条件都得满足 | FR-014 | 🧪 + 🌐 | 🧪 `inv7_coexistsWithActualPaymentDateFilter`（三条语句 × 四个片段同时出现）<br>🧪 `#listWithContracts_bothDateRangesCoexistInParams` / `#sumPaymentAmount_bothDateRangesCoexistInParams` / `#export_bothDateRangesCoexistInParams`<br>🌐 `:568` 开票期间 + 实际回款期间：两个条件是 AND，不是 OR | ✅ 单测绿<br>✅ **e2e 实测通过（182ms）** |
| 4.2 | 开票期间 + 付款状态 | FR-014 | 🌐 | `:598` 开票期间 + 付款状态：两个条件叠加生效<br>代码里有 `test.skip(!paymentStatusValue, …)` 兜底（字典 `sys_fkzt` 取不到值时跳过） | ✅ **实测通过（111ms）**<br>🔓 **skip 未触发**，预警排除 |
| 4.3 | 开票期间 + 合同所属团队 | FR-014 | 🌐 + 🛡️ | `:617` 开票期间 + 合同所属团队：两个条件叠加生效<br>代码里有 `test.skip(!contractDeptId, …)` 兜底（自造合同无 deptId 时跳过）<br>🛡️ `tests/payment-status-filter-regression.spec.js` 保绿 | ✅ **实测通过（88ms）**<br>🔓 **skip 未触发**，预警排除 |
| 4.4 | 开票期间 + 合同名称 | FR-014 | 🌐 | `:638` 开票期间 + 合同名称：两个条件叠加生效 | ✅ **实测通过（74ms）** |

> ✅ **4.2 / 4.3 的 skip 预警已被实测排除（本次最值得记录的一条核查结果）。**
>
> 上一版这里写的是「跑完必须看报告确认它们没被静默跳过」——因为
> `test.skip(!paymentStatusValue, …)` / `test.skip(!contractDeptId, …)` 是**真实存在的降级路径**，
> 一旦触发，报告上是 `-` 而不是 `✓`，FR-014 的这两个组合维度就会**零覆盖却看着像绿的**。
>
> **本次实测结果：两条都真实执行并通过（`:598` 111ms、`:617` 88ms），零 skip。**
> 全套件结果行是 `28 passed (42.4s)` —— **28 passed 而非「28 tests」**，
> 且 Playwright 的 list reporter 会把 skipped 单独计数，输出里没有任何 skipped 项。
> 说明本地环境下字典 `sys_fkzt` 取到了可用值、自造合同也带上了 `deptId`。
>
> 📖 **方法论仍然保留**：skip 分支还在代码里，**换个环境（空字典库 / 不同造数）依然可能触发**。
> 判据不变——`--reporter=list` 输出里这两条前面必须是 `✓` 而不是 `-`，
> 或 `test-results.json` 里 `status` 不是 `skipped`。
> **「跳过」要报成跳过，不能报成通过** —— MEMORY 记过这类事故。
>
> **4.1 的单测部分守的是「新条件没被写进既有条件的 `<if>` 内部」**：缩进错位是 XML 编辑的常见手滑，
> 会造成「不填开票日期时回款日期也失效」——四个片段共存的断言能当场抓住它。
> 另一半（既有行一字未动）由 `git diff` 自检 + 🛡️ `e2e-sort-state-persist.spec.js:457/485-488` 的既有断言守，
> 以及 🧪 `#listWithContracts_actualPaymentDateBehaviorUnchanged` / `#sumPaymentAmount_…` / `#export_…` 三例。

### 五、不填、重置与离场返回

| # | 场景标题 | 覆盖 FR | 落地方式 | 实际用例名 | 状态 |
|---|---|---|---|---|---|
| 5.1 | 不填开票日期时，台账与本次改造前一模一样 | FR-013 | 🧪 + 🌐 | 🧪 `inv6_noBounds_rendersNoInvoiceDatePredicate`<br>🧪 `inv4_emptyStringDoesNotTrigger`（空串 `""` 也不渲染）<br>🧪 `#listWithContracts_nullSubmitAcceptanceDates_keysAbsentFromParams` / `#sumPaymentAmount_null…` / `#export_null…`<br>🧪 `#listWithContracts_emptySubmitAcceptanceDates_keysPresentAsEmptyString`（**空串会进 params，由 `<if>` 的 `!= ''` 挡住 —— 两层职责的分界点**）<br>🌐 `:335` 不填开票日期：6 条里程碑（含未开票那条）全部在列表里，合计为全量<br>🌐 `:341` 开票日期传空串等同于不传：条件不生效，台账保持全量 | ✅ 单测绿<br>✅ **e2e 实测通过（97ms / 98ms）** |
| 5.2 | 「重置」把开票日期条件清干净 | FR-016 | 🖱️ 仅 UI e2e | `:708` 「重置」把开票日期条件清干净，下一次请求不再带该参数 | ✅ **实测通过（4.8s）** |
| 5.3 | **进详情再回列表，条件还在且下一次查询仍然生效** | **FR-015** | 🖱️ 仅 UI e2e | **`:742` 进详情再回列表：开票日期条件保留，且重建后的请求仍带该参数** | ✅ **实测通过（8.1s）** |
| 5.4 ➕ | **（新增，`.feature` 里没有对应场景）选了日期不点查询直接导出** | FR-006 / FR-015 同源 | 🖱️ 仅 UI e2e | **`:810` 选了日期不点查询直接导出：导出请求仍带开票日期与实际回款日期条件** | ✅ **实测通过（5.6s）**<br>D-5 缺陷修复已被实测锁死 |

> **5.1 的空串处理是两层职责，不要合并理解。**
> `PaymentController` 的判据是 `if (x != null)` —— 所以**空串会被放进 `params`**
> （`#listWithContracts_emptySubmitAcceptanceDates_keysPresentAsEmptyString` 就是钉这个的）；
> 真正挡住空串的是 mapper `<if>` 里的 `!= ''`（`inv4_emptyStringDoesNotTrigger`）。
> `<if>` 若漏写 `!= ''`，空串会渲染成 `>= ''`，MySQL 隐式转换后把结果集静默改变且不报错。
> 变异 **M4 / M4b**（摘掉 `!= ''`）实测**红**，两种风格的语句都覆盖到了。
>
> **5.3 的真判据是「重建后的请求仍带参数」，不是「控件里看着有值」。**
> 前端的区间值存在**独立 ref**（不在 `queryParams` 里），发请求前才同步。
> 搜索状态缓存若只还原 `queryParams` 而漏存这个 ref，返回列表后的第一次 `getList` 会
> **当场把刚还原的条件抹成 null** —— 表现为「控件里看着有值、结果却是全量」。
> **只断言控件值会完整地漏掉这条主缺陷**，`:742` 两条都断言了。
>
> **5.4 是本次实现过程中新发现并修复的缺陷**，`.feature` 里**没有**对应的 Gherkin 场景
> （设计阶段没想到「导出」也是 `queryParams` 的一个消费出口）。详见 §七 D-5。

---

## 三、FR → 场景 → 执行层 反查表（每个 FR 都必须有归属）

| FR | 要求摘要 | 场景 | 单测（🧪，✅ 已绿） | E2E（🌐/🖱️，✅ **全部实测通过**） |
|---|---|---|---|---|
| **FR-001** | 查询区新增控件，形态与「实际回款日期」一致，置于「更多」 | 1.1 | — | 🖱️ `:664` |
| **FR-002** | 参数名 `submitAcceptanceDateStart/End`，不得改名 | 1.2 | 🧪 全部 13 例（断言里写死 `#{params.submitAcceptanceDateStart}` 的渲染形状）+ `PaymentControllerTest` 三处各 1 例 | 🖱️ `:686` |
| **FR-003** | 闭区间，含端点 | 2.1 / 2.2a / 2.2b | 🧪 `inv1_*`（形状里含 `>=` / `<=`，不含 `>` / `<`） | 🌐 `:354` / `:388` ×2 |
| **FR-004** | 列表按条件过滤 | 2.1 | 🧪 `inv1_contractList_rendersBareColumnComparison` | 🌐 `:354` / `:365` |
| **FR-005** | **底部合计同口径** | **3.1** | 🧪 `inv1_contractSum_*` + `inv3_contractSum_fragmentInsideWhereClause` | 🌐 **`:501`** / `:488` |
| **FR-006** | 导出同口径 | 3.2 / 5.4 | 🧪 `export_*` 四例 | 🌐 `:521`（**判据已降级**） + 🖱️ `:810` |
| **FR-007** | `selectPaymentList` 支持同名 params | 3.3 | 🧪 `inv1_paymentList_*` + `inv2_paymentList_*` | 🌐 `:548` |
| **FR-008** | 仅填开始 → 只压下界 | 2.3 | 🧪 `inv5_startOnly_rendersOnlyLowerBound` | 🌐 `:400` |
| **FR-009** | 仅填结束 → 只压上界 | 2.4 | 🧪 `inv5_endOnly_rendersOnlyUpperBound` | 🌐 `:410` |
| **FR-010** | 起止相等 → 精确单日 | 2.5 | — | 🌐 `:423` |
| **FR-011** | 起 > 止 → 空结果、合计 0、不报错 | 2.7 | — | 🌐 `:465` |
| **FR-012** | 空开票日期行在任一端有值时不出现 | 2.6 | — | 🌐 `:445` ×4 + `:457` 反证 |
| **FR-013** | 两端为空 → 不追加条件，结果与上线前一致 | 5.1 | 🧪 `inv6_noBounds_*` / `inv4_emptyStringDoesNotTrigger` / `*_null…KeysAbsent` ×3 | 🌐 `:335` / `:341` |
| **FR-014** | 与其余条件 AND；与回款区间互不干扰 | 4.1~4.4 | 🧪 `inv7_coexistsWithActualPaymentDateFilter` + `*_bothDateRangesCoexistInParams` ×3 | 🌐 `:568` / `:598`✅**未 skip** / `:617`✅**未 skip** / `:638` |
| **FR-015** | **搜索状态缓存必须存独立 ref** | **5.3** | — | 🖱️ **`:742`** |
| **FR-016** | 重置清空 ref + 清缓存条目 | 5.2 | — | 🖱️ `:708` |

**16 个 FR 全部有归属，无空白，且全部有实测证据。**

其中 **FR-010 / FR-011 / FR-012 / FR-015 / FR-016 / FR-001 六条单测层原理上够不到**
（真实 SQL 语义 / 前端状态），**只能由 e2e 证**。上一版这六条是「设计上有覆盖、事实上零证据」；
✅ **本次 e2e 已真跑，这六条各自的用例全部通过，零证据状态解除**：

| FR | 唯一证据来源 | 实测 |
|---|---|---|
| FR-001 | 🖱️ `:664` | ✅ 通过（8.5s） |
| FR-010 | 🌐 `:423` | ✅ 通过（81ms） |
| FR-011 | 🌐 `:465` | ✅ 通过（89ms） |
| FR-012 | 🌐 `:445` ×4 + `:457` 反证 | ✅ 5 例全通过（54/54/52/45ms + 119ms） |
| FR-015 | 🖱️ `:742`（+ `:810`） | ✅ 通过（8.1s / 5.6s） |
| FR-016 | 🖱️ `:708` | ✅ 通过（4.8s） |

**FR-005 / FR-007 两条单测与 e2e 缺一不可**（单测证「那条独立 SQL 也改了」，e2e 证「改对了、口径真的一致」）
—— ✅ 两条的 e2e 侧（`:501` 285ms / `:548` 225ms）本次均已实测通过；
FR-007 因此从「只有单测证明 SQL 改了、Spring Map 绑定未验证」升级为**端到端已验证**（见 §二 3.3 注）。

---

## 四、不变式（INV）与既有回归套件的守护关系

> ⚠️ **两套 INV 编号并存，不要混用。** `spec.md §五` 的 INV-1~INV-7 讲的是**特性边界约束**
> （「既有行不得动」「不得改 DDL」等）；`PaymentInvoiceDateFilterSqlTest` 的
> Javadoc/`@DisplayName` 里另有一套 INV-1~INV-7，讲的是**该测试类自己的断言维度**
> （形状 / 风格 / 位置 / 空串 / 单端 / 无参 / 共存）。**两套同号不同义**，这是设计与实现各自演进留下的编号碰撞。
> 本表左列的 INV 编号沿用 **spec.md** 的口径，用例名后括注测试类自己的编号。

| 套件 / 手段 | 守的 INV（spec.md 口径） | 变红意味着 | 状态 |
|---|---|---|---|
| 🛡️ `tests/e2e-sort-state-persist.spec.js`（`:457` / `:485-488`）<br>🧪 `#listWithContracts_actualPaymentDateBehaviorUnchanged` / `#sumPaymentAmount_actualPaymentDateBehaviorUnchanged` / `#export_actualPaymentDateBehaviorUnchanged` | **INV-1**（既有「实际回款日期」一行不动） | 参数名、缓存、排序被动过 —— 本特性是对称补齐 | ✅ 单测绿<br>✅ **e2e 实测通过**：`e2e-sort-state-persist.spec.js:407`「付款里程碑：「更多」区筛选 + 实际回款日期范围 + 排序 一并恢复且展开态可见」**9.6s 通过** —— 直接证明把 `getList()` 内联的日期反写逻辑抽成 `syncDateRangesToQuery()`（D-5）**零行为漂移** |
| 🧪 `inv2_contractStatements_mustNotUseDateFormatStyle`（测试类 INV-2 反向）<br>🧪 `inv2_paymentList_mustUseDateFormatStyle`（测试类 INV-2 正向） | **INV-2**（同文件内只有一种日期写法） | 风格串味：`ContractMapper` 里写了 `date_format`，或 `PaymentMapper` 里退化成裸列 | ✅ 绿；变异 **M2 / M2b / M6** 实测红 |
| 🧪 `inv1_contractList_*` 与 `inv1_contractSum_*` 断言**同一个** `BARE_GE`/`BARE_LE` 常量 | **INV-3**（两条镜像 SQL 的 `<where>` 对称） | 对称被破坏 —— 这是 FR-005 失守的前兆 | ✅ 绿；变异 **M1 / M5** 实测红 |
| 🧪 `inv3_fragmentRenderedBeforeDataScope`（测试类 INV-3，用 `indexOf` 比较位置，并**先断言哨兵确已渲染**以避免假红）<br>🧪 `inv3_contractSum_fragmentInsideWhereClause`（`sumPaymentAmount` 无 dataScope 的变体） | **INV-4**（`${params.dataScope}` 位置与语义不变） | 新条件被插到 `${params.dataScope}` **之后** —— 位置错乱在权限片段带 `OR` 顶层分支时会放大数据可见范围 | ✅ 绿；变异 **M3** 实测红 |
| 🛡️ `tests/payment-clear-field-regression.spec.js`<br>🛡️ `tests/clear-field-guards-regression.spec.js`（付款段） | **INV-5**（不碰 `insert`/`update`/DDL/字典） | `PaymentMapper.xml:172` 的 `submit_acceptance_date = #{...}` 被「顺手」补上 `<if>` 守卫 —— 那是 Issue #7/#10 治理后的**正确形态**，补回去等于把已修复四次的缺陷改回来 | ✅ **实测全绿**：`payment-clear-field-regression.spec.js` **5 例全通过** —— 证明没有给 `PaymentMapper` 的 update 语句误加 `<if>` 守卫 |
| 🛡️ `tests/e2e-payment-crud.spec.js` | **INV-6**（分页 / 排序不变） | 付款里程碑 CRUD 主流程被影响 | ✅ **实测全绿**（并入 `72 passed` 批次） |
| 🛡️ `tests/payment-status-filter-regression.spec.js` | **INV-7**（其余筛选不受影响） | 付款状态多选、季度多选、部门 ancestors 下钻被新条件干扰 | ✅ **实测全绿**（并入 `72 passed` 批次） |
| 🧪 `trapRegistry_bareColumnNameIsAlwaysPresent` | （不对应 spec INV，是**方法论防线**） | 有人把断言「简化」成裸列名 `contains` —— 该用例现场证明那样恒真 | ✅ 绿 |
| 🔍 `git diff` 自检 | INV-1 / OUT-6 | diff 内**出现对既有 `actualPaymentDate` 行的任何修改** = 越界 | ✅ 已核（本次 diff 对既有行零修改） |

### 变异验证结论（8 个变异体，全部实测变红）

「先红后绿」的真实证据。每个变异体都是**手动改坏实现 → 跑单测 → 确认红 → 还原**：

| 变异体 | 改法 | 应红的用例 | 实测 |
|---|---|---|---|
| **M1** | `selectContractWithPaymentsList` 删掉 `submitAcceptanceDateStart` 的整行 `<if>` | `inv1_contractList_*` / `inv5_*` | ✅ 红 |
| **M2** | 🔴**最关键** `selectPaymentList` 把 `date_format(…) >= date_format(?)` 改成裸列 `p.submit_acceptance_date >= ?`（模拟风格串味） | `inv1_paymentList_*` / `inv2_paymentList_*` | ✅ 红 |
| **M2b** | 同上但 `>=` `<=` 两条都改，单独验证 `inv2_paymentList` 的 `assertContains(FMT_MARKER)` 那一半也是活断言 | `inv2_paymentList_mustUseDateFormatStyle` | ✅ 红 |
| **M3** | `selectContractWithPaymentsList` 把两个新 `<if>` 整体挪到 `${params.dataScope}` **之后** | `inv3_fragmentRenderedBeforeDataScope` | ✅ 红 |
| **M4** | `selectContractWithPaymentsList` 的 Start `<if>` 去掉 `!= ''` 只留 `!= null` | `inv4_emptyStringDoesNotTrigger` | ✅ 红 |
| **M4b** | 同样的 `!= ''` 摘除做到 `PaymentMapper` 的 End 上，验证 INV-4 对 `date_format` 风格那条也有效 | `inv4_emptyStringDoesNotTrigger` | ✅ 红 |
| **M5** | 删掉 `sumPaymentAmount` 里 `submitAcceptanceDateEnd` 的整行 `<if>`（**FR-005 的探针**） | `inv1_contractSum_*` | ✅ 红 |
| **M6** | 把 `selectContractWithPaymentsList` 的两个新 `<if>` 改成 `date_format` 风格（**反向串味**） | `inv2_contractStatements_mustNotUseDateFormatStyle` | ✅ 红 |
| **M7** | 把 `selectContractWithPaymentsList` 的 Start `<if>` 改成 `test="true"`（无条件渲染） | `inv6_noBounds_*` / `inv4_*` | ✅ 红 |

`PaymentControllerTest` 的 15 例同样通过了变异验证（真能红）。

**结论：单测层不是废断言，两条风格、三条语句、位置约束、空串守卫、无条件渲染五个方向都有活探针。**

---

## 五、已知覆盖盲区（必须与结论一起读）

> 这一节记的是**这套测试原理上证不到的东西**。不写出来，上面的 ✅ 会被读成比实际更强的保证。

### 盲区 1 🔴 —— 「合计与列表口径一致」这个保证**只对 admin 成立**

**事实**（`ContractMapper.xml` 逐行核对）：

| 语句 | 有无 `${params.dataScope}` | 行号 |
|---|---|---|
| `selectContractWithPaymentsList`（列表 + 导出共用） | ✅ **有** | `:424` |
| `sumPaymentAmount`（页脚合计） | ❌ **没有** | `<where>` 从 `c.del_flag` 一路到 `</where>` 都没有 |

Service 层带 `@DataScope`，所以**同一个页面上**：列表被部门数据权限收窄，底部合计**不被收窄**。
对部门数据权限用户，页面会呈现「列表 3 行、合计却是全公司金额」。

**这是既有缺陷，早于本特性，且不在 Issue #36 范围内。** 实现里已在
`PaymentInvoiceDateFilterSqlTest` 的 Javadoc 与 `inv3_contractSum_fragmentInsideWhereClause`
的注释中如实登记，**不做修改也不做掩饰**；该用例还额外断言「哨兵确实不在」，
将来若有人给 `sumPaymentAmount` 补上 dataScope，用例会红并提示把它合并回上一条。

**为什么这套 e2e 永远测不出它**：`tests/helpers/api-client.js:17` 的
`setupApi(username = 'admin', password = '123456789')` 把账号写死成 admin，
而 admin 的数据权限是「全部」——两条语句在 admin 下渲染出的可见范围本来就相同。
**换句话说，`:501`「合计 == 列表全量之和」这条断言，在 admin 下必然绿，与实现对不对无关的那一部分风险它照不到。**

要覆盖它需要新建一个受限数据权限的测试账号并让 `setupApi` 支持切换 —— 属独立 Issue，本次不做。

> 🔴 **本次实测未改变这一结论。** `28/28 通过` 是在 **admin 视角**下取得的
> （`tests/helpers/api-client.js:17` 把账号写死成 `admin`，数据权限为「全部」）。
> 因此本次的绿灯**没有**、也**不可能**证明部门数据权限用户下「合计 == 列表」成立。
> 引用 `:501` 的结论时必须一并写「仅对 admin 成立」。

### 盲区 2 ✅ 已消除 —— e2e 已执行（原文保留供对照）

**原文（第二版）**：「e2e 尚未执行。FR-010 / FR-011 / FR-012 / FR-015 / FR-016 / FR-001 六条目前零执行证据。」

**现状（第三版）**：✅ **该盲区已消除。** e2e 28 例真跑，`28 passed (42.4s)`，零 skip；
上述六条各自的用例逐条通过（明细见 §三 的表格）。执行环境、命令、结果行与数据库取证见
[§八 实测执行记录](#八实测执行记录2026-08-07)。

⚠️ **但两条限定条件必须一起读**：① 执行发生在**本地 worktree 环境**（本地 docker MySQL/Redis），
不等同于生产库；② 全程 **admin 账号**，见盲区 1。

### 盲区 3 —— 导出的**内容**未逐行校验（**仍然成立，本次未消除**）

见 §七 D-3。`:521` 用字节数单调性代替行数比对，能抓「导出完全没筛」，
抓不到「筛错了但行数恰好也变了」。
**本次 `:521` 实测通过（2.9s）只说明"降级后的判据成立"，不说明导出内容逐行正确。**

### 盲区 4 —— 前端无单测地基（**结构性问题仍然成立**）

`payment/index.vue` 的所有逻辑（含本次新抽出的 `syncDateRangesToQuery()`）
只有 UI e2e 一层防线，没有 vitest/jest（Issue #15）。

✅ **本次 UI e2e 已跑（5 例全通过 + `e2e-sort-state-persist.spec.js:407` 9.6s 通过）**，
所以「UI e2e 不跑 = 前端零覆盖」这个当时的实然状态已不成立。
**但结构性风险没变**：这一层是**唯一**防线，它一旦不跑（比如 CI 里没有 Playwright、
或本地忘了起前端），前端立刻回到零覆盖 —— 没有任何更快的下层网兜底。

---

## 六、判定「BDD 测试通过」的命令

```bash
# ── 后端单测（无需 MySQL/Redis）──
mvn test -pl ruoyi-project -am -Dtest=PaymentInvoiceDateFilterSqlTest -Dsurefire.failIfNoSpecifiedTests=false
#   ✅ 判据：输出里出现  Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
mvn test -pl ruoyi-project -am -Dtest=PaymentControllerTest -Dsurefire.failIfNoSpecifiedTests=false
#   ✅ 判据：输出里出现  Tests run: 15, Failures: 0, Errors: 0, Skipped: 0

# 全模块回归（不得因本次改动新增红灯，与基线数比对）
mvn test -pl ruoyi-project -am
#   ✅ 实测（2026-08-07）：Tests run: 297, Failures: 0, Errors: 0, Skipped: 0
```

> 🔴🔴 **`-Dsurefire.failIfNoSpecifiedTests=false` 会把「测试类不存在」也变成 BUILD SUCCESS。**
>
> 这个 flag **是必须的** —— `-am` 会连带构建 `ruoyi-common` 等没有测试的模块，不加就会因
> 「No tests were executed」而失败。但它的代价是：**类名打错、文件没建、包路径不对、
> 整个类被 `@Disabled`，全都表现为 BUILD SUCCESS + 跑了 0 个用例。**
>
> 本特性的 coverage.md 第一版正是栽在这里：它声称 `PaymentInvoiceDateFilterSqlTest` 已存在（T1~T8 约 21 例），
> 而文件根本没建，验收命令照样绿。
>
> ### ✅ 正确的验收判据
>
> **看 `Tests run: N` 那一行，确认 N 等于上面写明的预期数（13 / 15）。**
> **不是**「BUILD SUCCESS」，**不是**「没有报错」，**不是** agent 说跑过了。
>
> 一行命令拿到判据：
> ```bash
> mvn test -pl ruoyi-project -am -Dtest=PaymentInvoiceDateFilterSqlTest \
>          -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "Tests run:.*PaymentInvoiceDateFilterSqlTest|^\[INFO\] Tests run:"
> ```
> `N=0` 或没有这一行 = **没跑**，不管 BUILD 是什么颜色。

```bash
# ── E2E ──
# 前置：后端 + 前端（默认 http://localhost:80，可用 E2E_BASE_URL 覆盖）均已启动；
#       UI 用例需临时关闭登录验证码（sys.account.captchaEnabled=false），跑完恢复。
npx playwright test tests/e2e-payment-invoice-date-filter.spec.js --reporter=list
#   ✅ 判据：Total 28 passed，且 4.2 / 4.3 两条【不是 skipped】
#      跑前先枚举确认数量：npx playwright test <file> --list  →  Total: 28 tests in 1 file
#   ✅ 实测（2026-08-07）：28 passed (42.4s)，零 skip

# 必须保持绿的既有回归
npx playwright test e2e-sort-state-persist.spec.js payment-status-filter-regression.spec.js \
                    e2e-payment-crud.spec.js payment-clear-field-regression.spec.js \
                    clear-field-guards-regression.spec.js
#   ✅ 实测（2026-08-07）：72 passed (1.6m)

# ── 静态检查 ──
mvn clean compile -pl ruoyi-project -am
cd ruoyi-ui && npx vue-tsc --noEmit    # 判据：payment/index.vue 零错误（全局存量基线约 39 错不作门槛）
```

**当前验收结论（第三版，实测后）**：
- ✅ 后端单测 28 例（13 + 15）全绿，且 8 个变异体逐一实测变红——**这一层是真覆盖，不是空头支票**。
  全模块 `mvn test -pl ruoyi-project -am` 实测 **297 全绿**（Failures 0 / Errors 0 / Skipped 0）。
- ✅ **e2e 28 例已真跑：`28 passed (42.4s)`，零 skip**（含此前预警"可能 skip"的 4.2 / 4.3 两条）。
  按 §〇 的痕迹判据核实过：`pm_contract` 软删痕迹在、`sys_oper_log` 导出记录在、自造里程碑已清理。
- ✅ **既有回归 72 例全绿**（`72 passed (1.6m)`），无新增红灯。
- 🔴 盲区 1（合计无 dataScope）**已知未修**，任何「口径一致」的结论都必须附带「仅对 admin 成立」
  —— **本次 28/28 正是在 admin 视角下取得的，不构成对该盲区的反驳**。
- ⚠️ 盲区 3（导出内容未逐行校验）**仍然成立**，`:521` 是降级判据。

---

## 七、设计与实现的偏离登记

> 「如实标注偏离」是硬性要求。以下每条都写清：**设计意图是什么 / 实际怎么做的 / 为什么**。
> 偏离本身不一定是坏事 —— D-1、D-2、D-5 三条实现**优于**设计。

### D-0 🔴 第一版 coverage.md 是空头支票（**流程缺陷，最严重的一条**）

- **声称**：`PaymentInvoiceDateFilterSqlTest`（T1~T8，约 21 例）作为用例载体已列在 §二。
- **实际**：写这份文档时该文件**不存在**；19 行状态全是「🔴 待落地」，§五 四行统计全是「待填」，
  而文档第 6-8 行自己白纸黑字写着「实现完成后必须回填实测状态与实际用例名」——**从未执行**。
- **放大器**：验收命令带 `-Dsurefire.failIfNoSpecifiedTests=false`，文件不存在也 BUILD SUCCESS。
- **修法**：本次流水线补齐了该测试类（13 例，8 个变异体验证），并把验收判据从「BUILD SUCCESS」
  改为「`Tests run: N` 的 N 等于预期数」（§六）。
- **教训**：**声称的用例名必须能被 `grep` 到才算数。** 回填时逐个核对的命令：
  `grep -n "void " <测试文件>` 与 `npx playwright test <spec> --list`。

### D-1 ✅ 单测用例名与编号体系全部重来（实现命名更可追溯）

- **设计**：`T1`~`T8` 八个编号，`T8` 表示「T1~T7 对三条语句各跑一遍」（`@ParameterizedTest` 或三份 `@Nested`）。
- **实际**：13 个方法，名字直接编码所守的不变式 —— `inv1_`~`inv7_` + `trapRegistry_`；
  「三条语句各跑一遍」不是一个独立编号，而是**在每个方法内部对三条语句循环断言**。
- **为什么**：`T8` 那种「对前七条再乘三」的编号在失败时无法定位（报 `T8` 你不知道是哪条语句哪个维度）；
  按不变式命名的方法失败时直接读出「INV-3 位置约束在 sumPaymentAmount 上破了」。
- **代价**：**`T1`~`T8` 这套编号在 plan.md 里仍在用**，读 plan 时需按本节换算。

### D-2 ✅ e2e 改为自造确定数据集，不再探测库中真实极值（**优于设计**）

- **设计**（旧 §六 第 206 行注）：「数据驱动，不硬编码日期」——先探测库里真实的最小/最大开票日期、
  真实存在的某一天 D、真实存在的空开票日期行，再据此构造断言；探测不到就 `skip`。
- **实际**：`beforeAll` 自建 1 个带唯一标记（`INV${Date.now()}`）的合同 + **6 条付款里程碑**，
  开票日期**硬编码** `2026-03-01` / `03-15` / `03-31` / `04-01` + 一条 NULL；
  金额取 2 的幂（100/200/400/800/1600/3200，任意子集和唯一）；所有查询带 `contractName=<唯一合同名>`
  收敛范围；`afterAll` 逐条删除。
- **为什么实际的更好**：
  1. **断言可以是精确集合而不是不等式。** 探测式只能断言「每行都 ≥ start」；自造数据能断言
     「命中集合恰好是 {A,B,C,F}」，把「多筛了」和「少筛了」一并抓住。
  2. **金额取 2 的幂 → 合计能反推命中集合**，`:501` 的「合计 == 命中集合金额之和」
     才成为 FR-005 的**充分**判据（探测式下合计只能做「变小了」这种弱断言）。
  3. **NULL 行自己造，2.6 不再需要 skip。** 设计里 2.6 依赖「库里恰好有空开票日期数据」，
     探测不到就整条降级 —— 换个环境覆盖度就变了。自造数据下它是确定覆盖。
  4. **不依赖库内容 = 跨环境可重复。** 探测式在空库、在生产、在同事机器上跑出的是三种不同的测试。
- **残留代价**：写死 2026 年的日期在遥远未来仍可读，但若哪天有人把 seed 日期改成「今天附近」，
  边界断言会跟着漂 —— 因此 seed 常量集中在文件顶部并标注了每条的用途。
- **口径已对齐**：旧文档那句「数据驱动，不硬编码日期」**作废**，以本节为准。

### D-3 ⚠️ 导出的判据降级：字节数单调性，不是行数比对

- **设计**（SC-005 / 旧 3.2）：「导出行数 == 列表无分页全量行数」。
- **实际**（`:521`）：三次导出（全量 / 3 月区间 / 空集），断言
  ① 均 HTTP 200 且 content-type 是 Excel 二进制、② 响应非空、
  ③ **字节数单调递增：空集 < 3 月区间(4 条) < 全量(6 条)**。
- **为什么**：不引入 `xlsx` 解析依赖就无法逐行读取 Excel 内容。
- **降级的后果（如实说明）**：这个判据能抓住「导出根本没加开票日期条件」（三者字节数会完全相同），
  **抓不到**「筛错了但行数恰好也变了」。属 §五 盲区 3。
- 设计文档说「若不可解析行数则降级并在报告里写明降级理由」——**这就是那份说明**。

### D-4 ⚠️ INV 编号在 spec.md 与测试类之间发生碰撞

- **spec.md §五** 的 INV-1~INV-7 = 特性边界约束（既有行不动 / 写法统一 / 镜像对称 / dataScope 位置 /
  不碰 update / 分页排序 / 其余筛选）。
- **`PaymentInvoiceDateFilterSqlTest`** 的 `@DisplayName` 里另有一套 INV-1~INV-7 =
  该测试类的断言维度（形状 / 风格 / 位置 / 空串 / 单端 / 无参 / 共存）。
- **两套同号不同义**：例如 spec 的 INV-4 是「dataScope 位置」，测试类的 INV-4 是「空串不触发」；
  测试类的 INV-3 才是「dataScope 位置」。
- **现状**：两套都已落地，改名会同时动测试文件与 spec，风险大于收益。
  §四 表格已显式标注换算关系；**读任何 INV-x 前先确认是哪一套**。

### D-5 ✅ 新发现并修复的缺陷：导出漏同步日期区间（`.feature` 无对应场景）

- **缺陷**：`payment/index.vue` 的两个日期区间是**双副本状态** —— 真值是 `el-date-picker`
  绑定的独立 ref（`actualPaymentDateRange` / `submitAcceptanceDateRange`），
  `queryParams` 里的 `xxxDateStart/End` 只是**派生快照**，没有任何 watch 维护。
  历史实现里只有 `getList()` 会刷新它；`handleExport()` 直接展开 `queryParams`。
  于是「**选日期 → 不点查询 → 直接点导出**」这条路径导出的是**全量台账**，
  界面上却明明摆着筛选条件 —— 财务据此对账会拿到错误口径。**开票日期与实际回款日期两个字段都中招。**
- **修法**（`ruoyi-ui/src/views/project/payment/index.vue`）：
  - `:454-480` 新增 `syncDateRangesToQuery()`：把原先内联在 `getList()` 里的两段
    「区间 ref → queryParams 派生快照」覆写逻辑**原样抽出**（`length===2` 取值、否则置 `null`
    的判断逐字未动，`includes null` / 非数组的短路守卫也保留）。函数头注释写明双副本状态的真值归属。
  - `:482-489` `getList()` 的两段内联覆写替换为一行调用，位置仍在
    `listPaymentWithContracts` / `sumPaymentAmount` 之前，**行为逐字不变**（回归底线）。
  - `:867-877` `handleExport()` 在展开 `queryParams` **之前**先调用它，缺陷即修。
- **回归用例**：`tests/e2e-payment-invoice-date-filter.spec.js:802-846` 在既有
  `describe('查询区交互')` 末尾**追加**一条（既有 27 例一行未动）。
  用 `page.waitForRequest` 拦截 `POST /project/payment/export`，把 `postData` 前补一个 `?`
  后复用既有 `paramOf()` helper，同一条用例内断言 **4 个**参数
  （`submitAcceptanceDateStart/End` + `actualPaymentDateStart/End`）。
  复用既有 `gotoList` / `expandMore` / `fillDateRange` helper，**未引入 `import.meta`**
  （根 `package.json` 无 `type:module`，含 `import.meta` 的 `.js` spec 无法被 Playwright 转译）。
- **实测（2026-08-07）**：✅ `:810` 通过（5.6s），缺陷修复被锁死；
  同时 🛡️ `tests/e2e-sort-state-persist.spec.js:407` 通过（9.6s），
  **证明「把 `getList()` 内联的日期反写逻辑抽成 `syncDateRangesToQuery()`」这次重构零行为漂移**
  —— 这是本次改动里唯一动了既有代码路径的地方，它的守门人就是这条既有用例。
- **文档偏差**：`.feature` 里**没有**对应的 Gherkin 场景（设计阶段没意识到「导出」也是
  `queryParams` 的一个消费出口）。本文件 §二 以 **5.4 ➕** 登记，`.feature` 保持 19 个场景不变
  —— 场景文件是评审快照，不追改；覆盖矩阵才是真值。

### D-6 ℹ️ 覆盖统计的计划值与实际值全部对不上（无害，但记录在案）

| 执行层 | 第一版计划 | 实际 | 差异说明 |
|---|---|---|---|
| JUnit SQL 渲染 | 「T1~T8 × 3 条语句 ≈ 21 例」 | **13 例** | 「× 3 条语句」在实现里收进了方法内部循环，不再乘出用例数 |
| JUnit Controller | **4 例**（三处 plumbing + null 不入 params） | **15 例** | 实现把三个出口 × {写入 / null 不写 / 空串写入 / 单端 / 双区间共存 / 既有行为不变} 展开 |
| Playwright API | 约 12 例 | **23 例，✅ 23/23 通过** | 2.6 展开 4 例 + 反证 1 例、2.2 展开 2 例、组合筛选 4 例等 |
| Playwright UI | 4 例 | **5 例，✅ 5/5 通过** | 新增 D-5 的导出缺陷回归 |
| 既有回归套件 | 5 个文件全绿 | 5 个文件 **72 例，✅ 72/72 通过** | 计划里没写例数；实测 `72 passed (1.6m)` |

---

## 八、实测执行记录（2026-08-07）

> 这一节是「跑过了」这句话的全部依据。写成**可照着复现**的形式：环境 → 命令 → 结果行 → 取证。
> 任何人重跑一遍应能得到同样的数字；对不上就说明环境或实现变了，**不要直接沿用本节结论**。

### 8.1 执行环境

| 项 | 值 | 备注 |
|---|---|---|
| 代码位置 | worktree `.claude/worktrees/payment-invoice-date-filter` | 未污染主工作区 |
| 后端 | worktree 自行编译的 `ruoyi-admin.jar`，**端口 8080** | `application.yml` 原值，未改动 |
| 前端 | worktree `ruoyi-ui` vite，**端口 5174**（`npm run dev -- --port 5174`） | 避开 019 worktree 占用的 5173；80 端口需 sudo，不用 |
| E2E 入口 | `E2E_BASE_URL=http://localhost:5174` | 必须显式设置，否则回落到默认 baseURL |
| MySQL | docker 容器 `newpm-mysql-1`（3306），库 `ry-vue` | 与主环境共用同一本地 docker 实例 |
| Redis | docker 容器 `newpm-redis-1`（6379） | — |
| 验证码 | `sys.account.captchaEnabled = false` | **本来就是关的，本次未改动，跑完也无需恢复** |
| 测试账号 | `admin`（`tests/helpers/api-client.js:17` 写死） | ⚠️ 数据权限「全部」，见 §五 盲区 1 |

> ⚠️ **端口是最容易踩的一处**：本仓库 E2E 有多套变量约定（`E2E_BASE_URL` / `E2E_API_URL`），
> 设错会**静默回落**到别的地址、打到别的库上，且毫无征兆。
> 复现时务必确认后端 8080、前端 5174 都在跑，再设 `E2E_BASE_URL`。

### 8.2 命令与结果行

**① 本特性 e2e 套件（28 例）**

```bash
E2E_BASE_URL=http://localhost:5174 \
  npx playwright test tests/e2e-payment-invoice-date-filter.spec.js --reporter=list
```

结果行：

```
28 passed (42.4s)
```

套件自带的造数 / 清理日志（stdout）：

```
🌱 造数完成：合同 477（E2E开票日期合同_INV1786109928918），里程碑 436,437,438,439,440,441
🧹 清理完成：自造的合同与付款里程碑已删除
```

**零 skip** —— 此前预警「可能 skip」的两条（`:598` 开票期间+付款状态、`:617` 开票期间+合同所属团队）
均真实执行并通过（111ms / 88ms）。

逐例耗时（用于回填 §二 各行）：

| 用例 | 耗时 | 用例 | 耗时 |
|---|---|---|---|
| `:335` 不填开票日期 | 97ms | `:501` 合计==列表之和 | 285ms |
| `:341` 空串等同不传 | 98ms | `:521` 导出口径 | 2.9s |
| `:354` 完整区间命中 | 62ms | `:548` 扁平清单同口径 | 225ms |
| `:365` 区间外不命中 | 116ms | `:568` +实际回款期间 | 182ms |
| `:388` 端点=起始日 | 60ms | `:598` +付款状态 | 111ms |
| `:388` 端点=结束日 | 65ms | `:617` +合同所属团队 | 88ms |
| `:400` 只填开始 | 80ms | `:638` +合同名称 | 74ms |
| `:410` 只填结束 | 85ms | `:664` 控件并排出现（UI） | 8.5s |
| `:423` 起止同一天 | 81ms | `:686` 参数上行（UI） | 4.3s |
| `:445` 未开票·完整区间 | 54ms | `:708` 重置（UI） | 4.8s |
| `:445` 未开票·仅开始 | 54ms | `:742` 进详情再回列表（UI） | 8.1s |
| `:445` 未开票·仅结束 | 52ms | `:810` 不点查询直接导出（UI） | 5.6s |
| `:445` 未开票·同一天 | 45ms | | |
| `:457` 未开票反证例 | 119ms | | |
| `:465` 起>止 | 89ms | | |
| `:488` 合计跟着变 | 50ms | | |

> 📌 **API 段普遍在 45~285ms，UI 段在 4.3~8.5s** —— 这个量级差本身是个健康信号：
> 若某条 API 用例耗时突然进入秒级，多半是打到了没预期的分页/全表路径，值得看一眼。

**② 既有回归套件（72 例）**

```bash
E2E_BASE_URL=http://localhost:5174 npx playwright test \
  tests/e2e-payment-crud.spec.js \
  tests/payment-status-filter-regression.spec.js \
  tests/payment-clear-field-regression.spec.js \
  tests/clear-field-guards-regression.spec.js \
  tests/e2e-sort-state-persist.spec.js --reporter=list
```

结果行：

```
72 passed (1.6m)
```

两条**守门人**用例的实测意义（不是普通绿灯，是本次改动的定向探针）：

| 用例 | 耗时 | 它证明了什么 |
|---|---|---|
| `e2e-sort-state-persist.spec.js:407`「付款里程碑：「更多」区筛选 + 实际回款日期范围 + 排序 一并恢复且展开态可见」 | 9.6s | 把 `getList()` 内联的日期反写逻辑抽成 `syncDateRangesToQuery()`（D-5）**零行为漂移** |
| `payment-clear-field-regression.spec.js` 全 **5 例** | — | **没有**给 `PaymentMapper` 的 update 语句误加 `<if>` 守卫，Issue #7/#10 治理后的正确形态被保住 |

**③ 后端单测（297 例）**

```bash
mvn test -pl ruoyi-project -am
```

结果行：

```
Tests run: 297, Failures: 0, Errors: 0, Skipped: 0
```

其中本特性两个测试类：`PaymentInvoiceDateFilterSqlTest` **13** 例、`PaymentControllerTest` **15** 例。

**变异验证**：7 条主变异（M1~M7）+ 2 条子变异（M2b / M4b），
**13 条 SQL 渲染用例每一条都至少被一条变异杀死过，零废断言**（明细见 §四）。

### 8.3 数据库取证（证明 e2e 真跑过，而非声称跑过）

在本地 docker MySQL 上执行（命令形态见 `CLAUDE.md`「Running SQL on Remote K3s MySQL / 本地 Docker」）：

```bash
cat verify.sql | docker exec -i newpm-mysql-1 \
  mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue
```

| # | SQL | 实际输出 | 说明 |
|---|---|---|---|
| 1 | `SELECT contract_id,contract_name,del_flag,create_time FROM pm_contract WHERE contract_name LIKE 'E2E开票日期合同%';` | `477 \| E2E开票日期合同_INV1786109928918 \| 1 \| 2026-08-07 21:38:50` | **软删痕迹**：`cleanup()` 走 `DELETE /project/contract/{id}`，合同是软删（`del_flag=1`），痕迹不会消失 —— 这是最稳的「跑过了」探针 |
| 2 | `SELECT COUNT(*) FROM sys_oper_log WHERE oper_url LIKE '%/payment/export%' AND oper_time>=CURDATE();` | `4` | 导出用例**真打到了后端**，`@Log(EXPORT)` 留痕（`:521` 三次 + `:810` 一次 = 4） |
| 3 | `SELECT COUNT(*) FROM pm_payment WHERE payment_id BETWEEN 436 AND 441 AND del_flag='0';` | `0` | `afterAll` 清理干净，**没有给库里留垃圾数据** |

> ✅ 三条**均已独立复核执行**，输出与套件日志完全对得上
> （合同号 477 = 造数日志里的 477；里程碑 436~441 = 造数日志里的 6 个 ID）。
>
> 📖 **为什么用这三条而不是别的**：
> ① 合同软删行**不可被清理抹掉**，是唯一「跑过就永远留痕」的证据；
> ② `sys_oper_log` 证明请求穿透到了**后端**，排除「前端 mock / 断言没真发请求」；
> ③ 里程碑残留数为 0 证明**清理逻辑本身也跑到了**，顺带排除「测试中途崩掉」。
> 三条合起来才能说明「跑完了整条链路」，缺一条都留有解释空间。

### 8.4 本次仍未验证的事项（如实保留）

| 事项 | 状态 | 原因 |
|---|---|---|
| 部门数据权限用户下「合计 == 列表」 | ❌ **未验证** | `setupApi` 写死 admin；这是既有缺陷 + 测试基础设施双重限制，见 §五 盲区 1 |
| 导出 Excel 的**逐行内容** | ❌ **未验证** | 判据降级为字节数单调性，见 §七 D-3 / §五 盲区 3 |
| 生产环境行为 | ❌ **未验证** | 全部执行发生在本地 worktree + 本地 docker 库 |
| 前端逻辑的单测层覆盖 | ❌ **不存在** | 无 vitest/jest 地基（Issue #15），UI e2e 是唯一防线，见 §五 盲区 4 |

---

> **先红后绿是字面要求，本特性的证据在 §四「变异验证结论」。**
> 8 个变异体逐一改坏实现 → 跑单测 → 确认红 → 还原，覆盖两种日期风格、三条语句、
> 位置约束、空串守卫、无条件渲染五个方向。**没跑过红的断言不算 TDD。**
