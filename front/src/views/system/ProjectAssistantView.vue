<template>
  <div class="assistant-page">
    <!-- Sidebar overlay -->
    <div class="sidebar-overlay" v-if="sidebarOpen" @click="sidebarOpen = false"></div>
    <!-- History sidebar -->
    <aside class="sidebar" :class="{ 'sidebar--open': sidebarOpen }">
      <div class="sidebar-header">
        <button class="new-chat-btn" @click="startNewConversation">
          <span class="new-chat-icon">+</span> {{ hasDraftConversation && isHistoryMode ? '返回当前对话' : '新对话' }}
        </button>
      </div>
      <div class="sidebar-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          :class="['conv-item', { 'conv-item--active': currentConversationId === conv.id }]"
          @click="viewConversation(conv)"
        >
          <div class="conv-item-content">
            <span class="conv-title">{{ conv.title }}</span>
            <span class="conv-time">{{ formatTime(conv.createdAt) }}</span>
          </div>
          <button class="conv-delete" @click.stop="handleDeleteConversation(conv)" title="删除">×</button>
        </div>
        <div v-if="conversations.length === 0" class="sidebar-empty">暂无历史对话</div>
        <button
          v-if="hasMoreConversations"
          class="load-more-btn"
          @click="loadMoreConversations"
        >加载更多</button>
      </div>
      <div class="sidebar-footer">
        <button
          class="clear-history-btn"
          :disabled="conversations.length === 0"
          @click="handleClearAllConversations"
        >清空会话历史</button>
      </div>
    </aside>
    <div class="assistant-container">
      <header class="assistant-header">
        <div class="header-nav">
          <button class="sidebar-toggle" @click="sidebarOpen = !sidebarOpen" title="历史对话">
            <span>☰</span>
          </button>
        </div>
        <div class="header-main">
          <div class="header-badge">
            <span class="badge-icon">◆</span>
            <span class="badge-text">AI</span>
          </div>
          <div class="header-info">
            <h1 class="header-title">项目助手</h1>
            <p class="header-subtitle">仅回答当前企业协同运营管理系统（ECS）项目相关问题 · 回答基于项目文档生成并附来源</p>
            <div class="assistant-controls">
              <div class="mode-switch" :class="{ 'mode-switch--disabled': isLoading || isHistoryMode }">
                <button
                  v-for="item in answerModeOptions"
                  :key="item.value"
                  class="mode-switch-btn"
                  :class="{ 'mode-switch-btn--active': answerMode === item.value }"
                  :disabled="isLoading || isHistoryMode"
                  @click="setAnswerMode(item.value)"
                >
                  {{ item.label }}
                </button>
              </div>
              <div
                v-if="showModelSelector"
                class="model-select-panel"
                :class="{ 'model-select-panel--disabled': isLoading || isHistoryMode }"
              >
                <span class="model-select-label">模型</span>
                <el-select
                  v-model="selectedModelCode"
                  class="model-select"
                  size="small"
                  :disabled="isLoading || isHistoryMode"
                  placeholder="请选择模型"
                >
                  <el-option
                    v-for="item in modelOptions"
                    :key="item.modelCode"
                    :label="item.label"
                    :value="item.modelCode"
                  />
                </el-select>
              </div>
            </div>
          </div>
        </div>
        <div class="header-role">
          <span class="role-dot"></span>
          <span class="role-label">{{ roleLabel }}</span>
        </div>
      </header>

      <!-- Chat area -->
      <div class="chat-area" ref="chatAreaRef">
        <!-- Welcome state -->
        <div v-if="messages.length === 0" class="welcome-zone">
          <div class="welcome-icon">
            <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect x="4" y="8" width="40" height="32" rx="4" stroke="currentColor" stroke-width="2.5"/>
              <path d="M4 16h40" stroke="currentColor" stroke-width="2.5"/>
              <circle cx="10" cy="12" r="1.5" fill="currentColor"/>
              <circle cx="15" cy="12" r="1.5" fill="currentColor"/>
              <circle cx="20" cy="12" r="1.5" fill="currentColor"/>
              <path d="M14 26h10M14 32h16" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              <path d="M32 24l4 4-4 4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <h2 class="welcome-title">有什么想了解的？</h2>
          <p class="welcome-desc">问我关于项目功能、技术栈、部署方式、角色权限或业务流程的问题</p>

          <!-- Suggestions -->
          <div class="suggestions" v-if="displaySuggestions.length > 0">
            <button
              v-for="(s, i) in displaySuggestions"
              :key="i"
              class="suggestion-pill"
              @click="askQuestion(s)"
            >
              <span class="pill-arrow">→</span>
              {{ s }}
            </button>
          </div>
        </div>

        <!-- Messages -->
        <div v-for="(msg, idx) in messages" :key="idx" :class="['message', `message--${msg.role}`]">
          <!-- User message -->
          <div v-if="msg.role === 'user'" class="message-bubble user-bubble">
            <p>{{ msg.content }}</p>
          </div>

          <!-- Assistant message -->
          <div v-else class="message-bubble assistant-bubble">
            <div class="assistant-avatar">
              <span>◆</span>
            </div>
            <div class="assistant-content">
              <div v-if="showAssistantMeta(msg) && !msg.loading" class="assistant-meta">
                <div v-if="msg.mode" class="mode-result-tag">
                  {{ getModeLabel(msg.mode) }}
                </div>
                <div v-if="getMessageModelLabel(msg)" class="assistant-meta-tag assistant-meta-tag--model">
                  {{ getMessageModelLabel(msg) }}
                </div>
                <div v-if="hasLatency(msg)" class="assistant-meta-tag assistant-meta-tag--latency">
                  {{ msg.latencyMs }} ms
                </div>
              </div>

              <!-- Loading state -->
              <div v-if="msg.loading" class="loading-indicator">
                <span class="dot"></span><span class="dot"></span><span class="dot"></span>
                <span class="loading-text">正在检索文档并生成回答...</span>
              </div>

              <!-- Answer -->
              <div v-else>
                <div v-if="getModeHint(msg) && !isHistoryMode" class="kb-miss-hint">
                  {{ getModeHint(msg) }}
                </div>
                <div
                  v-if="getOperationalHintText(msg)"
                  :class="['assistant-status-hint', `assistant-status-hint--${getOperationalHintTone(msg)}`]"
                >
                  <span class="assistant-status-badge">{{ getOperationalHintBadge(msg) }}</span>
                  <span>{{ getOperationalHintText(msg) }}</span>
                </div>
                <div class="answer-text" v-html="renderMarkdown(getDisplayContent(msg))"></div>

                <!-- Message actions -->
                <div class="msg-actions msg-actions--assistant">
                  <button
                    class="msg-action-btn"
                    :class="{ 'msg-action-btn--copied': copiedMessageState[idx] }"
                    :title="copiedMessageState[idx] ? '已复制' : '复制消息'"
                    @click="copyMessage(msg.content, idx)"
                  >
                    <svg v-if="copiedMessageState[idx]" viewBox="0 0 24 24" fill="none" width="15" height="15"><path d="M5 12.5l4.2 4.2L19 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
                    <svg v-else viewBox="0 0 24 24" fill="none" width="15" height="15"><rect x="9" y="9" width="11" height="11" rx="2" stroke="currentColor" stroke-width="2"/><path d="M5 15V5a2 2 0 012-2h10" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
                  </button>
                  <button class="msg-action-btn" v-if="!isHistoryMode && idx > 0 && messages[idx - 1]?.role === 'user'" @click="resendMessage(messages[idx - 1]?.content)" title="重新发送">
                    <svg viewBox="0 0 24 24" fill="none" width="15" height="15"><path d="M17.65 6.35A7.96 7.96 0 0012 4a8 8 0 108 8" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><polyline points="20 4 20 10 14 10" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
                  </button>
                </div>

                <!-- Sources -->
                <div v-if="msg.sources && msg.sources.length > 0" class="sources-section">
                  <div class="sources-label">
                    <span class="sources-icon">📄</span> 来源文档
                  </div>
                  <div class="sources-list">
                    <div
                      v-for="(src, si) in msg.sources"
                      :key="si"
                      :class="['source-card', { 'source-card--expanded': expandedSources[`${idx}-${si}`] }]"
                      @click="toggleSource(idx, si)"
                    >
                      <div class="source-header">
                        <span :class="['source-tag', `source-tag--${src.sourceType}`]">
                          {{ sourceLabels[src.sourceType] || src.sourceType }}
                        </span>
                        <span class="source-title">{{ src.titlePath || src.fileName }}</span>
                        <span class="source-expand">{{ expandedSources[`${idx}-${si}`] ? '▲' : '▼' }}</span>
                      </div>
                      <div v-if="expandedSources[`${idx}-${si}`]" class="source-snippet">
                        {{ src.snippet }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Input area -->
      <div class="input-area">
        <div v-if="isHistoryMode" class="history-hint">
          <span class="history-hint-icon">📋</span>
          <span>这是历史对话，仅供查看</span>
          <button class="history-new-btn" @click="startNewConversation">{{ hasDraftConversation ? '返回当前对话' : '开始新对话' }}</button>
        </div>
        <template v-else>
          <div v-if="isEmployeeUser" class="employee-scope-tip">
            <span class="employee-scope-badge">仅限项目内</span>
            <span>{{ employeeScopeHint }}</span>
          </div>
          <div class="input-wrapper">
            <input
              v-model="inputText"
              class="chat-input"
              type="text"
              :placeholder="inputPlaceholder"
              maxlength="500"
              :disabled="isLoading"
              @keydown.enter="sendMessage"
            />
            <button
              class="send-btn"
              :disabled="!inputText.trim() || isLoading"
              @click="sendMessage"
            >
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M5 12h14M12 5l7 7-7 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </div>
          <p class="input-hint">{{ currentModeHint }}</p>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { isSuperAdmin, isAdminRole, isEmployeeRole } from '@/utils/auth'
import { queryAssistantAPI, getSuggestionsAPI, getAssistantModelsAPI } from '@/api/projectAssistant'
import {
  clearAllConversationsAPI,
  createConversationAPI,
  listConversationsAPI,
  getConversationMessagesAPI,
  deleteConversationAPI,
  saveConversationMessagesAPI
} from '@/api/aiConversation'

const userStore = useUserStore()
const chatAreaRef = ref(null)
const inputText = ref('')
const isLoading = ref(false)
const messages = ref([])
const suggestions = ref([])
const expandedSources = reactive({})
const copiedMessageState = reactive({})
const copyResetTimers = new Map()
const KB_MISS_HINT_TEXT = '抱歉，我没有找到相关的知识库，请您提供更多的信息或者尝试其他的关键词。接下来采用大模型进行搜索为你解答。'
const STRICT_MODE_HINT_TEXT = '仅按项目文档回答模式下，当前未找到足够依据回答这个问题。你可以补充更具体的项目关键词，或切换到“文档优先，不足时允许模型补充”。'
const EMPLOYEE_BOUNDARY_MSG = '我目前只负责说明当前企业协同运营管理系统（ECS）项目，暂时不回答项目范围外的问题。你可以问我项目功能、技术栈、部署方式、模块职责、接口范围、角色权限或业务流程。'
const EMPLOYEE_BOUNDARY_HINT = '普通员工账号仅支持当前企业协同运营管理系统（ECS）项目内问答，不会切换到项目外通用模型。优先提问页面功能、角色权限、业务流程、部署接口等内容。'
const ANSWER_MODE = {
  STRICT: 'strict',
  HYBRID: 'hybrid'
}
const EMPLOYEE_PROJECT_KEYWORDS = [
  '仓库', '仓储', '库存', '采购', '销售', '退货', '商品', '供应商', '员工', '管理员', '超管',
  '财务', '人事', '审批', '作废', '预警', '工作要求', '消息中心', '公告', '登录', '权限',
  '角色', '首页', '页面', '模块', '接口', '部署', '数据库', '前端', '后端', '系统', '项目',
  '文档', '白名单', '日志', '图表', '毛利', '部门', '流程', '功能', '当前企业协同运营管理系统'
]
const EMPLOYEE_NON_PROJECT_PATTERNS = [
  /天气|气温|下雨|台风/,
  /星座|生肖|运势|塔罗/,
  /翻译|润色|作文|写一篇|写首|写个请假条/,
  /小说|诗歌|歌词|文案|朋友圈/,
  /电影|电视剧|动漫|明星|综艺/,
  /旅游|景点|机票|酒店|攻略/,
  /美食|菜谱|做饭|减肥|健身/,
  /股票|基金|彩票|比特币|黄金/,
  /新闻|国际|历史人物|时政/,
  /恋爱|情感|表白|相亲/,
  /数学题|算法题|编程题|英语作文|考研|高考/
]
const MODEL_LABELS = {
  'qwen-plus': '通义千问 Qwen Plus',
  'glm-4-flash': '智谱 GLM-4-Flash',
  'deepseek-chat': 'DeepSeek Chat',
  'moonshot-v1-8k': 'Kimi Moonshot 8K'
}
const PROVIDER_LABELS = {
  qwen: '通义千问',
  glm: '智谱 GLM',
  deepseek: 'DeepSeek',
  kimi: 'Kimi'
}

// Conversation history state
const conversations = ref([])
const currentConversationId = ref(null)
const isHistoryMode = ref(false)
const sidebarOpen = ref(false)
const conversationPage = ref(1)
const hasMoreConversations = ref(false)
const draftConversationState = ref(null)
const visibleSuggestions = ref([])
const answerMode = ref(ANSWER_MODE.HYBRID)
const modelOptions = ref([])
const selectedModelCode = ref('')
const answerModeOptions = [
  { value: ANSWER_MODE.STRICT, label: '仅按项目文档回答' },
  { value: ANSWER_MODE.HYBRID, label: '文档优先，不足时允许模型补充' }
]

const sourceLabels = {
  readme: 'README',
  project: '项目结构',
  front: '前端',
  back: '后端',
  role: '角色知识'
}

const deptLabels = {
  hr: '人事',
  purchase: '采购',
  sales: '销售',
  warehouse: '仓储',
  finance: '财务'
}

const roleLabel = computed(() => {
  const role = userStore.role
  if (isSuperAdmin(role)) return '超级管理员'
  if (isEmployeeRole(role)) return '普通员工'
  if (isAdminRole(role)) {
    const dept = deptLabels[userStore.deptCode] || userStore.deptCode
    return dept ? `${dept}管理员` : '管理员'
  }
  return '用户'
})

const isEmployeeUser = computed(() => isEmployeeRole(userStore.role))

const employeeScopeHint = computed(() => EMPLOYEE_BOUNDARY_HINT)

const inputPlaceholder = computed(() => {
  if (isEmployeeUser.value) {
    return '请输入当前项目相关问题，例如：员工首页有哪些功能？'
  }
  return '输入你的问题...'
})

const hasDraftConversation = computed(() => {
  const draft = draftConversationState.value
  if (!draft) return false
  return Boolean(draft.conversationId || draft.inputText || (draft.messages && draft.messages.length > 0))
})

const displaySuggestions = computed(() => {
  return visibleSuggestions.value.length > 0 ? visibleSuggestions.value : suggestions.value.slice(0, 3)
})

const showModelSelector = computed(() => modelOptions.value.length > 0)

const selectedModelOption = computed(() => {
  return modelOptions.value.find(item => item.modelCode === selectedModelCode.value) || null
})

const currentModeHint = computed(() => {
  if (isEmployeeUser.value) {
    if (answerMode.value === ANSWER_MODE.STRICT) {
      return `${EMPLOYEE_BOUNDARY_HINT} 当前仅按项目文档回答；未命中时会直接提示补充关键词。`
    }
    return `${EMPLOYEE_BOUNDARY_HINT} 当前文档优先，不足时允许模型补充，但仍只限项目内问题。`
  }

  if (answerMode.value === ANSWER_MODE.STRICT) {
    return '当前仅按项目文档回答；未命中时会直接提示补充关键词或切换模式。'
  }
  return '当前文档优先，不足时允许模型补充；回答会尽量附带项目文档来源。'
})

const syncSelectedModelCode = (options = []) => {
  if (!Array.isArray(options) || options.length === 0) {
    selectedModelCode.value = ''
    return
  }

  if (selectedModelCode.value && options.some(item => item.modelCode === selectedModelCode.value)) {
    return
  }

  const defaultOption = options.find(item => item.defaultSelected) || options[0]
  selectedModelCode.value = defaultOption?.modelCode || ''
}

const loadAssistantModels = async () => {
  try {
    const res = await getAssistantModelsAPI()
    if (res.code === 200 && Array.isArray(res.data)) {
      modelOptions.value = res.data
      syncSelectedModelCode(res.data)
      return
    }
  } catch (error) {
    console.warn('加载模型列表失败', error)
  }

  modelOptions.value = []
  selectedModelCode.value = ''
}

const renderMarkdown = (text) => {
  if (!text) return ''
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.+?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
}

const cloneMessages = (items = []) => items.map(msg => ({
  ...msg,
  sources: Array.isArray(msg.sources) ? msg.sources.map(src => ({ ...src })) : []
}))

const parseSourcesJson = (value) => {
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch (error) {
    console.warn('解析来源文档失败', error)
    return []
  }
}

const getDisplayContent = (msg) => {
  if (!msg?.content) return ''
  if (msg.hitType !== 'kb-miss-model') return msg.content

  let content = msg.content.trim()
  if (content.startsWith(KB_MISS_HINT_TEXT)) {
    content = content.slice(KB_MISS_HINT_TEXT.length).trim()
  }
  return content
}

const isEmployeeBoundaryMessage = (msg) => {
  if (!msg) return false
  return msg.hitType === 'employee-boundary' || msg.content === EMPLOYEE_BOUNDARY_MSG
}

const getModeLabel = (mode) => {
  if (mode === ANSWER_MODE.STRICT) return '仅按项目文档回答'
  return '文档优先，允许模型补充'
}

const getModelDisplayLabel = (modelCode, providerCode) => {
  if (!modelCode) return ''

  const optionLabel = modelOptions.value.find(item => item.modelCode === modelCode)?.label
  if (optionLabel) return optionLabel
  if (MODEL_LABELS[modelCode]) return MODEL_LABELS[modelCode]
  if (providerCode && PROVIDER_LABELS[providerCode]) {
    return `${PROVIDER_LABELS[providerCode]} · ${modelCode}`
  }
  return modelCode
}

const getMessageModelLabel = (msg) => {
  if (!msg) return ''
  return getModelDisplayLabel(msg.modelCode, msg.providerCode)
}

const hasLatency = (msg) => {
  return Boolean(msg && typeof msg.latencyMs === 'number' && msg.latencyMs > 0)
}

const showAssistantMeta = (msg) => {
  if (!msg) return false
  return Boolean(msg.mode || getMessageModelLabel(msg) || hasLatency(msg))
}

const getModeHint = (msg) => {
  if (!msg) return ''
  if (msg.hitType === 'kb-miss-strict') return STRICT_MODE_HINT_TEXT
  if (msg.hitType === 'kb-miss-model') return KB_MISS_HINT_TEXT
  return ''
}

const getOperationalHintTone = (msg) => {
  if (!msg) return 'info'
  if (isEmployeeBoundaryMessage(msg)) return 'neutral'
  if (msg.hitType === 'rate-limited') return 'danger'
  if (msg.hitType === 'budget-blocked') return 'warning'
  if (msg.hitType === 'general-limited') return 'neutral'
  if (msg.fallbackUsed) return 'info'
  return 'info'
}

const getOperationalHintBadge = (msg) => {
  if (!msg) return ''
  if (isEmployeeBoundaryMessage(msg)) return '项目边界'
  if (msg.hitType === 'rate-limited') return '限流保护'
  if (msg.hitType === 'budget-blocked') return '预算保护'
  if (msg.hitType === 'general-limited') return '范围限制'
  if (msg.fallbackUsed) return '自动回退'
  return ''
}

const getOperationalHintText = (msg) => {
  if (!msg) return ''
  if (isEmployeeBoundaryMessage(msg)) {
    return '普通员工账号只开放当前企业协同运营管理系统（ECS）项目内问答；请改问页面功能、角色权限、审批流程、部署接口或业务流程。'
  }
  if (msg.hitType === 'rate-limited') {
    return '当前账号请求过于频繁，系统已临时限制模型调用，请稍后再试。'
  }
  if (msg.hitType === 'budget-blocked') {
    const modelLabel = getMessageModelLabel(msg) || selectedModelLabel.value || '当前模型'
    return `${modelLabel} 今日预算已触顶，系统已阻止继续调用；可稍后再试或切换其他可用模型。`
  }
  if (msg.hitType === 'general-limited') {
    return '项目外问题已触发长度与开销限制，建议缩小问题范围后重试。'
  }
  if (msg.fallbackUsed) {
    const modelLabel = getMessageModelLabel(msg) || '备选模型'
    return `主模型暂不可用，系统已自动切换到 ${modelLabel} 继续完成本次回答。`
  }
  return ''
}

const setAnswerMode = (mode) => {
  if (isLoading.value || isHistoryMode.value) return
  answerMode.value = mode
}

const shouldInterceptEmployeeQuestion = (question) => {
  if (!isEmployeeUser.value) return false
  const normalized = (question || '').trim().toLowerCase()
  if (!normalized || normalized.length <= 3) return false
  if (EMPLOYEE_PROJECT_KEYWORDS.some(keyword => normalized.includes(keyword.toLowerCase()))) {
    return false
  }
  return EMPLOYEE_NON_PROJECT_PATTERNS.some(pattern => pattern.test(normalized))
}

const buildEmployeeBoundaryAssistantMessage = () => ({
  role: 'assistant',
  content: EMPLOYEE_BOUNDARY_MSG,
  sources: [],
  hitType: 'employee-boundary',
  providerCode: null,
  modelCode: null,
  fallbackUsed: false,
  latencyMs: null,
  mode: answerMode.value,
  loading: false
})

const ensureActiveConversation = async (question) => {
  if (currentConversationId.value) {
    return currentConversationId.value
  }

  try {
    const title = question.length > 50 ? question.substring(0, 50) + '...' : question
    const createRes = await createConversationAPI(title)
    if (createRes.code === 200 && createRes.data) {
      currentConversationId.value = createRes.data.id
      conversations.value.unshift(createRes.data)
      return currentConversationId.value
    }
  } catch (e) {
    console.error('创建会话失败', e)
  }

  return null
}

const persistEmployeeBoundaryMessage = async (conversationId, question) => {
  if (!conversationId) {
    return
  }

  try {
    await saveConversationMessagesAPI(conversationId, {
      userContent: question,
      assistantContent: EMPLOYEE_BOUNDARY_MSG,
      sourcesJson: null,
      hitType: 'employee-boundary',
      providerCode: null,
      modelCode: null,
      fallbackUsed: false
    })
  } catch (e) {
    console.error('保存员工边界提示失败', e)
  }
}

const saveDraftConversationState = () => {
  draftConversationState.value = {
    conversationId: currentConversationId.value,
    inputText: inputText.value,
    messages: cloneMessages(messages.value)
  }
}

const refreshWelcomeSuggestions = () => {
  if (suggestions.value.length <= 3) {
    visibleSuggestions.value = [...suggestions.value]
    return
  }

  const pool = [...suggestions.value]
  const chosen = []
  while (pool.length > 0 && chosen.length < 3) {
    const index = Math.floor(Math.random() * pool.length)
    chosen.push(pool.splice(index, 1)[0])
  }
  visibleSuggestions.value = chosen
}

const buildFallbackSuggestionData = () => {
  if (isSuperAdmin(userStore.role)) {
    return [
      '超管总览页面有哪些指标和快捷入口？',
      '部门审批页面怎么通过或驳回申请？',
      '安全策略页面怎么新增和管理IP白名单？',
      '登录日志页面记录哪些信息？',
      '公告管理页面怎么发布公告？',
      '跨部门业务归口是怎么划分的？'
    ]
  }

  if (isEmployeeRole(userStore.role)) {
    return [
      '员工首页都展示哪些功能模块？',
      '工作要求详情页怎么查看和接受任务？',
      '任务状态流程有哪些阶段？',
      '站内消息中心可以收到哪些通知？',
      '员工首页的浮动提醒会显示什么？',
      '部门场景说明里有哪些部门介绍？'
    ]
  }

  const adminFallback = {
    hr: [
      '全部门管理页面怎么新增和编辑部门？',
      '全员工管理页面怎么新增员工？',
      '员工分布图表页面展示哪些统计数据？',
      '工作要求页面怎么发布和审核任务？',
      '部门审批流程是怎样的？',
      '首页功能有哪些快捷入口和提醒？'
    ],
    purchase: [
      '商品进货页面怎么新增进货单？',
      '进货退货页面怎么操作退货？',
      '历史单据作废流程是怎样的？',
      '预警中心页面显示哪些库存预警？',
      '工作要求页面怎么发布和跟踪任务？',
      '首页功能有哪些预警指标和快捷入口？'
    ],
    sales: [
      '商品销售页面怎么新增销售单？',
      '销售退货页面怎么操作退货？',
      '历史单据作废流程是怎样的？',
      '预警中心页面显示哪些库存预警？',
      '工作要求页面怎么发布和跟踪任务？',
      '首页功能有哪些预警指标和快捷入口？'
    ],
    warehouse: [
      '供应商管理页面怎么新增和编辑供应商？',
      '商品资料管理页面怎么设置预警阈值？',
      '作废审批页面怎么审批通过或驳回？',
      '预警中心页面的零库存和低库存有什么区别？',
      '工作要求页面怎么发布和跟踪任务？',
      '首页功能有哪些预警指标和快捷入口？'
    ],
    finance: [
      '销售统计图表页面有哪些指标和图表？',
      '毛利视角指标卡展示哪些数据？',
      '毛利计算口径说明是怎样的？',
      '图表页面的时间筛选怎么操作？',
      '工作要求页面怎么发布和跟踪任务？',
      '首页功能有哪些快捷入口和提醒？'
    ]
  }

  return adminFallback[userStore.deptCode] || [
    '这个角色当前最核心的业务是什么？',
    '这个角色当前能处理哪些事情？',
    '这个角色的权限边界是什么？',
    '这个角色相关流程应该怎么走？'
  ]
}

const restoreDraftConversationState = () => {
  if (!hasDraftConversation.value) return false

  currentConversationId.value = draftConversationState.value.conversationId
  inputText.value = draftConversationState.value.inputText
  messages.value = cloneMessages(draftConversationState.value.messages)
  isHistoryMode.value = false
  sidebarOpen.value = false
  return true
}

const scrollToBottom = async () => {
  await nextTick()
  if (chatAreaRef.value) {
    chatAreaRef.value.scrollTop = chatAreaRef.value.scrollHeight
  }
}

const toggleSource = (msgIdx, srcIdx) => {
  const key = `${msgIdx}-${srcIdx}`
  expandedSources[key] = !expandedSources[key]
}

const clearCopiedState = (messageIdx) => {
  copiedMessageState[messageIdx] = false

  const timer = copyResetTimers.get(messageIdx)
  if (timer) {
    clearTimeout(timer)
    copyResetTimers.delete(messageIdx)
  }
}

const copyMessage = async (content, messageIdx) => {
  try {
    await navigator.clipboard.writeText(content)
  } catch {
    // fallback
    const ta = document.createElement('textarea')
    ta.value = content
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
  }

  clearCopiedState(messageIdx)
  copiedMessageState[messageIdx] = true
  const timer = window.setTimeout(() => {
    copiedMessageState[messageIdx] = false
    copyResetTimers.delete(messageIdx)
  }, 1400)
  copyResetTimers.set(messageIdx, timer)
}

const resendMessage = (question) => {
  if (!question || isLoading.value || isHistoryMode.value) return
  inputText.value = question
  sendMessage()
}

const askQuestion = (question) => {
  inputText.value = question
  sendMessage()
}

const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diffMs = now - d
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin}分钟前`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour}小时前`
  const diffDay = Math.floor(diffHour / 24)
  if (diffDay < 30) return `${diffDay}天前`
  return `${d.getMonth() + 1}/${d.getDate()}`
}

const loadConversations = async (page = 1) => {
  try {
    const res = await listConversationsAPI(page, 20)
    if (res.code === 200) {
      const data = res.data
      if (page === 1) {
        conversations.value = data.records || []
      } else {
        conversations.value.push(...(data.records || []))
      }
      conversationPage.value = page
      hasMoreConversations.value = (data.current || 1) < (data.pages || 1)
    }
  } catch (e) {
    console.error('加载会话列表失败', e)
  }
}

const loadMoreConversations = () => {
  loadConversations(conversationPage.value + 1)
}

const viewConversation = async (conv) => {
  // 如果点击的是当前正在进行的活跃对话，不切换到历史模式
  if (conv.id === currentConversationId.value && !isHistoryMode.value) {
    sidebarOpen.value = false
    return
  }

  if (!isHistoryMode.value) {
    saveDraftConversationState()
  }

  currentConversationId.value = conv.id
  isHistoryMode.value = true
  sidebarOpen.value = false
  messages.value = []

  try {
    const res = await getConversationMessagesAPI(conv.id)
    if (res.code === 200 && Array.isArray(res.data)) {
      messages.value = res.data.map(msg => ({
        role: msg.role,
        content: msg.content,
        sources: parseSourcesJson(msg.sourcesJson),
        hitType: msg.hitType,
        providerCode: msg.providerCode || null,
        modelCode: msg.modelCode || null,
        fallbackUsed: msg.fallbackUsed === true,
        latencyMs: typeof msg.latencyMs === 'number' ? msg.latencyMs : null,
        mode: null,
        loading: false
      }))
    }
  } catch (e) {
    console.error('加载会话消息失败', e)
  }
  await scrollToBottom()
}

const startNewConversation = () => {
  if (isHistoryMode.value && restoreDraftConversationState()) {
    return
  }

  draftConversationState.value = null
  currentConversationId.value = null
  inputText.value = ''
  isHistoryMode.value = false
  messages.value = []
  sidebarOpen.value = false
}

const handleDeleteConversation = async (conv) => {
  try {
    const res = await deleteConversationAPI(conv.id)
    if (res.code === 200) {
      conversations.value = conversations.value.filter(c => c.id !== conv.id)
      if (currentConversationId.value === conv.id) {
        startNewConversation()
      }
    }
  } catch (e) {
    console.error('删除会话失败', e)
  }
}

const handleClearAllConversations = async () => {
  if (conversations.value.length === 0) return

  try {
    await ElMessageBox.confirm(
      '清空后将删除当前账号下全部 AI 助手历史会话，且无法恢复。',
      '清空会话历史',
      {
        confirmButtonText: '确认清空',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
  } catch {
    return
  }

  try {
    const res = await clearAllConversationsAPI()
    if (res.code === 200) {
      conversations.value = []
      conversationPage.value = 1
      hasMoreConversations.value = false
      draftConversationState.value = null
      currentConversationId.value = null
      inputText.value = ''
      isHistoryMode.value = false
      messages.value = []
      sidebarOpen.value = false
      ElMessage.success('会话历史已清空')
    }
  } catch (e) {
    console.error('清空会话历史失败', e)
    ElMessage.error('清空会话历史失败')
  }
}

const sendMessage = async () => {
  const question = inputText.value.trim()
  if (!question || isLoading.value || isHistoryMode.value) return

  inputText.value = ''
  messages.value.push({ role: 'user', content: question })

  if (shouldInterceptEmployeeQuestion(question)) {
    const conversationId = await ensureActiveConversation(question)
    messages.value.push(buildEmployeeBoundaryAssistantMessage())
    await persistEmployeeBoundaryMessage(conversationId, question)
    await scrollToBottom()
    return
  }

  isLoading.value = true

  const loadingIdx = messages.value.length
  messages.value.push({ role: 'assistant', content: '', loading: true })
  await scrollToBottom()

  // Auto-create conversation on first message
  await ensureActiveConversation(question)

  try {
    const res = await queryAssistantAPI({
      question,
      mode: answerMode.value,
      conversationId: currentConversationId.value || null,
      modelCode: selectedModelCode.value || null
    })
    const data = res.data
    messages.value[loadingIdx] = {
      role: 'assistant',
      content: data.answer,
      sources: data.sources || [],
      hitType: data.hitType || null,
      providerCode: data.providerCode || null,
      modelCode: data.modelCode || null,
      fallbackUsed: data.fallbackUsed === true,
      latencyMs: typeof data.latencyMs === 'number' ? data.latencyMs : null,
      mode: data.mode || answerMode.value,
      loading: false
    }
  } catch (e) {
    messages.value[loadingIdx] = {
      role: 'assistant',
      content: '抱歉，请求失败，请稍后再试。',
      sources: [],
      loading: false
    }
  } finally {
    isLoading.value = false
    await scrollToBottom()
  }
}

onMounted(async () => {
  loadConversations()
  await loadAssistantModels()
  try {
    const res = await getSuggestionsAPI()
    if (res.code === 200 && Array.isArray(res.data)) {
      suggestions.value = res.data.slice(0, 6)
      refreshWelcomeSuggestions()
    }
  } catch {
    suggestions.value = buildFallbackSuggestionData()
    refreshWelcomeSuggestions()
  }
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@400;500;700;900&family=JetBrains+Mono:wght@400;500&display=swap');

.assistant-page {
  position: relative;
  display: flex;
  height: 100%;
  min-height: 0;
  background: #ffffff;
  color: #1f2937;
  font-family: 'Noto Sans SC', system-ui, sans-serif;
  overflow: hidden;
}

/* === Container === */
.assistant-container {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  max-width: 1040px;
  height: 100%;
  min-height: 0;
  padding: 0 24px;
  margin: 0 auto;
}

/* === Header === */
.assistant-header {
  display: grid;
  grid-template-columns: minmax(72px, 1fr) auto minmax(72px, 1fr);
  align-items: center;
  gap: 18px;
  padding: 24px 0 18px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.header-nav,
.header-role {
  flex-shrink: 0;
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-self: start;
}

.header-main {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  min-width: 0;
  width: min(100%, 760px);
  justify-self: center;
}

/* === Sidebar === */
.sidebar {
  width: 240px;
  height: 100%;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e5e7eb;
  background: #f9fafb;
  flex-shrink: 0;
}

.sidebar-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px 12px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.new-chat-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 16px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  border: none;
  border-radius: 10px;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
}

.new-chat-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.new-chat-icon {
  font-size: 18px;
  font-weight: 700;
}

.sidebar-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid #e5e7eb;
  background: #f9fafb;
  flex-shrink: 0;
}

.sidebar-list::-webkit-scrollbar {
  width: 4px;
}

.sidebar-list::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 2px;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 10px 12px;
  margin: 0 8px 2px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.conv-item:hover {
  background: #f3f4f6;
}

.conv-item--active {
  background: #eff6ff;
}

.conv-item-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.conv-title {
  font-size: 13px;
  color: #374151;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-item--active .conv-title {
  color: #2563eb;
  font-weight: 500;
}

.conv-time {
  font-size: 11px;
  color: #9ca3af;
}

.conv-delete {
  display: none;
  width: 22px;
  height: 22px;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #9ca3af;
  font-size: 16px;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.15s ease;
}

.conv-item:hover .conv-delete {
  display: flex;
}

.conv-delete:hover {
  background: #fee2e2;
  color: #ef4444;
}

.sidebar-empty {
  padding: 24px 16px;
  text-align: center;
  font-size: 13px;
  color: #9ca3af;
}

.load-more-btn {
  display: block;
  width: calc(100% - 24px);
  margin: 4px 12px;
  padding: 8px;
  border: 1px dashed #d1d5db;
  border-radius: 8px;
  background: transparent;
  color: #6b7280;
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.15s ease;
}

.load-more-btn:hover {
  border-color: #93c5fd;
  color: #2563eb;
  background: #eff6ff;
}

.clear-history-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 16px;
  border: 1px solid #fca5a5;
  border-radius: 10px;
  background: #fef2f2;
  color: #dc2626;
  font-size: 14px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
}

.clear-history-btn:hover:not(:disabled) {
  background: #fee2e2;
  border-color: #f87171;
}

.clear-history-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.sidebar-overlay {
  display: none;
  position: fixed;
  inset: 0;
  z-index: 99;
  background: rgba(0, 0, 0, 0.3);
}

.sidebar-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  background: #ffffff;
  color: #374151;
  font-size: 18px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.sidebar-toggle:hover {
  border-color: #93c5fd;
  color: #2563eb;
  background: #eff6ff;
}

/* === History hint === */
.history-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 14px 20px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  font-size: 13px;
  color: #6b7280;
}

.history-hint-icon {
  font-size: 16px;
}

.history-new-btn {
  padding: 6px 14px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
}

.history-new-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
}

.header-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: linear-gradient(135deg, #2563eb, #0ea5e9);
  border-radius: 8px;
  font-weight: 700;
  font-size: 13px;
  color: #fff;
  letter-spacing: 1px;
}

.badge-icon {
  font-size: 10px;
}

.header-info {
  flex: 0 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  text-align: center;
}

.header-title {
  margin: 0;
  font-size: 20px;
  font-weight: 900;
  color: #111827;
  letter-spacing: 0.5px;
}

.header-subtitle {
  margin: 2px 0 0;
  font-size: 12px;
  color: #6b7280;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.assistant-controls {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  min-width: 0;
}

.mode-switch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  width: fit-content;
  padding: 4px;
  border: 1px solid #dbeafe;
  border-radius: 999px;
  background: #f8fbff;
}

.mode-switch--disabled {
  opacity: 0.6;
}

.mode-switch-btn {
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  font-family: inherit;
  padding: 7px 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.mode-switch-btn:hover:not(:disabled) {
  color: #1d4ed8;
  background: #eff6ff;
}

.mode-switch-btn--active {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #fff;
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.2);
}

.mode-switch-btn:disabled {
  cursor: not-allowed;
}

.model-select-panel {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  min-width: 0;
  padding: 5px 10px;
  border: 1px solid #dbeafe;
  border-radius: 999px;
  background: linear-gradient(135deg, #f8fbff, #eef7ff);
}

.model-select-panel--disabled {
  opacity: 0.6;
}

.model-select-label {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 700;
  color: #1d4ed8;
}

.model-select {
  width: 168px;
  flex-shrink: 0;
}

.model-select-panel :deep(.el-select__wrapper) {
  min-height: 30px;
  border-radius: 999px;
  box-shadow: 0 0 0 1px #bfdbfe inset;
}

.header-role {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;
  border: 1px solid #dbeafe;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  color: #2563eb;
  white-space: nowrap;
  background: #eff6ff;
  justify-self: end;
}

.role-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #2563eb;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* === Chat area === */
.chat-area {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px 0 12px;
  scroll-behavior: smooth;
}

.chat-area::-webkit-scrollbar {
  width: 4px;
}

.chat-area::-webkit-scrollbar-track {
  background: transparent;
}

.chat-area::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 2px;
}

/* === Welcome zone === */
.welcome-zone {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 28px 20px 24px;
  text-align: center;
}

.welcome-icon {
  width: 64px;
  height: 64px;
  margin-bottom: 20px;
  color: #2563eb;
  opacity: 0.9;
}

.welcome-icon svg {
  width: 100%;
  height: 100%;
}

.welcome-title {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 900;
  color: #111827;
  letter-spacing: -0.5px;
}

.welcome-desc {
  margin: 0 0 20px;
  font-size: 14px;
  color: #6b7280;
  max-width: 400px;
  line-height: 1.6;
}

/* === Suggestions === */
.suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  max-width: 600px;
}

.suggestion-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: #ffffff;
  border: 1px solid #dbeafe;
  border-radius: 12px;
  color: #334155;
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.05);
  transition: all 0.2s ease;
}

.suggestion-pill:hover {
  background: #eff6ff;
  border-color: #93c5fd;
  color: #2563eb;
  transform: translateY(-1px);
}

.pill-arrow {
  color: #2563eb;
  font-weight: 700;
}

/* === Messages === */
.message {
  margin-bottom: 20px;
  animation: fadeSlideIn 0.3s ease;
}

@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.message--user {
  display: flex;
  justify-content: flex-end;
}

.user-bubble {
  max-width: 70%;
  padding: 12px 18px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  border-radius: 18px 18px 4px 18px;
  color: #fff;
  font-size: 14px;
  line-height: 1.6;
}

.user-bubble p {
  margin: 0;
}

.message--assistant {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.assistant-avatar {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2563eb, #0ea5e9);
  border-radius: 10px;
  font-size: 12px;
  color: #fff;
  margin-top: 2px;
}

.assistant-content {
  flex: 1;
  min-width: 0;
}

.assistant-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 32px;
  margin-bottom: 8px;
}

.assistant-meta-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.assistant-meta-tag--model {
  background: #ecfeff;
  color: #0f766e;
}

.assistant-meta-tag--latency {
  background: #f8fafc;
  color: #64748b;
}

.assistant-bubble {
  background: none;
}

/* === Loading === */
.loading-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 0;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #2563eb;
  animation: dotBounce 1s ease-in-out infinite;
}

.dot:nth-child(2) { animation-delay: 0.16s; }
.dot:nth-child(3) { animation-delay: 0.32s; }

@keyframes dotBounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

.loading-text {
  font-size: 13px;
  color: #6b7280;
  margin-left: 4px;
}

/* === KB miss hint === */
.kb-miss-hint {
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 10px;
  color: #dc2626;
  font-size: 13px;
  line-height: 1.6;
  font-weight: 500;
}

.assistant-status-hint {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 14px;
  margin-bottom: 10px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.6;
  font-weight: 500;
}

.assistant-status-hint--info {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1d4ed8;
}

.assistant-status-hint--warning {
  background: #fffbeb;
  border: 1px solid #fcd34d;
  color: #b45309;
}

.assistant-status-hint--danger {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
}

.assistant-status-hint--neutral {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #475569;
}

.assistant-status-badge {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  font-size: 11px;
  font-weight: 700;
}

.mode-result-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 600;
}

/* === Answer text === */
.answer-text {
  font-size: 14px;
  line-height: 1.8;
  color: #374151;
}

.answer-text :deep(strong) {
  color: #111827;
  font-weight: 700;
}

.answer-text :deep(code) {
  padding: 2px 6px;
  background: #eff6ff;
  border-radius: 4px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  color: #2563eb;
}

.action-guide-section {
  margin-top: 18px;
  padding: 14px 16px;
  border: 1px solid #dbeafe;
  border-radius: 14px;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
}

.action-guide-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
}

.action-guide-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  background: #dbeafe;
  font-size: 10px;
}

.action-guide-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.action-guide-card {
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
}

.action-guide-card-title {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 700;
  color: #0f172a;
}

.action-guide-list {
  margin: 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #475569;
  font-size: 12px;
  line-height: 1.6;
}


/* === Sources === */
.sources-section {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

.sources-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
  margin-bottom: 8px;
}

.sources-icon {
  font-size: 14px;
}

.sources-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.source-card {
  padding: 8px 12px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
  transition: all 0.2s ease;
}

.source-card:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.source-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.source-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  flex-shrink: 0;
}

.source-tag--readme { background: rgba(34, 197, 94, 0.15); color: #4ade80; }
.source-tag--project { background: rgba(59, 130, 246, 0.15); color: #60a5fa; }
.source-tag--front { background: rgba(249, 115, 22, 0.15); color: #fb923c; }
.source-tag--back { background: rgba(168, 85, 247, 0.15); color: #c084fc; }
.source-tag--role { background: rgba(236, 72, 153, 0.15); color: #f472b6; }

.source-title {
  flex: 1;
  font-size: 12px;
  color: #475569;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-expand {
  font-size: 10px;
  color: #94a3b8;
  flex-shrink: 0;
}

.source-snippet {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #e5e7eb;
  font-size: 12px;
  line-height: 1.6;
  color: #64748b;
}

/* === Message actions === */
.msg-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.15s ease;
  margin-top: 6px;
}

.msg-actions--assistant {
  margin-top: 8px;
}

.message:hover .msg-actions {
  opacity: 1;
}

.msg-action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.15s ease;
}

.msg-action-btn:hover {
  background: #f3f4f6;
  color: #6b7280;
}

.msg-action-btn--copied {
  color: #2563eb;
}

.msg-action-btn--copied:hover {
  background: #eff6ff;
  color: #2563eb;
}

/* === Input area === */
.input-area {
  position: sticky;
  bottom: 0;
  z-index: 2;
  padding: 16px 0 20px;
  border-top: 1px solid #e5e7eb;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92) 0%, #ffffff 30%);
  backdrop-filter: blur(8px);
  flex-shrink: 0;
}

.employee-scope-tip {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 14px;
  margin-bottom: 10px;
  border: 1px solid #dbeafe;
  border-radius: 12px;
  background: linear-gradient(135deg, #f8fbff, #eef6ff);
  color: #1e40af;
  font-size: 12px;
  line-height: 1.6;
}

.employee-scope-badge {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 11px;
  font-weight: 700;
}

.input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 4px 4px 20px;
  background: #ffffff;
  border: 1px solid #d1d5db;
  border-radius: 16px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
  transition: border-color 0.2s ease;
}

.input-wrapper:focus-within {
  border-color: #60a5fa;
}

.chat-input {
  flex: 1;
  padding: 12px 0;
  background: none;
  border: none;
  outline: none;
  color: #111827;
  font-size: 14px;
  font-family: inherit;
}

.chat-input::placeholder {
  color: #9ca3af;
}

.chat-input:disabled {
  opacity: 0.5;
}

.send-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  border: none;
  border-radius: 12px;
  color: #fff;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.05);
  box-shadow: 0 0 20px rgba(59, 130, 246, 0.3);
}

.send-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.send-btn svg {
  width: 18px;
  height: 18px;
}

.input-hint {
  margin: 6px 0 0;
  font-size: 11px;
  color: #94a3b8;
  text-align: center;
  line-height: 1.5;
}

/* === Responsive === */
@media screen and (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: -260px;
    top: 0;
    bottom: 0;
    z-index: 100;
    box-shadow: none;
    transition: left 0.3s ease, box-shadow 0.3s ease;
  }

  .sidebar--open {
    left: 0;
    box-shadow: 4px 0 24px rgba(0, 0, 0, 0.12);
  }

  .sidebar-overlay {
    display: block;
  }

  .assistant-container {
    padding: 0 12px;
  }

  .assistant-header {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }

  .header-main {
    order: 3;
    width: 100%;
    justify-content: center;
  }

  .header-subtitle {
    display: none;
  }

  .mode-switch {
    width: 100%;
    border-radius: 16px;
    flex-wrap: wrap;
  }

  .assistant-controls {
    width: 100%;
  }

  .mode-switch-btn {
    flex: 1 1 220px;
    text-align: center;
  }

  .model-select-panel {
    width: 100%;
    flex-wrap: wrap;
    justify-content: center;
  }

  .model-select {
    width: 100%;
  }

  .model-select-tip {
    max-width: none;
    text-align: center;
  }

  .welcome-title {
    font-size: 22px;
  }

  .employee-scope-tip {
    align-items: stretch;
  }

  .suggestions {
    flex-direction: column;
  }

  .user-bubble {
    max-width: 85%;
  }
}

@media screen and (min-width: 769px) {
  .sidebar-toggle {
    display: none;
  }

  .sidebar-overlay {
    display: none;
  }
}
</style>
