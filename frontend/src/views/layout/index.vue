<template>
  <el-container class="layout-container">
    <!-- 左侧菜单栏 -->
    <el-aside width="220px">
      <div class="logo">
        <el-icon style="margin-right: 10px"><TrendCharts /></el-icon>
        RBDAS 银行分析
      </div>
      <!-- 【修改点】去掉 router 属性，改用 @select 手动处理 -->
      <el-menu
          :default-active="$route.path"
          class="el-menu-vertical"
          background-color="#304156"
          text-color="#fff"
          active-text-color="#409EFF"
          @select="handleMenuSelect"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>业务控制台</span>
        </el-menu-item>

        <el-menu-item index="/data" v-if="userRole === 'ADMIN' || userRole === 'ANALYST' || userRole === 'RISK_OFFICER'">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据集成中心</span>
        </el-menu-item>

        <el-menu-item index="/analysis" v-if="userRole === 'ADMIN' || userRole === 'ANALYST' || userRole === 'MANAGER'" >
          <el-icon><PieChart /></el-icon>
          <span>客户分群分析</span>
        </el-menu-item>

        <el-menu-item index="/risk" v-if="userRole === 'ADMIN' || userRole === 'RISK_OFFICER'">
          <el-icon><WarnTriangleFilled /></el-icon>
          <span>风险预警监控</span>
        </el-menu-item>

        <el-menu-item index="/user" v-if="userRole === 'ADMIN'">
          <el-icon><UserFilled /></el-icon>
          <span>系统账户管理</span>
        </el-menu-item>
        <el-menu-item index="/log" v-if="userRole === 'ADMIN'">
          <el-icon><Document /></el-icon>
          <span>系统审计日志</span>
        </el-menu-item>
        <!-- index 改为 logout，用于逻辑判断 -->
        <el-menu-item index="logout" class="logout-item">
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
          <el-tag effect="dark" size="small" style="margin-right: 15px">{{ getRoleName(userRole) }}</el-tag>

          <el-dropdown trigger="click">
            <span class="user-dropdown-link">
              {{ nickname }} <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item icon="User" @click="openProfileDialog">个人中心</el-dropdown-item>
                <el-dropdown-item icon="SwitchButton" divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main>
        <router-view />
      </el-main>
    </el-container>

    <!-- 个人资料修改弹窗 -->
    <el-dialog v-model="profileDialogVisible" title="个人信息修改" width="400px" destroy-on-close>
      <el-form :model="profileForm" label-width="80px">
        <el-form-item label="账号">
          <el-input v-model="profileForm.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="profileForm.nickname" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="profileForm.password" type="password" placeholder="不修改请留空" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="profileLoading" @click="submitProfileUpdate">保存修改</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import axios from '@/utils/request'

const router = useRouter()
const nickname = ref(localStorage.getItem('nickname') || '未登录')
const userRole = ref(localStorage.getItem('role') || 'GUEST')
const username = ref(localStorage.getItem('username') || '')

// 【核心修复】统一处理菜单点击
const handleMenuSelect = (index) => {
  if (!index) return; // 防止空点击
  if (index === 'logout') {
    handleLogout()
  } else {
    // 只有当点击的不是当前页面时才跳转，防止重复点击报错
    if (router.currentRoute.value.path !== index) {
      router.push(index).catch(err => {
        // 捕获冗余导航错误
        if (err.name !== 'NavigationDuplicated') throw err
      })
    }
  }
}

const getRoleName = (role) => {
  const map = { 'ADMIN': '系统管理员', 'ANALYST': '业务分析师', 'RISK_OFFICER': '风险控制员', 'MANAGER': '市场经理' }
  return map[role] || '普通用户'
}

// 个人资料修改逻辑
const profileDialogVisible = ref(false)
const profileLoading = ref(false)
const profileForm = reactive({ username: '', nickname: '', password: '' })

const openProfileDialog = () => {
  profileForm.username = username.value
  profileForm.nickname = nickname.value
  profileForm.password = ''
  profileDialogVisible.value = true
}

const submitProfileUpdate = async () => {
  profileLoading.value = true
  try {
    await axios.put('/api/user/update-profile', profileForm)
    ElMessage.success('修改成功！请重新登录')
    setTimeout(() => {
      localStorage.clear()
      router.push('/login')
    }, 1500)
  } finally {
    profileLoading.value = false
  }
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出系统吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    localStorage.clear()
    router.push('/login')
  }).catch(() => {
    // 此时不会有任何跳转，页面保持原样
    console.log('取消退出')
  })
}
</script>

<style scoped>
.layout-container { height: 100vh; }
.el-aside { background-color: #304156; }
.logo {
  height: 60px; line-height: 60px; text-align: center;
  font-size: 16px; font-weight: bold; background: #2b2f3a; color: #fff;
  display: flex; align-items: center; justify-content: center;
}
.layout-header {
  background: #fff; border-bottom: 1px solid #e6e6e6;
  display: flex; justify-content: space-between; align-items: center; padding: 0 20px;
}
.header-right { display: flex; align-items: center; }
.user-dropdown-link { cursor: pointer; color: #409EFF; font-size: 14px; display: flex; align-items: center; }
.el-main { background-color: #f0f2f5; padding: 20px; }
.logout-item { margin-top: 20px; color: #ff4d4f !important; }
</style>