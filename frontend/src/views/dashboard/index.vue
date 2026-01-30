<template>
  <div class="dashboard-container">
    <!-- 顶部操作栏 -->
    <div class="action-bar">
      <el-button type="success" icon="Download" @click="handleDownloadReport">
        导出标准化分析报告 (PDF)
      </el-button>
      <el-button type="primary" icon="Document" @click="handleDownloadWord">
        导出分析报告 (Word)
      </el-button>
    </div>

    <!-- 第一行：核心指标卡片 -->
    <el-row :gutter="20">
      <el-col :span="8" v-for="(item, index) in cardConfigs" :key="index">
        <el-card shadow="hover" class="stat-card">
          <div class="card-header">{{ item.title }}</div>
          <div class="card-value">{{ item.prefix }}{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二行：系统公告 + 理财产品排行 (新增营销分析) -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="10">
        <el-card shadow="hover" style="height: 400px;">
          <template #header>
            <div class="card-title-bold"><el-icon><Bell /></el-icon> 系统公告与动态</div>
          </template>
          <el-timeline>
            <el-timeline-item timestamp="2025-12-23" type="primary" hollow>
              系统聚类模型已更新，当前轮廓系数提升至 0.65
            </el-timeline-item>
            <el-timeline-item timestamp="2025-12-22">
              完成 12 月份第二周交易流水数据集成
            </el-timeline-item>
            <el-timeline-item timestamp="2025-12-20" type="warning">
              风险预警规则库新增“高负债异常”判定逻辑
            </el-timeline-item>
            <el-timeline-item timestamp="2025-12-15">
              系统正式上线，开启数字化零售银行分析新篇章
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="hover" style="height: 400px;">
          <template #header>
            <div class="card-title-bold"><el-icon><DataLine /></el-icon> 业务板块规模排行 (营销转化分析)</div>
          </template>
          <div id="barChart" style="width: 100%; height: 320px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第三行：交易类型分布 + 客户性别分布 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><div class="card-title-bold">交易类型占比分析</div></template>
          <div id="pieChart" style="width: 100%; height: 350px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><div class="card-title-bold">客户性别分布 (全行)</div></template>
          <div id="genderChart" style="width: 100%; height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第四行：全屏趋势图 -->
    <el-row style="margin-top: 20px;">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header><div class="card-title-bold">全行交易金额趋势分析 (近 6 个月)</div></template>
          <div id="trendChart" style="width: 100%; height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from '@/utils/request'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'

// --- 响应式数据 ---
const rawData = ref({
  stats: {},
  chartData: [],
  genderData: [],
  trendData: [],
  productData: [] // 新增
})

// --- 计算属性：顶部指标 ---
const cardConfigs = computed(() => [
  { title: '管理客户总数', value: rawData.value.stats.totalCustomers || 0, prefix: '' },
  { title: '全行总资产 (AUM)', value: (rawData.value.stats.totalAssets || 0).toLocaleString(), prefix: '￥' },
  { title: '全行信贷余额', value: (rawData.value.stats.totalLoans || 0).toLocaleString(), prefix: '￥' }
])

// --- 方法：PDF导出 ---
const handleDownloadReport = () => {
  const url = 'http://localhost:9090/api/report/export'
  window.open(url, '_blank')
  ElMessage.success('报告正在生成并下载...')
}

// --- 图表渲染逻辑 ---

// 1. 交易类型饼图
const initPieChart = (data) => {
  const myChart = echarts.init(document.getElementById('pieChart'))
  myChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      name: '笔数',
      type: 'pie',
      radius: '55%',
      data: data,
      emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.5)' } }
    }]
  })
}

// 2. 性别分布环形图
const initGenderChart = (data) => {
  const myChart = echarts.init(document.getElementById('genderChart'))
  myChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: '0', left: 'center' },
    color: ['#409EFF', '#F56C6C'],
    series: [{
      name: '人数',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      data: data
    }]
  })
}

// 3. 业务排行柱状图 (满足市场经理需求)
const initBarChart = (data) => {
  const myChart = echarts.init(document.getElementById('barChart'))
  myChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map(i => i.name) },
    yAxis: { type: 'value', name: '金额(元)' },
    series: [{
      data: data.map(i => i.value),
      type: 'bar',
      showBackground: true,
      backgroundStyle: { color: 'rgba(180, 180, 180, 0.2)' },
      itemStyle: { color: '#67C23A' }
    }]
  })
}

// 4. 趋势折线图
const initTrendChart = (data) => {
  const myChart = echarts.init(document.getElementById('trendChart'))
  myChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map(i => i.month), boundaryGap: false },
    yAxis: { type: 'value' },
    series: [{
      data: data.map(i => i.total),
      type: 'line',
      smooth: true,
      areaStyle: { color: 'rgba(64, 158, 255, 0.1)' },
      itemStyle: { color: '#409EFF' }
    }]
  })
}

// --- 数据加载 ---
const loadData = async () => {
  try {
    const res = await axios.get('/api/dashboard/all')
    if (res.data) {
      rawData.value = res.data
      // 初始化所有图表
      initPieChart(res.data.chartData || [])
      initGenderChart(res.data.genderData || [])
      initTrendChart(res.data.trendData || [])
      initBarChart(res.data.productData || []) // 这里的 productData 需要后端同步返回
    }
  } catch (error) {
    console.error('数据加载失败', error)
  }
}

// --- 初始化生命周期 ---
onMounted(() => {
  // 修改标签页信息
  document.title = "业务控制台 - 零售银行分析系统"
  const link = document.querySelector("link[rel*='icon']") || document.createElement('link')
  link.type = 'image/png'
  link.rel = 'shortcut icon'
  link.href = '/icon.png?v=' + new Date().getTime()
  document.head.appendChild(link)

  // 载入业务数据
  loadData()

  // 窗口自适应
  window.onresize = () => {
    const charts = ['pieChart', 'genderChart', 'barChart', 'trendChart']
    charts.forEach(id => {
      const dom = document.getElementById(id)
      if (dom) echarts.getInstanceByDom(dom)?.resize()
    })
  }
})

const handleDownloadWord = () => {
  const url = 'http://localhost:9090/api/report/export-word'
  window.open(url, '_blank')
  ElMessage.success('Word报告正在生成并下载...')
}

</script>

<style scoped>
.dashboard-container { padding: 20px; background: #f5f7fa; min-height: 100vh; }
.action-bar { display: flex; justify-content: flex-end; margin-bottom: 15px; }
.stat-card { text-align: center; border-radius: 8px; border-top: 4px solid #409EFF; }
.card-header { color: #909399; font-size: 14px; margin-bottom: 10px; }
.card-value { font-size: 26px; font-weight: bold; color: #303133; }
.card-title-bold { font-weight: bold; color: #303133; display: flex; align-items: center; gap: 8px; }
</style>