<template>
  <div class="purchase-return-container">
    <el-card>
      <div class="search-box">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="退货单号">
            <el-input v-model="searchForm.keywords" placeholder="请输入退货单号或商品名" clearable></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
            <el-button v-permission="['admin']" type="warning" @click="handleAdd">新建退货单</el-button>
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
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button size="small" type="primary" link @click="handleView(scope.row)">查看</el-button>
            <el-button v-permission="['admin']" size="small" type="danger" link @click="handleDelete(scope.row)">撤销</el-button>
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
        <el-form-item label="退货商品" prop="goodsId">
          <el-select v-model="dialogForm.goodsId" placeholder="请选择商品" style="width: 100%">
            <el-option v-for="item in goodsOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="退货数量" prop="returnQuantity">
          <el-input-number v-model="dialogForm.returnQuantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="退货单价" prop="price">
          <el-input-number v-model="dialogForm.price" :min="0.01" :precision="2" :step="0.1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="退货日期" prop="returnDate">
          <el-date-picker v-model="dialogForm.returnDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
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
import {
  createPurchaseReturnAPI,
  deletePurchaseReturnAPI,
  getGoodsOptionsAPI,
  getPurchaseReturnDetailAPI,
  getPurchaseReturnPageAPI
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
const dialogForm = reactive({ goodsId: null, returnQuantity: 1, price: 0, returnDate: '', reason: '' })

const dialogRules = {
  goodsId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  returnQuantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  price: [{ required: true, message: '请输入退货单价', trigger: 'blur' }],
  returnDate: [{ required: true, message: '请选择退货日期', trigger: 'change' }]
}

const normalizeDateTime = (val) => {
  if (!val) return ''
  return String(val).replace('T', ' ')
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
    const res = await getPurchaseReturnPageAPI(params)
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
    ElMessage.error(error.message || '加载退货数据失败')
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
  Object.assign(dialogForm, { goodsId: null, returnQuantity: 1, price: 0, returnDate: '', reason: '' })
  dialogVisible.value = true
}

const handleView = async (row) => {
  try {
    const res = await getPurchaseReturnDetailAPI(row.id)
    if (res.code !== 200) {
      throw new Error(res.msg || '查询详情失败')
    }
    const detail = res.data || {}
    dialogType.value = 'view'
    Object.assign(dialogForm, {
      goodsId: detail.goodsId ?? null,
      returnQuantity: detail.returnQuantity ?? detail.quantity ?? 1,
      price: detail.unitPrice ?? (detail.returnAmount && detail.quantity ? Number(detail.returnAmount) / Number(detail.quantity) : 0),
      returnDate: detail.returnDate ? String(detail.returnDate).slice(0, 10) : '',
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
    ElMessage.success('已撤销')
    loadList()
  }).catch((error) => {
    if (error?.message) {
      ElMessage.error(error.message)
    }
  })
}

const submitForm = () => {
  dialogFormRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }
    try {
      const payload = {
        goodsId: dialogForm.goodsId,
        quantity: dialogForm.returnQuantity,
        unitPrice: Number(dialogForm.price),
        operationTime: dialogForm.returnDate ? `${dialogForm.returnDate}T00:00:00` : undefined,
        remark: dialogForm.reason || ''
      }
      const res = await createPurchaseReturnAPI(payload)
      if (res.code !== 200) {
        throw new Error(res.msg || '新增失败')
      }
      ElMessage.success('退货开单成功')
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
  margin-bottom: 20px;
}
</style>
