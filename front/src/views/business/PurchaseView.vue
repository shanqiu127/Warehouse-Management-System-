<template>
  <div class="purchase-container">
    <el-card>
      <div class="search-box">
        <div class="top-right-help">
          <span class="help-label">作废/红冲:</span>
          <el-tooltip content="当天单据可直接删除；历史单据的作废/红冲会提交给仓储管理员审批，通过后才执行。" placement="left">
            <el-icon class="void-help-icon"><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="进货单号">
            <el-input v-model="searchForm.keywords" placeholder="请输入进货单号" clearable></el-input>
          </el-form-item>
          <el-form-item label="进货日期">
            <el-date-picker
              v-model="searchForm.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            <el-button v-permission="{ roles: ['admin'], deptCodes: ['purchase'] }" type="success" :icon="Plus" @click="handleAdd">新增进货</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="tableData" border style="width: 100%" v-loading="loading">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="orderNo" label="进货单号" width="150" />
        <el-table-column prop="goodsName" label="商品名称" />
        <el-table-column prop="supplierName" label="供应商" />
        <el-table-column prop="price" label="进货单价(元)" width="120" />
        <el-table-column prop="quantity" label="进货数量" width="100" />
        <el-table-column prop="totalAmount" label="总金额(元)" width="120" />
        <el-table-column prop="purchaseDate" label="进货日期" width="180" />
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <div class="action-group">
              <el-button size="small" type="primary" link :icon="ViewIcon" @click="handleView(scope.row)">查看</el-button>
              <el-button
                v-if="showDeleteAction(scope.row)"
                v-permission="{ roles: ['admin'], deptCodes: ['purchase'] }"
                size="small"
                type="danger"
                link
                @click="handleDelete(scope.row)"
              >
                删除
              </el-button>
              <template v-else-if="showVoidActions(scope.row)">
              <el-button
                v-permission="{ roles: ['admin'], deptCodes: ['purchase'] }"
                size="small"
                type="warning"
                link
                :disabled="!canVoid(scope.row)"
                @click="handleVoid(scope.row, false)"
              >
                仅作废
              </el-button>
              <el-button
                v-permission="{ roles: ['admin'], deptCodes: ['purchase'] }"
                size="small"
                type="danger"
                link
                :disabled="!canVoid(scope.row)"
                @click="handleVoid(scope.row, true)"
              >
                作废并红冲
              </el-button>
              </template>
              <span v-else :class="['action-disabled', stateTextClass(scope.row)]">{{ resolveState(scope.row)?.label || '不可操作' }}</span>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页区域 -->
      <div class="pagination-box" style="margin-top: 20px; display: flex; justify-content: flex-end;">
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
    </el-card>

    <el-dialog :title="dialogType === 'view' ? '查看进货信息' : '新增进货'" v-model="dialogVisible" width="500px">
      <el-form ref="dialogFormRef" :model="dialogForm" :rules="dialogRules" label-width="100px" :disabled="dialogType === 'view'">
        <el-form-item label="商品名称" prop="goodsId">
          <el-select v-model="dialogForm.goodsId" placeholder="请选择商品" style="width: 100%">
            <el-option v-for="item in goodsOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="进货数量" prop="quantity">
          <el-input-number v-model="dialogForm.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="进货单价" prop="price">
          <el-input-number v-model="dialogForm.price" :min="0.01" :precision="2" :step="0.1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="总金额" prop="totalAmount">
          <el-input :value="totalAmountText" disabled>
            <template #append>元</template>
          </el-input>
        </el-form-item>
        <el-form-item label="进货日期" prop="purchaseDate">
          <el-date-picker
            v-model="dialogForm.purchaseDate"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择进货时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="dialogForm.remark" type="textarea" placeholder="请输入备注"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button :icon="Close" @click="dialogVisible = false">取消</el-button>
          <el-button v-if="dialogType !== 'view'" type="primary" :icon="Check" @click="submitForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { QuestionFilled, Search, Refresh, Plus, View as ViewIcon, Delete, DocumentRemove, DocumentDelete, Close, Check } from '@element-plus/icons-vue'
import { createApprovalOrderAPI } from '@/api/system'
import { hasBizDocumentWorkflowState, isBizDocumentDeleted, resolveBizDocumentState } from '@/utils/bizDocumentState'
import {
  createPurchaseAPI,
  deletePurchaseAPI,
  getGoodsOptionsAPI,
  getPurchaseDetailAPI,
  getPurchasePageAPI
} from '@/api/business'

const searchForm = reactive({
  keywords: '',
  dateRange: []
})

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const goodsOptions = ref([])

const tableData = ref([])

const dialogVisible = ref(false)
const dialogType = ref('add')
const dialogFormRef = ref(null)
const dialogForm = reactive({
  goodsId: null,
  quantity: 1,
  price: 0,
  purchaseDate: '',
  remark: ''
})

const totalAmountText = computed(() => {
  const qty = Number(dialogForm.quantity || 0)
  const price = Number(dialogForm.price || 0)
  return (qty * price).toFixed(2)
})

const dialogRules = {
  goodsId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入进货数量', trigger: 'blur' }],
  price: [{ required: true, message: '请输入进货单价', trigger: 'blur' }],
  purchaseDate: [{ required: true, message: '请选择进货日期', trigger: 'change' }]
}

const normalizeDateTime = (val) => {
  if (!val) return ''
  return String(val).replace('T', ' ')
}

const toDateOnly = (val) => {
  if (!val) return ''
  return String(val).slice(0, 10)
}

const localToday = () => {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const resolveBizDate = (row) => {
  return row?.purchaseDate || row?.operationTime || row?.createTime || ''
}

const canDelete = (row) => {
  if (isBizDocumentDeleted(row)) return false
  if (row?.bizStatus !== 1) return false
  return toDateOnly(resolveBizDate(row)) === localToday()
}

const canVoid = (row) => {
  if (isBizDocumentDeleted(row)) return false
  if (row?.bizStatus !== 1) return false
  return toDateOnly(resolveBizDate(row)) !== localToday()
}

const resolveState = (row) => resolveBizDocumentState(row)

const stateTextClass = (row) => {
  const state = resolveState(row)
  if (!state) return ''
  if (state.type === 'success') return 'state-success'
  if (state.type === 'danger') return 'state-danger'
  if (state.type === 'warning') return 'state-warning'
  return ''
}

const showDeleteAction = (row) => !hasBizDocumentWorkflowState(row) && canDelete(row)

const showVoidActions = (row) => !hasBizDocumentWorkflowState(row) && canVoid(row)

const buildOperationTime = (selectedDate) => {
  if (!selectedDate) return undefined
  return String(selectedDate).replace(' ', 'T')
}

const loadGoodsOptions = async () => {
  const res = await getGoodsOptionsAPI()
  if (res.code !== 200) {
    throw new Error(res.msg || '加载商品下拉失败')
  }
  goodsOptions.value = res.data || []
}

const loadList = async () => {
  loading.value = true
  try {
    const hasDateRange = Array.isArray(searchForm.dateRange) && searchForm.dateRange.length === 2
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      purchaseNo: searchForm.keywords || undefined,
      startDate: hasDateRange ? searchForm.dateRange[0] : undefined,
      endDate: hasDateRange ? searchForm.dateRange[1] : undefined
    }
    const res = await getPurchasePageAPI(params)
    if (res.code !== 200) {
      throw new Error(res.msg || '查询失败')
    }
    const pageData = res.data || {}
    tableData.value = (pageData.records || []).map((item) => ({
      ...item,
      purchaseDate: normalizeDateTime(item.purchaseDate || item.operationTime || item.createTime)
    }))
    total.value = pageData.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载进货数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadList()
}

const resetSearch = () => {
  searchForm.keywords = ''
  searchForm.dateRange = []
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
  dialogType.value = 'add'
  dialogFormRef.value?.clearValidate()
  Object.assign(dialogForm, { goodsId: null, quantity: 1, price: 0, purchaseDate: '', remark: '' })
  dialogVisible.value = true
}

const handleView = async (row) => {
  try {
    const res = await getPurchaseDetailAPI(row.id)
    if (res.code !== 200) {
      throw new Error(res.msg || '查询详情失败')
    }
    const detail = res.data || {}
    dialogType.value = 'view'
    Object.assign(dialogForm, {
      goodsId: detail.goodsId ?? null,
      quantity: detail.quantity ?? 1,
      price: detail.price ?? detail.unitPrice ?? 0,
      purchaseDate: normalizeDateTime(detail.purchaseDate || detail.operationTime || detail.createTime),
      remark: detail.remark || ''
    })
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载详情失败')
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该进货记录吗？删除后关联库存将会变更！', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await deletePurchaseAPI(row.id)
    if (res.code !== 200) {
      throw new Error(res.msg || '删除失败')
    }
    ElMessage.success('删除成功')
    row.__uiDeleted = true
    row.isDeleted = 1
  }).catch((error) => {
    if (error?.message) {
      ElMessage.error(error.message)
    }
  })
}

const handleVoid = async (row, createRedFlush) => {
  try {
    const title = createRedFlush ? '作废并红冲' : '作废单据'
    const promptText = createRedFlush ? '请输入红冲原因' : '请输入作废原因'
    const { value } = await ElMessageBox.prompt(promptText, title, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '默认: 手工作废',
      inputValue: ''
    })

    const res = await createApprovalOrderAPI({
      bizType: 'purchase',
      bizId: row.id,
      requestAction: createRedFlush ? 'void_red' : 'void',
      reason: value || ''
    })
    if (res.code !== 200) {
      throw new Error(res.msg || '操作失败')
    }

    ElMessage.success('作废并红冲审批已提交，等待仓储管理员处理')
    await loadList()
  } catch (error) {
    if (error?.message && error.message !== 'cancel') {
      ElMessage.error(error.message)
    }
  }
}

const submitForm = () => {
  dialogFormRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }
    try {
      const payload = {
        goodsId: dialogForm.goodsId,
        quantity: dialogForm.quantity,
        unitPrice: Number(dialogForm.price),
        operationTime: buildOperationTime(dialogForm.purchaseDate),
        remark: dialogForm.remark || ''
      }
      const res = await createPurchaseAPI(payload)
      if (res.code !== 200) {
        throw new Error(res.msg || '新增失败')
      }
      ElMessage.success('新增成功')
      dialogVisible.value = false
      loadList()
    } catch (error) {
      ElMessage.error(error.message || '新增失败')
    }
  })
}

onMounted(async () => {
  try {
    await loadGoodsOptions()
    await loadList()
  } catch (error) {
    ElMessage.error(error.message || '初始化失败')
  }
})
</script>

<style scoped>
.search-box {
  position: relative;
  margin-bottom: 20px;
}

.top-right-help {
  position: absolute;
  right: 0;
  top: -8px;
  color: #909399;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  z-index: 2;
}

.help-label {
  font-size: 12px;
  color: #909399;
}

.action-disabled {
  color: #999;
  font-size: 12px;
}

.state-success {
  color: #16a34a;
}

.state-danger {
  color: #dc2626;
}

.state-warning {
  color: #d97706;
}

.action-group {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.action-group :deep(.el-button + .el-button) {
  margin-left: 0;
}

.void-help-icon {
  color: #909399;
  font-size: 15px;
  cursor: pointer;
}
</style>
