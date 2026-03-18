# 实现计划：日报白名单管理

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**分支**：`002-daily-report-whitelist` | **日期**：2026-03-17 | **规格**：[spec.md](spec.md)

**目标**：新增日报白名单管理功能，管理员可在系统管理菜单下维护免填日报的人员名单（含原因），白名单人员被禁止提交日报，且从日报动态和统计报表的统计范围中排除。

**架构**：新增 `pm_daily_report_whitelist` 表 + 完整 CRUD 后端；修改 `selectActivityUsers` 和相关用户范围 SQL 追加白名单排除子查询；前端新增白名单管理页面，write.vue 加白名单检查拦截。

**技术栈**：Java 17 / Spring Boot 3.5.8 / MyBatis；Vue 3 / TypeScript / Element Plus

---

## 技术上下文

**语言/版本**：Java 17（后端）、Vue 3 + TypeScript 5.6（前端）
**主要依赖**：Spring Boot 3.5.8、MyBatis、Element Plus 2.13
**存储**：MySQL 8.x，新增 `pm_daily_report_whitelist` 表
**项目类型**：企业后台管理系统

---

## 宪法合规检查

| 原则 | 状态 | 说明 |
|------|------|------|
| I. 业务完整性 | ✅ | 增删接口加 `@Log`；软删除（符合规范，白名单不在硬删除例外表中） |
| II. 权限驱动 | ✅ | 管理接口用 `@PreAuthorize("@ss.hasRole('admin')")`；`checkSelf` 无需特殊权限 |
| III. API 一致性 | ✅ | 新 Controller 继承 `BaseController`；list 接口调用 `startPage()` |
| IV. 关注点分离 | ✅ | 不涉及 `pm_project` / `pm_task` |
| V. 数据库规范 | ✅ | 新表用 `utf8mb4_unicode_ci`（与系统表同），JOIN 时无需额外 COLLATE |
| VI. 前端组件 | ✅ | 人员选择使用 `<user-select post-code="pm" />`；部门展示用数据驱动 |

---

## 项目结构

```text
后端（ruoyi-project 模块）：
ruoyi-project/src/main/java/com/ruoyi/project/
├── domain/
│   └── DailyReportWhitelist.java          # 新增：白名单实体
├── mapper/
│   └── DailyReportWhitelistMapper.java    # 新增：Mapper 接口
├── service/
│   ├── IDailyReportWhitelistService.java  # 新增：Service 接口
│   └── impl/DailyReportWhitelistServiceImpl.java  # 新增：实现
└── controller/
    └── DailyReportWhitelistController.java  # 新增：Controller

ruoyi-project/src/main/resources/mapper/project/
├── DailyReportWhitelistMapper.xml         # 新增：SQL
└── DailyReportMapper.xml                  # 修改：3 处排除白名单

前端（ruoyi-ui）：
ruoyi-ui/src/
├── api/project/whitelist.js               # 新增：API 函数
└── views/
    ├── system/whitelist/
    │   └── index.vue                      # 新增：白名单管理页面
    └── project/dailyReport/
        └── write.vue                      # 修改：加白名单检查拦截

数据库 SQL：
pm-sql/init/00_tables_ddl.sql              # 修改：追加白名单表 DDL
pm-sql/init/02_menu_data.sql               # 修改：追加白名单菜单
pm-sql/fix_whitelist_20260317.sql          # 新增：增量 DDL + 菜单（部署用，gitignored）
```

---

## Task 1：新增数据库表 DDL

**文件**：
- 修改：`pm-sql/init/00_tables_ddl.sql`
- 新建：`pm-sql/fix_whitelist_20260317.sql`（gitignored）

**步骤 1**：在 `00_tables_ddl.sql` 末尾追加建表语句

```sql
-- 日报填写白名单
CREATE TABLE `pm_daily_report_whitelist` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       NOT NULL                COMMENT '白名单用户ID',
  `reason`      VARCHAR(500) NOT NULL                COMMENT '加入原因',
  `del_flag`    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0正常 1删除)',
  `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建者',
  `create_time` DATETIME     DEFAULT NULL            COMMENT '创建时间',
  `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新者',
  `update_time` DATETIME     DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='日报填写白名单（免填日报人员名单）';
```

**步骤 2**：创建 fix SQL（含建表 + 菜单，gitignored）

**步骤 3**：在本地 Docker MySQL 执行建表
```bash
cat pm-sql/fix_whitelist_20260317.sql | docker exec -i 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue
```

**步骤 4**：提交（只提交 00_tables_ddl.sql）
```bash
git add pm-sql/init/00_tables_ddl.sql
git commit -m "feat: 新增 pm_daily_report_whitelist 表 DDL"
```

---

## Task 2：新增 DailyReportWhitelist 实体类

**文件**：
- 新建：`ruoyi-project/src/main/java/com/ruoyi/project/domain/DailyReportWhitelist.java`

```java
package com.ruoyi.project.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class DailyReportWhitelist extends BaseEntity {
    private Long id;

    @NotNull(message = "用户不能为空")
    private Long userId;

    @NotBlank(message = "加入原因不能为空")
    private String reason;

    private String delFlag;

    // 显示字段（JOIN 查询结果）
    @Excel(name = "姓名")
    private String nickName;

    @Excel(name = "部门")
    private String deptName;

    // 搜索条件（transient）
    private String keyword;

    // getters and setters
}
```

**步骤**：创建文件，编译验证
```bash
mvn clean compile -pl ruoyi-project -am
git add ruoyi-project/src/main/java/com/ruoyi/project/domain/DailyReportWhitelist.java
git commit -m "feat: 新增 DailyReportWhitelist 实体类"
```

---

## Task 3：新增 Mapper 接口和 XML

**文件**：
- 新建：`ruoyi-project/src/main/java/com/ruoyi/project/mapper/DailyReportWhitelistMapper.java`
- 新建：`ruoyi-project/src/main/resources/mapper/project/DailyReportWhitelistMapper.xml`

**Mapper 接口**：

```java
package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.DailyReportWhitelist;

public interface DailyReportWhitelistMapper {
    List<DailyReportWhitelist> selectWhitelistPage(DailyReportWhitelist query);
    int countByUserId(Long userId);
    int insertWhitelist(DailyReportWhitelist whitelist);
    int deleteWhitelistById(Long id);       // 软删除：update del_flag='1'
    int checkSelfInWhitelist(Long userId);  // 同 countByUserId，语义更清晰
}
```

**XML SQL**：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ruoyi.project.mapper.DailyReportWhitelistMapper">

    <resultMap type="DailyReportWhitelist" id="WhitelistResult">
        <id     property="id"          column="id"          />
        <result property="userId"      column="user_id"     />
        <result property="reason"      column="reason"      />
        <result property="delFlag"     column="del_flag"    />
        <result property="createBy"    column="create_by"   />
        <result property="createTime"  column="create_time" />
        <result property="nickName"    column="nick_name"   />
        <result property="deptName"    column="dept_name"   />
    </resultMap>

    <select id="selectWhitelistPage" parameterType="DailyReportWhitelist"
            resultMap="WhitelistResult">
        SELECT w.id, w.user_id, w.reason, w.del_flag,
               w.create_by, w.create_time,
               u.nick_name, d.dept_name
        FROM pm_daily_report_whitelist w
        INNER JOIN sys_user u ON w.user_id = u.user_id
        LEFT JOIN sys_dept d ON u.dept_id = d.dept_id
        WHERE w.del_flag = '0'
        <if test="keyword != null and keyword != ''">
            AND (u.nick_name LIKE CONCAT('%', #{keyword}, '%')
              OR d.dept_name LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        ORDER BY w.create_time DESC
    </select>

    <select id="countByUserId" parameterType="Long" resultType="int">
        SELECT COUNT(*) FROM pm_daily_report_whitelist
        WHERE user_id = #{userId} AND del_flag = '0'
    </select>

    <select id="checkSelfInWhitelist" parameterType="Long" resultType="int">
        SELECT COUNT(*) FROM pm_daily_report_whitelist
        WHERE user_id = #{userId} AND del_flag = '0'
    </select>

    <insert id="insertWhitelist" parameterType="DailyReportWhitelist"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO pm_daily_report_whitelist
            (user_id, reason, del_flag, create_by, create_time, update_by, update_time)
        VALUES
            (#{userId}, #{reason}, '0', #{createBy}, #{createTime}, #{updateBy}, #{updateTime})
    </insert>

    <update id="deleteWhitelistById" parameterType="Long">
        UPDATE pm_daily_report_whitelist
        SET del_flag = '1', update_by = #{_parameter}, update_time = NOW()
        WHERE id = #{id}
    </update>

</mapper>
```

**步骤**：
```bash
mvn clean compile -pl ruoyi-project -am
git add ruoyi-project/src/main/java/com/ruoyi/project/mapper/DailyReportWhitelistMapper.java
git add ruoyi-project/src/main/resources/mapper/project/DailyReportWhitelistMapper.xml
git commit -m "feat: DailyReportWhitelistMapper 接口和 SQL"
```

---

## Task 4：新增 Service 接口和实现

**文件**：
- 新建：`ruoyi-project/src/main/java/com/ruoyi/project/service/IDailyReportWhitelistService.java`
- 新建：`ruoyi-project/src/main/java/com/ruoyi/project/service/impl/DailyReportWhitelistServiceImpl.java`

**接口**：

```java
public interface IDailyReportWhitelistService {
    List<DailyReportWhitelist> selectWhitelistPage(DailyReportWhitelist query);
    int addToWhitelist(DailyReportWhitelist whitelist);
    int removeFromWhitelist(Long id);
    boolean isInWhitelist(Long userId);
}
```

**实现**：

```java
@Service
public class DailyReportWhitelistServiceImpl implements IDailyReportWhitelistService {

    @Autowired
    private DailyReportWhitelistMapper whitelistMapper;

    @Override
    public List<DailyReportWhitelist> selectWhitelistPage(DailyReportWhitelist query) {
        return whitelistMapper.selectWhitelistPage(query);
    }

    @Override
    public int addToWhitelist(DailyReportWhitelist whitelist) {
        // 防重复添加
        if (whitelistMapper.countByUserId(whitelist.getUserId()) > 0) {
            throw new ServiceException("该人员已在白名单中，请勿重复添加");
        }
        whitelist.setCreateBy(SecurityUtils.getUsername());
        whitelist.setCreateTime(DateUtils.getNowDate());
        return whitelistMapper.insertWhitelist(whitelist);
    }

    @Override
    public int removeFromWhitelist(Long id) {
        return whitelistMapper.deleteWhitelistById(id);
    }

    @Override
    public boolean isInWhitelist(Long userId) {
        return whitelistMapper.checkSelfInWhitelist(userId) > 0;
    }
}
```

**步骤**：
```bash
mvn clean compile -pl ruoyi-project -am
git add ruoyi-project/src/main/java/com/ruoyi/project/service/
git commit -m "feat: DailyReportWhitelistService 接口和实现"
```

---

## Task 5：新增 Controller

**文件**：
- 新建：`ruoyi-project/src/main/java/com/ruoyi/project/controller/DailyReportWhitelistController.java`

```java
@RestController
@RequestMapping("/project/whitelist")
public class DailyReportWhitelistController extends BaseController {

    @Autowired
    private IDailyReportWhitelistService whitelistService;

    /** 白名单列表（仅 admin） */
    @PreAuthorize("@ss.hasRole('admin')")
    @GetMapping("/list")
    public TableDataInfo list(DailyReportWhitelist whitelist) {
        startPage();
        return getDataTable(whitelistService.selectWhitelistPage(whitelist));
    }

    /** 添加白名单（仅 admin） */
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "日报白名单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody DailyReportWhitelist whitelist) {
        return toAjax(whitelistService.addToWhitelist(whitelist));
    }

    /** 移除白名单（仅 admin） */
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "日报白名单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(whitelistService.removeFromWhitelist(id));
    }

    /** 检查当前登录用户是否在白名单中（任意登录用户可调用） */
    @GetMapping("/checkSelf")
    public AjaxResult checkSelf() {
        Long userId = SecurityUtils.getUserId();
        return success(whitelistService.isInWhitelist(userId));
    }
}
```

**步骤**：
```bash
mvn clean compile -pl ruoyi-project -am
git add ruoyi-project/src/main/java/com/ruoyi/project/controller/DailyReportWhitelistController.java
git commit -m "feat: DailyReportWhitelistController 新增白名单 CRUD + checkSelf 端点"
```

---

## Task 6：修改 DailyReportMapper.xml 排除白名单用户

**文件**：
- 修改：`ruoyi-project/src/main/resources/mapper/project/DailyReportMapper.xml`

**步骤 1**：在 `selectActivityUsers` 的 `<where>` 块末尾追加排除子查询

```xml
<!-- 在原有 ${params.dataScope} 之后追加 -->
AND u.user_id NOT IN (
    SELECT user_id FROM pm_daily_report_whitelist WHERE del_flag = '0'
)
```

**步骤 2**：在 001 特性新增的 `selectTotalUserCount` 的 `<where>` 块末尾追加同样的排除子查询

**步骤 3**：在 001 特性新增的 `selectUnsubmittedUsersOnDate` 的 `<where>` 块末尾追加同样的排除子查询

> 注意：`selectSubmittedCountByDate` 不需要修改（只统计实际提交者，白名单人员因被禁止提交，自然不会出现在结果中）

**步骤 4**：
```bash
mvn clean compile -pl ruoyi-project -am
git add ruoyi-project/src/main/resources/mapper/project/DailyReportMapper.xml
git commit -m "feat: DailyReportMapper 排除白名单用户（activityUsers / totalCount / unsubmitted）"
```

---

## Task 7：DailyReportServiceImpl 日报保存时拦截白名单用户

**文件**：
- 修改：`ruoyi-project/src/main/java/com/ruoyi/project/service/impl/DailyReportServiceImpl.java`

**步骤 1**：注入 `IDailyReportWhitelistService`

```java
@Autowired
private IDailyReportWhitelistService whitelistService;
```

**步骤 2**：在 `saveDailyReport` 方法开头添加白名单校验

```java
@Override
@Transactional
public int saveDailyReport(DailyReport dailyReport) {
    Long userId = SecurityUtils.getUserId();
    // 白名单用户禁止提交日报
    if (whitelistService.isInWhitelist(userId)) {
        throw new ServiceException("您已被设置为无需填写日报，如有疑问请联系管理员");
    }
    // ... 原有逻辑不变
}
```

**步骤 3**：
```bash
mvn clean compile -pl ruoyi-project -am
git add ruoyi-project/src/main/java/com/ruoyi/project/service/impl/DailyReportServiceImpl.java
git commit -m "feat: 日报保存接口拦截白名单用户"
```

---

## Task 8：前端 API 文件

**文件**：
- 新建：`ruoyi-ui/src/api/project/whitelist.js`

```javascript
import request from '@/utils/request'

// 白名单列表（管理员）
export function listWhitelist(query) {
  return request({ url: '/project/whitelist/list', method: 'get', params: query })
}

// 添加白名单
export function addWhitelist(data) {
  return request({ url: '/project/whitelist', method: 'post', data })
}

// 移除白名单
export function removeWhitelist(id) {
  return request({ url: '/project/whitelist/' + id, method: 'delete' })
}

// 检查当前用户是否在白名单中
export function checkSelfInWhitelist() {
  return request({ url: '/project/whitelist/checkSelf', method: 'get' })
}
```

**步骤**：
```bash
git add ruoyi-ui/src/api/project/whitelist.js
git commit -m "feat: 白名单 API 函数"
```

---

## Task 9：前端白名单管理页面

**文件**：
- 新建：`ruoyi-ui/src/views/system/whitelist/index.vue`

**页面结构**：
- 搜索栏：关键词输入框（姓名/部门）+ 查询/重置按钮 + 右侧"添加"按钮
- 表格：姓名、部门、加入原因、添加时间、操作人、操作列（移除）
- 添加弹框：`<user-select>` 人员选择器 + 原因文本框（必填）
- 移除：点击"移除"弹出二次确认对话框

**关键代码片段**：

```typescript
// 添加白名单
async function handleAdd() {
  const valid = await formRef.value.validate()
  if (!valid) return
  await addWhitelist({ userId: form.userId, reason: form.reason })
  ElMessage.success('添加成功')
  dialogVisible.value = false
  getList()
}

// 移除白名单
function handleRemove(row) {
  ElMessageBox.confirm(`确定要将 ${row.nickName} 从白名单中移除吗？`, '确认移除', {
    type: 'warning'
  }).then(async () => {
    await removeWhitelist(row.id)
    ElMessage.success('移除成功')
    getList()
  })
}
```

**步骤**：
```bash
git add ruoyi-ui/src/views/system/whitelist/index.vue
git commit -m "feat: 日报白名单管理页面"
```

---

## Task 10：修改 write.vue 加白名单拦截

**文件**：
- 修改：`ruoyi-ui/src/views/project/dailyReport/write.vue`

**步骤 1**：在 `onMounted` 中调用 `checkSelfInWhitelist()`，若为 true 则设置 `isWhitelisted = true`

**步骤 2**：在页面顶部添加提示卡片

```vue
<el-alert
  v-if="isWhitelisted"
  title="您已被设置为无需填写日报"
  description="如有疑问，请联系系统管理员。"
  type="info"
  show-icon
  :closable="false"
  style="margin-bottom: 16px;"
/>
```

**步骤 3**：在右侧日报填写区域加 `v-if="!isWhitelisted"` 控制显示

**步骤 4**：
```bash
git add ruoyi-ui/src/views/project/dailyReport/write.vue
git commit -m "feat: write.vue 白名单用户拦截提示"
```

---

## Task 11：菜单 SQL

**文件**：
- 修改：`pm-sql/init/02_menu_data.sql`（在系统管理菜单段末尾追加）

```sql
-- ---- 日报白名单 ----
-- 在系统管理目录下新增
SET @sysManageMenuId = (SELECT menu_id FROM sys_menu WHERE menu_name = '系统管理' AND parent_id = 0 LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
VALUES ('日报白名单', @sysManageMenuId, 99, 'whitelist', 'system/whitelist/index', 1, 0, 'C', '0', '0', 'project:whitelist:list', 'peoples', 'admin', sysdate(), '', NULL, '日报填写白名单管理', 'DailyReportWhitelist');
SELECT @whitelistMenuId := LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('查询', @whitelistMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'project:whitelist:list', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('添加', @whitelistMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'project:whitelist:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('移除', @whitelistMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'project:whitelist:remove', '#', 'admin', sysdate(), '', NULL, '');
```

**步骤**：
```bash
git add pm-sql/init/02_menu_data.sql
git commit -m "feat: 日报白名单菜单数据"
```

---

## Task 12：集成验证

**步骤 1**：后端打包
```bash
mvn clean package -pl ruoyi-admin -am -Dmaven.test.skip=true
```

**步骤 2**：验证白名单 CRUD
```bash
# 添加白名单
curl -X POST "http://localhost:8080/project/whitelist" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"userId": 100, "reason": "长期外派"}'

# 检查白名单
curl "http://localhost:8080/project/whitelist/checkSelf" -H "Authorization: Bearer {token}"
```

**步骤 3**：用被加入白名单的账号登录，验证：
- 日报填写页显示提示、表单禁用
- 直接调用日报保存 API 被拒绝

**步骤 4**：验证日报动态和统计报表中白名单用户不出现在未填写名单

**步骤 5**：在 K3s 执行 fix SQL
```bash
cat pm-sql/fix_whitelist_20260317.sql | ssh k3s001 "kubectl exec -i mysql-0 -n newpm -- mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue"
```

**步骤 6**：推送分支，合并到 main
```bash
git push origin 002-daily-report-whitelist
```
