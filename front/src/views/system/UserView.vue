<template>
  <el-card>
    <el-form :inline="true" :model="searchForm">
      <el-form-item label="用户名">
        <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="searchForm.role" clearable placeholder="全部" style="width: 120px;">
          <el-option label="超级管理员" value="superadmin" />
          <el-option label="管理员" value="admin" />
          <el-option label="普通用户" value="employee" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
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
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="realName" label="真实姓名" />
      <el-table-column prop="role" label="角色">
        <template #default="scope">
          <el-tag :type="scope.row.role === 'superadmin' ? 'danger' : (scope.row.role === 'admin' ? 'warning' : 'info')">
            {{ scope.row.role === 'superadmin' ? '超级管理员' : (scope.row.role === 'admin' ? '管理员' : '普通用户') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态">
        <template #default="scope">
          <el-switch
            :model-value="scope.row.status"
            active-text="正常"
            inactive-text="禁用"
            :disabled="scope.row.role === 'superadmin'"
            @change="(val) => handleStatusChange(scope.row, val)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="150" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" :disabled="scope.row.role === 'superadmin'" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" @click="handleResetPassword(scope.row)" :disabled="!canResetPassword(scope.row)">设置密码</el-button>
          <el-button size="small" type="danger" :disabled="scope.row.role === 'superadmin'" @click="handleDelete(scope.row)">删除</el-button>
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
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username"></el-input>
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName"></el-input>
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%;">
            <el-option label="管理员" value="admin" />
            <el-option label="普通用户" value="employee" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="true">正常</el-radio>
            <el-radio :value="false">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone"></el-input>
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email"></el-input>
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
import { useUserStore } from '@/stores/user'
import {
  createUserAPI,
  deleteUserAPI,
  getUserDetailAPI,
  getUserPageAPI,
  resetUserPasswordAPI,
  updateUserAPI,
  updateUserStatusAPI
} from '@/api/system'

const userStore = useUserStore()

const searchForm = reactive({ username: '', role: '', status: null })
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const formRef = ref(null)
const form = reactive({ id: null, username: '', realName: '', role: 'employee', status: true, phone: '', email: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const loadList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      username: searchForm.username || undefined,
      role: searchForm.role || undefined,
      status: searchForm.status === null ? undefined : searchForm.status
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
  searchForm.role = ''
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

const handleAdd = () => {
  dialogTitle.value = '新增用户'
  form.id = null
  form.username = ''
  form.realName = ''
  form.role = 'employee'
  form.status = true
  form.phone = ''
  form.email = ''
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
  if (row.role === 'superadmin') {
    ElMessage.warning('超级管理员状态不允许修改')
    row.status = true
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

const canResetPassword = (row) => {
  if (row.role === 'superadmin') return false
  if (userStore.role === 'superadmin') {
    return row.role === 'admin' || row.role === 'employee'
  }
  return userStore.role === 'admin' && row.role === 'employee'
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
      const isCreate = !form.id
      const payload = {
        username: form.username,
        realName: form.realName,
        role: form.role,
        status: form.status ? 1 : 0,
        phone: form.phone || '',
        email: form.email || ''
      }
      const res = form.id ? await updateUserAPI(form.id, payload) : await createUserAPI(payload)
      if (res.code !== 200) {
        throw new Error(res.msg || '保存失败')
      }

      if (!isCreate) {
        ElMessage.success('修改成功')
        dialogVisible.value = false
        await loadList()
        return
      }

      ElMessage.success('新增成功')
      dialogVisible.value = false
      await loadList()

      const createdRole = payload.role
      if (createdRole === 'superadmin') {
        ElMessage.warning('已新增用户，请及时设置密码')
        return
      }

      if (!canResetPassword({ role: createdRole })) {
        ElMessage.warning('已新增用户，请联系超级管理员设置密码')
        return
      }

      try {
        await ElMessageBox.confirm('新增成功，建议立即设置初始密码，是否现在设置？', '提醒', {
          type: 'warning',
          confirmButtonText: '立即设置',
          cancelButtonText: '稍后设置'
        })

        const queryRes = await getUserPageAPI({
          pageNum: 1,
          pageSize: 50,
          username: payload.username
        })
        const matched = (queryRes.data?.records || []).find(item => item.username === payload.username)
        if (!matched?.id) {
          ElMessage.warning('未定位到新用户，请在列表中点击“设置密码”')
          return
        }
        await handleResetPassword({ id: matched.id, username: matched.username, role: matched.role })
      } catch (error) {
        if (error === 'cancel') {
          ElMessage.info('已保留默认密码，请尽快设置密码')
          return
        }
        ElMessage.error(error.message || '提醒设置密码失败，请在列表中手动设置')
      }
    } catch (error) {
      ElMessage.error(error.message || '保存失败')
    }
  })
}

onMounted(() => {
  loadList()
})
</script>