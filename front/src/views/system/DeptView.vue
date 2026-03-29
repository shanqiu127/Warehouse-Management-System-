<template>
  <el-card>
    <el-form :inline="true" :model="searchForm">
      <el-form-item label="部门名称">
        <el-input v-model="searchForm.deptName" placeholder="请输入部门名称" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
        <el-button type="success" @click="handleAdd">新增部门</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" border style="width: 100%" row-key="id" v-loading="loading">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="deptName" label="部门名称" />
      <el-table-column prop="manager" label="负责人（可多位）" />
      <el-table-column prop="contactPhone" label="联系电话" />
      <el-table-column prop="createTime" label="创建时间" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" @click="handleEdit(scope.row)">编辑</el-button>
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

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="400px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="form.deptName"></el-input>
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.manager" placeholder="支持填写多位负责人，使用逗号或顿号分隔"></el-input>
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone"></el-input>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确认</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createDeptAPI,
  deleteDeptAPI,
  getDeptDetailAPI,
  getDeptPageAPI,
  updateDeptAPI
} from '@/api/system'

const searchForm = reactive({ deptName: '' })
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('新增部门')
const formRef = ref(null)
const form = reactive({ id: null, deptName: '', manager: '', contactPhone: '', description: '' })

const rules = {
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
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
      deptName: searchForm.deptName || undefined
    }
    const res = await getDeptPageAPI(params)
    if (res.code !== 200) {
      throw new Error(res.msg || '部门查询失败')
    }
    const pageData = res.data || {}
    tableData.value = (pageData.records || []).map((item) => ({
      ...item,
      createTime: normalizeDateTime(item.createTime)
    }))
    total.value = pageData.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载部门失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadList()
}

const resetSearch = () => {
  searchForm.deptName = ''
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

const handleAdd = () => {
  dialogTitle.value = '新增部门'
  form.id = null
  form.deptName = ''
  form.manager = ''
  form.contactPhone = ''
  form.description = ''
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  try {
    const res = await getDeptDetailAPI(row.id)
    if (res.code !== 200) {
      throw new Error(res.msg || '部门详情查询失败')
    }
    const detail = res.data || {}
    dialogTitle.value = '编辑部门'
    Object.assign(form, {
      id: detail.id,
      deptName: detail.deptName || '',
      manager: detail.manager || detail.leader || '',
      contactPhone: detail.contactPhone || detail.phone || '',
      description: detail.description || ''
    })
    formRef.value?.clearValidate()
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载部门详情失败')
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该部门?', '提示', { type: 'warning' })
    .then(async () => {
      const res = await deleteDeptAPI(row.id)
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
        deptName: form.deptName,
        leader: form.manager || '',
        phone: form.contactPhone || '',
        description: form.description || ''
      }
      const res = form.id ? await updateDeptAPI(form.id, payload) : await createDeptAPI(payload)
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