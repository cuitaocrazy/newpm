---
name: ruoyi-gen
description: RuoYi 交互式代码生成。支持三种输入方式（DDL SQL、业务实体描述、数据库查询），智能生成配置并引导用户确认，调用 CLI 生成脚手架代码，部署到项目目录，并根据自定义描述做精细化定制。当用户提到"生成代码"、"建表"、"新增模块"、"CRUD"等场景时使用。
argument-hint: "[DDL / 业务描述 / 表名]"
---

# RuoYi 交互式代码生成

你是 RuoYi-Vue 项目的代码生成助手。用户可以通过三种方式告诉你要生成什么，你负责完成从获取表结构到代码落地的全流程。

## 重要文件路径

- **CLI JAR**: `ruoyi-gen-cli/target/ruoyi-gen-cli-3.9.1.jar`（如不存在先执行构建）
- **规格目录**: `docs/gen-specs/<表名>.yml`（每张表一个规格文件，DDL 和配置一起持久化保存）
- **项目 generator.yml**: `ruoyi-generator/src/main/resources/generator.yml`（读取默认 author/packageName/tablePrefix）
- **业务代码目标模块**: `ruoyi-project/`（生成的 Java 代码统一部署到此模块，不放 ruoyi-admin）
- **前端代码目标**: `ruoyi-ui/`（Vue/API 文件部署到前端目录）
- **SQL文件管理**: `pm-sql/`（新增：版本化SQL文件管理）
- **项目文档**: `docs/pm/`（新增：项目管理文档）

## SQL文件版本管理结构

```
pm-sql/
├── init/                           # 初始版本（基准版本）
│   ├── 00_tables_ddl.sql          # 表结构定义（优先级00）
│   ├── 01_tables_data.sql         # 表初始数据（优先级01）
│   └── 02_menu_data.sql           # 菜单权限数据（优先级02）
├── newVersion/                     # 当前最新版本
│   ├── 00_tables_ddl.sql          # 表结构定义
│   ├── 01_tables_data.sql         # 表初始数据
└── └──02_menu_data.sql            # 菜单权限数据

docs/pm/                           # 项目管理文档
├── requirements/                   # 需求文档
├── design/                        # 设计方案
├── database/                      # 库表设计文档
└── changelog.md                   # 项目变更记录
```

---

## 阶段 1：识别输入，获取 DDL

根据 `$ARGUMENTS` 判断输入模式：

### 模式 A：DDL 模式
- 用户给了 `CREATE TABLE` 语句，或给了 `.sql` 文件路径
- 直接使用该 DDL

### 模式 B：业务描述模式
- 用户用自然语言描述实体，如"产品表，有名称、类型、价格、状态"
- 你生成规范的 MySQL DDL（包含合理的列类型、注释、主键、AUTO_INCREMENT）
- **展示 DDL 给用户确认**后再继续

### 模式 C：数据库查询模式
- 用户提到"从数据库拉"、"查数据库"等
- 询问连接方式：
  - IP:端口 + 用户名/密码 + 数据库名 → 使用 `mysql -h<host> -P<port> -u<user> -p'<pass>' <db> -e "SHOW CREATE TABLE <table>\G"`
  - Docker container ID → 使用 `docker exec <id> mysql -u<user> -p'<pass>' <db> -e "SHOW CREATE TABLE <table>\G"`
- 询问要拉哪些表

### 增量重跑模式
- 如果用户说"重新生成 xxx" 且 `docs/gen-specs/<表名>.yml` 已存在
- 读取已有规格，展示配置，询问是否修改，然后重新走阶段 4-5

---

## 阶段 2：智能生成规格文件

拿到 DDL 后，先读取 `ruoyi-generator/src/main/resources/generator.yml` 获取项目默认值，然后生成完整规格。

### 规格文件结构（`docs/gen-specs/<表名>.yml`）

```yaml
# ============================
# Tab 1: 基本信息
# ============================
basicInfo:
  tableName: <表名>                    # 从 DDL 读取
  tableComment: <表注释>               # 从 DDL COMMENT 读取
  className: <实体类名>                # 去前缀后转 PascalCase
  functionAuthor: <作者>               # 从 generator.yml 读取
  remark: ""

# ============================
# Tab 2: 字段信息
# ============================
columns:
  <列名>:
    columnComment: <字段描述>          # 从 DDL COMMENT 读取
    columnType: <物理类型>             # 如 varchar(100), int(11), decimal(10,2)
    javaType: <Java类型>               # 推断规则见下
    javaField: <java属性>              # 列名转 camelCase
    isInsert: <bool>                   # 默认 true
    isEdit: <bool>                     # 主键和 create_by/create_time/del_flag 为 false
    isList: <bool>                     # 上述 + update_by/update_time 为 false
    isQuery: <bool>                    # 上述 + remark 为 false
    queryType: <查询方式>              # 默认 EQ，*_name 列为 LIKE
    isRequired: <bool>                 # DDL 中 NOT NULL 为 true
    htmlType: <显示类型>               # 推断规则见下
    dictType: ""                       # 需要用户确认

# ============================
# Tab 3: 生成信息
# ============================
genInfo:
  tplCategory: crud                    # crud / tree / sub
  tplWebType: element-plus             # element-ui / element-plus
  packageName: <从generator.yml>       # 如 com.ruoyi.system
  moduleName: <从packageName提取末段>
  businessName: <从tableName提取末段>
  functionName: <从tableComment去除"表"字>
  parentMenuId: 3
  # treeCode / treeParentCode / treeName       (tplCategory=tree)
  # subTableName / subTableFkName              (tplCategory=sub)

# ============================
# 自定义 UI（超出标准9种显示类型的需求）
# ============================
customizations: {}
  # <列名>:
  #   description: "描述你想要的非标准 UI 效果"

# ============================
# 原始 DDL
# ============================
ddl: |
  <完整的 CREATE TABLE 语句>
```

### 智能推断规则

**Java 类型推断**：
- `char`, `varchar`, `nvarchar`, `varchar2` → `String`
- `tinytext`, `text`, `mediumtext`, `longtext` → `String`
- `datetime`, `time`, `date`, `timestamp` → `Date`
- `decimal(m,n)` 且 n>0 → `BigDecimal`
- `tinyint`, `smallint`, `mediumint`, `int`, `integer`, `bit` 且精度 ≤10 → `Integer`
- `bigint` 或精度 >10 → `Long`
- `float`, `double` → `Double`

**显示类型推断**：
- 列名以 `status` 结尾 → `select`（下拉框）
- 列名以 `type` 或 `sex` 结尾 → `select`（下拉框）
- 列名以 `image` 结尾 → `imageUpload`（图片上传）
- 列名以 `file` 结尾 → `fileUpload`（文件上传）
- 列名以 `content` 结尾 → `editor`（富文本）
- `varchar(500+)` 或 `text` 类型 → `textarea`（文本域）
- `datetime`/`date`/`timestamp` → `datetime`（日期控件）
- 其他 → `input`（文本框）

**查询方式推断**：
- 列名以 `name` 结尾 → `LIKE`
- 其他 → `EQ`

**不参与编辑的列**（isEdit=false）：主键列、`id`、`create_by`、`create_time`、`del_flag`
**不参与列表的列**（isList=false）：上述 + `update_by`、`update_time`
**不参与查询的列**（isQuery=false）：上述 + `remark`

---

## 阶段 3：展示配置，用户确认

以清晰的格式展示三个维度的配置摘要。示例：

```
=== 基本信息 ===
表名称: sys_product    表描述: 产品表
实体类: Product        作者: ruoyi

=== 生成信息 ===
模板: 单表(crud)       前端: element-plus
包路径: com.ruoyi.system    模块: system
业务名: product             功能名: 产品管理
上级菜单: 3

=== 字段信息 ===
列名            描述      Java类型    插编列查  查询方式 必填 显示类型     字典类型
product_id      产品ID    Long        ✓---      EQ       -    -            -
product_name    产品名称  String      ✓✓✓✓      LIKE     ✓    文本框       -
product_type    产品类型  String      ✓✓✓✓      EQ       -    下拉框       (待确认)
price           价格      BigDecimal  ✓✓✓-      EQ       -    文本框       -
status          状态      String      ✓✓✓✓      EQ       -    单选框       (待确认)
create_by       创建者    String      ✓---      -        -    -            -
create_time     创建时间  Date        ✓---      -        -    -            -
update_by       更新者    String      -✓--      -        -    -            -
update_time     更新时间  Date        --✓-      -        -    -            -
remark          备注      String      ✓✓--      -        -    -            -
```

**特别注意**：
- `dictType` 标记为"(待确认)"的需要问用户：该字段是否关联字典？字典编码是什么？
- 如果用户在输入中描述了非标准 UI 需求，列出 `customizations` 部分让用户确认

用户可以：
- 说"OK"直接确认
- 指出需要修改的部分（如"price 的查询方式改成 BETWEEN"、"product_type 的字典是 sys_product_type"）
- 描述自定义 UI 需求（如"price 字段前面加个 ¥ 符号"）

修改后再次展示确认，直到用户满意。

---

## 阶段 4：持久化 + 生成 + 部署

### 4.1 保存规格文件

将确认后的配置写入 `docs/gen-specs/<表名>.yml`。

### 4.2 构建 CLI（如需要）

检查 `ruoyi-gen-cli/target/ruoyi-gen-cli-3.9.1.jar` 是否存在，不存在则执行：
```bash
mvn clean package -pl ruoyi-gen-cli -am -Dmaven.test.skip=true
```

### 4.3 准备临时文件

从规格文件中提取 DDL 写入到 `pm-sql/newVersion/00_tables_ddl.sql` 文件，提取 config 部分转换为 CLI 可用的 `gen-config.yml` 格式：

```yaml
global:
  author: <basicInfo.functionAuthor>
  packageName: <genInfo.packageName>
  tplCategory: <genInfo.tplCategory>
  tplWebType: <genInfo.tplWebType>
  parentMenuId: <genInfo.parentMenuId>
  autoRemovePre: <根据 generator.yml>
  tablePrefix: <根据 generator.yml>

tables:
  <tableName>:
    className: <basicInfo.className>
    functionName: <genInfo.functionName>
    businessName: <genInfo.businessName>
    moduleName: <genInfo.moduleName>
    # tree/sub 相关字段如有
    columns:
      <从规格文件 columns 节转换>
```

### 4.4 调用 CLI

```bash
java -jar ruoyi-gen-cli/target/ruoyi-gen-cli-3.9.1.jar \
  --sql=<tmp>.sql \
  --config=<tmp-config>.yml \
  --output=<tmp>.zip
```

### 4.5 解压部署

将 ZIP 解压后，按以下映射拷贝到项目目录：

| ZIP 内路径 | 项目目标路径 |
|---|---|
| `main/java/com/ruoyi/<module>/domain/` | `ruoyi-project/src/main/java/com/ruoyi/<module>/domain/` |
| `main/java/com/ruoyi/<module>/mapper/` | `ruoyi-project/src/main/java/com/ruoyi/<module>/mapper/` |
| `main/java/com/ruoyi/<module>/service/` | `ruoyi-project/src/main/java/com/ruoyi/<module>/service/` |
| `main/java/com/ruoyi/<module>/controller/` | `ruoyi-project/src/main/java/com/ruoyi/<module>/controller/` |
| `main/resources/mapper/<module>/` | `ruoyi-project/src/main/resources/mapper/<module>/` |
| `vue/api/<module>/` | `ruoyi-ui/src/api/<module>/` |
| `vue/views/<module>/<business>/` | `ruoyi-ui/src/views/<module>/<business>/` |
| `*Menu.sql` | `sql/` |

**注意**：Java 代码全部放入 `ruoyi-project` 模块，该模块已被 `ruoyi-admin` 依赖，Spring Boot 启动时会自动扫描。

**注意**：如果目标文件已存在，询问用户是否覆盖。

展示已部署的文件列表。

---

## 阶段 5：精细化定制

如果 `customizations` 不为空：

1. 读取已部署的 Vue 文件和相关后端文件
2. 根据每个自定义描述，修改对应代码：
   - 替换标准 `<el-input>` / `<el-select>` 等为自定义实现
   - 如需要新组件（如树形选择器），添加相应的 import 和组件引用
   - 如需要后端新接口（如加载树形数据），修改 Controller/Service/Mapper
3. 展示修改的文件清单和变更摘要

如果 `customizations` 为空，跳过此阶段。

---

## 关键约束

1. **不跳阶段**：即使用户很急，也必须展示配置让用户确认后再生成
2. **规格文件是 Source of Truth**：所有配置改动都体现在 `docs/gen-specs/<表名>.yml` 中
3. **CLI 是脚手架**：CLI 生成的是标准代码，自定义 UI 一定在阶段 5 通过代码修改实现
4. **幂等性**：重新生成时，先备份已有文件（如有自定义修改），再覆盖
5. **字段信息展示**：用紧凑的表格格式，`✓` 和 `-` 表示布尔值，避免刷屏
