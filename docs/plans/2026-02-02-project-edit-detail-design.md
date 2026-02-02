# 项目编辑和详情页面设计文档

## 概述

为项目管理模块创建独立的编辑页面和详情页面，支持完整的项目信息查看和修改。

## 页面结构

### 文件位置
- **编辑页面**：`ruoyi-ui/src/views/project/project/edit.vue`
- **详情页面**：`ruoyi-ui/src/views/project/project/detail.vue`

### 路由配置（动态路由）

```javascript
{
  path: 'edit/:projectId(\\d+)',
  component: () => import('@/views/project/project/edit.vue'),
  name: 'ProjectEdit',
  meta: { title: '编辑项目', activeMenu: '/project/project' },
  hidden: true
}

{
  path: 'detail/:projectId(\\d+)',
  component: () => import('@/views/project/project/detail.vue'),
  name: 'ProjectDetail',
  meta: { title: '项目详情', activeMenu: '/project/project' },
  hidden: true
}
```

### 访问方式
1. 从列表页跳转：`router.push({ name: 'ProjectEdit', params: { projectId: row.projectId } })`
2. 直接 URL 访问：`http://localhost/project/project/edit/123`

## 编辑页面设计

### 数据加载策略
- 页面 `onMounted` 时根据路由参数 `projectId` 自动调用 `getProject(projectId)` 获取数据
- 加载后自动填充表单

### 字段转换规则

#### 1. 参与人员（participants）
- **数据库存储**：`"1,2,3"` (逗号分隔字符串)
- **表单使用**：`[1, 2, 3]` (数组)
- **加载时转换**：
  ```javascript
  if (data.participants) {
    data.participants = data.participants.split(',').map(Number)
  }
  ```
- **提交时转换**：
  ```javascript
  if (Array.isArray(submitData.participants)) {
    submitData.participants = submitData.participants.join(',')
  }
  ```

#### 2. 只读字段（回显不可编辑）
- 项目编号（projectCode）- 显示但禁用
- 审核状态（approvalStatus）- 只读展示
- 审核原因（approvalReason）- 只读展示

#### 3. 关联字段（自动填充）
- 销售联系方式 - 选择销售负责人后自动填充手机号
- 客户联系方式 - 选择客户联系人后自动填充电话

### 表单结构（7个折叠面板）

#### 一、项目基本信息
- 项目编号（只读，灰色背景）
- 项目名称*、项目全称
- 行业、区域、简称、年份
- 项目分类*、项目部门*（树形选择）
- 项目状态*、验收状态*
- 预估工作量*、实际工作量
- 项目地址*
- 项目计划*（文本域）
- 项目描述*（文本域）
- 审核状态（只读，标签显示）
- 审核原因（只读，文本域）

#### 二、合同信息（只读区域）
- 合同名称（只读）
- 合同状态（只读）
- 合同金额（只读）
- 免费维护期（只读）

**说明**：合同信息完全只读，不能在项目编辑页面修改。需要修改合同信息请前往合同管理模块。

#### 三、人员配置
- 项目经理*（下拉选择）
- 市场经理*（下拉选择）
- 参与人员*（多选下拉框）
- 销售负责人*（下拉选择）
- 销售联系方式（自动填充，只读）

#### 四、客户信息
- 客户名称*（下拉搜索）
- 客户联系人*（级联下拉）
- 客户联系方式（自动填充，只读）
- 商户联系人
- 商户联系方式

#### 五、时间规划
- 启动日期、结束日期
- 投产日期、验收日期
- **注意**：不包含实施年度字段

#### 六、成本预算
- 项目预算*
- 项目费用、费用预算
- 成本预算、人力费用
- 采购成本

#### 七、备注
- 备注（文本域）

### 底部操作按钮
- **保存按钮**（主按钮，蓝色）- 保存后返回列表页
- **取消按钮**（次按钮，灰色）- 返回列表页

### 提交逻辑

```javascript
function submitForm() {
  proxy.$refs['editFormRef'].validate(valid => {
    if (valid) {
      proxy.$modal.confirm('确认保存项目信息？').then(() => {
        submitLoading.value = true
        const submitData = { ...form.value }

        // 参与人员数组转字符串
        if (Array.isArray(submitData.participants)) {
          submitData.participants = submitData.participants.join(',')
        }

        updateProject(submitData).then(response => {
          proxy.$modal.msgSuccess('保存成功')
          router.push('/project/project')
        }).finally(() => {
          submitLoading.value = false
        })
      })
    } else {
      proxy.$modal.msgError('请完善必填信息')
    }
  })
}
```

## 详情页面设计

### 展示方式
使用 **信息卡片 + 描述列表** 的方式展示，不使用表单组件。

### 页面结构

```vue
<el-card class="page-header">
  <h2>项目详情</h2>
  <el-tag :type="getStatusType(project.approvalStatus)">
    {{ getStatusText(project.approvalStatus) }}
  </el-tag>
</el-card>

<el-collapse v-model="activeNames">
  <el-collapse-item name="1" title="一、项目基本信息">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="项目编号">
        {{ project.projectCode }}
      </el-descriptions-item>
      <!-- ... 其他字段 -->
    </el-descriptions>
  </el-collapse-item>
</el-collapse>
```

### 特殊字段展示

#### 1. 参与人员 - 标签列表
```vue
<el-descriptions-item label="参与人员" :span="2">
  <el-tag v-for="user in participantList" :key="user.userId"
    type="info" style="margin-right: 8px;">
    {{ user.nickName }}
  </el-tag>
</el-descriptions-item>
```

#### 2. 审核状态 - 带颜色标签
```vue
<el-descriptions-item label="审核状态">
  <el-tag :type="statusType">{{ statusText }}</el-tag>
</el-descriptions-item>
```

#### 3. 金额字段 - 格式化显示
```vue
<el-descriptions-item label="项目预算">
  {{ formatMoney(project.projectBudget) }} 元
</el-descriptions-item>
```

### 数据加载

```javascript
function loadProjectDetail(projectId) {
  loading.value = true
  getProject(projectId).then(response => {
    project.value = response.data

    // 转换参与人员ID为用户对象列表
    if (project.value.participants) {
      const userIds = project.value.participants.split(',').map(Number)
      loadParticipantUsers(userIds)
    }
  }).finally(() => {
    loading.value = false
  })
}
```

### 底部操作按钮
- **返回按钮** - 返回列表页
- **编辑按钮** - 跳转到编辑页面

## API 接口

### 1. 获取项目详情
```javascript
getProject(projectId)
// GET /project/project/{projectId}
```

### 2. 更新项目信息
```javascript
updateProject(data)
// PUT /project/project
```

### 3. 获取用户列表
```javascript
listUser(query)
// GET /system/user/list
// 用于参与人员回显
```

## 注意事项

### 1. 确认收入模块
**暂不实现**。确认收入分为"公司确认收入"和"团队确认收入"，是独立的功能模块，等后续开发完成后再集成到项目信息中。

### 2. 实施年度字段
编辑页面和立项申请页面保持一致，**不显示实施年度字段**。

### 3. 合同信息
编辑页面的合同信息**完全只读**，不能修改。项目列表会提供"添加合同"按钮，跳转到合同新增页面。

### 4. 数据一致性
- 参与人员的格式转换要在加载和提交时都处理
- 关联字段（销售联系方式、客户联系方式）要正确监听变化并自动填充

## 实现优先级

1. **编辑页面** - 优先实现，基于立项申请页面修改
2. **详情页面** - 其次实现，展示逻辑相对简单
3. **路由配置** - 在 router/index.ts 中添加动态路由
4. **列表页集成** - 在项目列表页添加"编辑"和"查看"按钮
