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
              <!-- K值选择器：所有人可见，因为经理需要切换看效果 -->
              <span class="label">聚类规模(K):</span>
              <el-input-number v-model="clusterK" :min="2" :max="5" size="small" style="margin-right: 15px" />

              <!-- 场景 A：分析师/管理员按钮 -->
              <template v-if="userRole !== 'MANAGER'">
                <el-button type="primary" icon="VideoPlay" @click="handleRunAnalysis">执行模型训练</el-button>
                <el-button @click="handleReset" plain icon="RefreshLeft">重置结果</el-button>
              </template>

              <!-- 场景 B：经理按钮 -->
              <template v-else>
                <el-button type="success" icon="DataAnalysis" @click="handleRunAnalysis">生成决策建议视图</el-button>
                <el-tag type="info" effect="plain" style="margin-left: 10px">决策探索模式</el-tag>
              </template>
            </div>
          </div>
        </template>

        <!-- 提示语根据角色动态变化 -->
        <el-alert
            :title="userRole === 'MANAGER' ? '经理您好，您可以调整 K 值并点击按钮，系统将实时计算该规模下的客户分布与营销建议。' : '模型管理：调整 K 值以训练最佳的客户分群模型。'"
            :type="userRole === 'MANAGER' ? 'info' : 'success'"
            show-icon
            style="margin-bottom: 20px"
        />

        <!-- 模型指标卡片 (所有人可见) -->
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
              <div class="score-tip">↑ 越接近 1 表示分类越科学</div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="never" class="score-card kvalue">
              <el-statistic title="当前视图规模" :value="currentModelK" />
              <div class="score-tip">个业务目标群体</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 图表与解读并排 -->
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
              <p><b>💡 经理决策参考：</b></p>
              <p>当前分群模型下，建议对 <b>{{ getTopGroup() }}</b> 投入 60% 的营销预算，其转化潜力最高。</p>
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
const userRole = ref(localStorage.getItem('role') || 'GUEST')
const clusterK = ref(3)
const currentModelK = ref(0)
const modelScore = ref(0)
const silScore = ref(0)
const clusterSummary = ref([])
let myChart = null

const loadingText = computed(() => {
  return userRole.value === 'MANAGER' ? '正在为您生成决策分析视图...' : 'AI 算法正在进行模型训练...'
})

// 判定哪个群体最值得关注（取平均收入最高的）
const getTopGroup = () => {
  if (clusterSummary.value.length === 0) return "目标群体"
  const top = [...clusterSummary.value].sort((a,b) => b.avgIncome - a.avgIncome)[0]
  return top.label
}

const handleResize = () => myChart && myChart.resize()

const getTagType = (label) => {
  if (label.includes('顶级')) return 'danger'
  if (label.includes('中坚')) return 'success'
  return 'info'
}

const fetchData = async () => {
  try {
    const scoreRes = await axios.get('/api/analysis/score')
    if (scoreRes.data) {
      modelScore.value = scoreRes.data.sse || 0
      silScore.value = scoreRes.data.sil || 0
      currentModelK.value = scoreRes.data.k || 0
      // 初始化进入时，同步选择器
      if (currentModelK.value > 0) clusterK.value = currentModelK.value
    }
    const res = await axios.get('/api/analysis/chart-data')
    renderScatterChart(res.data)
    const summaryRes = await axios.get('/api/analysis/summary')
    clusterSummary.value = summaryRes.data || []
  } catch (error) {
    console.error(error)
  }
}

const handleRunAnalysis = async () => {
  loading.value = true
  try {
    const res = await axios.post(`/api/analysis/run?k=${clusterK.value}`)
    ElMessage({
      message: userRole.value === 'MANAGER' ? '决策视图更新成功！' : '模型训练完成！',
      type: 'success'
    })
    await fetchData()
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  ElMessageBox.confirm('确定重置所有模型结果吗？', '系统提示').then(async () => {
    await axios.post('/api/analysis/reset')
    fetchData()
  }).catch(() => {})
}

const renderScatterChart = (data) => {
  const chartDom = document.getElementById('scatterChart')
  if (!chartDom || !echarts) return
  if (!myChart) myChart = echarts.init(chartDom)

  const maxTag = Math.max(...data.map(item => item.clusterTag), -1)
  const numGroups = maxTag + 1
  const seriesData = Array.from({ length: Math.max(numGroups, 0) }, () => [])
  const colors = ['#F56C6C', '#409EFF', '#67C23A', '#E6A23C', '#9b59b6']

  data.forEach(item => {
    if (item.clusterTag !== -1 && item.clusterTag !== null) {
      seriesData[item.clusterTag].push([item.annualIncome, item.totalAssets, item.custName])
    }
  })

  const series = seriesData.map((group, index) => ({
    name: `群体 ${index}`,
    type: 'scatter',
    data: group,
    symbolSize: 12,
    itemStyle: { color: colors[index] || '#999' }
  }))

  myChart.setOption({
    title: { text: '客户画像分布空间', left: 'center' },
    tooltip: { formatter: (p) => `姓名: ${p.value[2]}<br/>年收入: ￥${p.value[0].toLocaleString()}` },
    xAxis: { name: '年收入', scale: true },
    yAxis: { name: '总资产', scale: true },
    legend: { bottom: 0 },
    series: series
  }, true)
}

onMounted(() => {
  setPageInfo("客户分群分析")
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (myChart) myChart.dispose()
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
.summary-title { font-weight: bold; margin-bottom: 12px; font-size: 15px; }
.biz-advice { margin-top: 15px; font-size: 12px; color: #7f8c8d; line-height: 1.6; background: #fffdf6; padding: 10px; border: 1px dashed #e6a23c; border-radius: 4px; }
</style>