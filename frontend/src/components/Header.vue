<template>
  <header class="rh-glass-nav pc-header">
    <div class="rh-container header-inner">
      <!-- 左侧 Logo 与城市选择 -->
      <div class="header-left">
        <div class="logo-box" @click="toHome">
          <div class="logo-icon">RH</div>
          <span class="logo-name">RateHub</span>
        </div>
        
        <div class="city-selector">
          <el-icon class="loc-icon"><Location /></el-icon>
          <span class="city-name">杭州市</span>
          <el-icon class="arrow-down-icon"><ArrowDown /></el-icon>
        </div>
      </div>

      <!-- 中部导航链接 -->
      <nav class="header-nav">
        <router-link to="/" class="nav-item" :class="{ 'active': route.path === '/' }">
          首页
        </router-link>
        <router-link to="/shop-list?type=1&name=美食" class="nav-item" :class="{ 'active': route.path.startsWith('/shop-list') }">
          商户探索
        </router-link>
      </nav>

      <!-- 右侧搜索、发笔记及用户头像下拉单 -->
      <div class="header-right">
        <!-- 搜索输入框 -->
        <div class="search-input-box">
          <el-input
            v-model="searchVal"
            placeholder="搜索商户、美食..."
            clearable
            @keyup.enter="handleSearch"
            class="header-search"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <!-- 发笔记按钮 -->
        <el-button type="primary" class="publish-btn" @click="toPublish">
          <el-icon class="btn-icon"><Plus /></el-icon>
          发笔记
        </el-button>

        <!-- 用户头像区 -->
        <div class="user-menu-box">
          <!-- 已登录 -->
          <el-dropdown v-if="token" trigger="hover" @command="handleCommand">
            <div class="avatar-dropdown-trigger">
              <img :src="userAvatar" alt="Avatar" class="header-avatar" />
              <span class="header-username">{{ nickname }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu class="custom-dropdown-menu">
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人主页
                </el-dropdown-item>
                <el-dropdown-item command="edit">
                  <el-icon><EditPen /></el-icon>资料编辑
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided class="logout-item">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <!-- 未登录 -->
          <el-button v-else type="primary" plain round size="small" @click="toLogin">
            登录 / 注册
          </el-button>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import request from '../utils/request'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const searchVal = ref('')

const token = computed(() => userStore.token)
const nickname = computed(() => userStore.userInfo?.nickName || '用户')
const userAvatar = computed(() => userStore.userInfo?.icon || '/imgs/icons/default-icon.png')

const toHome = () => router.push('/')
const toPublish = () => router.push('/blog-edit')
const toLogin = () => router.push('/login')

const handleSearch = async () => {
  if (!searchVal.value.trim()) return
  try {
    const res = await request.get(`/shop/of/name?name=${encodeURIComponent(searchVal.value)}`)
    if (res.code === 200 && res.data && res.data.length > 0) {
      // 模糊匹配到商户，跳转至对应的第一个商户详情页
      router.push(`/shop-detail?id=${res.data[0].id}`)
    } else {
      ElMessage.warning('未找到相关商户')
    }
  } catch (error) {
    console.error(error)
  }
}

const handleCommand = (command) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'edit') {
    router.push('/profile-edit')
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定要退出当前账户登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--primary',
      type: 'warning'
    }).then(async () => {
      try {
        await request.post('/user/logout')
        userStore.clearUser()
        ElMessage.success('退出成功')
        router.replace('/')
      } catch (error) {
        userStore.clearUser()
        router.replace('/')
      }
    }).catch(() => {})
  }
}
</script>

<style scoped>
.pc-header {
  height: 64px;
  width: 100%;
}

.header-inner {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 左侧 Logo */
.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.logo-box {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, var(--rh-primary), #FF8855);
  color: white;
  font-size: 16px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  box-shadow: 0 4px 10px rgba(255, 102, 51, 0.2);
  border: 1px dashed rgba(255, 255, 255, 0.4);
}

.logo-name {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0.5px;
  background: linear-gradient(135deg, var(--rh-primary), #FF3300);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.city-selector {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--rh-text-sub);
  background: rgba(0, 0, 0, 0.03);
  padding: 6px 12px;
  border-radius: 16px;
  cursor: pointer;
  transition: var(--rh-transition);
}

.city-selector:hover {
  background: rgba(0, 0, 0, 0.06);
  color: var(--rh-primary);
}

.loc-icon {
  color: var(--rh-primary);
}

.arrow-down-icon {
  font-size: 10px;
}

/* 中部导航 */
.header-nav {
  display: flex;
  gap: 24px;
}

.nav-item {
  font-size: 15px;
  font-weight: 600;
  color: var(--rh-text-sub);
  padding: 8px 4px;
  position: relative;
}

.nav-item::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 2px;
  background: var(--rh-primary);
  border-radius: 2px;
  transition: var(--rh-transition);
}

.nav-item:hover, .nav-item.active {
  color: var(--rh-primary);
}

.nav-item.active::after {
  width: 100%;
}

/* 右侧菜单区域 */
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.search-input-box {
  width: 220px;
}

:deep(.header-search .el-input__wrapper) {
  border-radius: 20px;
  background-color: rgba(245, 247, 250, 0.8);
  border: 1px solid transparent;
  box-shadow: none !important;
}

:deep(.header-search .el-input__wrapper.is-focus) {
  background-color: white;
  border-color: var(--rh-primary);
}

.publish-btn {
  height: 36px;
  font-size: 13px;
  border-radius: 18px !important;
}

.btn-icon {
  margin-right: 4px;
}

/* 头像菜单 */
.user-menu-box {
  display: flex;
  align-items: center;
  margin-left: 4px;
}

.avatar-dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px;
  border-radius: 20px;
  transition: var(--rh-transition);
}

.avatar-dropdown-trigger:hover {
  background-color: rgba(0, 0, 0, 0.03);
}

.header-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--rh-border);
}

.header-username {
  font-size: 13px;
  font-weight: 700;
  color: var(--rh-text-sub);
}

.custom-dropdown-menu {
  border-radius: 10px !important;
  box-shadow: var(--rh-shadow-md) !important;
  border: 1px solid var(--rh-border);
}

.logout-item {
  color: #f56c6c !important;
}
</style>
