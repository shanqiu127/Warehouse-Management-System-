<template>
  <el-card>
    <el-form :inline="true" :model="searchForm" class="search-form">
      <el-form-item label="部门名称">
        <el-input v-model="searchForm.deptName" placeholder="请输入部门名称" clearable />
      </el-form-item>
      <el-form-item label="提交人">
        <el-input v-model="searchForm.requesterName" placeholder="请输入提交人" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 140px;">
          <el-option label="待审批" :value="1" />
          <el-option label="已通过" :value="2" />
          <el-option label="已驳回" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item class="search-actions">
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" border style="width: 100%" v-loading="loading">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="deptName" label="部门名称" min-width="140" />
      <el-table-column prop="manager" label="负责人" min-width="120" />
      <el-table-column prop="contactPhone" label="联系电话" width="140" />
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      <el-table-column prop="requesterName" label="提交人" width="120" />
      <el-table-column label="提交时间" width="170">
        <template #default="scope">{{ formatDateTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="approverName" label="审批人" width="120" />
      <el-table-column label="审批时间" width="170">
        <template #default="scope">{{ formatDateTime(scope.row.approvedAt || scope.row.rejectedAt) }}</template>
      </el-table-column>
      <el-table-column prop="approvalRemark" label="审批备注" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="scope">
          <el-button
            v-if="scope.row.status === 1"
            size="small"
            type="success"
            link
            @click="handleApprove(scope.row)"
          >
            通过
          </el-button>
          <el-button
            v-if="scope.row.status === 1"
            size="small"
            type="danger"
            link
            @click="handleReject(scope.row)"
          >
            驳回
          </el-button>
          <span v-else class="muted-text">已处理</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager-box">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveDeptAPI, getDeptPageAPI, rejectDeptAPI } from '@/api/system'

const searchForm = reactive({
  deptName: '',
  requesterName: '',
  status: null
})

const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const formatDateTime = (val) => {
  if (!val) return '-'
  return String(val).replace('T', ' ')
}

const statusLabel = (status) => {
  if (status === 1) return '待审批'
  if (status === 2) return '已通过'
  if (status === 3) return '已驳回'
  return '未知'
}

const statusTagType = (status) => {
  if (status === 1) return 'warning'
  if (status === 2) return 'success'
  if (status === 3) return 'danger'
  return 'info'
}

const loadList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      deptName: searchForm.deptName || undefined,
      requesterName: searchForm.requesterName || undefined,
      status: searchForm.status === null ? undefined : searchForm.status
    }
    const res = await getDeptPageAPI(params)
    if (res.code !== 200) {
      throw new Error(res.msg || '部门审批查询失败')
    }
    const pageData = res.data || {}
    tableData.value = pageData.records || []
    total.value = pageData.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载部门审批失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadList()
}

const resetSearch = () => {
  searchForm.deptName = ''
  searchForm.requesterName = ''
  searchForm.status = null
  currentPage.value = 1
  loadList()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  loadList()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  loadList()
}

const handleApprove = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入审批备注（选填）', '部门审批通过', {
      confirmButtonText: '通过',
      cancelButtonText: '取消',
      inputPlaceholder: '可留空'
    })
    const res = await approveDeptAPI(row.id, { remark: value || '' })
    if (res.code !== 200) {
      throw new Error(res.msg || '审批通过失败')
    }
    ElMessage.success('部门已审批通过')
    await loadList()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error.message || '审批通过失败')
  }
}

const handleReject = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因（选填）', '部门审批驳回', {
      confirmButtonText: '驳回',
      cancelButtonText: '取消',
      inputPlaceholder: '可留空'
    })
    const res = await rejectDeptAPI(row.id, { remark: value || '' })
    if (res.code !== 200) {
      throw new Error(res.msg || '审批驳回失败')
    }
    ElMessage.success('部门申请已驳回')
    await loadList()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error.message || '审批驳回失败')
  }
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.search-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.search-form :deep(.search-actions) {
  margin-left: auto;
}

.pager-box {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.muted-text {
  color: #909399;
  font-size: 12px;
}
</style>