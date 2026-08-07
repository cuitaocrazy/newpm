# Implementation Plan: 合同编号判重

**特性目录**: `020-contract-code-unique` | **分支**: `fix/contract-code-unique` | **日期**: 2026-08-06
**Spec**: [spec.md](./spec.md) | **Issue**: #32

## Summary

一句话：**给合同编号补上从前端到数据库的四层防线，并让「什么算同一个编号」在 Java、SQL、数据库约束三处用同一把尺子。**

改动面：1 个 Service（3 处）、1 个 Mapper 接口 + 1 条新 SQL、1 处 `<if>` 守卫拆除、2 个 Vue 文件、1 处 DDL 变更（新增条件生成列 + 唯一索引）。**不新增接口、不新增表、不新增依赖、不新增前端组件。**

技术上有五件事，每件都有一个反直觉的坑：

1. **判重不能复用列表查询** —— `ContractMapper.xml:9` 是 `like` 模糊匹配，输 `ABC` 会命中 `ABC-001`。必须另起精确语句。
2. **判重不能走数据权限** —— 实测重复的两条跨部门（201 vs 103），走 `@DataScope` 就永远看不见对方。
3. **唯一索引不能是裸列** —— 软删记录同表参与约束，实测 5 组冲突，`ALTER` 直接失败；且「删掉再用同号重建」这个合法动作会被 `1062` 拒。
4. **MySQL 的 `TRIM()` 不去 TAB**，Java 的 `String.trim()` 去 —— 三处口径必须显式对齐，否则出现「应用放行、数据库 1062」的裂缝。
5. **`ContractMapper.xml:267` 的 `<if>` 守卫会吞掉清空** —— 只改 Service 不改 XML，表现是「新增拦住了，但编辑页清空编号保存后编号还在」，且操作日志里请求体完全正常，极难定位。

---

## Technical Context

| 项 | 值 |
|---|---|
| **语言/版本** | Java 17 / TypeScript 5.6 |
| **主要依赖** | Spring Boot 3.5.8、MyBatis、Vue 3.5、Element Plus 2.13（均为既有，无新增） |
| **存储** | MySQL 8.x (`ry-vue`)。涉及表：**仅 `pm_contract`**。有 DDL 变更（新增虚拟生成列 + 唯一索引） |
| **表规模** | 生产 337 行（在用 309 / 软删 28）—— 量级极小，查询性能不是约束条件 |
| **表排序规则** | `utf8mb4_0900_ai_ci`（`00_tables_ddl.sql:641`）→ 比对**大小写与重音不敏感**（见 D6 / OQ-3） |
| **测试** | JUnit 5 + Mockito（`ruoyi-project`）、Playwright（根 `tests/`） |
| **构建校验** | `mvn clean compile -pl ruoyi-project -am`；`mvn test -pl ruoyi-project -am -Dtest=ContractServiceImplTest`；`cd ruoyi-ui && npx vue-tsc --noEmit`（存量基线约 39 错，判据是本次改动文件零错误） |
| **目标平台** | 既有 K3s 部署（namespace `newpm`），无变化 |

---

## Constitution Check

*GATE：设计前必须通过，设计后复查。依据 `.specify/memory/constitution.md`。*

| 原则 | 相关要求 | 本特性符合性 |
|---|---|---|
| **I. 业务完整性优先** | mutating 接口 MUST 加 `@Log` | ✅ 不新增 mutating 接口；`ContractController` 的 `add`(:86) / `edit`(:97) 既有 `@Log` 不动 |
| | 软删是默认策略 | ✅ 合同继续软删；本特性反而**依赖**软删语义（约束只管在用记录） |
| **II. 权限驱动访问控制** | Controller MUST 有 `@PreAuthorize` | ✅ 无新增 Controller 方法；`checkContractCodeUnique`(`:169`) 既有 `@ss.hasAnyPermi('project:contract:query,add,edit')` 不动 |
| | 部门隔离用 `@DataScope` | ⚠️ **本特性新增的判重查询刻意不经 `@DataScope`**。这是**必需的正确设计**而非违规：唯一性是全局属性，按部门收窄会漏判（实测重复对跨 201/103）。判重语句**只返回布尔/ID**，不吐任何业务字段，不构成越权读取。既有列表查询的数据权限一行不动 |
| **III. API 与代码一致性** | 业务异常抛 `ServiceException` | ✅ FR-9/10/12 全部走 `ServiceException` → `GlobalExceptionHandler` → `AjaxResult.error` |
| | Service 命名约定 | ✅ 新增 mapper 方法沿用 `select*` 前缀（`selectContractIdsByNormalizedCode`） |
| | 前端响应取值 `res.data` | ✅ 校验接口返回 `AjaxResult`，前端判据保持 `response.data === true`（与 name 校验同构） |
| **IV. 任务与项目解耦** | 禁止向 `pm_project` 加任务字段 | ⚪ 不适用，本特性只碰 `pm_contract` |
| **V. 数据库规范** | 跨字符集 JOIN 加 `COLLATE` | ⚪ 不适用：判重语句是 `pm_contract` **单表**查询，无 JOIN，无跨字符集比较 |
| | Schema 变更策略 | ✅ 双轨：新环境改 `pm-sql/init/00_tables_ddl.sql:609/636`；已部署库出 `pm-sql/fix_contract_code_unique_20260806.sql`（**gitignored，不提交**） |
| **VI. 前端组件与字典规范** | 字典用 `<dict-select>` | ⚪ 不适用：合同编号是自由文本，非字典项。模板 `prop="contractCode"` 已存在，只补 `rules` 键 |

**结论**：通过。唯一的 ⚠️ 是「判重绕过 `@DataScope`」，属**显式登记的必需设计**，理由与边界已在上表写明。无需 Complexity Tracking。

---

## 一、四层防御总览

四层不是冗余，是**四种不同的失效模式各有一层对应**：

| 层 | 位置 | 拦什么 | 拦不住什么 | 失效后果 |
|---|---|---|---|---|
| **L1 校验接口** | `ContractServiceImpl.checkContractCodeUnique` ← `GET /project/contract/checkContractCodeUnique` | 提供「这个编号能不能用」的**查询能力** | 它自己不阻止任何写入 | 前端拿不到判据 |
| **L2 服务端硬拦截** | `ContractServiceImpl.insertContract` / `updateContract` | **所有**经由 API 的写入（含绕过前端直接打接口、脚本、未来新入口） | 两个请求同时通过检查后先后落库（TOCTOU 窗口） | 重复数据进库 |
| **L3 前端反馈** | `contract/add.vue`、`contract/edit.vue` 的 `rules` | 用户**在提交前**就看到错误，不必等保存失败 | 接口 403/500/断网时 `catch` 分支放行（既有 name 校验就有这个洞）；并发 | 体验退化，但 L2 仍拦得住 |
| **L4 数据库约束** | `pm_contract` 条件生成列 + 唯一索引 | L2 的 TOCTOU 窗口、直连 SQL 写入、将来任何新增写入路径 | 无（最后一道） | 唯一性彻底失守 |

**顺序关系**：L3 是体验层，L2 是**唯一可靠的业务防线**，L4 是并发与旁路的兜底。**只做 L3 等于没做**——前端 `catch` 分支的放行策略（`add.vue:301-304` / `edit.vue:316-319`）意味着接口一挂校验就静默失效。

```
用户输入 ──blur──> L3 前端 validator ──> L1 校验接口 ──> 提示「已存在」并阻止提交
                                                              │（用户仍可直接打 API）
提交 ──POST/PUT──> L2 insert/updateContract 内判重 ──> ServiceException「合同编号已存在」
                                                              │（并发同时通过）
                  ──INSERT/UPDATE──> L4 uk_contract_code_norm ──> 1062 ──> 转 ServiceException
```

---

## 二、归一化口径的三处实现（必须逐字一致）

**唯一定义**（重述 spec §三）：删除所有 `CHAR(9)/CHAR(13)/CHAR(10)` → 去首尾**半角空格** → 结果为 `''` 或 `'无'` 则视为未填写（`NULL`）。

| 处 | 实现 | 位置 |
|---|---|---|
| **Java** | `normalizeContractCode(String)` 私有静态工具 | `ContractServiceImpl.java`（建议紧邻 `calculateTaxAmounts`） |
| **判重 SQL** | `TRIM(REPLACE(REPLACE(REPLACE(c.contract_code, CHAR(9), ''), CHAR(13), ''), CHAR(10), ''))` + `del_flag='0'` | `ContractMapper.xml` 新增 `<select>` |
| **数据库生成列** | 同一表达式外包一层 `NULLIF(...,'')` / `NULLIF(...,'无')` + `IF(del_flag='0', ..., NULL)` | `pm_contract.contract_code_norm` |

### Java 侧

```java
/** 合同编号中表示「未填写」的字面值 */
private static final String CONTRACT_CODE_NONE = "无";

/**
 * 合同编号归一化：删除全部 TAB/CR/LF → 去首尾半角空格 → 空串或字面「无」视为未填写(null)。
 *
 * 口径必须与 ContractMapper.xml 的判重语句、pm_contract.contract_code_norm 生成列逐字一致。
 * 注意：此处刻意不使用 String.trim() —— 它会去掉全部 <= U+0020 的字符（含 \f \v \0），
 * 而 MySQL 的 TRIM() 只去半角空格，用 trim() 会让 Java 与 SQL 在这些字符上产生分歧。
 */
private static String normalizeContractCode(String contractCode)
{
    if (contractCode == null) {
        return null;
    }
    String s = contractCode.replace("\t", "").replace("\r", "").replace("\n", "");
    int begin = 0, end = s.length();
    while (begin < end && s.charAt(begin) == ' ') { begin++; }
    while (end > begin && s.charAt(end - 1) == ' ') { end--; }
    s = s.substring(begin, end);
    return (s.isEmpty() || CONTRACT_CODE_NONE.equals(s)) ? null : s;
}
```

### 判重 SQL 侧

```xml
<!-- 合同编号判重专用：精确 + 归一化 + 仅在用记录 + 全局（不带 ${params.dataScope}）。
     禁止复用 contractFilterConditions —— 那里的 contract_code 是 like 模糊匹配，
     是合同列表搜索功能的正确实现，输 ABC 会命中 ABC-001。
     MySQL 的 TRIM() 只去空格不去 TAB，三层 REPLACE 不可省；
     用 CHAR(9)/CHAR(13)/CHAR(10) 而非 '\t' 字面量以避免转义歧义。 -->
<select id="countByContractCode" resultType="int">
    select count(1)
    from pm_contract c
    where c.del_flag = '0'
      and TRIM(REPLACE(REPLACE(REPLACE(c.contract_code, CHAR(9), ''), CHAR(13), ''), CHAR(10), ''))
          = #{normalizedCode}
    <if test="excludeContractId != null">
      and c.contract_id != #{excludeContractId}
    </if>
</select>
```

对应接口方法：

```java
int countByContractCode(@Param("normalizedCode") String normalizedCode,
                        @Param("excludeContractId") Long excludeContractId);
```

> **返回 `int` 而非 `List<Long>`（2026-08-06 修订）**：原设计返回 ID 列表是为「提示里带上冲突合同名」留余地，
> 但 OQ-4 已拍板**提示不带冲突方名称**——判重刻意绕过 `@DataScope`，带出对方合同名等于跨部门泄露。
> 既然不需要冲突方身份，`count` 就是最小够用的返回，也避免了「查到 ID 后再查名称」的第二次访库。
> 将来若业务要求带名称，届时按「冲突方在当前用户数据权限范围内才带出」增量实现，那时再改签名。
>
> ⚠️ 本方法名已被 `ContractServiceImplTest` 的 27 个红灯用例锁定，实现时**必须逐字一致**，
> 否则测试编译不过。测试是验收标准，不要反过来改测试迁就实现。

### 数据库生成列侧

```sql
ALTER TABLE pm_contract
  ADD COLUMN `contract_code_norm` varchar(100)
    GENERATED ALWAYS AS (
      IF(`del_flag` = '0',
         NULLIF(NULLIF(
           TRIM(REPLACE(REPLACE(REPLACE(`contract_code`, CHAR(9), ''), CHAR(13), ''), CHAR(10), ''))
         , ''), '无'),
         NULL)
    ) VIRTUAL COMMENT '合同编号归一化值(仅在用记录;空串/无→NULL),唯一约束用';

ALTER TABLE pm_contract
  ADD UNIQUE KEY `uk_contract_code_norm` (`contract_code_norm`);
```

**三处的差异是有意的，且不影响一致性**：

- 生成列多了 `IF(del_flag='0', ..., NULL)` 与 `NULLIF(...,'无')`，是把「只约束在用记录」「空值不参与」这两条规则编码进列本身；判重 SQL 用 `where del_flag='0'` 与「Java 侧 normalize 为 null 就不查」达成同样效果。
- `contract_code` 为 `NULL` 时，`REPLACE` 链结果为 `NULL` → `TRIM(NULL)=NULL` → `NULLIF(NULL,'')=NULL`，天然落到「不参与约束」。**MySQL 的唯一索引不约束 `NULL`**，这正是「多条空编号合法共存」（FR-2）在数据库层的实现方式。

---

## 三、关键决策

### D1：为什么必须是「生成列 + `del_flag` 条件」，不能是裸唯一索引

`pm-sql/init/00_tables_ddl.sql:636` 里写着 `UNIQUE KEY uk_contract_code (contract_code)`，但生产与本地开发库都没有它（schema 漂移）。**不能简单地把它补上**，两个硬性理由：

**理由一：装不上。** 软删记录保留原编号，与在用记录共处一表，实测「软删 × 在用」的编号冲突共 **5 组**（其中一组是同一编号三行）。裸唯一索引对 `del_flag` 无感，`ALTER` 会直接因存量重复失败。要装上就得先破坏软删记录的编号（违反 INV-6：软删记录是审计材料）。

**理由二：装上了也是错的。** 「删掉合同 A，再用同一个编号重建合同 B」是完全合法的业务动作（录错了删掉重录）。裸唯一索引会用 `1062` 拒绝它，把一个正常操作变成故障。

**条件生成列同时解决两者**：软删行的生成列取值为 `NULL`，MySQL 唯一索引不约束 `NULL` → 软删记录既不参与冲突、也不占用编号。

> 技术说明：MySQL 5.7+ 支持在 **VIRTUAL 生成列**上建二级索引（含 UNIQUE），索引本身是物化的，虚拟列不占行存储。选 `VIRTUAL` 而非 `STORED` 是为了避免 `ADD COLUMN` 触发表重建 —— 虽然 337 行的表怎么改都是秒级，但保持 `INPLACE` 是好习惯。**建议拆成两条 `ALTER` 分别执行**（加列 / 加索引），合并成一条可能被优化器降级为 `COPY`。

**副作用核查（已实测）**：全仓 mapper 中**没有任何针对 `pm_contract` 的 `select *`**（`grep -rn "select \*" ruoyi-project/src/main/resources/mapper/ | grep -i contract` 零命中），`insertContract`(`:204`) 显式列出列名，因此新增的虚拟列对所有既有查询与写入**完全不可见**，不需要改任何映射。

### D2：MySQL `TRIM()` 不去 TAB —— 三处口径的对齐点

`contract_id=263` 的编号末尾带一个 TAB（`LENGTH=24` vs 正常 23），它与 258 在**任何朴素字符串比较下都不相等**。这是本特性最容易被漏掉的事实：

| | Java `String.trim()` | MySQL `TRIM()` |
|---|---|---|
| 半角空格 | 去 | 去 |
| TAB / CR / LF | **去** | **不去** |
| `\f` `\v` `\0` 等 `<= U+0020` | **去** | **不去** |

因此：**SQL 侧必须显式 `REPLACE` 掉 `CHAR(9)/CHAR(13)/CHAR(10)`**，Java 侧则刻意不用 `String.trim()`（改为只剥半角空格，见 §二代码注释），把三处的差异压到零。

写 `CHAR(9)` 而不是 `'\t'`：MyBatis XML → JDBC → MySQL 解析器一路上有多层转义，`'\t'` 在不同层可能被解释成两字符字面量 `\` + `t`。`CHAR(9)` 无歧义。

### D3：判重查询必须绕过 `@DataScope`

现有 `checkContractCodeUnique` 直接调 `contractMapper.selectContractList(...)` 而非 `this.selectContractList(...)`，因此 AOP 切面不触发、`${params.dataScope}` 渲染为空串。**这是必需的正确设计，改造时必须保持。**

依据：`@DataScope(deptAlias="d", userAlias="u1")` 声明在 **Service 方法** `ContractServiceImpl:120` 上；实测重复的两条记录分属部门 **201** 与 **103** —— 任一部门的用户走数据权限查询都看不见对方，判重会 100% 漏判。

新增的 `selectContractIdsByNormalizedCode` 天然不带 `${params.dataScope}`，达成同样效果且更明确。

**为什么这不构成越权**：该语句只返回 `contract_id`，不吐合同名、金额、客户等任何业务字段；调用方 Service 也只用它做布尔判断，不把 ID 传回前端。唯一性是全局属性，判定它需要全局视野——这与「能读到别的部门的合同内容」是两件事。

### D4：判重语句必须独立，且**不**查生成列

**不复用 `contractFilterConditions`**：`ContractMapper.xml:9` 的 `like concat('%',...,'%')` 被 `selectContractList(:126)` 与 `selectContractSummary(:182)` 共用，是合同列表「合同编号」筛选的正确实现（`tests/contract-filter.spec.js:72/93/113` 断言 `toContain(kw)`）。把它改成精确匹配会打红 5 条以上用例并让搜索功能退化。

**判重语句写函数表达式，而不是 `where contract_code_norm = ?`**：后者能走 `uk_contract_code_norm` 索引，但会让**应用代码强依赖 DDL 已经执行**——若代码先上线、`ALTER` 后执行，查询会因 `Unknown column` 全线报错。表只有 337 行，全表扫描的代价可以忽略；换来的是**代码不依赖 DDL 是否已执行——代码可以安全地先上线**。

> ⚠️ **但反过来不成立，解耦只解决了一个方向**（2026-08-07 本地实证，见 §5.4）：
> DDL 先上而代码未上时，判重完全不存在，重复提交会一路走到 `INSERT` 撞上 `1062`，
> 用户看到的是裸的 `SQLIntegrityConstraintViolationException` —— 含表名、完整 SQL 与
> jar 路径的一大坨技术信息，而不是「合同编号已存在：xxx，请使用其他编号」。
> 实证来自另一个未合并本特性的 worktree（019-outsource-workhour）：它的后端不含判重代码，
> 但共用同一个已建索引的本地库，新增重复编号时前端直接糊了一屏异常。

> 若将来 `pm_contract` 增长到十万行量级，可把判重语句改为直接命中 `contract_code_norm`（届时索引早已就位，无顺序风险）。现在不做。

### D5：校验插入位置 + `<if>` 守卫必须拆除

**位置**：编号判重必须插在**既有「项目已关联合同」校验之后**、`calculateTaxAmounts` 之前。

- `insertContract`：`ContractServiceImpl.java:155-163` 之后、`:165` 之前
- `updateContract`：`ContractServiceImpl.java:212-220` 之后、`:222` 之前

依据：`tests/006-code-review-fixes.spec.js:235` 断言 `expect(updateBody.msg).toContain('已关联其他合同')`。若编号判重前置，且被测那条合同的编号恰好与别人重复，返回的 `msg` 会被「合同编号已存在」顶掉而变红。

**归一化值必须回写实体**：判重前先 `contract.setContractCode(normalizeContractCode(contract.getContractCode()))`，保证落库的是归一化后的值（FR-3），不再产生 263 那种脏数据。

**`ContractMapper.xml:267` 的 `<if>` 守卫必须去掉**：

```xml
<!-- 改前 -->
<if test="contractCode != null">contract_code = #{contractCode},</if>
<!-- 改后（与 :279 contract_sign_date 同处理，并补同款注释） -->
contract_code = #{contractCode},
```

不改的后果：Service 归一化出的 `NULL` 永远写不回库 → **用户清空合同编号 → 提示保存成功 → 值不变**（INV-3）。CLAUDE.md 记录该类缺陷已复发四次，特征是操作日志里请求体完全正常、极难定位。

**去掉守卫的安全性已被现场注释论证**：`ContractMapper.xml:273-278` 已写明另一调用方 `deleteContractByContractIds`(`ContractServiceImpl.java:274`) 传的是 `selectContractByContractId` 查回的**完整实体**，而该查询的 select 列表含 `contract_code`(`:141`)，因此只是等值回写，不会误清空。`contractMapper.updateContract` 的全部调用方只有两个（`:226` 全量表单、`:274` 软删回写），已逐个核实。

### D6：大小写口径 —— Java 只归一化，比对一律交给 SQL

`pm_contract` 的排序规则是 `utf8mb4_0900_ai_ci`（`00_tables_ddl.sql:641`），**大小写与重音不敏感**：`abc-001` 与 `ABC-001` 在 `=` 比较和唯一索引里都算同一个值。而 Java 的 `String.equals` 大小写敏感。

**若在 Java 侧再补一层字符串比对，就会出现「Service 放行但唯一索引报 1062」的裂缝。** 因此本方案约定：

> **Java 只负责 normalize（删控制字符 / 剥空格 / 空值判定），一切「是否相同」的判断交给 SQL 与索引。**

`ContractServiceImpl` 中不得出现 `code.equals(other)` 之类的编号比对。该口径已在 spec **OQ-3** 登记待业务确认；若业务要求区分大小写，则需给生成列与判重语句显式加 `COLLATE utf8mb4_0900_as_cs`（或 `_bin`），届时 Java 侧仍不需要改。

### D7：`1062` 的用户可读化（L4 兜底的收尾）

L4 触发时（并发撞车），MySQL 抛 `1062`，Spring 包成 `DuplicateKeyException`，`GlobalExceptionHandler` 会返回一条无意义的系统异常。按 FR-20，在 `insertContract` / `updateContract` 内捕获并转译：

```java
try {
    rows = contractMapper.insertContract(contract);
} catch (DuplicateKeyException e) {   // org.springframework.dao.DuplicateKeyException
    throw new ServiceException("合同编号「" + contract.getContractCode() + "」已存在，请使用其他编号");
}
```

文案与 L2 主动拦截保持**同一句**，用户无法（也不需要）区分自己撞的是哪一层。

### D8：前端两处 validator 与 name 校验同构

`contractCode` 的模板项 `prop="contractCode"` 在 `add.vue:31-33` / `edit.vue:19-21` **已存在**，只需补 `rules` 键即可生效，模板零改动。

- **只加 `validator`，不加 `required`**（FR-17 / D-A）。
- **空值短路必须是 validator 函数体第一段、`try` 之前**，且判据用归一化后的值 —— 不能沿用 name 校验的 `!value || value.trim()===''`（漏掉字面「无」与内部 TAB）。
- **传给接口的是归一化后的值**，不是 `value.trim()`。
- **`edit.vue` 的 `contractId` 用 `form.value.contractId ?? route.params.contractId` 兜底** —— `form.contractId` 要等 `getContract` 回填（`edit.vue:472`）才有值，加载完成前触发 blur 会让合同把自己判成重复（FR-16）。
- 前端 normalize 只用于校验与入参，**是否把归一化值写回输入框**（所见即所存）本次**不做** —— 后端 FR-3 已保证落库值干净，写回会改变用户输入、语义更重。

前端 normalize 实现（两个文件各一份，或抽到 `utils/`）：

```js
function normalizeContractCode(v) {
  return String(v ?? '').replace(/[\t\r\n]/g, '').replace(/^ +| +$/g, '')
}
```

> 注意正则用 `^ +| +$`（只剥半角空格）而非 `.trim()`，与 Java/SQL 口径对齐。

---

## 四、改动清单

### 后端

| # | 文件 | 位置 | 改动 | FR |
|---|---|---|---|---|
| B1 | `ContractServiceImpl.java` | 新增私有方法（`calculateTaxAmounts` 附近） | `normalizeContractCode(String)` | FR-1 |
| B2 | `ContractServiceImpl.java` | `:366-395` 整体重写 | 空→`true`；改调新 mapper 方法；保持直调 `contractMapper`（绕 `@DataScope`）；保留 `list == null` 保护 | FR-4~8 |
| B3 | `ContractServiceImpl.java` | `:163` 之后 / `:165` 之前 | insert：归一化回写 + 判重 + `ServiceException` | FR-3/9/11/12/13 |
| B4 | `ContractServiceImpl.java` | `:220` 之后 / `:222` 之前 | update：同上 + 排除自身 | FR-3/10/11/12/13 |
| B5 | `ContractServiceImpl.java` | `:173` / `:226` 的 mapper 调用 | 包 `try/catch DuplicateKeyException` → `ServiceException` | FR-20 |
| B6 | `ContractMapper.java` | `:119` 之后 | 新增 `selectContractIdsByNormalizedCode` | FR-5 |
| B7 | `ContractMapper.xml` | `:478 </mapper>` 之前 | 新增判重 `<select>`（见 §二） | FR-5/6/7 |
| B8 | `ContractMapper.xml` | `:267` | 去掉 `<if>` 守卫 + 补注释 | INV-3 |
| B9 | `IContractService.java` | `:102-109` javadoc | 更新语义说明（空=未填写=唯一） | FR-4 |
| — | `ContractMapper.xml:9` | — | **禁止改动**（模糊匹配是列表搜索的正确实现） | INV-1 |
| — | `ContractServiceImpl.java:274` | — | **禁止改动**（软删走 mapper，天然绕开判重） | INV-4 |

### 前端

| # | 文件 | 位置 | 改动 | FR |
|---|---|---|---|---|
| F1 | `contract/add.vue` | `:272` | import 加 `checkContractCodeUnique` | FR-14 |
| F2 | `contract/add.vue` | `:306` 后 | 新增 `normalizeContractCode` + `validateContractCode`（`contractId` 传 `null`） | FR-14/15 |
| F3 | `contract/add.vue` | `rules`（`:326` 后） | `contractCode: [{ validator: validateContractCode, trigger: 'blur' }]`，**无 `required`** | FR-14/17 |
| F4 | `contract/edit.vue` | `:290` | import 同上 | FR-14 |
| F5 | `contract/edit.vue` | `:321` 后 | 同 F2，`contractId` 用 `form.value.contractId ?? route.params.contractId` | FR-14/15/16 |
| F6 | `contract/edit.vue` | `rules`（`:346` 后） | 同 F3 | FR-14/17 |
| — | `api/project/contract.js:68` | — | **零改动**（签名已可用，`null` 参数会被 `tansParams` 剔除） | — |
| — | 两处模板 | — | **零改动**（`prop="contractCode"` 已存在） | — |

### 数据库

| # | 文件 / 目标 | 改动 |
|---|---|---|
| D1 | `pm-sql/init/00_tables_ddl.sql:636` | `UNIQUE KEY uk_contract_code (contract_code)` → 删除；改为在 `:609` 之后加 `contract_code_norm` 生成列，并加 `UNIQUE KEY uk_contract_code_norm (contract_code_norm)` |
| D2 | `pm-sql/fix_contract_code_unique_20260806.sql` | 已部署库（生产 + 本地 docker）的迁移脚本。**gitignored，不提交**（Constitution 安全治理条款） |

---

## 五、存量数据处理与迁移

### 5.1 前置检查（必跑，`ALTER` 前）

```sql
-- ① 当次口径重新点数（回答 spec OQ-1）
SELECT del_flag, COUNT(*) FROM pm_contract GROUP BY del_flag;

-- ② 在用记录中归一化后的重复组（必须返回 0 行才能加索引）
SELECT norm, COUNT(*) AS cnt, GROUP_CONCAT(contract_id) AS ids
FROM (
  SELECT contract_id,
         NULLIF(NULLIF(TRIM(REPLACE(REPLACE(REPLACE(contract_code, CHAR(9), ''), CHAR(13), ''), CHAR(10), '')), ''), '无') AS norm
  FROM pm_contract WHERE del_flag = '0'
) t
WHERE norm IS NOT NULL
GROUP BY norm HAVING COUNT(*) > 1;

-- ③ 现有索引确认（预期只有 PRIMARY —— 印证 schema 漂移）
SHOW INDEX FROM pm_contract WHERE Non_unique = 0;
```

### 5.2 环境差异（两个库状态不同，不能套用同一结论）

| 环境 | ② 的预期结果 | 处置 |
|---|---|---|
| **生产** | 预期 **0 行**（258 已于 2026-08-06 软删，冲突对已解除）—— 但**必须实跑确认**，不得凭这句话直接 `ALTER` | 0 行 → 直接执行迁移 |
| **本地 docker `newpm-mysql-1`** | 预期 **1 行**（258/263 双双 `del_flag='0'`） | 先在本地把其中一条软删或改号，再 `ALTER`。**本地处置不要同步到生产** |

若 ② 返回非空且不是已知的那一组，**停止迁移**，把结果拿给业务方判定哪条留、哪条删 —— 这是业务决策，不由开发代做。

### 5.3 迁移脚本（`fix_contract_code_unique_20260806.sql`）

```sql
-- 前置：5.1 的 ② 必须返回 0 行
ALTER TABLE pm_contract
  ADD COLUMN `contract_code_norm` varchar(100)
    GENERATED ALWAYS AS (
      IF(`del_flag` = '0',
         NULLIF(NULLIF(
           TRIM(REPLACE(REPLACE(REPLACE(`contract_code`, CHAR(9), ''), CHAR(13), ''), CHAR(10), ''))
         , ''), '无'),
         NULL)
    ) VIRTUAL COMMENT '合同编号归一化值(仅在用记录;空串/无→NULL),唯一约束用';

ALTER TABLE pm_contract
  ADD UNIQUE KEY `uk_contract_code_norm` (`contract_code_norm`);

-- 事后验证
SHOW CREATE TABLE pm_contract\G
SELECT COUNT(*) FROM pm_contract WHERE contract_code_norm IS NOT NULL;   -- 预期 ≈ 196
```

执行方式（中文注释必须走文件管道，禁止 `-e` 内嵌）：

```bash
# 生产
cat pm-sql/fix_contract_code_unique_20260806.sql | ssh k3s001 \
  "kubectl exec -i mysql-0 -n newpm -- mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue"
# 本地
cat pm-sql/fix_contract_code_unique_20260806.sql | docker exec -i newpm-mysql-1 \
  mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue
```

### 5.4 上线顺序

> ⚠️ **顺序是硬约束，不是偏好：代码必须先上，DDL 后上。**
> 原方案写的是「DDL 先行，顺序反过来也不会挂」，**这是错的**，已于 2026-08-07 依实证修正。
>
> | 顺序 | 中间窗口期发生什么 |
> |---|---|
> | **代码先 → DDL 后**（✅ 采用） | 判重立即生效（函数表达式不引用生成列，见 D4），用户看到的是人话提示；此时缺的只是并发兜底那一层，而并发撞车本就是极小概率 |
> | DDL 先 → 代码后（❌ 禁止） | 判重完全不存在，重复提交一路走到 `INSERT` 撞 `1062`，用户看到含表名、SQL、jar 路径的裸异常 —— 已实证 |

1. **备份先行**（生产变更前手动触发一次 DB 备份：`ssh k3s001 "sudo /usr/local/bin/backup-newpm-db.sh"`）
2. **合并代码 → push `main` → GitHub Actions 构建 → K3s 滚动重启，确认新版本已生效**
3. 跑 5.1 前置检查 → ② 必须 0 行（此时判重已在应用层生效，不会有新的重复产生）
4. 执行 5.3 的两条 `ALTER`（补上并发兜底与直连写库的最后防线）
5. 跑 §六 的验收清单

**不需要重启应用来「感知」新列** —— MyBatis 不做 schema 内省，判重语句也不引用生成列（D4）。

### 5.5 存量脏值的去留

31 条空串 + 2 条「无」**本次不改写**（spec OUT-3 / OQ-2）：生成列已把它们映射为 `NULL`，约束正确性不受影响。改写字面值属于数据订正，需业务方单独确认。

---

## 六、测试与验收方式

### 6.1 TDD 红绿（后端，先红后绿）

必须先看到失败输出再写实现。执行：`mvn test -pl ruoyi-project -am -Dtest=ContractServiceImplTest`

**基线先跑一次**（记录改动前的通过数），避免把既有红灯误记成本次引入。

| 循环 | 红点 | 目标 |
|---|---|---|
| T1 | `checkContractCodeUnique` 对 `null` / `""` / `"  "` / `"\t"` / `"无"` 返回 `true` | FR-4 |
| T2 | 输入 `ABC` 时，库中存在 `ABC-001` **不**判为重复（精确匹配） | FR-5 / SC-005 |
| T3 | `"\tABC "` 与 `"ABC"` 判为同一编号 | FR-1 / SC-006 |
| T4 | 编辑模式排除自身：命中的唯一 ID 等于自己 → `true` | FR-8 |
| T5 | `insertContract` 编号重复 → 抛 `ServiceException`，且 `verify(contractMapper, never()).insertContract(any())` | FR-9 |
| T6 | `updateContract` 编号重复（非自身）→ 抛 `ServiceException` | FR-10 |
| T7 | insert/update 归一化回写：入参 `"\tABC "` → 落库实体的 `contractCode` 为 `"ABC"`；入参 `"无"` → `null` | FR-3 |
| T8 | 编号为 `null` 时**不调用**判重 mapper（短路） | 性能 + 避免 NPE |

**既有用例的处置**（必须在同一提交内改）：

- `ContractServiceImplTest.java:235-246 checkContractCodeUnique_sameLogicAsName` —— mock 的是 `selectContractList`，实现改走新 mapper 后这两个 stub 变成 unnecessary stubbing，**`MockitoExtension` 默认 STRICT_STUBS 会直接红**。改写 mock 目标，三条断言语义可保留。这是「不改测试就必红」，不要误判成实现有 bug。
- `:60-120`（税额）与 `:250-274`（项目关联）的 7 个用例：`buildContractForInsert`(`:278-287`) 从不设 `contractCode`（恒 `null`），走 T8 的短路分支不会红 —— **前提是实现先判空再调 mapper**，且保留 `list == null` 保护。

### 6.2 BDD

`specs/020-contract-code-unique/bdd/contract-code-unique.feature`（中文 Gherkin，评审用，不直接执行）+ `bdd/coverage.md`（场景 → JUnit/Playwright 映射），格式照 `specs/017-contract-code-sort/bdd/`。

核心场景：录入重复编号被拒 / 空编号可存多条 / TAB 差异也算重复 / 编辑自身不被判重 / 删除后编号可重用 / 跨部门重复能检出。

### 6.3 E2E

**必须改的既有用例**

| 文件 | 位置 | 为什么会红 | 改法 |
|---|---|---|---|
| `tests/e2e-contract-crud.spec.js` | `:110-135` | 盲取 `list?pageSize=1` 的 `rows[0].contractCode`（默认 `order by c.update_time desc`，内容随库漂移）断言 `toBe(false)`。空串编号在新语义下返回 `true` → 红；`NULL` 会被 `URLSearchParams` 序列化成字面量 `"null"` → 今天就可能红 | 改为先筛出一条归一化后非空的编号再断言 `false`；另加一条「空编号 → `true`」用例 |

**新增用例**：`tests/e2e-contract-code-unique.spec.js` —— **自带造数，不抽样既有数据**（合同类 E2E 目前零 fixture，抽样是所有「数据漂移红灯」的根因）。自建合同 A（编号 `E2E-UNIQ-${TS}`）→ 用同编号新增 B 应被拒（`code=500` 且 `msg` 含「合同编号已存在」）→ 用 `\tE2E-UNIQ-${TS}` 再试仍被拒 → A 自身不改编号保存成功 → 清空 A 的编号保存后详情回读为空 → 软删 A 后用同编号新建成功 → 清理。

**必须保持绿的回归套件**（每一条都是本次设计约束的探针）

| 套件 | 守的是什么 |
|---|---|
| `tests/contract-filter.spec.js` | INV-1：模糊匹配没被「顺手」改成精确 |
| `tests/clear-field-guards-regression.spec.js:577-655` | 4 次携带自身编号的全量 PUT 全 200 → 自排除逻辑生效 |
| `tests/guard-payload-runtime.spec.js:576-627` | 合同编辑页真实 UI 保存能发出 PUT（前端 validator 若不排除自身，PUT 根本发不出去 → 25s 超时红） |
| `tests/006-code-review-fixes.spec.js:198-236` | `msg` 含「已关联其他合同」→ 校验顺序没有前置（D5） |
| `tests/global-string-trim.spec.js:49-95` | 提交 `\tCODE ` 后详情精确回读干净值 → normalize 是 trim 的超集，没把含空白的输入拒掉 |

### 6.4 静态检查

- `mvn clean compile -pl ruoyi-project -am`
- `cd ruoyi-ui && npx vue-tsc --noEmit` —— 判据是 `add.vue` / `edit.vue` **零错误**（全局存量基线约 39 错不作门槛）

### 6.5 验收对照

spec §六 的 SC-001~SC-012 逐条勾稽；SC-009（`SHOW CREATE TABLE` + 手工插重复报 1062）与 SC-007（跨部门）需在**生产变更后**各做一次实测观测。

---

## 七、风险与缓解

| ID | 风险 | 证据 | 缓解 |
|---|---|---|---|
| **R-001** | 只改 Service 不改 `ContractMapper.xml:267` 的 `<if>` 守卫 → 「编辑页清空编号保存后编号还在」，且操作日志请求体完全正常，极难定位 | CLAUDE.md 记录该类缺陷已复发四次（`pm_payment` 三次 + Issue #10 跨 5 mapper） | B8 列为独立改动项；E2E「清空编号」用例（SC-008）专门守它 |
| **R-002** | update 侧判重**忘了排除自身** → 全部 309 条存量合同（含 114 条空编号）都无法编辑保存 | `clear-field-guards-regression` 4 次 PUT + `guard-payload-runtime` UI 保存 + `006-code-review-fixes` 三处会同时红 | T4/T6 单测先红；三个 E2E 套件是现成防线，**不得在本次改动中跳过它们** |
| **R-003** | 判重被前置到项目关联校验之前 → `006-code-review-fixes.spec.js:235` 的 `toContain('已关联其他合同')` 被新文案顶掉 | 该用例断言的是**具体文案** | D5 明确插入位置；PR 自检 diff 看行号 |
| **R-004** | 「顺手」把 `ContractMapper.xml:9` 的 `like` 改成精确匹配 | 这是本次最诱人的错误修复方向 | INV-1 + `contract-filter.spec.js` 5 条断言；改动清单里显式标「禁止改动」 |
| **R-005** | `checkContractCodeUnique` 语义从「空→false」翻转为「空→true」是**破坏性变更** | 后端侧当前**零调用方**（前端定义了但没调），风险可控；但 `e2e-contract-crud.spec.js:132` 按旧语义写 | §6.3 已列改法；前端接入时按新语义写（空编号不弹「已存在」） |
| **R-006** | 生产 `ALTER` 因未预期的存量重复失败 | 本地库实测有 1 组在用冲突（258/263）；生产状态**未在本次重新实测** | 5.1 的 ② 是硬闸门：非 0 行不执行；发现新冲突交业务方判定，不由开发代做 |
| **R-007** | Java `String.trim()` 与 MySQL `TRIM()` 在 `\f` `\v` `\0` 上的差异造成「应用放行、数据库 1062」 | Java trim 去所有 `<= U+0020`，MySQL 只去空格 | D2：Java 侧刻意不用 `trim()`，改为只剥半角空格。**残余风险**：存量是否含此类字符**未验证**，可用 5.1 扩展一条检查 |
| **R-008** | 大小写口径未经业务确认（`_ai_ci` 下 `abc` = `ABC`） | 库排序规则 `utf8mb4_0900_ai_ci`（DDL `:641`） | spec OQ-3 登记；D6 约定「Java 不做二次比对」，使得将来改口径只需动 DDL 与一条 SQL |
| **R-009** | 前端 validator 的 `catch` 分支放行（接口 403/500/断网时校验静默失效） | 既有 name 校验就有这个洞（`add.vue:301-304`） | 不修（保持与 name 校验一致的行为），由 L2/L4 兜底。**这正是「只做前端等于没做」的量化理由** |
| **R-010** | TOCTOU：blur 时查唯一 → 保存前另一用户已插入同编号 | 前端预检结构性拦不住并发 | L4 唯一索引兜底 + D7 的 `1062` 转译 |
| **R-011** | 误改到 `.bak` 文件 | 仓库里有被 git 跟踪的 `ContractServiceImpl.java.bak` / `ContractController.java.bak` / `IContractService.java.bak`，grep 会双份命中 | 改完 `git status` 确认动的是 `.java` 不是 `.bak` |
| **R-012** | 纯文档提交也会触发约 6 分钟的生产空转部署 | CI `paths-ignore` 的 `'*.md'` 只匹配仓库根目录（glob 的 `*` 不跨 `/`），改 `specs/**` 照常触发 | 已知，接受；本特性最终会带代码一起提交 |

---

## 八、回滚方案

| 层 | 回滚动作 | 影响 |
|---|---|---|
| **代码（L1/L2/L3）** | `git revert` 本特性提交 → push `main` → 自动构建部署（约 6 分钟） | 判重能力消失，回到现状；已落库的干净编号不受影响 |
| **数据库（L4）** | `ALTER TABLE pm_contract DROP INDEX uk_contract_code_norm;`<br>`ALTER TABLE pm_contract DROP COLUMN contract_code_norm;` | 虚拟列与索引均可 INPLACE 删除，**不动任何业务数据**（生成列不存储原始值，删掉它不会丢 `contract_code`） |
| **数据** | 无需回滚 —— 本特性**不改写任何存量字面值**（OUT-3 / INV-6） | — |

**回滚顺序**：先回代码再回 DDL。反过来（先删列后回代码）在本方案下也安全，因为判重语句用的是函数表达式、不引用生成列（D4）。

**兜底**：生产变更前的手工备份（5.4 步骤 1）可整库恢复，但那是最后手段 —— 本特性的两个 `ALTER` 都是可逆的。

---

## 九、Project Structure

### Documentation

```
specs/020-contract-code-unique/
├── spec.md          # 需求（本次产出）
├── plan.md          # 本文件
├── bdd/
│   ├── contract-code-unique.feature   # 中文 Gherkin（后续产出）
│   └── coverage.md                    # 场景 → JUnit/Playwright 映射（后续产出）
└── tasks.md         # TDD 红绿任务清单（后续产出）
```

### Source Code

```
ruoyi-project/src/main/
├── java/com/ruoyi/project/
│   ├── service/impl/ContractServiceImpl.java   # B1~B5：normalize + 判重重写 + 两处硬拦截 + 1062 转译
│   ├── service/IContractService.java           # B9：javadoc 语义更新
│   └── mapper/ContractMapper.java              # B6：新增判重方法签名
└── resources/mapper/project/ContractMapper.xml # B7 新增判重 select；B8 去掉 :267 守卫

ruoyi-project/src/test/java/com/ruoyi/project/service/impl/
└── ContractServiceImplTest.java                # T1~T8 新增；:235-246 改写

ruoyi-ui/src/views/project/contract/
├── add.vue                                     # F1~F3
└── edit.vue                                    # F4~F6

pm-sql/
├── init/00_tables_ddl.sql                      # D1：裸唯一索引 → 生成列 + uk_contract_code_norm
└── fix_contract_code_unique_20260806.sql       # D2：已部署库迁移（gitignored，不提交）

tests/
├── e2e-contract-code-unique.spec.js            # 新增（自带造数）
└── e2e-contract-crud.spec.js                   # :110-135 改造
```

---

## 十、设计后复查

- **无新增接口**（复用既有 `checkContractCodeUnique` 端点与既有 `add`/`edit`）
- **无新增表、无新增依赖、无新增前端组件**
- **唯一的 schema 变更是可逆的虚拟生成列 + 唯一索引**，不改写任何业务数据
- **判重绕过 `@DataScope`** 已作为必需设计显式登记，且限定只返回 ID、不吐业务字段
- **三处归一化口径**已给出逐字对照与差异说明（D2/D6）
- **三处「禁止改动」**（`ContractMapper.xml:9`、`ContractServiceImpl.java:274`、前端不加 `required`）已写进改动清单

**Constitution 复查：仍全部通过。**
