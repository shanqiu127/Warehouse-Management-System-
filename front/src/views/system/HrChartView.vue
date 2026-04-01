<template>
  <div class="hr-chart-page">
    <section class="hero-panel">
      <div>
        <p class="hero-eyebrow">HR ANALYTICS</p>
        <h1>员工分布图表</h1>
        <p class="hero-desc">当前统计各部门在岗人员，包含员工、管理员以及超级管理员；超级管理员只有1人(归入系统管理部)。</p>
      </div>
      <el-button type="primary" :loading="loading" @click="loadData">刷新统计</el-button>
    </section>

    <section class="summary-grid">
      <article class="summary-card">
        <span>在岗人数总数</span>
        <strong>{{ chartData.totalEmployeeCount ?? 0 }}</strong>
      </article>
      <article class="summary-card">
        <span>有人口部门数</span>
        <strong>{{ chartData.occupiedDeptCount ?? 0 }}</strong>
      </article>
      <article class="summary-card">
        <span>人数最多部门</span>
        <strong>{{ chartData.topDeptName || '-' }}</strong>
        <small>{{ chartData.topDeptEmployeeCount ?? 0 }} 人</small>
      </article>
    </section>

    <section class="chart-grid" v-loading="loading">
      <el-card class="chart-card" shadow="never">
        <template #header>
          <div class="card-head">
            <span>各部门在岗人数</span>
            <small>柱状分布</small>
          </div>
        </template>
        <div ref="barChartRef" class="chart-canvas"></div>
      </el-card>

      <el-card class="chart-card" shadow="never">
        <template #header>
          <div class="card-head">
            <span>各部门员工占比</span>
            <small>饼图分布</small>
          </div>
        </template>
        <div ref="pieChartRef" class="chart-canvas"></div>
      </el-card>
    </section>

    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-head">
          <span>部门人口明细</span>
          <small>按部门名称展示当前统计结果</small>
        </div>
      </template>
      <el-table :data="chartData.deptStats || []" border>
        <el-table-column prop="deptName" label="部门名称" min-width="180" />
        <el-table-column prop="employeeCount" label="人数" width="140" />
        <el-table-column label="占比" width="140">
          <template #default="scope">
            {{ formatRatio(scope.row.ratio) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getHrEmployeeDistributionAPI } from '@/api/hr'
import { loadECharts } from '@/utils/echartsLoader'

const loading = ref(false)
const barChartRef = ref(null)
const pieChartRef = ref(null)
const chartData = ref({
  totalEmployeeCount: 0,
  deptCount: 0,
  occupiedDeptCount: 0,
  topDeptName: '-',
  topDeptEmployeeCount: 0,
  deptStats: []
})

let echarts = null
let barChart = null
let pieChart = null

const formatRatio = (ratio) => `${Number(ratio || 0).toFixed(2)}%`

const initCharts = async () => {
  if (!echarts) {
    echarts = await loadECharts()
  }
  if (barChartRef.value && !barChart) {
    barChart = echarts.init(barChartRef.value)
  }
  if (pieChartRef.value && !pieChart) {
    pieChart = echarts.init(pieChartRef.value)
  }
  updateCharts()
}

const updateCharts = () => {
  const stats = chartData.value.deptStats || []
  const barNames = stats.map((item) => item.deptName)
  const barValues = stats.map((item) => item.employeeCount || 0)
  const pieValues = stats
    .filter((item) => (item.employeeCount || 0) > 0)
    .map((item) => ({ name: item.deptName, value: item.employeeCount || 0 }))

  if (barChart) {
    barChart.setOption({
      color: ['#0f766e'],
      tooltip: { trigger: 'axis' },
      grid: { left: 48, right: 24, top: 36, bottom: 48 },
      xAxis: {
        type: 'category',
        data: barNames,
        axisLabel: { rotate: 25, color: '#475569' },
        axisLine: { lineStyle: { color: '#cbd5e1' } }
      },
      yAxis: {
        type: 'value',
        axisLabel: { color: '#475569' },
        splitLine: { lineStyle: { color: '#e2e8f0' } }
      },
      series: [{
        type: 'bar',
        data: barValues,
        barMaxWidth: 44,
        itemStyle: { borderRadius: [12, 12, 0, 0] }
      }]
    })
  }

  if (pieChart) {
    pieChart.setOption({
      color: ['#0f766e', '#f97316', '#2563eb', '#14b8a6', '#f59e0b', '#475569'],
      tooltip: { trigger: 'item', formatter: '{b}: {c} 人 ({d}%)' },
      legend: { bottom: 0, textStyle: { color: '#475569' } },
      series: [{
        type: 'pie',
        radius: ['42%', '72%'],
        center: ['50%', '44%'],
        label: { formatter: '{b}\n{d}%' },
        data: pieValues.length ? pieValues : [{ name: '暂无数据', value: 1, itemStyle: { color: '#cbd5e1' } }]
      }]
    })
  }
}

const handleResize = () => {
  barChart?.resize()
  pieChart?.resize()
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getHrEmployeeDistributionAPI()
    if (res.code !== 200) {
      throw new Error(res.msg || '员工图表加载失败')
    }
    chartData.value = {
      totalEmployeeCount: res.data?.totalEmployeeCount ?? 0,
      deptCount: res.data?.deptCount ?? 0,
      occupiedDeptCount: res.data?.occupiedDeptCount ?? 0,
      topDeptName: res.data?.topDeptName || '-',
      topDeptEmployeeCount: res.data?.topDeptEmployeeCount ?? 0,
      deptStats: res.data?.deptStats || []
    }
    await nextTick()
    await initCharts()
  } catch (error) {
    ElMessage.error(error.message || '员工图表加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  pieChart?.dispose()
  barChart = null
  pieChart = null
  echarts = null
})
</script>

<style scoped>
.hr-chart-page {
  display: grid;
  gap: 24px;
}

.hero-panel,
.summary-card,
.chart-card,
.table-card {
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 22px 45px -34px rgba(15, 23, 42, 0.32);
}

.hero-panel {
  border-radius: 24px;
  padding: 28px 32px;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
  background:
    radial-gradient(circle at top right, rgba(15, 118, 110, 0.14), transparent 30%),
    linear-gradient(180deg, #f8fcfb 0%, #eef6f5 100%);
}

.hero-eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.22em;
  color: #0f766e;
}

.hero-panel h1 {
  margin: 0;
  font-size: 2rem;
  color: #0f172a;
}

.hero-desc {
  margin: 12px 0 0;
  max-width: 760px;
  color: #475569;
  line-height: 1.75;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 18px;
}

.summary-card {
  border-radius: 20px;
  padding: 22px;
  display: grid;
  gap: 8px;
}

.summary-card span,
.summary-card small {
  color: #64748b;
}

.summary-card strong {
  font-size: 1.9rem;
  color: #0f172a;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.chart-card,
.table-card {
  border-radius: 24px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.card-head span {
  font-weight: 700;
  color: #0f172a;
}

.card-head small {
  color: #64748b;
}

.chart-canvas {
  height: 360px;
}

@media (max-width: 960px) {
  .hero-panel,
  .chart-grid {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
}
</style>