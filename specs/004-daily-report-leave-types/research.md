# Research: 日报假期模块扩展

**Feature**: 004-daily-report-leave-types
**Date**: 2026-03-20

## 1. 现有保存 API 结构

**Decision**: 新增 `POST /project/dailyReport/batchLeave` 独立接口，不复用现有 `POST /project/dailyReport`

**Rationale**: 现有 `saveDailyReport` 是**单日全量替换**逻辑——先删除该日所有 detail 行，再插入新行。批量填假期若直接循环调用，会清除每日已填的工时记录。正确做法是在新接口中，按日期读取已有 details，**合并**现有工时行与新假期行后再调用 save，或直接操作 mapper 层插入假期 detail 而不触发全量替换。

**Alternatives considered**:
- 前端循环调用单日 save：会清除工时，不可行
- 前端传 `dateList[]` 到现有接口：破坏现有接口语义

## 2. WorkCalendar 跳过节假日

**Decision**: 批量填写逻辑在后端进行节假日判断，查询 `pm_work_calendar` 中 `dayType='holiday'` 的日期

**Rationale**: 工作日历（`pm_work_calendar`）已有标准 `dayType` 字段（`holiday`=节假日, `workday`=调班工作日）。批量填写时后端查询覆盖范围内的工作日历，跳过 `dayType='holiday'` 的日期，同时跳过自然周末（`DayOfWeek.SATURDAY/SUNDAY`），但若工作日历标记某周末为 `workday`（调班）则不跳过。

**Alternatives considered**:
- 纯前端计算：前端已有 `workCalendarMap`，但放到后端更可靠且避免前后端不一致

## 3. 颜色动态化方案

**Decision**: 用 `sys_rbtype.list_class`（Element Plus tag type name）映射到固定色值 Map，替换硬编码的 `LEAVE_TYPE_COLOR`

**Rationale**:
- 字典数据经 `useDict()` → `dict.ts` 转换后，每个字典项有 `elTagType`（来自 `list_class`）
- Element Plus 标准 tag 色：`primary=#409eff`, `success=#67c23a`, `warning=#e6a23c`, `danger=#f56c6c`, `info=#909399`
- 日历角标和颜色圆点需要 hex 色值（不能直接用 tag type），通过 `ELEMENT_COLORS[elTagType]` 桥接即可
- 新增字典类型只需在字典中配置 `list_class`，前端零改动

**Alternatives considered**:
- 用 `css_class` 字段存 hex 颜色：更灵活但引入字典维护负担
- 前端 DictTag 组件完全替代手动色值：日历角标是纯文字/color样式，无法直接用 DictTag

## 4. 冲突处理策略

**Decision**: 支持 `skip`（默认）和 `overwrite` 两种策略，以同日同类型假期是否存在为判断标准

**Rationale**: 同一天可能已有「请假」记录，批量填写「婚假」时不应互相干扰。只有同类型（相同 `entryType`）才视为冲突，不同类型假期可并存（如同天有请假和倒休各 4h）。

**Alternatives considered**:
- 以"该日是否有任何假期"为冲突判断：太严格，不同类型假期理应可并存

## 5. 批量填写的「本周限制」豁免

**Decision**: `batchLeave` 接口不做周范围校验，允许历史和未来日期

**Rationale**: 产假等长假需要事后/提前补录，严格限制本周会使功能失去意义。现有 `isEditable` 限制仅存在于前端，后端 `saveDailyReport` 本身无周范围校验。新接口同样不加此限制。

## 6. 新接口响应设计

**Decision**: 返回操作摘要 `{ created: N, skipped: N, overwritten: N, totalWorkdays: N }`

**Rationale**: 让用户了解批量操作的实际效果，特别是有多少天被跳过（周末/节假日/冲突），提升操作透明度。
