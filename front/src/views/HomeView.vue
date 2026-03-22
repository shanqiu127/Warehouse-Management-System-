<template>
  <div class="home-page">
    <el-card class="welcome-card" shadow="never">
      <div class="welcome-header">
        <div>
          <h2>欢迎回来，{{ summary.realName || summary.username || '用户' }}</h2>
          <p class="role-line">当前角色：{{ roleLabel(summary.role) }}</p>
        </div>
        <div class="date-box">
          <div>{{ nowDateText }}</div>
          <div>{{ weekdayText }}</div>
        </div>
      </div>

      <el-row :gutter="16" class="meta-row">
        <el-col :xs="24" :sm="12" :md="8">
          <div class="meta-item">
            <span>本次登录时间</span>
            <strong>{{ formatTime(summary.currentLoginTime) }}</strong>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8">
          <div class="meta-item">
            <span>上次登录时间</span>
            <strong>{{ formatTime(summary.lastLoginTime) }}</strong>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" v-if="isSuperAdmin">
          <div class="meta-item">
            <span>数据库连接状态</span>
            <strong :class="summary.dbStatus === '正常' ? 'ok' : 'bad'">{{ summary.dbStatus || '未知' }}</strong>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="16" class="content-row" v-if="isSuperAdmin">
      <el-col :xs="24" :lg="24">
        <el-card shadow="never">
          <template #header>
            <div class="card-header card-header-row">
              <span>系统错误监控</span>
              <el-tag type="danger" effect="light">24h: {{ summary.errorCount24h || 0 }}</el-tag>
            </div>
          </template>
          <el-table :data="summary.recentErrorLogs || []" size="small" stripe empty-text="暂无系统错误日志">
            <el-table-column prop="createTime" label="时间" width="180">
              <template #default="scope">
                {{ formatTime(scope.row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="statusCode" label="状态码" width="90" />
            <el-table-column prop="errorType" label="错误类型" width="180" show-overflow-tooltip />
            <el-table-column prop="requestUri" label="请求路径" min-width="210" show-overflow-tooltip />
            <el-table-column prop="message" label="错误摘要" min-width="240" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="content-row" v-if="isEmployee">
      <el-col :xs="24" :lg="24">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">系统公告</div>
          </template>
          <el-table :data="noticeList" size="small" v-loading="noticeLoading" stripe>
            <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
            <el-table-column prop="date" label="发布时间" width="180" />
            <el-table-column label="操作" width="90">
              <template #default="scope">
                <el-button text type="primary" @click="openNotice(scope.row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="noticeDialogVisible" title="公告详情" width="620px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="标题">{{ noticeDetail.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ noticeDetail.date || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发布人">{{ noticeDetail.author || '-' }}</el-descriptions-item>
        <el-descriptions-item label="正文">{{ noticeDetail.content || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getHomeSummaryAPI } from '@/api/home'
import { getNoticeDetailAPI, getNoticePageAPI } from '@/api/system'

const summary = ref({
  username: '',
  realName: '',
  role: '',
  currentLoginTime: null,
  lastLoginTime: null,
  dbStatus: '',
  errorCount24h: 0,
  recentErrorLogs: []
})

const noticeList = ref([])
const noticeLoading = ref(false)
const noticeDialogVisible = ref(false)
const noticeDetail = ref({ title: '', date: '', author: '', content: '' })

const normalizeRole = (role) => {
  return String(role || '').trim().toLowerCase()
}

const roleLabel = (role) => {
  const normalized = normalizeRole(role)
  if (normalized === 'superadmin') return '超级管理员'
  if (normalized === 'admin') return '管理员'
  if (normalized === 'employee') return '普通用户'
  return '未知角色'
}

const formatTime = (val) => {
  if (!val) return '暂无记录'
  return String(val).replace('T', ' ')
}

const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
const now = new Date()
const nowDateText = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
const weekdayText = weekdays[now.getDay()]

const isSuperAdmin = computed(() => normalizeRole(summary.value.role) === 'superadmin')
const isEmployee = computed(() => normalizeRole(summary.value.role) === 'employee')

const loadSummary = async () => {
  const res = await getHomeSummaryAPI()
  if (res.code !== 200) {
    throw new Error(res.msg || '首页汇总加载失败')
  }
  const data = res.data || {}
  summary.value = {
    ...summary.value,
    ...data,
    role: normalizeRole(data.role)
  }
}

const loadNotices = async () => {
  if (!isEmployee.value) {
    noticeList.value = []
    return
  }
  noticeLoading.value = true
  try {
    const res = await getNoticePageAPI({ pageNum: 1, pageSize: 5, status: 1 })
    if (res.code !== 200) {
      throw new Error(res.msg || '公告加载失败')
    }
    noticeList.value = (res.data?.records || []).map((item) => ({
      ...item,
      date: formatTime(item.date || item.publishTime)
    }))
  } finally {
    noticeLoading.value = false
  }
}

const openNotice = async (row) => {
  try {
    const res = await getNoticeDetailAPI(row.id)
    if (res.code !== 200) {
      throw new Error(res.msg || '公告详情加载失败')
    }
    noticeDetail.value = {
      ...res.data,
      date: formatTime(res.data?.date || res.data?.publishTime)
    }
    noticeDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '公告详情加载失败')
  }
}

onMounted(async () => {
  try {
    await loadSummary()
    await loadNotices()
  } catch (error) {
    ElMessage.error(error.message || '首页数据加载失败')
  }
})
</script>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.welcome-card {
  background: linear-gradient(135deg, #f8fbff 0%, #eef6ff 100%);
  border: 1px solid #d9ecff;
}

.welcome-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.welcome-header h2 {
  margin: 0;
  font-size: 22px;
  color: #1f2d3d;
}

.role-line {
  margin: 8px 0 0;
  color: #5b6b79;
}

.date-box {
  text-align: right;
  color: #375a7f;
  font-weight: 600;
}

.meta-row {
  margin-top: 16px;
}

.meta-item {
  border: 1px solid #e6eef7;
  border-radius: 8px;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 10px;
  background: #fff;
}

.meta-item span {
  color: #6b7785;
  font-size: 13px;
}

.meta-item strong {
  color: #1f2d3d;
}

.meta-item strong.ok {
  color: #1f9d55;
}

.meta-item strong.bad {
  color: #d94848;
}

.content-row {
  margin-top: 0;
}

.card-header {
  font-weight: 700;
  color: #2f3b47;
}

.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

@media (max-width: 768px) {
  .welcome-header {
    flex-direction: column;
  }

  .date-box {
    text-align: left;
  }
}
</style>