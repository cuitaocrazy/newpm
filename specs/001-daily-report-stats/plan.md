# 实现计划：日报统计报表

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**分支**：`001-daily-report-stats` | **日期**：2026-03-17 | **规格**：[spec.md](spec.md)

**目标**：在日报管理菜单下新增"日报统计报表"页面，按月展示每天日报提交/未提交人数（按周分组），支持部门筛选、人员明细查看和 Excel 导出。

**架构**：后端在现有 `DailyReportController` 新增 3 个只读端点，复用 `@DataScope` 数据权限模式；前端新增 `weeklyStats.vue` 页面，使用 `project-dept-select` 组件，周次分组逻辑在前端计算；菜单和权限通过 SQL 脚本插入。

**技术栈**：Java 17 / Spring Boot 3.5.8 / MyBatis / Apache POI（双 Sheet 导出）；Vue 3 / TypeScript / Element Plus

---

## 技术上下文

**语言/版本**：Java 17（后端）、Vue 3 + TypeScript 5.6（前端）
**主要依赖**：Spring Boot 3.5.8、MyBatis、Apache POI（已在 pom.xml 中）、Element Plus 2.13
**存储**：MySQL 8.x，`pm_daily_report`、`pm_daily_report_detail`、`sys_user`、`sys_dept`、`pm_work_calendar`
**目标平台**：K3s 集群（Linux）+ 浏览器
**项目类型**：企业后台管理系统（Web Service + SPA）
**性能目标**：一整月数据（~31天 × N用户）在 3 秒内返回

---

## 宪法合规检查

| 原则 | 状态 | 说明 |
|------|------|------|
| I. 业务完整性 | ✅ | 只读功能，无 mutating 操作，无需 `@Log` |
| II. 权限驱动 | ✅ | 所有端点使用 `@PreAuthorize` + `@DataScope` |
| III. API 一致性 | ✅ | 扩展现有 `DailyReportController`，遵循 `BaseController` 规范 |
| IV. 关注点分离 | ✅ | 不涉及 `pm_project` 或 `pm_task` |
| V. 数据库规范 | ✅ | JOIN `sys_user`/`sys_dept` 时加 `COLLATE`；部门过滤用 `find_in_set` |
| VI. 前端组件 | ✅ | 部门选择使用 `<project-dept-select />`；不硬编码字典 |

---

## 项目结构

### 文档（本特性）

```text
specs/001-daily-report-stats/
├── plan.md              # 本文件
├── research.md          # 技术决策记录
├── data-model.md        # 数据模型和 SQL 设计
├── contracts/           # API 契约
└── tasks.md             # 任务清单（由 /speckit.tasks 生成）
```

### 源码（相对项目根目录）

```text
后端（ruoyi-project 模块）：
ruoyi-project/src/main/java/com/ruoyi/project/
├── domain/vo/
│   └── DailySubmissionStat.java          # 新增：每天统计 VO
├── mapper/
│   └── DailyReportMapper.java             # 修改：新增 3 个 Mapper 方法
├── service/
│   ├── IDailyReportService.java           # 修改：新增 3 个 Service 接口方法
│   └── impl/DailyReportServiceImpl.java   # 修改：实现 3 个新方法
└── controller/
    └── DailyReportController.java         # 修改：新增 3 个端点

ruoyi-project/src/main/resources/mapper/project/
└── DailyReportMapper.xml                  # 修改：新增 4 条 SQL 查询

前端（ruoyi-ui）：
ruoyi-ui/src/
├── api/project/dailyReport.js             # 修改：追加 3 个 API 函数
└── views/project/dailyReport/
    └── weeklyStats.vue                    # 新增：日报统计报表页面

数据库 SQL：
pm-sql/init/02_menu_data.sql               # 修改：追加日报统计报表菜单
pm-sql/fix_weekly_stats_menu_20260317.sql  # 新增：增量菜单 SQL（部署用，gitignored）
```

---

## Task 1：新增 DailySubmissionStat VO 类

**文件**：
- 新建：`ruoyi-project/src/main/java/com/ruoyi/project/domain/vo/DailySubmissionStat.java`

**步骤 1**：创建 `vo` 包目录（如不存在）

**步骤 2**：编写 VO 类

```java
package com.ruoyi.project.domain.vo;

public class DailySubmissionStat {
    private String reportDate;       // yyyy-MM-dd
    private String dayOfWeek;        // 周一~周日
    private Boolean isWorkday;       // 是否工作日
    private Integer submittedCount;  // 已提交人数
    private Integer unsubmittedCount; // 未提交人数

    // getters and setters
}
```

**步骤 3**：提交
```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/domain/vo/
git commit -m "feat: 新增 DailySubmissionStat VO 类"
```

---

## Task 2：Mapper 接口新增 4 个查询方法

**文件**：
- 修改：`ruoyi-project/src/main/java/com/ruoyi/project/mapper/DailyReportMapper.java`

**步骤 1**：在 `DailyReportMapper` 接口末尾添加以下方法声明

```java
/**
 * 按日期统计已提交人数（指定日期范围内每天去重计数）
 * 用于日报统计报表的汇总数据
 */
List<Map<String, Object>> selectSubmittedCountByDate(DailyReport query);

/**
 * 查询数据权限范围内的活跃用户总数
 * 用于计算未提交人数 = 总数 - 已提交数
 */
int selectTotalUserCount(DailyReport query);

/**
 * 查询某天已提交人员明细（含工时和工作内容摘要）
 */
List<Map<String, Object>> selectSubmittedUsersOnDate(DailyReport query);

/**
 * 查询某天未提交人员明细
 */
List<Map<String, Object>> selectUnsubmittedUsersOnDate(DailyReport query);
```

**步骤 2**：在 DailyReport 实体中确认或新增 transient 字段（如缺少）

检查 `DailyReport.java`，确保以下字段存在（用于 Query 参数传递）：
- `startDate`（String，`@JsonIgnore` 或直接用 `params` Map 传递）
- `endDate`（String）
- `reportDate`（String，单日查询用）
- `type`（String，`submitted`/`unsubmitted`）

**步骤 3**：提交
```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/mapper/DailyReportMapper.java
git commit -m "feat: DailyReportMapper 新增周统计查询方法"
```

---

## Task 3：DailyReportMapper.xml 新增 SQL

**文件**：
- 修改：`ruoyi-project/src/main/resources/mapper/project/DailyReportMapper.xml`

**步骤 1**：在 XML 文件末尾（`</mapper>` 之前）添加 4 条 SQL

```xml
<!-- 按日期统计已提交人数 -->
<select id="selectSubmittedCountByDate" parameterType="DailyReport" resultType="java.util.Map">
    SELECT r.report_date AS reportDate,
           COUNT(DISTINCT r.user_id) AS submittedCount
    FROM pm_daily_report r
    INNER JOIN sys_user u ON r.user_id = u.user_id
    LEFT JOIN sys_dept d ON u.dept_id = d.dept_id
    WHERE r.del_flag = '0'
      AND u.del_flag = '0' AND u.status = '0'
      AND r.report_date BETWEEN #{startDate} AND #{endDate}
      <if test="deptId != null">
          AND (u.dept_id = #{deptId}
               OR u.dept_id IN (SELECT dept_id FROM sys_dept
                                WHERE find_in_set(#{deptId}, ancestors) > 0))
      </if>
      ${params.dataScope}
    GROUP BY r.report_date
</select>

<!-- 查询数据权限范围内活跃用户总数 -->
<select id="selectTotalUserCount" parameterType="DailyReport" resultType="int">
    SELECT COUNT(*)
    FROM sys_user u
    LEFT JOIN sys_dept d ON u.dept_id = d.dept_id
    <where>
        u.del_flag = '0' AND u.status = '0'
        <if test="deptId != null">
            AND (u.dept_id = #{deptId}
                 OR u.dept_id IN (SELECT dept_id FROM sys_dept
                                  WHERE find_in_set(#{deptId}, ancestors) > 0))
        </if>
        ${params.dataScope}
    </where>
</select>

<!-- 某天已提交人员明细（含工时和工作内容摘要） -->
<select id="selectSubmittedUsersOnDate" parameterType="DailyReport" resultType="java.util.Map">
    SELECT u.user_id AS userId,
           u.nick_name COLLATE utf8mb4_unicode_ci AS nickName,
           d.dept_name AS deptName,
           r.total_work_hours AS totalWorkHours,
           LEFT(GROUP_CONCAT(
               dd.work_content ORDER BY dd.detail_id SEPARATOR '；'
           ), 50) AS workContentSummary
    FROM pm_daily_report r
    INNER JOIN sys_user u ON r.user_id = u.user_id
    LEFT JOIN sys_dept d ON u.dept_id = d.dept_id
    LEFT JOIN pm_daily_report_detail dd
           ON dd.report_id = r.report_id
          AND dd.entry_type = 'work'
          AND dd.del_flag = '0'
    WHERE r.del_flag = '0'
      AND u.del_flag = '0' AND u.status = '0'
      AND r.report_date = #{reportDate}
      <if test="deptId != null">
          AND (u.dept_id = #{deptId}
               OR u.dept_id IN (SELECT dept_id FROM sys_dept
                                WHERE find_in_set(#{deptId}, ancestors) > 0))
      </if>
      ${params.dataScope}
    GROUP BY r.report_id, u.user_id, u.nick_name, d.dept_name, r.total_work_hours
    ORDER BY d.dept_name, u.nick_name
</select>

<!-- 某天未提交人员明细 -->
<select id="selectUnsubmittedUsersOnDate" parameterType="DailyReport" resultType="java.util.Map">
    SELECT u.user_id AS userId,
           u.nick_name COLLATE utf8mb4_unicode_ci AS nickName,
           d.dept_name AS deptName
    FROM sys_user u
    LEFT JOIN sys_dept d ON u.dept_id = d.dept_id
    <where>
        u.del_flag = '0' AND u.status = '0'
        AND u.user_id NOT IN (
            SELECT r.user_id FROM pm_daily_report r
            WHERE r.report_date = #{reportDate} AND r.del_flag = '0'
        )
        <if test="deptId != null">
            AND (u.dept_id = #{deptId}
                 OR u.dept_id IN (SELECT dept_id FROM sys_dept
                                  WHERE find_in_set(#{deptId}, ancestors) > 0))
        </if>
        ${params.dataScope}
    </where>
    ORDER BY d.dept_name, u.nick_name
</select>
```

**步骤 2**：提交
```bash
git add ruoyi-project/src/main/resources/mapper/project/DailyReportMapper.xml
git commit -m "feat: DailyReportMapper.xml 新增周统计 SQL"
```

---

## Task 4：Service 接口和实现

**文件**：
- 修改：`ruoyi-project/src/main/java/com/ruoyi/project/service/IDailyReportService.java`
- 修改：`ruoyi-project/src/main/java/com/ruoyi/project/service/impl/DailyReportServiceImpl.java`

**步骤 1**：在 `IDailyReportService` 追加接口方法

```java
/**
 * 查询某月每天日报提交统计（已提交/未提交人数）
 * @param query 查询条件（yearMonth 必填，deptId 可选）
 */
List<DailySubmissionStat> selectWeeklyStats(DailyReport query);

/**
 * 查询某天已提交人员明细
 * @param query 查询条件（reportDate 必填，deptId 可选）
 */
List<Map<String, Object>> selectSubmittedDetail(DailyReport query);

/**
 * 查询某天未提交人员明细
 * @param query 查询条件（reportDate 必填，deptId 可选）
 */
List<Map<String, Object>> selectUnsubmittedDetail(DailyReport query);
```

**步骤 2**：在 `DailyReportServiceImpl` 实现 `selectWeeklyStats`

```java
@Override
@DataScope(deptAlias = "d", userAlias = "u")
public List<DailySubmissionStat> selectWeeklyStats(DailyReport query) {
    // 1. 解析月份，获取起止日期
    String yearMonth = query.getYearMonth(); // "2026-03"
    LocalDate start = YearMonth.parse(yearMonth).atDay(1);
    LocalDate end = YearMonth.parse(yearMonth).atEndOfMonth();
    query.setStartDate(start.toString());
    query.setEndDate(end.toString());

    // 2. 查询该月每天已提交人数 → Map<date, count>
    List<Map<String, Object>> submittedRows = dailyReportMapper.selectSubmittedCountByDate(query);
    Map<String, Integer> submittedMap = submittedRows.stream()
        .collect(Collectors.toMap(
            r -> r.get("reportDate").toString(),
            r -> ((Number) r.get("submittedCount")).intValue()
        ));

    // 3. 查询总用户数
    int total = dailyReportMapper.selectTotalUserCount(query);

    // 4. 查询工作日历（该月范围）
    // 注：workCalendarMapper 需 autowire，或在此用 JDBC 直接查
    Map<String, String> calendarMap = getWorkCalendarMap(start, end);

    // 5. 构建结果：遍历日期范围，逐天填充
    List<DailySubmissionStat> result = new ArrayList<>();
    String[] weekDays = {"", "周日", "周一", "周二", "周三", "周四", "周五", "周六"};
    for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
        String dateStr = date.toString();
        int submitted = submittedMap.getOrDefault(dateStr, 0);

        boolean workday;
        if (calendarMap.containsKey(dateStr)) {
            workday = "workday".equals(calendarMap.get(dateStr));
        } else {
            DayOfWeek dow = date.getDayOfWeek();
            workday = dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
        }

        DailySubmissionStat stat = new DailySubmissionStat();
        stat.setReportDate(dateStr);
        stat.setDayOfWeek(weekDays[date.getDayOfWeek().getValue() % 7 + 1]);
        stat.setIsWorkday(workday);
        stat.setSubmittedCount(submitted);
        stat.setUnsubmittedCount(total - submitted);
        result.add(stat);
    }
    return result;
}
```

**步骤 3**：实现 `selectSubmittedDetail` 和 `selectUnsubmittedDetail`

```java
@Override
@DataScope(deptAlias = "d", userAlias = "u")
public List<Map<String, Object>> selectSubmittedDetail(DailyReport query) {
    return dailyReportMapper.selectSubmittedUsersOnDate(query);
}

@Override
@DataScope(deptAlias = "d", userAlias = "u")
public List<Map<String, Object>> selectUnsubmittedDetail(DailyReport query) {
    return dailyReportMapper.selectUnsubmittedUsersOnDate(query);
}
```

**步骤 4**：新增私有辅助方法 `getWorkCalendarMap`

```java
private Map<String, String> getWorkCalendarMap(LocalDate start, LocalDate end) {
    // 查询 pm_work_calendar 日历表，返回 Map<date_str, day_type>
    // 通过 JdbcTemplate 或新增 WorkCalendarMapper 方法实现
    // 推荐：复用 WorkCalendarMapper（已存在），查询 del_flag='0'
    //       AND calendar_date BETWEEN start AND end
    // 返回示例：{"2026-01-01": "holiday", "2026-02-08": "workday"}
}
```

**步骤 5**：编译验证
```bash
mvn clean compile -pl ruoyi-project -am
```

**步骤 6**：提交
```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/service/
git commit -m "feat: DailyReportService 新增周统计查询方法"
```

---

## Task 5：Controller 新增 3 个端点

**文件**：
- 修改：`ruoyi-project/src/main/java/com/ruoyi/project/controller/DailyReportController.java`

**步骤 1**：追加以下 3 个方法

```java
/**
 * 日报周统计报表 - 按天汇总
 */
@PreAuthorize("@ss.hasPermi('project:dailyReport:weeklyStats')")
@GetMapping("/weeklyStats")
public AjaxResult weeklyStats(DailyReport dailyReport) {
    List<DailySubmissionStat> list = dailyReportService.selectWeeklyStats(dailyReport);
    return success(list);
}

/**
 * 日报周统计报表 - 人员明细
 */
@PreAuthorize("@ss.hasPermi('project:dailyReport:weeklyStats')")
@GetMapping("/weeklyStatsDetail")
public AjaxResult weeklyStatsDetail(DailyReport dailyReport) {
    List<Map<String, Object>> list;
    if ("submitted".equals(dailyReport.getType())) {
        list = dailyReportService.selectSubmittedDetail(dailyReport);
    } else {
        list = dailyReportService.selectUnsubmittedDetail(dailyReport);
    }
    return success(list);
}

/**
 * 日报周统计报表 - Excel 导出（双 Sheet）
 */
@PreAuthorize("@ss.hasPermi('project:dailyReport:weeklyStatsExport')")
@Log(title = "日报统计报表", businessType = BusinessType.EXPORT)
@GetMapping("/weeklyStatsExport")
public void weeklyStatsExport(HttpServletResponse response, DailyReport dailyReport) {
    List<DailySubmissionStat> statList = dailyReportService.selectWeeklyStats(dailyReport);
    dailyReportService.exportWeeklyStats(response, statList, dailyReport);
}
```

**步骤 2**：编译验证
```bash
mvn clean compile -pl ruoyi-project -am
```

**步骤 3**：提交
```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/controller/DailyReportController.java
git commit -m "feat: DailyReportController 新增日报统计报表端点"
```

---

## Task 6：Excel 导出实现（双 Sheet）

**文件**：
- 修改：`IDailyReportService.java`（新增 `exportWeeklyStats` 方法签名）
- 修改：`DailyReportServiceImpl.java`（实现导出逻辑）

**步骤 1**：在 `IDailyReportService` 新增方法
```java
void exportWeeklyStats(HttpServletResponse response, List<DailySubmissionStat> statList, DailyReport query);
```

**步骤 2**：在 `DailyReportServiceImpl` 实现，使用 Apache POI 创建双 Sheet Excel

```java
@Override
public void exportWeeklyStats(HttpServletResponse response,
                               List<DailySubmissionStat> statList,
                               DailyReport query) {
    try (XSSFWorkbook wb = new XSSFWorkbook()) {
        // Sheet 1：汇总表
        Sheet sheet1 = wb.createSheet("汇总");
        String[] headers1 = {"日期", "星期", "是否工作日", "已提交人数", "未提交人数"};
        // 写表头 + 数据行...

        // Sheet 2：明细表（遍历 statList 的每一天，查询 submitted + unsubmitted）
        Sheet sheet2 = wb.createSheet("明细");
        String[] headers2 = {"日期", "姓名", "部门", "提交状态", "工时合计"};
        // 写表头 + 数据行...

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String filename = URLEncoder.encode("日报统计报表_" + query.getYearMonth() + ".xlsx", "UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        wb.write(response.getOutputStream());
    } catch (Exception e) {
        throw new ServiceException("导出失败：" + e.getMessage());
    }
}
```

**步骤 3**：提交
```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/service/
git commit -m "feat: 日报统计报表 Excel 双Sheet导出"
```

---

## Task 7：前端 API 追加

**文件**：
- 修改：`ruoyi-ui/src/api/project/dailyReport.js`

**步骤 1**：在文件末尾追加 3 个函数

```javascript
// 日报统计报表 - 按天汇总（按月）
export function getWeeklyStats(query) {
  return request({
    url: '/project/dailyReport/weeklyStats',
    method: 'get',
    params: query
  })
}

// 日报统计报表 - 人员明细
export function getWeeklyStatsDetail(query) {
  return request({
    url: '/project/dailyReport/weeklyStatsDetail',
    method: 'get',
    params: query
  })
}

// 日报统计报表 - Excel 导出
export function exportWeeklyStats(query) {
  return request({
    url: '/project/dailyReport/weeklyStatsExport',
    method: 'get',
    params: query,
    responseType: 'blob'
  })
}
```

**步骤 2**：提交
```bash
git add ruoyi-ui/src/api/project/dailyReport.js
git commit -m "feat: dailyReport API 新增周统计查询函数"
```

---

## Task 8：前端页面 weeklyStats.vue

**文件**：
- 新建：`ruoyi-ui/src/views/project/dailyReport/weeklyStats.vue`

**页面核心逻辑**：

```typescript
// 1. 查询参数
const queryParams = reactive({
  yearMonth: dayjs().format('YYYY-MM'),  // 默认当月
  weekNum: '',                            // 周次筛选，空=全部
  deptId: null
})

// 2. 周次选项（根据 yearMonth 动态生成）
function computeWeekOptions(yearMonth: string) {
  const weeks = []
  const firstDay = dayjs(yearMonth + '-01')
  const lastDay = firstDay.endOf('month')
  let current = firstDay
  let weekNum = 1
  while (current.isBefore(lastDay) || current.isSame(lastDay, 'day')) {
    const weekStart = current.startOf('isoWeek')
    const weekEnd = current.endOf('isoWeek')
    const clampedStart = weekStart.isBefore(firstDay) ? firstDay : weekStart
    const clampedEnd = weekEnd.isAfter(lastDay) ? lastDay : weekEnd
    weeks.push({
      label: `第${weekNum}周（${clampedStart.format('MM-DD')}～${clampedEnd.format('MM-DD')}）`,
      value: weekNum,
      startDate: clampedStart.format('YYYY-MM-DD'),
      endDate: clampedEnd.format('YYYY-MM-DD')
    })
    current = weekEnd.add(1, 'day')
    weekNum++
  }
  return weeks
}

// 3. 将后端按天返回的数组分组为按周的结构
interface WeekGroup {
  weekNum: number
  startDate: string
  endDate: string
  label: string
  days: DailySubmissionStat[]
}

// 4. 点击人数 → 打开明细 Dialog
async function openDetail(date: string, type: 'submitted' | 'unsubmitted') {
  const res = await getWeeklyStatsDetail({ reportDate: date, type, deptId: queryParams.deptId })
  // 展示 res.data 到 Dialog
}
```

**步骤 1**：创建 Vue 文件

文件结构：
- `<template>`：查询栏（月份选择器 + 周次下拉 + 部门选择器 + 导出按钮）→ 按周分组的表格 → 明细 Dialog
- `<script setup lang="ts">`：上述逻辑
- `<style scoped>`：非工作日行灰色背景（`.non-workday { background: #f5f5f5; color: #aaa; }`）

**步骤 2**：提交
```bash
git add ruoyi-ui/src/views/project/dailyReport/weeklyStats.vue
git commit -m "feat: 日报统计报表前端页面 weeklyStats.vue"
```

---

## Task 9：菜单 SQL

**文件**：
- 修改：`pm-sql/init/02_menu_data.sql`（追加到日报管理菜单块末尾）
- 新建：`pm-sql/fix_weekly_stats_menu_20260317.sql`（gitignored，部署用）

**步骤 1**：在 `02_menu_data.sql` 日报管理菜单段末尾追加

```sql
-- ---- 日报统计报表 ----
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
VALUES ('日报统计报表', @dailyReportRootMenuId, 6, 'weeklyStats', 'project/dailyReport/weeklyStats', 1, 0, 'C', '0', '0', 'project:dailyReport:weeklyStats', 'bar-chart', 'admin', sysdate(), '', NULL, '日报统计报表菜单', 'DailyReportWeeklyStats');
SELECT @weeklyStatsMenuId := LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('查询', @weeklyStatsMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'project:dailyReport:weeklyStats', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('导出', @weeklyStatsMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'project:dailyReport:weeklyStatsExport', '#', 'admin', sysdate(), '', NULL, '');
```

**步骤 2**：创建增量 fix SQL（用于在已部署环境执行）

```sql
-- pm-sql/fix_weekly_stats_menu_20260317.sql
-- 在日报管理菜单下新增日报统计报表子菜单

SET @dailyReportRootMenuId = (SELECT menu_id FROM sys_menu WHERE menu_name = '日报管理' AND parent_id = 0 LIMIT 1);

INSERT INTO sys_menu (...) VALUES ('日报统计报表', @dailyReportRootMenuId, 6, 'weeklyStats', ...);
-- （同上完整 SQL）
```

**步骤 3**：在本地/测试环境执行 fix SQL 验证菜单显示正确

**步骤 4**：提交（只提交 02_menu_data.sql，fix SQL 不提交）
```bash
git add pm-sql/init/02_menu_data.sql
git commit -m "feat: 日报统计报表菜单数据"
```

---

## Task 10：集成验证

**步骤 1**：后端打包
```bash
mvn clean package -pl ruoyi-admin -am -Dmaven.test.skip=true
```

**步骤 2**：启动后端，验证 3 个端点（Swagger 或 curl）
```bash
# 汇总统计
curl "http://localhost:8080/project/dailyReport/weeklyStats?yearMonth=2026-03" -H "Authorization: Bearer {token}"
# 明细
curl "http://localhost:8080/project/dailyReport/weeklyStatsDetail?reportDate=2026-03-10&type=submitted" -H "..."
# 导出（应返回 xlsx 文件）
curl "http://localhost:8080/project/dailyReport/weeklyStatsExport?yearMonth=2026-03" -H "..." -o test.xlsx
```

**步骤 3**：前端启动，验证页面
```bash
cd ruoyi-ui && npm run dev
```
- 访问"日报统计报表"菜单
- 切换月份验证数据更新
- 切换周次筛选验证过滤效果
- 点击人数验证明细弹框
- 点击导出验证 xlsx 下载

**步骤 4**：在 K3s 远端执行 fix SQL
```bash
cat pm-sql/fix_weekly_stats_menu_20260317.sql | ssh k3s001 "kubectl exec -i mysql-0 -n newpm -- mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue"
```

**步骤 5**：最终提交（如有遗漏）并推送到 main
```bash
git push origin 001-daily-report-stats
# 合并到 main → CI/CD 自动部署
```
