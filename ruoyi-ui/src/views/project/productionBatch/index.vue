<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="批次号" prop="batchNo">
        <el-select v-model="queryParams.batchNo" placeholder="请选择批次号" clearable style="width: 180px">
          <el-option v-for="no in batchNoOptions" :key="no" :label="no" :value="no" />
        </el-select>
      </el-form-item>
      <el-form-item label="投产年份" prop="productionYear">
        <el-select v-model="queryParams.productionYear" placeholder="请选择投产年份" clearable style="width: 160px">
          <el-option
            v-for="dict in sys_ndgl"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="计划投产日期" style="width: 340px" label-width="100px">
        <el-date-picker
          v-model="daterangePlanProductionDate"
          value-format="YYYY-MM-DD"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['project:productionBatch:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['project:productionBatch:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['project:productionBatch:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['project:productionBatch:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="productionBatchList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="序号" type="index" width="60" align="center" />
      <el-table-column label="批次号" align="center" prop="batchNo" />
      <el-table-column label="投产年份" align="center" prop="productionYear">
        <template #default="scope">
          <dict-tag :options="sys_ndgl" :value="scope.row.productionYear"/>
        </template>
      </el-table-column>
      <el-table-column label="计划投产日期" align="center" prop="planProductionDate" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.planProductionDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sortOrder" width="80" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['project:productionBatch:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['project:productionBatch:remove']">删除</el-button>
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

    <!-- 添加或修改投产批次 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="productionBatchRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="批次号" prop="batchNo">
          <el-input v-model="form.batchNo" placeholder="请输入批次号" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="投产年份" prop="productionYear">
          <el-select v-model="form.productionYear" placeholder="请选择投产年份" style="width: 100%">
            <el-option v-for="dict in sys_ndgl" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划投产日期" prop="planProductionDate">
          <el-date-picker clearable v-model="form.planProductionDate" type="date" value-format="YYYY-MM-DD"
            placeholder="请选择计划投产日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
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

<script setup name="ProductionBatch">
import { listProductionBatch, getProductionBatch, delProductionBatch, addProductionBatch, updateProductionBatch, getBatchNoOptions } from "@/api/project/productionBatch"

const { proxy } = getCurrentInstance()
const { sys_ndgl } = proxy.useDict('sys_ndgl')

const productionBatchList = ref([])
const batchNoOptions = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const daterangePlanProductionDate = ref([])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    batchNo: null,
    productionYear: null,
  },
  rules: {
    batchNo: [{ required: true, message: "批次号不能为空", trigger: "blur" }],
    sortOrder: [{ required: true, message: "排序不能为空", trigger: "blur" }],
    productionYear: [{ required: true, message: "投产年份不能为空", trigger: "change" }],
    planProductionDate: [{ required: true, message: "计划投产日期不能为空", trigger: "blur" }],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 加载批次号下拉选项 */
function loadBatchNoOptions() {
  getBatchNoOptions().then(res => {
    batchNoOptions.value = res.data || []
  })
}

/** 查询列表 */
function getList() {
  loading.value = true
  queryParams.value.params = {}
  if (daterangePlanProductionDate.value && daterangePlanProductionDate.value.length === 2) {
    queryParams.value.params["beginPlanProductionDate"] = daterangePlanProductionDate.value[0]
    queryParams.value.params["endPlanProductionDate"] = daterangePlanProductionDate.value[1]
  }
  listProductionBatch(queryParams.value).then(response => {
    productionBatchList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = {
    batchId: null, batchNo: null, sortOrder: null,
    productionYear: null, planProductionDate: null, remark: null
  }
  proxy.resetForm("productionBatchRef")
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  daterangePlanProductionDate.value = []
  proxy.resetForm("queryRef")
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.batchId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

function handleAdd() {
  reset()
  open.value = true
  title.value = "新增投产批次"
}

function handleUpdate(row) {
  reset()
  const _batchId = row.batchId || ids.value
  getProductionBatch(_batchId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改投产批次"
  })
}

function submitForm() {
  proxy.$refs["productionBatchRef"].validate(valid => {
    if (valid) {
      if (form.value.batchId != null) {
        updateProductionBatch(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
          loadBatchNoOptions()
        })
      } else {
        addProductionBatch(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
          loadBatchNoOptions()
        })
      }
    }
  })
}

function handleDelete(row) {
  const _batchIds = row.batchId || ids.value
  proxy.$modal.confirm('是否确认删除该投产批次？').then(function() {
    return delProductionBatch(_batchIds)
  }).then(() => {
    getList()
    loadBatchNoOptions()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

function handleExport() {
  proxy.download('project/productionBatch/export', {
    ...queryParams.value
  }, `投产批次_${new Date().getTime()}.xlsx`)
}

loadBatchNoOptions()
getList()
</script>
