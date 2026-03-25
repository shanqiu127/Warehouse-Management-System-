<template>
  <div class="oplog-page">
    <div class="paper-layer" aria-hidden="true">
      <span class="paper-grain"></span>
      <span class="paper-line"></span>
    </div>

    <el-card class="panel" shadow="never">
      <template #header>
        <div class="head-wrap">
          <p class="kicker">AUDIT TRAIL</p>
          <h2>操作日志</h2>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="filter-row">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" clearable placeholder="输入用户名" />
        </el-form-item>
        <el-form-item label="模块">
          <el-input v-model="searchForm.module" clearable placeholder="输入模块" />
        </el-form-item>
        <el-form-item label="动作">
          <el-input v-model="searchForm.action" clearable placeholder="输入动作" />
        </el-form-item>
        <el-form-item label="目标类型">
          <el-input v-model="searchForm.targetType" clearable placeholder="输入目标类型" />
        </el-form-item>
        <el-form-item label="时间区间">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" border stripe v-loading="loading" empty-text="暂无操作日志">
        <el-table-column prop="createTime" label="时间" width="180">
          <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="module" label="模块" width="160" show-overflow-tooltip />
        <el-table-column prop="action" label="动作" width="180" show-overflow-tooltip />
        <el-table-column prop="targetType" label="目标类型" width="140" />
        <el-table-column prop="targetId" label="目标 ID" width="120" />
        <el-table-column prop="requestUri" label="请求路径" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="scope">
            <el-button text type="primary" @click="openDetail(scope.row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" title="操作日志详情" size="560px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="时间">{{ formatTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="用户 ID">{{ detail.userId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ detail.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ detail.module || '-' }}</el-descriptions-item>
        <el-descriptions-item label="动作">{{ detail.action || '-' }}</el-descriptions-item>
        <el-descriptions-item label="目标类型">{{ detail.targetType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="目标 ID">{{ detail.targetId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求路径">{{ detail.requestUri || '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ detail.ip || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getOperationLogDetailAPI, getOperationLogPageAPI } from '@/api/audit'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const detailVisible = ref(false)
const detail = reactive({})

const searchForm = reactive({
  username: '',
  module: '',
  action: '',
  targetType: '',
  dateRange: []
})

const formatTime = (val) => {
  if (!val) return '-'
  return String(val).replace('T', ' ')
}

const loadList = async () => {
  loading.value = true
  try {
    const hasRange = Array.isArray(searchForm.dateRange) && searchForm.dateRange.length === 2
    const res = await getOperationLogPageAPI({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      username: searchForm.username || undefined,
      module: searchForm.module || undefined,
      action: searchForm.action || undefined,
      targetType: searchForm.targetType || undefined,
      startDate: hasRange ? searchForm.dateRange[0] : undefined,
      endDate: hasRange ? searchForm.dateRange[1] : undefined
    })
    if (res.code !== 200) throw new Error(res.msg || '加载操作日志失败')
    tableData.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '加载操作日志失败')
  } finally {
    loading.value = false
  }
}

const openDetail = async (row) => {
  try {
    const res = await getOperationLogDetailAPI(row.id)
    if (res.code !== 200) throw new Error(res.msg || '加载详情失败')
    Object.assign(detail, res.data || {})
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载详情失败')
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadList()
}

const handleReset = () => {
  searchForm.username = ''
  searchForm.module = ''
  searchForm.action = ''
  searchForm.targetType = ''
  searchForm.dateRange = []
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

onMounted(() => {
  loadList()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Cinzel:wght@500;700&family=Manrope:wght@400;500;700&display=swap');


.oplog-page {
  --paper: #f4efe6;
  --paper-soft: #f8f3eb;

  position: relative;
  min-height: calc(100vh - 120px);
  padding: 20px;
  border: 1px solid rgba(51, 44, 35, 0.16);
  border-radius: 8px;
  overflow: hidden;
  font-family: 'Manrope', 'Segoe UI', sans-serif;
  background:
    radial-gradient(circle at 8% 10%, rgba(198, 176, 147, 0.16), transparent 36%),
    radial-gradient(circle at 90% 6%, rgba(160, 183, 201, 0.14), transparent 30%),
    linear-gradient(135deg, var(--paper) 0%, var(--paper-soft) 100%);
}

.paper-layer {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.paper-grain {
  position: absolute;
  inset: 0;
  opacity: 0.22;
  background-image: radial-gradient(rgba(0, 0, 0, 0.11) 0.45px, transparent 0.45px);
  background-size: 4px 4px;
}

.paper-line {
  position: absolute;
  inset: 12px;
  border: 1px solid rgba(51, 44, 35, 0.1);
}

.panel {
  position: relative;
  z-index: 1;
  border: 1px solid rgba(44, 37, 31, 0.15);
  background: rgba(255, 255, 255, 0.64);
}

.kicker {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.3em;
  color: #2e4e66;
}

.head-wrap h2 {
  margin: 4px 0 0;
  font-family: 'Cinzel', 'Times New Roman', serif;
}

.filter-row {
  margin-bottom: 8px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .oplog-page {
    padding: 12px;
  }
}
</style>


