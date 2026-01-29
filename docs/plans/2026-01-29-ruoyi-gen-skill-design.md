# /ruoyi-gen Skill 设计文档

## 概述

交互式代码生成 Skill，对齐 RuoYi Web UI 的三个配置维度（基本信息、字段信息、生成信息），通过对话引导用户完成配置，调用 `ruoyi-gen-cli` 生成脚手架代码，部署到项目目录，并根据自定义 UI 描述做精细化定制。

## 三种输入模式

| 模式 | 用户输入示例 | Claude 动作 |
|---|---|---|
| DDL 模式 | `CREATE TABLE ...` 或 `.sql` 文件路径 | 直接解析 DDL |
| 业务描述模式 | "用户表，有姓名、手机号、状态" | Claude 生成 DDL，展示给用户确认 |
| 数据库查询模式 | "从数据库拉 sys_user 的表结构" | 询问连接方式（IP:port 或 container ID），用 `mysql -e 'SHOW CREATE TABLE'` 获取 DDL |

## 五阶段流程

### 阶段 1：识别输入，获取 DDL

根据 `$ARGUMENTS` 内容判断输入模式：
- 包含 `CREATE TABLE` 或以 `.sql` 结尾 → DDL 模式
- 包含数据库/连接相关关键词 → 数据库查询模式
- 其他自然语言 → 业务描述模式

数据库查询模式下，询问用户连接方式：
- 直接 IP:port + 用户名密码 → `mysql -h<host> -P<port> -u<user> -p<pass> <db> -e "SHOW CREATE TABLE <table>"`
- Docker container ID → `docker exec <id> mysql -u<user> -p<pass> <db> -e "SHOW CREATE TABLE <table>"`

### 阶段 2：智能生成规格文件

拿到 DDL 后，Claude 生成完整的规格文件（`docs/gen-specs/<表名>.yml`），智能推断：

- **基本信息**：从 DDL 的表名/注释推断 className、functionAuthor 读项目默认
- **字段信息**：从列类型推断 javaType、htmlType、queryType，从列名规律推断 dictType
  - `*_type`、`*_sex` → `select`
  - `*_status` → `radio`
  - `*_name` → queryType=`LIKE`
  - `*_content` → `editor`
  - `*_image` → `imageUpload`
  - `*_file` → `fileUpload`
  - `*_time`、`datetime` → `datetime`
  - `varchar(500+)` / `text` → `textarea`
- **生成信息**：从 `generator.yml` 读取默认包名/模块名，根据表名推断 businessName
- **自定义 UI**：如果用户在业务描述中提到了非标准 UI 需求，记录到 `customizations` 节

### 阶段 3：展示配置，用户确认

以结构化方式展示三个配置维度的摘要：

```
=== 基本信息 ===
表名称: sys_product    表描述: 产品表
实体类: Product        作者: ruoyi

=== 生成信息 ===
模板: 单表(crud)       前端: element-plus
包路径: com.ruoyi.system   模块: system
业务名: product        功能名: 产品管理
上级菜单: 3

=== 字段信息 ===
  列名            描述    Java类型  插入 编辑 列表 查询 查询方式 必填 显示类型     字典类型
  product_id      产品ID  Long      ✓    -    -    -    EQ      -    -            -
  product_name    产品名  String    ✓    ✓    ✓    ✓    LIKE    ✓    文本框       -
  product_type    产品类型 String   ✓    ✓    ✓    ✓    EQ      -    下拉框       sys_product_type
  price           价格    BigDecimal ✓   ✓    ✓    -    EQ      -    文本框       -
  status          状态    String    ✓    ✓    ✓    ✓    EQ      -    单选框       sys_normal_disable
  ...

=== 自定义 UI ===
  price: 带货币符号 ¥ 前缀的输入框
```

用户可以直接说"OK"确认，或指出需要修改的部分。

### 阶段 4：持久化规格 + 生成代码

1. 将确认后的配置写入 `docs/gen-specs/<表名>.yml`
2. 从规格文件提取 DDL 写入临时 `.sql` 文件
3. 从规格文件提取 config 部分转换为 `gen-config.yml` 格式
4. 调用 `java -jar ruoyi-gen-cli/target/ruoyi-gen-cli.jar --sql=<tmp>.sql --config=<tmp>.yml --output=<tmp>.zip`
5. 解压 ZIP，按包名映射拷贝到项目目录：
   - `main/java/...` → `ruoyi-project/src/main/java/...` 或对应模块
   - `main/resources/mapper/...` → `ruoyi-project/src/main/resources/mapper/...`
   - `vue/api/...` → `ruoyi-ui/src/api/...`
   - `vue/views/...` → `ruoyi-ui/src/views/...`
   - `*Menu.sql` → `sql/` 目录

### 阶段 5：精细化定制

根据 `customizations` 节中的描述，逐个修改已部署的文件：
- 修改 Vue 组件（替换标准控件为自定义实现）
- 修改 Service / Controller（如自定义数据源加载）
- 安装需要的 npm 依赖（如果引入了新组件）

## 增量重跑

当 `docs/gen-specs/<表名>.yml` 已存在时：

```
/ruoyi-gen 重新生成 sys_product
```

1. 读取已有规格文件
2. 展示当前配置，询问是否需要修改
3. 用户修改后重新确认
4. 重新走阶段 4-5

## 规格文件格式

路径：`docs/gen-specs/<表名>.yml`

```yaml
# ============================
# Tab 1: 基本信息
# ============================
basicInfo:
  tableName: sys_config
  tableComment: 参数配置表
  className: SysConfig
  functionAuthor: ruoyi
  remark: ""

# ============================
# Tab 2: 字段信息
# ============================
columns:
  config_id:
    columnComment: 参数主键
    columnType: int(5)
    javaType: Integer
    javaField: configId
    isInsert: true
    isEdit: false
    isList: false
    isQuery: false
    queryType: EQ
    isRequired: false
    htmlType: input
    dictType: ""
  config_name:
    columnComment: 参数名称
    columnType: varchar(100)
    javaType: String
    javaField: configName
    isInsert: true
    isEdit: true
    isList: true
    isQuery: true
    queryType: LIKE
    isRequired: true
    htmlType: input
    dictType: ""

# ============================
# Tab 3: 生成信息
# ============================
genInfo:
  tplCategory: crud
  tplWebType: element-plus
  packageName: com.ruoyi.system
  moduleName: system
  businessName: config
  functionName: 参数配置
  parentMenuId: 1
  # treeCode / treeParentCode / treeName  (tplCategory=tree)
  # subTableName / subTableFkName         (tplCategory=sub)

# ============================
# 自定义 UI
# ============================
customizations: {}

# ============================
# 原始 DDL
# ============================
ddl: |
  CREATE TABLE sys_config (...) ENGINE=InnoDB COMMENT='参数配置表';
```

## 文件结构

```
.claude/skills/ruoyi-gen/
├── SKILL.md                    # 主指令文件
docs/gen-specs/
├── sys_product.yml             # 产品表规格（示例）
├── sys_order.yml               # 订单表规格（示例）
```
