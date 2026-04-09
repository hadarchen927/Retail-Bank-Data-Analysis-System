<template>
  <div class="risk-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span><el-icon><Warning /></el-icon> 零售银行业务风险监控台</span>
          <div class="op-group">
            <!-- 启动扫描按钮，加入扫描中的 Loading 状态 -->
            <el-button type="danger" icon="Radar" @click="handleScan" :loading="scanLoading">
              {{ scanLoading ? 'AI 风险扫描中...' : '启动 AI 风险扫描' }}
            </el-button>
          </div>
        </div>
      </template>

      <!-- 快捷统计卡片 -->
      <el-row :gutter="20" style="margin-bottom: 20px">
        <el-col :span="6">
          <div class="stat-box red">
            <div class="label">待处理风险</div>
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

      <!-- 数据表格，加入表格专属的 v-loading 状态 -->
      <el-table
          :data="riskList"
          stripe
          border
          style="width: 100%"
          height="520"
          v-loading="tableLoading"
          element-loading-text="正在加载风险预警数据..."
      >
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

// 数据状态
const riskList = ref([])

// 精细化 Loading 状态管理
const tableLoading = ref(false)   // 控制表格加载
const scanLoading = ref(false)    // 控制扫描按钮
const importLoading = ref(false)  // 控制导入按钮

// 计算属性
const highRiskCount = computed(() => riskList.value.filter(i => i.status === '待处理').length)
const processedCount = computed(() => riskList.value.filter(i => i.status !== '待处理').length)

// --- 文件上传生命周期 ---
const handleBeforeUpload = () => {
  importLoading.value = true // 上传开始前，开启按钮 loading
  return true
}

const handleImportSuccess = () => {
  importLoading.value = false // 上传成功，关闭 loading
  ElMessage.success('待检流水上传成功！请点击启动扫描进行风险判定。')
}

const handleImportError = () => {
  importLoading.value = false // 上传失败，关闭 loading
  ElMessage.error('文件导入失败，请检查网络或后端服务。')
}

// --- 接口调用 ---
const fetchList = async () => {
  tableLoading.value = true // 开启表格 loading
  try {
    const res = await axios.get('/api/risk/list')
    riskList.value = res.data
  } catch (error) {
    console.error('获取风险列表失败:', error)
    ElMessage.error('获取数据失败')
  } finally {
    tableLoading.value = false // 无论成功失败，关闭表格 loading
  }
}

const handleScan = async () => {
  scanLoading.value = true // 开启扫描按钮 loading
  try {
    const res = await axios.post('/api/risk/scan')
    ElMessage.success(res.msg || '扫描完成')
    await fetchList() // 扫描完成后自动刷新列表
  } catch (error) {
    console.error('风险扫描失败:', error)
  } finally {
    scanLoading.value = false // 关闭扫描按钮 loading
  }
}

const handleAction = async (id, action) => {
  try {
    await axios.put(`/api/risk/handle/${id}?action=${action}`)
    ElMessage.info(`预警已标记为：${action}`)
    fetchList() // 状态改变后静默刷新列表
  } catch (error) {
    console.error('状态更新失败:', error)
  }
}

// 初始化
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