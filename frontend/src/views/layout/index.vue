<template>
  <el-container class="layout-container">
    <!-- 左侧菜单栏 -->
    <el-aside width="220px">
      <div class="logo">
        <el-icon style="margin-right: 10px"><TrendCharts /></el-icon>
        RBDAS 银行分析
      </div>
      <el-menu
          :default-active="$route.path"
          router
          class="el-menu-vertical"
          background-color="#304156"
          text-color="#fff"
          active-text-color="#409EFF"
      >
        <!-- 1. 业务控制台：所有人可见 -->
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>业务控制台</span>
        </el-menu-item>

        <!-- 2. 数据导入管理：仅管理员和分析师可见 -->
        <el-menu-item index="/data" v-if="userRole === 'ADMIN' || userRole === 'ANALYST'">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据导入管理</span>
        </el-menu-item>

        <!-- 3. 客户分群分析：仅管理员和分析师可见 (这是我们下一步要做的) -->
        <el-menu-item index="/analysis" v-if="userRole === 'ADMIN' || userRole === 'ANALYST'">
          <el-icon><PieChart /></el-icon>
          <span>客户分群分析</span>
        </el-menu-item>

        <!-- 4. 风险监控：仅管理员和风控员可见 -->
        <el-menu-item index="/risk" v-if="userRole === 'ADMIN' || userRole === 'RISK_OFFICER'">
          <el-icon><WarnTriangleFilled /></el-icon>
          <span>风险预警监控</span>
        </el-menu-item>

        <!-- 5. 系统账户管理：仅管理员可见 -->
        <el-menu-item index="/user" v-if="userRole === 'ADMIN'">
          <el-icon><UserFilled /></el-icon>
          <span>系统账户管理</span>
        </el-menu-item>

        <el-menu-item @click="handleLogout" class="logout-item">
          <el-icon><SwitchButton /></el-icon>
          <span>退出系统</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 右侧内容区 -->
    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>零售银行系统</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.name }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tag effect="dark" size="small" style="margin-right: 10px">{{ getRoleName(userRole) }}</el-tag>
          <span class="user-name">欢迎您，{{ nickname }}</span>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'

const router = useRouter()

// 从本地存储获取用户信息
const nickname = ref(localStorage.getItem('nickname') || '未登录')
const userRole = ref(localStorage.getItem('role') || 'GUEST')

// 角色转中文显示
const getRoleName = (role) => {
  const map = {
    'ADMIN': '系统管理员',
    'ANALYST': '业务分析师',
    'RISK_OFFICER': '风险控制员',
    'MANAGER': '市场部经理'
  }
  return map[role] || '普通用户'
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出系统吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
      .then(() => {
        // 点击确定
        localStorage.clear()
        router.push('/login')
      })
      .catch(() => {
        // 【关键修复】点击取消或关闭弹窗，捕获异常但不做任何事，这样就不会报错了
        console.log('用户取消了退出')
      })
}
</script>

<style scoped>
.layout-container { height: 100vh; }
.el-aside { background-color: #304156; color: white; transition: width 0.3s; }
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  font-size: 16px;
  font-weight: bold;
  background: #2b2f3a;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.layout-header {
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}
.header-right { display: flex; align-items: center; }
.user-name { font-size: 14px; color: #606266; }
.el-main { background-color: #f0f2f5; padding: 20px; }
.logout-item { margin-top: auto; color: #ff4d4f !important; }
</style>