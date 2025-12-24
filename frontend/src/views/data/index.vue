<template>
  <!-- 为整个容器增加加载遮罩，当导入或重置时触发 -->
  <div class="data-integration-container" v-loading="globalLoading" element-loading-text="系统正在处理大规模金融数据，请稍候...">

    <!-- 顶部操作区 -->
    <div class="top-bar">
      <div class="page-title">
        <el-icon><DataAnalysis /></el-icon> 零售银行数据集成中心
      </div>
      <div class="global-actions">
        <el-button type="danger" icon="Delete" plain @click="handleClearAll">清空系统数据 (重置ID)</el-button>
        <!-- 增加按钮加载状态 -->
        <el-button type="info" :loading="refreshLoading" icon="Refresh" @click="manualRefresh">刷新列表</el-button>
      </div>
    </div>

    <el-row :gutter="15">
      <!-- 左侧：客户基础信息框架 -->
      <el-col :span="13">
        <el-card shadow="hover" class="data-card" v-loading="customerLoading">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon><User /></el-icon> 1. 客户基础数据管理 (含自动化清洗)</span>
              <el-upload
                  action="http://localhost:9090/api/data/import"
                  :on-success="handleCustomerSuccess"
                  :before-upload="beforeCustomerUpload"
                  :show-file-list="false"
              >
                <el-button type="primary" size="small" icon="Upload">导入客户表</el-button>
              </el-upload>
            </div>
          </template>

          <el-table :data="customerList" stripe border height="580px" size="small">
            <el-table-column prop="id" label="ID" width="55" align="center" />
            <el-table-column prop="custName" label="姓名" width="80" />
            <el-table-column prop="gender" label="性别" width="50" align="center" />
            <el-table-column prop="age" label="年龄" width="50" align="center" />
            <el-table-column prop="annualIncome" label="年收入" width="110">
              <template #default="p">￥{{ p.row.annualIncome?.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="totalAssets" label="总资产" width="110">
              <template #default="p">￥{{ p.row.totalAssets?.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="loanBalance" label="贷款余额" width="100">
              <template #default="p">￥{{ p.row.loanBalance?.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="clusterTag" label="状态" align="center">
              <template #default="p">
                <el-tag size="small" :type="p.row.clusterTag === -1 ? 'info' : 'success'">
                  {{ p.row.clusterTag === -1 ? '未分群' : '群' + p.row.clusterTag }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="creditScore" label="信用分" width="70" align="center" />
            <el-table-column prop="overdueDays" label="逾期" width="60" align="center">
              <template #default="p">
                <span :style="{color: p.row.overdueDays > 0 ? 'red' : '#67C23A'}">{{ p.row.overdueDays }}天</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右侧：交易流水明细框架 -->
      <el-col :span="11">
        <el-card shadow="hover" class="data-card" v-loading="transactionLoading">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon><List /></el-icon> 2. 客户交易记录管理</span>
              <el-upload
                  action="http://localhost:9090/api/data/import-transactions"
                  :on-success="handleTransSuccess"
                  :before-upload="beforeTransUpload"
                  :show-file-list="false"
              >
                <el-button type="success" size="small" icon="Upload">导入流水表</el-button>
              </el-upload>
            </div>
          </template>

          <el-table :data="transactionList" stripe border height="580px" size="small">
            <el-table-column prop="custId" label="客户ID" width="70" align="center" />
            <el-table-column prop="type" label="类型" width="70" align="center">
              <template #default="p">
                <span :style="{ color: p.row.type === '支出' ? '#F56C6C' : '#67C23A', fontWeight: 'bold' }">{{ p.row.type }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="交易金额" width="90">
              <template #default="p">￥{{ p.row.amount?.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="transTime" label="交易时间" width="140" />
            <el-table-column prop="location" label="地点" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from '@/utils/request'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { setPageInfo } from '@/utils/setInfo'

// --- 状态控制变量 ---
const globalLoading = ref(false)
const refreshLoading = ref(false)
const customerLoading = ref(false)
const transactionLoading = ref(false)

const customerList = ref([])
const transactionList = ref([])

// --- 数据获取方法 ---
const fetchAllData = async () => {
  try {
    const resCust = await axios.get('/api/data/list')
    customerList.value = resCust.data
    const resTrans = await axios.get('/api/data/transaction-list')
    transactionList.value = resTrans.data
  } catch (error) {
    console.error('获取数据失败', error)
  }
}

// 1. 手动刷新按钮交互
const manualRefresh = async () => {
  refreshLoading.value = true
  await fetchAllData()
  setTimeout(() => {
    refreshLoading.value = false
    ElMessage.success('数据列表已同步至最新')
  }, 500)
}

// 2. 客户数据导入交互
const beforeCustomerUpload = () => {
  customerLoading.value = true
  return true
}
const handleCustomerSuccess = (response) => {
  customerLoading.value = false
  // 使用 Notification 显示更专业的清洗报告
  ElNotification({
    title: '客户数据集成成功',
    message: response.data, // 显示后端返回的统计字符串
    type: 'success',
    duration: 5000
  })
  fetchAllData()
}

// 3. 流水数据导入交互
const beforeTransUpload = () => {
  transactionLoading.value = true
  return true
}
const handleTransSuccess = (response) => {
  transactionLoading.value = false
  ElNotification({
    title: '交易流水集成成功',
    message: response.data,
    type: 'success',
    duration: 5000
  })
  fetchAllData()
}

// 4. 清空数据交互
const handleClearAll = () => {
  ElMessageBox.confirm('该操作将物理删除全表并重置计数器，确定执行？', '高危操作提示', {
    confirmButtonText: '确定重置',
    cancelButtonText: '取消',
    type: 'error',
  }).then(async () => {
    globalLoading.value = true
    try {
      await axios.post('/api/data/clear-all')
      ElMessage.success('系统数据已全部重置')
      await fetchAllData()
    } finally {
      globalLoading.value = false
    }
  }).catch(() => {})
}

onMounted(() => {
  setPageInfo("数据集成中心")
  fetchAllData()
})
</script>

<style scoped>
.data-integration-container { padding: 15px; background-color: #f0f2f5; min-height: calc(100vh - 60px); }
.top-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.page-title { font-size: 18px; font-weight: bold; color: #303133; display: flex; align-items: center; gap: 8px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-weight: bold; font-size: 14px; display: flex; align-items: center; gap: 5px; }
.data-card { border-radius: 4px; }
</style>