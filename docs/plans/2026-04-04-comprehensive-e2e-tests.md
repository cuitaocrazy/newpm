# 全项目 E2E 测试覆盖 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为全部 18 个 Controller 的核心端点生成 API 级 E2E 测试，覆盖 CRUD + 业务工作流 + 负面用例

**Architecture:** 提取公共 API Helper 模块，按业务域拆分测试文件，每个文件独立可运行。所有测试使用 Playwright APIRequestContext（纯 HTTP，不启动浏览器），`beforeAll` 登录拿 token。

**Tech Stack:** Playwright Test, API-based (no browser), JavaScript/ESM

---

## 测试文件清单

| 文件 | 覆盖模块 | 测试数（预估） |
|------|----------|--------------|
| `tests/helpers/api-client.js` | 公共 helper | — |
| `tests/e2e-contract-crud.spec.js` | 合同管理 | 8 |
| `tests/e2e-customer-crud.spec.js` | 客户管理 | 7 |
| `tests/e2e-task-crud.spec.js` | 任务管理 | 9 |
| `tests/e2e-payment-crud.spec.js` | 款项管理 | 7 |
| `tests/e2e-daily-report.spec.js` | 日报 + 统计 | 8 |
| `tests/e2e-approval-workflow.spec.js` | 立项审批 | 6 |
| `tests/e2e-manager-stage-change.spec.js` | 项目经理变更 + 阶段变更 | 8 |
| `tests/e2e-team-revenue.spec.js` | 团队收入确认 | 6 |
| `tests/e2e-auxiliary-modules.spec.js` | 投产批次 + 二级区域 + 工作日历 + 白名单 | 10 |
| `tests/e2e-cross-module-workflow.spec.js` | 跨模块集成 | 4 |

## 公共 Helper 设计

```javascript
// tests/helpers/api-client.js
const BASE = 'http://localhost:80';

export async function login(request, user = 'admin', pass = '123456789') { ... }
export function api(request, token) {
  return {
    get: (path, params) => ...,
    post: (path, data) => ...,
    put: (path, data) => ...,
    del: (path) => ...,
  }
}
```

## 每个测试文件结构

1. **List 查询** — 验证分页、total、rows
2. **Create** — 新增记录，验证 code=200
3. **Get by ID** — 读取刚创建的记录
4. **Update** — 修改记录
5. **业务特有操作** — 搜索、唯一性检查、关联操作等
6. **Delete** — 删除测试数据（清理）
7. **负面用例** — 缺字段、重复数据、引用保护

---

## Task 1: 公共 Helper

**Files:** Create `tests/helpers/api-client.js`

## Task 2-11: 各模块测试文件（可并行）

每个文件遵循上述结构，具体端点见 Controller 清单。

## Task 12: 跨模块集成

依赖 Task 2-11 的模式，串联完整业务流程。
