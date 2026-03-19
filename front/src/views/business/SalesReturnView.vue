<template>
  <div class="sales-return-container">
    <el-card>
      <div class="search-box">
        <div class="top-right-help">
          <span class="help-label">作废红冲:</span>
          <el-tooltip content="作废红冲：保留原单并生成反向红字记录，便于审计追溯。" placement="left">
            <el-icon class="void-help-icon"><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="退货单号">
            <el-input v-model="searchForm.keywords" placeholder="请输入销售退货单号或商品名" clearable></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
            <el-button v-permission="['admin']" type="warning" @click="handleAdd">新建销售退货单</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="tableData" border style="width: 100%" v-loading="loading">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="returnNo" label="销售退货单号" width="150" />
        <el-table-column prop="goodsName" label="退回商品" />
        <el-table-column prop="reason" label="退货原因" show-overflow-tooltip />
        <el-table-column prop="quantity" label="退货数量" width="100" />
        <el-table-column prop="refundAmount" label="退货金额(元)" width="120" />
        <el-table-column prop="returnDate" label="退货日期" width="180" />
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="scope">
            <div class="action-group">
              <el-button size="small" type="primary" link @click="handleView(scope.row)">详情</el-button>
              <template v-if="canDelete(scope.row)">
                <el-button v-permission="['admin']" size="small" type="danger" link @click="handleDelete(scope.row)">删除</el-button>
              </template>
              <template v-else-if="canRedFlush(scope.row)">
                <el-button v-permission="['admin']" size="small" type="danger" link @click="handleVoid(scope.row, true)">作废红冲</el-button>
              </template>
              <span v-else class="action-disabled">{{ actionDisabledText(scope.row) }}</span>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-box" style="margin-top: 20px; display: flex; justify-content: flex-end;">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog :title="dialogType === 'view' ? '销售退货详情' : '新增销售退货单'" v-model="dialogVisible" width="500px">
      <el-form ref="dialogFormRef" :model="dialogForm" :rules="dialogRules" label-width="100px" :disabled="dialogType === 'view'">
        <el-form-item label="退回商品" prop="goodsId">
          <el-select v-model="dialogForm.goodsId" placeholder="请选择商品" style="width: 100%">
            <el-option v-for="item in goodsOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="退回数量" prop="quantity">
          <el-input-number v-model="dialogForm.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="退货单价" prop="unitPrice">
          <el-input-number v-model="dialogForm.unitPrice" :min="0.01" :precision="2" :step="0.1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="退货金额" prop="refundAmount">
          <el-input :value="refundAmountText" disabled>
            <template #append>元</template>
          </el-input>
        </el-form-item>
        <el-form-item label="退货日期" prop="returnDate">
          <el-date-picker
            v-model="dialogForm.returnDate"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择退货时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="退货原因" prop="reason">
          <el-input v-model="dialogForm.reason" type="textarea" placeholder="填写退换货原因"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button v-if="dialogType !== 'view'" type="primary" @click="submitForm">确定新增</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { QuestionFilled } from '@element-plus/icons-vue'
import {
  createSalesReturnAPI,
  deleteSalesReturnAPI,
  getGoodsOptionsAPI,
  getSalesReturnDetailAPI,
  getSalesReturnPageAPI,
  voidSalesReturnAPI
} from '@/api/business'

const searchForm = reactive({ keywords: '' })
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const goodsOptions = ref([])

const tableData = ref([])

const dialogVisible = ref(false)
const dialogType = ref('add')
const dialogFormRef = ref(null)
const dialogForm = reactive({ goodsId: null, quantity: 1, unitPrice: 0, returnDate: '', reason: '' })

const refundAmountText = computed(() => {
  const qty = Number(dialogForm.quantity || 0)
  const price = Number(dialogForm.unitPrice || 0)
  return (qty * price).toFixed(2)
})

const dialogRules = {
  goodsId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  unitPrice: [{ required: true, message: '请输入退货单价', trigger: 'blur' }],
  returnDate: [{ required: true, message: '请选择退货日期', trigger: 'change' }]
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

const canDelete = (row) => {
  if (row?.__uiDeleted) return false
  if (row?.bizStatus !== 1) return false
  return toDateOnly(row?.returnDate) === localToday()
}

const canRedFlush = (row) => {
  if (row?.__uiDeleted) return false
  if (row?.bizStatus !== 1) return false
  return toDateOnly(row?.returnDate) !== localToday()
}

const actionDisabledText = (row) => {
  if (row?.__uiDeleted) return '已删除'
  if (row?.bizStatus === 2) return '已作废'
  if (row?.bizStatus === 3) return '已红冲'
  if (row?.bizStatus === 1) return '已删除'
  return '不可操作'
}

const buildOperationTime = (selectedDate) => {
  if (!selectedDate) return undefined
  return String(selectedDate).replace(' ', 'T')
}

const chooseFrontendRecordBehavior = async () => {
  try {
    await ElMessageBox.confirm('操作已完成，是否保留当前列表中的前端记录？', '前端记录处理', {
      type: 'info',
      confirmButtonText: '保留',
      cancelButtonText: '移除',
      distinguishCancelAndClose: true
    })
    return 'keep'
  } catch (action) {
    if (action === 'cancel') return 'remove'
    return 'keep'
  }
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
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      returnNo: searchForm.keywords || undefined,
      goodsName: searchForm.keywords || undefined
    }
    const res = await getSalesReturnPageAPI(params)
    if (res.code !== 200) {
      throw new Error(res.msg || '查询失败')
    }
    const pageData = res.data || {}
    tableData.value = (pageData.records || []).map((item) => ({
      ...item,
      returnDate: normalizeDateTime(item.returnDate)
    }))
    total.value = pageData.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载销售退货数据失败')
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
  Object.assign(dialogForm, { goodsId: null, quantity: 1, unitPrice: 0, returnDate: '', reason: '' })
  dialogVisible.value = true
}

const handleView = async (row) => {
  try {
    const res = await getSalesReturnDetailAPI(row.id)
    if (res.code !== 200) {
      throw new Error(res.msg || '查询详情失败')
    }
    const detail = res.data || {}
    dialogType.value = 'view'
    Object.assign(dialogForm, {
      goodsId: detail.goodsId ?? null,
      quantity: detail.quantity ?? 1,
      unitPrice: detail.unitPrice ?? (detail.refundAmount && detail.quantity ? Number(detail.refundAmount) / Number(detail.quantity) : 0),
      returnDate: normalizeDateTime(detail.returnDate),
      reason: detail.reason || detail.remark || ''
    })
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载详情失败')
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('删除此退货记录？', '确认', { type: 'warning' }).then(async () => {
    const res = await deleteSalesReturnAPI(row.id)
    if (res.code !== 200) {
      throw new Error(res.msg || '删除失败')
    }
    ElMessage.success('删除成功')
    const behavior = await chooseFrontendRecordBehavior()
    if (behavior === 'remove') {
      tableData.value = tableData.value.filter((item) => item.id !== row.id)
      return
    }
    row.__uiDeleted = true
  }).catch((error) => {
    if (error?.message) {
      ElMessage.error(error.message)
    }
  })
}

const handleVoid = async (row, createRedFlush) => {
  try {
    const title = createRedFlush ? '作废并红冲' : '作废单据'
    const promptText = createRedFlush ? '请输入红冲原因（可选）' : '请输入作废原因（可选）'
    const { value } = await ElMessageBox.prompt(promptText, title, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '默认: 手工作废',
      inputValue: ''
    })

    const res = await voidSalesReturnAPI(row.id, {
      reason: value || '',
      createRedFlush
    })
    if (res.code !== 200) {
      throw new Error(res.msg || '操作失败')
    }
    ElMessage.success(createRedFlush ? '已完成作废红冲' : '作废成功')
    const behavior = await chooseFrontendRecordBehavior()
    if (behavior === 'remove') {
      tableData.value = tableData.value.filter((item) => item.id !== row.id)
      return
    }
    row.bizStatus = createRedFlush ? 3 : 2
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
        unitPrice: Number(dialogForm.unitPrice),
        operationTime: buildOperationTime(dialogForm.returnDate),
        remark: dialogForm.reason || ''
      }
      const res = await createSalesReturnAPI(payload)
      if (res.code !== 200) {
        throw new Error(res.msg || '新增失败')
      }
      ElMessage.success('销售退货新增成功')
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

.action-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.void-help-icon {
  color: #909399;
  font-size: 15px;
  cursor: pointer;
}
</style>
