<template>
  <div class="sa-page">
    <div class="paper-layer" aria-hidden="true">
      <span class="paper-grain"></span>
      <span class="paper-line"></span>
    </div>

    <section class="hero">
      <p class="kicker">SUPER ADMIN CONSOLE</p>
      <h1>超管总览</h1>
      <p class="subtitle">统一管理安全策略与审计轨迹，保持系统边界清晰可控。</p>
    </section>

    <section class="metric-grid" v-loading="loading">
      <article class="metric-card metric-card-accent">
        <p>待审批部门</p>
        <strong>{{ metrics.pendingDeptApprovalCount }}</strong>
        <span>人事提交的新部门请求</span>
      </article>
      <article class="metric-card">
        <p>启用策略</p>
        <strong>{{ metrics.enabledPolicyCount }}</strong>
        <span>当前白名单生效条目</span>
      </article>
      <article class="metric-card">
        <p>策略总数</p>
        <strong>{{ metrics.policyTotal }}</strong>
        <span>可进入策略页维护</span>
      </article>
      <article class="metric-card">
        <p>登录日志</p>
        <strong>{{ metrics.loginLogTotal }}</strong>
        <span>累计登录审计记录</span>
      </article>
      <article class="metric-card">
        <p>操作日志</p>
        <strong>{{ metrics.operationLogTotal }}</strong>
        <span>累计关键操作记录</span>
      </article>
    </section>

    <section class="nav-grid">
      <article class="nav-card" @click="go('/system/dept-approval')">
        <h3>部门审批</h3>
        <p>审核人事提交的新部门请求，并决定是否正式生效。</p>
        <el-button type="primary" text :icon="Stamp">进入页面</el-button>
      </article>
      <article class="nav-card" @click="go('/system/security-ip-policy')">
        <h3>安全策略</h3>
        <p>维护 IP 白名单策略，控制登录入口边界。</p>
        <el-button type="primary" text :icon="Lock">进入页面</el-button>
      </article>
      <article class="nav-card" @click="go('/system/login-log')">
        <h3>登录日志</h3>
        <p>查看成功与失败登录记录，快速追踪异常来源。</p>
        <el-button type="primary" text :icon="Notebook">进入页面</el-button>
      </article>
      <article class="nav-card" @click="go('/system/operation-log')">
        <h3>操作日志</h3>
        <p>审计关键业务动作，确保每次变更可回溯。</p>
        <el-button type="primary" text :icon="Document">进入页面</el-button>
      </article>
    </section>

    <section class="quick-table">
      <el-card shadow="never">
        <template #header>
          <div class="table-head">
            <span>最新操作日志</span>
            <el-button type="primary" text :icon="ArrowRight" @click="go('/system/operation-log')">查看全部</el-button>
          </div>
        </template>
        <el-table :data="recentOperations" size="small" stripe empty-text="暂无操作日志">
          <el-table-column prop="createTime" label="时间" width="180">
            <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column prop="username" label="用户" width="120" />
          <el-table-column prop="module" label="模块" width="160" />
          <el-table-column prop="action" label="动作" width="180" show-overflow-tooltip />
          <el-table-column prop="requestUri" label="请求路径" min-width="220" show-overflow-tooltip />
        </el-table>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Stamp, Lock, Notebook, Document } from '@element-plus/icons-vue'
import { getEnabledIpPoliciesAPI, getIpPolicyPageAPI } from '@/api/security'
import { getLoginLogPageAPI, getOperationLogPageAPI } from '@/api/audit'
import { getDeptPageAPI } from '@/api/system'

const router = useRouter()
const loading = ref(false)
const recentOperations = ref([])

const metrics = reactive({
  pendingDeptApprovalCount: 0,
  enabledPolicyCount: 0,
  policyTotal: 0,
  loginLogTotal: 0,
  operationLogTotal: 0
})

const formatTime = (val) => {
  if (!val) return '-'
  return String(val).replace('T', ' ')
}

const go = (path) => {
  router.push(path)
}

const loadDashboard = async () => {
  loading.value = true
  try {
    const [enabledRes, policyRes, loginRes, operationRes, deptApprovalRes] = await Promise.all([
      getEnabledIpPoliciesAPI(),
      getIpPolicyPageAPI({ pageNum: 1, pageSize: 1 }),
      getLoginLogPageAPI({ pageNum: 1, pageSize: 1 }),
      getOperationLogPageAPI({ pageNum: 1, pageSize: 5 }),
      getDeptPageAPI({ pageNum: 1, pageSize: 5, status: 1 })
    ])

    if (enabledRes.code !== 200) throw new Error(enabledRes.msg || '加载启用策略失败')
    if (policyRes.code !== 200) throw new Error(policyRes.msg || '加载策略总数失败')
    if (loginRes.code !== 200) throw new Error(loginRes.msg || '加载登录日志总数失败')
    if (operationRes.code !== 200) throw new Error(operationRes.msg || '加载操作日志失败')
    if (deptApprovalRes.code !== 200) throw new Error(deptApprovalRes.msg || '加载部门审批失败')

    metrics.enabledPolicyCount = Array.isArray(enabledRes.data) ? enabledRes.data.length : 0
    metrics.policyTotal = Number(policyRes.data?.total || 0)
    metrics.loginLogTotal = Number(loginRes.data?.total || 0)
    metrics.operationLogTotal = Number(operationRes.data?.total || 0)
    metrics.pendingDeptApprovalCount = Number(deptApprovalRes.data?.total || 0)
    recentOperations.value = operationRes.data?.records || []
  } catch (error) {
    ElMessage.error(error.message || '加载超管总览失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Cinzel:wght@500;700&family=Manrope:wght@400;500;700&display=swap');


.sa-page {
  --paper: #f4efe6;
  --paper-soft: #f8f3eb;
  --ink: #1f1f1d;
  --ink-soft: #6d675f;
  --accent: #2e4e66;

  position: relative;
  min-height: calc(100vh - 120px);
  padding: 22px;
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

.hero {
  position: relative;
  z-index: 1;
  margin-bottom: 18px;
}

.kicker {
  margin: 0 0 8px;
  font-size: 11px;
  letter-spacing: 0.3em;
  color: var(--accent);
}

.hero h1 {
  margin: 0;
  font-family: 'Cinzel', 'Times New Roman', serif;
  font-size: clamp(1.9rem, 3vw, 2.6rem);
}

.subtitle {
  margin: 8px 0 0;
  color: var(--ink-soft);
}

.metric-grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.metric-card-accent {
  border-color: rgba(46, 78, 102, 0.26);
  background: rgba(241, 247, 251, 0.9);
}

.metric-card {
  border: 1px solid rgba(44, 37, 31, 0.16);
  background: rgba(255, 255, 255, 0.5);
  border-radius: 4px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.metric-card p {
  margin: 0;
  color: #6c665e;
  font-size: 13px;
}

.metric-card strong {
  font-family: 'Cinzel', 'Times New Roman', serif;
  color: #1f1f1d;
  font-size: 1.7rem;
  line-height: 1;
}

.metric-card span {
  font-size: 12px;
  color: #777168;
}

.nav-grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.nav-card {
  border: 1px solid rgba(44, 37, 31, 0.16);
  background: rgba(255, 255, 255, 0.46);
  border-radius: 4px;
  padding: 14px;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.nav-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 18px rgba(44, 37, 31, 0.12);
}

.nav-card h3 {
  margin: 0 0 8px;
  font-family: 'Cinzel', 'Times New Roman', serif;
  font-size: 1.1rem;
}

.nav-card p {
  margin: 0 0 8px;
  color: #6d675f;
  min-height: 42px;
}

.quick-table {
  position: relative;
  z-index: 1;
}

.table-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.quick-table :deep(.el-card) {
  border: 1px solid rgba(44, 37, 31, 0.16);
  background: rgba(255, 255, 255, 0.62);
}

@media (max-width: 1100px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .nav-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .sa-page {
    padding: 14px;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>


