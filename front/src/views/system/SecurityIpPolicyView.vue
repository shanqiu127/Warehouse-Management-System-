<template>
  <div class="policy-page">
    <div class="paper-layer" aria-hidden="true">
      <span class="paper-grain"></span>
      <span class="paper-line"></span>
    </div>

    <el-card class="panel" shadow="never">
      <template #header>
        <div class="card-head">
          <div>
            <p class="kicker">SECURITY POLICY</p>
            <h2>IP 白名单策略</h2>
          </div>
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增策略</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="filter-row">
        <el-form-item label="策略名">
          <el-input v-model="searchForm.policyName" placeholder="输入策略名" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="停用" />
          </el-select>
        </el-form-item>
        <el-form-item label="策略类型">
          <el-select v-model="searchForm.allowFlag" placeholder="全部" clearable style="width: 140px">
            <el-option :value="1" label="放行" />
            <el-option :value="0" label="拒绝" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe border empty-text="暂无策略数据">
        <el-table-column prop="policyName" label="策略名称" min-width="180" />
        <el-table-column prop="ipCidr" label="IP/CIDR" min-width="180" />
        <el-table-column prop="allowFlag" label="类型" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.allowFlag === 1 ? 'success' : 'danger'" effect="light">
              {{ scope.row.allowFlag === 1 ? '放行' : '拒绝' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="100" align="center" />
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="scope">
            <el-switch
              :model-value="scope.row.status === 1"
              @change="(val) => handleStatusChange(scope.row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="更新时间" width="165">
          <template #default="scope">{{ formatTime(scope.row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button text type="primary" :icon="Edit" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button text type="danger" :icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'add' ? '新增策略' : '编辑策略'" width="560px">
      <el-form ref="formRef" :model="formModel" :rules="rules" label-width="98px">
        <el-form-item label="策略名称" prop="policyName">
          <el-input v-model="formModel.policyName" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="IP/CIDR" prop="ipCidr">
          <el-input v-model="formModel.ipCidr" placeholder="例：192.168.1.10 或 192.168.1.0/24" />
        </el-form-item>
        <el-form-item label="策略类型" prop="allowFlag">
          <el-radio-group v-model="formModel.allowFlag">
            <el-radio :value="1">放行</el-radio>
            <el-radio :value="0">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formModel.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="formModel.priority" :min="1" :max="999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="formModel.remark" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :icon="Close" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :icon="Check" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Close, Check } from '@element-plus/icons-vue'
import {
  createIpPolicyAPI,
  deleteIpPolicyAPI,
  getIpPolicyDetailAPI,
  getIpPolicyPageAPI,
  updateIpPolicyAPI,
  updateIpPolicyStatusAPI
} from '@/api/security'

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const dialogVisible = ref(false)
const dialogMode = ref('add')
const formRef = ref(null)

const searchForm = reactive({
  policyName: '',
  status: undefined,
  allowFlag: undefined
})

const formModel = reactive({
  id: null,
  policyName: '',
  ipCidr: '',
  allowFlag: 1,
  status: 1,
  priority: 100,
  remark: ''
})

const rules = {
  policyName: [{ required: true, message: '请输入策略名称', trigger: 'blur' }],
  ipCidr: [{ required: true, message: '请输入 IP/CIDR', trigger: 'blur' }],
  allowFlag: [{ required: true, message: '请选择策略类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const formatTime = (val) => {
  if (!val) return '-'
  return String(val).replace('T', ' ')
}

const resetFormModel = () => {
  formModel.id = null
  formModel.policyName = ''
  formModel.ipCidr = ''
  formModel.allowFlag = 1
  formModel.status = 1
  formModel.priority = 100
  formModel.remark = ''
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await getIpPolicyPageAPI({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      policyName: searchForm.policyName || undefined,
      status: searchForm.status,
      allowFlag: searchForm.allowFlag
    })
    if (res.code !== 200) throw new Error(res.msg || '加载策略失败')
    tableData.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '加载策略失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadList()
}

const handleReset = () => {
  searchForm.policyName = ''
  searchForm.status = undefined
  searchForm.allowFlag = undefined
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
  dialogMode.value = 'add'
  resetFormModel()
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  try {
    const res = await getIpPolicyDetailAPI(row.id)
    if (res.code !== 200) throw new Error(res.msg || '加载策略详情失败')
    const data = res.data || {}
    dialogMode.value = 'edit'
    formModel.id = data.id
    formModel.policyName = data.policyName || ''
    formModel.ipCidr = data.ipCidr || ''
    formModel.allowFlag = Number(data.allowFlag ?? 1)
    formModel.status = Number(data.status ?? 1)
    formModel.priority = Number(data.priority ?? 100)
    formModel.remark = data.remark || ''
    formRef.value?.clearValidate()
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载策略详情失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除策略“${row.policyName}”吗？`, '删除确认', { type: 'warning' })
    const res = await deleteIpPolicyAPI(row.id)
    if (res.code !== 200) throw new Error(res.msg || '删除失败')
    ElMessage.success('删除成功')
    loadList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close' && error?.message) {
      ElMessage.error(error.message)
    }
  }
}

const handleStatusChange = async (row, enabled) => {
  const originalStatus = row.status
  try {
    const res = await updateIpPolicyStatusAPI(row.id, Boolean(enabled))
    if (res.code !== 200) throw new Error(res.msg || '状态更新失败')
    row.status = enabled ? 1 : 0
    ElMessage.success('状态更新成功')
  } catch (error) {
    row.status = originalStatus
    ElMessage.error(error.message || '状态更新失败')
    loadList()
  }
}

const submitForm = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = {
        policyName: formModel.policyName,
        ipCidr: formModel.ipCidr,
        allowFlag: Number(formModel.allowFlag),
        status: Number(formModel.status),
        priority: Number(formModel.priority || 100),
        remark: formModel.remark || ''
      }
      const res = dialogMode.value === 'add'
        ? await createIpPolicyAPI(payload)
        : await updateIpPolicyAPI(formModel.id, payload)

      if (res.code !== 200) throw new Error(res.msg || '保存失败')
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadList()
    } catch (error) {
      ElMessage.error(error.message || '保存失败')
    } finally {
      saving.value = false
    }
  })
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Cinzel:wght@500;700&family=Manrope:wght@400;500;700&display=swap');


.policy-page {
  --paper: #f4efe6;
  --paper-soft: #f8f3eb;
  --ink-soft: #6d675f;

  position: relative;
  min-height: calc(100vh - 120px);
  padding: 20px;
  border: 1px solid rgba(51, 44, 35, 0.16);
  border-radius: 8px;
  overflow: hidden;
  font-family: 'Manrope', 'Segoe UI', sans-serif;
  background:
    radial-gradient(circle at 8% 10%, rgba(198, 176, 147, 0.16), transparent 36%),
    radial-gradient(circle at 90% 6%, rgba(160, 183, 201, 0.14), transparent 30%),
    linear-gradient(135deg, var(--paper) 0%, var(--paper-soft) 100%);
}

.paper-layer {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.paper-grain {
  position: absolute;
  inset: 0;
  opacity: 0.22;
  background-image: radial-gradient(rgba(0, 0, 0, 0.11) 0.45px, transparent 0.45px);
  background-size: 4px 4px;
}

.paper-line {
  position: absolute;
  inset: 12px;
  border: 1px solid rgba(51, 44, 35, 0.1);
}

.panel {
  position: relative;
  z-index: 1;
  border: 1px solid rgba(44, 37, 31, 0.15);
  background: rgba(255, 255, 255, 0.64);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kicker {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.3em;
  color: #2e4e66;
}

.card-head h2 {
  margin: 4px 0 0;
  font-family: 'Cinzel', 'Times New Roman', serif;
}

.filter-row {
  margin-bottom: 8px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .policy-page {
    padding: 12px;
  }

  .card-head {
    gap: 10px;
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>


