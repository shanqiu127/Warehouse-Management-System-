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

      <section class="panel-card">
        <div class="panel-head">
          <h2>系统错误监控</h2>
          <button type="button" class="audit-link" @click="go('/system/operation-log')">查看完整审计</button>
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
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getHomeSummaryAPI } from '@/api/home'

const router = useRouter()
const summary = ref({ username: '', realName: '', dbStatus: '', currentLoginTime: null, lastLoginTime: null, errorCount24h: 0, recentErrorLogs: [] })

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

const go = (path) => router.push(path)

onMounted(() => {
  loadSummary()
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

.audit-link {
  border: none;
  background: rgba(185, 28, 28, 0.08);
  color: #b91c1c;
  border-radius: 999px;
  padding: 8px 14px;
  cursor: pointer;
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
}
</style>