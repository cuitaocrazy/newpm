# 更新人显示用户昵称 - 修复说明

## 问题描述

项目列表中的"更新人"字段显示的是用户名（如 "admin"），而不是用户昵称（如"张三"）

## 修复内容

### 1. 修改 SQL 查询（ProjectMapper.xml）

**文件位置：** `ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml`

**修改内容：**

```xml
<!-- 添加关联查询 -->
LEFT JOIN sys_user updater ON p.update_by = updater.user_name

<!-- 修改字段查询 -->
COALESCE(updater.nick_name, p.update_by) AS update_by,
```

**完整 SQL（第102-162行）：**
```sql
<select id="selectProjectList" parameterType="Project" resultMap="ProjectResult">
    SELECT
        p.project_id, p.project_code, p.project_name, ...,
        p.create_by, p.create_time,
        COALESCE(updater.nick_name, p.update_by) AS update_by,  -- 修改这里
        p.update_time,
        ...
    FROM pm_project p
    LEFT JOIN pm_project_contract_rel pcr ON ...
    LEFT JOIN pm_contract c ON ...
    LEFT JOIN sys_user pm ON p.project_manager_id = pm.user_id
    LEFT JOIN sys_user mm ON p.market_manager_id = mm.user_id
    LEFT JOIN sys_user updater ON p.update_by = updater.user_name  -- 添加这行
    <where>
        ...
    </where>
</select>
```

### 2. 更新元数据（pm_project.yml）

**文件位置：** `docs/gen-specs/pm_project.yml`

**添加说明：**
```yaml
update_by:
  columnComment: 更新者
  notes: |
    显示更新人的用户昵称（nick_name），而不是用户名（user_name）

    SQL 实现：
    LEFT JOIN sys_user updater ON p.update_by = updater.user_name
    SELECT COALESCE(updater.nick_name, p.update_by) AS update_by
```

## 重启步骤

### 1. 重新编译后端

```bash
cd /Users/kongli/ws-claude/PM/newpm
mvn clean package -pl ruoyi-admin -am -Dmaven.test.skip=true
```

### 2. 停止旧的后端服务

```bash
# 查找进程
ps aux | grep ruoyi-admin.jar | grep -v grep

# 如果有进程在运行，停止它
kill <PID>
```

### 3. 启动新的后端服务

```bash
java -Xms256m -Xmx1024m -jar ruoyi-admin/target/ruoyi-admin.jar
```

或者使用后台运行：
```bash
nohup java -Xms256m -Xmx1024m -jar ruoyi-admin/target/ruoyi-admin.jar > logs/ruoyi-admin.log 2>&1 &
```

### 4. 刷新前端页面

- 在浏览器中按 `Ctrl+Shift+R`（Windows/Linux）或 `Cmd+Shift+R`（Mac）强制刷新
- 或者清除浏览器缓存后刷新

## 验证步骤

1. **编辑项目**
   - 进入项目列表
   - 点击任意项目的"编辑"按钮
   - 修改"备注"字段
   - 点击"保存"

2. **查看列表**
   - 返回项目列表
   - 查看该项目的"更新人"列
   - ✅ 应该显示用户昵称（如"张三"）
   - ❌ 不应该显示用户名（如"admin"）

## 技术说明

### COALESCE 函数

```sql
COALESCE(updater.nick_name, p.update_by) AS update_by
```

**作用：**
- 优先返回 `updater.nick_name`（用户昵称）
- 如果为 NULL，则返回 `p.update_by`（用户名）
- 确保即使用户被删除，仍能显示原始用户名

### 关联逻辑

```sql
LEFT JOIN sys_user updater ON p.update_by = updater.user_name
```

**说明：**
- `p.update_by` 存储的是用户名（user_name）
- 通过 user_name 关联 sys_user 表
- 获取用户的昵称（nick_name）

## 影响范围

- ✅ 项目列表页面（index.vue）
- ✅ 所有调用 `selectProjectList` 方法的地方
- ❌ 不影响其他模块

## 注意事项

1. **必须重启后端**
   - MyBatis 的 XML 配置在启动时加载
   - 修改后必须重启才能生效

2. **前端无需修改**
   - 前端代码已经正确绑定 `updateBy` 字段
   - 只需要后端返回正确的数据

3. **数据库无需修改**
   - 不需要修改表结构
   - 不需要迁移数据
   - 只是查询时关联显示

## 相关文件

- `ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml` - SQL 查询
- `ruoyi-ui/src/views/project/project/index.vue` - 前端列表页面
- `docs/gen-specs/pm_project.yml` - 元数据配置

## 完成时间

2026-02-08
