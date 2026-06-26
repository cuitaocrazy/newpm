# Research: 日报填写右侧查询条件

**Feature**: 013-daily-report-write-filter | **Date**: 2026-06-26

本特性技术上下文中无 NEEDS CLARIFICATION 项；以下记录关键设计取舍。

## Decision 1：纯前端过滤 vs. 后端查询

- **Decision**: 纯前端过滤已加载数据，不新增/修改后端接口。
- **Rationale**: 右侧编辑区数据在页面加载时已一次性取得——`/project/dailyReport/myProjects`（返回 `projectName`、`projectManagerName`、`hasSubProject` 等）与每个含子任务项目的 `/project/task/options`（返回 `taskName`、`taskManagerName`）。用户需求是"在已铺开的长列表里快速定位"，本质是视图收窄，内存过滤即可满足，零网络往返、实时响应。
- **Alternatives considered**: 给 `myProjects` 加查询参数走后端过滤——被否决：会引入接口改动、权限/分页复杂度，且任务名过滤需 join，收益不及前端方案；用户列表规模（项目 5–20+、任务数十级）远未到需要后端分页的体量。

## Decision 2：过滤语义 = 显示/隐藏，绝不删数据

- **Decision**: 过滤通过 `computed` 派生可见列表（`filteredFormList`）+ 模板层任务行 `filter`，**不修改** `formList` / `taskRows` 原始结构。
- **Rationale**: `handleSave` 遍历完整 `formList` 收集工时。若过滤直接增删 `formList`，被隐藏项目/任务上已录入的工时会丢失（违反 FR-007 / SC-003）。派生只读视图保证保存逻辑看到的永远是全量数据。
- **Alternatives considered**: 用 `v-show` 隐藏 DOM——可行但模板更繁琐；用 `computed` 派生列表更清晰且与既有 Vue3 `<script setup>` 风格一致。任务行选择"模板内 `taskRows.filter(t => 名称匹配)`"而非派生新数组，避免再引一层状态。

## Decision 3：项目经理下拉的数据来源

- **Decision**: 下拉选项 = 当前用户参与项目去重后的 `projectManagerName` 集合（`computed`），不查字典、不查 `system/user`。
- **Rationale**: 用户只关心"我参与的项目里的经理"。从已加载数据去重最准确（SC-004：100% 来自参与项目集合），也规避了 PM 角色无 `system/user` 权限的 403 风险（宪法 II）。项目经理是人名而非字典值，无需 `dict-select`。
- **Alternatives considered**: 用 `UserSelect post-code="pm"` 组件——会拉全量 pm 岗位用户，含与当前用户无关的人，违反 SC-004，否决。

## Decision 4：模糊匹配规则

- **Decision**: 大小写不敏感子串匹配（`String.prototype.includes` on `toLowerCase()`）。
- **Rationale**: 项目/任务名多为中文（大小写无差异），但常含英文/编号片段（如 "ZH-BJ"），大小写不敏感更友好。子串匹配符合"模糊查询"直觉。
- **Alternatives considered**: 拼音首字母/正则——超出需求范围，不引入。

## Decision 5：查询条件的生命周期

- **Decision**: 三个查询条件为会话内 reactive ref，切换日期时保留，页面刷新后重置（不持久化到 sessionStorage / 后端）。
- **Rationale**: FR-010 要求切换日期时保留（reactive ref 天然满足，`selectedDate` 的 `watch` 只重载 `formList` 数据，不动条件）。需求未要求跨刷新持久化（Assumptions 已记录），保持简单。
- **Alternatives considered**: 复用 CLAUDE.md 的"搜索状态缓存"sessionStorage 模式——对本页非必要（不存在跳详情页再返回的场景），不引入额外复杂度。

## E2E 测试策略

- **现状**: 日报相关仅有 `tests/e2e-daily-report.spec.js`（纯 API），无 write 页面 UI 测试。
- **范式参考**: `tests/project-management.spec.js` 的 UI 测试模式——`login(page)` 走表单登录（`input[placeholder="账号"]` / `密码`），`[data-prop]` 或 Element Plus class 选择器定位，断言可见性/数量。
- **前置**: `playwright.config.js` baseURL `http://localhost:80`，需前端(80)+后端(本地 8085)同时运行；跑前临时关闭登录验证码（见用户记忆 `feedback_e2e_captcha_toggle`），跑完恢复。
- **路由**: `/project/dailyReport/write`。
