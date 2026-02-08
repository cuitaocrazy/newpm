# regionCode 改为 provinceCode 改动方案

## 📋 改动目标

将前端字段名 `regionCode` 统一改为 `provinceCode`，使命名更清晰，避免混淆。

---

## 🎯 改动范围

### 保持不变
- ✅ 数据库字段：`pm_project.region_code`（不改）
- ✅ 后端 Java 字段：`regionCode`（不改）
- ✅ 后端 API 接口：接收/返回 `regionCode`（不改）

### 需要修改
- ❌ 前端字段名：`form.regionCode` → `form.provinceCode`
- ❌ 前端验证规则：`regionCode` → `provinceCode`
- ❌ 前端查询参数：`queryParams.regionCode` → `queryParams.provinceCode`
- ❌ 项目编号生成逻辑中的变量名
- ❌ 元数据文件中的说明

---

## 📝 详细改动计划

### 任务 1: apply.vue（立项申请页面）

**文件：** `/Users/kongli/ws-claude/PM/newpm/ruoyi-ui/src/views/project/project/apply.vue`

**改动点：**

1. **模板部分 - 表单项 prop**
   ```vue
   <!-- 改动前 -->
   <el-form-item label="二级区域" prop="regionCode">
     <el-select v-model="form.regionCode" ...>

   <!-- 改动后 -->
   <el-form-item label="二级区域" prop="provinceCode">
     <el-select v-model="form.provinceCode" ...>
   ```

2. **script 部分 - form 初始化**
   ```javascript
   // 改动前
   const form = ref({
     region: null,
     regionCode: null,
     provinceId: null,

   // 改动后
   const form = ref({
     region: null,
     provinceCode: null,
     provinceId: null,
   ```

3. **script 部分 - 验证规则**
   ```javascript
   // 改动前
   regionCode: [{ required: true, message: "二级区域不能为空", trigger: "change" }],

   // 改动后
   provinceCode: [{ required: true, message: "二级区域不能为空", trigger: "change" }],
   ```

4. **script 部分 - handleRegionChange 函数**
   ```javascript
   // 改动前
   function handleRegionChange(value) {
     form.value.regionCode = null
     form.value.provinceId = null

   // 改动后
   function handleRegionChange(value) {
     form.value.provinceCode = null
     form.value.provinceId = null
   ```

5. **script 部分 - generateProjectCode 函数**
   ```javascript
   // 改动前
   const { industry, region, regionCode, shortName, establishedYear } = form.value
   if (industry && region && regionCode && shortName && establishedYear) {
     form.value.projectCode = `${industry}-${region}-${regionCode}-${shortName}-${establishedYear}`

   // 改动后
   const { industry, region, provinceCode, shortName, establishedYear } = form.value
   if (industry && region && provinceCode && shortName && establishedYear) {
     form.value.projectCode = `${industry}-${region}-${provinceCode}-${shortName}-${establishedYear}`
   ```

6. **script 部分 - 提交前数据映射**
   ```javascript
   // 在 submitForm 函数中添加映射
   function submitForm() {
     proxy.$refs["formRef"].validate(valid => {
       if (valid) {
         const submitData = {
           ...form.value,
           regionCode: form.value.provinceCode  // 映射到后端字段
         }
         delete submitData.provinceCode  // 删除前端字段

         addProject(submitData).then(response => {
           // ...
         })
       }
     })
   }
   ```

---

### 任务 2: edit.vue（编辑项目页面）

**文件：** `/Users/kongli/ws-claude/PM/newpm/ruoyi-ui/src/views/project/project/edit.vue`

**改动点：**

1. **模板部分 - 表单项 prop**
   ```vue
   <!-- 改动前 -->
   <el-form-item label="二级区域" prop="regionCode">
     <el-select v-model="form.regionCode" ...>

   <!-- 改动后 -->
   <el-form-item label="二级区域" prop="provinceCode">
     <el-select v-model="form.provinceCode" ...>
   ```

2. **script 部分 - form 初始化**
   ```javascript
   // 改动前
   form: {
     region: '',
     regionCode: '',
     provinceId: null,

   // 改动后
   form: {
     region: '',
     provinceCode: '',
     provinceId: null,
   ```

3. **script 部分 - 验证规则**
   ```javascript
   // 改动前
   regionCode: [{ required: true, message: '请选择二级区域', trigger: 'change' }],

   // 改动后
   provinceCode: [{ required: true, message: '请选择二级区域', trigger: 'change' }],
   ```

4. **script 部分 - handleRegionChange 函数**
   ```javascript
   // 改动前
   function handleRegionChange(value) {
     form.value.regionCode = null
     form.value.provinceId = null

   // 改动后
   function handleRegionChange(value) {
     form.value.provinceCode = null
     form.value.provinceId = null
   ```

5. **script 部分 - watch 项目编号生成**
   ```javascript
   // 改动前
   watch([() => form.value.industry, () => form.value.region,
          () => form.value.regionCode, () => form.value.shortName, () => form.value.establishedYear],
     ([industry, region, regionCode, shortName, establishedYear]) => {
       if (industry && region && regionCode && shortName && establishedYear) {
         form.value.projectCode = `${industry}-${region}-${regionCode}-${shortName}-${establishedYear}`

   // 改动后
   watch([() => form.value.industry, () => form.value.region,
          () => form.value.provinceCode, () => form.value.shortName, () => form.value.establishedYear],
     ([industry, region, provinceCode, shortName, establishedYear]) => {
       if (industry && region && provinceCode && shortName && establishedYear) {
         form.value.projectCode = `${industry}-${region}-${provinceCode}-${shortName}-${establishedYear}`
   ```

6. **script 部分 - loadProjectData 数据映射**
   ```javascript
   // 在 loadProjectData 函数中添加映射
   getProject(projectId).then(response => {
     const data = response.data
     // 后端 regionCode 映射到前端 provinceCode
     if (data.regionCode) {
       data.provinceCode = data.regionCode
     }
     Object.assign(form.value, data)

     if (data.region) {
       getSecondaryRegions(data.region)
     }
   })
   ```

7. **script 部分 - submitForm 数据映射**
   ```javascript
   // 在提交前添加映射
   function submitForm() {
     proxy.$refs["editFormRef"].validate(valid => {
       if (valid) {
         const submitData = {
           ...form.value,
           regionCode: form.value.provinceCode  // 映射到后端字段
         }
         delete submitData.provinceCode  // 删除前端字段

         updateProject(submitData).then(response => {
           // ...
         })
       }
     })
   }
   ```

---

### 任务 3: index.vue（项目列表页面）

**文件：** `/Users/kongli/ws-claude/PM/newpm/ruoyi-ui/src/views/project/project/index.vue`

**改动点：**

1. **模板部分 - 查询条件**
   ```vue
   <!-- 改动前 -->
   <el-form-item label="二级区域" prop="regionCode">
     <el-select v-model="queryParams.regionCode" ...>

   <!-- 改动后 -->
   <el-form-item label="二级区域" prop="provinceCode">
     <el-select v-model="queryParams.provinceCode" ...>
   ```

2. **模板部分 - 编辑对话框**
   ```vue
   <!-- 改动前 -->
   <el-form-item label="二级区域" prop="regionCode">
     <el-select v-model="form.regionCode" ...>

   <!-- 改动后 -->
   <el-form-item label="二级区域" prop="provinceCode">
     <el-select v-model="form.provinceCode" ...>
   ```

3. **script 部分 - queryParams 初始化**
   ```javascript
   // 改动前
   queryParams: {
     region: null,
     regionCode: null,

   // 改动后
   queryParams: {
     region: null,
     provinceCode: null,
   ```

4. **script 部分 - form 初始化**
   ```javascript
   // 改动前
   form: {
     regionCode: null,

   // 改动后
   form: {
     provinceCode: null,
   ```

5. **script 部分 - handleRegionChange 函数**
   ```javascript
   // 改动前
   function handleRegionChange(value) {
     queryParams.value.regionCode = null

   // 改动后
   function handleRegionChange(value) {
     queryParams.value.provinceCode = null
   ```

6. **script 部分 - handleDialogRegionChange 函数**
   ```javascript
   // 改动前
   function handleDialogRegionChange(value) {
     form.value.regionCode = null
     form.value.provinceId = null

   // 改动后
   function handleDialogRegionChange(value) {
     form.value.provinceCode = null
     form.value.provinceId = null
   ```

7. **script 部分 - getList 查询参数映射**
   ```javascript
   // 在 getList 函数中添加映射
   function getList() {
     loading.value = true
     const queryData = {
       ...queryParams.value,
       regionCode: queryParams.value.provinceCode  // 映射到后端字段
     }
     delete queryData.provinceCode  // 删除前端字段

     listProject(queryData).then(response => {
       // ...
     })
   }
   ```

8. **script 部分 - handleAdd 函数**
   ```javascript
   // 改动前
   function handleAdd() {
     reset()
     open.value = true
     title.value = "添加项目管理"
     dialogSecondaryRegionOptions.value = []
   }

   // 改动后（无需改动，reset() 会清空 form）
   ```

9. **script 部分 - submitForm 数据映射**
   ```javascript
   // 在提交前添加映射
   function submitForm() {
     proxy.$refs["projectRef"].validate(valid => {
       if (valid) {
         const submitData = {
           ...form.value,
           regionCode: form.value.provinceCode  // 映射到后端字段
         }
         delete submitData.provinceCode  // 删除前端字段

         if (form.value.projectId != null) {
           updateProject(submitData).then(response => {
             // ...
           })
         } else {
           addProject(submitData).then(response => {
             // ...
           })
         }
       }
     })
   }
   ```

---

### 任务 4: pm_project.yml（元数据文件）

**文件：** `/Users/kongli/ws-claude/PM/newpm/docs/gen-specs/pm_project.yml`

**改动点：**

1. **region_code 字段的 notes**
   ```yaml
   # 改动前
   notes: |
     存储二级区域代码（province_code），如"BJ"、"SH"

     数据来源：pm_secondary_region.province_code
     前端绑定：v-model="form.regionCode" 绑定到 provinceCode
     用途：项目编号生成的第三部分

   # 改动后
   notes: |
     存储二级区域代码（province_code），如"BJ"、"SH"

     数据来源：pm_secondary_region.province_code
     前端绑定：v-model="form.provinceCode"
     后端字段：regionCode（对应数据库 region_code）
     用途：项目编号生成的第三部分
   ```

2. **established_year 字段的 notes**
   ```yaml
   # 改动前
   notes: |
     项目编号格式：{industry}-{region}-{regionCode}-{shortName}-{establishedYear}

   # 改动后
   notes: |
     项目编号格式：{industry}-{region}-{provinceCode}-{shortName}-{establishedYear}
   ```

3. **project_code 字段的 notes**
   ```yaml
   # 改动前
   notes: |
     项目编号生成规则：
     格式：{行业代码}-{一级区域代码}-{二级区域代码}-{简称}-{年份}

     字段来源：
     - 二级区域代码：regionCode 字段的值（provinceCode）

   # 改动后
   notes: |
     项目编号生成规则：
     格式：{行业代码}-{一级区域代码}-{二级区域代码}-{简称}-{年份}

     字段来源：
     - 二级区域代码：provinceCode（前端字段，映射到后端 regionCode）
   ```

---

## 🔄 前后端字段映射关系

### 提交数据时（前端 → 后端）

```javascript
// 前端数据
const frontendData = {
  region: "HBQY",
  provinceCode: "BJ",  // 前端字段
  provinceId: 1,
  // ...
}

// 映射到后端
const backendData = {
  region: "HBQY",
  regionCode: "BJ",    // 后端字段（对应数据库 region_code）
  provinceId: 1,
  // ...
}
```

### 接收数据时（后端 → 前端）

```javascript
// 后端返回
const backendData = {
  region: "HBQY",
  regionCode: "BJ",    // 后端字段
  provinceId: 1,
  // ...
}

// 映射到前端
const frontendData = {
  region: "HBQY",
  provinceCode: "BJ",  // 前端字段
  provinceId: 1,
  // ...
}
```

---

## ✅ 验证清单

完成所有改动后，需要验证：

1. **立项申请页面（apply.vue）**
   - [ ] 选择一级区域后，二级区域列表正确加载
   - [ ] 选择二级区域后，provinceId 自动设置
   - [ ] 项目编号正确生成：`IT-HBQY-BJ-测试-2024`
   - [ ] 提交表单成功，数据正确保存到数据库

2. **编辑项目页面（edit.vue）**
   - [ ] 打开编辑页面，二级区域正确回显
   - [ ] 修改一级区域后，二级区域列表正确更新
   - [ ] 修改二级区域后，provinceId 自动更新
   - [ ] 项目编号自动更新
   - [ ] 保存修改成功

3. **项目列表页面（index.vue）**
   - [ ] 查询条件中选择二级区域，查询结果正确
   - [ ] 编辑对话框中二级区域选择正常
   - [ ] 新增项目功能正常

4. **数据库验证**
   - [ ] `pm_project.region_code` 字段存储的是省份代码（如"BJ"）
   - [ ] `pm_project.province_id` 字段存储的是省份ID（如1）

---

## 🚀 执行建议

### 并发执行方案

可以将任务分为 3 个独立的 subAgent 并发执行：

**Agent 1: apply.vue**
- 独立文件，无依赖
- 改动点：6 处

**Agent 2: edit.vue**
- 独立文件，无依赖
- 改动点：7 处

**Agent 3: index.vue**
- 独立文件，无依赖
- 改动点：9 处

**Agent 4: pm_project.yml**
- 独立文件，无依赖
- 改动点：3 处

所有 Agent 可以同时执行，互不影响。

---

## 📌 注意事项

1. **数据映射是关键**
   - 前端使用 `provinceCode`
   - 后端使用 `regionCode`
   - 提交/接收时需要做字段映射

2. **验证规则同步修改**
   - `prop` 属性要改为 `provinceCode`
   - 验证规则的 key 也要改为 `provinceCode`

3. **项目编号生成**
   - 所有生成逻辑中的变量名都要改为 `provinceCode`

4. **测试覆盖**
   - 新增、编辑、查询三个场景都要测试
   - 确保数据正确保存到数据库

---

## 🎯 预期结果

完成后：
- ✅ 前端代码更清晰，`provinceCode` 表示省份代码
- ✅ 后端代码不变，保持稳定
- ✅ 数据库不变，无需迁移
- ✅ 功能完全正常，无任何影响
