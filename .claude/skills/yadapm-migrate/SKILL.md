---
name: yadapm-migrate
description: yadapm 功能迁移开发流水线。把老系统 yadapm 的某个功能（出入库版本/项目质量管理共5个菜单）迁移落地到 newpm，沉淀自"批次版本管理"已跑通的全流程。当用户说"开发非批次版本管理/旧数据查询/批次问题单/非批次问题单"、"迁移 yadapm 的XX功能"、"按批次版本管理那套做下一个功能"时使用。
argument-hint: "<功能名，如 非批次版本管理>"
---

# yadapm 功能迁移流水线

你是 yadapm → newpm 迁移开发助手。用户指定一个待迁移功能，你按本流程**端到端**交付：需求 → 设计 → 实现 → 测试三件套 → 验证 → 提交。本流程是从**已跑通的"批次版本管理"(spec 007)**沉淀的，包含全部踩过的坑。

## ⭐ 第一原则：以旧系统源码为准（最重要，先读这条）

**需求文档是导航地图，旧系统源码是唯一事实来源（source of truth）。**

- 5 个功能的需求文档（`docs/pm/yadapm需求-0X-*.md`）只给**方向和概览**，**不够详细、可能有遗漏或不准**。开工读它了解全貌即可，**绝不能只照需求文档写代码**。
- 具体开发的每一处字段、控件、联动、校验、默认值、只读、去重、字段语义、SQL 逻辑，**一律去旧系统源码里逐文件核实**，**以源码为准**。
- **必看旧系统的 HTML 模板（尤其 `<script>` 里的 JS）**：联动/去重/校验/readonly 等规则几乎都藏在 `templates/<模块>/create.html`·`edit.html`·`list.html`·`show.html` 的 JS 里，后端代码看不出来。批次版本管理踩的坑（子产品凭空多出、任务号该用下拉、版本简介借 taskName 列、提交人员只读、任务禁止重复选、升级包字段布局…）**全是需求文档没写细、但源码里写得清清楚楚的**，靠用户实操才补回。
- **冲突时以源码为准**，并**回写修正需求文档**（把核实到的细节补进对应 `yadapm需求-0X` 的字段表/差异表，让文档逐步变准）。
- 不确定/源码也看不明白的业务取舍（如新旧概念映射），**问用户**，别擅自决定。

**旧系统源码根**：`/Users/kongli/Documents/companyProjectDoc/ConfigDoc/ConfigItemDocuments/一部项目管理源码和资料-华冰提供/yadapm`
逐功能要读的源码：`src/main/scala/com/yada/mag/{web,service,dao,model,query}/<模块>*` + `src/main/resources/templates/<模块>/*.html`（含建表SQL在 `document/sql/`）。

## 一、待迁移功能清单

| 功能 | 老系统URL前缀 | 老模块名 | 需求文档(导航) | 复杂度 |
|---|---|---|---|---|
| 批次版本管理 | /storage | storage | `docs/pm/yadapm需求-01-批次版本管理.md` | ✅ 已完成(样板, spec 007) |
| 非批次版本管理 | /storageManual | storageManual | `docs/pm/yadapm需求-02-非批次版本管理.md` | 中(共用 pm_version_out, manual_input=1) |
| 旧数据查询 | /oldStorage | oldStorage | `docs/pm/yadapm需求-03-旧数据查询.md` | 低(只读) |
| 批次任务问题单及缺陷 | /proListAndDefect | proListAndDefect | `docs/pm/yadapm需求-04-批次任务问题单及缺陷.md` | 高(含附件) |
| 非批次任务问题单及缺陷 | /proNoBatchListAndDefect | proNoBatchListAndDefect | `docs/pm/yadapm需求-05-非批次任务问题单及缺陷.md` | 中 |

## 二、开工前必读

1. **先读需求文档了解全貌**，再**逐文件精读旧系统源码核实细节**（第一原则）。
2. **以批次版本管理为蓝本**：参考 `specs/007-batch-version-management/` 设计 + `ruoyi-project` 下 `VersionOut*`/`SysName*`/`VersionNumberGenerator` + `ruoyi-ui/src/views/project/versionOut/` 四件套 + `tests/e2e-version-out-crud.spec.js`。
3. 旧系统是 Oracle+Scala+Thymeleaf+Shiro，newpm 是 MySQL+SpringBoot3+Vue3+SpringSecurity：类型/语法/鉴权/模板都要换壳。

## 三、流水线步骤（speckit 驱动主干 + 手动分段验证）

> **核心方式（C）**：设计阶段**全程用 /speckit-* skill**串起来；实现阶段用 **`/speckit-implement` 起底**（生成骨架、铺代码），但**保留手动分段验证 + review 的灵活性**——每段实现后立即测试/浏览器验证/必要时插入修正，不是跑完一把 implement 就完事。
>
> 完整 speckit 调用链（批次版本管理实测走通的）：
> `/speckit-git-feature` → `/speckit-specify` → `/speckit-plan` → `/speckit-tasks` → `/speckit-analyze` → `/speckit-implement`（分段）→ 各段 `/speckit-git-commit`。
> 每个 speckit skill 触发后会输出可选/强制钩子（如 before_specify 的 git-feature、after_* 的 git-commit），按提示执行。

### Step 0 — 切分支：`/speckit-git-feature <短名>`
每个功能独立分支（sequential 自动取下一个 NNN，如 008/009…）。注意 `/speckit-specify` 的 before_specify 钩子也会触发它，别重复切。

### Step 1 — 设计三件套（全程 speckit）
1. **`/speckit-specify`** —— 功能描述里**引用需求文档路径 + 旧系统源码模块路径**，让 spec 基于源码事实而非凭空。产出 spec.md（用户故事P1..Pn + FR + SC + 假设）。
2. **`/speckit-plan`** —— 产出 plan.md + research/data-model/contracts。**对照源码把字段/规则/核心算法就地写清楚**（如版本号生成规则全表），不要只留"详见XX"链接。
3. **`/speckit-tasks`** —— 产出 tasks.md，分阶段：Setup → Foundational → US1(MVP) → US2.. → Polish。
4. **`/speckit-analyze`** —— 跨文档一致性体检，**修掉 MEDIUM 及以上再往下**（批次版本管理就靠它抓出 product/子产品、版本状态枚举等问题）。

### Step 2 — 实现：`/speckit-implement` 起底 + 手动分段验证（C 的核心）
用 **`/speckit-implement`** 读 tasks.md 驱动实现，但**按 tasks 的阶段分段做、每段验证**（批次版本管理实测分了 5 段：MVP新增→查询→改删→导出→Polish）：

**每段内部顺序**（后端→前端→验证）：
- 后端（ruoyi-project）：domain → mapper(+xml) → service接口 → serviceImpl → controller → 前端api骨架。
  - 继承 `BaseController`，list 首行 `startPage()`，`@PreAuthorize`+`@Log`。
  - 列表子表聚合（多任务号等）用 `GROUP_CONCAT` 子查询；创建/修改人昵称 JOIN `sys_user` **必加 `COLLATE utf8mb4_unicode_ci`**。
  - 唯一性防并发：组合唯一键 + 插入失败重试(≤3)。
- 前端（ruoyi-ui）：四件套 index/add/edit/detail + api ts，参考 versionOut/。
  - 字典用 `<dict-select>`/`<dict-tag>`；服务端排序 `sortable=custom`；搜索缓存 sessionStorage。
  - **add≡edit（同字段同布局）；detail=add/edit超集+审计字段；index查询条件和列对齐老系统**。
  - 联动/去重/readonly/默认值**全部照旧系统 HTML 的 JS 实现**（第一原则）。
- **每段验证**：重建jar重启后端 → 关验证码 → API冒烟/浏览器点验 → **及时插入 review 修正**（这是 C 比纯 implement 灵活的地方）。
- 每段用 **`/speckit-git-commit`**（或 after_implement 钩子）提交。

数据库（在 Setup 段做）：新表写 `pm-sql/init/00_tables_ddl.sql`、字典 `01_tables_data.sql`、菜单 `02_menu_data.sql`；另产可移植迁移脚本 `pm-sql/migration_<NNN>_<feature>.sql`（建表+列+字典+菜单一文件，菜单用 `LAST_INSERT_ID` 取父id）；应用到本地 + 刷 Redis 字典缓存。

### Step 3 — 测试三件套（强制，不可跳过）
1. **单元测试**（JUnit5+Mockito，service层）：核心算法+CRUD+重试/级联分支+各转发方法。**逻辑类（算法+Service）指令覆盖率 ≥ 90%**（实体getter/setter、Controller不靠单测）。
   跑：`mvn test -pl ruoyi-project -am -Dtest=XxxServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
2. **E2E**（Playwright API驱动 `tests/e2e-<feature>-crud.spec.js`）：覆盖 Controller **全部端点**，用 `setupApi()`（`tests/helpers/api-client.js`，支持 `E2E_BASE_URL`）。**必须真跑通**（批次版本管理犯过"写了从没跑、还过时"的错）。
3. **JaCoCo**：已配进根 pom，`mvn test` 自动出 `target/site/jacoco/index.html`，提取数字确认达标。
4. **Playwright 浏览器 UI 验证（强制走完整流程，不是只截图看渲染）**：必须**真点击操作**走完整 CRUD，因为很多 bug API/E2E 测不到（前端表单漏传字段、联动、回显）：
   - **新增页**：依次选 年份→批次（看投产日期带出）→产品（看子系统联动）→子系统（看基准版本号带出）→版本类型（看出入库版本号实时生成）→填手填项 → **点保存** → 看是否跳列表。
   - **列表页**：看新记录出现、各列值对、字典标签渲染、提交人员/创建人有值。
   - **详情页**：点详情，核对全字段（尤其审计字段、JOIN 出的人员姓名）。
   - **编辑页**：点编辑看回显，改关键字段看版本号重算，保存。
   - ⚠️ **重点查"提交人员/创建人"等 JOIN 字段在列表/详情是否为空**——为空说明前端表单漏传了 id（见坑14）。
   - 截图存证。**只截一张静态图 = 没验**（批次版本管理就因只截新增页没点，漏了 commName bug 到非批次才发现）。

### Step 4 — 收尾
- 全量回归 `mvn test -pl ruoyi-project -am`（零回归）+ 前端 `npm run build:prod`（退出码0）。
- 把核实到的细节**回写修正需求文档**，差异表标记完成。
- 可选跑 `/speckit-analyze` 复检 spec↔plan↔tasks↔实现一致性。
- 最终 `/speckit-git-commit` 提交，commit 含做了什么+验证结果。

## 四、避坑清单（实战教训，完整版见 `docs/pm/yadapm迁移踩坑记录.md`）

> 完整坑表（现象+根因+解法）见 `docs/pm/yadapm迁移踩坑记录.md`。**开发新功能先扫一遍**；遇到新坑**随手追加到该文档，并同步回下表**。

| 坑 | 教训（以源码为准就能避免） |
|---|---|
| 只读后端漏前端规则 | **必读 create.html/edit.html 的 JS**：联动、去重(`taskNoFun`)、readonly、校验 |
| 字段"一列两义" | 老 `T_B_VERSION_OUT.TASK_NAME`：批次=版本简介、非批次=任务名称。按 manual_input 分别解释 |
| 凭空多字段 | 老"产品"存 subVersionCode 一个字段，**没有"子产品"**。别拆两个 |
| 接口返回裸字符串 | RuoYi `success(String)` 把值塞 msg 不是 data。返回数据用 `success(对象)` 或前端从已有选项取 |
| 提交人员可改 | 老系统 readonly：新增=当前用户、编辑=记录原始提交人 |
| 任务可重复选 | 老 `taskNoFun` 禁止重复：已选任务在其它行 disabled |
| 升级包字段单独占行 | 条件显示字段(v-if)用自适应列宽融入整行 |
| 列表/详情字段缺失 | 对齐老 show.html/list.html 全字段(含审计4字段、多任务号聚合列) |
| JOIN 字符集报错 | PM表与系统表 JOIN 必加 `COLLATE utf8mb4_unicode_ci` |
| E2E 跑不起来 | ①80端口常被占→`E2E_BASE_URL=http://localhost:8090`；②容器闲置会挂→先查MySQL/Redis健康，挂了先`docker start`再重启后端 |
| **共用实体 @Validated 误伤** | 多模块共用一个 domain 时，实体 `@NotBlank/@NotNull` 是"全字段集"约束。被某模块裁剪掉的字段，其校验会在该模块 `@Validated` 时误伤（报"XX不能为空"但 XX 根本不在该模块）。对策：复用实体但裁字段的模块，Controller **去掉 `@Validated`**，改 service 层手动校验真正必填项 |
| **软删除占用唯一键** | 业务唯一键(如 out_lib_version)软删记录仍占号→删后重建同值撞键。对策：**软删时改写唯一字段加 `_DEL_{id}` 后缀腾位**(`set del_flag='1', x=concat(x,'_DEL_',id)`)，唯一键不含 del_flag。⚠️别把 del_flag 纳入唯一键(会导致两条软删同号互撞)。**E2E连续CRUD才暴露**，手工点验碰不到 |
| **只读字段漏传 id** | 提交人员/创建人等只读显示昵称的字段，前端只 `:value=昵称` 展示，**忘了把 id 放进 form 提交** → 存了空 → 列表/详情 JOIN 不出姓名(空白)。对策：onMounted 设 `form.commName=userStore.id`。**API/E2E测不到**(测试直接传了id)，**只有浏览器真点保存+看详情才暴露** |

## 五、本地环境与命令速查

- 后端 **8085**（不是8080！），前端 vite 默认80（被占时 `npm run dev -- --port 8090 --host 0.0.0.0`），代理 `/dev-api→8085`。
- 登录 `admin`/`123456789`；MySQL容器 `newpm-mysql-1`、Redis `newpm-redis-1`，密码 `password`，库 `ry-vue`。
- **跑e2e/浏览器验证前临时关验证码，跑完恢复**（本地可逆）：
  ```bash
  docker exec -i newpm-mysql-1 mysql -u root -ppassword ry-vue -e "UPDATE sys_config SET config_value='false' WHERE config_key='sys.account.captchaEnabled';"
  docker exec -i newpm-redis-1 redis-cli DEL "sys_config:sys.account.captchaEnabled"
  # 恢复：把 false 改回 true，同样删缓存
  ```
- 改后端Java/Mapper后**必须重建jar并重启**：
  ```bash
  mvn clean package -pl ruoyi-admin -am -Dmaven.test.skip=true
  OLDPID=$(lsof -ti :8085); [ -n "$OLDPID" ] && kill $OLDPID; sleep 3
  nohup java -jar ruoyi-admin/target/ruoyi-admin.jar > /tmp/newpm-backend.log 2>&1 &   # 等 "Started RuoYiApplication"
  ```
- 应用中文SQL：`cat xxx.sql | docker exec -i newpm-mysql-1 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue`
- 刷字典缓存：`docker exec -i newpm-redis-1 redis-cli DEL sys_dict:<type>`
- 容器挂了：`docker start newpm-mysql-1 newpm-redis-1`，等就绪后重启后端。
- 跑E2E：`E2E_BASE_URL=http://localhost:8090 npx playwright test e2e-<feature>-crud.spec.js --reporter=list`

## 六、完成标准（Definition of Done）

- [ ] **全程走 speckit 链**：git-feature→specify→plan→tasks→analyze→implement(分段)→git-commit，产物 spec/plan/tasks 齐全
- [ ] **每处实现都核对过旧系统源码**（非仅照需求文档），核实细节已回写需求文档
- [ ] 单元测试逻辑类覆盖率 ≥ 90%，全量 `mvn test` 零回归
- [ ] E2E 覆盖 Controller 全端点，**真跑通过**
- [ ] Playwright 浏览器验证关键页面，截图确认
- [ ] 前端 `build:prod` 退出码 0
- [ ] 可移植迁移SQL已产出
- [ ] 分段提交，commit 含验证结果
- [ ] 验证码已恢复、需求文档差异表已更新

> 数据迁移（老库存量导入）是独立后续任务，依赖旧库数据到位，不阻塞功能开发。
