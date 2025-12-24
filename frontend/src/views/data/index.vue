<template>
  <div class="data-integration-container">
    <!-- 顶部操作区 -->
    <div class="top-bar">
      <div class="page-title">
        <el-icon><DataAnalysis /></el-icon> 零售银行数据集成中心
      </div>
      <div class="global-actions">
        <el-button type="danger" icon="Delete" plain @click="handleClearAll">清空系统数据 (重置ID)</el-button>
        <el-button type="info" icon="Refresh" @click="fetchAllData">刷新列表</el-button>
        <el-button type="warning" icon="Download" @click="handleExport">导出分析结果 (Excel)</el-button>
      </div>
    </div>

    <el-row :gutter="15">
      <!-- 左侧：客户基础信息框架 (共计 8 列) -->
      <el-col :span="13">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon><User /></el-icon> 1. 客户基础数据管理</span>
              <el-upload
                  action="http://localhost:9090/api/data/import"
                  :on-success="() => handleSuccess('客户基础数据')"
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
            <!-- 补全缺失列：总资产 -->
            <el-table-column prop="totalAssets" label="总资产" width="110">
              <template #default="p">￥{{ p.row.totalAssets?.toLocaleString() }}</template>
            </el-table-column>
            <!-- 补全缺失列：贷款余额 -->
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
          </el-table>
        </el-card>
      </el-col>

      <!-- 右侧：交易流水明细框架 -->
      <el-col :span="11">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon><List /></el-icon> 2. 客户交易记录管理</span>
              <el-upload
                  action="http://localhost:9090/api/data/import-transactions"
                  :on-success="() => handleSuccess('交易流水数据')"
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { setPageInfo } from '@/utils/setInfo'

const customerList = ref([])
const transactionList = ref([])

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

const handleSuccess = (type) => {
  ElMessage.success(type + ' 导入成功！')
  fetchAllData()
}

const handleClearAll = () => {
  ElMessageBox.confirm('警告：此操作将清空所有数据并重置自增ID为1，是否继续？', '风险提示', {
    confirmButtonText: '确定重置',
    cancelButtonText: '取消',
    type: 'error',
  }).then(async () => {
    await axios.post('/api/data/clear-all')
    ElMessage.success('系统已重置')
    fetchAllData()
  }).catch(() => {})
}

onMounted(() => {
  setPageInfo("数据集成中心")
  fetchAllData()
})

const handleExport = () => {
  // 直接通过浏览器下载
  window.open('http://localhost:9090/api/data/export', '_blank');
  ElMessage.success('正在准备导出文件...');
}
</script>

<style scoped>
.data-integration-container { padding: 15px; background-color: #f0f2f5; min-height: calc(100vh - 60px); }
.top-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.page-title { font-size: 18px; font-weight: bold; color: #303133; display: flex; align-items: center; gap: 8px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-weight: bold; font-size: 14px; display: flex; align-items: center; gap: 5px; }
.data-card { border-radius: 4px; }
</style>