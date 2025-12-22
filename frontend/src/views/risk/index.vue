<template>
  <div class="risk-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span><el-icon><Warning /></el-icon> 零售银行业务风险监控台</span>
          <el-button type="danger" icon="Radar" @click="handleScan">启动 AI 风险扫描</el-button>
        </div>
      </template>

      <!-- 快捷统计 -->
      <el-row :gutter="20" style="margin-bottom: 20px">
        <el-col :span="6">
          <el-card shadow="never" class="stat-mini high">
            <div class="label">高风险待办</div>
            <div class="value">{{ highRiskCount }} 件</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 预警列表 -->
      <el-table :data="riskList" stripe border style="width: 100%" height="500">
        <el-table-column prop="createTime" label="发现时间" width="160" />
        <el-table-column prop="custName" label="客户姓名" width="100" />
        <el-table-column prop="riskType" label="类型" width="120">
          <template #default="s">
            <el-tag effect="dark" :type="s.row.riskLevel === '高' ? 'danger' : 'warning'">{{ s.row.riskType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="风险情报" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="s">
            <el-tag :type="s.row.status === '待处理' ? 'danger' : 'info'">{{ s.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="决策操作" width="160">
          <template #default="s">
            <template v-if="s.row.status === '待处理'">
              <el-button size="small" type="success" @click="handleAction(s.row.id, '已标记')">标记</el-button>
              <el-button size="small" @click="handleAction(s.row.id, '已忽略')">忽略</el-button>
            </template>
            <span v-else>处理完成</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from '@/utils/request'
import { ElMessage } from 'element-plus'

const riskList = ref([])
const highRiskCount = computed(() => riskList.value.filter(i => i.riskLevel === '高' && i.status === '待处理').length)

const fetchList = async () => {
  const res = await axios.get('/api/risk/list')
  riskList.value = res.data
}

const handleScan = async () => {
  const res = await axios.post('/api/risk/scan')
  ElMessage.success(res.msg)
  fetchList()
}

const handleAction = async (id, action) => {
  await axios.put(`/api/risk/handle/${id}?action=${action}`)
  ElMessage.info('状态已更新')
  fetchList()
}

onMounted(fetchList)
</script>

<style scoped>
.risk-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; font-weight: bold; }
.stat-mini { text-align: center; border-left: 5px solid #F56C6C; }
.stat-mini .label { font-size: 14px; color: #999; }
.stat-mini .value { font-size: 24px; font-weight: bold; color: #F56C6C; }
</style>