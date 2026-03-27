<template>
  <div class="purchase-return-container">
    <el-card>
      <div class="search-box">
        <div class="top-right-help">
          <span class="help-label">作废/红冲:</span>
          <el-tooltip content="仅作废：撤销业务并回滚库存；作废并红冲：额外生成红冲单用于审计追溯。" placement="left">
            <el-icon class="void-help-icon"><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="退货单号">
            <el-input v-model="searchForm.keywords" placeholder="请输入退货单号" clearable></el-input>
          </el-form-item>
          <el-form-item label="退货日期">
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
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
            <el-button v-permission="['admin', 'employee']" type="warning" @click="handleAdd">新建退货单</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="tableData" border style="width: 100%" v-loading="loading">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="returnNo" label="退货单号" width="150" />
        <el-table-column prop="orderNo" label="原进货单" width="150" />
        <el-table-column prop="goodsName" label="退货商品" />
        <el-table-column prop="supplierName" label="退货至供应商" />
        <el-table-column prop="returnQuantity" label="退货数量" width="100" />
        <el-table-column prop="returnAmount" label="退货金额(元)" width="120" />
        <el-table-column prop="returnDate" label="退货日期" width="180" />
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="reason" label="退货原因" show-overflow-tooltip />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="scope">
            <div class="action-group">
              <el-button size="small" type="primary" link @click="handleView(scope.row)">查看</el-button>
              <el-button
                v-if="scope.row?.__uiDeleted"
                v-permission="['admin', 'employee']"
                size="small"
                type="danger"
                link
                disabled
              >
                删除
              </el-button>
              <el-button
                v-else-if="canDelete(scope.row)"
                v-permission="['admin', 'employee']"
                size="small"
                type="danger"
                link
                @click="handleDelete(scope.row)"
              >
                删除
              </el-button>
              <template v-else>
              <el-button
                v-permission="['admin', 'employee']"
                size="small"
                type="warning"
                link
                :disabled="!canVoid(scope.row)"
                @click="handleVoid(scope.row, false)"
              >
                仅作废
              </el-button>
              <el-button
                v-permission="['admin', 'employee']"
                size="small"
                type="danger"
                link
                :disabled="!canVoid(scope.row)"
                @click="handleVoid(scope.row, true)"
              >
                作废并红冲
              </el-button>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>

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

    <el-dialog :title="dialogType === 'view' ? '查看退货信息' : '发起退货'" v-model="dialogVisible" width="500px">
      <el-form ref="dialogFormRef" :model="dialogForm" :rules="dialogRules" label-width="100px" :disabled="dialogType === 'view'">
        <el-form-item label="来源进货单" prop="sourcePurchaseId">
          <el-select
            v-model="dialogForm.sourcePurchaseId"
            placeholder="请选择来源进货单"
            style="width: 100%"
            filterable
            @change="handleSourcePurchaseChange"
          >
            <el-option
              v-for="item in sourcePurchaseOptions"
              :key="item.id"
              :label="`${item.purchaseNo} | ${item.goodsName} | 可退:${item.returnableQuantity}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="退货商品">
          <el-input :value="selectedSourcePurchase?.goodsName || '-'" disabled />
        </el-form-item>
        <el-form-item label="可退数量">
          <el-input :value="String(selectedSourcePurchase?.returnableQuantity ?? '-')" disabled />
        </el-form-item>
        <el-form-item label="退货数量" prop="returnQuantity">
          <el-input-number v-model="dialogForm.returnQuantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="退货单价" prop="price">
          <el-input-number v-model="dialogForm.price" :min="0.01" :precision="2" :step="0.1" style="width: 100%" />
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
          <el-input v-model="dialogForm.reason" type="textarea" placeholder="请输入备注说明"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button v-if="dialogType !== 'view'" type="primary" @click="submitForm">确定发起</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { QuestionFilled } from '@element-plus/icons-vue'
import {
  createPurchaseReturnAPI,
  deletePurchaseReturnAPI,
  getPurchaseReturnDetailAPI,
  getPurchaseReturnPageAPI,
  getReturnablePurchaseOptionsAPI,
  voidPurchaseReturnAPI
} from '@/api/business'

const searchForm = reactive({ keywords: '', dateRange: [] })
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const sourcePurchaseOptions = ref([])
const selectedSourcePurchase = ref(null)

const tableData = ref([])

const dialogVisible = ref(false)
const dialogType = ref('add')
const dialogFormRef = ref(null)
const dialogForm = reactive({ sourcePurchaseId: null, returnQuantity: 1, price: 0, returnDate: '', reason: '' })

const dialogRules = {
  sourcePurchaseId: [{ required: true, message: '请选择来源进货单', trigger: 'change' }],
  returnQuantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  price: [{ required: true, message: '请输入退货单价', trigger: 'blur' }],
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

const resolveBizDate = (row) => {
  return row?.returnDate || row?.operationTime || row?.createTime || ''
}
// 允许删除
const canDelete = (row) => {
  if (row?.__uiDeleted) return false
  if (row?.bizStatus !== 1) return false
  return toDateOnly(resolveBizDate(row)) === localToday()
}
// 允许作废
const canVoid = (row) => {
  if (row?.__uiDeleted) return false
  if (row?.bizStatus !== 1) return false
  return toDateOnly(resolveBizDate(row)) !== localToday()
}

const actionDisabledText = (row) => {
  if (row?.__uiDeleted) return '已删除'
  if (row?.bizStatus === 2) return '已作废'
  if (row?.bizStatus === 3) return '已红冲'
  if (row?.bizStatus === 1) return '已删除'
  return '不可操作'
}
// 构建后端所需的操作时间格式
const buildOperationTime = (selectedDate) => {
  if (!selectedDate) return undefined
  return String(selectedDate).replace(' ', 'T')
}
// 前端记录是否保留
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

const loadSourcePurchaseOptions = async () => {
  const res = await getReturnablePurchaseOptionsAPI()
  if (res.code !== 200) {
    throw new Error(res.msg || '加载来源进货单失败')
  }
  sourcePurchaseOptions.value = res.data || []
}

const handleSourcePurchaseChange = (sourcePurchaseId) => {
  selectedSourcePurchase.value = sourcePurchaseOptions.value.find((item) => item.id === sourcePurchaseId) || null
  if (dialogType.value === 'add' && selectedSourcePurchase.value) {
    dialogForm.price = Number(selectedSourcePurchase.value.unitPrice || 0)
  }
}

const loadList = async () => {
  loading.value = true
  try {
    const hasDateRange = Array.isArray(searchForm.dateRange) && searchForm.dateRange.length === 2
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      returnNo: searchForm.keywords || undefined,
      startDate: hasDateRange ? searchForm.dateRange[0] : undefined,
      endDate: hasDateRange ? searchForm.dateRange[1] : undefined
    }
    const res = await getPurchaseReturnPageAPI(params)
    if (res.code !== 200) {
      throw new Error(res.msg || '查询失败')
    }
    const pageData = res.data || {}
    tableData.value = (pageData.records || []).map((item) => ({
      ...item,
      returnDate: normalizeDateTime(item.returnDate || item.operationTime || item.createTime)
    }))
    total.value = pageData.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载退货数据失败')
  } finally {
    loading.value = false
  }
}
// 搜索
const handleSearch = () => {
  currentPage.value = 1
  loadList()
}
// 重置搜索
const resetSearch = () => {
  searchForm.keywords = ''
  searchForm.dateRange = []
  currentPage.value = 1
  loadList()
}
// 分页大小改变
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  loadList()
}
// 当前页改变
const handleCurrentChange = (val) => {
  currentPage.value = val
  loadList()
}
// 新增退货单
const handleAdd = () => {
  dialogType.value = 'add'
  dialogFormRef.value?.clearValidate()
  selectedSourcePurchase.value = null
  // 重置表单数据
  Object.assign(dialogForm, { sourcePurchaseId: null, returnQuantity: 1, price: 0, returnDate: '', reason: '' })
  dialogVisible.value = true
}
// 查看退货单详情
const handleView = async (row) => {
  try {
    const res = await getPurchaseReturnDetailAPI(row.id)
    if (res.code !== 200) {
      throw new Error(res.msg || '查询详情失败')
    }
    const detail = res.data || {}
    dialogType.value = 'view'
    selectedSourcePurchase.value = {
      id: detail.sourcePurchaseId,
      purchaseNo: detail.sourcePurchaseNo,
      goodsName: detail.goodsName,
      returnableQuantity: detail.returnQuantity
    }
    Object.assign(dialogForm, {
      sourcePurchaseId: detail.sourcePurchaseId ?? null,
      returnQuantity: detail.returnQuantity ?? detail.quantity ?? 1,
      price: detail.unitPrice ?? (detail.returnAmount && detail.quantity ? Number(detail.returnAmount) / Number(detail.quantity) : 0),
      returnDate: normalizeDateTime(detail.returnDate || detail.operationTime || detail.createTime),
      reason: detail.reason || detail.remark || ''
    })
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载详情失败')
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('撤销此退货单，相应库存将会扣回，继续吗？', '确认', { type: 'warning' }).then(async () => {
    const res = await deletePurchaseReturnAPI(row.id)
    if (res.code !== 200) {
      throw new Error(res.msg || '删除失败')
    }
    ElMessage.success('删除成功')
    const behavior = await chooseFrontendRecordBehavior()
    // 用户选择移除前端记录，则直接从列表中删除，否则标记为已删除
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
// 作废单据（红冲）
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

    const res = await voidPurchaseReturnAPI(row.id, {
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
      if (selectedSourcePurchase.value && dialogForm.returnQuantity > selectedSourcePurchase.value.returnableQuantity) {
        throw new Error(`退货数量超出可退数量，最多可退 ${selectedSourcePurchase.value.returnableQuantity}`)
      }
      const payload = {
        sourcePurchaseId: dialogForm.sourcePurchaseId,
        quantity: dialogForm.returnQuantity,
        unitPrice: Number(dialogForm.price),
        operationTime: buildOperationTime(dialogForm.returnDate),
        remark: dialogForm.reason || ''
      }
      const res = await createPurchaseReturnAPI(payload)
      if (res.code !== 200) {
        throw new Error(res.msg || '新增失败')
      }
      ElMessage.success('退货开单成功')
      dialogVisible.value = false
      await loadSourcePurchaseOptions()
      loadList()
    } catch (error) {
      ElMessage.error(error.message || '新增失败')
    }
  })
}

onMounted(async () => {
  try {
    await loadSourcePurchaseOptions()
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
