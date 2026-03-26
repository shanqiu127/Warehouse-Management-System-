<template>
  <div class="admin-dashboard">
    <div class="glass-orb shape-1" style="background: #dcfce7;"></div>
    <div class="glass-orb shape-2" style="background: #fef9c3;"></div>
    
    <div class="content-wrapper">
      <header class="admin-header">
        <div class="header-text">
          <span class="eyebrow">WORKSPACE / EMPLOYEE</span>
          <h1>Welcome back, <br/> <span class="highlight-emp">{{ summary.realName || summary.username || 'Employee' }}</span></h1>
        </div>
        <div class="header-date">
          <div class="glass-chip">
            <span class="status-dot"></span>
            {{ nowDateText }}
          </div>
        </div>
      </header>

      <section class="metrics-grid">
        <div class="metric-glass card-hover">
          <div class="metric-content">
            <p>Current Session</p>
            <h3>{{ formatTime(summary.currentLoginTime) }}</h3>
          </div>
        </div>
        <div class="metric-glass card-hover">
          <div class="metric-content">
            <p>Previous Login</p>
            <h3>{{ formatTime(summary.lastLoginTime) }}</h3>
          </div>
        </div>

        <div class="metric-glass card-hover" @click="goStockWarning('low')" style="cursor: pointer;">
          <div class="metric-content">
            <p>Low Stock Alerts</p>
            <h3>{{ summary.lowStockCount ?? 0 }}</h3>
          </div>
        </div>

        <div class="metric-glass card-hover" @click="goStockWarning('zero')" style="cursor: pointer;">
          <div class="metric-content">
            <p>Zero Stock Alerts</p>
            <h3>{{ summary.zeroStockCount ?? 0 }}</h3>
          </div>
        </div>
      </section>

      <div class="layout-grid-full">
        <section class="system-status card-glass" v-loading="noticeLoading">
          <div class="section-header">
            <h2>System Announcements</h2>
            <div class="notice-count" v-if="noticeList.length">{{ noticeList.length }} New</div>
          </div>
          
          <div class="notice-list">
            <div class="notice-item" v-for="(item, index) in noticeList" :key="index" @click="openNotice(item)">
              <div class="notice-info">
                <h3>{{ item.title }}</h3>
                <p>Published by {{ item.author || 'System' }}</p>
              </div>
              <div class="notice-date">{{ formatTime(item.date || item.publishTime).split(' ')[0] }}</div>
            </div>
            <div class="empty-state" v-if="!noticeLoading && noticeList.length === 0">
              No new announcements at this time.
            </div>
          </div>
        </section>
      </div>

      <!-- Element Plus 原生弹窗 (沿用原有交互逻辑) -->
      <el-dialog v-model="noticeDialogVisible" title="System Announcement" width="620px">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="标题">{{ noticeDetail.title || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ noticeDetail.date || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发布人">{{ noticeDetail.author || '-' }}</el-descriptions-item>
          <el-descriptions-item label="正文">
            <p style="white-space: pre-wrap; line-height: 1.6;">{{ noticeDetail.content || '-' }}</p>
          </el-descriptions-item>
        </el-descriptions>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getHomeSummaryAPI } from '@/api/home'
import { getNoticeDetailAPI, getNoticePageAPI } from '@/api/system'

const router = useRouter()
const summary = ref({
  username: '',
  realName: '',
  currentLoginTime: null,
  lastLoginTime: null,
  lowStockCount: 0,
  zeroStockCount: 0
})
const noticeList = ref([])
const noticeLoading = ref(false)
const noticeDialogVisible = ref(false)
const noticeDetail = ref({ title: '', date: '', author: '', content: '' })

const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }
const nowDateText = new Date().toLocaleDateString('en-US', options)

const formatTime = (val) => {
  if (!val) return '--'
  return String(val).replace('T', ' ').substring(0, 19)
}

const loadSummary = async () => {
  try {
    const res = await getHomeSummaryAPI()
    if (res.code === 200) summary.value = { ...summary.value, ...res.data }
  } catch (error) {
    ElMessage.error('Failed to load summary')
  }
}

const loadNotices = async () => {
  noticeLoading.value = true
  try {
    const res = await getNoticePageAPI({ pageNum: 1, pageSize: 5, status: 1 })
    if (res.code === 200) noticeList.value = res.data?.records || []
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
  } catch (error) {
    ElMessage.error('Failed to load notice content')
  }
}

const goStockWarning = (type = 'low') => {
  router.push({ path: '/business/stock-warning', query: { type } })
}

onMounted(() => { loadSummary(); loadNotices(); })
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');
.admin-dashboard {
  --bg-color: #f6f8fb; --text-primary: #111827; --text-secondary: #6b7280;
  --glass-bg: rgba(255, 255, 255, 0.7); --glass-border: rgba(255, 255, 255, 0.5);
  position: relative; min-height: calc(100vh - 100px); background: var(--bg-color); font-family: 'Plus Jakarta Sans', sans-serif; color: var(--text-primary); overflow: hidden; padding: 40px; z-index: 1;
}
.glass-orb { position: absolute; border-radius: 50%; filter: blur(80px); z-index: -1; opacity: 0.6; }
.shape-1 { width: 450px; height: 450px; top: -100px; right: -100px; }
.shape-2 { width: 550px; height: 550px; bottom: -200px; left: -200px; }
.content-wrapper { max-width: 1280px; margin: 0 auto; }
.admin-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 48px; animation: slideDown 0.6s ease-out; }
.eyebrow { font-size: 0.75rem; font-weight: 700; letter-spacing: 0.1em; color: var(--text-secondary); text-transform: uppercase; margin-bottom: 12px; display: block; }
.admin-header h1 { font-size: 3rem; font-weight: 800; letter-spacing: -0.02em; line-height: 1.1; margin: 0; }
.highlight-emp { background: linear-gradient(135deg, #059669, #ca8a04); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.glass-chip { background: var(--glass-bg); backdrop-filter: blur(12px); border: 1px solid var(--glass-border); padding: 10px 20px; border-radius: 100px; font-size: 0.875rem; font-weight: 600; display: flex; align-items: center; gap: 8px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }
.status-dot { width: 8px; height: 8px; background: #10b981; border-radius: 50%; box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2); }
.metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 24px; margin-bottom: 24px; animation: slideUp 0.6s ease-out 0.1s both; }
.metric-glass { background: var(--glass-bg); backdrop-filter: blur(20px); border: 1px solid var(--glass-border); border-radius: 24px; padding: 24px; display: flex; align-items: flex-start; gap: 16px; box-shadow: 0 10px 30px -10px rgba(0,0,0,0.05); transition: all 0.3s ease; }
.card-hover:hover { transform: translateY(-4px) scale(1.01); box-shadow: 0 20px 40px -10px rgba(0,0,0,0.08); border-color: rgba(255,255,255,0.8); }
.metric-content p { margin: 0 0 4px; font-size: 0.875rem; font-weight: 500; color: var(--text-secondary); }
.metric-content h3 { margin: 0; font-size: 1.25rem; font-weight: 700; color: var(--text-primary); }
.layout-grid-full { display: grid; grid-template-columns: 1fr; gap: 24px; animation: slideUp 0.6s ease-out 0.2s both; }
.card-glass { background: var(--glass-bg); backdrop-filter: blur(20px); border: 1px solid var(--glass-border); border-radius: 24px; padding: 32px; box-shadow: 0 10px 30px -10px rgba(0,0,0,0.05); }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.section-header h2 { font-size: 1.25rem; font-weight: 700; margin: 0; }
.notice-count { font-size: 0.75rem; font-weight: 700; padding: 4px 12px; border-radius: 100px; background: #dcfce7; color: #059669; }
.notice-list { display: flex; flex-direction: column; gap: 12px; }
.notice-item { display: flex; justify-content: space-between; align-items: center; background: white; border-radius: 16px; padding: 24px; cursor: pointer; transition: all 0.2s; border: 1px solid rgba(0,0,0,0.02); }
.notice-item:hover { transform: translateY(-2px); box-shadow: 0 8px 16px rgba(0,0,0,0.04); border-color: #dcfce7; }
.notice-info h3 { margin: 0 0 4px; font-size: 1rem; font-weight: 600; color: var(--text-primary); }
.notice-info p { margin: 0; font-size: 0.875rem; color: var(--text-secondary); }
.notice-date { font-size: 0.875rem; font-weight: 500; color: #a1a1aa; }
.empty-state { text-align: center; padding: 40px; color: var(--text-secondary); }
@keyframes slideDown { from { opacity: 0; transform: translateY(-20px); } to { opacity: 1; transform: translateY(0); } }
@keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
</style>