<template>
  <el-card>
    <el-form :inline="true" :model="searchForm">
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" clearable placeholder="全部" style="width: 140px;">
          <el-option label="未完成" :value="0" />
          <el-option label="待审核" :value="2" />
          <el-option label="已完成" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item label="超时">
        <el-select v-model="searchForm.overdueType" clearable placeholder="全部" style="width: 150px;">
          <el-option label="超时中" value="overdue" />
          <el-option label="逾期提交" value="overdue_submit" />
          <el-option label="正常" value="normal" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="searchForm.keyword" placeholder="搜索内容" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        <el-button type="success" :icon="Plus" @click="handleAdd">发布工作要求</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border style="width: 100%">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="content" label="内容" min-width="180" show-overflow-tooltip />
      <el-table-column label="时间范围" min-width="180">
        <template #default="{ row }">{{ fmtTime(row.startTime) }} ~ {{ fmtTime(row.endTime) }}</template>
      </el-table-column>
      <el-table-column prop="targetScope" label="下达范围" width="100">
        <template #default="{ row }">{{ row.targetScope === 'all' ? '全员' : '指定人员' }}</template>
      </el-table-column>
      <el-table-column label="进度" width="160">
        <template #default="{ row }">{{ row.completedCount }}/{{ row.totalCount }} 完成 · {{ row.pendingReviewCount }} 待审</template>
      </el-table-column>
      <el-table-column prop="overdueCount" label="超时中" width="88" />
      <el-table-column prop="overdueSubmitCount" label="逾期提交" width="88" />
      <el-table-column label="汇总状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.summaryStatus)" size="small">{{ row.summaryStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :icon="ViewIcon" @click="handleDetail(row)">详情</el-button>
          <el-button size="small" type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top: 20px; display: flex; justify-content: flex-end;">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 发布工作要求对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      title="发布工作要求"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="工作内容" prop="content">
          <el-input v-model="createForm.content" type="textarea" :rows="4" placeholder="请输入工作要求内容" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <div class="time-field">
            <el-date-picker
              v-model="createForm.endTime"
              type="datetime"
              placeholder="请选择结束时间"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placement="right-start"
              :teleported="false"
              popper-class="work-requirement-time-popper"
              style="width: 100%"
            />
            <div class="time-shortcuts">
              <el-button
                v-for="item in shortcutButtons"
                :key="item.key"
                size="small"
                plain
                @click="applyTimeShortcut(item.key)"
              >
                {{ item.label }}
              </el-button>
            </div>
            <div class="time-hint">开始时间将自动取点击“确认发布”时的当前时间</div>
          </div>
        </el-form-item>
        <el-form-item label="下达范围" prop="targetScope">
          <el-radio-group v-model="createForm.targetScope">
            <el-radio value="all">全员</el-radio>
            <el-radio value="selected">指定人员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="createForm.targetScope === 'selected'" label="选择人员" prop="employeeUserIds">
          <el-select
            v-model="createForm.employeeUserIds"
            multiple
            filterable
            :teleported="false"
            placeholder="请选择员工"
            style="width: 100%"
          >
            <el-option
              v-for="emp in employeeOptions"
              :key="emp.userId"
              :label="`${emp.realName}（${emp.username}）`"
              :value="emp.userId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :icon="Close" @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :icon="Check" @click="handleCreate">确认发布</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="工作要求详情" width="980px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="工作内容" :span="2">{{ detail.content }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ fmtTime(detail.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="截止时间">{{ fmtTime(detail.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="下达范围">{{ detail.targetScope === 'all' ? '全员' : '指定人员' }}</el-descriptions-item>
        <el-descriptions-item label="发布人">{{ detail.creatorName }}</el-descriptions-item>
      </el-descriptions>

      <h4 style="margin: 20px 0 12px;">员工执行情况</h4>
      <el-table :data="detail.assigns || []" border size="small">
        <el-table-column prop="employeeName" label="员工" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="assignStatusTagType(row.status)" size="small">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="超时状态" width="110">
          <template #default="{ row }">
            <el-tag :type="overdueTagType(row.overdueLabel)" size="small">{{ row.overdueLabel || '正常' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="首次超时" width="170">
          <template #default="{ row }">{{ fmtTime(row.overdueAt) }}</template>
        </el-table-column>
        <el-table-column prop="executeResult" label="执行结果" min-width="160" show-overflow-tooltip />
        <el-table-column label="附件" min-width="140">
          <template #default="{ row }">
            <template v-if="row.attachments && row.attachments.length">
              <div v-for="att in row.attachments" :key="att.id" class="attachment-item">
                <el-image
                  v-if="isImage(att.fileName)"
                  :src="getAttachmentUrl(att)"
                  :preview-src-list="getImagePreviewList(row.attachments)"
                  preview-teleported
                  fit="cover"
                  style="width: 40px; height: 40px; margin-right: 4px; cursor: pointer;"
                />
                <a v-else :href="getAttachmentUrl(att)" target="_blank" class="attachment-link">{{ att.fileName }}</a>
              </div>
            </template>
            <span v-else style="color: #999;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <template v-if="row.status === 2">
              <el-button size="small" type="success" @click="handleReview(row, true)">通过</el-button>
              <el-button size="small" type="warning" @click="handleReview(row, false)">驳回</el-button>
            </template>
            <span v-else style="color: #999;">-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, View as ViewIcon, Delete, Close, Check } from '@element-plus/icons-vue'
import {
  downloadWorkRequirementAttachmentAPI,
  getWorkRequirementPageAPI,
  getWorkRequirementDetailAPI,
  createWorkRequirementAPI,
  deleteWorkRequirementAPI,
  reviewWorkRequirementAPI,
  getDeptEmployeesAPI
} from '@/api/workRequirement'

const searchForm = reactive({ status: '', overdueType: '', keyword: '' })
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const createDialogVisible = ref(false)
const createFormRef = ref(null)
const createForm = reactive({
  content: '',
  endTime: '',
  targetScope: 'all',
  employeeUserIds: []
})
const employeeOptions = ref([])

const detailDialogVisible = ref(false)
const detail = ref({})
const attachmentObjectUrls = ref({})

const createRules = {
  content: [{ required: true, message: '请输入工作内容', trigger: 'blur' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  targetScope: [{ required: true, message: '请选择下达范围', trigger: 'change' }],
  employeeUserIds: [{ required: true, message: '请选择员工', trigger: 'change' }]
}

const shortcutButtons = [
  { key: 'today', label: '今天' },
  { key: 'tomorrow', label: '明天' },
  { key: 'week', label: '一周内' },
  { key: 'month', label: '一个月内' }
]

const formatPickerValue = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
}

const applyTimeShortcut = (key) => {
  const end = new Date()

  if (key === 'today') {
    end.setHours(23, 59, 59, 0)
  } else if (key === 'tomorrow') {
    end.setDate(end.getDate() + 1)
    end.setHours(23, 59, 59, 0)
  } else if (key === 'week') {
    end.setDate(end.getDate() + 7)
    end.setHours(23, 59, 59, 0)
  } else if (key === 'month') {
    end.setMonth(end.getMonth() + 1)
    end.setHours(23, 59, 59, 0)
  }

  createForm.endTime = formatPickerValue(end)
}

const fmtTime = (val) => {
  if (!val) return '--'
  return String(val).replace('T', ' ').substring(0, 19)
}

const statusTagType = (s) => {
  if (s === '已完成') return 'success'
  if (s === '待审核') return 'warning'
  if (s === '拒收') return 'danger'
  return 'info'
}

const assignStatusTagType = (status) => {
  const map = { 0: 'info', 1: '', 2: 'warning', 3: 'success', 4: 'danger', 5: 'danger' }
  return map[status] ?? 'info'
}

const overdueTagType = (label) => {
  if (label === '超时中') return 'danger'
  if (label === '逾期提交' || label === '逾期完成') return 'warning'
  return 'success'
}

const isImage = (name) => /\.(jpe?g|png|gif|webp|bmp)$/i.test(name || '')

const revokeAttachmentUrls = () => {
  Object.values(attachmentObjectUrls.value).forEach((url) => {
    if (url) URL.revokeObjectURL(url)
  })
  attachmentObjectUrls.value = {}
}

const getAttachmentUrl = (attachment) => {
  if (!attachment) return ''
  return attachmentObjectUrls.value[attachment.id] || ''
}

const getImagePreviewList = (attachments = []) => attachments
  .filter((attachment) => isImage(attachment.fileName))
  .map((attachment) => getAttachmentUrl(attachment))
  .filter(Boolean)

const loadAttachmentUrls = async (detailData = {}) => {
  revokeAttachmentUrls()
  const attachments = (detailData.assigns || []).flatMap((assign) => assign.attachments || [])
  if (!attachments.length) return
  const nextUrls = {}
  await Promise.all(attachments.map(async (attachment) => {
    if (!attachment?.id) return
    try {
      const blob = await downloadWorkRequirementAttachmentAPI(attachment.id)
      nextUrls[attachment.id] = URL.createObjectURL(blob)
    } catch {
      nextUrls[attachment.id] = ''
    }
  }))
  attachmentObjectUrls.value = nextUrls
}

const loadList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      status: searchForm.status || undefined,
      overdueType: searchForm.overdueType || undefined,
      keyword: searchForm.keyword || undefined
    }
    const res = await getWorkRequirementPageAPI(params)
    if (res.code !== 200) throw new Error(res.msg || '查询失败')
    const pageData = res.data || {}
    tableData.value = pageData.records || []
    total.value = pageData.total || 0
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { currentPage.value = 1; loadList() }
const resetSearch = () => { searchForm.status = ''; searchForm.overdueType = ''; searchForm.keyword = ''; currentPage.value = 1; loadList() }
const handleSizeChange = () => { currentPage.value = 1; loadList() }
const handleCurrentChange = () => { loadList() }

const handleAdd = async () => {
  createForm.content = ''
  createForm.endTime = ''
  createForm.targetScope = 'all'
  createForm.employeeUserIds = []
  createFormRef.value?.clearValidate()
  try {
    const res = await getDeptEmployeesAPI()
    if (res.code !== 200) throw new Error(res.msg || '员工列表加载失败')
    employeeOptions.value = res.data || []
  } catch (e) {
    ElMessage.error(e.message || '员工列表加载失败')
  }
  createDialogVisible.value = true
}

const handleCreate = () => {
  createFormRef.value?.validate(async (valid) => {
    if (!valid) return
    try {
      const startTime = formatPickerValue(new Date())
      const payload = {
        content: createForm.content,
        startTime,
        endTime: createForm.endTime,
        targetScope: createForm.targetScope,
        employeeUserIds: createForm.targetScope === 'selected' ? createForm.employeeUserIds : undefined
      }
      const res = await createWorkRequirementAPI(payload)
      if (res.code !== 200) throw new Error(res.msg || '发布失败')
      ElMessage.success('发布成功')
      createDialogVisible.value = false
      await loadList()
    } catch (e) {
      ElMessage.error(e.message || '发布失败')
    }
  })
}

const handleDetail = async (row) => {
  try {
    const res = await getWorkRequirementDetailAPI(row.id)
    if (res.code !== 200) throw new Error(res.msg || '详情加载失败')
    detail.value = res.data || {}
    await loadAttachmentUrls(detail.value)
    detailDialogVisible.value = true
  } catch (e) {
    ElMessage.error(e.message || '详情加载失败')
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该工作要求吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteWorkRequirementAPI(row.id)
      if (res.code !== 200) throw new Error(res.msg || '删除失败')
      ElMessage.success('删除成功')
      await loadList()
    } catch (e) {
      ElMessage.error(e.message || '删除失败')
    }
  }).catch(() => {})
}

const handleReview = (assignRow, approved) => {
  const msg = approved ? '确认通过该员工的执行结果？' : '确认驳回该员工的执行结果？将退回重新执行。'
  ElMessageBox.confirm(msg, '审核确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: approved ? 'success' : 'warning'
  }).then(async () => {
    try {
      const res = await reviewWorkRequirementAPI(assignRow.assignId, { approved })
      if (res.code !== 200) throw new Error(res.msg || '审核失败')
      ElMessage.success(approved ? '已通过' : '已驳回')
      const [detailRes] = await Promise.all([
        getWorkRequirementDetailAPI(detail.value.id),
        loadList()
      ])
      if (detailRes.code === 200) {
        detail.value = detailRes.data || {}
        await loadAttachmentUrls(detail.value)
      }
    } catch (e) {
      ElMessage.error(e.message || '审核失败')
    }
  }).catch(() => {})
}

onMounted(() => { loadList() })

onBeforeUnmount(() => {
  revokeAttachmentUrls()
})
</script>

<style scoped>
.time-field {
  width: 100%;
  position: relative;
}

.time-shortcuts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.time-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

:deep(.work-requirement-time-popper .el-picker-panel__sidebar) {
  display: none;
}

:deep(.work-requirement-time-popper .el-picker-panel__footer) {
  display: none;
}

:deep(.time-field .work-requirement-time-popper.el-popper) {
  inset: 0 auto auto calc(100% + 16px) !important;
  transform: none !important;
}

.attachment-item {
  display: inline-flex;
  align-items: center;
  margin-right: 8px;
  margin-bottom: 4px;
}
.attachment-link {
  color: #409eff;
  text-decoration: none;
  font-size: 12px;
}
.attachment-link:hover {
  text-decoration: underline;
}
</style>
