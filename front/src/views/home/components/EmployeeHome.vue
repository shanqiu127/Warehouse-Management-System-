<template>
  <div class="employee-dashboard">
    <div class="dashboard-shell">
      <header class="hero-card">
        <div class="hero-copy">
          <p class="eyebrow">EMPLOYEE WORKBENCH</p>
          <h1>{{ displayName }}</h1>
          <p class="hero-desc">{{ deptLabel }} · {{ deptScene.title }}</p>
          <p class="hero-subtitle">{{ deptScene.subtitle }}</p>
        </div>
        <div class="hero-meta">
          <span class="hero-meta-label">当前登录时间</span>
          <strong>{{ formatTime(summary.currentLoginTime) }}</strong>
        </div>
      </header>

      <section class="metric-grid">
        <article v-for="item in metricCards" :key="item.label" class="metric-card">
          <span class="metric-label">{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <span class="metric-desc">{{ item.description }}</span>
        </article>
      </section>

      <section class="panel-grid panel-grid--double">
        <article class="panel-card">
          <div class="panel-head">
            <h2>我的档案</h2>
            <span>编号岗位只读，联系方式可修改</span>
          </div>
          <div class="info-grid">
            <div v-for="item in profileReadonlyItems" :key="item.label" class="info-row">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
          <div v-if="!shouldShowContactForm" class="contact-display-block">
            <div class="contact-display-head">
              <div>
                <div class="contact-display-title">已保存联系方式</div>
                <div class="contact-display-text">当前手机号和邮箱已完善，如需调整可重新进入编辑</div>
              </div>
            </div>
            <div class="info-grid">
              <div v-for="item in contactDisplayItems" :key="item.label" class="info-row">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
            <div class="contact-display-actions">
              <el-button class="contact-edit-button" plain native-type="button" @click="startEditContact">修改</el-button>
            </div>
          </div>
          <el-form v-else ref="contactFormRef" :model="contactForm" :rules="contactRules" class="contact-form" label-position="top">
            <div class="contact-editor">
              <div class="contact-editor-head">
                <span class="contact-editor-title">联系方式维护</span>
                <span class="contact-editor-text">支持修改手机号和邮箱，保存后即时生效</span>
              </div>
              <div class="contact-field-grid">
                <div class="contact-field">
                  <span class="contact-field-label">手机号</span>
                  <el-form-item prop="phone" class="contact-form-item">
                    <el-input v-model="contactForm.phone" clearable placeholder="请输入手机号" />
                  </el-form-item>
                </div>
                <div class="contact-field">
                  <span class="contact-field-label">邮箱</span>
                  <el-form-item prop="email" class="contact-form-item">
                    <el-input v-model="contactForm.email" clearable placeholder="请输入邮箱" />
                  </el-form-item>
                </div>
              </div>
              <div class="contact-actions">
                <el-button native-type="button" @click="resetContactForm">取消</el-button>
                <el-button class="contact-save-button" native-type="button" :loading="savingContact" @click="handleSaveContact">保存联系方式</el-button>
              </div>
            </div>
          </el-form>
        </article>

        <article class="panel-card">
          <div class="panel-head">
            <h2>部门联络信息</h2>
            <span>用于快速协同</span>
          </div>
          <div class="info-grid">
            <div v-for="item in deptContactItems" :key="item.label" class="info-row">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </article>
      </section>

      <section v-if="workRequirements.length" class="panel-grid">
        <article class="panel-card panel-card--accent">
          <div class="panel-head">
            <h2>工作要求</h2>
            <span>{{ workRequirements.length }} 项</span>
          </div>
          <div class="wr-list">
            <button
              v-for="item in sortedWorkRequirements"
              :key="item.assignId"
              type="button"
              class="wr-item"
              :class="{ 'wr-item--done': item.status === 3, 'wr-item--rejected': item.status === 4, 'wr-item--overdue': item.overdueCurrent }"
              @click="goWorkRequirement(item)"
            >
              <div class="wr-item-main">
                <span class="wr-item-content">{{ item.content }}</span>
                <div class="wr-item-tags">
                  <el-tag v-if="item.overdueLabel && item.overdueLabel !== '正常'" :type="overdueTagType(item.overdueLabel)" size="small">{{ item.overdueLabel }}</el-tag>
                  <el-tag :type="wrTagType(item.status)" size="small">{{ item.statusLabel }}</el-tag>
                </div>
              </div>
              <span class="wr-item-deadline">截止：{{ formatTime(item.endTime) }}</span>
            </button>
          </div>
        </article>
      </section>

      <section class="panel-grid">
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

      <transition name="task-reminder-fade">
        <div v-if="showTaskReminder" class="task-reminder task-reminder--pending" role="status" aria-live="polite">
          <button type="button" class="task-reminder__close" aria-label="关闭提醒" @click="dismissTaskReminder">×</button>
          <div class="task-reminder__eyebrow">工作要求提醒</div>
          <div class="task-reminder__title">待接受任务：{{ pendingTaskCount }}</div>
          <p class="task-reminder__text">你当前有待接受的工作要求，请及时处理。</p>
        </div>
      </transition>

      <transition name="task-reminder-fade">
        <div v-if="showOverdueReminder" class="task-reminder task-reminder--overdue" role="status" aria-live="polite">
          <button type="button" class="task-reminder__close" aria-label="关闭提醒" @click="dismissOverdueReminder">×</button>
          <div class="task-reminder__eyebrow">超时任务提醒</div>
          <div class="task-reminder__title">超时任务：{{ overdueTaskCount }}</div>
          <p class="task-reminder__text">你当前有超时中的工作要求，请优先处理并补交执行结果。</p>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getEmployeeWorkbenchAPI, updateEmployeeContactAPI } from '@/api/home'
import { getNoticeDetailAPI, getNoticePageAPI } from '@/api/system'
import { useUserStore } from '@/stores/user'
import { getToken, normalizeDeptCode } from '@/utils/auth'

const userStore = useUserStore()
const router = useRouter()

const summary = ref({
  userId: null,
  username: '',
  realName: '',
  deptId: null,
  deptCode: '',
  deptName: '',
  currentLoginTime: null,
  lastLoginTime: null,
  lowStockCount: null,
  zeroStockCount: null
})
const profile = ref({
  empCode: '',
  position: '',
  phone: '',
  email: ''
})
const deptContact = ref({
  deptName: '',
  leader: '',
  phone: ''
})
const contactFormRef = ref(null)
const contactForm = reactive({
  phone: '',
  email: ''
})
const savedContact = ref({
  phone: '',
  email: ''
})
const savingContact = ref(false)
const editingContact = ref(false)
const workRequirements = ref([])
const noticeList = ref([])
const noticeLoading = ref(false)
const noticeDialogVisible = ref(false)
const noticeDetail = ref({})
const dismissedTaskReminderSignature = ref('')
const dismissedOverdueReminderSignature = ref('')

const TASK_REMINDER_KEY_PREFIX = 'employee-pending-task-reminder'
const OVERDUE_REMINDER_KEY_PREFIX = 'employee-overdue-task-reminder'

const deptCode = computed(() => normalizeDeptCode(summary.value.deptCode || userStore.deptCode))
const displayName = computed(() => summary.value.realName || userStore.realName || summary.value.username || '部门员工')
const deptLabel = computed(() => summary.value.deptName || userStore.deptName || '未分配部门')

const deptSceneMap = {
  hr: {
    title: '人事资料与日常协同看板',
    subtitle: '聚焦档案完整性、资料时效和日常组织协同。'
  },
  purchase: {
    title: '采购执行与补货协同看板',
    subtitle: '优先处理补货节奏、供应商跟进和退货登记规范。'
  },
  sales: {
    title: '销售订单与缺货沟通看板',
    subtitle: '围绕订单核对、库存反馈和退货登记保持闭环。'
  },
  warehouse: {
    title: '仓储预警与盘点纪律看板',
    subtitle: '持续关注低库存、零库存和盘点差异处理时效。'
  },
  finance: {
    title: '财务对账与月结节奏看板',
    subtitle: '围绕资料齐套、数据核对和月结进度保持准确性。'
  }
}

const deptScene = computed(() => deptSceneMap[deptCode.value] || {
  title: '员工工作台',
  subtitle: '查看个人资料、部门联络信息、公告和当前工作要求。'
})

const metricCards = computed(() => {
  const cards = [
    {
      label: '当前部门',
      value: deptLabel.value,
      description: '当前账号归属部门'
    },
    {
      label: '公告数量',
      value: noticeList.value.length,
      description: '当前账号可见公告'
    }
  ]

  if ([ 'purchase', 'sales', 'warehouse' ].includes(deptCode.value)) {
    cards.push(
      {
        label: '低库存预警',
        value: summary.value.lowStockCount ?? 0,
        description: '需优先跟进的预警商品'
      },
      {
        label: '零库存预警',
        value: summary.value.zeroStockCount ?? 0,
        description: '已无现货可用商品'
      }
    )
  } else {
    cards.push(
      {
        label: '档案状态',
        value: hasProfileCompleted.value ? '已完善' : '待完善',
        description: '手机号、邮箱、岗位信息检查'
      },
      {
        label: '岗位名称',
        value: withFallback(profile.value.position),
        description: '当前员工岗位信息'
      }
    )
  }

  return cards
})

const hasProfileCompleted = computed(() => Boolean(profile.value.phone && profile.value.email && profile.value.position))
const hasCompleteContact = computed(() => Boolean(profile.value.phone && profile.value.email))
const shouldShowContactForm = computed(() => editingContact.value || !hasCompleteContact.value)

const profileReadonlyItems = computed(() => [
  { label: '员工编号', value: withFallback(profile.value.empCode) },
  { label: '岗位名称', value: withFallback(profile.value.position) }
])

const contactDisplayItems = computed(() => [
  { label: '手机号', value: withFallback(profile.value.phone) },
  { label: '邮箱', value: withFallback(profile.value.email) }
])

const deptContactItems = computed(() => [
  { label: '部门名称', value: withFallback(deptContact.value.deptName || deptLabel.value) },
  { label: '部门负责人', value: withFallback(deptContact.value.leader) },
  { label: '联系电话', value: withFallback(deptContact.value.phone) }
])

const formatTime = (val) => {
  if (!val) return '--'
  return String(val).replace('T', ' ').substring(0, 19)
}

const withFallback = (value) => value || '未完善'

const validatePhone = (_, value, callback) => {
  if (!value) {
    callback()
    return
  }
  if (!/^[0-9+\-()\s]{6,20}$/.test(String(value).trim())) {
    callback(new Error('手机号格式不正确'))
    return
  }
  callback()
}

const validateEmail = (_, value, callback) => {
  if (!value) {
    callback()
    return
  }
  if (!/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(String(value).trim())) {
    callback(new Error('邮箱格式不正确'))
    return
  }
  callback()
}

const contactRules = {
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  email: [{ validator: validateEmail, trigger: 'blur' }]
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

const pendingWorkRequirements = computed(() => workRequirements.value.filter(item => item.status === 0))

const pendingTaskCount = computed(() => pendingWorkRequirements.value.length)

const overdueWorkRequirements = computed(() => workRequirements.value.filter(item => item.overdueCurrent))

const overdueTaskCount = computed(() => overdueWorkRequirements.value.length)

const pendingTaskSignature = computed(() => {
  if (!summary.value.userId || pendingTaskCount.value === 0) {
    return ''
  }
  const assignIds = pendingWorkRequirements.value
    .map(item => Number(item.assignId))
    .filter(item => !Number.isNaN(item))
    .sort((a, b) => a - b)
    .join(',')
  return `${summary.value.userId}:${assignIds}`
})

const showTaskReminder = computed(() => {
  return Boolean(pendingTaskSignature.value) && dismissedTaskReminderSignature.value !== pendingTaskSignature.value
})

const overdueTaskSignature = computed(() => {
  if (!summary.value.userId || overdueTaskCount.value === 0) {
    return ''
  }
  const assignIds = overdueWorkRequirements.value
    .map(item => Number(item.assignId))
    .filter(item => !Number.isNaN(item))
    .sort((a, b) => a - b)
    .join(',')
  return `${summary.value.userId}:${assignIds}`
})

const showOverdueReminder = computed(() => {
  return Boolean(overdueTaskSignature.value) && dismissedOverdueReminderSignature.value !== overdueTaskSignature.value
})

const sortedWorkRequirements = computed(() => {
  return [...workRequirements.value].sort((a, b) => {
    if (a.overdueCurrent && !b.overdueCurrent) return -1
    if (!a.overdueCurrent && b.overdueCurrent) return 1
    if (a.status === 3 && b.status !== 3) return 1
    if (a.status !== 3 && b.status === 3) return -1
    if (a.status === 4 && b.status !== 4) return 1
    if (a.status !== 4 && b.status === 4) return -1
    return 0
  })
})

const wrTagType = (status) => {
  const map = { 0: 'info', 1: '', 2: 'warning', 3: 'success', 4: 'danger', 5: 'danger' }
  return map[status] ?? 'info'
}

const overdueTagType = (label) => {
  if (label === '超时中') return 'danger'
  if (label === '逾期提交' || label === '逾期完成') return 'warning'
  return 'success'
}

const goWorkRequirement = (item) => {
  router.push(`/work-requirement/${item.assignId}`)
}

const getTaskReminderStorageKey = (userId) => {
  const token = getToken()
  return `${TASK_REMINDER_KEY_PREFIX}:${userId}:${token}`
}

const getOverdueReminderStorageKey = (userId) => {
  const token = getToken()
  return `${OVERDUE_REMINDER_KEY_PREFIX}:${userId}:${token}`
}

const syncDismissedTaskReminder = () => {
  if (!summary.value.userId) {
    dismissedTaskReminderSignature.value = ''
    dismissedOverdueReminderSignature.value = ''
    return
  }
  dismissedTaskReminderSignature.value = sessionStorage.getItem(getTaskReminderStorageKey(summary.value.userId)) || ''
  dismissedOverdueReminderSignature.value = sessionStorage.getItem(getOverdueReminderStorageKey(summary.value.userId)) || ''
}

const dismissTaskReminder = () => {
  if (!summary.value.userId || !pendingTaskSignature.value) {
    dismissedTaskReminderSignature.value = pendingTaskSignature.value
    return
  }
  sessionStorage.setItem(getTaskReminderStorageKey(summary.value.userId), pendingTaskSignature.value)
  dismissedTaskReminderSignature.value = pendingTaskSignature.value
}

const dismissOverdueReminder = () => {
  if (!summary.value.userId || !overdueTaskSignature.value) {
    dismissedOverdueReminderSignature.value = overdueTaskSignature.value
    return
  }
  sessionStorage.setItem(getOverdueReminderStorageKey(summary.value.userId), overdueTaskSignature.value)
  dismissedOverdueReminderSignature.value = overdueTaskSignature.value
}

const loadWorkbench = async () => {
  try {
    const res = await getEmployeeWorkbenchAPI()
    if (res.code !== 200) {
      throw new Error(res.msg || '员工工作台加载失败')
    }
    summary.value = { ...summary.value, ...(res.data?.summary || {}) }
    profile.value = { ...profile.value, ...(res.data?.profile || {}) }
    deptContact.value = { ...deptContact.value, ...(res.data?.deptContact || {}) }
    savedContact.value = {
      phone: res.data?.profile?.phone || '',
      email: res.data?.profile?.email || ''
    }
    contactForm.phone = savedContact.value.phone
    contactForm.email = savedContact.value.email
    editingContact.value = !(savedContact.value.phone && savedContact.value.email)
    workRequirements.value = Array.isArray(res.data?.workRequirements) ? res.data.workRequirements : []
    syncDismissedTaskReminder()
  } catch (error) {
    ElMessage.error(error.message || '员工工作台加载失败')
  }
}

const startEditContact = () => {
  editingContact.value = true
  contactForm.phone = savedContact.value.phone
  contactForm.email = savedContact.value.email
  contactFormRef.value?.clearValidate()
}

const resetContactForm = () => {
  contactForm.phone = savedContact.value.phone
  contactForm.email = savedContact.value.email
  if (savedContact.value.phone && savedContact.value.email) {
    editingContact.value = false
  }
  contactFormRef.value?.clearValidate()
}

const handleSaveContact = () => {
  contactFormRef.value?.validate(async (valid) => {
    if (!valid) return
    savingContact.value = true
    try {
      const res = await updateEmployeeContactAPI({
        phone: contactForm.phone || '',
        email: contactForm.email || ''
      })
      if (res.code !== 200) {
        throw new Error(res.msg || '联系方式保存失败')
      }
      ElMessage.success('联系方式已保存')
      await loadWorkbench()
      editingContact.value = false
      contactFormRef.value?.clearValidate()
    } catch (error) {
      ElMessage.error(error.message || '联系方式保存失败')
    } finally {
      savingContact.value = false
    }
  })
}

const loadNotices = async () => {
  noticeLoading.value = true
  try {
    const res = await getNoticePageAPI({ pageNum: 1, pageSize: 6 })
    if (res.code !== 200) {
      throw new Error(res.msg || '公告加载失败')
    }
    noticeList.value = res.data?.records || []
  } catch (error) {
    ElMessage.error(error.message || '公告加载失败')
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

onMounted(() => {
  loadWorkbench()
  loadNotices()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');

.employee-dashboard {
  min-height: calc(100vh - 100px);
  padding: 36px;
  background:
    radial-gradient(circle at top right, rgba(5, 150, 105, 0.16), transparent 28%),
    radial-gradient(circle at 20% 10%, rgba(249, 115, 22, 0.12), transparent 24%),
    linear-gradient(180deg, #f5faf7 0%, #eaf1ef 100%);
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
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 20px 45px -30px rgba(15, 23, 42, 0.28);
}

.hero-card {
  border-radius: 30px;
  padding: 32px;
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
}

.hero-copy {
  display: grid;
  gap: 12px;
}

.eyebrow {
  margin: 0;
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
  margin: 0;
  color: #0f172a;
  font-size: 1.02rem;
  font-weight: 600;
}

.hero-subtitle {
  margin: 0;
  color: #475569;
  max-width: 720px;
  line-height: 1.7;
}

.hero-meta {
  min-width: 220px;
  padding: 18px 20px;
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(5, 150, 105, 0.1), rgba(255, 255, 255, 0.96));
  display: grid;
  gap: 8px;
}

.hero-meta-label {
  color: #64748b;
  font-size: 0.86rem;
}

.hero-meta strong {
  color: #0f172a;
  font-size: 1.05rem;
}

.metric-label,
.metric-desc,
.panel-head span,
.notice-meta,
.notice-date,
.empty-state,
.info-row span,
.tip-index {
  color: #64748b;
}

.metric-card strong,
.info-row strong,
.notice-title {
  color: #0f172a;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.metric-card {
  border-radius: 22px;
  padding: 22px;
  display: grid;
  gap: 10px;
}

.metric-card strong {
  font-size: 1.35rem;
  word-break: break-word;
}

.metric-desc {
  font-size: 0.88rem;
  line-height: 1.6;
}

.panel-grid {
  display: grid;
  gap: 24px;
}

.panel-grid--double {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.panel-card {
  border-radius: 28px;
  padding: 28px;
}

.panel-card--accent {
  background:
    linear-gradient(135deg, rgba(5, 150, 105, 0.08), rgba(255, 255, 255, 0.96)),
    rgba(255, 255, 255, 0.92);
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.panel-head h2 {
  margin: 0;
  font-size: 1.2rem;
  color: #0f172a;
}

.info-grid,
.contact-form,
.notice-list {
  display: grid;
  gap: 12px;
}

.contact-form {
  margin-top: 18px;
}

.contact-display-block {
  margin-top: 18px;
  display: grid;
  gap: 14px;
}

.contact-display-head {
  display: grid;
  gap: 2px;
}

.contact-display-title {
  color: #0f172a;
  font-weight: 700;
  font-size: 0.98rem;
}

.contact-display-text {
  color: #64748b;
  font-size: 0.82rem;
  line-height: 1.45;
}

.contact-display-actions {
  display: flex;
  justify-content: flex-end;
}

.contact-edit-button {
  border-color: rgba(148, 163, 184, 0.5);
  color: #475569;
  background: rgba(255, 255, 255, 0.8);
}

.contact-editor {
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 22px;
  padding: 18px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.92), rgba(255, 255, 255, 0.96));
  display: grid;
  gap: 16px;
}

.contact-editor-head {
  display: grid;
  gap: 4px;
}

.contact-editor-title {
  color: #0f172a;
  font-weight: 700;
}

.contact-editor-text,
.contact-field-label {
  color: #64748b;
  font-size: 0.9rem;
}

.contact-field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.contact-field {
  border: 1px solid rgba(148, 163, 184, 0.12);
  border-radius: 18px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.92);
  display: grid;
  gap: 10px;
}

:deep(.contact-form-item) {
  margin-bottom: 0;
}

:deep(.contact-field .el-input__wrapper) {
  min-height: 42px;
  border-radius: 12px;
  box-shadow: 0 0 0 1px rgba(203, 213, 225, 0.9) inset;
}

:deep(.contact-field .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px rgba(5, 150, 105, 0.9) inset;
}

.contact-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.contact-save-button {
  border-color: rgba(5, 150, 105, 0.2);
  color: #166534;
  background: rgba(220, 252, 231, 0.85);
}

.contact-save-button:hover,
.contact-save-button:focus {
  border-color: rgba(5, 150, 105, 0.35);
  color: #14532d;
  background: rgba(187, 247, 208, 0.92);
}

.info-row,
.notice-item {
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 18px;
}

.info-row {
  padding: 16px 18px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  background: rgba(248, 250, 252, 0.78);
}

.info-row strong {
  text-align: right;
  word-break: break-word;
}

.notice-item {
  width: 100%;
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
  margin-bottom: 8px;
}

.notice-content {
  white-space: pre-wrap;
  line-height: 1.7;
  margin: 0;
}

.empty-state {
  font-size: 0.92rem;
}

@media (max-width: 1100px) {
  .metric-grid,
  .panel-grid--double {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 900px) {
  .employee-dashboard {
    padding: 20px;
  }

  .hero-card,
  .info-row,
  .notice-item {
    flex-direction: column;
  }

  .contact-field-grid,
  .panel-grid--double,
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .contact-display-head {
    gap: 4px;
  }

  .hero-meta {
    min-width: 0;
    width: 100%;
  }

  .info-row strong {
    text-align: left;
  }
}

.wr-list {
  display: grid;
  gap: 8px;
}

.wr-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
  padding: 12px 16px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.92);
  cursor: pointer;
  text-align: left;
  transition: box-shadow 0.2s;
  font-family: inherit;
}

.wr-item:hover {
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.10);
}

.wr-item--done {
  opacity: 0.6;
}

.wr-item--done .wr-item-content {
  text-decoration: line-through;
}

.wr-item--rejected {
  opacity: 0.5;
}

.wr-item-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.wr-item-tags {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.wr-item-content {
  flex: 1;
  font-size: 0.92rem;
  font-weight: 500;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wr-item-deadline {
  font-size: 0.78rem;
  color: #64748b;
}

.wr-item--overdue {
  border-color: rgba(220, 38, 38, 0.28);
  background: linear-gradient(135deg, rgba(254, 242, 242, 0.88), rgba(255, 255, 255, 0.94));
}

.task-reminder {
  position: fixed;
  right: 28px;
  width: min(320px, calc(100vw - 32px));
  padding: 18px 20px 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 22px;
  box-shadow: 0 22px 44px -24px rgba(15, 23, 42, 0.34);
  z-index: 60;
  animation: task-reminder-float 3.2s ease-in-out infinite;
}

.task-reminder--pending {
  bottom: 28px;
  background: linear-gradient(145deg, rgba(255, 251, 235, 0.97), rgba(255, 255, 255, 0.96));
}

.task-reminder--overdue {
  bottom: 170px;
  background: linear-gradient(145deg, rgba(254, 242, 242, 0.97), rgba(255, 255, 255, 0.96));
}

.task-reminder__close {
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

.task-reminder__close:hover {
  background: rgba(15, 23, 42, 0.1);
}

.task-reminder__eyebrow {
  margin-bottom: 8px;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.task-reminder--pending .task-reminder__eyebrow {
  color: #b45309;
}

.task-reminder--overdue .task-reminder__eyebrow {
  color: #b91c1c;
}

.task-reminder__title {
  color: #111827;
  font-size: 1.2rem;
  font-weight: 800;
}

.task-reminder__text {
  margin: 8px 0 0;
  color: #475569;
  font-size: 0.9rem;
  line-height: 1.6;
}

.task-reminder-fade-enter-active,
.task-reminder-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.task-reminder-fade-enter-from,
.task-reminder-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

@keyframes task-reminder-float {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-6px);
  }
}

@media (max-width: 900px) {
  .task-reminder {
    right: 16px;
    width: calc(100vw - 32px);
  }

  .task-reminder--pending {
    bottom: 16px;
  }

  .task-reminder--overdue {
    bottom: 158px;
  }
}
</style>