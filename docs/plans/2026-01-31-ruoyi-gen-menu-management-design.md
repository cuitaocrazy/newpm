# RuoYi-Gen 菜单管理功能设计

**日期**: 2026-01-31
**版本**: 1.0
**状态**: 已确认

## 概述

在 ruoyi-gen 技能的代码生成流程中，添加自动化的菜单管理功能。该功能在代码生成完成后，自动检查菜单是否存在，如不存在则引导用户生成菜单SQL并导入数据库。

## 核心需求

1. 代码生成后自动检查菜单是否存在
2. 通过权限标识（perms）匹配菜单
3. 询问用户选择一级菜单
4. 生成完整的菜单项和按钮权限（7个）
5. 智能更新 SQL 文件（存在则更新，不存在则追加）
6. 支持多种数据库连接方式执行 SQL

## 详细设计

### 1. 核心流程

#### 1.1 触发时机
代码生成完成并部署后，自动进入菜单管理流程。

#### 1.2 执行步骤

**步骤1：检查菜单是否存在**
- 连接数据库，查询 `sys_menu` 表
- 通过权限标识前缀匹配（如 `project:customer:%`）
- SQL示例：
  ```sql
  SELECT COUNT(*) FROM sys_menu WHERE perms LIKE 'project:customer:%'
  ```
- 如果找到任何匹配记录，说明菜单已存在，跳过生成流程

**步骤2：询问一级菜单**
- 如果菜单不存在，询问用户："该功能应该挂在哪个一级菜单下？"
- 从数据库查询所有一级菜单：
  ```sql
  SELECT menu_id, menu_name, path
  FROM sys_menu
  WHERE menu_type = 'M' AND parent_id = 0
  ORDER BY order_num
  ```
- 以多选题形式展示，格式：`[菜单ID] 菜单名称 (路径)`
- 用户选择后，获取对应的 `menu_id` 作为 `parent_id`

**步骤3：生成菜单SQL**
- 生成1个菜单项（二级菜单，`menu_type='C'`）
- 生成7个按钮权限（`menu_type='F'`）：
  1. 查询 - `{module}:{business}:query`
  2. 新增 - `{module}:{business}:add`
  3. 修改 - `{module}:{business}:edit`
  4. 删除 - `{module}:{business}:remove`
  5. 导出 - `{module}:{business}:export`
  6. 导入 - `{module}:{business}:import`
  7. 详情 - `{module}:{business}:detail`

### 2. SQL文件管理

#### 2.1 文件位置
`pm-sql/newVersion/02_menu_data.sql`

#### 2.2 智能更新策略

**读取现有文件**
1. 解析 `02_menu_data.sql` 文件内容
2. 识别每个 INSERT 语句中的权限标识（perms 字段）
3. 建立权限标识到SQL语句的映射关系

**匹配和更新**
1. 对于新生成的8条菜单SQL（1个菜单 + 7个按钮）
2. 逐条检查权限标识是否已存在于文件中
3. **如果存在**：替换原有的 INSERT 语句（保持在原位置）
4. **如果不存在**：追加到文件末尾

**SQL语句格式**
```sql
-- 客户信息管理菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('客户信息管理', 2000, 1, 'customer', 'project/customer/index', 1, 0, 'C', '0', '0', 'project:customer:list', 'user', 'admin', sysdate(), '', null, '客户信息管理菜单');

-- 客户信息管理按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('客户查询', 2001, 1, '#', '', 1, 0, 'F', '0', '0', 'project:customer:query', '#', 'admin', sysdate(), '', null, '');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('客户新增', 2001, 2, '#', '', 1, 0, 'F', '0', '0', 'project:customer:add', '#', 'admin', sysdate(), '', null, '');

-- ... 其他按钮权限
```

#### 2.3 menu_id 管理
- 查询当前最大的 menu_id
- 新菜单从 `MAX(menu_id) + 1` 开始分配
- 按钮权限的 parent_id 指向菜单项的 menu_id

### 3. 数据库执行

#### 3.1 执行时机
SQL文件更新完成后，立即执行导入操作。

#### 3.2 连接方式选择

询问用户："如何连接到数据库执行菜单SQL？"

**选项A：Docker容器**
- 用户提供：容器ID
- 执行命令：
  ```bash
  docker exec -i <container_id> mysql -u<user> -p'<pass>' <db> < pm-sql/newVersion/02_menu_data.sql
  ```

**选项B：直接连接**
- 用户提供：host、port、username、password、database
- 执行命令：
  ```bash
  mysql -h<host> -P<port> -u<user> -p'<pass>' <db> < pm-sql/newVersion/02_menu_data.sql
  ```

**选项C：SSH隧道**
- 用户提供：SSH host、SSH user、SSH port、数据库信息
- 步骤：
  1. 通过SCP上传SQL文件到远程服务器
  2. SSH连接后执行：
     ```bash
     ssh <ssh_user>@<ssh_host> -p<ssh_port> "mysql -u<user> -p'<pass>' <db> < /path/to/02_menu_data.sql"
     ```

#### 3.3 错误处理
- 如果执行失败，显示错误信息
- 询问用户是否需要手动执行
- 提供完整的执行命令供用户复制

### 4. 用户交互流程

#### 4.1 代码生成完成后
```
✓ 代码已生成并部署完成
→ 正在检查菜单是否存在...
```

#### 4.2 菜单检查结果
**场景1：菜单已存在**
```
✓ 检测到菜单已存在（权限标识：project:customer:*）
→ 跳过菜单生成
```

**场景2：菜单不存在**
```
✗ 未检测到菜单
→ 开始菜单生成流程
```

#### 4.3 一级菜单选择
```
该功能应该挂在哪个一级菜单下？

A) [2000] 市场管理 (/market)
B) [1000] 系统管理 (/system)
C) [3000] 项目管理 (/project)
D) [4000] 日报管理 (/daily)

请选择 (A/B/C/D):
```

#### 4.4 SQL生成确认
```
将生成以下菜单结构：

菜单项：客户信息管理
  ├─ 查询 (project:customer:query)
  ├─ 新增 (project:customer:add)
  ├─ 修改 (project:customer:edit)
  ├─ 删除 (project:customer:remove)
  ├─ 导出 (project:customer:export)
  ├─ 导入 (project:customer:import)
  └─ 详情 (project:customer:detail)

将更新 0 条，新增 8 条菜单记录到 pm-sql/newVersion/02_menu_data.sql

确认生成菜单SQL吗？(Y/n):
```

#### 4.5 数据库连接选择
```
如何连接到数据库执行菜单SQL？

A) Docker容器 - 提供容器ID
B) 直接连接 - 提供IP、端口、用户名、密码
C) SSH隧道 - 通过SSH连接远程服务器

请选择 (A/B/C):
```

**选择A后：**
```
请输入Docker容器ID: mysql-container-123
正在执行SQL导入...
```

**选择B后：**
```
请输入数据库信息：
Host: localhost
Port: 3306
Username: root
Password: ******
Database: ry-vue

正在执行SQL导入...
```

**选择C后：**
```
请输入SSH连接信息：
SSH Host: 192.168.1.100
SSH User: root
SSH Port: 22
数据库用户名: root
数据库密码: ******
数据库名: ry-vue

正在上传SQL文件...
正在执行SQL导入...
```

#### 4.6 执行结果反馈

**成功：**
```
✓ 菜单SQL已成功导入数据库
✓ 共导入 8 条菜单记录

提示：请刷新前端页面查看新菜单
```

**失败：**
```
✗ 菜单SQL导入失败

错误信息：
ERROR 1045 (28000): Access denied for user 'root'@'localhost'

您可以手动执行以下命令：
mysql -hlocalhost -P3306 -uroot -p'password' ry-vue < pm-sql/newVersion/02_menu_data.sql
```

## 技术实现要点

### 5.1 数据库连接
- 优先从 `application-druid.yml` 读取数据库配置
- 支持环境变量覆盖
- 密码需要安全处理（不记录到日志）

### 5.2 SQL解析
- 使用正则表达式提取 perms 字段值
- 匹配模式：`perms\s*=\s*'([^']+)'`
- 建立权限标识索引，快速查找

### 5.3 menu_id 分配
- 查询当前最大 menu_id：`SELECT MAX(menu_id) FROM sys_menu`
- 新菜单从 `max_id + 1` 开始
- 确保 menu_id 唯一性

### 5.4 文件更新
- 使用临时文件避免写入失败导致数据丢失
- 更新完成后原子性替换原文件
- 保持文件格式和注释结构

## 集成到 ruoyi-gen 技能

### 6.1 修改位置
在 `SKILL.md` 的"阶段 4：持久化 + 生成 + 部署"之后，添加"阶段 5：菜单管理"

### 6.2 流程集成
```
阶段 4：持久化 + 生成 + 部署
  ↓
阶段 5：菜单管理
  ├─ 检查菜单是否存在
  ├─ 询问一级菜单
  ├─ 生成菜单SQL
  ├─ 更新SQL文件
  └─ 执行SQL导入
  ↓
完成
```

## 示例场景

### 场景1：首次生成客户管理模块
```
用户：生成客户管理模块
助手：[执行代码生成流程]
助手：✓ 代码已生成并部署完成
助手：→ 正在检查菜单是否存在...
助手：✗ 未检测到菜单
助手：该功能应该挂在哪个一级菜单下？
      A) [2000] 市场管理 (/market)
      B) [1000] 系统管理 (/system)
用户：A
助手：[生成菜单SQL预览]
助手：确认生成菜单SQL吗？
用户：Y
助手：如何连接到数据库？
      A) Docker容器
      B) 直接连接
用户：B
助手：请输入数据库信息...
用户：[输入连接信息]
助手：✓ 菜单SQL已成功导入数据库
```

### 场景2：重新生成已有模块
```
用户：重新生成客户管理模块
助手：[执行代码生成流程]
助手：✓ 代码已生成并部署完成
助手：→ 正在检查菜单是否存在...
助手：✓ 检测到菜单已存在（权限标识：project:customer:*）
助手：→ 跳过菜单生成
```

## 后续优化方向

1. **菜单模板管理**：支持自定义菜单模板
2. **批量生成**：一次生成多个模块的菜单
3. **菜单预览**：在导入前预览菜单在系统中的效果
4. **回滚功能**：支持菜单导入失败后的回滚
5. **权限验证**：检查生成的权限标识是否与代码中的注解一致

## 总结

该设计方案通过自动化的菜单管理流程，简化了 RuoYi 项目的开发流程，减少了手动配置菜单的工作量。通过智能更新策略和多种数据库连接方式，提供了灵活且可靠的菜单管理解决方案。
