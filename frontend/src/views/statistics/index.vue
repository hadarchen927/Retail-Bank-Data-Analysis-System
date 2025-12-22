<template>
  <div class="statistics-container">
    <!-- 1. 高级筛选区 -->
    <el-card class="filter-card">
      <template #header>
        <div class="card-header">
          <span>高级交叉维度筛选（画像 + 行为）</span>
        </div>
      </template>
      <el-form :inline="true" :model="queryForm" size="default">
        <el-form-item label="姓名">
          <el-input v-model="queryForm.custName" placeholder="模糊搜索" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="queryForm.gender" placeholder="全部" clearable style="width: 100px">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="年龄范围">
          <el-input-number v-model="queryForm.ageMin" :min="0" placeholder="最小" style="width: 100px" :controls="false" />
          <span style="margin: 0 5px">-</span>
          <el-input-number v-model="queryForm.ageMax" :min="0" placeholder="最大" style="width: 100px" :controls="false" />
        </el-form-item>
        <el-form-item label="分群标签">
          <el-select v-model="queryForm.clusterTag" placeholder="全部" clearable style="width: 130px">
            <el-option label="高价值客户" :value="0" />
            <el-option label="潜力客户" :value="1" />
            <el-option label="普通客户" :value="2" />
            <el-option label="未分群" :value="-1" />
          </el-select>
        </el-form-item>

        <!-- 新增交易维度筛选 -->
        <el-form-item label="交易笔数 ≥">
          <el-input-number v-model="queryForm.minTransCount" :min="0" style="width: 100px" :controls="false" />
        </el-form-item>
        <el-form-item label="累计流水 ≥">
          <el-input-number v-model="queryForm.minTotalAmount" :min="0" style="width: 130px" :controls="false" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">执行交叉分析</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 2. 交叉统计汇总结果 -->
    <el-row :gutter="20" style="margin-top: 20px" v-if="summary.totalPeople !== undefined">
      <el-col :span="12">
        <el-card shadow="never" style="background: #fdf6ec">
          <el-statistic title="符合条件客户总数" :value="summary.totalPeople" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" style="background: #f0f9eb">
          <el-statistic title="该群体平均流水贡献" :precision="2" :value="summary.avgTransAmount" prefix="￥" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 3. 详细明细列表 -->
    <el-table :data="resultList" border stripe style="width: 100%; margin-top: 20px" height="450">
      <el-table-column prop="cust_name" label="姓名" width="100" />
      <el-table-column prop="gender" label="性别" width="70" />
      <el-table-column prop="age" label="年龄" width="70" />
      <el-table-column prop="annual_income" label="年收入" sortable>
        <template #default="scope">￥{{ scope.row.annual_income?.toLocaleString() }}</template>
      </el-table-column>

      <!-- 行为数据列 -->
      <el-table-column prop="trans_count" label="交易笔数" sortable width="110">
        <template #default="scope">
          <el-tag :type="scope.row.trans_count > 5 ? 'success' : 'info'">{{ scope.row.trans_count }} 次</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="total_trans_amount" label="累计交易金额" sortable>
        <template #default="scope">
          <b style="color: #67C23A">￥{{ scope.row.total_trans_amount?.toLocaleString() || '0' }}</b>
        </template>
      </el-table-column>

      <el-table-column prop="cluster_tag" label="分群状态" width="100">
        <template #default="scope">
          <span v-if="scope.row.cluster_tag === 0">高价值</span>
          <span v-else-if="scope.row.cluster_tag === 1">潜力</span>
          <span v-else-if="scope.row.cluster_tag === 2">普通</span>
          <span v-else>未分析</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import axios from '@/utils/request'
import { ElMessage } from 'element-plus'

// 表单对象需包含后端 DTO 对应的所有字段
const queryForm = reactive({
  custName: '',
  gender: null,
  ageMin: null,
  ageMax: null,
  clusterTag: null,
  minTransCount: null,
  minTotalAmount: null
})

const resultList = ref([])
const summary = ref({})

const handleSearch = async () => {
  try {
    const res = await axios.post('/api/data/search', queryForm)
    // 后端返回的对象包含 list 和 summary
    resultList.value = res.data.list
    summary.value = res.data.summary
  } catch (error) {
    ElMessage.error('查询分析失败')
  }
}

const resetQuery = () => {
  // 重置所有字段为初始值
  queryForm.custName = ''
  queryForm.gender = null
  queryForm.ageMin = null
  queryForm.ageMax = null
  queryForm.clusterTag = null
  queryForm.minTransCount = null
  queryForm.minTotalAmount = null
  handleSearch()
}

// 页面加载时先查一次
onMounted(() => {
  handleSearch()
})
</script>

<style scoped>
.statistics-container { padding: 20px; }
.filter-card { margin-bottom: 20px; border-top: 4px solid #409EFF; }
.card-header { font-weight: bold; }
:deep(.el-statistic__content) { font-size: 24px; color: #409EFF; }
</style>