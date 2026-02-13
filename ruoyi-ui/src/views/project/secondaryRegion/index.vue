<template>
  <div class="app-container secondary-region-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="省份代码" prop="provinceCode">
        <el-autocomplete
          v-model="queryParams.provinceCode"
          :fetch-suggestions="queryProvinceCodeSearch"
          placeholder="请输入省份代码"
          clearable
          style="width: 240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="省份名称" prop="provinceName">
        <el-autocomplete
          v-model="queryParams.provinceName"
          :fetch-suggestions="queryProvinceNameSearch"
          placeholder="请输入省份名称"
          clearable
          style="width: 240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="省份类型" prop="provinceType">
        <el-select v-model="queryParams.provinceType" placeholder="请选择省份类型" clearable style="width: 240px">
          <el-option
            v-for="dict in sflx"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="一级区域" prop="regionDictValue">
        <el-select v-model="queryParams.regionDictValue" placeholder="请选择一级区域" clearable style="width: 240px">
          <el-option
            v-for="dict in sys_yjqy"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 240px">
          <el-option
            v-for="dict in sys_normal_disable"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['project:secondaryRegion:add']"
        >新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="secondaryRegionList" :height="tableHeight">
      <el-table-column label="省份ID" align="center" prop="provinceId" />
      <el-table-column label="省份代码" align="center" prop="provinceCode" />
      <el-table-column label="省份名称" align="center" prop="provinceName" />
      <el-table-column label="省份类型" align="center" prop="provinceType">
        <template #default="scope">
          <dict-tag :options="sflx" :value="scope.row.provinceType"/>
        </template>
      </el-table-column>
      <el-table-column label="一级区域" align="center" prop="regionDictValue">
        <template #default="scope">
          <dict-tag :options="sys_yjqy" :value="scope.row.regionDictValue"/>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sortOrder" />
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="sys_normal_disable" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['project:secondaryRegion:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['project:secondaryRegion:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改省级区域对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="secondaryRegionRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="省份代码" prop="provinceCode">
          <el-input v-model="form.provinceCode" placeholder="请输入省份代码" />
        </el-form-item>
        <el-form-item label="省份名称" prop="provinceName">
          <el-input v-model="form.provinceName" placeholder="请输入省份名称" />
        </el-form-item>
        <el-form-item label="省份类型" prop="provinceType">
          <el-select v-model="form.provinceType" placeholder="请选择省份类型">
            <el-option
              v-for="dict in sflx"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="一级区域" prop="regionDictValue">
          <el-select v-model="form.regionDictValue" placeholder="请选择一级区域">
            <el-option
              v-for="dict in sys_yjqy"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" controls-position="right" :min="0" :max="9999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in sys_normal_disable"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="SecondaryRegion">
import { listSecondaryRegion, getSecondaryRegion, delSecondaryRegion, addSecondaryRegion, updateSecondaryRegion } from "@/api/project/secondaryRegion"

const { proxy } = getCurrentInstance()
const { sflx, sys_yjqy, sys_normal_disable } = proxy.useDict('sflx', 'sys_yjqy', 'sys_normal_disable')

const secondaryRegionList = ref([])
const allProvinceData = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const tableHeight = ref(600)
const title = ref("")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    provinceCode: null,
    provinceName: null,
    provinceType: null,
    regionDictValue: null,
    status: null,
  },
  rules: {
    provinceId: [
      { required: true, message: "省份ID不能为空", trigger: "blur" }
    ],
    provinceCode: [
      { required: true, message: "省份代码不能为空", trigger: "blur" }
    ],
    provinceName: [
      { required: true, message: "省份名称不能为空", trigger: "blur" }
    ],
    regionDictValue: [
      { required: true, message: "一级区域不能为空", trigger: "change" }
    ],
    sortOrder: [
      { required: true, message: "排序不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询省级区域列表 */
function getList() {
  loading.value = true
  listSecondaryRegion(queryParams.value).then(response => {
    secondaryRegionList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 加载全量省份数据用于 autocomplete */
function loadAllProvinceData() {
  listSecondaryRegion({ pageNum: 1, pageSize: 1000 }).then(response => {
    allProvinceData.value = response.rows
  })
}

/** 省份代码 autocomplete 查询 */
function queryProvinceCodeSearch(queryString, cb) {
  const results = queryString
    ? allProvinceData.value.filter(item =>
        item.provinceCode && item.provinceCode.toLowerCase().includes(queryString.toLowerCase())
      ).map(item => ({ value: item.provinceCode }))
    : allProvinceData.value.map(item => ({ value: item.provinceCode }))
  cb(results)
}

/** 省份名称 autocomplete 查询 */
function queryProvinceNameSearch(queryString, cb) {
  const results = queryString
    ? allProvinceData.value.filter(item =>
        item.provinceName && item.provinceName.includes(queryString)
      ).map(item => ({ value: item.provinceName }))
    : allProvinceData.value.map(item => ({ value: item.provinceName }))
  cb(results)
}

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    provinceId: null,
    provinceCode: null,
    provinceName: null,
    provinceType: null,
    regionDictValue: null,
    sortOrder: null,
    status: '0',
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("secondaryRegionRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _provinceId = row.provinceId
  getSecondaryRegion(_provinceId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改省级区域"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["secondaryRegionRef"].validate(valid => {
    if (valid) {
      if (form.value.provinceId != null) {
        updateSecondaryRegion(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addSecondaryRegion(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加省级区域"
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _provinceIds = row.provinceId
  proxy.$modal.confirm('是否确认删除省级区域编号为"' + _provinceIds + '"的数据项？').then(function() {
    return delSecondaryRegion(_provinceIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 计算表格高度 */
function calcTableHeight() {
  nextTick(() => {
    const windowHeight = window.innerHeight
    const searchHeight = showSearch.value ? 100 : 0
    const toolbarHeight = 50
    const paginationHeight = 50
    const padding = 120
    tableHeight.value = windowHeight - searchHeight - toolbarHeight - paginationHeight - padding
  })
}

// 监听窗口大小变化
onMounted(() => {
  calcTableHeight()
  window.addEventListener('resize', calcTableHeight)
})

onUnmounted(() => {
  window.removeEventListener('resize', calcTableHeight)
})

// 监听搜索框显示/隐藏
watch(showSearch, () => {
  calcTableHeight()
})

loadAllProvinceData()
getList()
</script>

<style scoped lang="scss">
.secondary-region-container {
  height: 100%;
  display: flex;
  flex-direction: column;

  :deep(.el-form--inline .el-form-item) {
    margin-right: 20px;
    margin-bottom: 15px;
  }

  :deep(.el-table) {
    font-size: 13px;

    .el-table__header th {
      background-color: #f5f7fa;
      color: #606266;
      font-weight: 600;
    }

    .el-table__body tr:hover > td {
      background-color: #f5f7fa !important;
    }
  }

  :deep(.el-pagination) {
    margin-top: 15px;
    text-align: right;
  }
}
</style>
