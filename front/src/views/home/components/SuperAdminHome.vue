<template>
  <div class="admin-dashboard">
    <div class="glass-orb shape-1" style="background: #fee2e2;"></div>
    <div class="glass-orb shape-2" style="background: #e0e7ff;"></div>
    
    <div class="content-wrapper">
      <header class="admin-header">
        <div class="header-text">
          <span class="eyebrow">WORKSPACE / SUPER ADMIN</span>
          <h1>Welcome back, <br/> <span class="highlight-sa">{{ summary.realName || summary.username || 'SuperAdmin' }}</span></h1>
        </div>
        <div class="header-date">
          <div class="glass-chip">
            <span class="status-dot" :class="summary.dbStatus === '正常' ? 'ok' : 'bad'"></span>
            DB Status: {{ summary.dbStatus || 'UNKNOWN' }}
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
        <div class="metric-glass card-hover error-metric">
          <div class="metric-content">
            <p style="color: #ef4444;">System Errors (24h)</p>
            <h3 style="color: #dc2626;">{{ summary.errorCount24h || 0 }} Event(s)</h3>
          </div>
        </div>
      </section>

      <div class="layout-grid-full">
        <section class="system-status card-glass">
          <div class="section-header">
            <h2>System Error Monitor</h2>
            <div class="action-btn-small" @click="go('/system/operation-log')">Full Audit</div>
          </div>
          <div class="status-list">
            <template v-if="summary.recentErrorLogs && summary.recentErrorLogs.length">
              <div class="status-item error-item" v-for="(log, idx) in summary.recentErrorLogs" :key="idx">
                <div class="log-meta">
                  <span class="time">{{ formatTime(log.createTime) }}</span>
                  <span class="badge red">{{ log.statusCode }}</span>
                  <span class="badge dark">{{ log.errorType }}</span>
                </div>
                <div class="log-detail">
                  <div class="uri">{{ log.requestUri }}</div>
                  <div class="msg">{{ log.message }}</div>
                </div>
              </div>
            </template>
            <div class="empty-state" v-else>
              <div class="status-badge success">All Systems Operational</div>
              <p>No critical anomalies detected in the current cycle.</p>
            </div>
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

const router = useRouter()
const summary = ref({ username: '', realName: '', dbStatus: '', currentLoginTime: null, lastLoginTime: null, errorCount24h: 0, recentErrorLogs: [] })

const formatTime = (val) => {
  if (!val) return '--'
  return String(val).replace('T', ' ').substring(0, 19)
}

const loadSummary = async () => {
  try {
    const res = await getHomeSummaryAPI()
    if (res.code === 200) summary.value = { ...summary.value, ...res.data }
  } catch (error) {
    ElMessage.error('Failed to load SuperAdmin summary')
  }
}

const go = (path) => router.push(path)
onMounted(() => { loadSummary() })
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');
.admin-dashboard {
  --bg-color: #f6f8fb;
  --text-primary: #111827;
  --text-secondary: #6b7280;
  --glass-bg: rgba(255, 255, 255, 0.7);
  --glass-border: rgba(255, 255, 255, 0.5);
  position: relative; min-height: calc(100vh - 100px); background: var(--bg-color); font-family: 'Plus Jakarta Sans', sans-serif; color: var(--text-primary); overflow: hidden; padding: 40px; z-index: 1;
}
.glass-orb { position: absolute; border-radius: 50%; filter: blur(80px); z-index: -1; opacity: 0.6; }
.shape-1 { width: 500px; height: 500px; top: -150px; right: -100px; }
.shape-2 { width: 600px; height: 600px; bottom: -200px; left: -200px; }
.content-wrapper { max-width: 1280px; margin: 0 auto; }
.admin-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 48px; animation: slideDown 0.6s ease-out; }
.eyebrow { font-size: 0.75rem; font-weight: 700; letter-spacing: 0.1em; color: var(--text-secondary); text-transform: uppercase; margin-bottom: 12px; display: block; }
.admin-header h1 { font-size: 3rem; font-weight: 800; letter-spacing: -0.02em; line-height: 1.1; margin: 0; }
.highlight-sa { background: linear-gradient(135deg, #b91c1c, #4338ca); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.glass-chip { background: var(--glass-bg); backdrop-filter: blur(12px); border: 1px solid var(--glass-border); padding: 10px 20px; border-radius: 100px; font-size: 0.875rem; font-weight: 600; display: flex; align-items: center; gap: 8px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }
.status-dot { width: 8px; height: 8px; border-radius: 50%; }
.status-dot.ok { background: #10b981; box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2); }
.status-dot.bad { background: #ef4444; box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.2); }
.metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 24px; margin-bottom: 24px; animation: slideUp 0.6s ease-out 0.1s both; }
.metric-glass { background: var(--glass-bg); backdrop-filter: blur(20px); border: 1px solid var(--glass-border); border-radius: 24px; padding: 24px; display: flex; align-items: flex-start; gap: 16px; box-shadow: 0 10px 30px -10px rgba(0,0,0,0.05); transition: all 0.3s ease; }
.card-hover:hover { transform: translateY(-4px) scale(1.01); box-shadow: 0 20px 40px -10px rgba(0,0,0,0.08); border-color: rgba(255,255,255,0.8); }
.error-metric { background: rgba(254, 242, 242, 0.6); border-color: rgba(254, 226, 226, 1); }
.metric-content p { margin: 0 0 4px; font-size: 0.875rem; font-weight: 500; color: var(--text-secondary); }
.metric-content h3 { margin: 0; font-size: 1.25rem; font-weight: 700; color: var(--text-primary); }
.layout-grid-full { display: grid; grid-template-columns: 1fr; gap: 24px; animation: slideUp 0.6s ease-out 0.2s both; }
.card-glass { background: var(--glass-bg); backdrop-filter: blur(20px); border: 1px solid var(--glass-border); border-radius: 24px; padding: 32px; box-shadow: 0 10px 30px -10px rgba(0,0,0,0.05); }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.section-header h2 { font-size: 1.25rem; font-weight: 700; margin: 0; }
.action-btn-small { font-size: 0.875rem; font-weight: 600; color: #4338ca; cursor: pointer; padding: 6px 12px; border-radius: 100px; background: rgba(67, 56, 202, 0.1); transition: all 0.2s; }
.action-btn-small:hover { background: rgba(67, 56, 202, 0.2); }
.status-list { display: flex; flex-direction: column; gap: 12px; }
.status-item { background: white; border-radius: 16px; padding: 20px; display: flex; flex-direction: column; gap: 12px; border: 1px solid transparent; transition: all 0.2s; }
.status-item.error-item:hover { border-color: #fee2e2; transform: translateX(4px); box-shadow: 0 4px 12px rgba(239,68,68,0.05); }
.log-meta { display: flex; gap: 12px; align-items: center; font-size: 0.75rem; font-family: monospace; }
.log-meta .time { color: var(--text-secondary); }
.badge { padding: 4px 8px; border-radius: 6px; font-weight: 700; }
.badge.red { background: #fee2e2; color: #b91c1c; }
.badge.dark { background: #f3f4f6; color: #374151; }
.log-detail .uri { font-size: 0.875rem; font-weight: 600; color: var(--text-primary); margin-bottom: 4px; }
.log-detail .msg { font-size: 0.875rem; color: var(--text-secondary); }
.empty-state { text-align: center; padding: 40px; color: var(--text-secondary); }
@keyframes slideDown { from { opacity: 0; transform: translateY(-20px); } to { opacity: 1; transform: translateY(0); } }
@keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
</style>