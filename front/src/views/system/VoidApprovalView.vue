<template>
  <el-card>
    <el-form :inline="true" :model="searchForm" class="search-form">
      <el-form-item label="审批单号">
        <el-input v-model="searchForm.approvalNo" placeholder="请输入审批单号" clearable />
      </el-form-item>
      <el-form-item label="业务类型">
        <el-select v-model="searchForm.bizType" placeholder="全部" clearable style="width: 160px;">
          <el-option v-for="item in bizTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="申请动作">
        <el-select v-model="searchForm.requestAction" placeholder="全部" clearable style="width: 160px;">
          <el-option v-for="item in actionOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
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
      <el-table-column prop="approvalNo" label="审批单号" width="180" />
      <el-table-column label="业务类型" width="120">
        <template #default="scope">
          {{ bizTypeLabel(scope.row.bizType) }}
        </template>
      </el-table-column>
      <el-table-column prop="bizNo" label="业务单号" width="170" />
      <el-table-column label="申请动作" width="120">
        <template #default="scope">
          {{ actionLabel(scope.row.requestAction) }}
        </template>
      </el-table-column>
      <el-table-column prop="requestReason" label="申请原因" min-width="180" show-overflow-tooltip />
      <el-table-column prop="requesterName" label="申请人" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" width="170">
        <template #default="scope">{{ formatDateTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column prop="approverName" label="审批人" width="120" />
      <el-table-column label="审批时间" width="170">
        <template #default="scope">{{ formatDateTime(scope.row.approvedAt || scope.row.rejectedAt) }}</template>
      </el-table-column>
      <el-table-column prop="approveRemark" label="审批备注" min-width="160" show-overflow-tooltip />
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
import {
  approveApprovalOrderAPI,
  getApprovalOrderPageAPI,
  rejectApprovalOrderAPI
} from '@/api/system'

const bizTypeOptions = [
  { label: '进货单', value: 'purchase' },
  { label: '进货退货单', value: 'purchase_return' },
  { label: '销售单', value: 'sales' },
  { label: '销售退货单', value: 'sales_return' }
]

const actionOptions = [
  { label: '仅作废', value: 'void' },
  { label: '作废并红冲', value: 'void_red' }
]

const searchForm = reactive({
  approvalNo: '',
  bizType: '',
  requestAction: '',
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

const bizTypeLabel = (val) => {
  return bizTypeOptions.find((item) => item.value === val)?.label || val || '-'
}

const actionLabel = (val) => {
  return actionOptions.find((item) => item.value === val)?.label || val || '-'
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
      approvalNo: searchForm.approvalNo || undefined,
      bizType: searchForm.bizType || undefined,
      requestAction: searchForm.requestAction || undefined,
      status: searchForm.status === null ? undefined : searchForm.status
    }
    const res = await getApprovalOrderPageAPI(params)
    if (res.code !== 200) {
      throw new Error(res.msg || '审批查询失败')
    }
    const pageData = res.data || {}
    tableData.value = pageData.records || []
    total.value = pageData.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载审批列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadList()
}

const resetSearch = () => {
  searchForm.approvalNo = ''
  searchForm.bizType = ''
  searchForm.requestAction = ''
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
    const { value } = await ElMessageBox.prompt('可填写审批备注（选填）', '审批通过', {
      confirmButtonText: '通过',
      cancelButtonText: '取消',
      inputValue: ''
    })

    const res = await approveApprovalOrderAPI(row.id, { remark: value || '' })
    if (res.code !== 200) {
      throw new Error(res.msg || '审批失败')
    }
    ElMessage.success('审批通过，已执行作废流程')
    await loadList()
  } catch (error) {
    if (error?.message && error.message !== 'cancel') {
      ElMessage.error(error.message)
    }
  }
}

const handleReject = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因（选填）', '审批驳回', {
      confirmButtonText: '驳回',
      cancelButtonText: '取消',
      inputValue: ''
    })

    const res = await rejectApprovalOrderAPI(row.id, { remark: value || '' })
    if (res.code !== 200) {
      throw new Error(res.msg || '驳回失败')
    }
    ElMessage.success('已驳回审批申请')
    await loadList()
  } catch (error) {
    if (error?.message && error.message !== 'cancel') {
      ElMessage.error(error.message)
    }
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
