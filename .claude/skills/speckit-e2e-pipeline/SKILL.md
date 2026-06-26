---
name: speckit-e2e-pipeline
description: newpm 的 Spec Kit 全流程 + 本地 e2e 验证流水线。把一句话需求端到端落地：specify → plan → tasks → implement → 本地 Playwright e2e（含造数/关验证码/起前端/截图/还原）。沉淀自 spec 013「日报填写右侧查询条件」已跑通的全过程，含全部踩过的坑。当用户说「按 speckit 流水线做这个需求」「specify 到 plan tasks implement 然后本地 e2e」「做个功能并跑 e2e 给我看」时使用。
argument-hint: "<一句话需求描述>"
---

# Spec Kit → 本地 e2e 流水线（newpm）

你是 newpm 的特性交付助手。用户给一句话需求，你按本流程**端到端**交付：
**规格 → 计划 → 任务 → 实现 → 本地 e2e 跑通 → 截图给用户看 → 还原测试环境**。
本流程沉淀自 **spec 013「日报填写右侧查询条件」**已跑通的全过程，下面每个坑都真实踩过。

## ⭐ 第一原则（先读这三条）

1. **`.specify/feature.json` 是 spec-kit 解析特性目录的真源**，优先级高于 git 分支名。`create-new-feature.sh` 建分支后**不会自动更新它**——若它还指向上一个特性，`setup-plan.sh` / `check-prerequisites.sh` 会把 plan/tasks 全写到**错的旧目录**。**每次 specify 建完分支，第一件事就是把 `.specify/feature.json` 的 `feature_directory` 改成新特性目录。**
2. **本地后端端口是 8085，不是 8080**；前端 vite 配置在 80 端口（绑 80 需 root）。跑 e2e 别傻等 80。
3. **RuoYi 路由是菜单驱动**：页面真实路由 = 父菜单 `path` 串接子菜单 `path`，**不是** component 路径。猜组件路径当路由 → 404。先查 `sys_menu`。

## 五个阶段（按序）

| 阶段 | 命令 | 产物 / 要点 |
|---|---|---|
| 1 规格 | `Skill speckit.specify` | `specs/NNN-x/spec.md` + `checklists/requirements.md`。建分支后**立刻改 `.specify/feature.json`**（见坑①） |
| 2 计划 | `Skill speckit.plan` | `plan.md` + research/data-model/contracts/quickstart。先跑 `setup-plan.sh --json` 确认解析到**新**目录 |
| 3 任务 | `Skill speckit.tasks` | `tasks.md`，按用户故事 P1/P2/P3 分组。**含明确的 e2e 测试任务** |
| 4 实现 | `Skill speckit.implement` | 改代码 + 勾 `tasks.md`。清单全 PASS 才进 |
| 5 e2e | 见下「本地 e2e 操作手册」 | 起前端 + 造数 + 关验证码 + 跑 Playwright + 截图 + **还原** |

> **不要自动 git commit**：`.specify/extensions.yml` 里 before/after_* 全是 optional 的自动提交 hook。用户通常要先看结果，**跳过自动提交**，最后问用户是否提交。

## 本地 e2e 操作手册（核心价值，全是坑）

### A. 环境事实（记牢，省去反复查）
- 后端 **8085**（已在跑就别重启；只改前端不用重建 jar）。
- 本地 MySQL 容器 **`newpm-mysql-1`**（root/password，库 `ry-vue`）。中文 SQL 走文件管道：
  `cat x.sql | docker exec -i newpm-mysql-1 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue`
- 后端连的 Redis 是映射了 6379 的容器（实测是 **`fmp-redis`**，非 `newpm-redis-1`；用 `docker ps | grep 6379` 确认）。
- 账号 **admin / 123456789**。

### B. 三道前置拦路虎（跑前必查，否则页面空白/登录失败）
1. **验证码**：`sys.account.captchaEnabled` 默认 `true`。关：改 DB=`false` **且**清后端 Redis 缓存键 `sys_config:sys.account.captchaEnabled`（否则仍用旧缓存 true）。
2. **目标用户是否有可见数据**：很多页面只显示「我参与/我负责」的数据。先用 API 验证真实返回，没数据就**最小侵入播种**（如给 admin 插 `pm_project_member` 行），并打 `remark='e2e-seed-NNN'` 便于精确清理。
3. **页面级开关**：如日报填写页，admin 可能在「免填白名单」(`pm_daily_report_whitelist`) 里 → 右侧整块不渲染。临时把对应行 `del_flag='1'`。

用 API 自检（关掉验证码后）：
```bash
TOKEN=$(curl -s -X POST http://localhost:8085/login -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456789"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")
curl -s http://localhost:8085/<接口路径> -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

### C. 起前端（免 sudo 跑 e2e）
vite 配置死绑 80（需 root）。**不要碰 80**：
1. 让 `playwright.config.js` 的 baseURL 支持环境变量覆盖（一次性、向后兼容）：
   `baseURL: process.env.E2E_BASE_URL || 'http://localhost:80'`
2. vite 起在非特权端口，proxy 照样转 8085：
   `cd ruoyi-ui && nohup npm run dev -- --port 8090 --host > /tmp/vite-e2e.log 2>&1 &`
3. 跑测试带上 `E2E_BASE_URL=http://localhost:8090`。

### D. 写 e2e（参考 `tests/project-management.spec.js`）
- UI 登录：填 `input[placeholder="账号"]` / `密码`，点 `button.el-button--primary`，等 `!location.pathname.startsWith('/login')`，关可能的改密弹窗。
- **导航用相对路径**（走 baseURL，端口无关）：`page.goto('/<真实菜单路由>')`。真实路由查 `sys_menu`（见坑③）：
  ```sql
  SELECT m.path child, p.path parent, p.parent_id FROM sys_menu m
  LEFT JOIN sys_menu p ON m.parent_id=p.menu_id WHERE m.component LIKE '%<组件路径>%';
  ```
  路由 = `/` + parent.path + `/` + child.path（父 parent_id=0 时）。
- 断言尽量**数据无关 + 结构稳定**：用 class/placeholder 选择器（`.el-select-dropdown__item`、`input[placeholder="x"]`），断行数 `toHaveCount`、空状态可见等；造的数据用唯一片段精确命中。
- 跑：`E2E_BASE_URL=http://localhost:8090 npx playwright test <spec>.spec.js --reporter=list`（config: chromium / workers=1 / retries=2）。

### E. 截图给用户看
跑通后用一次性 Playwright 脚本截 2–4 张关键态（基线 / 各过滤态）。脚本里 `import { chromium } from '@playwright/test'` 必须在**项目根目录**执行（node 模块解析），存到 scratchpad 后用 Read 工具贴给用户。

### F. 还原（必做，别把环境留脏）
把 B 阶段所有临时改动还原：验证码 DB→`true` 且清 Redis 缓存键；白名单行 `del_flag` 还原；`DELETE ... WHERE remark='e2e-seed-NNN'`；`kill` 掉临时 vite。逐项 SELECT 确认还原到位。

## 关键坑速查表

| 现象 | 根因 | 解法 |
|---|---|---|
| plan/tasks 写进了**旧** spec 目录 | `.specify/feature.json` 还指旧特性 | specify 后立刻改它为新目录 |
| e2e 全挂在找元素，截图是 **404 页** | 用 component 路径当路由 | 查 `sys_menu` 取真实菜单路由 |
| 页面右侧/列表**空白** | 验证码没关 / 无参与数据 / 在白名单 | 关验证码+清缓存 / 播种 / 临时移出白名单 |
| 关了验证码登录仍要验证码 | 只改 DB 没清 Redis 配置缓存 | `DEL sys_config:sys.account.captchaEnabled` |
| 前端起不来（EACCES 80） | vite 绑 80 需 root | `--port 8090` + `E2E_BASE_URL` 覆盖 |
| 截图脚本 `ERR_MODULE_NOT_FOUND` | 在 /tmp 跑找不到 node_modules | 在项目根目录执行脚本 |

## 红旗（出现就停下自查）
- 「plan.md 怎么跑到 012 去了」→ 先看 `.specify/feature.json`。
- 「e2e 一个都过不了」→ 先看失败截图是不是 404 / 空白，别急着调选择器。
- 「测完忘了还原」→ 收尾前对照 F 阶段逐项还原并 SELECT 确认。
- 「要不要顺手 commit」→ 不要，先把结果给用户看，再问。

## 完成标准
specify→implement 全产物齐 + `tasks.md` 全勾 + e2e **全绿** + 截图已给用户 + 测试环境**已还原** + 改动**未提交**（等用户确认）。
