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

| 14 | **只读字段漏传 id（提交人员空白）** | 列表/详情"提交人员"列空白，但"创建人员"有值 | 前端提交人员只 `:value="userStore.nickName"` 只读展示，**没把 commName=userStore.id 放进 form** → 提交时 comm_name 为空 → JOIN sys_user 取不到 userName | add.vue `onMounted(() => form.value.commName = userStore.id)`。批次 add 当时绑了，非批次复制裁剪时漏了 |

> **坑14 升华**：只读显示的关联字段（人员/部门等"显示名+底层id"的字段），**展示用昵称、提交用id，两者别混**。务必确认 form 里有那个 id 字段并初始化。
> **坑14 教训**：**API/E2E 测不到**（测试 payload 直接传了 commName）；**只有浏览器真点保存 + 看详情/列表 JOIN 字段是否空** 才暴露。这就是为什么"浏览器 UI 验证必须真走完整流程、不能只截静态图"。

| 15 | **兜底值类型不一致（commName 兜底成登录名）** | code review 发现：commName 为空时兜底 `setCommName(getUsername())`(登录名)，但该字段存 userId、JOIN `sys_user.user_id` | 从批次复制的兜底逻辑，批次也有此隐患（前端都传 userId 所以兜底分支没触发，潜伏） | 兜底改 `String.valueOf(getUserId())`。**Code Review 才发现**，测试不触发(都传了值) |

> **坑15 升华**：兜底/默认值要和字段的**真实存储类型/口径一致**（存 id 就兜 id，别兜 name）。**这类潜伏隐患测试覆盖不到（兜底分支不触发），靠 Code Review 通审才能揪出**——印证"测试通过≠代码好，Code Review 必做"。

> **教训升华**：多个功能共用一个 domain 实体时，实体上的 Bean Validation 注解（@NotBlank/@NotNull）是"最全字段集"的约束。被某个模块裁剪掉的字段，其校验注解会在该模块 `@Validated` 时误伤。**对策**：共用实体的不同模块，校验要么各自 service 层手动做，要么用 validation group 分组，不能无脑 `@Validated`。

---

## 旧数据查询 (spec 009) 踩坑

| 16 | **迁移脚本菜单 INSERT 不幂等 / 父id 为空插孤儿菜单** | Code Review 发现：建表用 `CREATE TABLE IF NOT EXISTS`（幂等），但 `INSERT sys_menu` 无去重——生产重跑会插重复菜单；且 `@parent_id` 取不到（一级菜单还没建）时静默插出 `parent_id=NULL` 的孤儿菜单 | 纯只读功能本身零 bug，问题全在迁移脚本健壮性 | ① INSERT 前 `DELETE FROM sys_menu WHERE perms='xxx:list'` 去重（对齐 `02_menu_data.sql` 约定）；② `SET @parent_id=(...)` 后加一句校验 SELECT 让执行者肉眼确认非空；③ 顺手补 `route_name`（与既有 C 类菜单风格一致，keep-alive 缓存命名才对得上 `<script setup name>`） |

> **坑16 升华**：迁移脚本的 DDL 幂等（`IF NOT EXISTS`）容易，**DML（菜单/字典 INSERT）幂等容易被忽略**。凡 `INSERT sys_menu/sys_dict_*` 一律「先按唯一标识 DELETE 再 INSERT」。父 id 用子查询取时，**要防子查询命中 0 行返回 NULL** → 插出孤儿数据。
> **坑16 教训**：**功能再简单也要做 Code Review**——本功能纯只读、Controller/Service/Mapper 零 bug、零回归，但 Code Review 仍揪出 2 个迁移脚本健壮性问题（生产环境才会炸的那种）。测试覆盖功能正确性，Code Review 覆盖「运维/重跑/异常环境」这类测试照不到的角落。

---

## 批次任务问题单及缺陷 (spec 010) 踩坑

| 17 | **复用通用附件体系不止加权限白名单——`getBusinessFolder` 硬编码 switch 漏分支** | 浏览器上传附件报「业务数据不存在」 | `AttachmentServiceImpl.getBusinessFolder(businessType, businessId)` 是硬编码 if-else，只认 contract/project/payment，新业务类型返回 null → 校验失败 | 新业务类型复用 pm_attachment **三处都要改**：①各端点 `@PreAuthorize` 白名单加新权限；②`getBusinessFolder` 加 `else if("prolist")` 分支(反查业务记录、返回存储子目录)；③注入该业务的 Mapper。只做①不做②会上传失败 |
| 18 | **又一次 `success(String)` 塞 msg（批次→计划投产日期联动）** | 选批次后前端"计划投产日期"空白；网络请求 200 但 res.data 为 null | controller `return success(service.selectXxx())` 当返回值是 String 时命中 `success(String msg)` 重载，值进 msg 不进 data | 用 2 参重载 `AjaxResult.success("查询成功", value)` 强制进 data。**凡联动端点返回单个 String/日期，一律用 2 参重载**（坑「接口返回裸字符串」的再次复发，说明此坑极易踩，写联动端点先自检返回类型） |
| 19 | **新旧概念映射后，存储口径要让"过滤维度"与"被过滤数据"同源** | Code Review 发现：列表按"项目组"筛 `pd.dept_id`，但任务下拉按任务所属项目 `p.project_dept` 过滤——用户选父部门(ancestors命中子部门任务)时，存进主表的是"选中的父部门id"，与任务真实 `project_dept` 不一致 → "新增搜得到、列表按部门筛不到" | 老"项目组"映射到新"部门"时，主表 dept_id **不存用户在表单选的过滤部门，而是 insert/update 时按 taskId 反查任务的 project_dept 落库**，保证列表过滤(dept_id+ancestors)与任务下拉(project_dept)同源 |

> **坑17 升华**：「复用现成体系」要顺着调用链把所有"按类型分发"的地方都补全——权限白名单、业务校验 switch、存储路径 switch、（可能还有）日志/导出分发。漏一处就在某个操作上炸。
> **坑18 升华**：`success(String)` 坑会反复踩（007 一次、010 又一次）。**写任何返回单值的 GET 联动端点，先问自己返回类型是不是 String**，是就用 `success(msg,data)` 2 参重载或包成对象。
> **坑19 升华**：新旧概念映射（项目组→部门）不只是"字段换名"，要追问"这个值参与哪些过滤、和谁比较"。**过滤条件与被过滤数据必须同源同口径**，否则出现自相矛盾的查询结果。这类逻辑坑测试不易发现（需构造跨层级部门数据），靠 Code Review 的数据链路追踪揪出。
