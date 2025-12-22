<template>
  <div style="display: flex; justify-content: flex-end; margin-bottom: 10px;">
    <el-button type="success" icon="Download" @click="handleDownloadReport">导出标准化分析报告 (PDF)</el-button>
  </div>
  <div class="dashboard-container">
    <!-- 顶部卡片 -->
    <el-row :gutter="20">
      <el-col :span="8" v-for="(item, index) in cardConfigs" :key="index">
        <el-card shadow="hover" class="stat-card">
          <div class="card-header">{{ item.title }}</div>
          <div class="card-value">{{ item.prefix }}{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <!-- 修改后的图表区 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 左侧：交易类型分布 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>交易类型分布分析</template>
          <div id="pieChart" style="width: 100%; height: 350px;"></div>
        </el-card>
      </el-col>

      <!-- 右侧：客户性别比例 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>客户性别分布</template>
          <div id="genderChart" style="width: 100%; height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>
    <el-row style="margin-top: 20px;">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>全行交易金额趋势分析（近 6 个月）</template>
          <div id="trendChart" style="width: 100%; height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
const handleDownloadReport = () => {
  // 注意：下载文件不能直接用 axios.get，最简单的办法是直接用 window.open
  // 或者创建一个隐藏的 <a> 标签
  const url = 'http://localhost:9090/api/report/export';
  window.open(url, '_blank');
}

import { ref, onMounted, computed } from 'vue'
import axios from '@/utils/request'
import * as echarts from 'echarts'

const rawData = ref({ stats: {}, chartData: [], genderData: [] })

// 格式化卡片显示
const cardConfigs = computed(() => [
  { title: '管理客户总数', value: rawData.value.stats.totalCustomers || 0, prefix: '' },
  { title: '银行总资产', value: (rawData.value.stats.totalAssets || 0).toLocaleString(), prefix: '￥' },
  { title: '信贷余额总计', value: (rawData.value.stats.totalLoans || 0).toLocaleString(), prefix: '￥' }
])

const initChart = (data) => {
  const chartDom = document.getElementById('pieChart');
  const myChart = echarts.init(chartDom);
  const option = {
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [
      {
        name: '交易笔数',
        type: 'pie',
        radius: '50%',
        data: data, // 后端传来的 [{name: '存入', value: 10}, ...]
        emphasis: {
          itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' }
        }
      }
    ]
  };
  myChart.setOption(option);
}

const initGenderChart = (data) => {
  const chartDom = document.getElementById('genderChart');
  const myChart = echarts.init(chartDom);
  const option = {
    tooltip: { trigger: 'item' },
    legend: { bottom: '5%', left: 'center' },
    color: ['#409EFF', '#F56C6C'], // 蓝色代表男，粉色代表女
    series: [
      {
        name: '性别统计',
        type: 'pie',
        radius: ['40%', '70%'], // 环形图效果，更显高端
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: false, position: 'center' },
        emphasis: {
          label: { show: true, fontSize: '20', fontWeight: 'bold' }
        },
        data: data // 后端传来的 [{name: '男', value: 50}, {name: '女', value: 30}]
      }
    ]
  };
  myChart.setOption(option);
}

const initTrendChart = (data) => {
  const chartDom = document.getElementById('trendChart');
  const myChart = echarts.init(chartDom);

  // 提取月份和金额
  const months = data.map(item => item.month)
  const totals = data.map(item => item.total)

  const option = {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: months, boundaryGap: false },
    yAxis: { type: 'value', name: '金额 (元)' },
    series: [{
      data: totals,
      type: 'line',
      smooth: true, // 平滑曲线
      areaStyle: { color: 'rgba(64, 158, 255, 0.2)' }, // 阴影区域
      itemStyle: { color: '#409EFF' }
    }]
  };
  myChart.setOption(option);
}

const loadData = async () => {
  const res = await axios.get('/api/dashboard/all')
  if (res.data) {
    rawData.value = res.data
    initChart(res.data.chartData)
    initGenderChart(res.data.genderData)
    // 【关键】渲染趋势图
    initTrendChart(res.data.trendData || [])
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.dashboard-container { padding: 20px; background: #f5f7fa; min-height: 100vh; }
.stat-card { text-align: center; border-radius: 8px; }
.card-header { color: #909399; font-size: 14px; margin-bottom: 10px; }
.card-value { font-size: 28px; font-weight: bold; color: #409EFF; }
</style>