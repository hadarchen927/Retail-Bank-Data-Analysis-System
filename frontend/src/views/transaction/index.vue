<template>
  <div class="transaction-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>客户交易流水分析</span>
          <el-upload
              action="http://localhost:9090/api/data/import-transactions"
              :on-success="fetchTransactions"
              :show-file-list="false"
          >
            <el-button type="success" icon="Upload">导入交易流水 (.xlsx)</el-button>
          </el-upload>
        </div>
      </template>

      <el-table :data="tableData" border stripe height="550">
        <el-table-column prop="custId" label="客户ID" width="100" />
        <el-table-column prop="type" label="交易类型" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.type === '支出' ? 'danger' : 'success'">{{ scope.row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="交易金额">
          <template #default="scope">￥{{ scope.row.amount?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="transTime" label="交易时间" width="180" />
        <el-table-column prop="location" label="地点" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from '@/utils/request'
import { ElMessage } from 'element-plus'

const tableData = ref([])

const fetchTransactions = async () => {
  const res = await axios.get('/api/data/transaction-list')
  tableData.value = res.data
}

onMounted(fetchTransactions)
</script>