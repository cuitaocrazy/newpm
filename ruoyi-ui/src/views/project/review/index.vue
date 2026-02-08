<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="项目名称" prop="projectName">
        <el-input
          v-model="queryParams.projectName"
          placeholder="请输入项目名称"
          clearable
          @keyup.enter="handleQuery"
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item label="项目部门" prop="projectDept">
        <el-tree-select
          v-model="queryParams.projectDept"
          :data="deptOptions"
          :props="{ value: 'id', label: 'label', children: 'children' }"
          value-key="id"
          placeholder="请选择项目部门"
          check-strictly
          clearable
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item label="项目分类" prop="projectCategory">
        <el-select v-model="queryParams.projectCategory" placeholder="请选择项目分类" clearable style="width: 200px">
          <el-option
            v-for="dict in sys_xmfl"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="一级区域" prop="region">
        <el-select v-model="queryParams.region" placeholder="请选择一级区域" clearable @change="handleRegionChange" style="width: 200px">
          <el-option
            v-for="dict in sys_yjqy"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="二级区域" prop="provinceId">
        <el-select v-model="queryParams.provinceId" placeholder="请选择二级区域" clearable :disabled="!queryParams.region" style="width: 200px">
          <el-option
            v-for="item in secondaryRegionOptions"
            :key="item.provinceId"
            :label="item.provinceName"
            :value="item.provinceId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="项目经理" prop="projectManagerId">
        <el-select v-model="queryParams.projectManagerId" placeholder="请选择项目经理" clearable filterable style="width: 200px">
          <el-option
            v-for="user in projectManagerOptions"
            :key="user.userId"
            :label="user.nickName"
            :value="user.userId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="市场经理" prop="marketManagerId">
        <el-select v-model="queryParams.marketManagerId" placeholder="请选择市场经理" clearable filterable style="width: 200px">
          <el-option
            v-for="user in marketManagerOptions"
            :key="user.userId"
            :label="user.nickName"
            :value="user.userId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="项目阶段" prop="projectStatus">
        <el-select v-model="queryParams.projectStatus" placeholder="请选择项目阶段" clearable style="width: 200px">
          <el-option
            v-for="dict in sys_xmjd"
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

    <!-- 列表和对话框将在后续步骤添加 -->
  </div>
</template>

<script setup name="Review">
import { listReview, getReview, approveProject } from "@/api/project/review"
import { listSecondaryRegion } from "@/api/project/secondaryRegion"
import { listUserByPost } from "@/api/system/user"
import { deptTreeSelect } from "@/api/system/dept"

const { proxy } = getCurrentInstance()
const { sys_xmfl, sys_yjqy, sys_xmjd, sys_shzt } = proxy.useDict('sys_xmfl', 'sys_yjqy', 'sys_xmjd', 'sys_shzt')

const reviewList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const deptOptions = ref([])
const secondaryRegionOptions = ref([])
const projectManagerOptions = ref([])
const marketManagerOptions = ref([])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectName: null,
    projectDept: null,
    projectCategory: null,
    region: null,
    provinceId: null,
    projectManagerId: null,
    marketManagerId: null,
    projectStatus: null
  }
})

const { queryParams } = toRefs(data)

/** 查询列表 */
function getList() {
  loading.value = true
  listReview(queryParams.value).then(response => {
    reviewList.value = response.rows
    total.value = response.total
    loading.value = false
  })
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

/** 一级区域变化 */
function handleRegionChange(value) {
  queryParams.value.provinceId = null
  secondaryRegionOptions.value = []
  if (value) {
    listSecondaryRegion({ regionCode: value }).then(response => {
      secondaryRegionOptions.value = response.rows
    })
  }
}

/** 查询部门树 */
function getDeptTree() {
  deptTreeSelect().then(response => {
    deptOptions.value = response.data
  })
}

/** 查询用户列表 */
function getUserOptions() {
  // 查询项目经理（岗位代码：xmjl）
  listUserByPost({ postCode: 'xmjl' }).then(response => {
    projectManagerOptions.value = response.data || []
  })
  // 查询市场经理（岗位代码：scjl）
  listUserByPost({ postCode: 'scjl' }).then(response => {
    marketManagerOptions.value = response.data || []
  })
}

// 初始化
getDeptTree()
getUserOptions()
getList()
</script>
