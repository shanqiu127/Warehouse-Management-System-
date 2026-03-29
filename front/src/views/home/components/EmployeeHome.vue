<template>
  <div class="employee-dashboard">
    <div class="dashboard-shell">
      <header class="hero-card">
        <div>
          <p class="eyebrow">DEPARTMENT EMPLOYEE</p>
          <h1>{{ summary.realName || summary.username || '部门员工' }}</h1>
          <p class="hero-desc">{{ summary.deptName || '当前部门' }} · 今日工作看板</p>
        </div>
        <div class="hero-meta">
          <span>{{ formatTime(summary.currentLoginTime) }}</span>
        </div>
      </header>

      <section class="metric-grid">
        <article class="metric-card">
          <span class="metric-label">当前部门</span>
          <strong>{{ summary.deptName || '未分配部门' }}</strong>
        </article>
        <article class="metric-card">
          <span class="metric-label">公告数量</span>
          <strong>{{ noticeList.length }}</strong>
        </article>
        <article class="metric-card">
          <span class="metric-label">上次登录</span>
          <strong>{{ formatTime(summary.lastLoginTime) }}</strong>
        </article>
      </section>

      <section class="panel-card" v-loading="noticeLoading">
        <div class="panel-head">
          <h2>最新公告</h2>
          <span>按当前账号可见范围过滤</span>
        </div>
        <div v-if="noticeList.length" class="notice-list">
          <button v-for="item in noticeList" :key="item.id" type="button" class="notice-item" @click="openNotice(item)">
            <div>
              <div class="notice-title">{{ item.title }}</div>
              <div class="notice-meta">{{ item.author || item.publisher || '系统发布' }}</div>
            </div>
            <span class="notice-date">{{ formatTime(item.date || item.publishTime) }}</span>
          </button>
        </div>
        <div v-else class="empty-state">暂无可见公告</div>
      </section>

      <el-dialog v-model="noticeDialogVisible" title="公告详情" width="620px">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="标题">{{ noticeDetail.title || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ noticeDetail.date || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发布人">{{ noticeDetail.author || '-' }}</el-descriptions-item>
          <el-descriptions-item label="正文">
            <p class="notice-content">{{ noticeDetail.content || '-' }}</p>
          </el-descriptions-item>
        </el-descriptions>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getHomeSummaryAPI } from '@/api/home'
import { getNoticeDetailAPI, getNoticePageAPI } from '@/api/system'

const summary = ref({
  username: '',
  realName: '',
  deptName: '',
  currentLoginTime: null,
  lastLoginTime: null
})
const noticeList = ref([])
const noticeLoading = ref(false)
const noticeDialogVisible = ref(false)
const noticeDetail = ref({ title: '', date: '', author: '', content: '' })

const formatTime = (val) => {
  if (!val) return '--'
  return String(val).replace('T', ' ').substring(0, 19)
}

const loadSummary = async () => {
  try {
    const res = await getHomeSummaryAPI()
    if (res.code === 200) {
      summary.value = { ...summary.value, ...res.data }
    }
  } catch {
    ElMessage.error('首页摘要加载失败')
  }
}

const loadNotices = async () => {
  noticeLoading.value = true
  try {
    const res = await getNoticePageAPI({ pageNum: 1, pageSize: 6 })
    if (res.code === 200) {
      noticeList.value = res.data?.records || []
    }
  } catch {
    ElMessage.error('公告加载失败')
  } finally {
    noticeLoading.value = false
  }
}

const openNotice = async (row) => {
  try {
    const res = await getNoticeDetailAPI(row.id)
    if (res.code === 200) {
      noticeDetail.value = { ...res.data, date: formatTime(res.data?.date || res.data?.publishTime) }
      noticeDialogVisible.value = true
    }
  } catch {
    ElMessage.error('公告详情加载失败')
  }
}

onMounted(() => {
  loadSummary()
  loadNotices()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');

.employee-dashboard {
  min-height: calc(100vh - 100px);
  padding: 36px;
  background:
    radial-gradient(circle at top right, rgba(5, 150, 105, 0.12), transparent 28%),
    radial-gradient(circle at bottom left, rgba(202, 138, 4, 0.1), transparent 28%),
    linear-gradient(180deg, #f7fafc 0%, #edf4f7 100%);
  font-family: 'Plus Jakarta Sans', sans-serif;
}

.dashboard-shell {
  max-width: 1280px;
  margin: 0 auto;
  display: grid;
  gap: 24px;
}

.hero-card,
.metric-card,
.panel-card,
.notice-item {
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 20px 45px -30px rgba(15, 23, 42, 0.3);
}

.hero-card {
  border-radius: 28px;
  padding: 32px;
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
}

.eyebrow {
  margin: 0 0 12px;
  font-size: 12px;
  letter-spacing: 0.22em;
  color: #059669;
}

.hero-card h1 {
  margin: 0;
  font-size: 2.5rem;
  line-height: 1.06;
  color: #0f172a;
}

.hero-desc {
  margin: 14px 0 0;
  color: #475569;
  font-size: 1rem;
}

.hero-meta {
  display: grid;
  gap: 10px;
  min-width: 240px;
  color: #334155;
  font-size: 0.95rem;
  justify-items: end;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.metric-card {
  border-radius: 22px;
  padding: 22px;
  display: grid;
  gap: 8px;
}

.metric-label {
  font-size: 0.86rem;
  color: #64748b;
}

.metric-card strong {
  font-size: 1.35rem;
  color: #0f172a;
  word-break: break-word;
}

.panel-card {
  border-radius: 28px;
  padding: 28px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.panel-head h2 {
  margin: 0;
  font-size: 1.2rem;
  color: #0f172a;
}

.panel-head span,
.notice-meta,
.notice-date,
.empty-state {
  color: #64748b;
  font-size: 0.9rem;
}

.notice-list {
  display: grid;
  gap: 12px;
}

.notice-item {
  width: 100%;
  border-radius: 18px;
  padding: 18px 20px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.notice-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 22px 45px -34px rgba(15, 23, 42, 0.5);
}

.notice-title {
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 8px;
}

.notice-content {
  white-space: pre-wrap;
  line-height: 1.7;
  margin: 0;
}

@media (max-width: 960px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .hero-card {
    flex-direction: column;
  }
}
</style>