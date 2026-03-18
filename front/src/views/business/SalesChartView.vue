<template>
  <div class="chart-container">
    <el-row :gutter="16" style="margin-bottom: 20px;">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">销售总额</div>
          <div class="summary-value">{{ formatCurrency(overview.salesAmount) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">退货总额</div>
          <div class="summary-value warning">{{ formatCurrency(overview.returnAmount) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">净销售额</div>
          <div class="summary-value success">{{ formatCurrency(overview.netSalesAmount) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-label">销售/退货数量</div>
          <div class="summary-value">{{ overview.salesQuantity }} / {{ overview.returnQuantity }}</div>
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
          <el-button type="primary" :loading="loading" @click="fetchData">查询统计</el-button>
          <el-button :disabled="loading" @click="resetData">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <div ref="barChartRef" style="height: 350px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
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
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { loadECharts } from '@/utils/echartsLoader'
import {
  getChartOverviewAPI,
  getChartTop5API,
  getChartBrandRatioAPI,
  getChartDailyTrendAPI
} from '@/api/business'

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

let barChart = null
let pieChart = null
let lineChart = null
let echarts = null

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
        stack: 'Total',
        data: trendAmounts,
        smooth: true,
        areaStyle: {},
        itemStyle: { color: '#67C23A' }
      }]
    })
  }
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

const fetchData = async () => {
  loading.value = true
  try {
    const params = buildParams()
    const [overviewRes, top5Res, brandRes, trendRes] = await Promise.all([
      getChartOverviewAPI(params),
      getChartTop5API(params),
      getChartBrandRatioAPI(params),
      getChartDailyTrendAPI(params)
    ])

    overview.value = overviewRes?.data || {
      salesAmount: 0,
      returnAmount: 0,
      netSalesAmount: 0,
      salesQuantity: 0,
      returnQuantity: 0
    }
    top5Data.value = top5Res?.data || { nameList: [], dataList: [] }
    brandRatioData.value = Array.isArray(brandRes?.data) ? brandRes.data : []
    trendData.value = trendRes?.data || { dateList: [], amountList: [] }

    updateCharts()
    ElMessage.success('图表数据刷新成功')
  } catch (error) {
    ElMessage.error('图表数据加载失败，请检查登录状态或后端服务')
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
</style>
