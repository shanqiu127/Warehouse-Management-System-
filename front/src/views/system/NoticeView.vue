<template>
  <el-card>
    <el-form :inline="true" :model="searchForm" class="search-form">
      <el-form-item label="公告标题">
        <el-input v-model="searchForm.title" placeholder="请输入公告标题" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px;">
          <el-option label="已发布" :value="1" />
          <el-option label="草稿" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
        <el-button type="success" @click="handleAdd" v-permission="['admin']">新增公告</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" border style="width: 100%" v-loading="loading">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="title" label="公告标题" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{ scope.row.status === 1 ? '已发布' : '草稿' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="date" label="发布时间" width="180" />
      <el-table-column prop="author" label="发布人" width="120" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="handleView(scope.row)">查看</el-button>
          <el-button size="small" type="primary" @click="handleEdit(scope.row)" v-permission="['admin']">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)" v-permission="['admin']">删除</el-button>
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

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="50%">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" :disabled="isView">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" :disabled="isView" placeholder="请输入标题"></el-input>
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input type="textarea" v-model="form.content" :disabled="isView" :rows="4" placeholder="请输入正文"></el-input>
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
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave">确认</el-button>
        </span>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createNoticeAPI,
  deleteNoticeAPI,
  getNoticeDetailAPI,
  getNoticePageAPI,
  updateNoticeAPI
} from '@/api/system'

const searchForm = reactive({ title: '', status: null })
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isView = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, title: '', content: '', status: 1, publishDate: '' })

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
  status: [{ required: true, message: '请选择公告状态', trigger: 'change' }]
}

const normalizeDateTime = (val) => {
  if (!val) return ''
  return String(val).replace('T', ' ')
}

const loadList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      title: searchForm.title || undefined,
      status: searchForm.status === null ? undefined : searchForm.status
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
  searchForm.status = null
  currentPage.value = 1
  loadList()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  loadList()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  loadList()
}

const resetForm = () => {
  form.id = null
  form.title = ''
  form.content = ''
  form.status = 1
  form.publishDate = ''
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
  try {
    await openByDetail(row, false)
  } catch (error) {
    ElMessage.error(error.message || '加载公告详情失败')
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该公告吗?', '警告', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
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
        status: form.status,
        publishTime: form.publishDate ? `${String(form.publishDate).replace(' ', 'T')}` : undefined
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

onMounted(() => {
  loadList()
})
</script>