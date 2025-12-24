<template>
  <div class="user-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>系统用户管理</span>
          <el-button type="primary" icon="Plus" @click="handleOpenDialog('add')">新增账号</el-button>
        </div>
      </template>

      <!-- 表格区域 -->
      <el-table :data="userList" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="登录名" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="role" label="角色">
          <template #default="scope">
            <el-tag :type="getRoleTag(scope.row.role)">{{ scope.row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" @click="handleOpenDialog('edit', scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="loginCount" label="登录频次" width="100" align="center" />
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogType === 'add' ? '新增用户' : '修改用户'" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="登录名">
          <el-input v-model="form.username" :disabled="dialogType === 'edit'" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" placeholder="请选择角色">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="分析师" value="ANALYST" />
            <el-option label="风控员" value="RISK_OFFICER" />
            <el-option label="经理" value="MANAGER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确认保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import axios from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const userList = ref([])
const dialogVisible = ref(false)
const dialogType = ref('add') // 'add' 或 'edit'

const form = reactive({
  id: null,
  username: '',
  password: '',
  nickname: '',
  role: ''
})

// 获取列表
const fetchList = async () => {
  const res = await axios.get('/api/user/list')
  userList.value = res.data
}

// 标签颜色
const getRoleTag = (role) => {
  if (role === 'ADMIN') return 'danger'
  if (role === 'ANALYST') return 'success'
  return 'info'
}

// 打开弹窗
const handleOpenDialog = (type, row = null) => {
  dialogType.value = type
  dialogVisible.value = true
  if (type === 'edit' && row) {
    Object.assign(form, row)
  } else {
    // 重置表单
    form.id = null
    form.username = ''
    form.password = ''
    form.nickname = ''
    form.role = 'ANALYST'
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    if (dialogType.value === 'add') {
      await axios.post('/api/user/add', form)
      ElMessage.success('添加成功')
    } else {
      await axios.put('/api/user/update', form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch (err) {}
}

// 删除用户
const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除该用户吗？', '警告', { type: 'warning' }).then(async () => {
    await axios.delete(`/api/user/delete/${id}`)
    ElMessage.success('删除成功')
    fetchList()
  })
}

onMounted(() => fetchList())
</script>

<style scoped>
.user-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>