<template>
  <!-- 唯一的根容器 -->
  <div class="analysis-page-wrapper">
    <div class="analysis-container" v-loading="loading" element-loading-text="AI 算法建模计算中...">
      <el-card>
        <template #header>
          <div class="card-header">
            <span class="title">
              <el-icon><MagicStick /></el-icon> AI 客户价值聚类分析
            </span>
            <div class="op-group">
              <span class="label">聚类数量(K):</span>
              <!-- 输入框绑定 clusterK -->
              <el-input-number v-model="clusterK" :min="2" :max="5" size="small" style="margin-right: 15px" />
              <el-button type="primary" icon="VideoPlay" @click="handleRunAnalysis">执行模型训练</el-button>
              <el-button @click="handleReset" plain icon="RefreshLeft">重置结果</el-button>
            </div>
          </div>
        </template>

        <!-- 模型指标卡片 -->
        <el-row :gutter="20" style="margin-bottom: 20px;">
          <el-col :span="8">
            <el-card shadow="never" class="score-card sse">
              <el-statistic title="簇内平方和 (SSE)" :value="modelScore" :precision="2" />
              <div class="score-tip">↓ 越小表示群体内越凝聚</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="never" class="score-card silhouette">
              <el-statistic title="轮廓系数 (Silhouette)" :value="silScore" :precision="3" />
              <div class="score-tip">↑ 越接近1表示分类越科学</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="never" class="score-card kvalue">
              <el-statistic title="当前分群规模" :value="currentModelK" />
              <div class="score-tip">个业务目标群体</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 图表与统计表并排 -->
        <el-row :gutter="20">
          <el-col :span="15">
            <div id="scatterChart" style="width: 100%; height: 520px; border: 1px solid #f0f2f5; border-radius: 8px;"></div>
          </el-col>
          <el-col :span="9">
            <div class="summary-title">群体特征精细化解读</div>
            <el-table :data="clusterSummary" border size="small" style="width: 100%" height="450px">
              <el-table-column label="群体" width="60" align="center">
                <template #default="s">#{{ s.row.cluster_tag }}</template>
              </el-table-column>
              <el-table-column prop="label" label="业务定义">
                <template #default="s">
                  <el-tag size="small" :type="getTagType(s.row.label)">{{ s.row.label }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="count" label="人数" width="60" align="center" />
              <el-table-column label="均收(万)" align="right">
                <template #default="s">{{ (s.row.avgIncome/10000).toFixed(1) }}</template>
              </el-table-column>
            </el-table>
            <div class="biz-advice">
              <p><b>💡 决策参考：</b></p>
              <p>模型已根据“资产存量”与“收入流量”双因子自动标记客户。请根据业务定义对不同颜色的点簇制定差异化营销方案。</p>
            </div>
          </el-col>
        </el-row>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import axios from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { setPageInfo } from '@/utils/setInfo'

// --- 状态变量 ---
const loading = ref(false)
const clusterK = ref(3)       // 输入框控制的 K
const currentModelK = ref(0)  // 模型实际运行的 K
const modelScore = ref(0)
const silScore = ref(0)
const clusterSummary = ref([])
let myChart = null

// --- 工具函数 ---
const handleResize = () => myChart && myChart.resize()

const getTagType = (label) => {
  if (label.includes('顶级')) return 'danger'
  if (label.includes('中坚')) return 'success'
  if (label.includes('潜力')) return 'warning'
  return 'info'
}

// --- 数据获取核心逻辑 ---
const fetchData = async () => {
  try {
    // 1. 同步上一次运行的评分和 K 值 (确保刷新后状态一致)
    const scoreRes = await axios.get('/api/analysis/score')
    if (scoreRes.data) {
      modelScore.value = scoreRes.data.sse || 0
      silScore.value = scoreRes.data.sil || 0
      currentModelK.value = scoreRes.data.k || 0
      clusterK.value = scoreRes.data.k || 3 // 将输入框也同步为实际模型的值
    }

    // 2. 获取散点图原始数据
    const res = await axios.get('/api/analysis/chart-data')
    // 注意：我们将绘图逻辑放在这里，并传入后端真实数据
    renderScatterChart(res.data)

    // 3. 获取表格解读数据
    const summaryRes = await axios.get('/api/analysis/summary')
    clusterSummary.value = summaryRes.data || []
  } catch (error) {
    console.error('数据加载失败', error)
  }
}

// --- 执行训练 ---
const handleRunAnalysis = async () => {
  loading.value = true
  try {
    const res = await axios.post(`/api/analysis/run?k=${clusterK.value}`)
    ElMessage.success("AI 聚类模型训练成功！")
    await fetchData() // 重新获取所有数据并绘图
  } finally {
    loading.value = false
  }
}

// --- 重置 ---
const handleReset = () => {
  ElMessageBox.confirm('重置将清空所有分群标签，确定继续？', '提示', { type: 'warning' }).then(async () => {
    await axios.post('/api/analysis/reset')
    ElMessage.info('分群结果已重置')
    fetchData()
  }).catch(() => {})
}

// --- 绘图逻辑：数据驱动 ---
const renderScatterChart = (data) => {
  const chartDom = document.getElementById('scatterChart')
  if (!chartDom) return
  if (!myChart) myChart = echarts.init(chartDom)

  // 【核心修复】根据数据中实际存在的 clusterTag 来计算分群，而不是根据本地 clusterK 过滤
  const maxTag = Math.max(...data.map(item => item.clusterTag), -1)
  const numGroups = maxTag + 1

  const seriesData = Array.from({ length: Math.max(numGroups, 0) }, () => [])
  const colors = ['#F56C6C', '#409EFF', '#67C23A', '#E6A23C', '#9b59b6']
  const groupNames = ['高价值组', '潜力组', '一般组', '关注组', '其他组']

  data.forEach(item => {
    if (item.clusterTag !== -1 && item.clusterTag !== null) {
      seriesData[item.clusterTag].push([
        item.annualIncome,
        item.totalAssets,
        item.custName
      ])
    }
  })

  const series = seriesData.map((group, index) => ({
    name: groupNames[index] || `群体 ${index}`,
    type: 'scatter',
    data: group,
    symbolSize: 12,
    itemStyle: { color: colors[index] || '#999' },
    emphasis: { focus: 'series' }
  }))

  myChart.setOption({
    title: { text: '全行客户资产聚类分布空间', left: 'center', top: 10 },
    grid: { top: '15%', bottom: '12%', left: '10%', right: '5%' },
    tooltip: {
      formatter: (p) => `<b>${p.seriesName}</b><br/>姓名: ${p.value[2]}<br/>年收入: ￥${p.value[0].toLocaleString()}<br/>总资产: ￥${p.value[1].toLocaleString()}`
    },
    xAxis: { name: '年收入', scale: true, splitLine: { lineStyle: { type: 'dashed' } } },
    yAxis: { name: '总资产', scale: true, splitLine: { lineStyle: { type: 'dashed' } } },
    legend: { bottom: 0 },
    series: series
  }, true) // true 表示不合并旧数据，防止切换K值时点簇颜色混乱
}

// --- 生命周期 ---
onMounted(() => {
  setPageInfo("客户分群分析")
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (myChart) {
    myChart.dispose()
    myChart = null
  }
})
</script>

<style scoped>
.analysis-page-wrapper { padding: 20px; background-color: #f5f7fa; min-height: calc(100vh - 60px); }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.title { font-weight: bold; font-size: 18px; display: flex; align-items: center; gap: 8px; }
.op-group { display: flex; align-items: center; }
.op-group .label { font-size: 13px; color: #606266; margin-right: 8px; }
.score-card { text-align: center; border-top: 4px solid #409EFF; }
.score-tip { font-size: 11px; color: #999; margin-top: 5px; }
.summary-title { font-weight: bold; margin-bottom: 12px; color: #303133; font-size: 15px; }
.biz-advice { margin-top: 15px; font-size: 12px; color: #7f8c8d; line-height: 1.6; background: #fffdf6; padding: 10px; border: 1px dashed #e6a23c; border-radius: 4px; }
</style>