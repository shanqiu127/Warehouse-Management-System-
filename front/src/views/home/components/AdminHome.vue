<template>
  <div class="admin-dashboard">
    <div class="glass-orb shape-1"></div>
    <div class="glass-orb shape-2"></div>
    
    <div class="content-wrapper">
      <header class="admin-header">
        <div class="header-text">
          <span class="eyebrow">WORKSPACE / MANAGER</span>
          <h1>Welcome back, <br/> <span class="highlight">{{ summary.realName || summary.username || 'Admin' }}</span></h1>
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
          <div class="metric-icon">
            <svg viewBox="0 0 24 24" fill="none" class="icon"><path d="M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z" fill="currentColor" fill-opacity="0.1"/><path d="M12 16V12M12 8H12.01M22 12C22 17.5228 17.5228 22 12 22C6.47715 22 2 17.5228 2 12C2 6.47715 6.47715 2 12 2C17.5228 2 22 6.47715 22 12Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </div>
          <div class="metric-content">
            <p>Current Session</p>
            <h3>{{ formatTime(summary.currentLoginTime) }}</h3>
          </div>
        </div>
        
        <div class="metric-glass card-hover">
          <div class="metric-icon">
            <svg viewBox="0 0 24 24" fill="none" class="icon"><path d="M12 8V12L15 15M22 12C22 17.5228 17.5228 22 12 22C6.47715 22 2 17.5228 2 12C2 6.47715 6.47715 2 12 2C17.5228 2 22 6.47715 22 12Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </div>
          <div class="metric-content">
            <p>Previous Login</p>
            <h3>{{ formatTime(summary.lastLoginTime) }}</h3>
          </div>
        </div>

        <div class="metric-glass card-hover">
          <div class="metric-icon">
            <svg viewBox="0 0 24 24" fill="none" class="icon"><path d="M9 12L11 14L15 10M22 12C22 17.5228 17.5228 22 12 22C6.47715 22 2 17.5228 2 12C2 6.47715 6.47715 2 12 2C17.5228 2 22 6.47715 22 12Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </div>
          <div class="metric-content">
            <p>Role</p>
            <h3>{{ roleLabel }}</h3>
          </div>
        </div>

        <div class="metric-glass card-hover" @click="go('/system/void-approval')" style="cursor: pointer;">
          <div class="metric-icon approval-alert-icon">
            <svg viewBox="0 0 24 24" fill="none" class="icon"><path d="M12 9V13M12 17H12.01M10.29 3.86L1.82 18A2 2 0 0 0 3.55 21H20.45A2 2 0 0 0 22.18 18L13.71 3.86A2 2 0 0 0 10.29 3.86Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </div>
          <div class="metric-content">
            <p>Pending Approvals</p>
            <h3>{{ pendingApprovalCount }}</h3>
          </div>
        </div>
      </section>

      <div class="layout-grid">
        <section class="quick-actions card-glass">
          <h2>Quick Actions</h2>
          <div class="actions-wrapper">
            <div class="action-btn" @click="go('/business/sales-chart')">
              <div class="icon-wrap bg-blue"><div class="circle"></div></div>
              <span>Analytics</span>
            </div>
            <div class="action-btn" @click="go('/system/void-approval')">
              <div class="icon-wrap bg-red"><div class="circle"></div></div>
              <span>Approvals</span>
            </div>
            <div class="action-btn" @click="go('/system/notice')">
              <div class="icon-wrap bg-purple"><div class="circle"></div></div>
              <span>Notices</span>
            </div>
            <div class="action-btn" @click="go('/system/user')">
              <div class="icon-wrap bg-orange"><div class="circle"></div></div>
              <span>Users</span>
            </div>
            <div class="action-btn" @click="go('/system/employee')">
              <div class="icon-wrap bg-green"><div class="circle"></div></div>
              <span>Employees</span>
            </div>
          </div>
        </section>

        <section class="system-status card-glass" v-loading="noticeLoading">
          <h2>Latest Notices</h2>
          <div class="status-list">
            <div class="status-item" v-for="item in noticeList" :key="item.id">
              <div>
                <div class="status-label">{{ item.title }}</div>
                <div class="status-sub">{{ formatTime(item.date || item.publishTime) }}</div>
              </div>
              <div class="status-badge success">公告</div>
            </div>
            <div class="status-empty" v-if="!noticeLoading && noticeList.length === 0">暂无公告数据</div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getHomeSummaryAPI } from '@/api/home'
import { getApprovalPendingCountAPI, getNoticePageAPI } from '@/api/system'

const router = useRouter()
const summary = ref({
  username: '',
  realName: '',
  role: '',
  currentLoginTime: null,
  lastLoginTime: null,
  lowStockCount: 0,
  zeroStockCount: 0
})
const noticeList = ref([])
const noticeLoading = ref(false)
const pendingApprovalCount = ref(0)

const roleLabel = 'Administrator'

const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }
const nowDateText = new Date().toLocaleDateString('en-US', options)

const formatTime = (val) => {
  if (!val) return '--'
  return String(val).replace('T', ' ')
}

const loadSummary = async () => {
  try {
    const res = await getHomeSummaryAPI()
    if (res.code === 200) {
      summary.value = { ...summary.value, ...res.data }
    }
  } catch (error) {
    ElMessage.error('Failed to load summary')
  }
}

const loadNotices = async () => {
  noticeLoading.value = true
  try {
    const res = await getNoticePageAPI({ pageNum: 1, pageSize: 3, status: 1 })
    if (res.code === 200) {
      noticeList.value = res.data?.records || []
    }
  } catch (error) {
    ElMessage.error('Failed to load notices')
  } finally {
    noticeLoading.value = false
  }
}

const loadPendingCount = async () => {
  try {
    const res = await getApprovalPendingCountAPI()
    if (res.code === 200) {
      pendingApprovalCount.value = Number(res.data || 0)
    }
  } catch (error) {
    ElMessage.error('Failed to load pending approvals')
  }
}

const go = (path) => {
  router.push(path)
}

onMounted(() => {
  loadSummary()
  loadNotices()
  loadPendingCount()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');

.admin-dashboard {
  --bg-color: #f6f8fb;
  --text-primary: #111827;
  --text-secondary: #6b7280;
  --glass-bg: rgba(255, 255, 255, 0.7);
  --glass-border: rgba(255, 255, 255, 0.5);
  
  position: relative;
  min-height: calc(100vh - 100px);
  background: var(--bg-color);
  font-family: 'Plus Jakarta Sans', sans-serif;
  color: var(--text-primary);
  overflow: hidden;
  padding: 40px;
  z-index: 1;
}

.glass-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  z-index: -1;
  opacity: 0.6;
}

.shape-1 {
  width: 400px;
  height: 400px;
  background: #e0e7ff;
  top: -100px;
  right: -100px;
}

.shape-2 {
  width: 500px;
  height: 500px;
  background: #fdf4ff;
  bottom: -200px;
  left: -200px;
}

.content-wrapper {
  max-width: 1280px;
  margin: 0 auto;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 48px;
  animation: slideDown 0.6s ease-out;
}

.eyebrow {
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--text-secondary);
  text-transform: uppercase;
  margin-bottom: 12px;
  display: block;
}

.admin-header h1 {
  font-size: 3rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  line-height: 1.1;
  margin: 0;
}

.highlight {
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.glass-chip {
  background: var(--glass-bg);
  backdrop-filter: blur(12px);
  border: 1px solid var(--glass-border);
  padding: 10px 20px;
  border-radius: 100px;
  font-size: 0.875rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);
}

.status-dot {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 24px;
  margin-bottom: 24px;
  animation: slideUp 0.6s ease-out 0.1s both;
}

.metric-glass {
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  border: 1px solid var(--glass-border);
  border-radius: 24px;
  padding: 24px;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  box-shadow: 0 10px 30px -10px rgba(0,0,0,0.05);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.card-hover:hover {
  transform: translateY(-4px) scale(1.01);
  box-shadow: 0 20px 40px -10px rgba(0,0,0,0.08);
  border-color: rgba(255,255,255,0.8);
}

.metric-icon {
  width: 48px;
  height: 48px;
  background: white;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4f46e5;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);
}

.approval-alert-icon {
  background: #fee2e2;
  color: #dc2626;
}

.metric-icon .icon {
  width: 24px;
  height: 24px;
}

.metric-content p {
  margin: 0 0 4px;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-secondary);
}

.metric-content h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-primary);
}

.layout-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
  animation: slideUp 0.6s ease-out 0.2s both;
}

.card-glass {
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  border: 1px solid var(--glass-border);
  border-radius: 24px;
  padding: 32px;
  box-shadow: 0 10px 30px -10px rgba(0,0,0,0.05);
}

.card-glass h2 {
  font-size: 1.25rem;
  font-weight: 700;
  margin: 0 0 24px;
}

.actions-wrapper {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 16px;
}

.action-btn {
  background: white;
  border-radius: 20px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid rgba(0,0,0,0.02);
}

.action-btn:hover {
  background: #f8fafc;
  transform: translateY(-2px);
  box-shadow: 0 8px 16px -8px rgba(0,0,0,0.05);
}

.icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-wrap .circle {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.8;
}

.bg-blue { background: #e0e7ff; color: #4f46e5; }
.bg-purple { background: #fae8ff; color: #c026d3; }
.bg-orange { background: #ffedd5; color: #ea580c; }
.bg-green { background: #d1fae5; color: #059669; }
.bg-red { background: #fee2e2; color: #dc2626; }

.action-btn span {
  font-weight: 600;
  font-size: 0.875rem;
  color: var(--text-primary);
}

.status-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: white;
  border-radius: 16px;
}

.status-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
}

.status-sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-secondary);
}

.status-badge {
  font-size: 0.75rem;
  font-weight: 700;
  padding: 4px 12px;
  border-radius: 100px;
}

.status-badge.success {
  background: #dcfce7;
  color: #059669;
}

.status-empty {
  color: var(--text-secondary);
  font-size: 13px;
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 992px) {
  .layout-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .admin-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  .admin-dashboard {
    padding: 20px;
  }
}
</style>
