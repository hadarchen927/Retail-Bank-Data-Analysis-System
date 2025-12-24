<template>
  <div class="login-container">
    <el-card class="login-card">
      <div class="title-area">
        <el-icon size="30" color="#409EFF"><TrendCharts /></el-icon>
        <h2>RBDAS 银行分析系统</h2>
      </div>

      <!-- 切换标签 -->
      <div class="tab-menu">
        <span :class="{ active: isLogin }" @click="isLogin = true">账号登录</span>
        <span :class="{ active: !isLogin }" @click="isLogin = false">新用户注册</span>
      </div>

      <!-- 登录/注册表单 -->
      <el-form :model="form" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>

        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
        </el-form-item>

        <!-- 注册特有字段：昵称 -->
        <el-form-item v-if="!isLogin" label="显示昵称">
          <el-input v-model="form.nickname" placeholder="大家怎么称呼您" prefix-icon="Edit" />
        </el-form-item>

        <el-form-item v-if="!isLogin" label="选择身份">
          <el-select v-model="form.role" placeholder="请选择您的岗位身份" style="width: 100%">
            <el-option label="业务分析师" value="ANALYST" />
            <el-option label="风险控制员" value="RISK_OFFICER" />
            <el-option label="市场经理" value="MANAGER" />
          </el-select>
        </el-form-item>

        <div style="margin-top: 20px">
          <el-button v-if="isLogin" type="primary" style="width: 100%" :loading="loading" @click="handleLogin">立即登录</el-button>
          <el-button v-else type="success" style="width: 100%" :loading="loading" @click="handleRegister">提交注册</el-button>
        </div>
      </el-form>


    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from '@/utils/request'

const router = useRouter()
const isLogin = ref(true) // 控制是登录还是注册
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  role: 'ANALYST' // 默认值
})

// 登录逻辑
const handleLogin = async () => {
  if (!form.username || !form.password) return ElMessage.warning('请填写完整')
  loading.value = true
  try {
    const res = await axios.post('/api/auth/login', form)
    // 只有成功了才执行下面的逻辑
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('role', res.data.role)
    localStorage.setItem('nickname', res.data.nickname)
    localStorage.setItem('username', res.data.username)
    ElMessage.success('欢迎回来，' + res.data.nickname)
    router.push('/dashboard')
  } catch (error) {
    // 【关键修复】捕获后端的“用户名或密码错误”异常，不再弹出红色错误层
    console.error('登录失败', error)
  } finally {
    loading.value = false
  }
}

// 注册逻辑
const handleRegister = async () => {
  if (!form.username || !form.password || !form.nickname) return ElMessage.warning('请填写完整信息')
  loading.value = true
  try {
    await axios.post('/api/auth/register', form) // 直接传整个 form，包含 role
    ElMessage.success('注册成功！已切换到登录')
    isLogin.value = true
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #1d2b64 0%, #f8cdda 100%);
}
.login-card {
  width: 420px;
  padding: 20px;
  border-radius: 15px;
}
.title-area {
  text-align: center;
  margin-bottom: 30px;
}
.title-area h2 {
  margin-top: 10px;
  color: #303133;
}
.tab-menu {
  display: flex;
  justify-content: space-around;
  margin-bottom: 25px;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 10px;
}
.tab-menu span {
  cursor: pointer;
  color: #909399;
  font-size: 16px;
  transition: 0.3s;
}
.tab-menu span.active {
  color: #409EFF;
  font-weight: bold;
  border-bottom: 2px solid #409EFF;
  padding-bottom: 8px;
}
.tips {
  margin-top: 20px;
  text-align: center;
  font-size: 12px;
  color: #909399;
}
</style>