<template>
  <div class="data-integration-container">
    <!-- 顶部操作区 -->
    <div class="top-bar">
      <div class="page-title">
        <el-icon><DataAnalysis /></el-icon> 零售银行数据集成中心
      </div>
      <div class="global-actions">
        <el-button type="danger" icon="Delete" plain @click="handleClearAll">清空所有业务数据</el-button>
        <el-button type="info" icon="Refresh" @click="fetchAllData">刷新页面</el-button>
      </div>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：客户基础信息框架 -->
      <el-col :span="12">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon><User /></el-icon> 客户基础信息库</span>
              <el-upload
                  action="http://localhost:9090/api/data/import"
                  :on-success="() => handleSuccess('客户基础数据')"
                  :show-file-list="false"
              >
                <el-button type="primary" size="small" icon="Upload">导入客户 Excel</el-button>
              </el-upload>
            </div>
          </template>

          <el-table :data="customerList" stripe border height="550px" size="small">
            <el-table-column prop="id" label="ID" width="60" align="center" />
            <el-table-column prop="custName" label="姓名" width="90" />
            <el-table-column prop="gender" label="性别" width="60" align="center">
              <template #default="scope">
                <span :style="{ color: scope.row.gender === '男' ? '#409EFF' : '#F56C6C' }">{{ scope.row.gender }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="age" label="年龄" width="60" align="center" />
            <el-table-column prop="annualIncome" label="年收入" width="100">
              <template #default="p">￥{{ p.row.annualIncome?.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="totalAssets" label="总资产" width="100">
              <template #default="p">￥{{ p.row.totalAssets?.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="clusterTag" label="分群" width="80" align="center">
              <template #default="p">
                <el-tag size="small" :type="p.row.clusterTag === -1 ? 'info' : 'success'">
                  {{ p.row.clusterTag === -1 ? '未分析' : '群体' + p.row.clusterTag }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右侧：交易流水明细框架 -->
      <el-col :span="12">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon><List /></el-icon> 客户交易流水库</span>
              <el-upload
                  action="http://localhost:9090/api/data/import-transactions"
                  :on-success="() => handleSuccess('交易流水数据')"
                  :show-file-list="false"
              >
                <el-button type="success" size="small" icon="Upload">导入流水 Excel</el-button>
              </el-upload>
            </div>
          </template>

          <el-table :data="transactionList" stripe border height="550px" size="small">
            <el-table-column prop="custId" label="客户ID" width="70" align="center" />
            <el-table-column prop="type" label="交易类型" width="80" align="center">
              <template #default="p">
                <el-tag size="small" :type="p.row.type === '支出' ? 'danger' : 'success'">{{ p.row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="交易金额" width="100">
              <template #default="p">￥{{ p.row.amount?.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="transTime" label="交易时间" width="160" />
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

// 获取所有数据
const fetchAllData = async () => {
  try {
    // 同时获取客户列表和流水列表
    const resCust = await axios.get('/api/data/list')
    customerList.value = resCust.data

    const resTrans = await axios.get('/api/data/transaction-list')
    transactionList.value = resTrans.data
  } catch (error) {
    console.error('获取数据失败:', error)
  }
}

// 导入成功回调
const handleSuccess = (type) => {
  ElMessage.success(type + ' 导入成功！')
  fetchAllData()
}

// 一键清空系统数据
const handleClearAll = () => {
  ElMessageBox.confirm(
      '警告：此操作将永久清空数据库中所有客户、流水及预警记录，并重置自增ID，是否继续？',
      '系统重置确认',
      {
        confirmButtonText: '确认清空',
        cancelButtonText: '取消',
        type: 'error',
      }
  ).then(async () => {
    try {
      const res = await axios.post('/api/data/clear-all')
      ElMessage.success(res.msg || '数据已全部重置')
      fetchAllData()
    } catch (error) {
      console.error(error)
    }
  }).catch(() => {})
}

onMounted(() => {
  setPageInfo("数据集成中心")
  fetchAllData()
})
</script>

<style scoped>
.data-integration-container { padding: 20px; background-color: #f0f2f5; min-height: calc(100vh - 60px); }
.top-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title { font-size: 20px; font-weight: bold; color: #303133; display: flex; align-items: center; gap: 10px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-weight: bold; display: flex; align-items: center; gap: 5px; }
.data-card { border-radius: 8px; }
</style>