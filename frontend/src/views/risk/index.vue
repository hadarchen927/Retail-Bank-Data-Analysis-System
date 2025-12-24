<template>
  <div class="risk-container" v-loading="loading" element-loading-text="全行大数据风险扫描中...">
    <el-card>
      <template #header>
        <div class="card-header">
          <span><el-icon><Warning /></el-icon> 零售银行业务风险监控台</span>
          <div class="op-group">
            <!-- 新增：风控专用导入按钮 -->
            <el-upload
                action="http://localhost:9090/api/data/import-transactions"
                :on-success="handleImportSuccess"
                :show-file-list="false"
                style="display: inline-block; margin-right: 10px;"
            >
              <el-button type="warning" icon="Upload" plain>导入待检流水</el-button>
            </el-upload>

            <el-button type="danger" icon="Radar" @click="handleScan">启动 AI 风险扫描</el-button>
          </div>
        </div>
      </template>


      <!-- 快捷统计卡片 -->
      <el-row :gutter="20" style="margin-bottom: 20px">
        <el-col :span="6">
          <div class="stat-box red">
            <div class="label">待处理高风险</div>
            <div class="number">{{ highRiskCount }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-box blue">
            <div class="label">已处理记录</div>
            <div class="number">{{ processedCount }}</div>
          </div>
        </el-col>
      </el-row>

      <el-table :data="riskList" stripe border style="width: 100%" height="520">
        <el-table-column prop="createTime" label="发现时间" width="160" sortable />
        <el-table-column prop="custName" label="客户姓名" width="100" />
        <el-table-column prop="riskType" label="类型" width="130">
          <template #default="s">
            <el-tag effect="dark" :type="s.row.riskLevel === '高' ? 'danger' : 'warning'">{{ s.row.riskType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="详细风险情报" />
        <el-table-column prop="status" label="当前状态" width="100">
          <template #default="s">
            <el-tag :type="s.row.status === '待处理' ? 'danger' : 'success'">{{ s.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="人工决策" width="150">
          <template #default="s">
            <div v-if="s.row.status === '待处理'">
              <el-button size="small" type="success" @click="handleAction(s.row.id, '已标记')">通过</el-button>
              <el-button size="small" type="info" @click="handleAction(s.row.id, '已忽略')">忽略</el-button>
            </div>
            <span v-else style="color: #999; font-size: 12px;">处理于 {{ s.row.createTime.split(' ')[0] }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { setPageInfo } from '@/utils/setInfo'
import axios from '@/utils/request'
import { ElMessage } from 'element-plus'

const riskList = ref([])
const loading = ref(false)

// 计算属性
const highRiskCount = computed(() => riskList.value.filter(i => i.riskLevel === '高' && i.status === '待处理').length)
const processedCount = computed(() => riskList.value.filter(i => i.status !== '待处理').length)

const handleImportSuccess = () => {
  ElMessage.success('待检流水上传成功！请点击启动扫描进行风险判定。')
}

const fetchList = async () => {
  try {
    const res = await axios.get('/api/risk/list')
    riskList.value = res.data
  } catch (error) {
    console.error(error)
  }
}

const handleScan = async () => {
  loading.value = true
  try {
    const res = await axios.post('/api/risk/scan')
    ElMessage.success(res.msg)
    await fetchList()
  } finally {
    loading.value = false
  }
}

const handleAction = async (id, action) => {
  try {
    await axios.put(`/api/risk/handle/${id}?action=${action}`)
    ElMessage.info(`预警已标记为：${action}`)
    fetchList()
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  setPageInfo("风险预警监控")
  fetchList()
})
</script>

<style scoped>
.risk-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; font-weight: bold; }
.stat-box { padding: 15px; border-radius: 8px; color: #fff; text-align: center; }
.stat-box.red { background: linear-gradient(135deg, #ff7875, #ff4d4f); }
.stat-box.blue { background: linear-gradient(135deg, #69c0ff, #1890ff); }
.stat-box .label { font-size: 12px; opacity: 0.9; }
.stat-box .number { font-size: 24px; font-weight: bold; margin-top: 5px; }
</style>