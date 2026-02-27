# 日报模块实施方案 (我的日报 + 工作日报动态)

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现日报填写 (4.1) 和工作日报动态 (4.2) 两个功能模块，包含前后端完整 CRUD 和日历 UI。

**Architecture:** 主子表模式 (pm_daily_report → pm_daily_report_detail)，参照 Contract+Payment 模式。同时补齐 pm_project_member 表的 Java 代码并在项目新增/编辑时同步。前端"我的日报"为左日历+右编辑器布局，"工作日报动态"为团队概览/个人详情双模式日历。

**Tech Stack:** Java 17 / Spring Boot 3 / MyBatis / Vue 3 / TypeScript / Element Plus / el-calendar / el-slider

**UI 原型:** `docs/plans/mockup-activity-A-v3.html` (已确认)

---

## Task 1: 建表 — 执行 DDL

DDL 已在 `pm-sql/init/00_tables_ddl.sql` 底部 (pm_daily_report, pm_daily_report_detail, pm_project_member)。

**Step 1: 执行建表语句**

```bash
mysql -u root -p ry-vue < pm-sql/init/00_tables_ddl.sql
```

如果报 "table already exists" 错误，因 DDL 里有 `DROP TABLE IF EXISTS` 所以应正常；若其他表被重建导致数据丢失，可改为只执行最后三张表的 DDL。

**Step 2: 验证表已创建**

```bash
mysql -u root -p ry-vue -e "SHOW TABLES LIKE 'pm_daily%'; SHOW TABLES LIKE 'pm_project_member';"
```

Expected: 看到 `pm_daily_report`, `pm_daily_report_detail`, `pm_project_member` 三张表。

---

## Task 2: ProjectMember 后端代码 (Domain + Mapper + Service)

pm_project_member 表已有 DDL 但无 Java 代码。此 Task 补齐。

**Files:**
- Create: `ruoyi-project/src/main/java/com/ruoyi/project/domain/ProjectMember.java`
- Create: `ruoyi-project/src/main/java/com/ruoyi/project/mapper/ProjectMemberMapper.java`
- Create: `ruoyi-project/src/main/resources/mapper/project/ProjectMemberMapper.xml`

### Step 1: 创建 ProjectMember 实体

```java
package com.ruoyi.project.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

public class ProjectMember extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long memberId;
    private Long projectId;
    private Long userId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date joinDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date leaveDate;

    /** 是否在项目中(1是 0否) */
    private String isActive;
    private String delFlag;

    /** 关联字段 */
    private String userName;
    private String nickName;
    private String deptName;

    // getters and setters ...
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Date getJoinDate() { return joinDate; }
    public void setJoinDate(Date joinDate) { this.joinDate = joinDate; }
    public Date getLeaveDate() { return leaveDate; }
    public void setLeaveDate(Date leaveDate) { this.leaveDate = leaveDate; }
    public String getIsActive() { return isActive; }
    public void setIsActive(String isActive) { this.isActive = isActive; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
}
```

### Step 2: 创建 ProjectMemberMapper.java

```java
package com.ruoyi.project.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.domain.ProjectMember;

public interface ProjectMemberMapper
{
    /** 按项目ID查询活跃成员 */
    List<ProjectMember> selectMembersByProjectId(Long projectId);

    /** 按用户ID查询参与的项目ID列表（is_active='1'） */
    List<Long> selectProjectIdsByUserId(Long userId);

    /** 新增 */
    int insertProjectMember(ProjectMember member);

    /** 按项目ID删除所有成员（物理删除，用于同步） */
    int deleteByProjectId(Long projectId);

    /** 批量插入 */
    int batchInsert(List<ProjectMember> list);
}
```

### Step 3: 创建 ProjectMemberMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ruoyi.project.mapper.ProjectMemberMapper">

    <resultMap type="ProjectMember" id="ProjectMemberResult">
        <result property="memberId"   column="member_id" />
        <result property="projectId"  column="project_id" />
        <result property="userId"     column="user_id" />
        <result property="joinDate"   column="join_date" />
        <result property="leaveDate"  column="leave_date" />
        <result property="isActive"   column="is_active" />
        <result property="delFlag"    column="del_flag" />
        <result property="createBy"   column="create_by" />
        <result property="createTime" column="create_time" />
        <result property="updateBy"   column="update_by" />
        <result property="updateTime" column="update_time" />
        <result property="remark"     column="remark" />
        <result property="userName"   column="user_name" />
        <result property="nickName"   column="nick_name" />
        <result property="deptName"   column="dept_name" />
    </resultMap>

    <select id="selectMembersByProjectId" parameterType="Long" resultMap="ProjectMemberResult">
        select m.*, u.user_name, u.nick_name, d.dept_name
        from pm_project_member m
        left join sys_user u on m.user_id = u.user_id
        left join sys_dept d on u.dept_id = d.dept_id
        where m.project_id = #{projectId} and m.is_active = '1' and m.del_flag = '0'
        order by m.join_date
    </select>

    <select id="selectProjectIdsByUserId" parameterType="Long" resultType="Long">
        select project_id from pm_project_member
        where user_id = #{userId} and is_active = '1' and del_flag = '0'
    </select>

    <insert id="insertProjectMember" parameterType="ProjectMember" useGeneratedKeys="true" keyProperty="memberId">
        insert into pm_project_member (project_id, user_id, join_date, is_active, del_flag, create_by, create_time)
        values (#{projectId}, #{userId}, #{joinDate}, #{isActive}, '0', #{createBy}, sysdate())
    </insert>

    <delete id="deleteByProjectId" parameterType="Long">
        delete from pm_project_member where project_id = #{projectId}
    </delete>

    <insert id="batchInsert" parameterType="java.util.List">
        insert into pm_project_member (project_id, user_id, join_date, is_active, del_flag, create_by, create_time)
        values
        <foreach collection="list" item="item" separator=",">
            (#{item.projectId}, #{item.userId}, #{item.joinDate}, #{item.isActive}, '0', #{item.createBy}, sysdate())
        </foreach>
    </insert>

</mapper>
```

### Step 4: 编译验证

```bash
mvn clean compile -pl ruoyi-project -am
```

Expected: BUILD SUCCESS

### Step 5: Commit

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/domain/ProjectMember.java \
        ruoyi-project/src/main/java/com/ruoyi/project/mapper/ProjectMemberMapper.java \
        ruoyi-project/src/main/resources/mapper/project/ProjectMemberMapper.xml
git commit -m "feat(project): add ProjectMember domain, mapper, XML for pm_project_member table"
```

---

## Task 3: ProjectServiceImpl 同步 pm_project_member

项目新增/编辑参与人员时，同步写入 pm_project_member 表。

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectServiceImpl.java`
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/service/IProjectService.java` (如需添加接口方法)

### Step 1: 在 ProjectServiceImpl 中注入 ProjectMemberMapper 并添加同步方法

在 `ProjectServiceImpl.java` 中：

1. 添加 import：
```java
import com.ruoyi.project.mapper.ProjectMemberMapper;
import com.ruoyi.project.domain.ProjectMember;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Date;
```

2. 注入 mapper：
```java
@Autowired
private ProjectMemberMapper projectMemberMapper;
```

3. 添加私有方法 `syncProjectMembers`：
```java
/**
 * 同步项目成员表（根据 participants 字段 + 项目经理 + 市场经理 + 团队负责人）
 */
private void syncProjectMembers(Project project)
{
    Long projectId = project.getProjectId();
    // 先删除旧数据
    projectMemberMapper.deleteByProjectId(projectId);

    // 收集所有相关人员ID（去重）
    java.util.Set<Long> userIds = new java.util.LinkedHashSet<>();
    if (project.getProjectManagerId() != null) userIds.add(project.getProjectManagerId());
    if (project.getMarketManagerId() != null) userIds.add(project.getMarketManagerId());
    if (project.getTeamLeaderId() != null) userIds.add(project.getTeamLeaderId());

    // 解析逗号分隔的参与人员
    if (com.ruoyi.common.utils.StringUtils.isNotEmpty(project.getParticipants()))
    {
        for (String id : project.getParticipants().split(","))
        {
            String trimmed = id.trim();
            if (!trimmed.isEmpty())
            {
                try { userIds.add(Long.parseLong(trimmed)); }
                catch (NumberFormatException ignored) {}
            }
        }
    }

    if (userIds.isEmpty()) return;

    // 批量插入
    List<ProjectMember> members = new ArrayList<>();
    String username = SecurityUtils.getUsername();
    for (Long userId : userIds)
    {
        ProjectMember m = new ProjectMember();
        m.setProjectId(projectId);
        m.setUserId(userId);
        m.setJoinDate(new Date());
        m.setIsActive("1");
        m.setCreateBy(username);
        members.add(m);
    }
    projectMemberMapper.batchInsert(members);
}
```

4. 修改 `insertProject` 方法：
```java
@Override
@Transactional
public int insertProject(Project project)
{
    project.setCreateTime(DateUtils.getNowDate());
    int rows = projectMapper.insertProject(project);
    // 同步项目成员
    syncProjectMembers(project);
    return rows;
}
```

5. 修改 `updateProject` 方法：
```java
@Override
@Transactional
public int updateProject(Project project)
{
    project.setUpdateTime(DateUtils.getNowDate());
    int rows = projectMapper.updateProject(project);
    // 同步项目成员
    syncProjectMembers(project);
    return rows;
}
```

### Step 2: 确认 ProjectMapper insertProject 使用了 useGeneratedKeys

查看 `ProjectMapper.xml` 中的 `insertProject`，确认有 `useGeneratedKeys="true" keyProperty="projectId"`。如果没有，需要添加，否则 `project.getProjectId()` 在 insert 后为 null，无法同步成员。

### Step 3: 编译验证

```bash
mvn clean compile -pl ruoyi-project -am
```

### Step 4: Commit

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/service/impl/ProjectServiceImpl.java
git commit -m "feat(project): sync pm_project_member on project insert/update"
```

---

## Task 4: DailyReport + DailyReportDetail Domain 实体

**Files:**
- Create: `ruoyi-project/src/main/java/com/ruoyi/project/domain/DailyReport.java`
- Create: `ruoyi-project/src/main/java/com/ruoyi/project/domain/DailyReportDetail.java`

### Step 1: 创建 DailyReport.java（主表实体）

```java
package com.ruoyi.project.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

public class DailyReport extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long reportId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date reportDate;

    private Long userId;
    private Long deptId;
    private BigDecimal totalWorkHours;
    private String delFlag;

    /** 子表：日报明细列表 */
    private List<DailyReportDetail> detailList;

    /** 关联字段（非数据库） */
    private String userName;
    private String nickName;
    private String deptName;

    /** 查询条件用 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date reportDateStart;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date reportDateEnd;
    private String yearMonth;  // "2026-02" 用于日历月查询

    // getters and setters
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public Date getReportDate() { return reportDate; }
    public void setReportDate(Date reportDate) { this.reportDate = reportDate; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public BigDecimal getTotalWorkHours() { return totalWorkHours; }
    public void setTotalWorkHours(BigDecimal totalWorkHours) { this.totalWorkHours = totalWorkHours; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public List<DailyReportDetail> getDetailList() { return detailList; }
    public void setDetailList(List<DailyReportDetail> detailList) { this.detailList = detailList; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public Date getReportDateStart() { return reportDateStart; }
    public void setReportDateStart(Date reportDateStart) { this.reportDateStart = reportDateStart; }
    public Date getReportDateEnd() { return reportDateEnd; }
    public void setReportDateEnd(Date reportDateEnd) { this.reportDateEnd = reportDateEnd; }
    public String getYearMonth() { return yearMonth; }
    public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }
}
```

### Step 2: 创建 DailyReportDetail.java（子表实体）

```java
package com.ruoyi.project.domain;

import java.math.BigDecimal;
import com.ruoyi.common.core.domain.BaseEntity;

public class DailyReportDetail extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long detailId;
    private Long reportId;
    private Long projectId;
    private String projectStage;
    private BigDecimal workHours;
    private String workContent;
    private String delFlag;

    /** 关联字段（非数据库） */
    private String projectName;
    private String projectCode;
    private String projectStageName;  // 字典翻译后的阶段名称

    // getters and setters
    public Long getDetailId() { return detailId; }
    public void setDetailId(Long detailId) { this.detailId = detailId; }
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectStage() { return projectStage; }
    public void setProjectStage(String projectStage) { this.projectStage = projectStage; }
    public BigDecimal getWorkHours() { return workHours; }
    public void setWorkHours(BigDecimal workHours) { this.workHours = workHours; }
    public String getWorkContent() { return workContent; }
    public void setWorkContent(String workContent) { this.workContent = workContent; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
    public String getProjectStageName() { return projectStageName; }
    public void setProjectStageName(String projectStageName) { this.projectStageName = projectStageName; }
}
```

### Step 3: 编译验证

```bash
mvn clean compile -pl ruoyi-project -am
```

### Step 4: Commit

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/domain/DailyReport.java \
        ruoyi-project/src/main/java/com/ruoyi/project/domain/DailyReportDetail.java
git commit -m "feat(dailyReport): add DailyReport and DailyReportDetail domain entities"
```

---

## Task 5: DailyReport Mapper + XML

**Files:**
- Create: `ruoyi-project/src/main/java/com/ruoyi/project/mapper/DailyReportMapper.java`
- Create: `ruoyi-project/src/main/java/com/ruoyi/project/mapper/DailyReportDetailMapper.java`
- Create: `ruoyi-project/src/main/resources/mapper/project/DailyReportMapper.xml`
- Create: `ruoyi-project/src/main/resources/mapper/project/DailyReportDetailMapper.xml`

### Step 1: DailyReportMapper.java

```java
package com.ruoyi.project.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.domain.DailyReport;

public interface DailyReportMapper
{
    /** 按主键查询（含明细） */
    DailyReport selectDailyReportById(Long reportId);

    /** 按用户+日期查询（含明细） */
    DailyReport selectByUserAndDate(@Param("userId") Long userId, @Param("reportDate") String reportDate);

    /** 查询日报列表（不含明细，用于日历概览） */
    List<DailyReport> selectDailyReportList(DailyReport query);

    /** 查询某月所有日报（工作日报动态用，含明细） */
    List<DailyReport> selectMonthlyReports(DailyReport query);

    /** 新增日报主表 */
    int insertDailyReport(DailyReport report);

    /** 修改日报主表 */
    int updateDailyReport(DailyReport report);

    /** 删除 */
    int deleteDailyReportById(Long reportId);

    /** 批量删除 */
    int deleteDailyReportByIds(Long[] reportIds);
}
```

### Step 2: DailyReportDetailMapper.java

```java
package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.DailyReportDetail;

public interface DailyReportDetailMapper
{
    /** 按日报ID查询明细 */
    List<DailyReportDetail> selectByReportId(Long reportId);

    /** 新增 */
    int insertDetail(DailyReportDetail detail);

    /** 批量插入 */
    int batchInsert(List<DailyReportDetail> list);

    /** 按日报ID删除所有明细 */
    int deleteByReportId(Long reportId);

    /** 批量按日报ID删除 */
    int deleteByReportIds(Long[] reportIds);
}
```

### Step 3: DailyReportMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ruoyi.project.mapper.DailyReportMapper">

    <resultMap type="DailyReport" id="DailyReportResult">
        <result property="reportId"       column="report_id" />
        <result property="reportDate"     column="report_date" />
        <result property="userId"         column="user_id" />
        <result property="deptId"         column="dept_id" />
        <result property="totalWorkHours" column="total_work_hours" />
        <result property="delFlag"        column="del_flag" />
        <result property="createBy"       column="create_by" />
        <result property="createTime"     column="create_time" />
        <result property="updateBy"       column="update_by" />
        <result property="updateTime"     column="update_time" />
        <result property="remark"         column="remark" />
        <result property="nickName"       column="nick_name" />
        <result property="deptName"       column="dept_name" />
    </resultMap>

    <resultMap type="DailyReport" id="DailyReportWithDetailResult" extends="DailyReportResult">
        <collection property="detailList" ofType="DailyReportDetail">
            <result property="detailId"         column="detail_id" />
            <result property="reportId"         column="detail_report_id" />
            <result property="projectId"        column="detail_project_id" />
            <result property="projectStage"     column="detail_project_stage" />
            <result property="workHours"        column="detail_work_hours" />
            <result property="workContent"      column="detail_work_content" />
            <result property="createTime"       column="detail_create_time" />
            <result property="updateTime"       column="detail_update_time" />
            <result property="projectName"      column="detail_project_name" />
            <result property="projectCode"      column="detail_project_code" />
            <result property="projectStageName" column="detail_stage_name" />
        </collection>
    </resultMap>

    <sql id="selectBaseSql">
        select r.report_id, r.report_date, r.user_id, r.dept_id, r.total_work_hours,
               r.del_flag, r.create_by, r.create_time, r.update_by, r.update_time, r.remark,
               u.nick_name, d.dept_name
        from pm_daily_report r
        left join sys_user u on r.user_id = u.user_id
        left join sys_dept d on r.dept_id = d.dept_id
    </sql>

    <sql id="selectWithDetailSql">
        select r.report_id, r.report_date, r.user_id, r.dept_id, r.total_work_hours,
               r.del_flag, r.create_by, r.create_time, r.update_by, r.update_time, r.remark,
               u.nick_name, d.dept_name,
               dd.detail_id, dd.report_id as detail_report_id, dd.project_id as detail_project_id,
               dd.project_stage as detail_project_stage, dd.work_hours as detail_work_hours,
               dd.work_content as detail_work_content,
               dd.create_time as detail_create_time, dd.update_time as detail_update_time,
               p.project_name as detail_project_name, p.project_code as detail_project_code,
               dict.dict_label as detail_stage_name
        from pm_daily_report r
        left join sys_user u on r.user_id = u.user_id
        left join sys_dept d on r.dept_id = d.dept_id
        left join pm_daily_report_detail dd on r.report_id = dd.report_id and dd.del_flag = '0'
        left join pm_project p on dd.project_id = p.project_id
        left join sys_dict_data dict on dd.project_stage COLLATE utf8mb4_unicode_ci = dict.dict_value
            and dict.dict_type = 'sys_xmjd'
    </sql>

    <select id="selectDailyReportById" parameterType="Long" resultMap="DailyReportWithDetailResult">
        <include refid="selectWithDetailSql"/>
        where r.report_id = #{reportId} and r.del_flag = '0'
    </select>

    <select id="selectByUserAndDate" resultMap="DailyReportWithDetailResult">
        <include refid="selectWithDetailSql"/>
        where r.user_id = #{userId} and r.report_date = #{reportDate} and r.del_flag = '0'
    </select>

    <select id="selectDailyReportList" parameterType="DailyReport" resultMap="DailyReportResult">
        <include refid="selectBaseSql"/>
        <where>
            r.del_flag = '0'
            <if test="userId != null"> and r.user_id = #{userId}</if>
            <if test="deptId != null"> and r.dept_id = #{deptId}</if>
            <if test="reportDate != null"> and r.report_date = #{reportDate}</if>
            <if test="reportDateStart != null"> and r.report_date &gt;= #{reportDateStart}</if>
            <if test="reportDateEnd != null"> and r.report_date &lt;= #{reportDateEnd}</if>
            <if test="yearMonth != null and yearMonth != ''">
                and DATE_FORMAT(r.report_date, '%Y-%m') = #{yearMonth}
            </if>
            <if test="params.nickName != null and params.nickName != ''">
                and u.nick_name like concat('%', #{params.nickName}, '%')
            </if>
        </where>
        order by r.report_date desc, r.user_id
    </select>

    <select id="selectMonthlyReports" parameterType="DailyReport" resultMap="DailyReportWithDetailResult">
        <include refid="selectWithDetailSql"/>
        <where>
            r.del_flag = '0'
            <if test="userId != null"> and r.user_id = #{userId}</if>
            <if test="deptId != null"> and r.dept_id = #{deptId}</if>
            <if test="reportDateStart != null"> and r.report_date &gt;= #{reportDateStart}</if>
            <if test="reportDateEnd != null"> and r.report_date &lt;= #{reportDateEnd}</if>
            <if test="yearMonth != null and yearMonth != ''">
                and DATE_FORMAT(r.report_date, '%Y-%m') = #{yearMonth}
            </if>
            <if test="params.nickName != null and params.nickName != ''">
                and u.nick_name like concat('%', #{params.nickName}, '%')
            </if>
        </where>
        order by r.report_date desc, r.user_id
    </select>

    <insert id="insertDailyReport" parameterType="DailyReport" useGeneratedKeys="true" keyProperty="reportId">
        insert into pm_daily_report (report_date, user_id, dept_id, total_work_hours, del_flag,
            create_by, create_time, remark)
        values (#{reportDate}, #{userId}, #{deptId}, #{totalWorkHours}, '0',
            #{createBy}, sysdate(), #{remark})
    </insert>

    <update id="updateDailyReport" parameterType="DailyReport">
        update pm_daily_report
        <set>
            <if test="totalWorkHours != null">total_work_hours = #{totalWorkHours},</if>
            <if test="remark != null">remark = #{remark},</if>
            update_by = #{updateBy},
            update_time = sysdate()
        </set>
        where report_id = #{reportId}
    </update>

    <update id="deleteDailyReportById" parameterType="Long">
        update pm_daily_report set del_flag = '1' where report_id = #{reportId}
    </update>

    <update id="deleteDailyReportByIds" parameterType="Long">
        update pm_daily_report set del_flag = '1' where report_id in
        <foreach collection="array" item="id" open="(" separator="," close=")">#{id}</foreach>
    </update>

</mapper>
```

### Step 4: DailyReportDetailMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ruoyi.project.mapper.DailyReportDetailMapper">

    <resultMap type="DailyReportDetail" id="DetailResult">
        <result property="detailId"         column="detail_id" />
        <result property="reportId"         column="report_id" />
        <result property="projectId"        column="project_id" />
        <result property="projectStage"     column="project_stage" />
        <result property="workHours"        column="work_hours" />
        <result property="workContent"      column="work_content" />
        <result property="delFlag"          column="del_flag" />
        <result property="createBy"         column="create_by" />
        <result property="createTime"       column="create_time" />
        <result property="updateBy"         column="update_by" />
        <result property="updateTime"       column="update_time" />
        <result property="projectName"      column="project_name" />
        <result property="projectCode"      column="project_code" />
        <result property="projectStageName" column="stage_name" />
    </resultMap>

    <select id="selectByReportId" parameterType="Long" resultMap="DetailResult">
        select dd.*, p.project_name, p.project_code,
               dict.dict_label as stage_name
        from pm_daily_report_detail dd
        left join pm_project p on dd.project_id = p.project_id
        left join sys_dict_data dict on dd.project_stage COLLATE utf8mb4_unicode_ci = dict.dict_value
            and dict.dict_type = 'sys_xmjd'
        where dd.report_id = #{reportId} and dd.del_flag = '0'
        order by dd.detail_id
    </select>

    <insert id="insertDetail" parameterType="DailyReportDetail" useGeneratedKeys="true" keyProperty="detailId">
        insert into pm_daily_report_detail (report_id, project_id, project_stage, work_hours, work_content,
            del_flag, create_by, create_time)
        values (#{reportId}, #{projectId}, #{projectStage}, #{workHours}, #{workContent},
            '0', #{createBy}, sysdate())
    </insert>

    <insert id="batchInsert" parameterType="java.util.List">
        insert into pm_daily_report_detail (report_id, project_id, project_stage, work_hours, work_content,
            del_flag, create_by, create_time)
        values
        <foreach collection="list" item="item" separator=",">
            (#{item.reportId}, #{item.projectId}, #{item.projectStage}, #{item.workHours}, #{item.workContent},
             '0', #{item.createBy}, sysdate())
        </foreach>
    </insert>

    <delete id="deleteByReportId" parameterType="Long">
        delete from pm_daily_report_detail where report_id = #{reportId}
    </delete>

    <delete id="deleteByReportIds" parameterType="Long">
        delete from pm_daily_report_detail where report_id in
        <foreach collection="array" item="id" open="(" separator="," close=")">#{id}</foreach>
    </delete>

</mapper>
```

### Step 5: 编译验证 + Commit

```bash
mvn clean compile -pl ruoyi-project -am
git add ruoyi-project/src/main/java/com/ruoyi/project/mapper/DailyReportMapper.java \
        ruoyi-project/src/main/java/com/ruoyi/project/mapper/DailyReportDetailMapper.java \
        ruoyi-project/src/main/resources/mapper/project/DailyReportMapper.xml \
        ruoyi-project/src/main/resources/mapper/project/DailyReportDetailMapper.xml
git commit -m "feat(dailyReport): add DailyReport and Detail mapper interfaces and XML"
```

---

## Task 6: IDailyReportService + DailyReportServiceImpl

**Files:**
- Create: `ruoyi-project/src/main/java/com/ruoyi/project/service/IDailyReportService.java`
- Create: `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/DailyReportServiceImpl.java`

### Step 1: IDailyReportService.java

```java
package com.ruoyi.project.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.project.domain.DailyReport;

public interface IDailyReportService
{
    /** 按主键查询（含明细） */
    DailyReport selectDailyReportById(Long reportId);

    /** 按当前用户+日期查询（我的日报用） */
    DailyReport selectMyReportByDate(String reportDate);

    /** 查询日报列表（概览，不含明细） */
    List<DailyReport> selectDailyReportList(DailyReport query);

    /** 查询某月日报（工作日报动态用，含明细） */
    List<DailyReport> selectMonthlyReports(DailyReport query);

    /** 查询当前用户参与的项目列表（我的日报自动填充用） */
    List<Map<String, Object>> selectMyProjects();

    /** 保存日报（新增或修改，含明细） */
    int saveDailyReport(DailyReport report);

    /** 删除 */
    int deleteDailyReportByIds(Long[] reportIds);
}
```

### Step 2: DailyReportServiceImpl.java

```java
package com.ruoyi.project.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.domain.DailyReport;
import com.ruoyi.project.domain.DailyReportDetail;
import com.ruoyi.project.mapper.DailyReportMapper;
import com.ruoyi.project.mapper.DailyReportDetailMapper;
import com.ruoyi.project.mapper.ProjectMemberMapper;
import com.ruoyi.project.mapper.ProjectMapper;
import com.ruoyi.project.service.IDailyReportService;

@Service
public class DailyReportServiceImpl implements IDailyReportService
{
    @Autowired
    private DailyReportMapper dailyReportMapper;

    @Autowired
    private DailyReportDetailMapper detailMapper;

    @Autowired
    private ProjectMemberMapper projectMemberMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Override
    public DailyReport selectDailyReportById(Long reportId)
    {
        return dailyReportMapper.selectDailyReportById(reportId);
    }

    @Override
    public DailyReport selectMyReportByDate(String reportDate)
    {
        Long userId = SecurityUtils.getUserId();
        return dailyReportMapper.selectByUserAndDate(userId, reportDate);
    }

    @Override
    public List<DailyReport> selectDailyReportList(DailyReport query)
    {
        return dailyReportMapper.selectDailyReportList(query);
    }

    @Override
    public List<DailyReport> selectMonthlyReports(DailyReport query)
    {
        return dailyReportMapper.selectMonthlyReports(query);
    }

    /**
     * 查询当前用户参与的项目列表
     * 查询条件：project_manager_id / market_manager_id / team_leader_id = userId
     *           OR 在 pm_project_member 中 is_active='1'
     * 同时返回项目名称和当前阶段
     */
    @Override
    public List<Map<String, Object>> selectMyProjects()
    {
        Long userId = SecurityUtils.getUserId();
        return projectMapper.selectProjectsByUserId(userId);
    }

    /**
     * 保存日报（新增 or 修改）
     * 逻辑：
     *   1. 按 userId + reportDate 查是否已存在
     *   2. 存在 → 更新主表 + 删除旧明细 + 插入新明细
     *   3. 不存在 → 插入主表 + 插入明细
     *   4. totalWorkHours 由明细自动汇总
     */
    @Override
    @Transactional
    public int saveDailyReport(DailyReport report)
    {
        Long userId = SecurityUtils.getUserId();
        Long deptId = SecurityUtils.getDeptId();
        String username = SecurityUtils.getUsername();

        report.setUserId(userId);
        report.setDeptId(deptId);

        // 计算总工时
        BigDecimal total = BigDecimal.ZERO;
        List<DailyReportDetail> details = report.getDetailList();
        if (details != null)
        {
            for (DailyReportDetail d : details)
            {
                if (d.getWorkHours() != null) total = total.add(d.getWorkHours());
            }
        }
        report.setTotalWorkHours(total);

        // 格式化日期用于查重
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = sdf.format(report.getReportDate());

        // 查询是否已有该日日报
        DailyReport existing = dailyReportMapper.selectByUserAndDate(userId, dateStr);

        int rows;
        if (existing != null)
        {
            // 更新
            report.setReportId(existing.getReportId());
            report.setUpdateBy(username);
            rows = dailyReportMapper.updateDailyReport(report);

            // 删除旧明细（物理删除）
            detailMapper.deleteByReportId(existing.getReportId());
        }
        else
        {
            // 新增
            report.setCreateBy(username);
            rows = dailyReportMapper.insertDailyReport(report);
        }

        // 插入明细
        if (details != null && !details.isEmpty())
        {
            for (DailyReportDetail d : details)
            {
                d.setReportId(report.getReportId());
                d.setCreateBy(username);
            }
            detailMapper.batchInsert(details);
        }

        return rows;
    }

    @Override
    @Transactional
    public int deleteDailyReportByIds(Long[] reportIds)
    {
        // 先删明细
        detailMapper.deleteByReportIds(reportIds);
        // 逻辑删除主表
        return dailyReportMapper.deleteDailyReportByIds(reportIds);
    }
}
```

### Step 3: 在 ProjectMapper 中添加 selectProjectsByUserId 方法

在 `ProjectMapper.java` 接口中添加：

```java
/** 查询用户参与的项目列表（用于日报填写） */
List<Map<String, Object>> selectProjectsByUserId(Long userId);
```

在 `ProjectMapper.xml` 中添加 SQL（放在文件末尾的 `</mapper>` 之前）：

```xml
<select id="selectProjectsByUserId" parameterType="Long" resultType="java.util.HashMap">
    select distinct p.project_id as projectId,
           p.project_name as projectName,
           p.project_code as projectCode,
           p.project_stage as projectStage,
           dict.dict_label as projectStageName
    from pm_project p
    left join sys_dict_data dict on p.project_stage COLLATE utf8mb4_unicode_ci = dict.dict_value
        and dict.dict_type = 'sys_xmjd'
    where p.del_flag = '0'
      and p.approval_status = '1'
      and (
          p.project_manager_id = #{userId}
          or p.market_manager_id = #{userId}
          or p.team_leader_id = #{userId}
          or FIND_IN_SET(#{userId}, p.participants)
          or p.project_id in (
              select pm.project_id from pm_project_member pm
              where pm.user_id = #{userId} and pm.is_active = '1' and pm.del_flag = '0'
          )
      )
    order by p.project_name
</select>
```

### Step 4: 编译验证 + Commit

```bash
mvn clean compile -pl ruoyi-project -am
git add ruoyi-project/src/main/java/com/ruoyi/project/service/IDailyReportService.java \
        ruoyi-project/src/main/java/com/ruoyi/project/service/impl/DailyReportServiceImpl.java \
        ruoyi-project/src/main/java/com/ruoyi/project/mapper/ProjectMapper.java \
        ruoyi-project/src/main/resources/mapper/project/ProjectMapper.xml
git commit -m "feat(dailyReport): add IDailyReportService with save/query/myProjects logic"
```

---

## Task 7: DailyReportController

**Files:**
- Create: `ruoyi-project/src/main/java/com/ruoyi/project/controller/DailyReportController.java`

### Step 1: 创建 Controller

```java
package com.ruoyi.project.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.project.domain.DailyReport;
import com.ruoyi.project.service.IDailyReportService;

@RestController
@RequestMapping("/project/dailyReport")
public class DailyReportController extends BaseController
{
    @Autowired
    private IDailyReportService dailyReportService;

    /**
     * 查询日报列表（概览，用于日历显示）
     */
    @PreAuthorize("@ss.hasAnyPermi('project:dailyReport:list,project:dailyReport:activity')")
    @GetMapping("/list")
    public TableDataInfo list(DailyReport query)
    {
        startPage();
        List<DailyReport> list = dailyReportService.selectDailyReportList(query);
        return getDataTable(list);
    }

    /**
     * 查询某月全部日报（含明细，工作日报动态用）
     */
    @PreAuthorize("@ss.hasPermi('project:dailyReport:activity')")
    @GetMapping("/monthly")
    public AjaxResult monthly(DailyReport query)
    {
        List<DailyReport> list = dailyReportService.selectMonthlyReports(query);
        return success(list);
    }

    /**
     * 查询日报详情
     */
    @PreAuthorize("@ss.hasAnyPermi('project:dailyReport:query,project:dailyReport:activity')")
    @GetMapping("/{reportId}")
    public AjaxResult getInfo(@PathVariable Long reportId)
    {
        return success(dailyReportService.selectDailyReportById(reportId));
    }

    /**
     * 查询我的某日日报（含明细）
     */
    @PreAuthorize("@ss.hasPermi('project:dailyReport:list')")
    @GetMapping("/my/{reportDate}")
    public AjaxResult getMyReport(@PathVariable String reportDate)
    {
        return success(dailyReportService.selectMyReportByDate(reportDate));
    }

    /**
     * 查询当前用户参与的项目列表
     */
    @PreAuthorize("@ss.hasPermi('project:dailyReport:list')")
    @GetMapping("/myProjects")
    public AjaxResult myProjects()
    {
        List<Map<String, Object>> list = dailyReportService.selectMyProjects();
        return success(list);
    }

    /**
     * 保存日报（新增或修改）
     */
    @PreAuthorize("@ss.hasAnyPermi('project:dailyReport:add,project:dailyReport:edit')")
    @Log(title = "日报管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult save(@RequestBody DailyReport report)
    {
        return toAjax(dailyReportService.saveDailyReport(report));
    }

    /**
     * 删除日报
     */
    @PreAuthorize("@ss.hasPermi('project:dailyReport:remove')")
    @Log(title = "日报管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{reportIds}")
    public AjaxResult remove(@PathVariable Long[] reportIds)
    {
        return toAjax(dailyReportService.deleteDailyReportByIds(reportIds));
    }
}
```

### Step 2: 编译完整项目并启动验证

```bash
mvn clean package -Dmaven.test.skip=true
# 启动后端验证无报错
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

### Step 3: Commit

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/controller/DailyReportController.java
git commit -m "feat(dailyReport): add DailyReportController with CRUD and myProjects API"
```

---

## Task 8: 菜单 SQL

**Files:**
- Modify: `pm-sql/init/02_menu_data.sql` (追加到文件末尾)

### Step 1: 追加菜单 SQL

在 `pm-sql/init/02_menu_data.sql` 文件末尾追加：

```sql
-- ========================================
-- 日报管理模块菜单数据
-- ========================================

-- 一级菜单：日报管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
VALUES ('日报管理', 0, 5, 'dailyReport', NULL, 1, 0, 'M', '0', '0', '', 'date', 'admin', sysdate(), '', NULL, '日报管理目录', 'DailyReportRoot');

SELECT @dailyReportRootId := LAST_INSERT_ID();

-- 二级菜单：我的日报
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
VALUES ('我的日报', @dailyReportRootId, 1, 'write', 'project/dailyReport/write', 1, 0, 'C', '0', '0', 'project:dailyReport:list', 'edit', 'admin', sysdate(), '', NULL, '日报填写菜单', 'DailyReportWrite');

SELECT @dailyReportWriteId := LAST_INSERT_ID();

-- 我的日报 - 按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('日报查询', @dailyReportWriteId, 1, '#', '', 1, 0, 'F', '0', '0', 'project:dailyReport:query', '#', 'admin', sysdate(), '', NULL, '');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('日报新增', @dailyReportWriteId, 2, '#', '', 1, 0, 'F', '0', '0', 'project:dailyReport:add', '#', 'admin', sysdate(), '', NULL, '');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('日报修改', @dailyReportWriteId, 3, '#', '', 1, 0, 'F', '0', '0', 'project:dailyReport:edit', '#', 'admin', sysdate(), '', NULL, '');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('日报删除', @dailyReportWriteId, 4, '#', '', 1, 0, 'F', '0', '0', 'project:dailyReport:remove', '#', 'admin', sysdate(), '', NULL, '');

-- 二级菜单：工作日报动态
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
VALUES ('工作日报动态', @dailyReportRootId, 2, 'activity', 'project/dailyReport/activity', 1, 0, 'C', '0', '0', 'project:dailyReport:activity', 'peoples', 'admin', sysdate(), '', NULL, '工作日报动态菜单', 'DailyReportActivity');

SELECT @dailyReportActivityId := LAST_INSERT_ID();

-- 工作日报动态 - 按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('日报动态查询', @dailyReportActivityId, 1, '#', '', 1, 0, 'F', '0', '0', 'project:dailyReport:activity', '#', 'admin', sysdate(), '', NULL, '');
```

### Step 2: 导入数据库

**注意：** 不要重新执行整个 `02_menu_data.sql` 以免重复插入。只执行新追加的部分。可以将上面的 SQL 单独放到临时文件执行：

```bash
# 方式1：直接在 MySQL 命令行执行上面的 SQL 片段
# 方式2：创建临时文件执行
mysql -u root -p ry-vue < pm-sql/fix_daily_report_menu.sql
```

### Step 3: Commit

```bash
git add pm-sql/init/02_menu_data.sql
git commit -m "feat(dailyReport): add menu SQL for 我的日报 and 工作日报动态"
```

---

## Task 9: 前端 API 层

**Files:**
- Create: `ruoyi-ui/src/api/project/dailyReport.js`

### Step 1: 创建 API 文件

```javascript
import request from '@/utils/request'

// 查询日报列表（概览）
export function listDailyReport(query) {
  return request({
    url: '/project/dailyReport/list',
    method: 'get',
    params: query
  })
}

// 查询某月全部日报（含明细，工作日报动态用）
export function getMonthlyReports(query) {
  return request({
    url: '/project/dailyReport/monthly',
    method: 'get',
    params: query
  })
}

// 查询日报详情
export function getDailyReport(reportId) {
  return request({
    url: '/project/dailyReport/' + reportId,
    method: 'get'
  })
}

// 查询我的某日日报
export function getMyReport(reportDate) {
  return request({
    url: '/project/dailyReport/my/' + reportDate,
    method: 'get'
  })
}

// 查询当前用户参与的项目列表
export function getMyProjects() {
  return request({
    url: '/project/dailyReport/myProjects',
    method: 'get'
  })
}

// 保存日报（新增或修改）
export function saveDailyReport(data) {
  return request({
    url: '/project/dailyReport',
    method: 'post',
    data: data
  })
}

// 删除日报
export function delDailyReport(reportIds) {
  return request({
    url: '/project/dailyReport/' + reportIds,
    method: 'delete'
  })
}
```

### Step 2: Commit

```bash
git add ruoyi-ui/src/api/project/dailyReport.js
git commit -m "feat(dailyReport): add frontend API layer for daily report"
```

---

## Task 10: "我的日报" 前端页面

**Files:**
- Create: `ruoyi-ui/src/views/project/dailyReport/write.vue`

### 页面布局

左日历 + 右编辑区。

**左侧（30%宽度）：**
- el-calendar 组件，slot `#date-cell` 显示当日总工时和颜色标识
- 点击日期 → 右侧加载该日数据

**右侧（70%宽度）：**
- 日期标题 + 总工时汇总
- 项目列表（自动从 myProjects 接口获取）
- 每个项目一组：项目名+阶段（只读）、el-slider 工时、textarea 内容
- 只提交填写了内容的项目（workHours > 0 且 workContent 非空）
- 保存按钮

### Step 1: 创建 write.vue

```vue
<template>
  <div class="app-container daily-report-page">
    <el-row :gutter="16">
      <!-- 左侧日历 -->
      <el-col :span="7">
        <el-card shadow="hover">
          <el-calendar v-model="currentDate" @click="onDateChange">
            <template #date-cell="{ data }">
              <div class="cal-cell-inner" :class="getCellClass(data)">
                <span class="cal-day">{{ data.day.split('-')[2] - 0 }}</span>
                <span v-if="getDateHours(data.day)" class="cal-hours" :class="getHoursClass(data.day)">
                  {{ getDateHours(data.day) }}h
                </span>
              </div>
            </template>
          </el-calendar>
        </el-card>
      </el-col>

      <!-- 右侧编辑区 -->
      <el-col :span="17">
        <el-card shadow="hover">
          <template #header>
            <div style="display: flex; align-items: center; justify-content: space-between;">
              <div>
                <span style="font-size: 16px; font-weight: bold;">{{ formatDate(selectedDate) }}</span>
                <el-tag v-if="totalHours > 0" :type="totalHours >= 8 ? 'success' : 'warning'" style="margin-left: 12px;">
                  总工时: {{ totalHours }}h
                </el-tag>
              </div>
              <el-button type="primary" @click="handleSave" :loading="saving">保存日报</el-button>
            </div>
          </template>

          <div v-if="loading" style="text-align: center; padding: 40px;">
            <el-icon class="is-loading" :size="24"><Loading /></el-icon>
            <p style="color: #909399; margin-top: 8px;">加载中...</p>
          </div>

          <div v-else-if="projects.length === 0" style="text-align: center; padding: 60px; color: #909399;">
            <p>暂无参与的项目</p>
            <p style="font-size: 12px;">请联系项目经理将您添加为项目成员</p>
          </div>

          <div v-else class="project-list">
            <div v-for="(item, index) in formList" :key="item.projectId" class="project-item">
              <!-- 第一行：项目名 + 阶段 -->
              <div class="prj-header">
                <div class="prj-color-bar" :style="{ background: getColor(index) }"></div>
                <span class="prj-name">{{ item.projectName }}</span>
                <el-tag size="small" type="info">{{ item.projectStageName || '未设置' }}</el-tag>
              </div>

              <!-- 第二行：工时（slider + 输入框） -->
              <div class="prj-hours-row">
                <span class="hours-label">工时:</span>
                <el-slider
                  v-model="item.workHours"
                  :min="0" :max="12" :step="0.5"
                  :marks="{ 0: '0', 4: '4h', 8: '8h', 12: '12h' }"
                  style="flex: 1; margin: 0 16px;"
                  @change="updateTotal"
                />
                <el-input-number
                  v-model="item.workHours"
                  :min="0" :max="24" :step="0.5" :precision="1"
                  size="small" style="width: 100px;"
                  @change="updateTotal"
                />
                <span style="margin-left: 4px; color: #909399;">h</span>
              </div>

              <!-- 第三行：工作内容 -->
              <div class="prj-content-row">
                <el-input
                  v-model="item.workContent"
                  type="textarea"
                  :rows="2"
                  :placeholder="'填写 ' + item.projectName + ' 的工作内容...'"
                  maxlength="2000"
                  show-word-limit
                />
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { getMyReport, getMyProjects, saveDailyReport, listDailyReport } from '@/api/project/dailyReport'

const currentDate = ref(new Date())
const selectedDate = ref(formatDateStr(new Date()))
const projects = ref([])
const formList = ref([])
const loading = ref(false)
const saving = ref(false)
const monthReports = ref({}) // { 'yyyy-MM-dd': totalHours }

const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#b37feb', '#00b894', '#fdcb6e']

const totalHours = computed(() => {
  return formList.value.reduce((sum, item) => sum + (item.workHours || 0), 0)
})

function formatDateStr(date) {
  const d = new Date(date)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function formatDate(dateStr) {
  const d = new Date(dateStr)
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${dateStr} ${weekdays[d.getDay()]}`
}

function getColor(index) {
  return colors[index % colors.length]
}

// 加载参与的项目列表
async function loadProjects() {
  const res = await getMyProjects()
  projects.value = res.data || []
}

// 加载某日的日报数据
async function loadDayReport(dateStr) {
  loading.value = true
  try {
    const res = await getMyReport(dateStr)
    const report = res.data

    // 构建表单：所有项目列出，已有数据的填充
    formList.value = projects.value.map(p => {
      const detail = report?.detailList?.find(d => d.projectId === p.projectId)
      return {
        projectId: p.projectId,
        projectName: p.projectName,
        projectCode: p.projectCode,
        projectStage: p.projectStage,
        projectStageName: p.projectStageName,
        workHours: detail ? Number(detail.workHours) : 0,
        workContent: detail ? detail.workContent : ''
      }
    })
  } finally {
    loading.value = false
  }
}

// 加载月度概览（日历标注用）
async function loadMonthOverview() {
  const d = new Date(currentDate.value)
  const ym = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
  const res = await listDailyReport({ yearMonth: ym, userId: undefined })
  const map = {}
  if (res.rows) {
    res.rows.forEach(r => {
      const day = r.reportDate?.substring(0, 10)
      if (day) map[day] = Number(r.totalWorkHours)
    })
  }
  monthReports.value = map
}

function getDateHours(day) {
  return monthReports.value[day]
}

function getCellClass(data) {
  const hours = monthReports.value[data.day]
  if (!hours) return ''
  if (hours >= 8) return 'cell-ok'
  return 'cell-warn'
}

function getHoursClass(day) {
  const hours = monthReports.value[day]
  if (!hours) return ''
  if (hours > 8) return 'hours-over'
  if (hours >= 8) return 'hours-ok'
  return 'hours-warn'
}

function updateTotal() {
  // computed 自动更新
}

function onDateChange() {
  const newDate = formatDateStr(currentDate.value)
  if (newDate !== selectedDate.value) {
    selectedDate.value = newDate
    loadDayReport(newDate)
  }
}

watch(currentDate, (newVal, oldVal) => {
  const newDate = formatDateStr(newVal)
  if (newDate !== selectedDate.value) {
    selectedDate.value = newDate
    loadDayReport(newDate)
  }
  // 月份变化时重新加载月概览
  const newMonth = newVal.getMonth()
  const oldMonth = oldVal?.getMonth()
  if (newMonth !== oldMonth) {
    loadMonthOverview()
  }
})

async function handleSave() {
  // 过滤有效条目（工时>0 且内容非空）
  const details = formList.value
    .filter(f => f.workHours > 0 && f.workContent && f.workContent.trim())
    .map(f => ({
      projectId: f.projectId,
      projectStage: f.projectStage,
      workHours: f.workHours,
      workContent: f.workContent
    }))

  if (details.length === 0) {
    ElMessage.warning('请至少填写一个项目的工时和工作内容')
    return
  }

  saving.value = true
  try {
    await saveDailyReport({
      reportDate: selectedDate.value,
      detailList: details
    })
    ElMessage.success('日报保存成功')
    loadMonthOverview()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadProjects()
  await loadDayReport(selectedDate.value)
  await loadMonthOverview()
})
</script>

<style scoped>
.daily-report-page {
  height: calc(100vh - 84px);
}

/* 日历格子 */
.cal-cell-inner {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px;
  border-radius: 4px;
}
.cal-cell-inner.cell-ok { background: #f0f9eb; }
.cal-cell-inner.cell-warn { background: #fdf6ec; }
.cal-day { font-size: 14px; font-weight: 500; }
.cal-hours { font-size: 11px; font-weight: 700; margin-top: 2px; }
.cal-hours.hours-ok { color: #67c23a; }
.cal-hours.hours-warn { color: #e6a23c; }
.cal-hours.hours-over { color: #409eff; }

/* 项目列表 */
.project-list { display: flex; flex-direction: column; gap: 16px; }

.project-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 14px 16px;
  transition: box-shadow 0.2s;
}
.project-item:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.06); }

.prj-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.prj-color-bar { width: 4px; height: 20px; border-radius: 2px; }
.prj-name { font-size: 15px; font-weight: 600; }

.prj-hours-row {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}
.hours-label { font-size: 13px; color: #606266; white-space: nowrap; }

.prj-content-row {}
</style>
```

### Step 2: Commit

```bash
mkdir -p ruoyi-ui/src/views/project/dailyReport
git add ruoyi-ui/src/views/project/dailyReport/write.vue
git commit -m "feat(dailyReport): add 我的日报 page with left calendar + right editor"
```

---

## Task 11: "工作日报动态" 前端页面

**Files:**
- Create: `ruoyi-ui/src/views/project/dailyReport/activity.vue`

### 页面设计

参照 `docs/plans/mockup-activity-A-v3.html` (已确认)。两种模式切换：
- **团队概览模式**（未选人时）：日历格子显示所有人的姓名+工时，点击格子开抽屉看详情
- **个人详情模式**（选人后）：日历格子显示该人的项目详情（项目名+阶段+工时+更新时间）

### Step 1: 创建 activity.vue

```vue
<template>
  <div class="app-container">
    <!-- 查询栏 -->
    <el-form :inline="true" :model="queryParams" class="query-form">
      <el-form-item label="日报日期">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="~"
          start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD"
          style="width: 260px;" @change="handleQuery" />
      </el-form-item>
      <el-form-item label="姓名">
        <el-select v-model="queryParams.userId" filterable clearable
          placeholder="全部人员" style="width: 180px;" @change="handleQuery">
          <el-option label="全部人员（团队概览）" :value="null" />
          <el-option v-for="u in userList" :key="u.userId"
            :label="u.nickName" :value="u.userId">
            <span>{{ u.nickName }}</span>
            <span style="color: #909399; margin-left: 8px; font-size: 12px;">{{ u.deptName }}</span>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="项目部门">
        <el-select v-model="queryParams.deptId" filterable clearable
          placeholder="全部部门" style="width: 180px;" @change="handleQuery">
          <el-option v-for="d in deptList" :key="d.deptId"
            :label="d.deptName" :value="d.deptId" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 信息栏：团队模式 or 个人模式 -->
    <el-card v-if="!queryParams.userId" shadow="hover" class="info-bar">
      <div style="display: flex; align-items: center; gap: 16px;">
        <el-avatar :size="44" style="background: linear-gradient(135deg, #409eff, #66b1ff);">
          <el-icon :size="24"><User /></el-icon>
        </el-avatar>
        <div style="flex: 1;">
          <div style="font-size: 17px; font-weight: 700;">{{ currentDeptName || '全部部门' }}</div>
          <div style="font-size: 13px; color: #909399;">点击日历格子中的人员可查看个人详情</div>
        </div>
        <div style="display: flex; gap: 24px;">
          <div class="stat-box"><div class="stat-num">{{ teamStats.total }}</div><div class="stat-label">团队人数</div></div>
          <div class="stat-box"><div class="stat-num">{{ teamStats.reported }}</div><div class="stat-label">今日已填</div></div>
          <div class="stat-box"><div class="stat-num">{{ teamStats.unreported }}</div><div class="stat-label">今日未填</div></div>
        </div>
      </div>
    </el-card>

    <el-card v-else shadow="hover" class="info-bar">
      <div style="display: flex; align-items: center; gap: 16px;">
        <el-avatar :size="44" :style="{ background: selectedUserColor }">
          {{ selectedUserName?.charAt(0) }}
        </el-avatar>
        <div style="flex: 1;">
          <div style="font-size: 17px; font-weight: 700;">{{ selectedUserName }}</div>
          <div style="font-size: 13px; color: #909399;">{{ selectedUserDept }}</div>
        </div>
        <div style="display: flex; gap: 24px;">
          <div class="stat-box"><div class="stat-num">{{ personStats.days }}</div><div class="stat-label">已填报天</div></div>
          <div class="stat-box"><div class="stat-num">{{ personStats.totalHours }}h</div><div class="stat-label">月累计工时</div></div>
          <div class="stat-box"><div class="stat-num">{{ personStats.fullDays }}/{{ personStats.days }}</div><div class="stat-label">满勤(≥8h)</div></div>
        </div>
        <el-button text type="primary" @click="clearPerson">返回团队概览</el-button>
      </div>
    </el-card>

    <!-- 日历 -->
    <el-card shadow="hover">
      <el-calendar v-model="calendarDate">
        <template #date-cell="{ data }">
          <div class="activity-cell" @click="handleCellClick(data.day)">
            <div class="cell-top">
              <span class="cell-day">{{ data.day.split('-')[2] - 0 }}</span>
              <span v-if="getCellSummary(data.day)" class="cell-summary">
                {{ getCellSummary(data.day) }}
              </span>
            </div>
            <!-- 团队模式：显示人员列表 -->
            <div v-if="!queryParams.userId" class="cell-people">
              <div v-for="person in getCellPeople(data.day).slice(0, 4)" :key="person.userId"
                class="person-chip" :class="getChipClass(person.totalWorkHours)"
                @click.stop="selectPerson(person.userId)">
                <span class="chip-name">{{ person.nickName }}</span>
                <span class="chip-hours">{{ person.totalWorkHours }}h</span>
              </div>
              <div v-if="getCellPeople(data.day).length > 4" class="more-chip"
                @click.stop="openDrawer(data.day)">
                +{{ getCellPeople(data.day).length - 4 }}人
              </div>
            </div>
            <!-- 个人模式：显示项目详情 -->
            <div v-else class="cell-projects">
              <div v-for="detail in getCellDetails(data.day)" :key="detail.detailId" class="cell-prj">
                <div class="cell-prj-bar" :style="{ background: getProjectColor(detail.projectId) }"></div>
                <div class="cell-prj-info">
                  <div class="cell-prj-top">
                    <span class="cell-prj-name" :title="detail.projectName">{{ detail.projectName }}</span>
                    <el-tag size="small" type="info">{{ detail.projectStageName }}</el-tag>
                    <span class="cell-prj-h">{{ detail.workHours }}h</span>
                  </div>
                  <div class="cell-prj-time">更新 {{ formatTime(detail.updateTime) }}</div>
                </div>
              </div>
              <div v-if="getCellDetails(data.day).length === 0 && isWorkday(data.day)" class="cell-empty">
                未填报
              </div>
            </div>
          </div>
        </template>
      </el-calendar>

      <div class="legend">
        <span class="legend-item"><span class="legend-dot" style="background:#67c23a"></span> ≥8h</span>
        <span class="legend-item"><span class="legend-dot" style="background:#e6a23c"></span> &lt;8h</span>
        <span class="legend-item"><span class="legend-dot" style="background:#409eff"></span> &gt;8h</span>
      </div>
    </el-card>

    <!-- 抽屉（团队模式查看某天详情） -->
    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="560px">
      <div style="display: flex; gap: 24px; padding: 14px 16px; background: #f5f7fa; border-radius: 8px; margin-bottom: 16px;">
        <div class="stat-box"><div class="stat-num">{{ drawerStats.count }}</div><div class="stat-label">已提交</div></div>
        <div class="stat-box"><div class="stat-num">{{ drawerStats.totalHours }}h</div><div class="stat-label">总工时</div></div>
        <div class="stat-box"><div class="stat-num">{{ drawerStats.fullCount }}/{{ drawerStats.count }}</div><div class="stat-label">满勤(≥8h)</div></div>
      </div>
      <el-card v-for="person in drawerPeople" :key="person.userId" shadow="hover" style="margin-bottom: 12px;">
        <template #header>
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <div style="display: flex; align-items: center; gap: 10px;">
              <el-avatar :size="34" :style="{ background: getPersonColor(person.userId) }">
                {{ person.nickName?.charAt(0) }}
              </el-avatar>
              <div>
                <div style="font-weight: 600;">{{ person.nickName }}</div>
                <div style="font-size: 12px; color: #909399;">{{ person.deptName }}</div>
              </div>
            </div>
            <el-tag :type="person.totalWorkHours >= 8 ? 'success' : 'warning'" size="large" round>
              {{ person.totalWorkHours }}h
            </el-tag>
          </div>
        </template>
        <div v-for="detail in person.detailList" :key="detail.detailId" class="drawer-prj">
          <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 4px;">
            <el-tag size="small" type="primary">{{ detail.projectName }}</el-tag>
            <el-tag size="small" type="info">{{ detail.projectStageName }}</el-tag>
            <span style="margin-left: auto; font-weight: 700;">{{ detail.workHours }}h</span>
          </div>
          <div style="font-size: 13px; color: #606266; line-height: 1.6; padding-left: 8px; border-left: 2px solid #e4e7ed;">
            {{ detail.workContent }}
          </div>
          <div style="font-size: 11px; color: #c0c4cc; margin-top: 3px;">
            更新于 {{ formatTime(detail.updateTime) }}
          </div>
        </div>
      </el-card>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { User } from '@element-plus/icons-vue'
import { getMonthlyReports } from '@/api/project/dailyReport'
import request from '@/utils/request'

const calendarDate = ref(new Date())
const dateRange = ref([])
const queryParams = ref({ userId: null, deptId: null })
const reportData = ref([]) // 月度完整数据
const userList = ref([])
const deptList = ref([])
const drawerVisible = ref(false)
const drawerTitle = ref('')
const drawerPeople = ref([])
const drawerStats = ref({ count: 0, totalHours: 0, fullCount: 0 })

const projectColors = ['#409eff','#67c23a','#f56c6c','#e6a23c','#b37feb','#00b894','#fdcb6e','#909399']
const personColorMap = {}
let colorIndex = 0

function getPersonColor(userId) {
  if (!personColorMap[userId]) {
    personColorMap[userId] = projectColors[colorIndex % projectColors.length]
    colorIndex++
  }
  return personColorMap[userId]
}

function getProjectColor(projectId) {
  return projectColors[(projectId || 0) % projectColors.length]
}

const currentDeptName = computed(() => {
  if (!queryParams.value.deptId) return '全部部门'
  const d = deptList.value.find(x => x.deptId === queryParams.value.deptId)
  return d?.deptName || ''
})

const selectedUserName = computed(() => {
  const u = userList.value.find(x => x.userId === queryParams.value.userId)
  return u?.nickName || ''
})

const selectedUserDept = computed(() => {
  const u = userList.value.find(x => x.userId === queryParams.value.userId)
  return u?.deptName || ''
})

const selectedUserColor = computed(() => getPersonColor(queryParams.value.userId))

// 按日期分组数据
const dataByDate = computed(() => {
  const map = {}
  reportData.value.forEach(r => {
    const day = r.reportDate?.substring(0, 10)
    if (!day) return
    if (!map[day]) map[day] = []
    map[day].push(r)
  })
  return map
})

// 团队统计
const teamStats = computed(() => {
  const today = formatDateStr(new Date())
  const todayData = dataByDate.value[today] || []
  return {
    total: userList.value.length,
    reported: todayData.length,
    unreported: userList.value.length - todayData.length
  }
})

// 个人统计
const personStats = computed(() => {
  const userReports = reportData.value.filter(r => r.userId === queryParams.value.userId)
  const totalH = userReports.reduce((s, r) => s + Number(r.totalWorkHours || 0), 0)
  const fullDays = userReports.filter(r => Number(r.totalWorkHours) >= 8).length
  return {
    days: userReports.length,
    totalHours: totalH.toFixed(1),
    fullDays
  }
})

function formatDateStr(date) {
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  return timeStr.substring(11, 16)
}

function isWorkday(day) {
  const d = new Date(day)
  const dow = d.getDay()
  return dow !== 0 && dow !== 6
}

function getChipClass(hours) {
  if (hours > 8) return 'chip-over'
  if (hours >= 8) return 'chip-ok'
  return 'chip-warn'
}

function getCellSummary(day) {
  const people = dataByDate.value[day]
  if (!people || people.length === 0) return null
  if (!queryParams.value.userId) {
    const totalH = people.reduce((s, r) => s + Number(r.totalWorkHours), 0)
    return `${people.length}人 ${totalH.toFixed(1)}h`
  } else {
    const report = people.find(r => r.userId === queryParams.value.userId)
    if (!report) return null
    const h = Number(report.totalWorkHours)
    return `${h}h`
  }
}

function getCellPeople(day) {
  return dataByDate.value[day] || []
}

function getCellDetails(day) {
  const people = dataByDate.value[day] || []
  const report = people.find(r => r.userId === queryParams.value.userId)
  return report?.detailList || []
}

function selectPerson(userId) {
  queryParams.value.userId = userId
}

function clearPerson() {
  queryParams.value.userId = null
}

function openDrawer(day) {
  const people = dataByDate.value[day] || []
  if (people.length === 0) return
  const weekdays = ['周日','周一','周二','周三','周四','周五','周六']
  const d = new Date(day)
  drawerTitle.value = `${day} ${weekdays[d.getDay()]} 日报详情`
  drawerPeople.value = people
  const totalH = people.reduce((s, r) => s + Number(r.totalWorkHours), 0)
  const fullCount = people.filter(r => Number(r.totalWorkHours) >= 8).length
  drawerStats.value = { count: people.length, totalHours: totalH.toFixed(1), fullCount }
  drawerVisible.value = true
}

function handleCellClick(day) {
  if (!queryParams.value.userId) {
    openDrawer(day)
  }
}

async function loadData() {
  const d = new Date(calendarDate.value)
  const ym = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}`
  const params = { yearMonth: ym }
  if (queryParams.value.userId) params.userId = queryParams.value.userId
  if (queryParams.value.deptId) params.deptId = queryParams.value.deptId
  if (dateRange.value?.length === 2) {
    params.reportDateStart = dateRange.value[0]
    params.reportDateEnd = dateRange.value[1]
  }
  const res = await getMonthlyReports(params)
  reportData.value = res.data || []
}

async function loadUsers() {
  const res = await request({ url: '/system/user/list', method: 'get', params: { pageSize: 500 } })
  userList.value = (res.rows || []).map(u => ({
    userId: u.userId,
    nickName: u.nickName,
    deptName: u.dept?.deptName || ''
  }))
}

async function loadDepts() {
  const res = await request({ url: '/system/dept/list', method: 'get' })
  deptList.value = (res.data || []).filter(d => d.status === '0')
}

function handleQuery() { loadData() }

function handleReset() {
  queryParams.value = { userId: null, deptId: null }
  dateRange.value = []
  loadData()
}

watch(calendarDate, () => loadData())

onMounted(() => {
  loadUsers()
  loadDepts()
  loadData()
})
</script>

<style scoped>
.info-bar { margin-bottom: 16px; }
.stat-box { text-align: center; padding: 0 16px; border-right: 1px solid #ebeef5; }
.stat-box:last-child { border-right: none; }
.stat-num { font-size: 22px; font-weight: 700; color: #409eff; }
.stat-label { font-size: 12px; color: #909399; margin-top: 2px; }

/* 日历格子 */
.activity-cell { min-height: 100px; cursor: pointer; }
.cell-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.cell-day { font-weight: 600; }
.cell-summary { font-size: 11px; color: #909399; }

/* 人员条 */
.person-chip {
  display: flex; justify-content: space-between; padding: 2px 6px;
  margin: 2px 0; border-radius: 4px; font-size: 11px; cursor: pointer;
}
.person-chip:hover { filter: brightness(0.95); }
.chip-ok { background: #f0f9eb; color: #67c23a; }
.chip-warn { background: #fdf6ec; color: #e6a23c; }
.chip-over { background: #ecf5ff; color: #409eff; }
.chip-name { font-weight: 500; }
.chip-hours { font-weight: 700; }
.more-chip { font-size: 10px; color: #909399; padding: 2px 6px; cursor: pointer; }
.more-chip:hover { color: #409eff; }

/* 项目条 */
.cell-prj { display: flex; gap: 4px; padding: 2px 0; }
.cell-prj-bar { width: 3px; border-radius: 1px; align-self: stretch; min-height: 24px; }
.cell-prj-info { flex: 1; min-width: 0; }
.cell-prj-top { display: flex; align-items: center; gap: 4px; }
.cell-prj-name { font-size: 12px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100px; }
.cell-prj-h { font-size: 11px; font-weight: 700; color: #409eff; margin-left: auto; }
.cell-prj-time { font-size: 10px; color: #c0c4cc; }
.cell-empty { font-size: 11px; color: #dcdfe6; text-align: center; margin-top: 16px; }

/* 图例 */
.legend { display: flex; gap: 16px; justify-content: flex-end; margin-top: 8px; }
.legend-item { font-size: 12px; color: #909399; display: flex; align-items: center; gap: 4px; }
.legend-dot { width: 10px; height: 10px; border-radius: 2px; }

/* 抽屉内项目 */
.drawer-prj { padding: 8px 0; border-bottom: 1px solid #f5f7fa; }
.drawer-prj:last-child { border-bottom: none; }
</style>
```

### Step 2: Commit

```bash
git add ruoyi-ui/src/views/project/dailyReport/activity.vue
git commit -m "feat(dailyReport): add 工作日报动态 page with team/personal calendar modes"
```

---

## Task 12: 构建 & 全流程验证

### Step 1: 后端编译打包

```bash
mvn clean package -Dmaven.test.skip=true
```

Expected: BUILD SUCCESS

### Step 2: 启动后端

```bash
java -Xms512m -Xmx1024m -jar ruoyi-admin/target/ruoyi-admin.jar
```

Expected: 启动无报错，日志中能看到 mapper 扫描到 DailyReportMapper 等

### Step 3: 前端启动

```bash
cd ruoyi-ui && npm run dev
```

### Step 4: 功能验证清单

1. **菜单是否出现：** 登录后左侧菜单出现"日报管理" → "我的日报" / "工作日报动态"
2. **我的日报 - 项目加载：** 打开页面，右侧应列出当前用户参与的项目
3. **我的日报 - 保存：** 填写工时和内容，点保存，刷新后数据仍在
4. **我的日报 - 日历标注：** 左侧日历已填报的日期显示工时和颜色
5. **工作日报动态 - 团队概览：** 不选人，日历格子显示每天填报的人员列表
6. **工作日报动态 - 点击人员切换：** 点击格子中的人名，切换到个人详情模式
7. **工作日报动态 - 个人详情：** 个人模式下日历格子显示项目名+阶段+工时
8. **工作日报动态 - 抽屉详情：** 团队模式点击格子，右侧抽屉展示当日所有人详情
9. **工作日报动态 - 返回团队：** 点"返回团队概览"或选"全部人员"能切回

### Step 5: 最终 Commit

```bash
git add -A
git commit -m "feat(dailyReport): complete 我的日报 and 工作日报动态 implementation"
```

---

## 文件清单汇总

### 新增文件 (12个)

| 文件 | 说明 |
|------|------|
| `ruoyi-project/.../domain/ProjectMember.java` | 项目成员实体 |
| `ruoyi-project/.../mapper/ProjectMemberMapper.java` | 项目成员 Mapper 接口 |
| `ruoyi-project/.../mapper/project/ProjectMemberMapper.xml` | 项目成员 SQL |
| `ruoyi-project/.../domain/DailyReport.java` | 日报主表实体 |
| `ruoyi-project/.../domain/DailyReportDetail.java` | 日报明细实体 |
| `ruoyi-project/.../mapper/DailyReportMapper.java` | 日报 Mapper 接口 |
| `ruoyi-project/.../mapper/DailyReportDetailMapper.java` | 日报明细 Mapper 接口 |
| `ruoyi-project/.../mapper/project/DailyReportMapper.xml` | 日报 SQL |
| `ruoyi-project/.../mapper/project/DailyReportDetailMapper.xml` | 日报明细 SQL |
| `ruoyi-project/.../service/IDailyReportService.java` | 日报 Service 接口 |
| `ruoyi-project/.../service/impl/DailyReportServiceImpl.java` | 日报 Service 实现 |
| `ruoyi-project/.../controller/DailyReportController.java` | 日报 Controller |
| `ruoyi-ui/src/api/project/dailyReport.js` | 前端 API 层 |
| `ruoyi-ui/src/views/project/dailyReport/write.vue` | 我的日报页面 |
| `ruoyi-ui/src/views/project/dailyReport/activity.vue` | 工作日报动态页面 |

### 修改文件 (4个)

| 文件 | 修改内容 |
|------|----------|
| `ruoyi-project/.../service/impl/ProjectServiceImpl.java` | 添加 syncProjectMembers 方法 |
| `ruoyi-project/.../mapper/ProjectMapper.java` | 添加 selectProjectsByUserId 方法 |
| `ruoyi-project/.../mapper/project/ProjectMapper.xml` | 添加 selectProjectsByUserId SQL |
| `pm-sql/init/02_menu_data.sql` | 追加日报菜单 SQL |
