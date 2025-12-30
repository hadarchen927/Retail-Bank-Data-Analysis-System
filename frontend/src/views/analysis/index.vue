<template>
  <div class="analysis-page-wrapper">
    <div class="analysis-container" v-loading="loading" :element-loading-text="loadingText">
      <el-card>
        <template #header>
          <div class="card-header">
            <span class="title">
              <el-icon><MagicStick /></el-icon> AI 客户价值聚类分析
            </span>

            <div class="op-group">
              <!-- K值选择器：支持 2-6 个分群 -->
              <span class="label">分群规模(K):</span>
              <el-input-number v-model="clusterK" :min="2" :max="6" size="small" style="margin-right: 15px" />

              <!-- 管理员/分析师权限：执行与重置 -->
              <template v-if="userRole !== 'MANAGER'">
                <el-button type="primary" icon="VideoPlay" @click="handleRunAnalysis">执行模型训练</el-button>
                <el-button @click="handleReset" plain icon="RefreshLeft">重置结果</el-button>
              </template>

              <!-- 经理权限：仅生成查阅视图 -->
              <template v-else>
                <el-button type="success" icon="DataAnalysis" @click="handleRunAnalysis">生成决策建议视图</el-button>
                <el-tag type="info" effect="plain" style="margin-left: 10px">决策探索模式</el-tag>
              </template>
            </div>
          </div>
        </template>

        <el-alert
            :title="userRole === 'MANAGER' ? '经理您好，您可以调整 K 值观察不同的客户画像分布，下方“肘部法则”图表将辅助您判断分类数选择的科学性。' : '模型训练说明：系统将通过 Python 执行 K-Means 算法，并自动计算最优 K 值评估指标。'"
            :type="userRole === 'MANAGER' ? 'info' : 'success'"
            show-icon
            style="margin-bottom: 20px"
        />

        <!-- 模型核心指标卡片 -->
        <el-row :gutter="20" style="margin-bottom: 20px;">
          <el-col :span="8">
            <el-card shadow="never" class="score-card sse">
              <el-statistic title="簇内平方和 (SSE)" :value="modelScore" :precision="2" />
              <div class="score-tip">↓ 数值越小表示群体内部越聚合</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="never" class="score-card silhouette">
              <el-statistic title="轮廓系数 (Silhouette)" :value="silScore" :precision="3" />
              <div class="score-tip">↑ 越接近 1 表示分群效果越完美</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="never" class="score-card kvalue">
              <el-statistic title="当前视图规模" :value="currentModelK" />
              <div class="score-tip">个差异化业务目标群体</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 第一行：分布散点图 + 特征解读表 -->
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
                  <el-tag size="small" :type="getTagType(s.row.label)" effect="dark">{{ s.row.label }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="count" label="人数" width="60" align="center" />
              <el-table-column label="均收(万)" align="right">
                <template #default="s">{{ (s.row.avgIncome/10000).toFixed(1) }}</template>
              </el-table-column>
            </el-table>
            <div class="biz-advice">
              <p><b>💡 经理决策参考：</b></p>
              <p>建议重点关注 <b>{{ getTopGroup() }}</b>，该群体在年收入与资产存量维度表现最突出，是开展私人银行与高端理财业务的核心目标。</p>
            </div>
          </el-col>
        </el-row>

        <!-- 第二行：肘部法则评估折线图 -->
        <el-row style="margin-top: 25px;">
          <el-col :span="24">
            <el-card shadow="never" class="evaluation-card">
              <template #header>
                <div class="card-title-bold"><el-icon><TrendCharts /></el-icon> 模型性能评估：肘部法则 (Elbow Method)</div>
              </template>
              <div id="elbowChart" style="width: 100%; height: 320px;"></div>
              <div class="elbow-desc">注：折线弯曲最剧烈处（即“肘部”）对应的 K 值通常为该数据集下的最佳聚类方案。</div>
            </el-card>
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
const userRole = ref(localStorage.getItem('role') || 'GUEST')
const clusterK = ref(3)
const currentModelK = ref(0)
const modelScore = ref(0)
const silScore = ref(0)
const clusterSummary = ref([])
let myChart = null
let myElbowChart = null

const loadingText = computed(() => {
  return userRole.value === 'MANAGER' ? '系统正在为您生成多维决策视图...' : 'AI 算法正在进行大规模模型训练与性能评估...'
})

// --- 逻辑函数 ---

const getTagType = (label) => {
  if (label.includes('顶级')) return 'danger'
  if (label.includes('中坚')) return 'success'
  if (label.includes('潜力')) return 'warning'
  return 'info'
}

const getTopGroup = () => {
  if (clusterSummary.value.length === 0) return "重点群体"
  const top = [...clusterSummary.value].sort((a,b) => b.avgIncome - a.avgIncome)[0]
  return top.label
}

const handleResize = () => {
  myChart && myChart.resize()
  myElbowChart && myElbowChart.resize()
}

// 渲染肘部图表
const renderElbowChart = (data) => {
  const chartDom = document.getElementById('elbowChart')
  if (!chartDom) return
  if (!myElbowChart) myElbowChart = echarts.init(chartDom)

  const xAxis = data.map(item => 'K=' + item.k)
  const yAxis = data.map(item => item.sse)

  myElbowChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '8%', right: '5%', bottom: '10%' },
    xAxis: { type: 'category', data: xAxis, boundaryGap: false },
    yAxis: { type: 'value', name: 'SSE 值' },
    series: [{
      data: yAxis,
      type: 'line',
      smooth: true,
      symbolSize: 10,
      itemStyle: { color: '#E6A23C' },
      lineStyle: { width: 3 },
      areaStyle: { color: 'rgba(230, 162, 60, 0.1)' },
      markPoint: { data: [{ type: 'min', name: '建议K值' }] }
    }]
  })
}

// 渲染散点分布图
const renderScatterChart = (data) => {
  const chartDom = document.getElementById('scatterChart')
  if (!chartDom) return
  if (!myChart) myChart = echarts.init(chartDom)

  // 1. 计算实际分群数
  const maxTag = Math.max(...data.map(item => item.clusterTag), -1)
  const numGroups = maxTag + 1

  // 2. 准备容器、颜色和语义化名称
  const seriesData = Array.from({ length: Math.max(numGroups, 0) }, () => [])
  const colors = ['#F56C6C', '#409EFF', '#67C23A', '#E6A23C', '#9b59b6', '#34495e']
  const groupNames = ['高价值组', '潜力组', '一般组', '关注组', '其他组', '长尾群体']

  // 3. 数据分类
  data.forEach(item => {
    if (item.clusterTag !== -1 && item.clusterTag !== null) {
      seriesData[item.clusterTag].push([
        item.annualIncome,
        item.totalAssets,
        item.custName
      ])
    }
  })

  // 4. 【修复点】构建 Series 列表
  const seriesList = seriesData.map((group, index) => ({
    name: groupNames[index] || `分群 ${index}`,
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
    legend: { bottom: 0, data: seriesList.map(s => s.name) },
    series: seriesList
  }, true)
}

const fetchData = async () => {
  try {
    const scoreRes = await axios.get('/api/analysis/score')
    if (scoreRes.data) {
      modelScore.value = scoreRes.data.sse || 0
      silScore.value = scoreRes.data.sil || 0
      currentModelK.value = scoreRes.data.k || 0
      if (currentModelK.value > 0) clusterK.value = currentModelK.value
      if (scoreRes.data.elbowData) renderElbowChart(scoreRes.data.elbowData)
    }
    const res = await axios.get('/api/analysis/chart-data')
    renderScatterChart(res.data)
    const summaryRes = await axios.get('/api/analysis/summary')
    clusterSummary.value = summaryRes.data || []
  } catch (error) {
    console.error('数据拉取失败', error)
  }
}

const handleRunAnalysis = async () => {
  loading.value = true
  try {
    await axios.post(`/api/analysis/run?k=${clusterK.value}`)
    ElMessage.success(userRole.value === 'MANAGER' ? '决策视图更新成功！' : '模型训练完成！')
    await fetchData()
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  ElMessageBox.confirm('确定清空所有聚类分析结果并重置模型吗？', '系统警告', { type: 'warning' }).then(async () => {
    await axios.post('/api/analysis/reset')
    ElMessage.info('结果已重置')
    fetchData()
  }).catch(() => {})
}

onMounted(() => {
  setPageInfo("客户分群分析")
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (myChart) myChart.dispose()
  if (myElbowChart) myElbowChart.dispose()
})
</script>

<style scoped>
.analysis-page-wrapper { padding: 20px; background-color: #f5f7fa; min-height: calc(100vh - 60px); }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.title { font-weight: bold; font-size: 18px; display: flex; align-items: center; gap: 8px; }
.op-group { display: flex; align-items: center; }
.label { font-size: 13px; color: #606266; margin-right: 8px; }
.score-card { text-align: center; border-top: 4px solid #409EFF; }
.score-tip { font-size: 11px; color: #999; margin-top: 5px; }
.summary-title { font-weight: bold; margin-bottom: 12px; font-size: 15px; color: #303133; }
.evaluation-card { border-top: 3px solid #E6A23C; margin-bottom: 20px; }
.card-title-bold { font-weight: bold; display: flex; align-items: center; gap: 5px; }
.elbow-desc { font-size: 12px; color: #909399; text-align: center; margin-top: 10px; }
.biz-advice { margin-top: 20px; font-size: 12px; color: #7f8c8d; line-height: 1.6; background: #fffdf6; padding: 12px; border: 1px dashed #e6a23c; border-radius: 4px; }
</style>