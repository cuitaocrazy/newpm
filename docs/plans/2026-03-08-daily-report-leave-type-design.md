# 日报假期类型设计文档

## 需求概述

在日报填写中支持四种条目类型：**项目进行中 / 请假 / 倒休 / 年假**。同一天可同时存在项目工时和假期记录（如上午请假2小时，下午干活6小时）。左侧日历和日报动态页按类型用不同颜色区分展示。

---

## 数据库变更

### 1. `pm_daily_report_detail` 表新增字段

```sql
ALTER TABLE pm_daily_report_detail
  MODIFY COLUMN `project_id` bigint DEFAULT NULL COMMENT '项目ID，entry_type=work时必填',
  ADD COLUMN `entry_type` varchar(20) NOT NULL DEFAULT 'work'
      COMMENT '条目类型: work=项目工时 / leave=请假 / comp=倒休 / annual=年假',
  ADD COLUMN `leave_hours` decimal(5,2) DEFAULT NULL
      COMMENT '假期时长(小时)，entry_type非work时使用，work行忽略';
```

**设计说明：**
- 假期条目复用明细表，不新增表
- `project_id` 改为允许 NULL（假期行不关联项目）
- `work_content` 假期行可为空（原表约束 NOT NULL 需同步改为 DEFAULT ''）

### 2. `pm_daily_report` 主表 `total_work_hours` 语义

`total_work_hours` **只统计 `entry_type = 'work'` 的行**，不含假期小时。月览接口额外返回假期条目信息。

### 3. 字典数据

```sql
-- 字典类型
INSERT INTO sys_dict_type (dict_name, dict_type, status, remark, create_by, create_time)
VALUES ('日报条目类型', 'sys_rbtype', '0', '日报明细行的类型', 'admin', NOW());

-- 字典值
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, status, create_by, create_time)
VALUES
  (1, '项目工时', 'work',   'sys_rbtype', '0', 'admin', NOW()),
  (2, '请假',     'leave',  'sys_rbtype', '0', 'admin', NOW()),
  (3, '倒休',     'comp',   'sys_rbtype', '0', 'admin', NOW()),
  (4, '年假',     'annual', 'sys_rbtype', '0', 'admin', NOW());
```

---

## 后端改动

### 涉及文件（ruoyi-project 模块）

| 文件 | 改动 |
|------|------|
| `DailyReportDetail.java` | 新增 `entryType`、`leaveHours` 字段 |
| `DailyReportDetailMapper.xml` | INSERT/SELECT 带上新字段；`total_work_hours` 汇总只计 `work` 行 |
| `DailyReportService.saveDailyReport` | 不再校验 `projectId` 非空（假期行允许为 null）；`work_content` 假期行默认空字符串 |
| `DailyReportController.listDailyReport` | 月览接口响应加 `leaveEntries: [{entryType, leaveHours}]` |
| `DailyReportController.getMyReport` | detailList 原样返回，包含假期行 |
| `DailyReportController.getMonthlyReports` | detailList 包含假期行（activity.vue 用） |

### `total_work_hours` 汇总 SQL

```xml
<!-- 只统计 work 行 -->
UPDATE pm_daily_report
SET total_work_hours = (
  SELECT COALESCE(SUM(work_hours), 0)
  FROM pm_daily_report_detail
  WHERE report_id = #{reportId} AND entry_type = 'work' AND del_flag = '0'
)
WHERE report_id = #{reportId}
```

### 月览接口响应扩展

```json
{
  "reportDate": "2026-03-08",
  "totalWorkHours": 6,
  "leaveEntries": [
    { "entryType": "leave", "leaveHours": 2 }
  ]
}
```

---

## 前端改动

### 颜色规范（前端硬编码）

| 类型 | 标签 | 颜色 | 色值 |
|------|------|------|------|
| `work` 项目进行中 | 🟢/🟠 | 绿(≥8h) / 橙(<8h) | `#00b42a` / `#ff7d00`（沿用） |
| `leave` 请假 | 🔴 | 玫红 | `#f56c6c` |
| `comp` 倒休 | 🟣 | 紫 | `#b37feb` |
| `annual` 年假 | 🩵 | 青 | `#36cfc9` |

```js
// 前端常量
const LEAVE_TYPE_COLOR = {
  leave:  '#f56c6c',
  comp:   '#b37feb',
  annual: '#36cfc9'
}
const LEAVE_TYPE_LABEL = {
  leave:  '请假',
  comp:   '倒休',
  annual: '年假'
}
```

---

### write.vue 改动

#### 右侧编辑区 — 新增"假期记录"区块

在项目列表下方追加：

```
┌─ 假期记录 ─────────────────────────────── [+ 添加假期] ─┐
│  [请假 ▼]  [2.0 ▲▼] 小时  [备注(选填)...]  [× 删除]    │
│  [倒休 ▼]  [1.0 ▲▼] 小时  [备注(选填)...]  [× 删除]    │
└─────────────────────────────────────────────────────────┘
```

- 类型：`dict-select`（`sys_rbtype`，过滤掉 `work`）
- 小时：`el-input-number`，step=0.5，min=0，max=24
- 备注：`el-input`，placeholder="备注(选填)"
- 保存：假期行和项目行合并到 `detailList` 提交，假期行 `projectId=null`，`workHours=leaveHours`，`workContent=''`

**保存校验：**
- 项目行：`workHours > 0 && workContent 非空`（不变）
- 假期行：`entryType 非空 && leaveHours > 0`

**isEditable 限制不变**，假期行同样只能填本周。

#### 左侧日历格子 — 角标展示

```
原：  ●15  6h
新：  ●15  6h  请2h倒1h
```

- `6h` 颜色逻辑不变（按 `totalWorkHours` 决定绿/橙）
- 假期角标：`leaveEntries` 有值时，每条用对应颜色小文字追加在后面
- `getDayBadgeClass` 逻辑**不变**

---

### activity.vue 改动

#### 个人模式格子 — 假期条目独立行

```
│ ▌ 项目A  需求评审           4h │  ← 蓝色竖条（现有 work 行）
│ 🔴 请假                    2h │  ← 新增假期行
│ 🟣 倒休                    1h │  ← 新增假期行
```

假期行渲染：`entryType !== 'work'` 的 detail 单独渲染，不走项目竖条逻辑。

#### 团队模式抽屉（drawer）

每人 detailList 中假期行追加在项目列表后面，样式同个人模式假期行。

#### 图例区新增

```
原：● >=8h  ● <8h  ● >8h
新：🟢 项目满勤(≥8h)  🟠 项目不满(<8h)  🔵 项目超时(>8h)  🔴 请假  🟣 倒休  🩵 年假
```

---

## 不涉及改动的部分

- `pm_daily_report` 主表结构不变
- `isEditable`（本周限制）逻辑不变
- `workCalendarMap` 工作日历逻辑不变
- stats.vue 人天统计页不变（`total_work_hours` 语义已限定为 work 行）
- 权限码不变

---

## 实现顺序建议

1. 执行 DDL + 字典 SQL
2. 后端：实体字段 → Mapper XML → Service 保存逻辑 → 月览接口扩展
3. 前端 write.vue：假期区块 UI → 保存逻辑 → 日历角标
4. 前端 activity.vue：假期行渲染 → 图例更新
