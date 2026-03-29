<template>
  <el-card>
    <el-form :inline="true" :model="searchForm">
      <el-form-item label="公告标题">
        <el-input v-model="searchForm.title" placeholder="请输入公告标题" clearable />
      </el-form-item>
      <el-form-item label="受众角色">
        <el-select v-model="searchForm.targetRole" clearable placeholder="全部" style="width: 140px;">
          <el-option v-for="item in targetRoleOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="目标部门" v-if="isSuperAdminUser">
        <el-select v-model="searchForm.targetDeptId" clearable placeholder="全部" style="width: 160px;">
          <el-option v-for="dept in deptOptions" :key="dept.id" :label="dept.name" :value="dept.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
        <el-button type="success" @click="handleAdd">新增公告</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border style="width: 100%">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="title" label="公告标题" min-width="180" />
      <el-table-column label="受众" min-width="160">
        <template #default="scope">{{ audienceLabel(scope.row) }}</template>
      </el-table-column>
      <el-table-column prop="date" label="发布时间" width="180" />
      <el-table-column prop="author" label="发布人" width="120" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="handleView(scope.row)">查看</el-button>
          <el-button size="small" type="primary" :disabled="!canManage(scope.row)" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" :disabled="!canManage(scope.row)" @click="handleDelete(scope.row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="50%">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" :disabled="isView">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" :disabled="isView" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" :disabled="isView" placeholder="请输入正文" />
        </el-form-item>
        <el-form-item label="受众角色" prop="targetRole">
          <el-select v-model="form.targetRole" style="width: 100%;" :disabled="isView || !isSuperAdminUser">
            <el-option v-for="item in formTargetRoleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="showTargetDeptField" label="目标部门">
          <el-select
            v-model="form.targetDeptId"
            style="width: 100%;"
            clearable
            :disabled="isView || !isSuperAdminUser || form.targetRole === 'all'"
          >
            <el-option v-for="dept in deptOptions" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status" :disabled="isView">
            <el-radio :value="1">已发布</el-radio>
            <el-radio :value="0">草稿</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="发布时间">
          <el-date-picker
            v-model="form.publishDate"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="不填则已发布状态自动取当前时间"
            :disabled="isView"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer v-if="!isView">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确认</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { isSuperAdmin } from '@/utils/auth'
import {
  createNoticeAPI,
  deleteNoticeAPI,
  getDeptOptionsAPI,
  getNoticeDetailAPI,
  getNoticePageAPI,
  updateNoticeAPI
} from '@/api/system'

const userStore = useUserStore()
const isSuperAdminUser = computed(() => isSuperAdmin(userStore.role))

const deptOptions = ref([])
const searchForm = reactive({ title: '', targetRole: '', targetDeptId: null })
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isView = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  title: '',
  content: '',
  targetRole: 'all',
  targetDeptId: null,
  status: 1,
  publishDate: ''
})

const targetRoleOptions = computed(() => (
  isSuperAdminUser.value
    ? [
        { label: '管理员', value: 'admin' },
        { label: '全员', value: 'all' }
      ]
    : [{ label: '本部门员工', value: 'employee' }]
))

const formTargetRoleOptions = computed(() => (
  isSuperAdminUser.value
    ? [
        { label: '管理员', value: 'admin' },
        { label: '全员', value: 'all' }
      ]
    : [{ label: '本部门员工', value: 'employee' }]
))

const showTargetDeptField = computed(() => isSuperAdminUser.value || form.targetRole === 'employee')

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
  targetRole: [{ required: true, message: '请选择受众角色', trigger: 'change' }],
  status: [{ required: true, message: '请选择公告状态', trigger: 'change' }]
}

const normalizeDateTime = (value) => {
  if (!value) return ''
  return String(value).replace('T', ' ').substring(0, 19)
}

const audienceLabel = (row = {}) => {
  if (row.targetRole === 'all') return '全员'
  if (row.targetRole === 'admin') return row.targetDeptName ? `${row.targetDeptName} 管理员` : '管理员'
  if (row.targetRole === 'employee') return row.targetDeptName ? `${row.targetDeptName} 员工` : '本部门员工'
  return '--'
}

const canManage = (row) => isSuperAdminUser.value || (row.targetRole === 'employee' && row.targetDeptId === userStore.deptId)

const resetForm = () => {
  form.id = null
  form.title = ''
  form.content = ''
  form.targetRole = isSuperAdminUser.value ? 'all' : 'employee'
  form.targetDeptId = isSuperAdminUser.value ? null : userStore.deptId
  form.status = 1
  form.publishDate = ''
}

const loadDeptOptions = async () => {
  const res = await getDeptOptionsAPI()
  if (res.code !== 200) {
    throw new Error(res.msg || '部门列表加载失败')
  }
  deptOptions.value = res.data || []
  if (!isSuperAdminUser.value) {
    searchForm.targetDeptId = userStore.deptId
    form.targetRole = 'employee'
    form.targetDeptId = userStore.deptId
  }
}

const loadList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      title: searchForm.title || undefined,
      targetRole: searchForm.targetRole || undefined,
      targetDeptId: searchForm.targetDeptId || undefined
    }
    const res = await getNoticePageAPI(params)
    if (res.code !== 200) {
      throw new Error(res.msg || '公告查询失败')
    }
    const pageData = res.data || {}
    tableData.value = (pageData.records || []).map((item) => ({
      ...item,
      date: normalizeDateTime(item.date || item.publishTime)
    }))
    total.value = pageData.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载公告失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadList()
}

const resetSearch = () => {
  searchForm.title = ''
  searchForm.targetRole = ''
  searchForm.targetDeptId = isSuperAdminUser.value ? null : userStore.deptId
  currentPage.value = 1
  loadList()
}

const handleSizeChange = (value) => {
  pageSize.value = value
  currentPage.value = 1
  loadList()
}

const handleCurrentChange = (value) => {
  currentPage.value = value
  loadList()
}

const handleAdd = () => {
  isView.value = false
  dialogTitle.value = '新增公告'
  resetForm()
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

const openByDetail = async (row, viewMode) => {
  const res = await getNoticeDetailAPI(row.id)
  if (res.code !== 200) {
    throw new Error(res.msg || '公告详情查询失败')
  }
  const detail = res.data || {}
  isView.value = viewMode
  dialogTitle.value = viewMode ? '查看公告' : '编辑公告'
  Object.assign(form, {
    id: detail.id,
    title: detail.title || '',
    content: detail.content || '',
    targetRole: detail.targetRole || (isSuperAdminUser.value ? 'all' : 'employee'),
    targetDeptId: detail.targetDeptId || (isSuperAdminUser.value ? null : userStore.deptId),
    status: detail.status ?? 1,
    publishDate: normalizeDateTime(detail.publishTime || detail.date)
  })
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

const handleView = async (row) => {
  try {
    await openByDetail(row, true)
  } catch (error) {
    ElMessage.error(error.message || '加载公告详情失败')
  }
}

const handleEdit = async (row) => {
  if (!canManage(row)) {
    ElMessage.warning('当前账号无权编辑该公告')
    return
  }
  try {
    await openByDetail(row, false)
  } catch (error) {
    ElMessage.error(error.message || '加载公告详情失败')
  }
}

const handleDelete = (row) => {
  if (!canManage(row)) {
    ElMessage.warning('当前账号无权删除该公告')
    return
  }
  ElMessageBox.confirm('确认删除该公告吗?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      const res = await deleteNoticeAPI(row.id)
      if (res.code !== 200) {
        throw new Error(res.msg || '删除失败')
      }
      ElMessage.success('删除成功')
      await loadList()
    })
    .catch(() => {})
}

const handleSave = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    try {
      const payload = {
        title: form.title,
        content: form.content,
        targetRole: isSuperAdminUser.value ? form.targetRole : 'employee',
        targetDeptId: isSuperAdminUser.value ? (form.targetRole === 'all' ? null : form.targetDeptId) : userStore.deptId,
        status: form.status,
        publishTime: form.publishDate ? String(form.publishDate).replace(' ', 'T') : undefined
      }
      const res = form.id ? await updateNoticeAPI(form.id, payload) : await createNoticeAPI(payload)
      if (res.code !== 200) {
        throw new Error(res.msg || '保存失败')
      }
      ElMessage.success(form.id ? '修改成功' : '新增成功')
      dialogVisible.value = false
      await loadList()
    } catch (error) {
      ElMessage.error(error.message || '保存失败')
    }
  })
}

onMounted(async () => {
  try {
    resetForm()
    await loadDeptOptions()
    await loadList()
  } catch (error) {
    ElMessage.error(error.message || '初始化失败')
  }
})
</script>
