<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Header from './components/Header.vue'

const route = useRoute()

// 登录页通常不需要顶部全局导航栏
const showHeader = computed(() => {
  return route.name !== 'Login'
})
</script>

<template>
  <div id="app">
    <!-- PC 全局顶部粘性导航栏 -->
    <Header v-if="showHeader" />
    
    <!-- 主版面渲染 -->
    <main class="rh-main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <keep-alive>
            <component :is="Component" v-if="route.meta.keepAlive" :key="route.fullPath" />
          </keep-alive>
        </transition>
        <transition name="fade" mode="out-in">
          <component :is="Component" v-if="!route.meta.keepAlive" :key="route.fullPath" />
        </transition>
      </router-view>
    </main>

    <!-- 统一简约 PC 页脚 -->
    <footer class="rh-footer" v-if="showHeader">
      <div class="rh-container footer-inner">
        <p class="copyright">Copyright &copy; 2026 RateHub. All Rights Reserved. 寻味生活，尽在 RateHub。</p>
      </div>
    </footer>
  </div>
</template>

<style>
/* 全局页面平滑过渡动效 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 页脚样式 */
.rh-footer {
  background-color: #FFFFFF;
  border-top: 1px solid var(--rh-border);
  padding: 24px 0;
  color: var(--rh-text-light);
  font-size: 13px;
  text-align: center;
  margin-top: auto;
}

.footer-inner {
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>
