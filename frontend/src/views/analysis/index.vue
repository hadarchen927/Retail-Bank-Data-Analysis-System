<template>
  <div class="analysis-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">
            <el-icon><MagicStick /></el-icon> AI 客户价值聚类分析
          </span>
          <div class="op-group">
            <!-- 执行算法按钮：不再需要数字前缀 -->
            <el-button
                type="primary"
                icon="VideoPlay"
                :loading="loading"
                @click="handleRunAnalysis"
            >
              执行 K-Means 聚类分析
            </el-button>
            <el-button @click="handleReset" plain icon="RefreshLeft">重置分群结果</el-button>
          </div>
        </div>
      </template>

      <!-- 说明文字调整 -->
      <el-alert
          title="算法说明：系统将读取“数据集成中心”导入的资产数据，利用机器学习算法自动识别客户价值分群。若数据未显示，请确认是否已完成数据导入。"
          type="success"
          show-icon
          style="margin-bottom: 20px"
      />

      <el-row :gutter="20" style="margin-bottom: 20px;">
        <el-col :span="8">
          <el-statistic title="模型评估得分 (SSE)" :value="modelScore" :precision="2" suffix=" (越小越紧凑)" />
        </el-col>
      </el-row>

      <!-- 图表容器 -->
      <div id="scatterChart" style="width: 100%; height: 500px; background: #fff; border-radius: 8px;"></div>
    </el-card>
  </div>
</template>

<script setup>
const modelScore = ref(0)
import { ref, onMounted } from 'vue'
import axios from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'

const loading = ref(false)
let myChart = null

// 获取最新分群数据并绘图
const fetchChartData = async () => {
  try {
    // 获取图表数据
    const res = await axios.get('/api/analysis/chart-data')
    renderScatterChart(res.data)

    // 【新增】获取之前的模型分数
    const scoreRes = await axios.get('/api/analysis/score')
    modelScore.value = scoreRes.data || 0
  } catch (error) {
    console.error('获取分析数据失败', error)
  }
}

// 调用 Python 算法
const handleRunAnalysis = async () => {
  loading.value = true
  try {
    const res = await axios.post('/api/analysis/run')
    // res.data 现在就是后端传回来的那个 Double 分数
    modelScore.value = res.data

    ElMessage.success("分析完成！")
    await fetchChartData()
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 重置算法结果（把所有标签变回 -1）
const handleReset = () => {
  ElMessageBox.confirm('确定要清空已有的分群结果吗？', '提示', { type: 'warning' }).then(async () => {
    await axios.post('/api/analysis/reset')
    ElMessage.info('结果已重置，请重新执行分析')
    fetchChartData()
  })
}

// ECharts 渲染逻辑
const renderScatterChart = (data) => {
  const chartDom = document.getElementById('scatterChart')
  if (!chartDom) return
  if (!myChart) myChart = echarts.init(chartDom)

  // 准备三个聚类组的数据
  const seriesData = [[], [], []]
  data.forEach(item => {
    if (item.clusterTag !== -1 && item.clusterTag !== null) {
      seriesData[item.clusterTag].push([
        item.annualIncome,
        item.totalAssets,
        item.custName
      ])
    }
  })

  const option = {
    title: { text: '零售银行客户资产聚类分布图', left: 'center' },
    grid: { top: '15%', bottom: '10%' },
    tooltip: {
      formatter: (p) => `姓名: ${p.value[2]}<br/>年收入: ￥${p.value[0].toLocaleString()}<br/>总资产: ￥${p.value[1].toLocaleString()}`
    },
    xAxis: { name: '年收入', scale: true, splitLine: { lineStyle: { type: 'dashed' } } },
    yAxis: { name: '总资产', scale: true, splitLine: { lineStyle: { type: 'dashed' } } },
    legend: { data: ['高价值客户', '潜力客户', '普通客户'], bottom: 0 },
    series: [
      { name: '高价值客户', type: 'scatter', data: seriesData[0], symbolSize: 15, itemStyle: { color: '#F56C6C' } },
      { name: '潜力客户', type: 'scatter', data: seriesData[1], symbolSize: 12, itemStyle: { color: '#409EFF' } },
      { name: '普通客户', type: 'scatter', data: seriesData[2], symbolSize: 10, itemStyle: { color: '#67C23A' } }
    ]
  }
  myChart.setOption(option)
}

onMounted(() => {
  fetchChartData()
  window.addEventListener('resize', () => myChart && myChart.resize())
})
</script>

<style scoped>
.analysis-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.title { font-size: 18px; font-weight: bold; color: #303133; display: flex; align-items: center; gap: 8px; }
</style>