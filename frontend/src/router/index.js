import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/views/layout/index.vue'

const routes = [
    { path: '/login', name: '登录', component: () => import('@/views/login/index.vue') },
    {
        path: '/',
        component: Layout,
        redirect: '/dashboard',
        children: [
            { path: 'dashboard', name: '业务控制台', component: () => import('@/views/dashboard/index.vue') },
            { path: 'analysis', name: '客户分群分析', component: () => import('@/views/analysis/index.vue') },
            { path: 'transaction', name: '客户交易分析', component: () => import('@/views/transaction/index.vue') },
            { path: 'risk', name: '风险预警监控', component: () => import('@/views/risk/index.vue') },
            { path: 'user', name: '系统账户管理', component: () => import('@/views/user/index.vue') },
            { path: 'data', name: '数据集成中心', component: () => import('@/views/data/index.vue') },
            { path: 'log', name: '系统审计日志', component: () => import('@/views/log/index.vue') },
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 增加路由守卫：没登录只能去登录页
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token')
    if (to.path !== '/login' && !token) {
        next('/login')
    } else {
        next()
    }
})

export default router