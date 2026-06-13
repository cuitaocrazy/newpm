# yadapm 迁移踩坑记录

> 开发各功能时遇到的坑，按功能/类别记录，供后续功能避雷。新坑随手追加，并同步进 `.claude/skills/yadapm-migrate/SKILL.md` 的避坑清单。

---

## 一、批次版本管理（spec 007）踩的坑

| # | 坑 | 现象 | 根因 | 解法 |
|---|---|---|---|---|
| 1 | 只读后端漏前端规则 | 需求实现后用户实操发现多处不对 | 只读了 Controller/Service/Model，没读 HTML 模板的 JS | **必读 create.html/edit.html 的 `<script>`**：联动、去重、readonly、校验都在那里 |
| 2 | 凭空多出"子产品"字段 | 表单多了个没用的子产品下拉 | 误把"产品"(name=subVersionCode)拆成产品+子产品两个 | 旧系统只有一个"产品"字段（存 subVersionCode），删掉子产品 |
| 3 | 投产日期不带出 | 选批次后版本投产日期空白 | 后端 `success(String)` 把日期塞进了 msg 不是 data，前端 res.data 为空 | 返回数据用 `success(对象)`；或前端直接从已加载的批次选项里取 planProductionDate |
| 4 | 提交人员可改 | 实现成可选下拉 | 没核实旧系统 | 旧系统是 `readonly`：新增=当前用户、编辑=记录原始提交人 |
| 5 | 任务号可重复选 | 多任务行能选同一个任务 | 没实现去重 | 旧系统 `taskNoFun()` 注释"禁止重复选中"：已选任务在其它行 disabled |
| 6 | 升级包字段单独占行 | 类型5/6 时升级包初级版本号单独一行、留空 | v-if 字段没做自适应布局 | 用 `:span="isUpgrade?6:8"` 自适应列宽，融入整行 |
| 7 | 详情/列表字段缺失 | 比旧系统少一堆字段 | 没对齐 show.html/list.html | 补全：审计4字段、多任务号 GROUP_CONCAT 聚合列、TWS/数据库/接口等 |
| 8 | JOIN 字符集报错 | 创建/修改人 JOIN sys_user 报错 | PM表 utf8mb4_0900 vs 系统表 utf8mb4_unicode_ci | JOIN 加 `COLLATE utf8mb4_unicode_ci` |
| 9 | 字段"一列两义" | TASK_NAME 列含义混 | 旧 T_B_VERSION_OUT.TASK_NAME：批次=版本简介、非批次=任务名称 | 按 manual_input 分别映射；newpm 拆成 version_brief / manual_task_name |
| 10 | 测试欠债 | E2E 写了从没跑、还过时 | 只手工 Playwright 点验，没真跑自动化 | 强制：单测逻辑≥90% + E2E真跑 + JaCoCo |
| 11 | E2E 跑不起来 | 登录超时 | ①80端口被别的项目占；②MySQL/Redis 容器闲置几小时会挂(Exited 255) | ①`E2E_BASE_URL=http://localhost:8090`；②先查容器健康，挂了 `docker start` 再重启后端 |

## 二、非批次版本管理（spec 008）踩的坑

| # | 坑 | 现象 | 根因 | 解法 |
|---|---|---|---|---|
| 12 | **共用实体 @Validated 误伤** | 非批次新增报"组包方式不能为空"(500)，但非批次根本没这字段 | 复用了批次的 `VersionOut` 实体，实体上 `packageMode`/`versionStatus` 等带 `@NotBlank`；Controller 用 `@Validated` 会强制校验这些被裁剪掉的批次专用字段 | 复用实体但裁剪字段的新模块，Controller **去掉 `@Validated`**，改在 **service 层手动校验**该模块真正必填的字段 |
| 13 | **软删除记录占用唯一键**（批次就埋下，E2E才暴露） | ①删一条后新增同号报"生成冲突"；②若把 del_flag 纳入唯一键，则软删两条同号记录又互相撞键 | 唯一键 `uk(sys_name,version_type,out_lib_version)`，软删(del_flag='1')记录仍占号 | **软删时改写版本号腾位**：`update set del_flag='1', out_lib_version=concat(out_lib_version,'_DEL_',id)`。id 唯一保证改写后也唯一；原号腾空可重建；软删记录间也不撞。唯一键保持不含 del_flag |

> **坑13 升华**：凡"软删除 + 业务唯一键"组合都有此隐患。**试过两个方案**：
> - ❌ 唯一键纳入 del_flag → 治了"删后重建"，却导致"两条软删同号"撞键（del_flag 都=1 时仍相同）。
> - ✅ **软删时改写唯一字段加 `_DEL_{id}` 后缀腾位** → 原号腾空、软删记录间靠 id 唯一不撞。这是软删+唯一键的稳妥解法。
> **坑13 教训**：缺陷批次就有，但当时没"删→重建同号"所以没暴露；非批次 E2E 跑（数据含软删记录）才炸出来。**印证 E2E 真跑的价值**——手工点验碰不到这种数据组合，连续 CRUD 才暴露。

> **教训升华**：多个功能共用一个 domain 实体时，实体上的 Bean Validation 注解（@NotBlank/@NotNull）是"最全字段集"的约束。被某个模块裁剪掉的字段，其校验注解会在该模块 `@Validated` 时误伤。**对策**：共用实体的不同模块，校验要么各自 service 层手动做，要么用 validation group 分组，不能无脑 `@Validated`。
