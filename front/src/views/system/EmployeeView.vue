<template>
  <el-card>
    <el-form :inline="true" :model="searchForm">
      <el-form-item label="员工姓名">
        <el-input v-model="searchForm.empName" placeholder="请输入员工姓名" clearable />
      </el-form-item>
      <el-form-item label="所属部门">
        <el-select v-model="searchForm.deptId" placeholder="请选择部门" clearable style="width: 150px;">
          <el-option v-for="dept in depts" :key="dept.id" :label="dept.name" :value="dept.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        <el-button type="success" :icon="Plus" @click="handleAdd">新增员工</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" border style="width: 100%" v-loading="loading">
      <el-table-column prop="empCode" label="员工工号" width="100" />
      <el-table-column prop="username" label="登录账号" width="120" />
      <el-table-column prop="empName" label="员工姓名" />
      <el-table-column prop="position" label="职位" width="100" />
      <el-table-column prop="phone" label="联系电话" />
      <el-table-column prop="deptName" label="所属部门" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{ scope.row.status === 1 ? '在职' : '离职' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="scope">
          <template v-if="isReadOnlyRow(scope.row)">
            <el-tag type="info">只读展示</el-tag>
          </template>
          <template v-else>
            <el-button size="small" type="primary" :icon="Edit" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" :icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
          </template>
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

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="450px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username"></el-input>
        </el-form-item>
        <el-form-item label="员工姓名" prop="empName">
          <el-input v-model="form.empName"></el-input>
        </el-form-item>
        <el-form-item label="职位">
          <el-input v-model="form.position"></el-input>
        </el-form-item>
        <el-form-item label="所属部门" prop="deptId">
          <el-select v-model="form.deptId" placeholder="请选择分配部门" style="width: 100%;">
            <el-option v-for="dept in editableDeptOptions" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone"></el-input>
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email"></el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">在职</el-radio>
            <el-radio :value="0">离职</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码">
          <span class="form-tip">默认密码为 123456，创建后可在用户管理中重置。</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :icon="Close" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :icon="Check" @click="handleSave">确认</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Close, Check } from '@element-plus/icons-vue'
import {
  createEmployeeAPI,
  deleteEmployeeAPI,
  getDeptOptionsAPI,
  getEmployeeDetailAPI,
  getEmployeePageAPI,
  updateEmployeeAPI
} from '@/api/system'

const SYSTEM_MANAGEMENT_DEPT_NAME = '系统管理部'

const depts = ref([])
const editableDeptOptions = computed(() => depts.value.filter((dept) => dept.name !== SYSTEM_MANAGEMENT_DEPT_NAME))

const searchForm = reactive({ empName: '', deptId: null })
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('新增员工')
const formRef = ref(null)
const form = reactive({ id: null, username: '', empName: '', deptId: null, position: '', phone: '', email: '', status: 1 })

const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  empName: [{ required: true, message: '请输入员工姓名', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择所属部门', trigger: 'change' }]
}

const isReadOnlyRow = (row) => row?.readOnly === true

const loadDeptOptions = async () => {
  const res = await getDeptOptionsAPI()
  if (res.code !== 200) {
    throw new Error(res.msg || '加载部门下拉失败')
  }
  depts.value = res.data || []
}

const loadList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      empName: searchForm.empName || undefined,
      deptId: searchForm.deptId || undefined
    }
    const res = await getEmployeePageAPI(params)
    if (res.code !== 200) {
      throw new Error(res.msg || '员工查询失败')
    }
    const pageData = res.data || {}
    tableData.value = pageData.records || []
    total.value = pageData.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载员工失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadList()
}

const resetSearch = () => {
  searchForm.empName = ''
  searchForm.deptId = null
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
  dialogTitle.value = '新增员工'
  form.id = null
  form.username = ''
  form.empName = ''
  form.deptId = null
  form.position = ''
  form.phone = ''
  form.email = ''
  form.status = 1
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  try {
    const res = await getEmployeeDetailAPI(row.id)
    if (res.code !== 200) {
      throw new Error(res.msg || '员工详情查询失败')
    }
    const detail = res.data || {}
    dialogTitle.value = '编辑员工'
    Object.assign(form, {
      id: detail.id,
      username: detail.username || '',
      empName: detail.empName || '',
      deptId: detail.deptId || null,
      position: detail.position || '',
      phone: detail.phone || '',
      email: detail.email || '',
      status: detail.status ?? 1
    })
    formRef.value?.clearValidate()
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载员工详情失败')
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该员工档案?', '提示', { type: 'warning' })
    .then(async () => {
      const res = await deleteEmployeeAPI(row.id)
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
        username: form.username,
        empName: form.empName,
        deptId: form.deptId,
        position: form.position || '',
        phone: form.phone || '',
        email: form.email || '',
        status: form.status
      }
      const res = form.id ? await updateEmployeeAPI(form.id, payload) : await createEmployeeAPI(payload)
      if (res.code !== 200) {
        throw new Error(res.msg || '保存失败')
      }
      ElMessage.success(form.id ? '修改成功' : '新增成功，初始密码为 123456')
      dialogVisible.value = false
      await loadList()
    } catch (error) {
      ElMessage.error(error.message || '保存失败')
    }
  })
}

onMounted(async () => {
  try {
    await loadDeptOptions()
    await loadList()
  } catch (error) {
    ElMessage.error(error.message || '初始化失败')
  }
})
</script>

<style scoped>
.form-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}
</style>