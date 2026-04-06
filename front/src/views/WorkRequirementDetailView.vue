<template>
  <div class="work-requirement-detail">
    <el-page-header @back="goBack" style="margin-bottom: 20px;">
      <template #content>工作要求详情</template>
    </el-page-header>

    <el-card v-loading="loading">
      <el-descriptions :column="2" border style="margin-bottom: 24px;">
        <el-descriptions-item label="工作内容" :span="2">{{ info.content || '-' }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ fmtTime(info.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="截止时间">{{ fmtTime(info.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <el-tag :type="statusTagType(info.status)" size="small">{{ info.statusLabel || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="驳回次数">{{ info.rejectCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="超时状态">
          <el-tag :type="overdueTagType(info.overdueLabel)" size="small">{{ info.overdueLabel || '正常' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="首次超时">{{ fmtTime(info.overdueAt) }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="info.overdueCurrent || info.lateSubmission" class="overdue-notice-stack">
        <el-alert
          v-if="info.overdueCurrent"
          title="当前任务已超时，请尽快补充执行结果并提交。"
          type="error"
          :closable="false"
          show-icon
        />
        <el-alert
          v-if="info.lateSubmission"
          :title="info.status === 3 ? '该任务为逾期完成，请留意后续时效要求。' : '你本次提交已属于逾期提交。'"
          type="warning"
          :closable="false"
          show-icon
        />
      </div>

      <el-steps :active="activeStep" finish-status="success" align-center style="margin-bottom: 30px;">
        <el-step title="接受任务" :description="info.acceptedAt ? fmtTime(info.acceptedAt) : ''" />
        <el-step title="提交执行结果" :description="info.submittedAt ? fmtTime(info.submittedAt) : ''" />
        <el-step title="审核通过" :description="info.reviewedAt ? fmtTime(info.reviewedAt) : ''" />
      </el-steps>

      <div v-if="info.status === 0" class="action-zone">
        <p class="action-hint action-hint--center">请确认是否接受该工作要求，或选择拒收。</p>
        <el-space class="action-buttons-center">
          <el-button type="primary" @click="handleAccept">接受</el-button>
          <el-button type="danger" plain @click="handleReject">拒收</el-button>
        </el-space>
      </div>

      <div v-else-if="info.status === 1 || info.status === 5" class="action-zone">
        <p v-if="info.status === 5" class="action-hint action-hint--center" style="color: #e6a23c;">
          执行结果已被驳回（{{ info.reviewerName }}），请重新提交。
        </p>
        <p v-if="info.overdueCurrent" class="action-hint action-hint--center action-hint--danger">
          该任务已超过截止时间，请优先补交执行结果。
        </p>
        <p v-else class="action-hint action-hint--center action-hint--danger">请先完成工作后提交执行结果。</p>
        <el-form ref="submitFormRef" :model="submitForm" :rules="submitRules" label-width="100px" class="submit-form-centered">
          <el-form-item label="执行结果" prop="executeResult">
            <el-input v-model="submitForm.executeResult" type="textarea" :rows="4" placeholder="请描述执行结果" />
          </el-form-item>
          <el-form-item label="附件上传">
            <el-upload
              :action="uploadUrl"
              :headers="uploadHeaders"
              :on-success="handleUploadSuccess"
              :on-remove="handleUploadRemove"
              :file-list="fileList"
              list-type="picture-card"
              accept="image/*"
              :limit="5"
            >
              <el-icon><Plus /></el-icon>
              <template #tip>
                <div class="el-upload__tip">支持图片文件，最多5个</div>
              </template>
            </el-upload>
          </el-form-item>
          <el-form-item class="submit-button-row">
            <el-button type="primary" @click="handleSubmit">提交执行结果</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div v-else-if="info.status === 2" class="action-zone">
        <div class="review-pending-layout">
          <div v-if="info.executeResult" class="submitted-content submitted-content--focus">
            <h4 class="submitted-title">提交的执行结果</h4>
            <p class="submitted-text submitted-text--large">{{ info.executeResult }}</p>
            <div v-if="info.attachments && info.attachments.length" class="submitted-images submitted-images--center">
              <el-image
                v-for="att in info.attachments"
                :key="att.id"
                :src="normalizeUploadUrl(att.filePath)"
                :preview-src-list="info.attachments.map(a => normalizeUploadUrl(a.filePath))"
                preview-teleported
                fit="cover"
                style="width: 96px; height: 96px; margin-right: 8px; border-radius: 6px;"
              />
            </div>
          </div>

          <div class="review-status-card">
            <h3>等待管理员审核</h3>
            <p>提交时间：{{ fmtTime(info.submittedAt) }}</p>
            <p>提交口径：{{ info.lateSubmission ? '逾期提交' : '按时提交' }}</p>
          </div>
        </div>
      </div>

      <div v-else-if="info.status === 3" class="action-zone">
        <el-result icon="success" title="工作要求已完成">
          <template #extra>
            <p>审核人：{{ info.reviewerName || '-' }} · 审核时间：{{ fmtTime(info.reviewedAt) }}</p>
            <p>完成口径：{{ info.lateSubmission ? '逾期完成' : '按时完成' }}</p>
          </template>
        </el-result>
        <div v-if="info.executeResult" class="submitted-content">
          <h4>执行结果</h4>
          <p>{{ info.executeResult }}</p>
          <div v-if="info.attachments && info.attachments.length" class="submitted-images">
            <el-image
              v-for="att in info.attachments"
              :key="att.id"
              :src="normalizeUploadUrl(att.filePath)"
              :preview-src-list="info.attachments.map(a => normalizeUploadUrl(a.filePath))"
              preview-teleported
              fit="cover"
              style="width: 80px; height: 80px; margin-right: 8px; border-radius: 4px;"
            />
          </div>
        </div>
      </div>

      <div v-else-if="info.status === 4" class="action-zone">
        <el-result icon="warning" title="已拒收该工作要求" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getToken } from '@/utils/auth'
import {
  getWorkRequirementAssignDetailAPI,
  acceptWorkRequirementAPI,
  rejectWorkRequirementAPI,
  submitWorkRequirementAPI
} from '@/api/workRequirement'

const route = useRoute()
const router = useRouter()
const assignId = computed(() => Number(route.params.assignId))

const loading = ref(false)
const info = ref({})

const submitFormRef = ref(null)
const submitForm = reactive({ executeResult: '', attachmentPaths: [] })
const submitRules = { executeResult: [{ required: true, message: '请描述执行结果', trigger: 'blur' }] }
const fileList = ref([])
const uploadUrl = '/api/upload/image'
const uploadHeaders = computed(() => {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
})

const activeStep = computed(() => {
  const status = info.value.status
  if (status === 0 || status === 4) return 0
  if (status === 1 || status === 5) return 1
  if (status === 2) return 2
  if (status === 3) return 3
  return 0
})

const statusTagType = (status) => {
  const map = { 0: 'info', 1: '', 2: 'warning', 3: 'success', 4: 'danger', 5: 'danger' }
  return map[status] ?? 'info'
}

const overdueTagType = (label) => {
  if (label === '超时中') return 'danger'
  if (label === '逾期提交' || label === '逾期完成') return 'warning'
  return 'success'
}

const fmtTime = (val) => {
  if (!val) return '--'
  return String(val).replace('T', ' ').substring(0, 19)
}

const normalizeUploadUrl = (path) => {
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) return path
  if (path.startsWith('/api/uploads/')) return path
  if (path.startsWith('/uploads/')) return `/api${path}`
  return path
}

const syncSubmitState = () => {
  submitForm.executeResult = info.value.executeResult || ''
  submitForm.attachmentPaths = (info.value.attachments || []).map(item => item.filePath)
  fileList.value = (info.value.attachments || []).map(item => ({
    name: item.fileName,
    url: normalizeUploadUrl(item.filePath),
    rawPath: item.filePath
  }))
}

const goBack = () => {
  router.back()
}

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await getWorkRequirementAssignDetailAPI(assignId.value)
    if (res.code !== 200) throw new Error(res.msg || '加载失败')
    info.value = res.data || {}
    syncSubmitState()
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleAccept = () => {
  ElMessageBox.confirm('确认接受该工作要求？', '确认', { type: 'info' }).then(async () => {
    try {
      const res = await acceptWorkRequirementAPI(assignId.value)
      if (res.code !== 200) throw new Error(res.msg || '操作失败')
      ElMessage.success('已接受')
      await loadDetail()
    } catch (e) {
      ElMessage.error(e.message || '操作失败')
    }
  }).catch(() => {})
}

const handleReject = () => {
  ElMessageBox.confirm('确认拒收该工作要求？拒收后将无法再接受。', '警告', { type: 'warning' }).then(async () => {
    try {
      const res = await rejectWorkRequirementAPI(assignId.value)
      if (res.code !== 200) throw new Error(res.msg || '操作失败')
      ElMessage.success('已拒收')
      await loadDetail()
    } catch (e) {
      ElMessage.error(e.message || '操作失败')
    }
  }).catch(() => {})
}

const handleUploadSuccess = (response, file) => {
  if (response.code === 200) {
    submitForm.attachmentPaths.push(response.data)
    file.rawPath = response.data
    file.url = normalizeUploadUrl(response.data)
    return
  }
  ElMessage.error(response.msg || '上传失败')
}

const handleUploadRemove = (file) => {
  const rawPath = file.rawPath || file.response?.data || file.url
  const index = submitForm.attachmentPaths.indexOf(rawPath)
  if (index !== -1) {
    submitForm.attachmentPaths.splice(index, 1)
  }
}

const handleSubmit = () => {
  submitFormRef.value?.validate(async (valid) => {
    if (!valid) return
    try {
      const res = await submitWorkRequirementAPI(assignId.value, {
        executeResult: submitForm.executeResult,
        attachmentPaths: submitForm.attachmentPaths
      })
      if (res.code !== 200) throw new Error(res.msg || '提交失败')
      ElMessage.success('提交成功，等待审核')
      await loadDetail()
    } catch (e) {
      ElMessage.error(e.message || '提交失败')
    }
  })
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.work-requirement-detail {
  padding: 20px;
}

.action-zone {
  padding: 20px 0;
}

.overdue-notice-stack {
  display: grid;
  gap: 12px;
  margin: 0 0 24px;
}

.action-hint {
  margin-bottom: 16px;
  color: #606266;
  font-size: 14px;
}

.action-hint--center {
  text-align: center;
}

.action-hint--danger {
  color: #f56c6c;
  font-weight: 600;
}

.action-buttons-center {
  display: flex;
  justify-content: center;
  width: 100%;
}

.submit-form-centered {
  max-width: 600px;
  margin: 0 auto;
}

:deep(.submit-form-centered .el-form-item__content) {
  justify-content: center;
}

:deep(.submit-form-centered .el-upload) {
  display: inline-flex;
}

.submit-button-row :deep(.el-form-item__content) {
  justify-content: center;
}

.submitted-content {
  margin-top: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.submitted-content--focus {
  max-width: 920px;
  margin: 0 auto 24px;
  text-align: center;
  padding: 24px;
}

.submitted-title {
  margin: 0 0 12px;
  color: #303133;
  font-size: 18px;
}

.submitted-text {
  margin: 0 0 12px;
  color: #606266;
  white-space: pre-wrap;
}

.submitted-text--large {
  font-size: 20px;
  line-height: 1.8;
  color: #303133;
}

.submitted-content h4 {
  margin: 0 0 8px;
  color: #303133;
}

.submitted-content p {
  margin: 0 0 12px;
  color: #606266;
  white-space: pre-wrap;
}

.submitted-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.submitted-images--center {
  justify-content: center;
}

.review-pending-layout {
  display: grid;
  gap: 20px;
}

.review-status-card {
  text-align: center;
  padding: 8px 0 4px;
}

.review-status-card h3 {
  margin: 0 0 10px;
  font-size: 28px;
  color: #303133;
}

.review-status-card p {
  margin: 0;
  font-size: 16px;
  color: #606266;
}
</style>
