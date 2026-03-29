<template>
  <el-card>
    <el-form :inline="true" :model="searchForm">
      <el-form-item label="商品名称">
        <el-input v-model="searchForm.goodsName" placeholder="请输入商品名称" clearable />
      </el-form-item>
      <el-form-item label="供应商">
        <el-select v-model="searchForm.supplierId" placeholder="请选择供应商" clearable style="width: 180px;">
          <el-option v-for="sup in suppliers" :key="sup.id" :label="sup.name" :value="sup.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
        <el-button type="success" @click="handleAdd" v-permission="{ roles: ['admin'], deptCodes: ['warehouse'] }">新增商品</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" border style="width: 100%" v-loading="loading">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="goodsName" label="商品名称" />
      <el-table-column prop="category" label="分类" width="100" />
      <!-- 商品关联供应商展示 -->
      <el-table-column prop="supplierName" label="所属供应商" />
      <el-table-column prop="price" label="单价" width="100" />
      <!-- 商品库存展示 -->
      <el-table-column prop="stock" label="当前库存" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.stock <= (scope.row.warningStock ?? 10) ? 'danger' : 'success'">{{ scope.row.stock }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="warningStock" label="预警阈值" width="100" />
      
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="handleView(scope.row)">详情</el-button>
          <el-button size="small" type="primary" @click="handleEdit(scope.row)" v-permission="{ roles: ['admin'], deptCodes: ['warehouse'] }">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)" v-permission="{ roles: ['admin'], deptCodes: ['warehouse'] }">删除</el-button>
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

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="550px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" :disabled="isView">
        <el-form-item label="商品名称" required>
          <el-input v-model="form.goodsName" :disabled="isView"></el-input>
        </el-form-item>
        <el-form-item label="分类" required>
          <el-input v-model="form.category" :disabled="isView"></el-input>
        </el-form-item>
        <el-form-item label="供应商" required>
          <el-select v-model="form.supplierId" placeholder="请绑定供应商" :disabled="isView" style="width: 100%;">
            <el-option v-for="sup in suppliers" :key="sup.id" :label="sup.name" :value="sup.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="进价" required>
          <el-input-number v-model="form.purchasePrice" :disabled="isView" :min="0.01" :precision="2" :step="0.1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="售价" required>
          <el-input-number v-model="form.salePrice" :disabled="isView" :min="0.01" :precision="2" :step="0.1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="品牌">
          <el-input v-model="form.brand" :disabled="isView"></el-input>
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="form.unit" :disabled="isView"></el-input>
        </el-form-item>
        <el-form-item label="初始库存" v-if="!isView && dialogTitle === '新增商品'">
          <el-input-number v-model="form.stock" :min="0" />
        </el-form-item>
        <el-form-item label="预警阈值" v-if="!isView">
          <el-input-number v-model="form.warningStock" :min="0" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="当前库存" v-if="isView">
          <el-input v-model="form.stock" disabled />
        </el-form-item>
        <el-form-item label="预警阈值" v-if="isView">
          <el-input v-model="form.warningStock" disabled />
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
  createGoodsAPI,
  deleteGoodsAPI,
  getGoodsDetailAPI,
  getGoodsPageAPI,
  getSupplierOptionsAPI,
  updateGoodsAPI
} from '@/api/base'

const suppliers = ref([])

const searchForm = reactive({ goodsName: '', supplierId: null })
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isView = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  goodsName: '',
  category: '',
  supplierId: null,
  purchasePrice: 0,
  salePrice: 0,
  brand: '',
  unit: '',
  stock: 0,
  warningStock: 10
})

const rules = {
  goodsName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  purchasePrice: [{ required: true, message: '请输入进价', trigger: 'change' }],
  salePrice: [{ required: true, message: '请输入售价', trigger: 'change' }]
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
      supplierId: searchForm.supplierId || undefined
    }
    const res = await getGoodsPageAPI(params)
    if (res.code !== 200) {
      throw new Error(res.msg || '商品查询失败')
    }
    const pageData = res.data || {}
    tableData.value = (pageData.records || []).map((item) => ({
      ...item,
      price: item.price ?? item.salePrice
    }))
    total.value = pageData.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载商品失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadList()
}

const resetSearch = () => {
  searchForm.goodsName = ''
  searchForm.supplierId = null
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
  form.id = null
  form.goodsName = ''
  form.category = ''
  form.supplierId = null
  form.purchasePrice = 0
  form.salePrice = 0
  form.brand = ''
  form.unit = ''
  form.stock = 0
  form.warningStock = 10
}

const handleAdd = () => {
  isView.value = false
  dialogTitle.value = '新增商品'
  initForm()
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

const openByDetail = async (row, viewMode) => {
  const res = await getGoodsDetailAPI(row.id)
  if (res.code !== 200) {
    throw new Error(res.msg || '商品详情查询失败')
  }
  const detail = res.data || {}
  isView.value = viewMode
  dialogTitle.value = viewMode ? '商品详情' : '编辑商品'
  Object.assign(form, {
    id: detail.id,
    goodsName: detail.goodsName || '',
    category: detail.category || '',
    supplierId: detail.supplierId || null,
    purchasePrice: detail.purchasePrice || 0,
    salePrice: detail.salePrice || 0,
    brand: detail.brand || '',
    unit: detail.unit || '',
    stock: detail.stock || 0,
    warningStock: detail.warningStock ?? 10
  })
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

const handleView = async (row) => {
  try {
    await openByDetail(row, true)
  } catch (error) {
    ElMessage.error(error.message || '加载商品详情失败')
  }
}

const handleEdit = async (row) => {
  try {
    await openByDetail(row, false)
  } catch (error) {
    ElMessage.error(error.message || '加载商品详情失败')
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该商品资料?', '警告', { type: 'warning' })
    .then(async () => {
      const res = await deleteGoodsAPI(row.id)
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
        goodsName: form.goodsName,
        category: form.category,
        brand: form.brand,
        supplierId: form.supplierId,
        purchasePrice: form.purchasePrice,
        salePrice: form.salePrice,
        stock: form.stock,
        warningStock: form.warningStock,
        unit: form.unit,
        status: 1
      }
      const res = form.id ? await updateGoodsAPI(form.id, payload) : await createGoodsAPI(payload)
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

onMounted(async () => {
  await loadSuppliers()
  await loadList()
})
</script>