# 项目立项申请日期（apply_date）改造设计

> 日期：2026-05-26
> 需求来源：打印立项申请书中的「立项日期」原取 `create_time`（创建时间），改为取新增的「立项申请日期」字段；并在立项申请页、编辑页、详情页展示该字段。

## 1. 背景与现状

- **打印取数**：`ruoyi-ui/src/views/project/project/index.vue:431` 立项日期取 `printProject.createTime`，截取前 10 位。
- **立项申请页**：`apply.vue` 第四块「时间规划」已有 开始/结束/投产/验收 日期；验收日期为标准 `el-date-picker`。
- **后端实体**：`Project.java` 有 `productionDate`、`acceptanceDate`（`Date` 类型）。
- **Mapper**：`ProjectMapper.xml` 中 `acceptance_date` 出现在 ~12 处（resultMap、4 个查询列清单、查询过滤、insert 列+值、update、别名查询、统计列清单）。
- **数据库**：`pm_project` 表 `acceptance_date` 列已存在。

## 2. 决策记录

| 决策点 | 结论 |
|---|---|
| 打印旧数据为空时 | **只取新字段，为空显示 `-`**，不回退 `create_time` |
| 编辑页是否加 | **加**（申请页 + 编辑页 + 详情页 + 打印） |
| 新建是否默认今天 | **留空手填**，不预填 |
| 申请页是否带 `validateOnBlur` | **带**，与投产/验收日期保持一致 |
| 列表页（index.vue 表格） | **不动** |

## 3. 字段命名

| 项 | 值 |
|---|---|
| Java 属性 | `applyDate` |
| 数据库列 | `apply_date` (`date`, `DEFAULT NULL`, COMMENT '立项申请日期') |
| 前端表单 | `form.applyDate` |

## 4. 改动清单（全栈纵切）

### 4.1 数据库
- `pm-sql/init/00_tables_ddl.sql`：`pm_project` 表在 `acceptance_date` 后加 `apply_date`。
- `pm-sql/fix_project_apply_date_20260526.sql`（不进 git）：已部署库执行 `ALTER TABLE`。

### 4.2 后端实体 `Project.java`
- 仿 `acceptanceDate` 加 `applyDate`：`@JsonFormat(pattern="yyyy-MM-dd")` + `@Excel(name="立项申请日期"...)` + getter/setter + `toString` 追加。

### 4.3 Mapper `ProjectMapper.xml`（逐处镜像 `acceptance_date`）
1. resultMap `<result>`
2. 4 处查询列清单（含 `selectProjectVo`、统计、别名查询等）
3. 查询过滤 `<if test="applyDate != null">`
4. insert 列 + 值
5. update `<if>`
6. 别名查询 `p.apply_date AS applyDate`
7. 统计列清单

### 4.4 前端
- `apply.vue`：验收日期后加 `applyDate` 的 `el-date-picker`，样式仿投产日期，带 `@blur="validateOnBlur('applyDate')"`；`form` 初值加 `applyDate: null`（不默认今天）。
- `edit.vue`：验收日期后同样加；`form` 加 `applyDate: ''`。
- `detail.vue`：确认日期（`revenueConfirmDate`）后加 `<el-descriptions-item label="立项申请日期">{{ form.applyDate || '-' }}</el-descriptions-item>`。

### 4.5 打印 `index.vue:431`
```vue
{{ printProject.applyDate ? printProject.applyDate.substring(0, 10) : '-' }}
```

## 5. 验证

1. 后端：`mvn clean compile -pl ruoyi-project -am`
2. 新建立项 → 填立项申请日期 → 保存 → 详情页/编辑页可见 → 打印立项日期正确。
3. 旧项目（apply_date 为空）→ 打印立项日期显示 `-`。
