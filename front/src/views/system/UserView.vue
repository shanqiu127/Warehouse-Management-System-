<template>
  <el-card>
    <el-form :inline="true" :model="searchForm">
      <el-form-item label="用户名">
        <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
      </el-form-item>
      <el-form-item v-if="isSuperAdminUser" label="角色">
        <el-select v-model="searchForm.role" placeholder="请选择角色范围" style="width: 180px;" :disabled="!isSuperAdminUser">
          <el-option v-for="item in roleFilterOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="canChooseDept" label="所属部门">
        <el-select v-model="searchForm.deptId" clearable placeholder="全部" style="width: 180px;">
          <el-option v-for="dept in deptOptions" :key="dept.id" :label="dept.name" :value="dept.id" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="!isSuperAdminUser" label="状态">
        <el-select v-model="searchForm.status" clearable placeholder="全部" style="width: 120px;">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
        <el-button type="success" @click="handleAdd">新增用户</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" border style="width: 100%" v-loading="loading">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="realName" label="真实姓名" min-width="120" />
      <el-table-column v-if="showDeptColumn" prop="deptName" label="所属部门" min-width="120" />
      <el-table-column v-if="isSuperAdminUser" prop="role" label="角色" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.role === 'superadmin' ? 'danger' : (scope.row.role === 'admin' ? 'warning' : 'info')">
            {{ roleLabel(scope.row.role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="scope">
          <el-switch
            :model-value="scope.row.status"
            active-text="正常"
            inactive-text="禁用"
            :disabled="!canManageRow(scope.row)"
            @change="(val) => handleStatusChange(scope.row, val)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="150" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" :disabled="!canManageRow(scope.row)" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" :disabled="!canResetPassword(scope.row)" @click="handleResetPassword(scope.row)">设置密码</el-button>
          <el-button size="small" type="danger" :disabled="!canManageRow(scope.row)" @click="handleDelete(scope.row)">删除</el-button>
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

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="460px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="88px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item v-if="isSuperAdminUser" label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%;">
            <el-option v-for="item in formRoleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="canChooseDept" label="所属部门" prop="deptId">
          <el-select v-model="form.deptId" placeholder="请选择所属部门" style="width: 100%;">
            <el-option v-for="dept in deptOptions" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="true">正常</el-radio>
            <el-radio :value="false">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { isSuperAdmin } from '@/utils/auth'
import {
  createUserAPI,
  deleteUserAPI,
  getDeptOptionsAPI,
  getUserDetailAPI,
  getUserPageAPI,
  resetUserPasswordAPI,
  updateUserAPI,
  updateUserStatusAPI
} from '@/api/system'

const userStore = useUserStore()

const isSuperAdminUser = computed(() => isSuperAdmin(userStore.role))
const canChooseDept = computed(() => isSuperAdminUser.value)
const showDeptColumn = computed(() => isSuperAdminUser.value)
const deptOptions = ref([])

const roleFilterOptions = computed(() => (
  isSuperAdminUser.value
    ? [
        { label: '管理员/超级管理员', value: 'management' },
        { label: '普通员工', value: 'employee' },
        { label: '全部', value: 'all' }
      ]
    : [{ label: '普通员工', value: 'employee' }]
))

const formRoleOptions = computed(() => (
  isSuperAdminUser.value
    ? [
        { label: '管理员', value: 'admin' },
        { label: '普通员工', value: 'employee' }
      ]
    : [{ label: '普通员工', value: 'employee' }]
))

const searchForm = reactive({ username: '', role: 'management', deptId: null, status: null })
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const formRef = ref(null)
const form = reactive({ id: null, username: '', realName: '', role: 'employee', deptId: null, status: true, phone: '', email: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  deptId: [{ required: true, message: '请选择所属部门', trigger: 'change' }]
}

const roleLabel = (role) => {
  if (role === 'superadmin') return '超级管理员'
  if (role === 'admin') return '管理员'
  return '普通员工'
}

const resetForm = () => {
  form.id = null
  form.username = ''
  form.realName = ''
  form.role = 'employee'
  form.deptId = canChooseDept.value ? null : userStore.deptId
  form.status = true
  form.phone = ''
  form.email = ''
}

const loadDeptOptions = async () => {
  const res = await getDeptOptionsAPI()
  if (res.code !== 200) {
    throw new Error(res.msg || '部门下拉加载失败')
  }
  deptOptions.value = res.data || []
  if (isSuperAdminUser.value) {
    searchForm.role = 'management'
    searchForm.deptId = null
    return
  }
  searchForm.role = 'employee'
  searchForm.deptId = canChooseDept.value ? null : userStore.deptId
}

const loadList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      username: searchForm.username || undefined,
      role: isSuperAdminUser.value ? (searchForm.role === 'all' ? undefined : (searchForm.role || undefined)) : 'employee',
      deptId: canChooseDept.value ? (searchForm.deptId || undefined) : (userStore.deptId || undefined),
      status: !isSuperAdminUser.value && searchForm.status !== null ? searchForm.status : undefined
    }
    const res = await getUserPageAPI(params)
    if (res.code !== 200) {
      throw new Error(res.msg || '用户查询失败')
    }
    const pageData = res.data || {}
    tableData.value = pageData.records || []
    total.value = pageData.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载用户失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadList()
}

const resetSearch = () => {
  searchForm.username = ''
  searchForm.role = isSuperAdminUser.value ? 'management' : 'employee'
  searchForm.deptId = canChooseDept.value ? null : userStore.deptId
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

const canManageRow = (row) => {
  if (row.role === 'superadmin') return false
  if (isSuperAdminUser.value) {
    return row.role === 'admin' || row.role === 'employee'
  }
  return row.role === 'employee' && row.deptId === userStore.deptId
}

const canResetPassword = (row) => canManageRow(row)

const handleAdd = () => {
  dialogTitle.value = '新增用户'
  resetForm()
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  try {
    const res = await getUserDetailAPI(row.id)
    if (res.code !== 200) {
      throw new Error(res.msg || '用户详情查询失败')
    }
    const detail = res.data || {}
    dialogTitle.value = '编辑用户'
    Object.assign(form, {
      id: detail.id,
      username: detail.username || '',
      realName: detail.realName || '',
      role: detail.role || 'employee',
      deptId: detail.deptId || (canChooseDept.value ? null : userStore.deptId),
      status: !!detail.status,
      phone: detail.phone || '',
      email: detail.email || ''
    })
    formRef.value?.clearValidate()
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载用户详情失败')
  }
}

const handleDelete = (row) => {
  if (!canManageRow(row)) {
    ElMessage.warning('当前账号无权删除该用户')
    return
  }
  ElMessageBox.confirm('确认删除该用户?', '提示', { type: 'warning' })
    .then(async () => {
      const res = await deleteUserAPI(row.id)
      if (res.code !== 200) {
        throw new Error(res.msg || '删除失败')
      }
      ElMessage.success('删除成功')
      await loadList()
    })
    .catch(() => {})
}

const handleStatusChange = async (row, val) => {
  if (!canManageRow(row)) {
    ElMessage.warning('当前账号无权修改该用户状态')
    return
  }
  const oldVal = row.status
  row.status = val
  try {
    const res = await updateUserStatusAPI(row.id, { status: val ? 1 : 0 })
    if (res.code !== 200) {
      throw new Error(res.msg || '状态更新失败')
    }
    ElMessage.success('状态已更新')
  } catch (error) {
    row.status = oldVal
    ElMessage.error(error.message || '状态更新失败')
  }
}

const handleResetPassword = async (row) => {
  if (!canResetPassword(row)) {
    ElMessage.warning('当前账号无权为该用户设置密码')
    return
  }
  try {
    const { value } = await ElMessageBox.prompt(`请输入 ${row.username} 的新密码（至少6位）`, '设置密码', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputType: 'password',
      inputValidator: (inputVal) => {
        if (!inputVal || inputVal.length < 6) return '密码至少 6 位'
        return true
      }
    })
    const res = await resetUserPasswordAPI(row.id, { newPassword: value })
    if (res.code !== 200) {
      throw new Error(res.msg || '设置密码失败')
    }
    ElMessage.success('密码设置成功')
  } catch (error) {
    if (error === 'cancel') return
    ElMessage.error(error.message || '设置密码失败')
  }
}

const handleSave = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    try {
      const payload = {
        username: form.username,
        realName: form.realName,
        role: isSuperAdminUser.value ? form.role : 'employee',
        deptId: canChooseDept.value ? form.deptId : userStore.deptId,
        status: form.status ? 1 : 0,
        phone: form.phone || '',
        email: form.email || ''
      }
      const res = form.id ? await updateUserAPI(form.id, payload) : await createUserAPI(payload)
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
    resetForm()
    await loadList()
  } catch (error) {
    ElMessage.error(error.message || '初始化失败')
  }
})
</script>