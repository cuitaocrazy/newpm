# 项目列表添加合同按钮设计方案

**日期：** 2026-02-13
**状态：** 已批准（2026-02-13 检查后更新）
**作者：** Claude Code

## 一、需求概述

在项目管理列表的操作列添加合同相关按钮，实现从项目快速创建或查看合同的功能。

### 核心需求

- 项目列表操作列添加动态按钮
- 无合同时显示"添加合同"按钮，跳转到合同新增页面
- 有合同时显示"查看合同"按钮，跳转到合同详情页面
- 从项目添加合同时，自动关联当前项目
- 业务规则：**一个项目只能关联一个合同**
- 项目详情页展示关联的合同信息

## 二、技术方案

### 2.1 整体架构

**涉及模块：**
- 后端：项目列表查询、合同保存逻辑
- 前端：项目列表页、合同新增页

**技术实现：**
- 使用 Vue Router 的 `query` 参数传递 projectId
- 使用 MyBatis LEFT JOIN 查询项目关联的合同ID
- 后端添加唯一性检查，防止重复关联

### 2.2 后端实现

#### 2.2.1 项目列表接口改动

**文件：** `Project.java`

```java
public class Project extends BaseEntity {
    // ... 其他字段

    /** 关联的合同ID */
    private Long contractId;

    // getter/setter
}
```

**文件：** `ProjectMapper.xml`

修改 `selectProjectList` 查询，添加 LEFT JOIN：

```xml
<select id="selectProjectList" resultMap="ProjectResult">
    SELECT
        p.*,
        pcr.contract_id as contractId
    FROM pm_project p
    LEFT JOIN pm_project_contract_rel pcr ON p.project_id = pcr.project_id
    <where>
        p.del_flag = '0'
        <if test="projectName != null and projectName != ''">
            AND p.project_name like concat('%', #{projectName}, '%')
        </if>
        <!-- 其他查询条件 -->
    </where>
    ORDER BY p.create_time DESC
</select>
```

**性能说明：** 使用 LEFT JOIN 一次查询获取所有数据，避免 N+1 查询问题。

#### 2.2.2 合同保存逻辑改动

**文件：** `ContractMapper.java`

添加查询方法：

```java
/**
 * 根据项目ID查询关联的合同ID
 * @param projectId 项目ID
 * @return 合同ID
 */
Long selectContractIdByProjectId(Long projectId);
```

**文件：** `ContractMapper.xml`

```xml
<select id="selectContractIdByProjectId" resultType="Long">
    SELECT contract_id
    FROM pm_project_contract_rel
    WHERE project_id = #{projectId}
    LIMIT 1
</select>
```

**文件：** `ContractServiceImpl.java`

修改 `insertContract()` 方法，添加唯一性检查：

```java
@Transactional
public int insertContract(Contract contract) {
    // 1. 检查项目是否已有合同
    if (contract.getProjectIds() != null && !contract.getProjectIds().isEmpty()) {
        for (Long projectId : contract.getProjectIds()) {
            Long existingContractId = contractMapper.selectContractIdByProjectId(projectId);
            if (existingContractId != null) {
                throw new ServiceException("项目已关联合同，无法重复添加");
            }
        }
    }

    // 2. 插入合同主记录
    int rows = contractMapper.insertContract(contract);

    // 3. 插入项目-合同关联关系
    if (contract.getProjectIds() != null && !contract.getProjectIds().isEmpty()) {
        insertProjectContractRel(contract);
    }

    return rows;
}

/**
 * 插入项目-合同关联关系
 */
private void insertProjectContractRel(Contract contract) {
    for (Long projectId : contract.getProjectIds()) {
        ProjectContractRel rel = new ProjectContractRel();
        rel.setProjectId(projectId);
        rel.setContractId(contract.getContractId());
        projectContractRelMapper.insert(rel);
    }
}
```

### 2.3 前端实现

#### 2.3.1 项目列表页改动

**文件：** `ruoyi-ui/src/views/project/project/index.vue`

**操作列添加动态按钮：**

```vue
<el-table-column label="操作" align="center" class-name="small-padding fixed-width">
  <template #default="scope">
    <el-button link type="primary" @click="handleDetail(scope.row)">
      查看
    </el-button>

    <!-- 动态显示：有合同显示"查看合同"，无合同显示"添加合同" -->
    <el-button
      v-if="scope.row.contractId"
      link
      type="primary"
      @click="handleViewContract(scope.row)"
    >
      查看合同
    </el-button>
    <el-button
      v-else
      link
      type="primary"
      @click="handleAddContract(scope.row)"
    >
      添加合同
    </el-button>

    <el-button link type="primary" @click="handleUpdate(scope.row)" v-hasPermi="['project:project:edit']">
      修改
    </el-button>
    <!-- 其他按钮... -->
  </template>
</el-table-column>
```

**添加跳转方法：**

```typescript
/** 添加合同 */
function handleAddContract(row: Project) {
  router.push({
    path: '/htkx/contract/add',  // 合同款项模块路径
    query: { projectId: row.projectId }
  })
}

/** 查看合同 */
function handleViewContract(row: Project) {
  router.push({
    path: `/htkx/contract/detail/${row.contractId}`  // 合同详情页路径
  })
}
```

**说明：**
- URL路径：`/htkx/contract/*` （htkx = "合同款项"拼音缩写）
- 组件路径：`project/contract/*` （实际Vue文件位置）
- RuoYi框架中URL路径和组件文件路径是分离的

**类型定义补充：**

```typescript
interface Project {
  projectId: number
  projectName: string
  projectCode: string
  contractId?: number  // 关联的合同ID
  // ... 其他字段
}
```

#### 2.3.2 合同新增页改动

**文件：** `ruoyi-ui/src/views/project/contract/add.vue`

**重要说明：** 当前合同新增页支持**多项目关联**（`projectIds` 数组 + `multiple` 选择器），需要改造为从项目进入时的**单项目模式**。

**添加路由参数监听：**

```typescript
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getProject } from '@/api/project/project'

const route = useRoute()
const projectInfo = ref<any>(null)  // 从项目进入时的项目信息
const form = ref({
  // ... 其他字段
  projectIds: []  // 关联的项目ID数组
})

/** 初始化 */
onMounted(() => {
  const projectId = route.query.projectId
  if (projectId) {
    loadProjectInfo(projectId as string)
  }
})

/** 加载项目信息 */
async function loadProjectInfo(projectId: string) {
  try {
    const response = await getProject(projectId)
    if (!response.data) {
      proxy.$modal.msgWarning('项目不存在')
      router.push('/htkx/contract')  // 返回合同列表
      return
    }
    projectInfo.value = response.data
    // 自动填充项目ID（单个）
    form.value.projectIds = [Number(projectId)]
  } catch (error) {
    proxy.$modal.msgError('加载项目信息失败')
    console.error(error)
  }
}
```

**模板添加项目信息提示：**

```vue
<!-- 从项目添加时，显示项目信息提示 -->
<el-alert
  v-if="projectInfo"
  type="info"
  :closable="false"
  style="margin-bottom: 16px"
>
  <template #title>
    将为项目创建合同：{{ projectInfo.projectName }} ({{ projectInfo.projectCode }})
  </template>
</el-alert>

<!-- 原有的项目选择器：从项目进入时隐藏 -->
<el-form-item label="关联项目" v-if="!projectInfo">
  <!-- 保持原有的多选功能（从合同列表直接新增时使用） -->
  <el-select
    v-model="form.projectIds"
    placeholder="请先选择部门，然后选择项目（可多选）"
    multiple
    filterable
    clearable
    :disabled="!form.deptId"
    style="width: 100%"
  >
    <el-option
      v-for="project in projectOptions"
      :key="project.projectId"
      :label="project.projectName"
      :value="project.projectId"
    />
  </el-select>
</el-form-item>
```

**改造说明：**
- **从项目列表进入**：projectInfo 有值，显示提示框，隐藏选择器，自动填充单个项目ID
- **从合同列表新增**：projectInfo 为 null，显示选择器，支持多项目关联
- 保持向后兼容：不影响原有的从合同列表直接新增的功能

#### 2.3.3 项目详情页添加合同信息

**文件：** `ruoyi-ui/src/views/project/project/detail.vue`

**新增功能：** 在项目详情页添加"关联合同"信息卡片

**实现方案：**

```vue
<!-- 关联合同信息卡片 -->
<el-card shadow="never" class="detail-card" v-if="contractInfo.contractId">
  <template #header>
    <div class="card-header">
      <span class="card-title">关联合同</span>
      <el-button
        link
        type="primary"
        @click="viewContract"
      >
        查看详情
      </el-button>
    </div>
  </template>
  <el-descriptions :column="3" border>
    <el-descriptions-item label="合同名称" :span="2">
      {{ contractInfo.contractName || '-' }}
    </el-descriptions-item>
    <el-descriptions-item label="合同编号">
      {{ contractInfo.contractCode || '-' }}
    </el-descriptions-item>
    <el-descriptions-item label="合同金额">
      {{ contractInfo.contractAmount || 0 }} 元
    </el-descriptions-item>
    <el-descriptions-item label="已收金额">
      {{ contractInfo.receivedAmount || 0 }} 元
    </el-descriptions-item>
    <el-descriptions-item label="合同状态">
      <dict-tag :options="sys_htzt" :value="contractInfo.contractStatus" />
    </el-descriptions-item>
  </el-descriptions>
</el-card>

<!-- 无合同时的提示 -->
<el-card shadow="never" class="detail-card" v-else>
  <template #header>
    <span class="card-title">关联合同</span>
  </template>
  <el-empty description="该项目暂无关联合同" :image-size="80">
    <el-button type="primary" @click="addContract">
      添加合同
    </el-button>
  </el-empty>
</el-card>
```

```typescript
// 数据定义
const contractInfo = ref({
  contractId: null,
  contractName: '',
  contractCode: '',
  contractAmount: 0,
  receivedAmount: 0,
  contractStatus: ''
})

// 加载合同信息
async function loadContractInfo(projectId: number) {
  try {
    // 调用接口查询项目关联的合同
    const response = await request({
      url: `/project/project/${projectId}/contract`,
      method: 'get'
    })
    if (response.data) {
      contractInfo.value = response.data
    }
  } catch (error) {
    console.error('加载合同信息失败', error)
  }
}

// 查看合同详情
function viewContract() {
  router.push(`/htkx/contract/detail/${contractInfo.value.contractId}`)
}

// 添加合同
function addContract() {
  router.push({
    path: '/htkx/contract/add',
    query: { projectId: form.value.projectId }
  })
}

// 在loadProjectData中调用
onMounted(() => {
  const projectId = route.params.projectId
  if (projectId) {
    loadProjectData(projectId)
    loadContractInfo(projectId)  // 加载合同信息
  }
})
```

**后端支持：**

需要添加接口：`GET /project/project/{projectId}/contract`

```java
// ProjectController.java
@GetMapping("/{projectId}/contract")
public AjaxResult getContractByProjectId(@PathVariable Long projectId) {
    Contract contract = projectService.selectContractByProjectId(projectId);
    return success(contract);
}
```

## 三、边界情况处理

### 3.1 数据异常处理

| 场景 | 处理方式 |
|------|---------|
| 项目ID无效 | 显示错误提示，重定向回项目列表 |
| 合同ID不存在 | 显示404提示，提供返回按钮 |
| contractId查询失败 | 字段为null，按无合同处理 |
| 并发添加合同 | 后端检查唯一性，返回业务错误 |

### 3.2 权限控制

- 项目列表按钮不检查合同权限（所有能看到项目的用户都能点击）
- 合同新增页面本身有权限控制，无权限时后端拒绝保存
- 合同查看页面根据权限决定是否显示编辑按钮

### 3.3 数据一致性

- 合同保存事务包含：插入合同记录 + 插入关联关系
- 删除合同时需同步删除关联关系（需确认现有删除逻辑）
- 项目列表刷新后，按钮状态自动更新

## 四、测试方案

### 4.1 功能测试

**项目列表页：**
- [ ] 无合同的项目显示"添加合同"按钮
- [ ] 有合同的项目显示"查看合同"按钮
- [ ] 点击"添加合同"跳转正确，URL包含projectId
- [ ] 点击"查看合同"跳转正确，显示对应合同

**合同新增页：**
- [ ] 从项目进入时，显示项目信息提示框
- [ ] 项目信息自动填充，选择器隐藏
- [ ] 从合同列表直接新增，显示多选选择器
- [ ] 保存成功，关联关系正确创建
- [ ] 保存失败，显示错误，数据不丢失

**项目详情页：**
- [ ] 有合同时，显示合同信息卡片
- [ ] 合同信息展示完整（名称、编号、金额、状态）
- [ ] "查看详情"按钮跳转到合同详情页
- [ ] 无合同时，显示空状态提示
- [ ] 点击"添加合同"按钮跳转正确

**边界情况：**
- [ ] 无效projectId显示错误提示
- [ ] 已有合同再添加，后端返回错误
- [ ] 并发添加，只有一个成功
- [ ] 删除关联后，按钮状态正确更新

### 4.2 性能测试

- [ ] 项目列表查询时间（1000条数据）< 500ms
- [ ] LEFT JOIN不产生笛卡尔积
- [ ] 并发保存不产生死锁

### 4.3 兼容性测试

- [ ] Chrome/Edge/Safari浏览器正常
- [ ] 移动端响应式布局正常
- [ ] 深色模式显示正常

## 五、实施计划

### Phase 1: 后端实现（优先级：高）

1. **项目实体和Mapper改动**
   - 添加 `contractId` 字段
   - 修改 `selectProjectList` 添加 LEFT JOIN
   - 预计耗时：30分钟

2. **合同保存逻辑改动**
   - 添加 `selectContractIdByProjectId` 方法
   - 修改 `insertContract` 添加唯一性检查
   - 预计耗时：45分钟

3. **后端测试**
   - 单元测试：唯一性检查逻辑
   - 集成测试：项目列表接口返回contractId
   - 预计耗时：30分钟

### Phase 2: 前端实现（优先级：高）

1. **项目列表页改动**
   - 操作列添加动态按钮
   - 添加跳转方法
   - 类型定义补充
   - 预计耗时：30分钟

2. **合同新增页改动**
   - 路由参数监听
   - 项目信息加载和展示
   - 条件渲染项目选择器
   - 错误处理
   - 预计耗时：1小时

3. **项目详情页改动**
   - 添加合同信息卡片
   - 加载合同数据
   - 添加跳转按钮
   - 空状态处理
   - 预计耗时：45分钟

4. **后端接口补充**
   - 添加 `/project/{projectId}/contract` 查询接口
   - 预计耗时：20分钟

5. **前端测试**
   - 功能测试：按钮显示和跳转
   - 项目详情页合同展示
   - UI测试：样式和响应式
   - 预计耗时：45分钟

### Phase 3: 联调和验收（优先级：中）

1. **功能联调**
   - 完整流程测试
   - 边界情况测试
   - 预计耗时：1小时

2. **性能测试**
   - 项目列表查询性能
   - 并发保存测试
   - 预计耗时：30分钟

3. **用户验收**
   - 演示核心功能
   - 收集反馈
   - 预计耗时：30分钟

### 总预计耗时

- **开发时间：** 4.5小时
  - 后端：1.5小时
  - 前端：3小时
- **测试时间：** 2.5小时
- **总计：** 7小时

## 六、风险评估

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|---------|
| LEFT JOIN性能问题 | 中 | 低 | 添加索引，监控查询时间 |
| 并发问题导致数据不一致 | 高 | 低 | 唯一性检查+事务控制 |
| 合同路由路径不匹配 | 低 | 中 | 先确认实际路由配置 |
| 用户误删关联关系 | 中 | 低 | 删除合同时二次确认 |

## 七、后续优化

- 项目列表添加合同金额汇总统计
- 合同列表添加"关联项目"筛选条件
- 支持项目批量关联合同（如果未来业务需要）
- 添加项目-合同关联日志记录
- 项目详情页合同信息实时更新（WebSocket推送）

## 八、检查确认记录

**2026-02-13 代码检查结果：**

1. ✅ **合同页面路径确认**
   - 实际路径：`/htkx/contract/*` （htkx = 合同款项拼音缩写）
   - 组件文件：`ruoyi-ui/src/views/project/contract/*.vue`
   - RuoYi框架URL路径与组件路径分离，从菜单数据库配置

2. ✅ **合同删除逻辑确认**
   - 删除合同时同步删除关联关系（已实现）
   - 使用逻辑删除，数据不会真正删除

3. ✅ **ProjectSelect组件确认**
   - 组件已存在：`ruoyi-ui/src/components/ProjectSelect/index.vue`
   - 支持单选/多选、远程搜索、部门过滤

4. ⚠️ **合同新增页现状**
   - 当前支持多项目关联（`projectIds` 数组）
   - 需要改造：从项目进入时使用单项目模式
   - 保持向后兼容：从合同列表新增仍支持多选

5. ✅ **菜单权限配置**
   - 已在 `pm-sql/init/02_menu_data.sql` 中配置完整

6. ✅ **合同列表显示项目**
   - 已在合同列表中显示关联项目信息

7. ⚠️ **项目详情页合同信息**
   - 当前未实现，需要新增
   - 需添加后端接口：`GET /project/{projectId}/contract`

## 九、参考资料

- RuoYi-Vue官方文档：http://doc.ruoyi.vip
- 项目需求文档：`docs/pm/PM需求.md`
- 数据库设计：`pm-sql/init/00_tables_ddl.sql`
