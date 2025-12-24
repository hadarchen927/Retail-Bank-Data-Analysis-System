<template>
  <div class="log-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span><el-icon><Document /></el-icon> 系统操作审计日志</span>
          <div class="button-group">
            <el-button type="primary" size="small" icon="Refresh" @click="fetchLogs">刷新日志</el-button>
            <!-- 新增清空按钮 -->
            <el-button type="danger" size="small" icon="Delete" plain @click="handleClearLogs">清空审计日志</el-button>
          </div>
        </div>
      </template>

      <el-table :data="logList" stripe border style="width: 100%" height="550px">
        <el-table-column prop="id" label="日志ID" width="80" />
        <el-table-column prop="username" label="操作人" width="100">
          <template #default="s">
            <el-tag size="small">{{ s.row.username }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operation" label="执行操作" width="180" />
        <el-table-column prop="method" label="调用方法" show-overflow-tooltip />
        <el-table-column prop="time" label="耗时" width="100">
          <template #default="s">
            <span :style="{color: s.row.time > 1000 ? 'red' : '#67C23A'}">{{ s.row.time }}ms</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from '@/utils/request'
import { setPageInfo } from '@/utils/setInfo'
import { ElMessage, ElMessageBox } from 'element-plus'

const logList = ref([])

const fetchLogs = async () => {
  try {
    const res = await axios.get('/api/user/logs')
    logList.value = res.data

    // 【新增】弹出刷新成功提示
    ElMessage({
      message: '审计日志已同步至最新状态',
      type: 'success',
      duration: 2000 // 提示显示 2 秒
    })
  } catch (error) {
    console.error('刷新日志失败', error)
    ElMessage.error('获取日志数据失败，请检查网络')
  }
}

onMounted(() => {
  setPageInfo("系统审计日志")
  fetchLogs()
})

const handleClearLogs = () => {
  ElMessageBox.confirm(
      '警告：清空日志后将无法追踪历史操作记录，确定执行此操作吗？',
      '系统审计提示',
      {
        confirmButtonText: '确定清空',
        cancelButtonText: '取消',
        type: 'warning',
      }
  ).then(async () => {
    try {
      await axios.delete('/api/user/logs/clear')
      ElMessage.success('系统日志已全部清空')
      logList.value = [] // 清空本地列表展示
    } catch (error) {
      console.error(error)
    }
  }).catch(() => {})
}
</script>

<style scoped>
.log-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>