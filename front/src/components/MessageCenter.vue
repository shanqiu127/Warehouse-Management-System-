<template>
  <div v-if="showMessageCenter" class="message-center">
    <el-badge :value="unreadBadgeValue" :hidden="!unreadCount" class="message-badge">
      <button type="button" class="message-button" aria-label="打开站内邮箱" @click="drawerVisible = true">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M3 6.75A1.75 1.75 0 0 1 4.75 5h14.5A1.75 1.75 0 0 1 21 6.75v10.5A1.75 1.75 0 0 1 19.25 19H4.75A1.75 1.75 0 0 1 3 17.25V6.75Zm1.84-.25 7.16 5.61 7.16-5.61H4.84Zm14.66 11V8.41l-7.04 5.52a.75.75 0 0 1-.92 0L4.5 8.41v9.09c0 .28.22.5.5.5h14a.5.5 0 0 0 .5-.5Z" />
        </svg>
      </button>
    </el-badge>

    <el-dialog
      v-model="drawerVisible"
      align-center
      width="720px"
      class="message-dialog"
      :show-close="true"
      @open="loadMessages"
    >
      <div class="dialog-shell">
        <div class="drawer-header">
          <div>
            <p class="drawer-eyebrow">MAILBOX</p>
            <h3>站内邮箱</h3>
            <p class="drawer-desc">逐条已读，支持一键已读和删除全部已读。</p>
          </div>
          <div class="drawer-action-row">
            <el-button link type="primary" :icon="Finished" :disabled="!unreadCount || actionLoading" @click="handleReadAll">一键已读</el-button>
            <el-button link type="danger" :icon="Delete" :disabled="!hasReadMessages || actionLoading" @click="handleDeleteRead">删除全部已读</el-button>
          </div>
        </div>

        <div v-loading="listLoading" class="message-list-shell">
          <div v-if="messageList.length" class="message-list">
            <article v-for="item in messageList" :key="item.id" class="message-card" :class="{ unread: !item.read }">
              <div class="message-card-head">
                <div>
                  <h4>{{ item.title }}</h4>
                  <p class="message-time">{{ formatTime(item.createTime) }}</p>
                </div>
                <el-tag :type="item.read ? 'info' : 'danger'">{{ item.read ? '已读' : '未读' }}</el-tag>
              </div>
              <p class="message-content">{{ item.content }}</p>
              <div class="message-card-foot">
                <span>{{ item.read ? `已读于 ${formatTime(item.readTime)}` : '等待处理' }}</span>
                <el-button link type="primary" :icon="Check" :disabled="item.read || actionLoading" @click="handleRead(item)">标记已读</el-button>
              </div>
            </article>
          </div>
          <el-empty v-else description="暂无消息" />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Finished, Delete, Check } from '@element-plus/icons-vue'
import {
  deleteAllReadMessagesAPI,
  getMessagePageAPI,
  getUnreadMessageCountAPI,
  markAllMessagesReadAPI,
  markMessageReadAPI
} from '@/api/message'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const showMessageCenter = computed(() => Boolean(userStore.token))
const unreadCount = ref(0)
const drawerVisible = ref(false)
const listLoading = ref(false)
const actionLoading = ref(false)
const messageList = ref([])

let pollingTimer = null

const unreadBadgeValue = computed(() => {
  if (unreadCount.value > 99) return '99+'
  return unreadCount.value
})

const hasReadMessages = computed(() => messageList.value.some((item) => item.read))

const formatTime = (val) => {
  if (!val) return '--'
  return String(val).replace('T', ' ').substring(0, 19)
}

const loadUnreadCount = async () => {
  if (!showMessageCenter.value) return
  try {
    const res = await getUnreadMessageCountAPI()
    if (res.code === 200) {
      unreadCount.value = Number(res.data || 0)
    }
  } catch {
    unreadCount.value = 0
  }
}

const loadMessages = async () => {
  if (!showMessageCenter.value) return
  listLoading.value = true
  try {
    const res = await getMessagePageAPI({ pageNum: 1, pageSize: 50 })
    if (res.code !== 200) {
      throw new Error(res.msg || '消息加载失败')
    }
    messageList.value = res.data?.records || []
  } catch (error) {
    ElMessage.error(error.message || '消息加载失败')
  } finally {
    listLoading.value = false
  }
}

const refreshMessageState = async (shouldReloadList = false) => {
  await loadUnreadCount()
  if (shouldReloadList || drawerVisible.value) {
    await loadMessages()
  }
}

const handleRead = async (message) => {
  if (!message || message.read) return
  actionLoading.value = true
  try {
    const res = await markMessageReadAPI(message.id)
    if (res.code !== 200) {
      throw new Error(res.msg || '消息已读失败')
    }
    await refreshMessageState(true)
  } catch (error) {
    ElMessage.error(error.message || '消息已读失败')
  } finally {
    actionLoading.value = false
  }
}

const handleReadAll = async () => {
  if (!unreadCount.value) return
  actionLoading.value = true
  try {
    const res = await markAllMessagesReadAPI()
    if (res.code !== 200) {
      throw new Error(res.msg || '一键已读失败')
    }
    ElMessage.success('全部未读消息已标记为已读')
    await refreshMessageState(true)
  } catch (error) {
    ElMessage.error(error.message || '一键已读失败')
  } finally {
    actionLoading.value = false
  }
}

const handleDeleteRead = async () => {
  if (!hasReadMessages.value) return
  try {
    await ElMessageBox.confirm('确认删除全部已读消息吗？未读消息会保留。', '提示', { type: 'warning' })
  } catch {
    return
  }

  actionLoading.value = true
  try {
    const res = await deleteAllReadMessagesAPI()
    if (res.code !== 200) {
      throw new Error(res.msg || '删除已读消息失败')
    }
    ElMessage.success('已删除全部已读消息')
    await refreshMessageState(true)
  } catch (error) {
    ElMessage.error(error.message || '删除已读消息失败')
  } finally {
    actionLoading.value = false
  }
}

const stopPolling = () => {
  if (pollingTimer) {
    window.clearInterval(pollingTimer)
    pollingTimer = null
  }
}

const startPolling = () => {
  stopPolling()
  if (!showMessageCenter.value) return
  loadUnreadCount()
  pollingTimer = window.setInterval(() => {
    loadUnreadCount()
  }, 15000)
}

watch(drawerVisible, (visible) => {
  if (visible) {
    loadMessages()
    loadUnreadCount()
  }
})

watch(showMessageCenter, (visible) => {
  if (visible) {
    startPolling()
    return
  }
  stopPolling()
  unreadCount.value = 0
  messageList.value = []
  drawerVisible.value = false
}, { immediate: true })

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped>
.message-center {
  display: inline-flex;
  align-items: center;
}

.message-button {
  width: 42px;
  height: 42px;
  border: 1px solid #dbe3ef;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, #ffffff 0%, #f6f9fc 100%);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.message-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 30px -24px rgba(15, 23, 42, 0.8);
}

.message-button svg {
  width: 20px;
  height: 20px;
  fill: #334155;
}

.dialog-shell {
  max-height: min(72vh, 760px);
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 16px;
}

:deep(.message-dialog) {
  max-width: calc(100vw - 32px);
}

:deep(.message-dialog .el-dialog__header) {
  display: none;
}

:deep(.message-dialog .el-dialog__body) {
  padding: 24px;
}

.drawer-header {
  display: grid;
  gap: 14px;
  padding-right: 8px;
}

.drawer-eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.2em;
  color: #0f766e;
}

.drawer-header h3 {
  margin: 0;
  font-size: 1.4rem;
  color: #0f172a;
}

.drawer-desc {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.6;
}

.drawer-action-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.message-list-shell {
  min-height: 240px;
  overflow: auto;
  padding-right: 8px;
}

.message-list {
  display: grid;
  gap: 14px;
}

.message-card {
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  padding: 16px 18px;
  background: #ffffff;
  box-shadow: 0 18px 35px -30px rgba(15, 23, 42, 0.4);
}

.message-card.unread {
  border-color: rgba(239, 68, 68, 0.35);
  background: linear-gradient(180deg, rgba(254, 242, 242, 0.7) 0%, #ffffff 100%);
}

.message-card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.message-card-head h4 {
  margin: 0 0 6px;
  color: #0f172a;
  font-size: 1rem;
}

.message-time,
.message-card-foot span {
  color: #64748b;
  font-size: 0.86rem;
}

.message-content {
  margin: 14px 0;
  color: #334155;
  line-height: 1.7;
  white-space: pre-wrap;
}

.message-card-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

@media (max-width: 640px) {
  .message-card-head,
  .message-card-foot {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>