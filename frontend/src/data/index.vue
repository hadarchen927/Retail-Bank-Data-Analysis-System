<template>
  <div class="data-manage-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>银行数据资产中心</span>
          <div class="button-group">
            <!-- 导入客户 -->
            <el-upload
                action="http://localhost:9090/api/data/import"
                :on-success="() => handleSuccess('客户数据')"
                :show-file-list="false"
                style="display: inline-block; margin-right: 10px;"
            >
              <el-button type="primary" icon="User">导入客户基础表</el-button>
            </el-upload>

            <!-- 导入流水 -->
            <el-upload
                action="http://localhost:9090/api/data/import-transactions"
                :on-success="() => handleSuccess('交易流水')"
                :show-file-list="false"
                style="display: inline-block;"
            >
              <el-button type="success" icon="List">导入交易流水表</el-button>
            </el-upload>
          </div>
        </div>
      </template>

      <!-- 表格区域：必须有这些 el-table-column 才能看到内容 -->
      <el-table :data="customerList" stripe border style="width: 100%" height="550">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="custName" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="70">
          <template #default="scope">
            <el-tag :type="scope.row.gender === '男' ? '' : 'danger'">{{ scope.row.gender }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="70" />
        <el-table-column prop="annualIncome" label="年收入" sortable>
          <template #default="scope">￥{{ scope.row.annualIncome?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="totalAssets" label="总资产" sortable>
          <template #default="scope">￥{{ scope.row.totalAssets?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="loanBalance" label="贷款余额">
          <template #default="scope">￥{{ scope.row.loanBalance?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="clusterTag" label="AI分群状态">
          <template #default="scope">
            <el-tag :type="scope.row.clusterTag === -1 ? 'info' : 'success'">
              {{ scope.row.clusterTag === -1 ? '未分析' : '群体 ' + scope.row.clusterTag }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from '@/utils/request'
import { ElMessage } from 'element-plus'

// 定义响应式变量
const customerList = ref([])

// 获取列表数据的方法
const fetchList = async () => {
  try {
    const res = await axios.get('/api/data/list')
    // 将后端返回的数据赋值给表格
    customerList.value = res.data
  } catch (error) {
    console.error('获取列表失败:', error)
  }
}

// 导入成功后的回调
const handleSuccess = (type) => {
  ElMessage.success(type + ' 导入成功！')
  fetchList() // 刷新列表
}

// 页面加载时自动获取数据
onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.data-manage-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>