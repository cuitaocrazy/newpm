# Contracts: 移动端消费的既有接口（全部复用，零后端改动）

**Date**: 2026-07-04
**Feature**: 014-daily-report-mobile

> 本特性不新增/不修改任何后端接口。下表是移动端消费的既有接口契约快照——若后端未来变更这些接口，移动端与桌面端同时受影响（同一契约）。
> 响应包络遵循 RuoYi 约定：单对象 `{ code, msg, data }`，列表 `{ code, msg, total, rows }`；`code=200` 成功、`401` 重登、`500` 业务错误。

## 认证链路

### GET /captchaImage（@Anonymous）
- **用途**: 移动登录页验证码
- **响应 data**: `{ captchaEnabled: boolean, uuid: string, img: string /* base64 */ }`
- **移动端行为**: `captchaEnabled=false` 时隐藏验证码输入行

### POST /login（@Anonymous）
- **Body**: `{ username, password, code, uuid }`
- **响应**: `{ code, msg, token }`
- **移动端行为**: 经 `useUserStore().login()` 调用（token 写 Cookie，桌面/移动共享登录态）

### GET /getInfo
- **响应 data**: `{ user, roles, permissions }`
- **移动端行为**: 由现有 permission.ts 守卫自动触发，移动端不直接调用

## 日报业务

### GET /project/whitelist/checkSelf
- **用途**: 判定当前用户是否在日报豁免白名单（`pm_daily_report_whitelist`）
- **响应**: `{ data: boolean }`
- **移动端行为**: 进入填写页**最先**调用（对齐桌面 write.vue onMounted）；`data===true` → 整页豁免提示并短路后续加载；请求异常按 `false` 兜底（`.catch(() => ({ data: false }))`）

### GET /project/dailyReport/myProjects
- **权限**: 登录即可（当前用户维度）
- **响应**: `{ data: MobileProjectItem[] }`（字段见 data-model.md §2.1；含 `hasSubProject` 标记）
- **移动端行为**: 白名单检查通过（false）后加载；项目卡片数据源

### GET /project/task/options?projectId={id}
- **响应**: `{ data: [{ subProjectId, taskName, ... }] }`
- **移动端行为**: 含子任务项目卡片**展开时懒加载**任务行（注意：此接口隐藏已结项任务 stage=11，与桌面行为一致——见 memory task_options_vs_list）

### GET /project/dailyReport/my/{reportDate}
- **路径参数**: `reportDate` = `YYYY-MM-DD`
- **响应 data**: `{ reportId, reportDate, detailList: [...] } | null`
- **移动端行为**: 选中日期变化时加载回显；保存成功后重新调用刷新 `reportId`

### POST /project/dailyReport
- **Body**: `{ reportDate, detailList }`（结构见 data-model.md §3，为**唯一保存契约**）
- **行为**: 同日覆盖更新（upsert）；后端级联重算任务/父项目 `actual_workload`；带 `@Log` 审计
- **移动端行为**: 保存按钮唯一出口；错误经 request.ts 统一 ElMessage 提示（SPA 内 Element Plus 样式可用）

## 字典

### GET /system/dict/data/type/{dictType}（经 useDict 封装，带 store 缓存）
- **消费**: `sys_rbtype`（条目类型，过滤 work 后作假期类型 Picker）、`sys_gzlb`（工作任务类别多选）
- **移动端行为**: `useDict('sys_rbtype', 'sys_gzlb')`——与桌面同一封装同一缓存，**不硬编码选项**（Constitution VI 底线）

## 契约测试锚点（供 tasks 阶段引用）

| 契约 | E2E 断言 |
|------|----------|
| 保存 payload 结构 | 移动端保存后经 API `GET my/{date}` 断言 detailList 字段与 data-model §3 一致 |
| 双端一致 | 移动保存 → 桌面 write.vue 打开同日回显一致（工时/内容/类别/假期） |
| 401 重定向 | 清除 token 访问 `/m/daily-report/write` → 落在 `/m/login?redirect=...` |
