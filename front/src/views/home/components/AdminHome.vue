<template>
  <div class="admin-dashboard">
    <div class="dashboard-shell">
      <header class="hero-card">
        <div>
          <p class="eyebrow">DEPARTMENT ADMIN</p>
          <h1>{{ summary.realName || userStore.realName || '部门管理员' }}</h1>
          <p class="hero-desc">{{ deptLabel }} · {{ moduleLabel }}</p>
        </div>
        <div class="hero-meta">
          <span>{{ formatTime(summary.currentLoginTime) }}</span>
        </div>
      </header>

      <section class="metric-grid">
        <article v-for="item in metricCards" :key="item.label" class="metric-card">
          <span class="metric-label">{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </article>
      </section>

      <section class="panel-grid">
        <article class="panel-card">
          <div class="panel-head">
            <h2>快捷入口</h2>
            <span>{{ quickActions.length }} 项</span>
          </div>
          <div class="action-grid">
            <button v-for="item in quickActions" :key="item.path" type="button" class="action-card" @click="go(item.path)">
              <span class="action-title">{{ item.title }}</span>
              <span class="action-text">{{ item.description }}</span>
            </button>
          </div>
        </article>

        <article class="panel-card" v-loading="noticeLoading">
          <div class="panel-head">
            <h2>最新公告</h2>
            <span>按当前账号可见范围过滤</span>
          </div>
          <div v-if="noticeList.length" class="notice-list">
            <button v-for="item in noticeList" :key="item.id" type="button" class="notice-item" @click="openNotice(item)">
              <div>
                <div class="notice-title">{{ item.title }}</div>
                <div class="notice-meta">{{ audienceLabel(item) }}</div>
              </div>
              <span class="notice-date">{{ formatTime(item.date || item.publishTime) }}</span>
            </button>
          </div>
          <div v-else class="empty-state">暂无可见公告</div>
        </article>
      </section>

      <el-dialog v-model="noticeDialogVisible" title="公告详情" width="620px">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="标题">{{ noticeDetail.title || '-' }}</el-descriptions-item>
          <el-descriptions-item label="受众">{{ audienceLabel(noticeDetail) }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ formatTime(noticeDetail.date || noticeDetail.publishTime) }}</el-descriptions-item>
          <el-descriptions-item label="发布人">{{ noticeDetail.author || noticeDetail.publisher || '-' }}</el-descriptions-item>
          <el-descriptions-item label="正文">
            <p class="notice-content">{{ noticeDetail.content || '-' }}</p>
          </el-descriptions-item>
        </el-descriptions>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getHomeSummaryAPI } from '@/api/home'
import { getAdminHomeLatestNoticeAPI, getNoticeDetailAPI } from '@/api/system'
import { useUserStore } from '@/stores/user'
import { normalizeDeptCode } from '@/utils/auth'

const router = useRouter()
const userStore = useUserStore()

const summary = ref({
  username: '',
  realName: '',
  role: '',
  deptName: '',
  currentLoginTime: null,
  lastLoginTime: null,
  lowStockCount: 0,
  zeroStockCount: 0
})
const noticeList = ref([])
const noticeLoading = ref(false)
const noticeDialogVisible = ref(false)
const noticeDetail = ref({})

const deptCode = computed(() => normalizeDeptCode(userStore.deptCode))
const deptLabel = computed(() => summary.value.deptName || userStore.deptName || '未分配部门')

const moduleLabelMap = {
  finance: '财务统计与公告分发',
  sales: '销售作业、退货与预警协同',
  warehouse: '仓储资料、预警与审批中心',
  purchase: '采购进货、退货与预警协同',
  hr: '人事组织与账号维护中心'
}

const moduleLabel = computed(() => moduleLabelMap[deptCode.value] || '部门管理员工作台')

const metricCards = computed(() => {
  const baseCards = [
    { label: '当前部门', value: deptLabel.value },
    { label: '公告数量', value: noticeList.value.length },
  ]

  if (deptCode.value === 'hr' || deptCode.value === 'finance') {
    return [...baseCards, { label: '上次登录', value: formatTime(summary.value.lastLoginTime) }]
  }

  return [
    ...baseCards,
    { label: '低库存预警', value: summary.value.lowStockCount ?? 0 },
    { label: '零库存预警', value: summary.value.zeroStockCount ?? 0 },
    { label: '上次登录', value: formatTime(summary.value.lastLoginTime) }
  ]
})

const quickActions = computed(() => {
  const commonActions = [
    { path: '/system/notice', title: '公告管理', description: '维护当前账号可发布的公告内容' },
    { path: '/system/user', title: '用户部门管理', description: '维护本部门用户账号与状态' }
  ]

  const deptActionsMap = {
    finance: [
      { path: '/business/sales-chart', title: '销售统计图表', description: '查看销售额、退货额与毛利趋势' }
    ],
    sales: [
      { path: '/business/sales', title: '商品销售', description: '处理销售单据与审批结果回看' },
      { path: '/business/sales-return', title: '销售退货', description: '处理销售退货业务与状态追踪' },
      { path: '/business/stock-warning', title: '预警中心', description: '查看低库存与零库存商品明细' }
    ],
    warehouse: [
      { path: '/base/supplier', title: '供应商管理', description: '维护供应商信息与合作资料' },
      { path: '/base/goods', title: '商品资料管理', description: '维护库存商品资料与阈值' },
      { path: '/business/stock-warning', title: '预警中心', description: '集中查看库存异常并跟进处理' },
      { path: '/system/void-approval', title: '作废审批', description: '审核采购与销售历史单据作废申请' }
    ],
    purchase: [
      { path: '/business/purchase', title: '商品进货', description: '处理进货单据与审批结果回看' },
      { path: '/business/purchase-return', title: '进货退货', description: '处理进货退货与状态追踪' },
      { path: '/business/stock-warning', title: '预警中心', description: '查看低库存与零库存商品明细' }
    ],
    hr: [
      { path: '/system/dept', title: '全部门管理', description: '维护部门资料与负责人信息' },
      { path: '/system/employee', title: '全员工管理', description: '维护跨部门员工档案资料' },
      { path: '/system/hr-chart', title: '员工分布图表', description: '查看在职员工分布与部门人口构成' }
    ]
  }

  return [...(deptActionsMap[deptCode.value] || []), ...commonActions]
})

const formatTime = (val) => {
  if (!val) return '--'
  return String(val).replace('T', ' ').substring(0, 19)
}

const audienceLabel = (row = {}) => {
  if (row.targetRole === 'all') return '全员公告'
  if (row.targetRole === 'admin') {
    return row.targetDeptName ? `${row.targetDeptName} 管理员` : '管理员公告'
  }
  if (row.targetRole === 'employee') {
    return row.targetDeptName ? `${row.targetDeptName} 员工` : '员工公告'
  }
  return '未设置'
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
    const res = await getAdminHomeLatestNoticeAPI(4)
    if (res.code === 200) {
      noticeList.value = res.data || []
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
    if (res.code !== 200) {
      throw new Error(res.msg || '公告详情加载失败')
    }
    noticeDetail.value = res.data || {}
    noticeDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '公告详情加载失败')
  }
}

const go = (path) => {
  router.push(path)
}

onMounted(() => {
  loadSummary()
  loadNotices()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');

.admin-dashboard {
  min-height: calc(100vh - 100px);
  padding: 36px;
  background:
    radial-gradient(circle at top right, rgba(14, 116, 144, 0.14), transparent 28%),
    radial-gradient(circle at bottom left, rgba(249, 115, 22, 0.1), transparent 28%),
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
.action-card,
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
  color: #0f766e;
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
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
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

.panel-grid {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 24px;
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

.panel-head span {
  color: #64748b;
  font-size: 0.88rem;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.action-card {
  border-radius: 20px;
  padding: 20px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.action-card:hover,
.notice-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 22px 45px -34px rgba(15, 23, 42, 0.5);
}

.action-title {
  display: block;
  color: #0f172a;
  font-weight: 700;
  margin-bottom: 8px;
}

.action-text {
  display: block;
  color: #475569;
  font-size: 0.92rem;
  line-height: 1.6;
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
}

.notice-title {
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 8px;
}

.notice-meta,
.notice-date,
.empty-state {
  color: #64748b;
  font-size: 0.9rem;
}

.notice-content {
  white-space: pre-wrap;
  line-height: 1.7;
  margin: 0;
}

@media (max-width: 960px) {
  .panel-grid,
  .action-grid {
    grid-template-columns: 1fr;
  }

  .hero-card {
    flex-direction: column;
  }
}
</style>
