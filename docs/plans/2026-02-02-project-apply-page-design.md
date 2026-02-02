# 项目立项申请页面设计方案

**日期：** 2026-02-02
**作者：** Claude & 用户
**状态：** 已确认

## 一、背景

当前项目管理模块的"新增"功能使用弹窗形式，字段较多且流程不够清晰。为了优化用户体验，将立项申请改为独立页面，分模块展示，提供更好的填写体验。

## 二、设计目标

1. 将"新增"按钮改为"立项申请"按钮
2. 跳转到独立的立项申请页面（非弹窗）
3. 使用折叠面板分模块展示，默认全部展开
4. 实现智能联动和自动填充功能
5. 提交后自动设置审核状态为"待审核"

## 三、页面结构

### 3.1 路由配置

- **路由路径：** `/project/apply`
- **组件位置：** `ruoyi-ui/src/views/project/apply/index.vue`
- **导航方式：** 从项目列表页面点击"立项申请"按钮跳转

### 3.2 页面布局

使用 `el-collapse` 折叠面板组件，包含7个模块：

1. **一、项目基本信息**
2. **二、合同信息**（只读回显）
3. **三、人员配置**
4. **四、客户信息**
5. **五、时间规划**
6. **六、成本预算**
7. **七、备注**

## 四、表单字段设计

### 4.1 数据结构

```javascript
{
  // 一、项目基本信息
  industry: '',           // 行业（必填，字典：industry）
  region: '',             // 区域（必填，字典：sys_yjqy）
  shortName: '',          // 项目简称（必填）
  year: '',               // 年份（必填）
  projectCode: '',        // 项目编号（必填，自动生成）
  projectName: '',        // 项目名称（必填）
  projectCategory: '',    // 项目分类（必填，字典：sys_xmfl）
  projectDept: '',        // 项目部门（必填）
  projectStatus: '',      // 项目状态（必填，字典：sys_xmjd）
  acceptanceStatus: '',   // 验收状态（必填，字典：sys_yszt）
  estimatedWorkload: '',  // 预估工作量（必填，人天）
  projectPlan: '',        // 项目计划（必填）
  projectDescription: '', // 项目描述（必填）
  projectAddress: '',     // 项目地址（必填）

  // 二、合同信息（只读回显，不在form中）
  // contractInfo: { name, status, amount, maintenancePeriod }

  // 三、人员配置
  projectManagerId: '',   // 项目经理（必填，岗位编码：pm）
  marketManagerId: '',    // 市场经理（必填，岗位编码：scjl）
  participants: [],       // 参与人员（必填，数组，存储用户ID）
  salesManagerId: '',     // 销售负责人（必填，岗位编码：xsfzr）
  salesContact: '',       // 销售联系方式（必填）

  // 四、客户信息
  customerId: '',         // 客户名称（必填）
  customerContactId: '',  // 客户联系人（必填）
  customerPhone: '',      // 客户联系方式（自动带出，只读）
  merchantContact: '',    // 商户联系人
  merchantPhone: '',      // 商户联系方式

  // 五、时间规划
  startDate: '',          // 启动日期
  endDate: '',            // 结束日期
  productionDate: '',     // 投产日期
  acceptanceDate: '',     // 验收日期
  implementationYear: '', // 实施年度

  // 六、成本预算
  projectCost: '',        // 项目费用
  projectBudget: '',      // 项目预算（必填）
  costBudget: '',         // 费用预算
  budgetCost: '',         // 成本预算
  laborCost: '',          // 人力费用
  purchaseCost: '',       // 采购成本

  // 七、备注
  remark: '',             // 备注

  // 默认字段
  approvalStatus: '0'     // 审核状态（默认：待审核）
}
```

### 4.2 字典数据

使用以下字典类型：

| 字典类型 | 说明 | 用途 |
|---------|------|------|
| industry | 行业 | 项目基本信息 |
| sys_yjqy | 区域 | 项目基本信息 |
| sys_xmfl | 项目分类 | 项目基本信息 |
| sys_xmjd | 项目阶段 | 项目基本信息 |
| sys_yszt | 验收状态 | 项目基本信息 |
| sys_shzt | 审批状态 | 默认值（待审核） |
| sys_qrzt | 确认状态 | 备用 |

## 五、关键交互逻辑

### 5.1 项目编号自动生成

**触发条件：** 行业、区域、简称、年份四个字段都有值时

**生成规则：** `{行业代码}-{区域代码}-{简称}-{年份}`

**实现方式：**
```javascript
watch([() => form.value.industry, () => form.value.region,
       () => form.value.shortName, () => form.value.year],
  ([industry, region, shortName, year]) => {
    if (industry && region && shortName && year) {
      // 从字典中获取对应的代码值
      const industryCode = industry
      const regionCode = region
      form.value.projectCode = `${industryCode}-${regionCode}-${shortName}-${year}`
    }
  }
)
```

### 5.2 客户联系人联动

**第一步：** 选择客户后，加载该客户的联系人列表

```javascript
watch(() => form.value.customerId, (customerId) => {
  if (customerId) {
    form.value.customerContactId = ''
    form.value.customerPhone = ''
    loadCustomerContacts(customerId)
  }
})
```

**第二步：** 选择联系人后，自动填充联系方式

```javascript
watch(() => form.value.customerContactId, (contactId) => {
  if (contactId) {
    const contact = customerContacts.value.find(c => c.contactId === contactId)
    if (contact) {
      form.value.customerPhone = contact.phone
    }
  }
})
```

### 5.3 参与人员智能提示

**展示方式：**
- 使用 `el-autocomplete` 组件实现智能提示
- 已选人员以 `el-tag` 标签形式展示在输入框上方
- 支持点击标签的关闭按钮删除已选人员

**实现逻辑：**

```javascript
// 已选人员列表
const selectedParticipants = ref([]) // [{ userId, userName, nickName }]

// 智能提示查询
function queryParticipants(queryString, cb) {
  listUser({ userName: queryString }).then(response => {
    const results = response.rows.map(user => ({
      value: user.nickName,
      userId: user.userId,
      userName: user.userName,
      nickName: user.nickName
    }))
    cb(results)
  })
}

// 选中人员
function handleSelectParticipant(item) {
  if (!selectedParticipants.value.find(p => p.userId === item.userId)) {
    selectedParticipants.value.push(item)
    form.value.participants = selectedParticipants.value.map(p => p.userId)
  }
  participantInput.value = ''
}

// 删除人员
function removeParticipant(userId) {
  selectedParticipants.value = selectedParticipants.value.filter(p => p.userId !== userId)
  form.value.participants = selectedParticipants.value.map(p => p.userId)
}
```

### 5.4 人员下拉框按岗位过滤

**项目经理：** 岗位编码 `pm`
**市场经理：** 岗位编码 `scjl`
**销售负责人：** 岗位编码 `xsfzr`

**实现方式：** 调用 `/system/user/listByPost?postCode=pm` 接口

## 六、表单验证规则

### 6.1 必填字段

**项目基本信息：**
- 行业、区域、项目简称、年份
- 项目编号、项目名称、项目分类、项目部门
- 项目状态、验收状态、预估工作量
- 项目计划、项目描述、项目地址

**人员配置：**
- 项目经理、市场经理、参与人员（至少1人）
- 销售负责人、销售联系方式

**客户信息：**
- 客户名称、客户联系人

**成本预算：**
- 项目预算

### 6.2 格式验证

**数字字段：**
- 预估工作量、项目预算、项目费用等
- 正则：`/^\d+(\.\d+)?$/`

## 七、后端接口需求

### 7.1 新增接口

**1. 根据岗位编码查询用户列表**

```
GET /system/user/listByPost
参数：postCode（岗位编码）
返回：List<SysUser>
```

**实现要点：**
- 查询 sys_user_post 关联表
- 根据 post_code 过滤用户
- 返回用户基本信息（userId、userName、nickName）

**2. 根据客户ID查询联系人列表**（假设已存在）

```
GET /project/contact/listByCustomer
参数：customerId
返回：List<Contact>
```

### 7.2 复用接口

**项目新增接口：**

```
POST /project/project
请求体：完整的项目表单数据
特殊处理：approvalStatus 默认设置为 '0'（待审核）
```

## 八、前端实现要点

### 8.1 文件结构

```
ruoyi-ui/src/views/project/apply/
  └── index.vue          # 立项申请主页面

ruoyi-ui/src/api/system/user.ts
  └── 新增 listUserByPost() 方法

ruoyi-ui/src/views/project/project/index.vue
  └── 修改"新增"按钮为"立项申请"
```

### 8.2 路由配置

```javascript
// ruoyi-ui/src/router/index.ts
{
  path: '/project/apply',
  component: () => import('@/views/project/apply/index.vue'),
  name: 'ProjectApply',
  meta: { title: '项目立项申请', activeMenu: '/project/list' }
}
```

### 8.3 按钮修改

**项目列表页面：**

```vue
<!-- 原来的新增按钮 -->
<el-button type="primary" icon="Plus" @click="handleAdd">新增</el-button>

<!-- 改为立项申请按钮 -->
<el-button type="primary" icon="Plus" @click="handleApply">立项申请</el-button>
```

```javascript
// 跳转到立项申请页面
function handleApply() {
  router.push('/project/apply')
}
```

### 8.4 样式设计

**关键样式：**

1. **折叠面板标题：** 左侧蓝色边框，加粗字体
2. **只读信息区域：** 浅灰色背景，圆角边框
3. **参与人员标签：** 使用 `el-tag` 组件，支持关闭
4. **底部按钮：** 固定在底部，白色背景，上边框

## 九、提交流程

### 9.1 表单验证

```javascript
function submitForm() {
  proxy.$refs['applyFormRef'].validate(valid => {
    if (valid) {
      // 验证通过，继续提交
    } else {
      proxy.$modal.msgError('请完善必填信息')
    }
  })
}
```

### 9.2 提交确认

```javascript
proxy.$modal.confirm('确认提交立项申请？').then(() => {
  // 调用接口
})
```

### 9.3 接口调用

```javascript
addProject(form.value).then(response => {
  proxy.$modal.msgSuccess('立项申请提交成功')
  router.push('/project/list')
})
```

### 9.4 取消操作

```javascript
function cancel() {
  proxy.$modal.confirm('确认取消立项申请？未保存的数据将丢失').then(() => {
    router.push('/project/list')
  })
}
```

## 十、实施步骤

### 10.1 后端开发

1. 新增 `/system/user/listByPost` 接口
   - 在 `SysUserController` 中添加方法
   - 在 `SysUserService` 中实现业务逻辑
   - 在 `SysUserMapper` 中添加 SQL 查询

2. 确认客户联系人接口是否存在
   - 如不存在，需要新增

### 10.2 前端开发

1. 创建立项申请页面 `ruoyi-ui/src/views/project/apply/index.vue`
2. 添加路由配置
3. 修改项目列表页面的"新增"按钮
4. 新增 API 方法 `listUserByPost()`
5. 实现7个折叠面板模块
6. 实现智能联动和自动填充逻辑
7. 添加表单验证规则
8. 实现提交和取消逻辑

### 10.3 测试验证

1. 项目编号自动生成功能测试
2. 客户联系人联动功能测试
3. 参与人员智能提示功能测试
4. 人员下拉框按岗位过滤测试
5. 表单验证规则测试
6. 提交流程测试（创建项目并跳转）
7. 审核状态默认值测试（应为"待审核"）

## 十一、注意事项

1. **合同信息模块：** 当前为只读回显，如需关联合同数据，需要确认合同表是否已创建
2. **项目编号：** 自动生成后用户可以手动修改，但需要保证唯一性
3. **参与人员：** 至少选择1人，存储时只保存用户ID数组
4. **客户联系方式：** 自动带出后为只读，不可手动修改
5. **审核状态：** 提交时自动设置为 '0'（待审核），用户不可选择
6. **数据字典：** 确保所有字典类型在数据库中已配置完整

## 十二、后续优化

1. 支持上传附件（合同扫描件、项目计划文档等）
2. 支持项目模板功能（快速填充常用项目信息）
3. 添加字段联动提示（如：选择某个项目分类后，提示推荐的项目状态）
4. 优化移动端适配
