# ruoyi-gen 技能简化设计

日期: 2026-02-02

## 目标

简化 ruoyi-gen 技能的输入方式，从三种输入模式（DDL/业务描述/数据库查询）精简为只接受表名，DDL 统一从 `pm-sql/init/00_tables_ddl.sql` 查找。

## 核心变更

### 输入简化

- **之前**: 支持 DDL SQL、业务实体描述、数据库查询三种输入模式
- **之后**: 只接受表名，如 `/ruoyi-gen pm_project`
- 一次只处理一个主表

### DDL 来源固定

- 从 `pm-sql/init/00_tables_ddl.sql` 中按表名查找对应的 `CREATE TABLE` 语句
- 找到 → 提取 DDL，进入下一阶段
- 找不到 → 提示用户先将 DDL 添加到该文件

### 外键关联分析

- 解析主表 DDL 中的 `FOREIGN KEY ... REFERENCES` 约束，记录主表引用了哪些父表
- 扫描整个 `00_tables_ddl.sql`，找出哪些表通过外键引用了主表（子表）
- 关联信息仅作为参考展示，不自动为关联表生成代码
- 帮助用户决定 `tplCategory` 配置（crud / tree / sub）
- 非强关系引用由用户在字段描述中补充

### 菜单 SQL 去重

- 生成的菜单 SQL 与 `pm-sql/init/02_menu_data.sql` 对比
- 通过权限标识前缀匹配判断是否已存在
- 不存在 → 追加到 `pm-sql/init/02_menu_data.sql`
- 已存在 → 提示跳过

### 规格文件精简

- 移除 `ddl:` 字段（DDL 已在 `00_tables_ddl.sql` 中，不需要冗余存储）

## 移除的内容

- 模式 B（业务描述模式）
- 模式 C（数据库查询模式）
- 多表 DDL 批量处理
- `pm-sql/newVersion/` 所有相关引用
- 规格文件中的 `ddl:` 字段
- 菜单数据库执行环节（Docker/直连/SSH）
- 临时文件清理章节

## 保留不变的内容

- 阶段 2：智能推断规则（Java 类型、显示类型、查询方式）
- 阶段 3：展示配置 + 用户确认流程
- 阶段 4：规格持久化 → CLI 构建 → 调用 CLI → 解压部署
- 阶段 5：精细化定制（customizations）
- 增量重跑模式（已有规格文件时）

## 新增的内容

- 从 `00_tables_ddl.sql` 查找解析 DDL
- 外键关联分析 + 关联信息展示（在配置摘要中新增"关联分析"区块）
- 菜单 SQL 与 `pm-sql/init/02_menu_data.sql` 文件级去重对比
