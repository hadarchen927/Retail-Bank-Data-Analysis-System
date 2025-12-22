<template>
  <div class="data-integration-container">
    <el-row :gutter="20">
      <!-- 左侧：客户基础数据框架 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="title"><el-icon><User /></el-icon> 1. 客户基础数据管理</span>
              <el-button type="danger" plain @click="handleClear">清空系统数据</el-button>
              <el-upload
                  action="http://localhost:9090/api/data/import"
                  :on-success="() => handleSuccess('客户数据')"
                  :show-file-list="false"
              >
                <el-button type="primary" size="small" icon="Upload">导入客户表</el-button>
              </el-upload>

            </div>
          </template>

          <el-table :data="customerList" stripe border height="500px" size="small">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="custName" label="姓名" width="80" />
            <el-table-column prop="gender" label="性别" width="60" />
            <el-table-column prop="annualIncome" label="年收入" width="100">
              <template #default="p">￥{{ p.row.annualIncome?.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="clusterTag" label="状态">
              <template #default="p">
                <el-tag size="small" :type="p.row.clusterTag === -1 ? 'info' : 'success'">
                  {{ p.row.clusterTag === -1 ? '未分群' : '已分群' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右侧：交易流水数据框架 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="title"><el-icon><List /></el-icon> 2. 客户交易记录管理</span>
              <el-upload
                  action="http://localhost:9090/api/data/import-transactions"
                  :on-success="() => handleSuccess('交易流水')"
                  :show-file-list="false"
              >
                <el-button type="success" size="small" icon="Upload">导入流水表</el-button>
              </el-upload>
            </div>
          </template>

          <el-table :data="transactionList" stripe border height="500px" size="small">
            <el-table-column prop="custId" label="所属客户" width="80" />
            <el-table-column prop="type" label="类型" width="70">
              <template #default="p">
                <span :style="{color: p.row.type === '支出' ? '#F56C6C' : '#67C23A'}">{{ p.row.type }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="金额">
              <template #default="p">￥{{ p.row.amount?.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="transTime" label="时间" width="150" />
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

const customerList = ref([])
const transactionList = ref([])

// 同时获取两张表的数据
const fetchData = async () => {
  try {
    const resCust = await axios.get('/api/data/list')
    customerList.value = resCust.data

    const resTrans = await axios.get('/api/data/transaction-list')
    transactionList.value = resTrans.data
  } catch (error) {
    console.error('获取数据失败', error)
  }
}

const handleClear = () => {
  // 1. 弹出确认框
  ElMessageBox.confirm(
      '此操作将永久清空数据库中的所有客户和流水记录，并重置缓存，是否继续？',
      '严正警告',
      {
        confirmButtonText: '确定清空',
        cancelButtonText: '点错了',
        type: 'error',
      }
  )
      .then(async () => {
        // 2. 调用后端清空接口
        try {
          const res = await axios.post('/api/data/clear-all')
          ElMessage.success(res.msg || '系统数据已彻底重置')
          // 3. 刷新页面数据
          fetchData()
        } catch (error) {
          console.error('清空失败', error)
        }
      })
      .catch(() => {
        // 用户取消操作，不执行任何逻辑
      })
}

const handleSuccess = (msg) => {
  ElMessage.success(msg + ' 导入成功！')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.data-integration-container { padding: 20px; background-color: #f0f2f5; min-height: calc(100vh - 100px); }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.title { font-weight: bold; display: flex; align-items: center; gap: 5px; }
</style>