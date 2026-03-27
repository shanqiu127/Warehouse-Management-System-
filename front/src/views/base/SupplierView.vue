<template>
  <el-card>
    <el-form :inline="true" :model="searchForm">
      <el-form-item label="供应商名称">
        <el-input v-model="searchForm.supplierName" placeholder="请输入供应商名称" clearable />
      </el-form-item>
      <el-form-item label="联系人">
        <el-input v-model="searchForm.contact" placeholder="请输入联系人" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
        <el-button type="success" @click="handleAdd" v-permission="['admin', 'employee']">新增供应商</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" border style="width: 100%" v-loading="loading">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="supplierName" label="供应商名称" />
      <el-table-column prop="contact" label="联系人" width="120" />
      <el-table-column prop="phone" label="联系电话" width="150" />
      <el-table-column prop="address" label="联系地址" />
      <!-- 员工与管理员均可执行增删改操作 -->
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="handleView(scope.row)">查看</el-button>
          <el-button size="small" type="primary" @click="handleEdit(scope.row)" v-permission="['admin', 'employee']">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)" v-permission="['admin', 'employee']">删除</el-button>
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

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" :disabled="isView">
        <el-form-item label="供应商名称" required>
          <el-input v-model="form.supplierName" :disabled="isView"></el-input>
        </el-form-item>
        <el-form-item label="联系人" required>
          <el-input v-model="form.contact" :disabled="isView"></el-input>
        </el-form-item>
        <el-form-item label="联系电话" required>
          <el-input v-model="form.phone" :disabled="isView"></el-input>
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input v-model="form.address" type="textarea" :disabled="isView"></el-input>
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createSupplierAPI,
  deleteSupplierAPI,
  getSupplierDetailAPI,
  getSupplierPageAPI,
  updateSupplierAPI
} from '@/api/base'

const searchForm = reactive({ supplierName: '', contact: '' })
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isView = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, supplierName: '', contact: '', phone: '', address: '' })

const rules = {
  supplierName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }]
}

const loadList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      supplierName: searchForm.supplierName || undefined,
      contact: searchForm.contact || undefined
    }
    const res = await getSupplierPageAPI(params)
    if (res.code !== 200) {
      throw new Error(res.msg || '供应商查询失败')
    }
    const pageData = res.data || {}
    tableData.value = (pageData.records || []).map((item) => ({
      ...item,
      contact: item.contact || item.contactPerson || '',
      phone: item.phone || item.contactPhone || ''
    }))
    total.value = pageData.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载供应商失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadList()
}

const resetSearch = () => {
  searchForm.supplierName = ''
  searchForm.contact = ''
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

const initForm = () => {
  form.id = null; form.supplierName = ''; form.contact = ''; form.phone = ''; form.address = ''
}

const handleAdd = () => {
  isView.value = false
  dialogTitle.value = '新增供应商'
  initForm()
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

const openByDetail = async (row, viewMode) => {
  const res = await getSupplierDetailAPI(row.id)
  if (res.code !== 200) {
    throw new Error(res.msg || '供应商详情查询失败')
  }
  const detail = res.data || {}
  isView.value = viewMode
  dialogTitle.value = viewMode ? '供应商详情 (仅查看)' : '编辑供应商'
  Object.assign(form, {
    id: detail.id,
    supplierName: detail.supplierName || '',
    contact: detail.contact || detail.contactPerson || '',
    phone: detail.phone || detail.contactPhone || '',
    address: detail.address || ''
  })
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

const handleView = async (row) => {
  try {
    await openByDetail(row, true)
  } catch (error) {
    ElMessage.error(error.message || '加载供应商详情失败')
  }
}

const handleEdit = async (row) => {
  try {
    await openByDetail(row, false)
  } catch (error) {
    ElMessage.error(error.message || '加载供应商详情失败')
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该供应商信息?', '警告', { type: 'warning' })
    .then(async () => {
      const res = await deleteSupplierAPI(row.id)
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
        supplierName: form.supplierName,
        contactPerson: form.contact,
        contactPhone: form.phone,
        address: form.address,
        status: 1
      }
      const res = form.id ? await updateSupplierAPI(form.id, payload) : await createSupplierAPI(payload)
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