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

### 2.1 起隔离环境（不碰本机任何现有数据）

Docker 与 brew MySQL 两条路都可能是坏的（见 memory `local-e2e-isolated-mysql`）。
下面这套起一个完全独立的实例，跑完整个删掉：

```bash
SP=/tmp/pm-e2e            # 任选目录

# ---- MySQL（3307）----
mysqld --initialize-insecure --datadir=$SP/mysql-e2e
# socket 必须放短路径：路径超过 103 字符会报 "socket file path is too long"
nohup mysqld --datadir=$SP/mysql-e2e --port=3307 --socket=/tmp/mysql-e2e.sock \
  --mysqlx=OFF --pid-file=/tmp/mysql-e2e.pid \
  --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci \
  --log-error=$SP/mysql-e2e.err &

mysql --protocol=TCP -h 127.0.0.1 -P 3307 -u root -e \
  "CREATE DATABASE \`ry-vue\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ALTER USER 'root'@'localhost' IDENTIFIED BY 'password';"

# 导 schema —— --force 不能省：00_tables_ddl.sql 末尾的增量 ALTER 早被合并进 CREATE TABLE，
# 重复执行必报 Duplicate column（既有维护滞后，非本特性引入）
cat pm-sql/init/0{0,1,2}_*.sql | mysql --force --protocol=TCP -h 127.0.0.1 -P 3307 \
  -u root -ppassword --default-character-set=utf8mb4 ry-vue

# ---- Redis（6380，避开本机可能在用的 6379）----
nohup redis-server --port 6380 --save '' --appendonly no --dir $SP &

# ---- 关验证码（隔离实例跑完即销毁，无需恢复）----
mysql --protocol=TCP -h 127.0.0.1 -P 3307 -u root -ppassword ry-vue \
  -e "UPDATE sys_config SET config_value='false' WHERE config_key='sys.account.captchaEnabled';"
```

> 全新导入的库里 **admin 密码是 RuoYi 默认的 `admin123`**，不是长期库的 `123456789`；
> 且 `pm_daily_report_whitelist` 为空，无需处理白名单。

### 2.2 造数

```bash
cat tests/fixtures/015-daily-report-ownership-seed.sql | \
  mysql --protocol=TCP -h 127.0.0.1 -P 3307 -u root -ppassword \
        --default-character-set=utf8mb4 ry-vue
```

造出四个项目，覆盖全部四种归属状态：

| ID | 名称 | 阶段 | admin 的关系 | 在「我的项目」中？ | 用途 |
|---|---|---|---|---|---|
| 100 | 015在建项目A | 在建 | 现役成员 | ✅ 可见 | 作用范围内 |
| 200 | 015已结项项目B | **结项** | 现役成员 | ❌ 不可见 | 作用范围外（保护对象） |
| 300 | 015无关项目C | 在建 | **从未参与** | ❌ | 越权拒绝 |
| 400 | 015离场项目D | 在建 | **已离场** | ❌ | US4 放行 |

> fixture 里 `actual_workload` 必须与明细汇总自洽。写错的话，保存触发重算后
> 「人天不变」的断言就会失败——实测踩过这一下。

### 2.3 起后端并跑测试

```bash
mvn clean package -Dmaven.test.skip=true

java -jar ruoyi-admin/target/ruoyi-admin.jar \
  --spring.datasource.druid.master.url="jdbc:mysql://127.0.0.1:3307/ry-vue?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true" \
  --spring.data.redis.port=6380 &

E2E_API_URL=http://localhost:8085 E2E_ADMIN_PASSWORD=admin123 \
  npx playwright test e2e-daily-report-ownership.spec.js --reporter=list
```

**预期**：`10 passed`

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
pkill -f "ruoyi-admin.jar"
mysqladmin --protocol=TCP -h 127.0.0.1 -P 3307 -u root -ppassword shutdown
redis-cli -p 6380 shutdown nosave
rm -rf $SP/mysql-e2e /tmp/mysql-e2e.sock /tmp/mysql-e2e.pid
```

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
