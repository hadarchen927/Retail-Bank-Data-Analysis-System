<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2>零售银行数据分析系统</h2>
      <el-form :model="loginForm">
        <el-form-item>
          <el-input v-model="loginForm.username" placeholder="用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-button type="primary" style="width: 100%" @click="handleLogin">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from '@/utils/request'

const router = useRouter()
const loginForm = reactive({
  username: '',
  password: ''
})

const handleLogin = async () => {
  try {
    const res = await axios.post('/api/auth/login', loginForm)

    // 保存关键信息
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('role', res.data.role)         // 存储角色
    localStorage.setItem('nickname', res.data.nickname) // 存储昵称

    ElMessage.success('登录成功，欢迎 ' + res.data.nickname)
    router.push('/dashboard')
  } catch (error) {
    console.error(error)
  }
}

</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f7fa;
}
.login-card {
  width: 400px;
  text-align: center;
}
</style>