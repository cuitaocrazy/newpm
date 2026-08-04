# Quickstart：本地验证 015 特性

**Feature**: 015-daily-report-ownership-check
**最后实测**: 2026-08-03，全部通过（单测 192 / e2e 10 / 对账 3 项）

本文件是**实测走通的可复现流程**，不是设计推演。照着走一遍即可确认改动是否生效。

---

## 1. 单元测试（最快，无需任何外部依赖）

服务层单测用 JUnit 5 + Mockito，不需要 MySQL / Redis：

```bash
mvn test -pl ruoyi-project -am -Dtest=DailyReportServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false
```

> ⚠️ `-Dsurefire.failIfNoSpecifiedTests=false` 不能省。`-am` 会连带构建 `ruoyi-common`，
> 那里没有这个测试类，surefire 会直接报错中止。（CLAUDE.md 里记的命令缺这个参数，实测会挂。）

**预期**：`Tests run: 46, Failures: 0, Errors: 0`
全模块 `mvn test -pl ruoyi-project -am` → `Tests run: 192, Failures: 0`

015 新增的测试在 `@DisplayName` 里以前缀标注类型，**维护时必须区分**：

- `[TDD]` — 描述改造前不存在的行为。新写时必须先失败。
- `[护栏]` — 描述改造前后都应成立的行为。**失败即表示踩坏了现状**，不要改测试去迁就实现。

---

## 2. 端到端测试（需要真实数据库）

**为什么不能只跑单测**：本特性有 4 条不变式是纯数据库层的——「被保留的明细字段逐字未变」
靠的是「范围外的行压根没被 touch」，而单测把 Mapper 全 mock 掉了，无从验证。
**实测中 e2e 抓出了 2 个单测完全抓不到的缺陷**（见 §5）。

### 2.0 ⚠️ 先读这段：本套件是破坏性的，打错库会毁真实数据

本套件会**重写 admin 当日日报、删除日报主记录与明细**，而日报是硬删除、无历史版本，
误删只能靠 6 小时一次的 OSS 归档备份（需付费解冻）。因此它**只能打在专用造数库上**。

**2026-08-03 真实事故**：执行者设了 `E2E_BASE_URL`（那是另两套 daily-report e2e 的变量），
本套件认的却是 `E2E_API_URL`，于是静默回落到默认的 `localhost:8085`——开发者自己的 dev 后端，
背后是从生产同步的真实库。没有任何征兆：登录成功、请求成功，只是断言失败，
而其中一次「空 `detailList` 保存」已经把一条日报的明细清空、汇总归零。

事后已加两道闸门，但**它们只在你按本文档搭环境时才成立**：

| 闸门 | 位置 | 拦什么 |
|---|---|---|
| SIGNAL 闸门 | fixture SQL 开头 | 往真实库**灌造数**（有效项目数 > 20 即中止） |
| 造数库闸门 | spec 的 `beforeAll` | 拿真实库**跑用例**（admin 名下有非 990xxx 项目即中止） |

两道各自独立、缺一不可，且都已做过活性验证（故意打真实库 → 确认被拒）。

### 2.1 起专用造数库（docker，不碰 dev 库 `ry-vue`）

**2026-08-03 实测通过的路线**。在既有 docker MySQL 里开一个独立库，
用 **dev 库的纯结构**而不是 `pm-sql/init`——后者与线上有 schema 漂移
（实测缺 `sys_menu.active_menu`、`province_code`，导致菜单/权限只导进一部分）：

```bash
SP=/tmp/pm-e2e; mkdir -p $SP
M="docker exec -i newpm-mysql-1 mysql -uroot -ppassword"

# ---- 建库 ----
$M -e "DROP DATABASE IF EXISTS \`ry_vue_e2e\`;
       CREATE DATABASE \`ry_vue_e2e\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# ---- 纯结构（--no-data，不带任何业务数据）----
docker exec newpm-mysql-1 mysqldump -uroot -ppassword --no-data --skip-add-drop-table \
  --routines --default-character-set=utf8mb4 ry-vue > $SP/structure.sql
$M --default-character-set=utf8mb4 ry_vue_e2e < $SP/structure.sql

# ---- 只导 sys_* 基准数据 + 日历 + 白名单；业务表全留空 ----
#      业务表留空 → pm_project 为 0 行 → fixture 的 SIGNAL 闸门才会放行
docker exec newpm-mysql-1 mysqldump -uroot -ppassword --no-create-info \
  --default-character-set=utf8mb4 ry-vue \
  sys_user sys_role sys_menu sys_role_menu sys_dept sys_post sys_user_role sys_user_post \
  sys_dict_type sys_dict_data sys_config pm_work_calendar pm_daily_report_whitelist \
  > $SP/sysdata.sql
$M --default-character-set=utf8mb4 ry_vue_e2e < $SP/sysdata.sql

# ---- 关验证码 + 把 admin 移出白名单（白名单用户禁止提交日报）----
$M ry_vue_e2e -e "
  UPDATE sys_config SET config_value='false' WHERE config_key='sys.account.captchaEnabled';
  DELETE FROM pm_daily_report_whitelist WHERE user_id=1;"
```

> 这条路线复用 dev 库的 `sys_user`，因此 **admin 密码与 dev 库相同**
> （不是 RuoYi 默认的 `admin123`——那只适用于从 `pm-sql/init` 全新导入的库）。
> 造数库是一次性的，跑完 `DROP DATABASE` 即可，无需恢复任何配置。

> Redis 用 `--spring.data.redis.database=3` 换库索引即可，不必另起实例——
> 但**不能省**：与主环境共用 db 0 会串字典缓存和验证码。

### 2.2 造数

```bash
cat tests/fixtures/015-daily-report-ownership-seed.sql | \
  docker exec -i newpm-mysql-1 mysql -uroot -ppassword \
    --default-character-set=utf8mb4 ry_vue_e2e
# 期望输出：015 e2e 造数完成（安全闸门已通过）
```

造出四个项目，覆盖全部四种归属状态。**ID 走 990xxx 高位号段**——初版用过
`100/200/300/400`，而那些在真实库里全是真实数据，跑一次会删掉 2 个真实项目、
22 条成员记录、3 条他人日报。低位 ID + 无差别 DELETE 是数据事故的标准配方：

| ID | 名称 | 阶段 | admin 的关系 | 在「我的项目」中？ | 用途 |
|---|---|---|---|---|---|
| 990100 | 015在建项目A | 在建 | 现役成员 | ✅ 可见 | 作用范围内 |
| 990200 | 015已结项项目B | **结项** | 现役成员 | ❌ 不可见 | 作用范围外（保护对象） |
| 990300 | 015无关项目C | 在建 | **从未参与** | ❌ | 越权拒绝 |
| 990400 | 015离场项目D | 在建 | **已离场** | ❌ | US4 放行 |
| 990500 | 015仅角色项目E | 在建 | **市场经理，但成员表无行** | ✅ 可见 | 读写口径同源（2026-08-04 新增） |

> 990500 刻意**不插** `pm_project_member` 行 —— 复刻生产上「历史项目漏同步
> syncProjectMembers」的真实形态（市场经理缺行 30 个 / 销售经理缺行 27 个）。
> `myProjects` 的 OR 列表含 `market_manager_id`，所以填报页会列出它；写侧若只认成员行，
> 该账号当日整张日报永久保存不了。它是「口径同源」两条用例的造数依赖。

日报 `991000`–`991003`，其中 **`991003` 属于 user_id=2**——它是 Issue #13 归属校验的
唯一证据：其余 991xxx 全是 admin 自己的，删与不删都是绿的。

> fixture 里 `actual_workload` 必须与明细汇总自洽（990100 = 12.00，含他人日报 991003 的 4h）。
> 写错的话 §3 的 SC-008 对账会报差异，而那条对账的唯一目的就是发现这种不一致——
> 执行者会把造数缺陷误判成产品缺陷。实测踩过这一下。

### 2.3 起后端并跑测试

```bash
mvn clean package -Dmaven.test.skip=true

# ⚠️ 命令行 --spring.datasource.druid.master.url 在本项目【不生效】：
#    DruidConfig 的 masterDataSource 走 DruidDataSourceBuilder.create().build()
#    + 方法级 @ConfigurationProperties，命令行覆盖在这条链上不可靠。
#    2026-08-03 实测：加了这个参数，后端仍然连的是 application-druid.yml 里的 ry-vue。
#    必须用 additional-location 覆盖文件。
cat > $SP/e2e-override.yml <<'YML'
server:
  port: 8087            # 与主环境 8085 错开
spring:
  datasource:
    druid:
      master:
        url: jdbc:mysql://localhost:3306/ry_vue_e2e?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true
        username: root
        password: <本地 docker MySQL 的 root 口令，见 CLAUDE.md「Database & SQL Management」>
  data:
    redis:
      database: 3       # 与主环境 db 0 错开，避免串字典缓存/验证码
YML

java -jar ruoyi-admin/target/ruoyi-admin.jar \
  --spring.config.additional-location="file:$SP/e2e-override.yml" &

# ⚠️ 变量名是 E2E_API_URL，不是 E2E_BASE_URL（后者属另两套 daily-report e2e）。
#    设错时本套件静默回落到 localhost:8085 = 你自己的 dev 后端 → 破坏性写入真实库（见 §2.0）。
E2E_API_URL=http://localhost:8087 E2E_ADMIN_PASSWORD=<dev 库的 admin 密码> \
  npx playwright test e2e-daily-report-ownership.spec.js --reporter=list
```

**预期**：`16 passed`。若看到
`[造数库闸门] 拒绝执行`，说明 `E2E_API_URL` 指向的库不对——**不要绕过它**，
它拦下的正是会毁真实数据的那一次执行。

> 计数变更记录：14 → 16（2026-08-04，集成收尾）。新增的两条是「口径同源」组
> ——「担任项目角色但成员表无行须放行」与其反向护栏「毫无关系仍须被拒」。
> 配套造数新增项目 **990500**（`market_manager_id = 1`，**刻意不插 `pm_project_member` 行**）。
> ⚠️ 这两条用例与 990500 是在**禁用数据库访问**的集成会话里写的，**未经实际执行**；
> 首次跑要留意是造数问题还是产品问题（对应的单测护栏已绿：
> `DailyReportServiceImplTest#saveDailyReport_projectRoleWithoutMemberRow_isAccepted`
> 等 4 条）。

**跑同一批改动时要一起过的另外三套**（变量约定各不相同，这本身就是坑）：

| 套件 | 变量 | 破坏性？ | 打哪个库 |
|---|---|---|---|
| `e2e-daily-report-ownership` | `E2E_API_URL`（直连后端） | **是** | 造数库 |
| `e2e-team-daily-workload` | `E2E_BASE_URL` + `/dev-api` 代理 | **是**（建项目、存 admin 日报、删项目） | 造数库 |
| `e2e-daily-report` | `E2E_BASE_URL` + `/dev-api` 代理 | 否（纯只读） | 真实库（需要真实数据） |
| `e2e-mobile-daily-report` | `E2E_BASE_URL`（走前端） | **是**（存/删 admin 当天日报） | 造数库 |

走 `/dev-api` 前缀的套件需要一个剥前缀的反代或 vite dev server；
mobile 套件走前端页面，必须起 vite（worktree 的 `vite.config.ts` 里
`baseUrl` 硬编码 `http://localhost:8080`，所以造数库后端要监听 8080），
且 mobile 需要造数库里同时有「普通项目」与「带任务的项目」各一个。

> 该 spec **直连后端**、不经前端 dev server，因此不用 `tests/helpers/api-client.js`
> （后者带 `/dev-api` 前缀、依赖 vite 代理），**也不需要起前端**。
> 改了后端代码必须重新 `package` 再重启——MyBatis XML 是打进 jar 的。

---

## 3. 对账（SC-008 / SC-010）

这三条需要跨全表聚合，HTTP 接口给不出来，只能直连数据库。**都应无输出（或 0）**：

```sql
-- SC-008：每个项目的实际人天 == 其全部日报明细汇总
select p.project_id, p.project_name, p.actual_workload,
       coalesce((select sum(d.work_hours) from pm_daily_report_detail d
                 join pm_daily_report r on r.report_id = d.report_id and r.del_flag = '0'
                 where d.project_id = p.project_id and d.entry_type = 'work'), 0) as detail_sum
from pm_project p
where p.del_flag = '0'
having abs(p.actual_workload - detail_sum) > 0.001;

-- SC-010①：不存在孤立明细（有工时记录、但其主记录已被软删）
select count(*) as orphan_details
from pm_daily_report_detail d
join pm_daily_report r on r.report_id = d.report_id
where r.del_flag = '1';

-- SC-010②：每条日报主记录的汇总工时 == 其 work 明细之和
select r.report_id, r.report_date, r.total_work_hours,
       coalesce((select sum(d.work_hours) from pm_daily_report_detail d
                 where d.report_id = r.report_id and d.entry_type = 'work'), 0) as detail_sum
from pm_daily_report r
where r.del_flag = '0'
having abs(r.total_work_hours - detail_sum) > 0.001;
```

**2026-08-03 实测**：三条全部通过。SC-010② 在修复前会命中一行（见 §5 ②）。

---

## 4. 清理

```bash
# 停掉本次起的临时进程（按端口精确停，不要 pkill 掉主环境的后端）
for p in 8087 8080; do
  PID=$(lsof -nP -iTCP:$p -sTCP:LISTEN | awk 'NR==2{print $2}')
  [ -n "$PID" ] && kill "$PID"
done

# 造数库是一次性的，直接删；Redis db 3 随库一起弃用，无需清
docker exec -i newpm-mysql-1 mysql -uroot -ppassword -e "DROP DATABASE IF EXISTS \`ry_vue_e2e\`;"

# 若跑过 mobile 套件（需要 vite）：软链进来的 node_modules 必须删，
# 否则会以 ?? 混进 git status 被误提交（.gitignore 只匹配目录，软链不匹配）
rm -f ruoyi-ui/node_modules
rm -f auto-imports.d.ts        # vite 跑起来时生成的构建产物
```

**清理后必做的环境审计**——只确认「造数库删了」不够，还要确认**真实库没被碰**。
用生产库当基准（只读）：

```sql
-- 真实库 ry-vue 上执行，全部应为 0（admin 在生产有 0 条日报，是最灵敏的探针）
SELECT 'admin 日报数 [生产=0]' k, COUNT(*) v FROM pm_daily_report WHERE user_id=1
UNION ALL SELECT '99xxxx 日报残留', COUNT(*) FROM pm_daily_report WHERE report_id>=990000
UNION ALL SELECT '99xxxx 明细残留', COUNT(*) FROM pm_daily_report_detail WHERE report_id>=990000
UNION ALL SELECT '孤儿明细', COUNT(*) FROM pm_daily_report_detail d
  WHERE NOT EXISTS (SELECT 1 FROM pm_daily_report r WHERE r.report_id=d.report_id)
UNION ALL SELECT '孤儿任务', COUNT(*) FROM pm_task t
  WHERE NOT EXISTS (SELECT 1 FROM pm_project p WHERE p.project_id=t.project_id);
```

排查真实库是否被误写时，**`sys_oper_log` 是最快的路径**：`oper_param` 存着请求体原文，
按 `oper_time` + `oper_url LIKE '%dailyReport%'` 就能把落地的每一次写入连同 payload 列出来，
`status=1` 是被拒（无写入）、`status=0` 是真的执行了。同一张表还能找回被覆盖前的原始
`detailList`，用于还原——2026-08-03 的事故定位与还原全靠它，不必读代码猜。

---

## 5. e2e 抓出的两个缺陷（单测完全抓不到）

记录在此，因为它们正是这一步不能省的理由。

### ① 删除后返回 500「操作失败」，但数据其实已正确处理

明细全部因不可见而被保留时，没有主记录可删，`rows = 0`，被 `BaseController.toAjax` 判为失败。
数据层面三条断言全对，接口却报错——**填报人会看到失败提示并重复点击**。

- 修复：返回「本次处理的日报条数」，而非「删掉了几条主记录」
- 回归：单测 `deleteDailyReport_allPreserved_stillReportsSuccess`
  + e2e「US1b｜回归：明细全部被保留、无主记录可删时，仍须返回成功」

### ② 保存后当日汇总工时偏小

`totalWorkHours` 只累加**提交内容**，不含被保留的不可见明细。
实测：明细之和 5.00，主记录写的却是 3.00 —— 日历卡上当日工时会偏小，违反 SC-010。
删除路径实现了汇总重算，保存路径漏了。

- 修复：保存既有日报后按 `sumWorkHoursByReportId` 重算
- 回归：单测 `saveDailyReport_totalWorkHours_includesPreservedDetails` + §3 的 SC-010②

**共同点**：两者都是「单测验证了 Mapper 被正确调用、却没验证语义后果」。
凡是断言「某个值没有改变」或「返回值的业务含义」的，单测通常无能为力。
