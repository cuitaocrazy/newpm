# Quickstart：本地验证合同编号判重

**Feature**: `020-contract-code-unique`（Issue #32）
**工作区**: worktree `.claude/worktrees/contract-code-unique`
**状态**: 本文件写于**实现之前**（BDD 阶段）。步骤按 `bdd/contract-code-unique.feature` 的场景顺序编排；
实现完成并实测走通后，须回填「最后实测」时间与实际结果。

---

## 一、前置条件

### 1.1 端口（**实测自本 worktree 的配置文件，不是记忆值**）

| 用途 | 端口 | 来源 |
|---|---|---|
| 后端 | **8080** | `ruoyi-admin/src/main/resources/application.yml` → `server.port: 8080` |
| 前端 dev | **80** | `ruoyi-ui/vite.config.ts` → `server.port: 80` |
| 前端代理 | `/dev-api` → `http://localhost:8080` | `vite.config.ts` 顶部 `const baseUrl = 'http://localhost:8080'` + `server.proxy['/dev-api']`（`rewrite` 去掉 `/dev-api` 前缀） |
| MySQL | 3306，库 `ry-vue` | `application-druid.yml`（`root` / `password`） |
| Redis | 6379 | `application.yml` |

> ⚠️ **8080 可能已被主工作区的后端占用**。若 `lsof -i:8080` 有进程，本 worktree 的后端换个端口起
> （`--server.port=8085`），并**同步改 `ruoyi-ui/vite.config.ts` 第 5 行的 `baseUrl` 常量** ——
> 那是个硬编码常量，不读环境变量，只改启动参数不改它，前端会把请求打到另一个后端上（静默串库，
> 没有任何征兆：登录成功、请求成功，只是断言莫名其妙）。
>
> ⚠️ 端口 80 需要 sudo。不想用 sudo 就把 vite 起在 5173，e2e 侧用 `E2E_BASE_URL=http://localhost:5173`
> 覆盖（`playwright.config.js` 与 `tests/helpers/api-client.js` 都认这个变量）。

### 1.2 依赖

Java 17 / Maven 3.6+ / Node 18+ / MySQL 8.x / Redis 6+。账号 `admin` / 密码 `123456789`。

### 1.3 关闭登录验证码（跑 e2e 前必做，跑完恢复）

```bash
docker exec -i newpm-mysql-1 mysql -uroot -ppassword ry-vue \
  -e "UPDATE sys_config SET config_value='false' WHERE config_key='sys.account.captchaEnabled';"
# 配置走缓存，改完清一下
docker exec -i newpm-redis-1 redis-cli --scan --pattern "sys_config:*" | xargs -r -n1 docker exec -i newpm-redis-1 redis-cli DEL
```

**收尾必须改回 `'true'`**（见 §六）。

### 1.4 数据库变更（本特性**有** DDL 改动，不做则第六组场景无法验证）

判重的应用层逻辑（L1–L3）不依赖新列，**先跑代码验证不需要执行 DDL**；
但 6.3「直连 SQL 被数据库挡住」必须先落这两条 `ALTER`。

```bash
# ① 前置检查：在用记录中归一化后的重复组，必须返回 0 行才能加索引
docker exec -i newpm-mysql-1 mysql -uroot -ppassword ry-vue < /dev/stdin <<'SQL'
SELECT norm, COUNT(*) AS cnt, GROUP_CONCAT(contract_id) AS ids
FROM (
  SELECT contract_id,
         NULLIF(NULLIF(TRIM(REPLACE(REPLACE(REPLACE(contract_code, CHAR(9), ''), CHAR(13), ''), CHAR(10), '')), ''), '无') AS norm
  FROM pm_contract WHERE del_flag = '0'
) t
WHERE norm IS NOT NULL
GROUP BY norm HAVING COUNT(*) > 1;
SQL

# ② 本地库预期返回 1 行（258/263 双双在用）→ 先把其中一条软删，再执行迁移
#    ⚠️ 本地的处置不要同步到生产；生产上 258 已于 2026-08-06 软删

# ③ 执行迁移（中文注释必须走文件管道，禁止 -e 内嵌中文）
cat pm-sql/fix_contract_code_unique_20260806.sql | docker exec -i newpm-mysql-1 \
  mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue
```

脚本内容见 `plan.md §5.3`。该文件按项目约定 **gitignored，不提交**。

### 1.5 启动

```bash
# 后端（worktree 根目录）
mvn clean package -Dmaven.test.skip=true
java -Xms512m -Xmx1024m -jar ruoyi-admin/target/ruoyi-admin.jar
# 若 8080 被占：java -jar ruoyi-admin/target/ruoyi-admin.jar --server.port=8085  （记得同步改 vite baseUrl）

# 前端
cd ruoyi-ui && npm run dev     # 端口 80，需 sudo
```

---

## 二、单元测试（最快，无需 MySQL/Redis）

```bash
mvn test -pl ruoyi-project -am -Dtest=ContractServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false
```

> ⚠️ `-Dsurefire.failIfNoSpecifiedTests=false` **不能省**：`-am` 会连带构建 `ruoyi-common`，
> 那里没有这个测试类，surefire 会直接报错中止。

**先跑一次基线**（改动前的通过数），避免把既有红灯误记成本次引入。
全模块回归：`mvn test -pl ruoyi-project -am`，数量不得低于基线。

覆盖的场景见 `bdd/coverage.md §三`（12 条计划用例，对应 plan.md 的 T1–T8 + 4 条新增）。

---

## 三、手动验证步骤（按 feature 场景顺序）

登录后打开合同列表 `http://localhost/htkx/contract`。
先手工建一份**基准合同**：点「新增」，填合同名称 `020基准合同`、部门、关联客户、合同类型、合同状态、合同金额，
**合同编号填 `QS-BASE-001`**，保存。下面所有步骤围绕它展开。

### 第一组：重复编号被当场拦住（场景 1.1–1.5）

1. **新增入口拦截**：再点「新增」，其余必填项随便填，合同编号填 `QS-BASE-001` → 保存
   → 期望：**保存失败**，提示含「合同编号已存在」和编号 `QS-BASE-001`；列表里没有多出这份合同。
2. **失焦即反馈**（场景 1.2）：同一个新增页，把编号删掉重新键入 `QS-BASE-001`，**光标移出输入框**
   → 期望：输入框下方立刻出现红字提示，不用点保存；此时点保存，表单不提交。
3. **编辑入口拦截**（场景 1.1b / 1.3）：另建一份不填编号的合同 `020副本合同` → 打开它的编辑页
   → 把编号改成 `QS-BASE-001` → 保存 → 期望：**被拒**；返回列表，`020副本合同` 的编号仍为空。
4. **没有后门**（场景 1.4）：在被拒状态下反复点保存 → 期望：每次都被拒，界面上**不出现**
   「仍要保存」「确认继续」之类的按钮。
5. **提示是人话**（场景 1.5）：确认提示里没有 `Exception`、`select`、表名字段名之类的字样。

### 第二组：不填编号照样能干活（场景 2.1–2.4）

6. **空编号可存**：新增一份合同，编号**留空**，其余填好 → 保存 → 期望：**成功**，且全程不出现
   「合同编号已存在」，也不出现「请输入合同编号」的必填提示（编号输入框标签前**没有**红色星号）。
7. **多份空编号并存**（场景 2.2）：重复步骤 6 再建一份 → 期望：仍然成功。
8. **五种"等同于没填"的输入**（场景 2.3）：分别用 `（留空）` / `空格` / `TAB` / `空格+TAB` / `无`
   作为编号新增 → 期望：**五次全部保存成功**，列表里这几份合同的编号列都显示为空。
   > TAB 用复制粘贴输入（表单里直接敲 Tab 会跳到下一个控件）。
9. **清空真的清空**（场景 2.4，历史高发缺陷）：打开基准合同 `020基准合同` 的编辑页，
   **把编号 `QS-BASE-001` 全部删空** → 保存 → 提示成功 → **重新打开这份合同的详情页**
   → 期望：编号显示为**空**。若还显示 `QS-BASE-001`，就是 `ContractMapper.xml` 的 `<if>` 守卫没拆
   （这一步验完把编号改回 `QS-BASE-001`，后续步骤还要用）。

### 第三组：看起来一样的就是一样的（场景 3.1–3.3）

10. **首尾空白算重复**：新增合同，编号填 `  QS-BASE-001`（前面两个空格）→ 期望：**被拒**。
    再试 `QS-BASE-001  `（后面空格）、`QS-BASE-001<TAB>`（末尾粘贴一个制表符）→ 期望：**都被拒**。
11. **中间空格不抹掉**（场景 3.2）：先建一份编号为 `QS 002` 的合同（成功），
    再建一份编号为 `QS002` 的 → 期望：**成功**，这是两个不同的编号。
12. **落库值干净**（场景 3.3）：新建一份合同，编号粘贴为 `QS-CLEAN-001<TAB>`（末尾带制表符）→ 保存成功
    → 打开详情 → 期望：显示的编号**不带制表符**。可用 SQL 复核：
    ```bash
    docker exec -i newpm-mysql-1 mysql -uroot -ppassword ry-vue \
      -e "SELECT contract_id, LENGTH(contract_code), contract_code FROM pm_contract WHERE contract_code LIKE 'QS-CLEAN%';"
    ```
    → 期望 `LENGTH` 等于干净字符串 `QS-CLEAN-001` 的长度（**12**），不是 13。

### 第四组：不能误伤正常操作（场景 4.1–4.5）

13. **前缀不误报**（场景 4.1）：新增合同，编号填 `QS-BASE`（是 `QS-BASE-001` 的前缀）
    → 期望：**保存成功**。反向再试 `QS-BASE-001-EXT` → 期望：也成功。
14. **编辑不动编号不被自己拦**（场景 4.2）：打开 `020基准合同` 编辑页，只改「合同金额」，
    编号 `QS-BASE-001` 原样不动 → 保存 → 期望：**成功**，金额已变、编号未变。
15. **模糊搜索照常可用**（场景 4.4）：回到合同列表，在「合同编号」查询框里输入 `QS`
    → 期望：上面建的 `QS-*` 合同**都被搜出来**（模糊匹配，不是精确匹配）。
16. **删除不被判重挡住**（场景 4.5）：直接用 SQL 造一对同号在用脏数据，再从界面删掉其中一条：
    ```bash
    # 造：把某条 QS-* 合同的编号改成与基准合同相同（绕过界面，模拟历史脏数据）
    docker exec -i newpm-mysql-1 mysql -uroot -ppassword ry-vue \
      -e "UPDATE pm_contract SET contract_code='QS-BASE-001' WHERE contract_name='020副本合同';"
    ```
    → 在列表里删除 `020副本合同` → 期望：**删除成功**，基准合同不受影响。
    > ⚠️ 若已执行 §1.4 的 DDL，这条 UPDATE 会被唯一索引拒绝（`1062`）——那本身就是 6.3 的证据。
    > 想验 4.5 就先跳过 DDL，或改用「先软删基准合同再造数」的顺序。

### 第五组：删除之后编号可以再用（场景 5.1–5.2）

17. **编号可复用**：新建合同编号 `QS-REUSE-001` → 保存成功 → 在列表里**删除**它
    → 再新建一份合同，编号同样填 `QS-REUSE-001` → 期望：**保存成功**。
18. **删除记录仍保留原编号**（场景 5.2）：
    ```bash
    docker exec -i newpm-mysql-1 mysql -uroot -ppassword ry-vue \
      -e "SELECT contract_id, del_flag, contract_code FROM pm_contract WHERE contract_code='QS-REUSE-001';"
    ```
    → 期望：两行——一行 `del_flag='1'`（已删）、一行 `del_flag='0'`（新建），
    **已删那行的编号字面值原样保留**，没有被清空或改写。

### 第六组：跨部门与数据库兜底（场景 6.1–6.3）

19. **跨部门检出**（场景 6.1）：用一个**只能看到某个部门数据**的非 admin 账号登录
    （admin 数据权限是全部，用 admin 验不出任何东西），新增合同并填 `QS-BASE-001`
    → 期望：**被拒**——即便他在合同列表里根本搜不到这份合同。
    > 没有现成的受限账号时，这一步靠单测的调用形态断言 + 生产上线后观测补齐，见 `bdd/coverage.md §二·6.1`。
20. **不泄露对方内容**（场景 6.2）：确认第 19 步的提示里**没有**对方合同的金额、客户、签订日期。
21. **数据库兜底**（场景 6.3，需先做 §1.4 的 DDL）：
    ```bash
    docker exec -i newpm-mysql-1 mysql -uroot -ppassword ry-vue \
      -e "SHOW CREATE TABLE pm_contract\G" | grep -i uk_contract_code_norm
    # 直接插一条同归一化编号的在用记录 → 期望报 ERROR 1062
    ```

---

## 四、自动化 e2e

```bash
# 确保前端(80)+后端(8080)在跑，且验证码已关
npx playwright test e2e-contract-code-unique.spec.js       # 新增：判重主用例（自带造数 + 自清理）
npx playwright test e2e-contract-code-unique-ui.spec.js    # 新增：失焦内联报错（场景 1.2）
npx playwright show-report
```

**必须保持绿的既有回归套件**（每一条都是设计约束的探针，红了就说明踩坏了不变式）：

```bash
npx playwright test contract-filter.spec.js                  # INV-1 模糊搜索没被改成精确
npx playwright test clear-field-guards-regression.spec.js    # INV-2 存量合同仍可全量 PUT
npx playwright test guard-payload-runtime.spec.js            # 前端 validator 未卡死真实 UI 保存
npx playwright test 006-code-review-fixes.spec.js            # 校验顺序未前置（「已关联其他合同」文案还在）
npx playwright test global-string-trim.spec.js               # INV-5 normalize 是全局 trim 的超集
npx playwright test e2e-contract-crud.spec.js                # 本次必改：:110-135 的盲取断言
```

> `e2e-contract-code-unique.spec.js` **自带造数，不抽样既有数据**。合同类 e2e 目前零 fixture，
> 「盲取 `rows[0]` 再断言」是既有套件所有数据漂移红灯的根因，新用例不要重蹈。

---

## 五、静态检查

```bash
mvn clean compile -pl ruoyi-project -am
cd ruoyi-ui && npx vue-tsc --noEmit
```

> `vue-tsc` 的判据是**本次改动的两个文件（`contract/add.vue`、`contract/edit.vue`）零错误**，
> 不是全局零错误——仓库存量基线约 39 个错误，不作门槛。

---

## 六、收尾恢复（**不要跳过**）

```bash
# 1. 验证码开回来
docker exec -i newpm-mysql-1 mysql -uroot -ppassword ry-vue \
  -e "UPDATE sys_config SET config_value='true' WHERE config_key='sys.account.captchaEnabled';"
docker exec -i newpm-redis-1 redis-cli --scan --pattern "sys_config:*" | xargs -r -n1 docker exec -i newpm-redis-1 redis-cli DEL

# 2. 清理手动造的合同（第三节建的都以 020/QS- 开头）
docker exec -i newpm-mysql-1 mysql -uroot -ppassword ry-vue \
  -e "SELECT contract_id, contract_name, contract_code, del_flag FROM pm_contract WHERE contract_name LIKE '020%' OR contract_code LIKE 'QS-%';"
# 确认无误后再软删，不要直接 DELETE
```

> 第 16 步用 SQL 改过 `020副本合同` 的编号，若不清理会在库里留一对同号脏数据，
> 下次跑 §1.4 的前置检查会被它挡住（那正是检查该干的事，但别让自己困惑）。

---

## 七、已知的坑

| 坑 | 表现 | 处置 |
|---|---|---|
| `vite.config.ts` 的 `baseUrl` 是硬编码常量 | 后端换端口后前端仍打旧端口，**静默串到另一个后端** | 改端口必须两处同步改 |
| 端口 80 需要 sudo | `npm run dev` 起不来 | 改用 5173 + `E2E_BASE_URL=http://localhost:5173` |
| `MySQL TRIM()` 不去 TAB | 手工写 SQL 核对时，肉眼一样的两个编号 `=` 比较不相等 | 核对一律带上 `REPLACE(...,CHAR(9),'')`，或先看 `LENGTH()` |
| 表单里直接敲 Tab 键会跳控件 | 无法输入制表符 | 用复制粘贴输入含 TAB 的编号 |
| `-am` + `-Dtest=` 不加 `failIfNoSpecifiedTests=false` | surefire 在 `ruoyi-common` 上直接报错中止 | 见 §二 |
| 中文 SQL 用 `-e` 内嵌 | 乱码 / 执行异常 | 一律走文件管道 + `--default-character-set=utf8mb4` |
