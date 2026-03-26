<template>
  <el-card>
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <span>库存预警中心</span>
        <el-tag type="warning">仅管理员/员工可见</el-tag>
      </div>
    </template>

    <el-form :inline="true" :model="searchForm">
      <el-form-item label="预警类型">
        <el-select v-model="searchForm.warningType" style="width: 160px;" clearable>
          <el-option label="全部预警" value="" />
          <el-option label="低库存" value="low" />
          <el-option label="零库存" value="zero" />
        </el-select>
      </el-form-item>
      <el-form-item label="商品名称">
        <el-input v-model="searchForm.goodsName" placeholder="请输入商品名称" clearable />
      </el-form-item>
      <el-form-item label="供应商">
        <el-select v-model="searchForm.supplierId" style="width: 180px;" clearable>
          <el-option v-for="sup in suppliers" :key="sup.id" :label="sup.name" :value="sup.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" border v-loading="loading" style="width: 100%; margin-top: 10px;">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="goodsName" label="商品名称" min-width="180" />
      <el-table-column prop="supplierName" label="供应商" min-width="140" />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column prop="stock" label="当前库存" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.stock <= (scope.row.warningStock ?? 10) ? 'danger' : 'success'">{{ scope.row.stock }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="warningStock" label="预警阈值" width="100" />
      <el-table-column label="预警级别" width="120">
        <template #default="scope">
          <el-tag v-if="scope.row.stock === 0" type="danger">零库存</el-tag>
          <el-tag v-else type="warning">低库存</el-tag>
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
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getStockWarningPageAPI, getSupplierOptionsAPI } from '@/api/base'

const route = useRoute()
const suppliers = ref([])
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  warningType: '',
  goodsName: '',
  supplierId: null
})

const applyRouteQuery = () => {
  const type = String(route.query.type || '').toLowerCase()
  if (type === 'zero' || type === 'low') {
    searchForm.warningType = type
  }
}

const loadSuppliers = async () => {
  try {
    const res = await getSupplierOptionsAPI()
    if (res.code !== 200) {
      throw new Error(res.msg || '供应商下拉加载失败')
    }
    suppliers.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || '供应商下拉加载失败')
  }
}

const loadList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      goodsName: searchForm.goodsName || undefined,
      supplierId: searchForm.supplierId || undefined,
      warningType: searchForm.warningType || undefined
    }
    const res = await getStockWarningPageAPI(params)
    if (res.code !== 200) {
      throw new Error(res.msg || '预警商品查询失败')
    }
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    ElMessage.error(error.message || '预警商品查询失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadList()
}

const resetSearch = () => {
  searchForm.warningType = ''
  searchForm.goodsName = ''
  searchForm.supplierId = null
  currentPage.value = 1
  loadList()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadList()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadList()
}

onMounted(async () => {
  applyRouteQuery()
  await loadSuppliers()
  await loadList()
})
</script>
