# BDD 场景覆盖映射

**特性**: `020-contract-code-unique`（合同编号判重，Issue #32）
**场景文件**: [contract-code-unique.feature](./contract-code-unique.feature)
**日期**: 2026-08-06
**阶段**: **BDD 设计阶段产出**（硬性流程第 3 步）——本文件先于 TDD 编写，
下表「状态」列此刻**全部为待落地**，不是实测结果。实现完成后必须回填实测状态与实际用例名，
并把与设计意图不符的地方**如实标注**（照 `specs/018-project-daily-report/bdd/coverage.md` 的「诚实版」写法）。

本项目**无 Cucumber 工具链**，`.feature` 不直接执行。本文件把每个 Gherkin 场景映射到实际可执行的
JUnit 单测与 Playwright e2e，作为「BDD 测试通过」的判定依据。

共 **24 个场景**（其中 3 个场景大纲，展开为 **32 例**）。

---

## 一、执行层选择原则

| 判据 | 执行层 | 理由 |
|---|---|---|
| 纯 Java 逻辑：归一化算法、空值短路、自排除、抛异常、校验顺序、异常转译 | **JUnit**（Mockito mock `ContractMapper`） | 无需 MySQL/Redis，秒级红绿，是 TDD 循环的主战场 |
| 真实 SQL 语义：`TRIM/REPLACE` 归一化、`del_flag` 过滤、`<if>` 守卫、模糊搜索、唯一索引 | **Playwright API e2e** | Mockito 把 Mapper 全 mock 掉，**XML 里的一个字都验不到**。合同编号判重有一半行为活在 SQL 里 |
| 表单交互：失焦内联报错、必填星号、拦截后表单不提交 | **Playwright UI e2e** | 前端无单测地基（无 vitest/jest，见 Issue #15） |
| DDL 约束、并发撞车 | **手工 SQL / 旁证** | 无法在应用层稳定复现，只能实测观测 + 单测覆盖其转译路径 |

> ⚠️ **本特性最容易被误判的一点**：`checkContractCodeUnique` 的单测全绿 ≠ 判重生效。
> 单测里 mock 的是 `selectContractIdsByNormalizedCode` 的返回值，**这条 SQL 写没写对、
> `TRIM()` 去没去掉 TAB、`del_flag='0'` 加没加，单测一律看不见**。SQL 侧的正确性只有 e2e 能证。

---

## 二、场景 → 可执行用例映射

图例：🧪 JUnit ｜ 🌐 Playwright API ｜ 🖱️ Playwright UI ｜ 🔧 手工 SQL ｜ 🛡️ 既有回归（不得改）

### 一、重复编号被当场拦住

| # | 场景标题 | 落地方式 | 对应用例名 | 覆盖状态 |
|---|---|---|---|---|
| 1.1a | 键入已被占用的合同编号时保存被拒绝（**新增**） | 🧪 + 🌐 | `insertContract_duplicateCode_throwsAndSkipsInsert`（T5）<br>`e2e-contract-code-unique.spec.js` → `新增：编号被占用时 code=500 且 msg 含「合同编号已存在」` | 🔴 待落地 |
| 1.1b | 键入已被占用的合同编号时保存被拒绝（**编辑**） | 🧪 + 🌐 | `updateContract_duplicateCode_throwsAndSkipsUpdate`（T6）<br>同上 → `编辑：把编号改成别人的被拒` | 🔴 待落地 |
| 1.2 | 失焦即给出内联反馈，表单不提交 | 🖱️ **仅 UI e2e** | `e2e-contract-code-unique-ui.spec.js` → `新增页编号失焦后显示内联错误且保存被阻止` | 🔴 待落地 |
| 1.3 | 编辑时把编号改成别人的，同样被拒 | 🧪 + 🌐 | 同 1.1b | 🔴 待落地 |
| 1.4 | 拦截是硬的，无「确认后放行」出口 | 🌐 + 代码 diff | `e2e-...spec.js` → `重复编号连续提交两次均被拒（无放行参数）`；另靠 diff 确认未引入任何跳过校验的入参 | 🔴 待落地 |
| 1.5 | 提示是人话，不含堆栈/SQL | 🌐 | 同文件 → `拒绝消息含编号本身且不含 Exception/select 字样` | 🔴 待落地 |
| 1.6 | 提示里带出冲突合同的名称 | — | **暂不落地** | ⏸ **待业务确认** |

> **1.6 为什么不落地**：spec.md §八「错误提示文案未定稿」尚未拍板。已定的下限是「提示含被占用的编号本身」
> （1.5 已覆盖）。带出对方合同名称会与「判重刻意不受数据权限约束」相撞——部门二组会因此看见部门一组的合同名。
> 实现侧已留余地：新增 mapper 方法返回 `List<Long>` 而非 `count`，业务方拍板后不必改 SQL。
> **业务方确认前，本行状态必须保持 ⏸，不得自行补断言把它做成既成事实。**

### 二、不填编号照样能干活

| # | 场景标题 | 落地方式 | 对应用例名 | 覆盖状态 |
|---|---|---|---|---|
| 2.1 | 不填编号可以正常保存 | 🧪 + 🌐 | `insertContract_nullCode_skipsDuplicateQuery`（T8）<br>`e2e-...spec.js` → `空编号合同可新增可编辑` | 🔴 待落地 |
| 2.2 | 多份空编号合同可以并存 | 🌐 **仅 e2e** | 同上 → `连续新增两份空编号合同均成功` | 🔴 待落地 |
| 2.3 | 五种输入等同于"没填"（空 / 空格 / TAB / 空格+TAB / 字面「无」） | 🧪（5 例参数化）+ 🌐（抽 2 例） | `checkContractCodeUnique_blankVariants_returnTrue`（T1，`@ParameterizedTest`）<br>`e2e-...spec.js` → `校验接口对空/空格/TAB/「无」均返回 true` | 🔴 待落地 |
| 2.4 | 清空编号保存后，编号真的被清掉 | 🌐 **仅 e2e** | `e2e-...spec.js` → `清空编号保存后详情回读为空` | 🔴 待落地 |

> **2.4 单测抓不到**：清空失效的根因在 `ContractMapper.xml:267` 的 `<if test="contractCode != null">` 守卫——
> Service 把实体的编号置为 `null` 后照常调 mapper，**mock 的 mapper 会"成功"返回 1**，单测全绿而库里的值纹丝不动。
> CLAUDE.md 记录该类缺陷已复发四次，特征正是「提示保存成功但值不变」。**这条必须由 e2e 回读详情来证。**

### 三、看起来一样的，就是一样的

| # | 场景标题 | 落地方式 | 对应用例名 | 覆盖状态 |
|---|---|---|---|---|
| 3.1 | 只差首尾空白/TAB 仍算同一个编号（4 例） | 🧪（比对入参）+ 🌐（**必需**） | `checkContractCodeUnique_tabAndSpaceVariant_returnsFalse`（T3）<br>`e2e-...spec.js` → `用 \tCODE 再建一次仍被拒` | 🔴 待落地 |
| 3.2 | 中间空格不抹掉，`ABC 001` ≠ `ABC001` | 🧪 | `normalizeContractCode_keepsInnerSpace`（T3 附带） | 🔴 待落地 |
| 3.3 | 落库的编号是干净的 | 🧪 + 🌐 | `insertContract_normalizesCodeBeforePersist`（T7，`ArgumentCaptor` 断言实体值）<br>`e2e-...spec.js` → `提交 \tCODE 后详情回读为干净值` | 🔴 待落地 |

> **3.1 为什么单测不够**：单测只能证明「Java 传给 mapper 的入参已被归一化」。
> 真正的风险在 SQL 侧——**MySQL 的 `TRIM()` 不去 TAB**，库里那条尾部带 TAB 的存量记录
> 只有靠 `REPLACE(...,CHAR(9),'')` 才能被匹配上。这条 SQL 表达式写错了，单测一个都不会红。

### 四、不能误伤正常操作

| # | 场景标题 | 落地方式 | 对应用例名 | 覆盖状态 |
|---|---|---|---|---|
| 4.1 | 前缀不误报（`ABC` vs `ABC-001`） | 🧪 + 🌐 | `checkContractCodeUnique_prefixOfExistingCode_returnsTrue`（T2）<br>`e2e-...spec.js` → `校验接口：前缀不误报` | 🔴 待落地 |
| 4.2 | 编辑不动编号，不被自己拦住 | 🧪 + 🛡️ | `checkContractCodeUnique_selfOnly_returnsTrue`（T4）<br>🛡️ `clear-field-guards-regression.spec.js:577-655`（4 次携带自身编号的全量 PUT 全 200） | 🔴 待落地 + 🛡️ 保绿 |
| 4.3 | 存量每一份合同都还能编辑保存 | 🛡️ 既有回归 | `clear-field-guards-regression.spec.js` + `guard-payload-runtime.spec.js:576-627`（真实 UI 保存能发出 PUT） | 🛡️ 必须保绿 |
| 4.4 | 编号模糊搜索照常可用 | 🛡️ **仅 e2e** | `contract-filter.spec.js:72/93/113`（`toContain(kw)`） | 🛡️ 必须保绿 |
| 4.5 | 删除合同不被编号重复挡住 | 🧪 + 🌐 | `deleteContractByContractIds_duplicateCodes_notBlocked`（verify 未调判重 mapper）<br>`e2e-...spec.js` → `两条同号历史合同都能删除` | 🔴 待落地 |

> **4.4 只能 e2e**：模糊匹配活在 `ContractMapper.xml:9` 的 `like concat('%',...,'%')` 里，
> 被 `selectContractList` 与 `selectContractSummary` 共用。判重若「顺手」把它改成精确匹配，
> 单测毫无反应，`contract-filter.spec.js` 会立刻变红——**它是 INV-1 唯一的探针**。
>
> **4.3 是不变式 INV-2 的探针**：存量 309 条在用合同里有一百多条空编号，
> 若空值放行（FR-4）或自排除（FR-8）任一处写错，这两个既有套件会大面积红。

### 五、删除之后编号可以再用

| # | 场景标题 | 落地方式 | 对应用例名 | 覆盖状态 |
|---|---|---|---|---|
| 5.1 | 已删除合同的编号可以重新使用 | 🌐 **仅 e2e** | `e2e-...spec.js` → `软删 A 后用同编号新建成功` | 🔴 待落地 |
| 5.2 | 被删除的合同仍保留原编号 | 🔧 手工 SQL | 迁移后查 `del_flag='1'` 记录的 `contract_code` 字面值未变（INV-6） | 🔴 待落地 |

> **5.1 只能 e2e**：「软删记录不占用编号」由两处协同保证——判重 SQL 的 `where del_flag='0'`
> 与生成列的 `IF(del_flag='0', ..., NULL)`。两处都在 XML/DDL 里，**Mockito 看不见**。
> 这条同时是 D1（为什么不能用裸唯一索引）的活性验证：若有人把约束改回裸 `UNIQUE(contract_code)`，本用例立刻红。

### 六、跨部门与最后一道兜底

| # | 场景标题 | 落地方式 | 对应用例名 | 覆盖状态 |
|---|---|---|---|---|
| 6.1 | 别的部门录过的编号也能检出 | 🧪（结构证明）+ 🔧（生产观测） | `checkContractCodeUnique_callsMapperDirectly_notDataScopedService`（verify 调的是 `contractMapper.selectContractIdsByNormalizedCode`，**不是** `this.selectContractList`） | 🔴 待落地 |
| 6.2 | 判重不泄露对方合同内容 | 🌐 | `e2e-...spec.js` → `拒绝响应体只含 msg，无金额/客户/签订日期字段` | 🔴 待落地 |
| 6.3 | 直连 SQL 写入被数据库挡住 | 🔧 **仅手工 SQL** | `SHOW CREATE TABLE pm_contract` 含 `uk_contract_code_norm`；手工 INSERT 同归一化编号 → 报 `1062`（SC-009） | 🔴 待落地 |
| 6.4 | 并发同号提交只有一个能成 | 🧪 转译路径 + 旁证 | `insertContract_duplicateKeyException_translatedToServiceException`（D7/FR-20）。**真并发不自动化** | 🔴 待落地（并发本身 ⏸ 不验） |

> **6.1 为什么不做真跨部门 e2e**：需要两个不同部门的账号，而 e2e 统一用 `admin`（数据权限=全部），
> 用 admin 跑跨部门等于什么都没验。**结构证明反而更强**：判重只要不经过带 `@DataScope` 的
> Service 方法，就必然是全局视野。单测用 `verify` 锁死这个调用形态即可——
> 若有人图省事改回 `this.selectContractList(...)`，用例立刻红。生产上线后再做一次真实观测补齐。
>
> **6.4 的并发部分**：本地无稳定复现手段，**如实记为不验**。但它的收尾路径（`1062` → 人话提示）
> 由单测覆盖，不留空白。

---

## 三、执行层归属汇总（TDD 阶段的输入）

### 只能靠 e2e / 手工验证的场景（跨层行为，单测层原理上够不到）

| 场景 | 够不到的原因 |
|---|---|
| 2.2 多份空编号并存 | 依赖真实唯一索引不约束 `NULL` 的特性 |
| 2.4 清空编号真的清空 | 缺陷在 `ContractMapper.xml` 的 `<if>` 守卫，mock 掉 mapper 即失效 |
| 3.1 TAB 差异算重复（SQL 侧） | `MySQL TRIM()` 不去 TAB，表达式对错只有真库知道 |
| 4.3 存量全量可编辑 | 需要 300+ 条真实数据逐条打 |
| 4.4 模糊搜索不变 | like 表达式在 XML 里 |
| 5.1 删除后编号可复用 | `del_flag='0'` 过滤 + 生成列条件，都在 XML/DDL |
| 6.2 不泄露对方合同内容 | 断言的是真实响应体形状 |
| 6.3 数据库兜底约束 | DDL 层，应用层测不到 |
| 1.2 失焦内联报错 | 前端无单测地基 |

### 单测层可以（也应该）覆盖的场景

| 场景 | 对应 plan.md TDD 循环 |
|---|---|
| 2.3 五种空值输入返回"唯一" | T1 |
| 4.1 前缀不误报 | T2 |
| 3.1 / 3.2 归一化比对语义 | T3 |
| 4.2 编辑排除自身 | T4 |
| 1.1a 新增重复抛异常且不落库 | T5 |
| 1.1b/1.3 编辑重复抛异常 | T6 |
| 3.3 归一化值回写实体 | T7 |
| 2.1 空编号短路不查库 | T8 |
| 6.1 判重绕过 `@DataScope`（调用形态） | 新增（D3） |
| 6.4 `1062` 转译为人话 | 新增（D7） |
| 4.5 删除路径不过判重 | 新增（INV-4） |
| 校验顺序在「项目已关联合同」之后 | 新增（FR-13 / D5） |

> **先红后绿是字面要求**：以上每条都必须先跑出失败输出再写实现。
> 另注意既有用例 `ContractServiceImplTest.java:235-246 checkContractCodeUnique_sameLogicAsName`
> mock 的是 `selectContractList`，实现改走新 mapper 后会因 `STRICT_STUBS` 直接红——
> **这是「不改测试就必红」，不要误判成实现有 bug**，须在同一提交内改写 mock 目标。

---

## 四、既有回归套件的守护关系（每条都是设计约束的探针）

| 套件 | 守的场景 | 变红意味着 |
|---|---|---|
| `contract-filter.spec.js` | 4.4 | 模糊匹配被「顺手」改成精确（违反 INV-1） |
| `clear-field-guards-regression.spec.js:577-655` | 4.2 / 4.3 | 自排除逻辑失效，存量合同改不动 |
| `guard-payload-runtime.spec.js:576-627` | 4.3 | 前端 validator 未排除自身 → PUT 根本发不出去（25s 超时） |
| `006-code-review-fixes.spec.js:198-236` | —（校验顺序） | 编号判重被前置，把「已关联其他合同」的提示顶掉（违反 D5/FR-13） |
| `global-string-trim.spec.js:49-95` | 3.3 | normalize 不是全局 trim 的超集，把含空白的合法输入拒掉了（违反 INV-5） |
| `e2e-contract-crud.spec.js:110-135` | — | **本次必改**：它盲取 `rows[0].contractCode` 断言 `toBe(false)`，空编号在新语义下返回 `true` |

---

## 五、覆盖统计（2026-08-06 实跑回填）

| 执行层 | 计划 | 实跑 | 状态 |
|---|---|---|---|
| JUnit（`ContractServiceImplTest`） | 12 | **47 通过 / 0 失败**（基线 20 + 新增 27，参数化展开后 27 条） | ✅ 红→绿闭环：改动前 `47 run / 28 failures`，实现后 `47 / 0` |
| Playwright API（`e2e-contract-code-unique.spec.js`） | 约 10 | **15 通过 / 0 失败** | ✅ 红→绿闭环：实现前 `8 failed / 7 passed`，实现后 `15 passed`；建索引后复跑仍 `15 passed` |
| Playwright UI（`e2e-contract-code-unique-ui.spec.js`） | 1–2 | **0** | ⚠️ **未落地**。L3 前端 validator 无直接 UI 断言，间接守护来自 `guard-payload-runtime.spec.js` 的合同编辑页用例（真实 UI 保存需能发出 PUT） |
| 手工 SQL 验证 | 3 组 | **4 条，全通过** | ✅ 本地库实跑迁移脚本：① 直连插重复编号 → `ERROR 1062 ... uk_contract_code_norm`；② 多条空编号并存成功；③ 仅被软删占用的编号（`41785202403001`）可被复用；④ 复用后再插同编号 → `1062` |
| 既有回归套件 | 6 个文件 | **9 个文件全绿**：首批 API 66 通过；`guard-payload-runtime` 20/20；`e2e-contract-code-sort`+`e2e-sort-state-persist` 23/23；`006`+`contract-filter`+`global-string-trim` 24 通过 / 1 跳过 | ✅ 唯一跳过项是 `006` 的 H5（测试自报「数据不足：需要至少 2 个有关联项目的合同」，本地库数据前置不满足，非改动导致；该场景另由单测 `updateContract_checksProjectRelationBeforeCodeDuplicate` 的 `InOrder` 断言锁定） |

**场景合计 24 条**：自动化覆盖 22 条；1 条（1.6 提示含合同名）经 OQ-4 拍板**判定不落地**，
由反向场景 6.2「判重不泄露对方合同内容」替代守护并已落地断言；1 条（6.4 真并发）如实记为**不验**，
其收尾路径（`1062` → 人话提示）由单测 `insertContract_duplicateKeyException_translatedToServiceException` 覆盖。

### 过程记录：回归一度大面积红，根因是 e2e 地址约定分裂（顺手修掉了）

首轮回归在 worktree 环境（后端 8090，避开另一 worktree 占用的 8080/5173）执行时曾 **12 个失败**，
错误清一色是 `ECONNREFUSED`——根本没进入业务逻辑。定位到全仓 e2e 存在 **5 种互不兼容的地址约定**：
`E2E_BASE_URL`（22 处）、硬编码 `localhost:80`（23 处）、`E2E_API_URL`（8 处）、裸 `localhost`（3 处）、
硬编码 `localhost:5174`（2 处）。

**如何确认与本次改动无关**：`guard-payload-runtime` 的 10 个纯 API「前置」用例**全过**，
10 个 UI 用例全超时，且横跨任务 / 项目 / 收入确认 / 款项 / 合同 / 客户 / 批次版本等 **9 个模块**——
本次只改合同，9 个模块同时挂只可能是环境。补起真实前端后该套件 **20/20 全绿**，确证。

**顺手修掉的技术债**：把 `contract-filter.spec.js` / `006-code-review-fixes.spec.js` /
`global-string-trim.spec.js` 三处硬编码改为 `process.env.E2E_BASE_URL || 'http://localhost:80'`，
**默认值不变、行为不变**，只是多了可覆盖的口子，与仓库主流约定对齐。
改后 `E2E_BASE_URL` 覆盖面 22 → 28 处，这三个套件在任意端口环境下均可执行。

> **INV-1（编号模糊搜索照常可用）现有三重证据**：
> ① `contract-filter.spec.js` 全绿，含「searchForFilter 按合同编号模糊匹配」直接断言；
> ② BDD `[4.4]` API 用例通过；
> ③ `git diff ContractMapper.xml` 中唯一含 `like` 的改动行是**新增注释**，
> 所有模糊匹配语句一字未动，判重走的是独立新增的 `countByContractCode`。

---

## 六、判定「BDD 测试通过」的命令

```bash
# 后端单测（无需 MySQL/Redis）
mvn test -pl ruoyi-project -am -Dtest=ContractServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false
# 全模块回归（数量不得低于改动前基线）
mvn test -pl ruoyi-project -am

# E2E（前端 80 + 后端 8080 在跑，且验证码已关；详见 ../quickstart.md）
npx playwright test e2e-contract-code-unique.spec.js
npx playwright test e2e-contract-code-unique-ui.spec.js

# 必须保持绿的既有回归
npx playwright test contract-filter.spec.js clear-field-guards-regression.spec.js \
                    guard-payload-runtime.spec.js 006-code-review-fixes.spec.js \
                    global-string-trim.spec.js e2e-contract-crud.spec.js
```

**验收标准**：24 个场景中标为「待落地」的全部转绿，⏸ 两条保持原状且理由未变；
`mvn test` 全量数量不低于改动前基线；SC-001 ~ SC-012 逐条勾稽（见 spec.md §六）。
