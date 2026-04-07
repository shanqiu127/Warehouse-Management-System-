<template>
  <div class="super-dashboard">
    <div class="dashboard-shell">
      <header class="hero-card">
        <div>
          <p class="eyebrow">SUPER ADMIN</p>
          <h1>{{ summary.realName || summary.username || '超级管理员' }}</h1>
          <p class="hero-desc">全局审计、登录安全与平台运行状态总览</p>
        </div>
      </header>

      <section class="metric-grid">
        <article class="metric-card">
          <span class="metric-label">本次登录</span>
          <strong>{{ formatTime(summary.currentLoginTime) }}</strong>
        </article>
        <article class="metric-card metric-card-danger">
          <span class="metric-label">24 小时系统错误</span>
          <strong>{{ summary.errorCount24h || 0 }}</strong>
        </article>
        <article class="metric-card metric-card-success">
          <span class="metric-label">数据库状态</span>
          <strong>{{ summary.dbStatus || '未知' }}</strong>
        </article>
      </section>

      <transition-group name="admin-reminder-fade" tag="div" class="admin-reminder-stack">
        <div
          v-if="showDeptApprovalReminder"
          key="deptApprovalReminder"
          class="admin-reminder admin-reminder--amber"
          role="status"
          aria-live="polite"
        >
          <button type="button" class="admin-reminder__close" aria-label="关闭提醒" @click="dismissDeptApprovalReminder">×</button>
          <div class="admin-reminder__eyebrow">人事部门审核提醒</div>
          <div class="admin-reminder__title">待审核任务：{{ deptApprovalReminder.count }}</div>
          <p class="admin-reminder__text">你当前有待审核的部门审批任务，请及时处理。</p>
        </div>
      </transition-group>

      <section class="panel-card">
        <div class="panel-head">
          <h2>系统错误监控</h2>
        </div>
        <div v-if="summary.recentErrorLogs?.length" class="log-list">
          <article v-for="(log, idx) in summary.recentErrorLogs" :key="idx" class="log-item">
            <div class="log-top">
              <span>{{ formatTime(log.createTime) }}</span>
              <span class="log-badge">{{ log.statusCode }}</span>
              <span class="log-type">{{ log.errorType }}</span>
            </div>
            <div class="log-uri">{{ log.requestUri }}</div>
            <div class="log-message">{{ log.message }}</div>
          </article>
        </div>
        <div v-else class="empty-state">当前周期内未发现新的严重异常。</div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getHomeSummaryAPI } from '@/api/home'
import { getDeptPageAPI } from '@/api/system'
import { getToken } from '@/utils/auth'

const router = useRouter()
const summary = ref({ username: '', realName: '', dbStatus: '', currentLoginTime: null, lastLoginTime: null, errorCount24h: 0, recentErrorLogs: [] })
const deptApprovalReminder = ref({ count: 0, signature: '' })
const dismissedDeptApprovalSignature = ref('')

const DEPT_APPROVAL_REMINDER_KEY_PREFIX = 'superadmin-dept-approval-reminder'

const showDeptApprovalReminder = computed(() => {
  return Number(deptApprovalReminder.value.count) > 0
    && dismissedDeptApprovalSignature.value !== deptApprovalReminder.value.signature
})

const formatTime = (val) => {
  if (!val) return '--'
  return String(val).replace('T', ' ').substring(0, 19)
}

const getReminderStorageKey = (userId) => {
  const token = getToken()
  return `${DEPT_APPROVAL_REMINDER_KEY_PREFIX}:${userId}:${token}`
}

const syncDismissedReminder = () => {
  if (!summary.value.userId) {
    dismissedDeptApprovalSignature.value = ''
    return
  }
  dismissedDeptApprovalSignature.value = sessionStorage.getItem(getReminderStorageKey(summary.value.userId)) || ''
}

const dismissDeptApprovalReminder = () => {
  if (!summary.value.userId || !deptApprovalReminder.value.signature) {
    return
  }
  sessionStorage.setItem(getReminderStorageKey(summary.value.userId), deptApprovalReminder.value.signature)
  dismissedDeptApprovalSignature.value = deptApprovalReminder.value.signature
}

const loadSummary = async () => {
  try {
    const res = await getHomeSummaryAPI()
    if (res.code === 200) {
      summary.value = { ...summary.value, ...res.data }
      syncDismissedReminder()
    }
  } catch {
    ElMessage.error('首页摘要加载失败')
  }
}

const loadDeptApprovalReminder = async () => {
  try {
    const res = await getDeptPageAPI({ pageNum: 1, pageSize: 50, status: 1 })
    if (res.code !== 200) {
      throw new Error(res.msg || '部门审批提醒加载失败')
    }

    const records = Array.isArray(res.data?.records) ? res.data.records : []
    const signature = Number(res.data?.total || 0) > 0
      ? `${Number(res.data?.total || 0)}:${records.map(item => item.id).filter(Boolean).join(',')}`
      : ''

    deptApprovalReminder.value = {
      count: Number(res.data?.total || 0),
      signature
    }
  } catch (error) {
    ElMessage.error(error.message || '部门审批提醒加载失败')
  }
}

onMounted(() => {
  loadSummary()
  loadDeptApprovalReminder()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');

.super-dashboard {
  min-height: calc(100vh - 100px);
  padding: 36px;
  background:
    radial-gradient(circle at top right, rgba(185, 28, 28, 0.12), transparent 28%),
    radial-gradient(circle at bottom left, rgba(37, 99, 235, 0.1), transparent 28%),
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
.log-item {
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
  color: #b91c1c;
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

.metric-card-danger {
  border-color: rgba(248, 113, 113, 0.35);
  background: rgba(255, 255, 255, 0.96);
}

.metric-card-success {
  border-color: rgba(74, 222, 128, 0.42);
  background: rgba(240, 253, 244, 0.96);
}

.metric-label {
  font-size: 0.86rem;
  color: #64748b;
}

.metric-card strong {
  font-size: 1.35rem;
  color: #0f172a;
}

.admin-reminder-stack {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 60;
  display: grid;
  gap: 14px;
  pointer-events: none;
}

.admin-reminder {
  position: relative;
  width: min(336px, calc(100vw - 32px));
  padding: 18px 20px 16px;
  border-radius: 22px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 22px 44px -24px rgba(15, 23, 42, 0.34);
  pointer-events: auto;
  animation: admin-reminder-float 3.4s ease-in-out infinite;
}

.admin-reminder--amber {
  background: linear-gradient(145deg, rgba(255, 251, 235, 0.98), rgba(255, 255, 255, 0.96));
}

.admin-reminder__close {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
  color: #475569;
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
}

.admin-reminder__close:hover {
  background: rgba(15, 23, 42, 0.1);
}

.admin-reminder__eyebrow {
  margin-bottom: 8px;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.admin-reminder--amber .admin-reminder__eyebrow {
  color: #b45309;
}

.admin-reminder__title {
  color: #111827;
  font-size: 1.18rem;
  font-weight: 800;
}

.admin-reminder__text {
  margin: 8px 0 0;
  color: #475569;
  font-size: 0.9rem;
  line-height: 1.6;
}

.admin-reminder-fade-enter-active,
.admin-reminder-fade-leave-active,
.admin-reminder-fade-move {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.admin-reminder-fade-enter-from,
.admin-reminder-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

@keyframes admin-reminder-float {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-6px);
  }
}

.metric-card-success strong {
  color: #15803d;
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

.log-list {
  display: grid;
  gap: 12px;
}

.log-item {
  border-radius: 18px;
  padding: 18px 20px;
}

.log-top {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  color: #64748b;
  font-size: 0.9rem;
  margin-bottom: 12px;
}

.log-badge {
  padding: 4px 8px;
  border-radius: 8px;
  background: #fee2e2;
  color: #b91c1c;
  font-weight: 700;
}

.log-type {
  padding: 4px 8px;
  border-radius: 8px;
  background: #f1f5f9;
  color: #334155;
}

.log-uri {
  color: #0f172a;
  font-weight: 700;
  margin-bottom: 6px;
}

.log-message,
.empty-state {
  color: #64748b;
  font-size: 0.94rem;
}

@media (max-width: 960px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .hero-card,
  .panel-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .admin-reminder-stack {
    right: 16px;
    bottom: 16px;
  }

  .admin-reminder {
    width: calc(100vw - 32px);
  }
}
</style>