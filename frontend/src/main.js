import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import './style.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(ElementPlus)

// 全局注册所有 Element Plus 的图标组件
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册 v-reveal 视口交叉观察器指令 (用于滚动动效)
app.directive('reveal', {
  mounted(el) {
    el.classList.add('reveal-item')
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          el.classList.add('is-visible')
          observer.unobserve(el) // 动画只执行一次
        }
      })
    }, { threshold: 0.1 })
    observer.observe(el)
  }
})

app.mount('#app')
