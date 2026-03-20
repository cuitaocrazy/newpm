# Implementation Plan: 日报假期模块扩展

**Branch**: `004-daily-report-leave-types` | **Date**: 2026-03-20 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/004-daily-report-leave-types/spec.md`

## Summary

新增婚假/产假/丧假三种假期类型（字典扩展），并提供「批量填假期」功能（选日期范围，自动跳过周末/节假日，支持冲突跳过/覆盖）。假期颜色改为字典 `list_class` 驱动，前端零硬编码。后端新增 `POST /project/dailyReport/batchLeave` 接口；现有单日保存、leaveSummary 统计无需改动。

## Technical Context

**Language/Version**: Java 17 / TypeScript 5.6
**Primary Dependencies**: Spring Boot 3.5.8, MyBatis, Vue 3.5, Element Plus 2.13
**Storage**: MySQL 8.x (`ry-vue`)，涉及表：`sys_dict_data`、`pm_daily_report`、`pm_daily_report_detail`、`pm_work_calendar`
**Testing**: Playwright E2E (`tests/`)
**Target Platform**: Web（Spring Boot 后端 + Vue 3 前端）
**Project Type**: Web application（企业级项目管理系统模块）
**Performance Goals**: 批量填写 90 天范围接口 < 3s 响应（90 次单日 UPSERT 事务）
**Constraints**: 不新增数据库表；不修改 `pm_daily_report_detail` 表结构；批量填写不清除已有工时记录
**Scale/Scope**: 单用户操作，无并发竞争，数据量小

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 原则 | 检查项 | 状态 |
|------|--------|------|
| I. 业务完整性 | `batchLeave` 接口有 `@Log(title="日报管理", businessType=INSERT)` | ✅ Pass |
| I. 业务完整性 | 软删除策略：`pm_daily_report`/`pm_daily_report_detail` 使用硬删除（现有策略，不变）| ✅ Pass |
| II. 权限驱动 | `batchLeave` 有 `@PreAuthorize("@ss.hasAnyPermi('project:dailyReport:add,project:dailyReport:edit')")` | ✅ Pass |
| III. API 一致性 | Controller extends BaseController，返回 `AjaxResult` | ✅ Pass |
| III. 前端规范 | 使用 `import request from '@/utils/request'`，不使用 proxy.$http | ✅ Pass |
| IV. 关注点分离 | 不涉及 `pm_task`/`pm_project` 字段 | ✅ Pass |
| V. 数据库规范 | 字典新增用 `fix_004_leave_types_20260320.sql`（gitignored）| ✅ Pass |
| VI. 前端字典规范 | 假期类型下拉用 `sys_rbtype.filter(d => d.value !== 'work')`，颜色动态化 | ✅ Pass |

**Constitution Check Post-Design**: 设计未引入新违规项，所有原则通过。无需 Complexity Tracking。

## Project Structure

### Documentation (this feature)

```text
specs/004-daily-report-leave-types/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 research output
├── data-model.md        # Phase 1 data model
├── contracts/api.md     # Phase 1 API contracts
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code

```text
backend/
ruoyi-project/src/main/java/com/ruoyi/project/
├── controller/
│   └── DailyReportController.java          # 新增 batchLeave endpoint
├── service/
│   └── IDailyReportService.java            # 新增 batchSaveLeave 方法签名
├── service/impl/
│   └── DailyReportServiceImpl.java         # 实现 batchSaveLeave 逻辑
└── domain/dto/
    └── BatchLeaveRequest.java              # 新增 DTO

pm-sql/
└── fix_004_leave_types_20260320.sql        # 新增3条字典记录（gitignored）

frontend/
ruoyi-ui/src/
├── views/project/dailyReport/
│   └── write.vue                           # 批量填假期按钮+弹窗；颜色动态化
└── api/project/
    └── dailyReport.js                      # 新增 batchSaveLeave 函数
```

**Structure Decision**: 标准 RuoYi 前后端分离结构，所有 Java 业务代码在 `ruoyi-project` 模块，前端在 `ruoyi-ui`。

## Implementation Steps

### Step 1 — 字典数据（SQL）

**文件**: `pm-sql/fix_004_leave_types_20260320.sql`

新增 3 条 `sys_rbtype` 字典记录：

```sql
insert into sys_dict_data values(384, 5, '婚假', 'marriage',    'sys_rbtype', '', 'primary', 'N', '0', 'admin', sysdate(), '', NULL, NULL);
insert into sys_dict_data values(385, 6, '产假', 'maternity',   'sys_rbtype', '', 'warning', 'N', '0', 'admin', sysdate(), '', NULL, NULL);
insert into sys_dict_data values(386, 7, '丧假', 'bereavement', 'sys_rbtype', '', 'info',    'N', '0', 'admin', sysdate(), '', NULL, NULL);
```

同步更新 `pm-sql/init/01_tables_data.sql`（追加相同 3 行，用于全量初始化）。

---

### Step 2 — BatchLeaveRequest DTO（新增文件）

**文件**: `ruoyi-project/src/main/java/com/ruoyi/project/domain/dto/BatchLeaveRequest.java`

```java
package com.ruoyi.project.domain.dto;

import java.math.BigDecimal;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class BatchLeaveRequest {
    @NotBlank(message = "假期类型不能为空")
    private String entryType;

    @NotBlank(message = "开始日期不能为空")
    private String startDate;   // yyyy-MM-dd

    @NotBlank(message = "结束日期不能为空")
    private String endDate;     // yyyy-MM-dd

    private BigDecimal leaveHoursPerDay = BigDecimal.valueOf(8);

    private String conflictStrategy = "skip"; // skip | overwrite

    // getters/setters
}
```

---

### Step 3 — IDailyReportService 新增方法

**文件**: `IDailyReportService.java`，追加：

```java
/**
 * 批量填写假期
 * @param request 批量请求（类型、日期范围、时长、冲突策略）
 * @return 操作摘要 Map {totalWorkdays, created, skipped, overwritten}
 */
Map<String, Integer> batchSaveLeave(BatchLeaveRequest request);
```

---

### Step 4 — DailyReportServiceImpl 实现批量填写

**文件**: `DailyReportServiceImpl.java`，新增方法：

核心逻辑：
1. 校验 `startDate <= endDate`，校验 `entryType != "work"`
2. 查询 `startDate.year ~ endDate.year` 范围内工作日历，构建 `holidaySet`（`dayType='holiday'`）和 `forcedWorkdaySet`（`dayType='workday'`，即调班）
3. 遍历 `[startDate, endDate]` 每一天：
   - 跳过周末（除非在 `forcedWorkdaySet`）
   - 跳过 `holidaySet` 中的日期
   - 查询该日是否已有同 `entryType` 的假期 detail
   - 按 `conflictStrategy` 决定 skip 或继续
   - 读取该日现有所有 detail（包括 work 条目）
   - 构建新 `DailyReport`：details = 现有非冲突条目 + 新假期条目
   - 调用 `saveDailyReport(report)`
4. 返回 `{totalWorkdays, created, skipped, overwritten}`

> 关键点：`saveDailyReport` 是全量替换，调用前必须将现有 work 条目合并进去，否则会清除工时记录。

---

### Step 5 — DailyReportController 新增 endpoint

**文件**: `DailyReportController.java`，追加：

```java
/**
 * 批量填写假期
 */
@PreAuthorize("@ss.hasAnyPermi('project:dailyReport:add,project:dailyReport:edit')")
@Log(title = "日报管理", businessType = BusinessType.INSERT)
@PostMapping("/batchLeave")
public AjaxResult batchLeave(@Validated @RequestBody BatchLeaveRequest request) {
    return success(dailyReportService.batchSaveLeave(request));
}
```

---

### Step 6 — 前端 API 新增函数

**文件**: `ruoyi-ui/src/api/project/dailyReport.js`，追加：

```javascript
// 批量填写假期（按日期范围）
export function batchSaveLeave(data) {
  return request({
    url: '/project/dailyReport/batchLeave',
    method: 'post',
    data,
    headers: { repeatSubmit: false }
  })
}
```

---

### Step 7 — write.vue 颜色动态化

**文件**: `ruoyi-ui/src/views/project/dailyReport/write.vue`

**改动 1**：删除 `LEAVE_TYPE_COLOR` 常量，替换为 computed：

```javascript
// 删除：
// const LEAVE_TYPE_COLOR = { leave: '#f56c6c', comp: '#b37feb', annual: '#36cfc9' }

// 新增：
const ELEMENT_PLUS_COLORS = {
  primary: '#409eff', success: '#67c23a',
  warning: '#e6a23c', danger: '#f56c6c', info: '#909399'
}
const leaveColorMap = computed(() => {
  return Object.fromEntries(
    (sys_rbtype.value || [])
      .filter(d => d.value !== 'work')
      .map(d => [d.value, ELEMENT_PLUS_COLORS[d.elTagType] || '#909399'])
  )
})
```

**改动 2**：全局替换 `LEAVE_TYPE_COLOR` → `leaveColorMap.value`（2 处，模板中用 `leaveColorMap`）

---

### Step 8 — write.vue 批量填假期弹窗

**文件**: `ruoyi-ui/src/views/project/dailyReport/write.vue`

**改动 1**：在「+ 添加假期」旁边增加「批量填假期」按钮：

```html
<el-button type="warning" size="small" plain @click="batchLeaveVisible = true">
  批量填假期
</el-button>
```

**改动 2**：新增 `el-dialog`（内联在 write.vue 底部）：

```html
<el-dialog v-model="batchLeaveVisible" title="批量填假期" width="460px">
  <el-form label-width="90px">
    <el-form-item label="假期类型">
      <el-select v-model="batchLeaveForm.entryType" style="width: 160px;">
        <el-option
          v-for="d in sys_rbtype.filter(d => d.value !== 'work')"
          :key="d.value" :label="d.label" :value="d.value"
        />
      </el-select>
    </el-form-item>
    <el-form-item label="日期范围">
      <el-date-picker
        v-model="batchLeaveForm.dateRange"
        type="daterange" value-format="YYYY-MM-DD"
        range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期"
        style="width: 280px;"
      />
    </el-form-item>
    <el-form-item label="每日时长">
      <el-input-number
        v-model="batchLeaveForm.leaveHoursPerDay"
        :min="0.5" :max="24" :step="0.5" :precision="1"
        style="width: 120px;"
      />
      <span style="margin-left: 8px; color: #909399;">小时/天</span>
    </el-form-item>
    <el-form-item label="冲突处理">
      <el-radio-group v-model="batchLeaveForm.conflictStrategy">
        <el-radio value="skip">跳过已有记录</el-radio>
        <el-radio value="overwrite">覆盖已有记录</el-radio>
      </el-radio-group>
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="batchLeaveVisible = false">取消</el-button>
    <el-button type="primary" :loading="batchLeaving" @click="handleBatchLeave">确认填写</el-button>
  </template>
</el-dialog>
```

**改动 3**：新增响应式变量和提交方法：

```javascript
import { batchSaveLeave } from '@/api/project/dailyReport'

const batchLeaveVisible = ref(false)
const batchLeaving = ref(false)
const batchLeaveForm = ref({
  entryType: 'leave',
  dateRange: [],
  leaveHoursPerDay: 8,
  conflictStrategy: 'skip'
})

async function handleBatchLeave() {
  const [startDate, endDate] = batchLeaveForm.value.dateRange || []
  if (!startDate || !endDate) {
    ElMessage.warning('请选择日期范围')
    return
  }
  batchLeaving.value = true
  try {
    const res = await batchSaveLeave({
      entryType: batchLeaveForm.value.entryType,
      startDate, endDate,
      leaveHoursPerDay: batchLeaveForm.value.leaveHoursPerDay,
      conflictStrategy: batchLeaveForm.value.conflictStrategy
    })
    const { created, skipped, totalWorkdays } = res.data
    ElMessage.success(`批量填写完成：共 ${totalWorkdays} 个工作日，新建 ${created} 条，跳过 ${skipped} 条`)
    batchLeaveVisible.value = false
    loadMonthOverview()  // 刷新日历角标
  } finally {
    batchLeaving.value = false
  }
}
```

---

## Complexity Tracking

无 Constitution 违规，无需填写。
