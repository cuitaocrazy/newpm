# 日报假期类型功能实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 日报明细支持请假/倒休/年假条目，与项目工时并列录入；左侧日历和动态页按类型用不同颜色区分展示。

**Architecture:** 复用 `pm_daily_report_detail` 表，新增 `entry_type`（work/leave/comp/annual）和 `leave_hours` 列；假期行 `project_id` 允许为 NULL。月览接口额外返回 `leaveSummary`（GROUP_CONCAT聚合）供日历角标使用；详情接口原样返回 detailList，前端按 `entryType` 区分渲染。

**Tech Stack:** Java 17 / MyBatis XML / Spring Boot 3 / Vue 3 / Element Plus

---

## Task 1: DDL 变更 + 字典数据

**Files:**
- Create: `pm-sql/fix_daily_report_leave_type_20260308.sql`
- Modify: `pm-sql/init/00_tables_ddl.sql`（同步 DDL 定义）
- Modify: `pm-sql/init/01_tables_data.sql`（同步字典数据）

**Step 1: 创建 fix SQL 文件**

内容如下（执行 DDL + 字典数据）：

```sql
-- 1. pm_daily_report_detail 新增字段
ALTER TABLE pm_daily_report_detail
  MODIFY COLUMN `project_id` bigint DEFAULT NULL COMMENT '项目ID，entry_type=work时必填',
  MODIFY COLUMN `work_content` text DEFAULT NULL COMMENT '工作内容，假期行可为空',
  ADD COLUMN `entry_type` varchar(20) NOT NULL DEFAULT 'work'
      COMMENT '条目类型: work=项目工时 / leave=请假 / comp=倒休 / annual=年假'
      AFTER `project_stage`,
  ADD COLUMN `leave_hours` decimal(5,2) DEFAULT NULL
      COMMENT '假期时长(小时)，entry_type非work时使用'
      AFTER `entry_type`;

-- 2. 字典类型
INSERT INTO sys_dict_type (dict_name, dict_type, status, remark, create_by, create_time)
VALUES ('日报条目类型', 'sys_rbtype', '0', '日报明细行的类型', 'admin', NOW());

-- 3. 字典值
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
VALUES
  (1, '项目工时', 'work',   'sys_rbtype', '', 'primary', 'N', '0', 'admin', NOW()),
  (2, '请假',     'leave',  'sys_rbtype', '', 'danger',  'N', '0', 'admin', NOW()),
  (3, '倒休',     'comp',   'sys_rbtype', '', 'warning', 'N', '0', 'admin', NOW()),
  (4, '年假',     'annual', 'sys_rbtype', '', 'success', 'N', '0', 'admin', NOW());
```

**Step 2: 在本地 MySQL 执行**

```bash
cat pm-sql/fix_daily_report_leave_type_20260308.sql | docker exec -i 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue
```

预期输出：无报错

**Step 3: 同步 00_tables_ddl.sql**

在 `pm_daily_report_detail` 的 `project_stage` 字段后追加：
```sql
`entry_type` varchar(20) NOT NULL DEFAULT 'work' COMMENT '条目类型: work=项目工时 / leave=请假 / comp=倒休 / annual=年假',
`leave_hours` decimal(5,2) DEFAULT NULL COMMENT '假期时长(小时)，entry_type非work时使用',
```
同时将 `project_id` 和 `work_content` 改为 `DEFAULT NULL`。

**Step 4: 同步 01_tables_data.sql**（追加字典 SQL，同 Step 1 中字典部分）

**Step 5: Commit**

```bash
git add pm-sql/
git commit -m "feat: 日报明细表新增 entry_type/leave_hours 字段 + sys_rbtype 字典"
```

---

## Task 2: 后端 — DailyReportDetail 实体 + DailyReportDetailMapper

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/domain/DailyReportDetail.java`
- Modify: `ruoyi-project/src/main/resources/mapper/project/DailyReportDetailMapper.xml`

**Step 1: DailyReportDetail.java 新增字段**

在 `workContent` 字段后添加：

```java
/** 条目类型: work=项目工时 / leave=请假 / comp=倒休 / annual=年假 */
private String entryType;

/** 假期时长(小时)，entryType非work时使用 */
private BigDecimal leaveHours;
```

同时添加 getter/setter：

```java
public void setEntryType(String entryType) { this.entryType = entryType; }
public String getEntryType() { return entryType; }

public void setLeaveHours(BigDecimal leaveHours) { this.leaveHours = leaveHours; }
public BigDecimal getLeaveHours() { return leaveHours; }
```

**Step 2: DailyReportDetailMapper.xml — resultMap 新增字段映射**

在 `DetailResult` resultMap 中追加（在 `workContent` 后）：

```xml
<result property="entryType"   column="entry_type"  />
<result property="leaveHours"  column="leave_hours" />
```

**Step 3: selectByReportId 查询 SQL 新增字段**

将 `dd.work_hours, dd.work_content` 改为：
```xml
dd.entry_type, dd.leave_hours, dd.work_hours, dd.work_content,
```

**Step 4: batchInsert SQL 更新**

将原 INSERT 语句：
```xml
insert into pm_daily_report_detail (report_id, project_id, project_stage, work_hours, work_content, del_flag, create_by, create_time)
values
<foreach ...>
    (#{item.reportId}, #{item.projectId}, #{item.projectStage}, #{item.workHours}, #{item.workContent}, '0', #{item.createBy}, #{item.createTime})
</foreach>
```

改为：
```xml
insert into pm_daily_report_detail (report_id, project_id, project_stage, entry_type, leave_hours, work_hours, work_content, del_flag, create_by, create_time)
values
<foreach item="item" index="index" collection="list" separator=",">
    (#{item.reportId}, #{item.projectId}, #{item.projectStage},
     #{item.entryType, jdbcType=VARCHAR},
     #{item.leaveHours, jdbcType=DECIMAL},
     #{item.workHours},
     #{item.workContent, jdbcType=VARCHAR},
     '0', #{item.createBy}, #{item.createTime})
</foreach>
```

**Step 5: sumWorkHoursByProjectId 只计 work 行**

```xml
<select id="sumWorkHoursByProjectId" parameterType="Long" resultType="java.math.BigDecimal">
    select coalesce(sum(work_hours), 0)
    from pm_daily_report_detail
    where project_id = #{projectId}
      and entry_type = 'work'
</select>
```

**Step 6: Commit**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/domain/DailyReportDetail.java
git add ruoyi-project/src/main/resources/mapper/project/DailyReportDetailMapper.xml
git commit -m "feat: DailyReportDetail 支持 entry_type/leave_hours 字段"
```

---

## Task 3: 后端 — DailyReportMapper.xml 更新

**Files:**
- Modify: `ruoyi-project/src/main/resources/mapper/project/DailyReportMapper.xml`
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/domain/DailyReport.java`

**Step 1: DailyReport.java 新增 leaveSummary 字段**

用于月览接口（日历角标），值格式为 `leave:2.00,comp:1.00`：

```java
/** 假期摘要（月览接口用）格式: "leave:2.00,comp:1.00" */
private String leaveSummary;

public void setLeaveSummary(String leaveSummary) { this.leaveSummary = leaveSummary; }
public String getLeaveSummary() { return leaveSummary; }
```

**Step 2: DailyReportResult resultMap 新增 leaveSummary**

```xml
<result property="leaveSummary" column="leave_summary" />
```

**Step 3: DailyReportWithDetailResult collection 新增字段映射**

在 collection 内追加（`revenueConfirmYear` 后）：

```xml
<result property="entryType"  column="detail_entry_type"  />
<result property="leaveHours" column="detail_leave_hours"  />
```

**Step 4: selectReportBase SQL 新增 leaveSummary 子查询**

在 `r.total_work_hours` 后追加：

```xml
(SELECT GROUP_CONCAT(CONCAT(dd_l.entry_type, ':', dd_l.leave_hours) SEPARATOR ',')
 FROM pm_daily_report_detail dd_l
 WHERE dd_l.report_id = r.report_id
   AND dd_l.entry_type != 'work') AS leave_summary,
```

**Step 5: selectReportWithDetail SQL 新增 detail 字段**

在 `p.revenue_confirm_year as detail_revenue_confirm_year` 后追加：

```xml
dd.entry_type    as detail_entry_type,
dd.leave_hours   as detail_leave_hours,
```

**Step 6: Commit**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/domain/DailyReport.java
git add ruoyi-project/src/main/resources/mapper/project/DailyReportMapper.xml
git commit -m "feat: DailyReport 月览接口带 leaveSummary，明细接口带 entryType/leaveHours"
```

---

## Task 4: 后端 — DailyReportServiceImpl 更新

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/DailyReportServiceImpl.java`

**Step 1: saveDailyReport — totalWorkHours 只计 work 行**

将：
```java
for (DailyReportDetail detail : detailList) {
    if (detail.getWorkHours() != null) {
        totalWorkHours = totalWorkHours.add(detail.getWorkHours());
    }
}
```

改为：
```java
for (DailyReportDetail detail : detailList) {
    if ("work".equals(detail.getEntryType()) && detail.getWorkHours() != null) {
        totalWorkHours = totalWorkHours.add(detail.getWorkHours());
    }
}
```

**Step 2: saveDailyReport — entryType 默认值兜底**

在 `detailMapper.batchInsert(detailList)` 之前，遍历 detailList 补充 entryType 默认值：

```java
for (DailyReportDetail detail : detailList) {
    detail.setReportId(report.getReportId());
    detail.setCreateBy(username);
    // entryType 默认 work
    if (detail.getEntryType() == null || detail.getEntryType().isEmpty()) {
        detail.setEntryType("work");
    }
    // 假期行 workContent 默认空字符串
    if (!"work".equals(detail.getEntryType()) && detail.getWorkContent() == null) {
        detail.setWorkContent("");
    }
    // 假期行 workHours = leaveHours（统一用 workHours 字段存，查询时 leaveHours 为展示用）
    if (!"work".equals(detail.getEntryType()) && detail.getLeaveHours() != null) {
        detail.setWorkHours(detail.getLeaveHours());
    }
}
```

**Step 3: saveDailyReport — 项目工时更新只处理 work 行**

将：
```java
Set<Long> affectedProjectIds = (detailList != null ? detailList : ...)
    .stream()
    .filter(d -> d.getProjectId() != null)
    .map(DailyReportDetail::getProjectId)
    .collect(Collectors.toSet());
```

改为：
```java
Set<Long> affectedProjectIds = (detailList != null ? detailList : java.util.Collections.<DailyReportDetail>emptyList())
    .stream()
    .filter(d -> d.getProjectId() != null && "work".equals(d.getEntryType()))
    .map(DailyReportDetail::getProjectId)
    .collect(Collectors.toSet());
```

**Step 4: 重新构建并验证**

```bash
cd /Users/kongli/ws-claude/PM/newpm
mvn clean compile -pl ruoyi-project -am -q
```

预期：BUILD SUCCESS，无编译错误

**Step 5: Commit**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/service/impl/DailyReportServiceImpl.java
git commit -m "feat: saveDailyReport 支持假期行，totalWorkHours 只计 work 行"
```

---

## Task 5: 前端 write.vue — 假期记录区块 + 保存逻辑

**Files:**
- Modify: `ruoyi-ui/src/views/project/dailyReport/write.vue`

**Step 1: script — 假期常量 + 假期列表响应式数据**

在 `const colors = [...]` 之后添加：

```js
// 假期类型颜色（不与现有绿/橙冲突）
const LEAVE_TYPE_COLOR = { leave: '#f56c6c', comp: '#b37feb', annual: '#36cfc9' }
const LEAVE_TYPE_LABEL = { leave: '请假', comp: '倒休', annual: '年假' }

// 假期记录列表（当天）
const leaveList = ref([])
```

**Step 2: script — loadDayReport 加载假期行**

在 `formList.value = projects.value.map(...)` 之后，追加加载假期行：

```js
leaveList.value = (report?.detailList || [])
  .filter(d => d.entryType && d.entryType !== 'work')
  .map(d => ({
    entailId: d.detailId,
    entryType: d.entryType,
    leaveHours: parseFloat(d.leaveHours || d.workHours) || 0,
    remark: d.remark || ''
  }))
```

**Step 3: script — handleSave 合并假期行**

将原 `details` 过滤后，追加假期行：

```js
// 原有项目工时行
const details = formList.value
  .filter(f => f.workHours > 0 && f.workContent && f.workContent.trim())
  .map(f => ({
    projectId: f.projectId,
    projectStage: f.projectStage,
    workHours: f.workHours,
    workContent: f.workContent,
    entryType: 'work'
  }))

// 追加假期行
const leaveDetails = leaveList.value
  .filter(l => l.entryType && l.leaveHours > 0)
  .map(l => ({
    projectId: null,
    workHours: l.leaveHours,
    workContent: '',
    entryType: l.entryType,
    leaveHours: l.leaveHours,
    remark: l.remark || ''
  }))

const allDetails = [...details, ...leaveDetails]

if (allDetails.length === 0) {
  ElMessage.warning('请至少填写一个项目的工时或假期记录')
  return
}
// 原 saveDailyReport 改为传 allDetails
await saveDailyReport({ reportDate: selectedDate.value, detailList: allDetails })
```

**Step 4: script — handleDelete 清空假期列表**

在 `formList.value.forEach(...)` 之后追加：

```js
leaveList.value = []
```

**Step 5: template — 假期记录区块（在 project-list div 之后添加）**

```html
<!-- 假期记录区块 -->
<div class="leave-section">
  <div class="leave-header">
    <span class="leave-title">假期记录</span>
    <el-button
      type="primary" size="small" plain
      :disabled="!isEditable"
      @click="leaveList.push({ entryType: 'leave', leaveHours: 1, remark: '' })"
    >+ 添加假期</el-button>
  </div>
  <div v-if="leaveList.length === 0" class="leave-empty">暂无假期记录</div>
  <div v-for="(item, idx) in leaveList" :key="idx" class="leave-item">
    <span class="leave-color-dot" :style="{ background: LEAVE_TYPE_COLOR[item.entryType] || '#ccc' }"></span>
    <el-select v-model="item.entryType" size="small" style="width: 90px;" :disabled="!isEditable">
      <el-option label="请假" value="leave" />
      <el-option label="倒休" value="comp" />
      <el-option label="年假" value="annual" />
    </el-select>
    <el-input-number
      v-model="item.leaveHours"
      :min="0.5" :max="24" :step="0.5" :precision="1"
      size="small" style="width: 120px;"
      :disabled="!isEditable"
    />
    <span style="color: #909399; font-size: 13px;">小时</span>
    <el-input
      v-model="item.remark"
      placeholder="备注(选填)"
      size="small"
      style="flex: 1;"
      :disabled="!isEditable"
    />
    <el-button link type="danger" icon="Delete" :disabled="!isEditable"
      @click="leaveList.splice(idx, 1)" />
  </div>
</div>
```

**Step 6: style — 假期区块样式**

```css
.leave-section {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 14px 16px;
  margin-top: 8px;
}
.leave-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.leave-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.leave-empty {
  font-size: 13px;
  color: #c0c4cc;
  text-align: center;
  padding: 8px 0;
}
.leave-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.leave-color-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
```

**Step 7: Commit**

```bash
git add ruoyi-ui/src/views/project/dailyReport/write.vue
git commit -m "feat: write.vue 日报填写支持假期记录区块"
```

---

## Task 6: 前端 write.vue — 左侧日历角标显示假期

**Files:**
- Modify: `ruoyi-ui/src/views/project/dailyReport/write.vue`

**Step 1: script — monthReports 改为存完整对象**

将 `loadMonthOverview` 函数中的 map 构建改为：

```js
async function loadMonthOverview() {
  const res = await listDailyReport({ yearMonth: currentYearMonth.value, userId: userStore.id })
  const map = {}
  if (res.rows) {
    res.rows.forEach(r => {
      const day = r.reportDate?.substring(0, 10)
      if (day) {
        map[day] = {
          workHours: Number(r.totalWorkHours),
          leaveSummary: r.leaveSummary || ''  // 格式: "leave:2.00,comp:1.00"
        }
      }
    })
  }
  monthReports.value = map
}
```

**Step 2: script — 辅助函数 getDateWorkHours / getDateLeaveEntries**

将原 `getDateHours` 改名为 `getDateWorkHours`，并新增 `getDateLeaveEntries`：

```js
function getDateWorkHours(dateStr) {
  return monthReports.value[dateStr]?.workHours || 0
}

// 解析 leaveSummary 为数组: [{type:'leave', hours:2}, ...]
function getDateLeaveEntries(dateStr) {
  const summary = monthReports.value[dateStr]?.leaveSummary
  if (!summary) return []
  return summary.split(',').map(seg => {
    const [type, hours] = seg.split(':')
    return { type, hours: parseFloat(hours) || 0 }
  }).filter(e => e.type && e.hours > 0)
}
```

**Step 3: script — getDayBadgeClass 改用 getDateWorkHours**

将 `getDayBadgeClass` 中所有 `const hours = monthReports.value[dateStr]` 改为 `const hours = getDateWorkHours(dateStr)`。

**Step 4: template — 日历格子渲染假期角标**

将 `<template #default="{ day, dateStr, isToday, dayType }">` 内的 `<span class="cal-hours">` 改为：

```html
<template #default="{ day, dateStr, isToday, dayType }">
  <div class="cal-cell">
    <span class="mc-day-num" :class="getDayBadgeClass(dateStr, dayType, isToday)">{{ day }}</span>
    <span v-if="getDateWorkHours(dateStr)" class="cal-hours" :class="getHoursClass(dateStr)">
      {{ getDateWorkHours(dateStr) }}h
    </span>
    <span v-for="le in getDateLeaveEntries(dateStr)" :key="le.type"
      class="cal-leave-badge"
      :style="{ color: LEAVE_TYPE_COLOR[le.type] }">
      {{ LEAVE_TYPE_LABEL[le.type] }}{{ le.hours }}h
    </span>
  </div>
</template>
```

同步 `getHoursClass` 也改用 `getDateWorkHours`：

```js
function getHoursClass(dateStr) {
  const hours = getDateWorkHours(dateStr)
  if (!hours) return ''
  if (hours > 8) return 'hours-over'
  if (hours >= 8) return 'hours-ok'
  return 'hours-warn'
}
```

**Step 5: style — 假期角标样式**

```css
.cal-leave-badge {
  font-size: 10px;
  font-weight: 600;
  margin-top: 1px;
  line-height: 1.2;
}
```

**Step 6: Commit**

```bash
git add ruoyi-ui/src/views/project/dailyReport/write.vue
git commit -m "feat: write.vue 日历角标显示假期类型和小时数"
```

---

## Task 7: 前端 activity.vue — 假期行渲染 + 图例

**Files:**
- Modify: `ruoyi-ui/src/views/project/dailyReport/activity.vue`

**Step 1: script — 假期常量（同 write.vue）**

在 `const projectColors = [...]` 之后添加：

```js
const LEAVE_TYPE_COLOR = { leave: '#f56c6c', comp: '#b37feb', annual: '#36cfc9' }
const LEAVE_TYPE_LABEL = { leave: '请假', comp: '倒休', annual: '年假' }
```

**Step 2: template — 个人模式格子内假期行**

在 `<!-- 个人模式：显示项目详情 -->` 的 `cell-projects` div 内，`v-for="detail in getCellDetails(dateStr)"` 区块之后，追加假期行：

```html
<!-- 假期行 -->
<div v-for="leave in getCellLeaves(dateStr)" :key="leave.detailId || leave.entryType"
  class="cell-leave">
  <span class="cell-leave-dot" :style="{ background: LEAVE_TYPE_COLOR[leave.entryType] }"></span>
  <span class="cell-leave-label" :style="{ color: LEAVE_TYPE_COLOR[leave.entryType] }">
    {{ LEAVE_TYPE_LABEL[leave.entryType] }}
  </span>
  <span class="cell-leave-h">{{ leave.leaveHours || leave.workHours }}h</span>
</div>
```

**Step 3: script — getCellDetails 只返回 work 行**

```js
function getCellDetails(dateStr) {
  const people = dataByDate.value[dateStr] || []
  const report = people.find(r => r.userId === queryParams.value.userId)
  return (report?.detailList || []).filter(d => !d.entryType || d.entryType === 'work')
}

// 新增：假期行
function getCellLeaves(dateStr) {
  const people = dataByDate.value[dateStr] || []
  const report = people.find(r => r.userId === queryParams.value.userId)
  return (report?.detailList || []).filter(d => d.entryType && d.entryType !== 'work')
}
```

**Step 4: template — 团队抽屉 drawer 内假期行**

在 `v-for="detail in person.detailList"` 的 `drawer-prj` div 内，将原来的遍历改为只遍历 work 行，并在其后追加假期行：

```html
<!-- work 行 -->
<div v-for="detail in person.detailList.filter(d => !d.entryType || d.entryType === 'work')"
  :key="detail.detailId" class="drawer-prj">
  <!-- 原有内容不变 -->
</div>
<!-- 假期行 -->
<div v-for="leave in person.detailList.filter(d => d.entryType && d.entryType !== 'work')"
  :key="'leave-' + leave.detailId" class="drawer-leave">
  <span class="cell-leave-dot" :style="{ background: LEAVE_TYPE_COLOR[leave.entryType] }"></span>
  <span :style="{ color: LEAVE_TYPE_COLOR[leave.entryType], fontWeight: 600 }">
    {{ LEAVE_TYPE_LABEL[leave.entryType] }}
  </span>
  <span style="margin-left: auto; font-weight: 700;">{{ leave.leaveHours || leave.workHours }}h</span>
  <span v-if="leave.remark" style="font-size: 12px; color: #909399; margin-left: 8px;">{{ leave.remark }}</span>
</div>
```

**Step 5: template — 图例更新**

将原图例区：
```html
<div class="legend">
  <span class="legend-item"><span class="legend-dot" style="background:#67c23a"></span> >=8h</span>
  <span class="legend-item"><span class="legend-dot" style="background:#e6a23c"></span> &lt;8h</span>
  <span class="legend-item"><span class="legend-dot" style="background:#409eff"></span> &gt;8h</span>
</div>
```

改为：
```html
<div class="legend">
  <span class="legend-item"><span class="legend-dot" style="background:#67c23a"></span>项目满勤(≥8h)</span>
  <span class="legend-item"><span class="legend-dot" style="background:#e6a23c"></span>项目不满(&lt;8h)</span>
  <span class="legend-item"><span class="legend-dot" style="background:#409eff"></span>项目超时(&gt;8h)</span>
  <span class="legend-item"><span class="legend-dot" style="background:#f56c6c"></span>请假</span>
  <span class="legend-item"><span class="legend-dot" style="background:#b37feb"></span>倒休</span>
  <span class="legend-item"><span class="legend-dot" style="background:#36cfc9"></span>年假</span>
</div>
```

**Step 6: style — 假期行样式**

```css
.cell-leave {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 0;
  font-size: 11px;
}
.cell-leave-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.cell-leave-label { font-weight: 600; }
.cell-leave-h { font-weight: 700; margin-left: auto; }
.drawer-leave {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px solid #f5f7fa;
  font-size: 13px;
}
```

**Step 7: Commit**

```bash
git add ruoyi-ui/src/views/project/dailyReport/activity.vue
git commit -m "feat: activity.vue 假期行渲染 + 图例更新"
```

---

## Task 8: 远程数据库执行 DDL + 验证

**Step 1: 执行 DDL 到 K3s 生产库**

```bash
cat pm-sql/fix_daily_report_leave_type_20260308.sql | ssh k3s001 "kubectl exec -i mysql-0 -n newpm -- mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue"
```

**Step 2: 构建后端并重启**

```bash
mvn clean package -Dmaven.test.skip=true
./ry.sh restart
```

**Step 3: 本地联调验证**

1. 打开日报填写页，验证：
   - 假期记录区块显示正常
   - 可添加/删除假期条目
   - 保存后左侧日历显示工时 + 假期角标（如"6h 请2h"）
2. 打开日报动态页个人模式，验证：
   - 格子内显示项目条目 + 假期行（颜色区分）
3. 团队抽屉，验证：
   - 假期行追加在项目列表后

**Step 4: Commit + Push**

```bash
git push
```

---

## 颜色规范速查

| 类型 | 色值 |
|------|------|
| 请假 `leave` | `#f56c6c` |
| 倒休 `comp`  | `#b37feb` |
| 年假 `annual`| `#36cfc9` |
| 项目满勤     | `#00b42a` / `#67c23a` |
| 项目不满     | `#ff7d00` / `#e6a23c` |
