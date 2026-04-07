<template>
  <div class="chart-container">
    <el-card shadow="never" class="mode-card">
      <div class="mode-switch">
        <span class="mode-label">统计视角</span>
        <el-radio-group v-model="viewMode" @change="fetchData">
          <el-radio-button label="sales">销售视角</el-radio-button>
          <el-radio-button v-if="canViewProfit" label="profit">毛利视角</el-radio-button>
        </el-radio-group>
      </div>
      <el-collapse
        v-if="canViewProfit && viewMode === 'profit'"
        v-model="profitExplainActiveNames"
        class="profit-explain-collapse"
      >
        <el-collapse-item name="profit-rule">
          <template #title>
            <div class="profit-title-wrap">
              <span class="profit-title-text">查看毛利计算规则</span>
              <el-tag type="success" size="small" effect="light">建议阅读</el-tag>
            </div>
          </template>

          <p v-if="Number(profitOverview.grossProfitAmount || 0) < 0" class="loss-warning">
            当前区间存在亏损
          </p>

          <p class="profit-explain-line scope">
            <span class="line-tag">范围</span>
            只统计已生效且未删除，并在所选时间内的销售单和销售退货单。
          </p>
          <p class="profit-explain-line sales">净销售额 = 销售总额 - 客退总额</p>
          <p class="profit-explain-line cost">快照成本 = 销售成本 - 客退成本（按开单时记录的成本）</p>
          <p class="profit-explain-line profit">毛利额 = 净销售额 - 快照成本</p>
          <p class="profit-explain-line rate">毛利率 = 毛利额 / 净销售额 × 100%（净销售额为 0 时按 0 处理）</p>
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <el-row :gutter="16" style="margin-bottom: 20px;">
      <el-col v-if="viewMode === 'sales'" :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">销售总额</div>
          <div class="summary-value">{{ formatCurrency(overview.salesAmount) }}</div>
        </el-card>
      </el-col>
      <el-col v-if="viewMode === 'sales'" :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">退货总额</div>
          <div class="summary-value warning">{{ formatCurrency(overview.returnAmount) }}</div>
        </el-card>
      </el-col>
      <el-col v-if="viewMode === 'sales'" :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">净销售额</div>
          <div class="summary-value success">{{ formatCurrency(overview.netSalesAmount) }}</div>
        </el-card>
      </el-col>
      <el-col v-if="viewMode === 'sales'" :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">销售/退货数量</div>
          <div class="summary-value">{{ overview.salesQuantity }} / {{ overview.returnQuantity }}</div>
        </el-card>
      </el-col>

      <el-col v-if="viewMode === 'profit'" :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">净销售额</div>
          <div class="summary-value">{{ formatCurrency(profitOverview.netSalesAmount) }}</div>
        </el-card>
      </el-col>
      <el-col v-if="viewMode === 'profit'" :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">快照成本</div>
          <div class="summary-value warning">{{ formatCurrency(profitOverview.estimatedCost) }}</div>
        </el-card>
      </el-col>
      <el-col v-if="viewMode === 'profit'" :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">毛利额</div>
          <div class="summary-value" :class="profitOverview.grossProfitAmount >= 0 ? 'success' : 'danger'">
            {{ formatCurrency(profitOverview.grossProfitAmount) }}
          </div>
        </el-card>
      </el-col>
      <el-col v-if="viewMode === 'profit'" :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">毛利率</div>
          <div class="summary-value" :class="profitOverview.grossProfitRate >= 0 ? 'success' : 'danger'">
            {{ formatRate(profitOverview.grossProfitRate) }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-bottom: 20px;">
      <template #header>
        <div class="card-header">
          <span>数据筛选</span>
        </div>
      </template>
      <el-form :inline="true">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          ></el-date-picker>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" :loading="loading" @click="fetchData">查询统计</el-button>
          <el-button :icon="Refresh" :disabled="loading" @click="resetData">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="20">
      <el-col :xs="24" :md="12">
        <el-card>
          <div ref="barChartRef" style="height: 350px;"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card>
          <div ref="pieChartRef" style="height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row style="margin-top: 20px;">
      <el-col :span="24">
        <el-card>
          <div ref="lineChartRef" style="height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { loadECharts } from '@/utils/echartsLoader'
import { useUserStore } from '@/stores/user'
import { normalizeDeptCode } from '@/utils/auth'
import {
  getChartOverviewAPI,
  getChartTop5API,
  getChartBrandRatioAPI,
  getChartDailyTrendAPI,
  getChartProfitOverviewAPI,
  getChartProfitBrandTopAPI,
  getChartProfitDailyTrendAPI
} from '@/api/business'

const userStore = useUserStore()
const canViewProfit = computed(() => userStore.role === 'admin' && normalizeDeptCode(userStore.deptCode) === 'finance')

const viewMode = ref('sales')
const profitExplainActiveNames = ref([])
const dateRange = ref([])
const loading = ref(false)
const barChartRef = ref(null)
const pieChartRef = ref(null)
const lineChartRef = ref(null)

const overview = ref({
  salesAmount: 0,
  returnAmount: 0,
  netSalesAmount: 0,
  salesQuantity: 0,
  returnQuantity: 0
})
const top5Data = ref({ nameList: [], dataList: [] })
const brandRatioData = ref([])
const trendData = ref({ dateList: [], amountList: [] })

const profitOverview = ref({
  netSalesAmount: 0,
  estimatedCost: 0,
  grossProfitAmount: 0,
  grossProfitRate: 0
})
const profitTopData = ref({ nameList: [], dataList: [] })
const profitTrendData = ref({ dateList: [], amountList: [] })

let barChart = null
let pieChart = null
let lineChart = null
let echarts = null

watch(canViewProfit, (allowed) => {
  if (!allowed && viewMode.value === 'profit') {
    viewMode.value = 'sales'
  }
}, { immediate: true })

const initCharts = async () => {
  if (!echarts) {
    echarts = await loadECharts()
  }
  if (barChartRef.value) barChart = echarts.init(barChartRef.value)
  if (pieChartRef.value) pieChart = echarts.init(pieChartRef.value)
  if (lineChartRef.value) lineChart = echarts.init(lineChartRef.value)

  updateCharts()
  window.addEventListener('resize', handleResize)
}

const updateCharts = () => {
  if (viewMode.value === 'profit') {
    updateProfitCharts()
    return
  }
  updateSalesCharts()
}

const updateSalesCharts = () => {
  const names = top5Data.value.nameList?.length ? top5Data.value.nameList : ['暂无数据']
  const quantities = top5Data.value.dataList?.length ? top5Data.value.dataList : [0]
  const brandList = brandRatioData.value?.length ? brandRatioData.value : [{ name: '暂无数据', value: 0 }]
  const trendDates = trendData.value.dateList?.length ? trendData.value.dateList : ['暂无数据']
  const trendAmounts = trendData.value.amountList?.length ? trendData.value.amountList : [0]

  if (barChart) {
    barChart.setOption({
      title: { text: '商品销量 TOP5', left: 'center' },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: names },
      yAxis: { type: 'value', name: '单位：件' },
      series: [{
        name: '销量',
        type: 'bar',
        barWidth: '40%',
        data: quantities,
        itemStyle: { color: '#409EFF' }
      }]
    })
  }

  if (pieChart) {
    pieChart.setOption({
      title: { text: '各品牌销售额占比', left: 'center' },
      tooltip: { trigger: 'item', formatter: '{a} <br/>{b} : {c}元 ({d}%)' },
      legend: { orient: 'vertical', left: 'left' },
      series: [{
        name: '销售额',
        type: 'pie',
        radius: '60%',
        center: ['50%', '55%'],
        data: brandList,
        emphasis: {
          itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' }
        }
      }]
    })
  }

  if (lineChart) {
    lineChart.setOption({
      title: { text: '销售额走势', left: 'center' },
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: trendDates },
      yAxis: { type: 'value', name: '金额(元)' },
      series: [{
        name: '销售额',
        type: 'line',
        data: trendAmounts,
        smooth: true,
        areaStyle: {},
        itemStyle: { color: '#67C23A' }
      }]
    })
  }
}

const updateProfitCharts = () => {
  const names = profitTopData.value.nameList?.length ? profitTopData.value.nameList : ['暂无数据']
  const profits = toNumberArray(profitTopData.value.dataList?.length ? profitTopData.value.dataList : [0])
  const trendDates = profitTrendData.value.dateList?.length ? profitTrendData.value.dateList : ['暂无数据']
  const trendAmounts = toNumberArray(profitTrendData.value.amountList?.length ? profitTrendData.value.amountList : [0])
  const pieData = buildProfitPieData()

  if (barChart) {
    barChart.setOption({
      title: { text: '品牌毛利 TOP5', left: 'center' },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: names },
      yAxis: { type: 'value', name: '金额(元)' },
      series: [{
        name: '毛利额',
        type: 'bar',
        barWidth: '40%',
        data: profits,
        itemStyle: { color: '#E67E22' }
      }]
    })
  }

  if (pieChart) {
    pieChart.setOption({
      title: { text: '毛利结构', left: 'center' },
      tooltip: { trigger: 'item', formatter: '{a} <br/>{b} : {c}元 ({d}%)' },
      legend: { orient: 'vertical', left: 'left' },
      series: [{
        name: '金额',
        type: 'pie',
        radius: '60%',
        center: ['50%', '55%'],
        data: pieData,
        emphasis: {
          itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' }
        }
      }]
    })
  }

  if (lineChart) {
    lineChart.setOption({
      title: { text: '毛利额走势', left: 'center' },
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: trendDates },
      yAxis: { type: 'value', name: '金额(元)' },
      series: [{
        name: '毛利额',
        type: 'line',
        data: trendAmounts,
        smooth: true,
        areaStyle: {},
        itemStyle: { color: '#F56C6C' }
      }]
    })
  }
}

const buildProfitPieData = () => {
  const netSales = Number(profitOverview.value.netSalesAmount || 0)
  const estimatedCost = Number(profitOverview.value.estimatedCost || 0)
  const grossProfit = Number(profitOverview.value.grossProfitAmount || 0)

  if (grossProfit >= 0) {
    const data = [
      { name: '快照成本', value: Math.max(estimatedCost, 0) },
      { name: '毛利额', value: Math.max(grossProfit, 0) }
    ].filter((item) => item.value > 0)
    return data.length ? data : [{ name: '暂无数据', value: 0 }]
  }

  const data = [
    { name: '净销售额', value: Math.max(netSales, 0) },
    { name: '亏损额', value: Math.abs(grossProfit) }
  ].filter((item) => item.value > 0)
  return data.length ? data : [{ name: '暂无数据', value: 0 }]
}

const handleResize = () => {
  barChart?.resize()
  pieChart?.resize()
  lineChart?.resize()
}

const buildParams = () => {
  if (!dateRange.value || dateRange.value.length !== 2) {
    return {}
  }
  return {
    startDate: dateRange.value[0],
    endDate: dateRange.value[1]
  }
}

const normalizeBizRes = (res, fallbackMsg) => {
  if (!res || res.code !== 200) {
    throw new Error(res?.msg || fallbackMsg)
  }
  return res.data
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = buildParams()

    if (viewMode.value === 'profit') {
      const [profitOverviewRes, profitTopRes, profitTrendRes] = await Promise.all([
        getChartProfitOverviewAPI(params),
        getChartProfitBrandTopAPI(params),
        getChartProfitDailyTrendAPI(params)
      ])

      const profitOverviewData = normalizeBizRes(profitOverviewRes, '毛利概览加载失败')
      const profitTopChartData = normalizeBizRes(profitTopRes, '品牌毛利TOP5加载失败')
      const profitTrendChartData = normalizeBizRes(profitTrendRes, '毛利走势加载失败')

      profitOverview.value = profitOverviewData || {
        netSalesAmount: 0,
        estimatedCost: 0,
        grossProfitAmount: 0,
        grossProfitRate: 0
      }
      profitTopData.value = profitTopChartData || { nameList: [], dataList: [] }
      profitTrendData.value = profitTrendChartData || { dateList: [], amountList: [] }
    } else {
      const [overviewRes, top5Res, brandRes, trendRes] = await Promise.all([
        getChartOverviewAPI(params),
        getChartTop5API(params),
        getChartBrandRatioAPI(params),
        getChartDailyTrendAPI(params)
      ])

      const overviewData = normalizeBizRes(overviewRes, '销售概览加载失败')
      const top5ChartData = normalizeBizRes(top5Res, '商品销量TOP5加载失败')
      const brandRatioChartData = normalizeBizRes(brandRes, '品牌占比加载失败')
      const trendChartData = normalizeBizRes(trendRes, '销售走势加载失败')

      overview.value = overviewData || {
        salesAmount: 0,
        returnAmount: 0,
        netSalesAmount: 0,
        salesQuantity: 0,
        returnQuantity: 0
      }
      top5Data.value = top5ChartData || { nameList: [], dataList: [] }
      brandRatioData.value = Array.isArray(brandRatioChartData) ? brandRatioChartData : []
      trendData.value = trendChartData || { dateList: [], amountList: [] }
    }

    updateCharts()
    ElMessage.success('图表数据刷新成功')
  } catch (error) {
    ElMessage.error(error?.message || '图表数据加载失败，请检查登录状态或后端服务')
  } finally {
    loading.value = false
  }
}

const resetData = () => {
  dateRange.value = []
  fetchData()
}

const formatCurrency = (value) => {
  const num = Number(value || 0)
  return `￥${num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

const formatRate = (value) => {
  const num = Number(value || 0)
  return `${num.toFixed(2)}%`
}

const toNumberArray = (arr) => arr.map((item) => Number(item || 0))

onMounted(() => {
  nextTick(async () => {
    await initCharts()
    fetchData()
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  pieChart?.dispose()
  lineChart?.dispose()
  barChart = null
  pieChart = null
  lineChart = null
  echarts = null
})
</script>

<style scoped>
.chart-container {
  padding: 10px;
}

.mode-card {
  margin-bottom: 16px;
}

.mode-switch {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.mode-label {
  color: #606266;
  font-size: 14px;
  font-weight: 600;
}

.profit-explain-collapse {
  margin-top: 10px;
}

.profit-title-wrap {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.profit-title-text {
  color: #1f2d3d;
  font-weight: 600;
}

:deep(.profit-explain-collapse .el-collapse-item__header) {
  height: 42px;
  padding: 0 12px;
  border-radius: 8px;
  background: #f5f9ff;
  border: 1px solid #d9ecff;
}

:deep(.profit-explain-collapse .el-collapse-item__wrap) {
  border: 1px solid #e4e7ed;
  border-top: none;
  border-radius: 0 0 8px 8px;
}

.loss-warning {
  margin: 0 0 8px;
  padding: 6px 10px;
  border-radius: 6px;
  color: #b42318;
  background: #fef3f2;
  border: 1px solid #fecdca;
  font-size: 13px;
  font-weight: 600;
}

.profit-explain-line {
  margin: 6px 0;
  line-height: 1.55;
  font-size: 13px;
}

.profit-explain-line .line-tag {
  display: inline-block;
  margin-right: 6px;
  padding: 1px 6px;
  border-radius: 4px;
  color: #606266;
  background: #f2f6fc;
  font-size: 12px;
}

.profit-explain-line.sales {
  color: #1d4ed8;
}

.profit-explain-line.cost {
  color: #b45309;
}

.profit-explain-line.profit {
  color: #047857;
  font-weight: 600;
}

.profit-explain-line.rate {
  color: #7c3aed;
}

.profit-explain-line.scope {
  color: #4b5563;
}

.card-header {
  font-weight: bold;
}

.summary-card {
  margin-bottom: 12px;
}

.summary-label {
  color: #909399;
  font-size: 13px;
}

.summary-value {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}

.summary-value.success {
  color: #67c23a;
}

.summary-value.warning {
  color: #e6a23c;
}

.summary-value.danger {
  color: #f56c6c;
}
</style>
