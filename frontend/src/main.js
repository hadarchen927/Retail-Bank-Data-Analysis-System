import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}
router.beforeEach((to, from, next) => {
    if (to.name) {
        document.title = to.name + ' - 零售银行数据分析系统';
    }
    next();
});

app.use(router)
app.use(ElementPlus)
app.mount('#app')