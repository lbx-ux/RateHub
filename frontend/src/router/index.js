import { createRouter, createWebHashHistory } from 'vue-router'

// 定义路由列表
const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { title: '首页', keepAlive: true }
  },
  {
    path: '/shop-list',
    name: 'ShopList',
    component: () => import('../views/ShopList.vue'),
    meta: { title: '商户列表' }
  },
  {
    path: '/shop-detail',
    name: 'ShopDetail',
    component: () => import('../views/ShopDetail.vue'),
    meta: { title: '商户详情' }
  },
  {
    path: '/blog-detail',
    name: 'BlogDetail',
    component: () => import('../views/BlogDetail.vue'),
    meta: { title: '日记详情' }
  },
  {
    path: '/blog-edit',
    name: 'BlogEdit',
    component: () => import('../views/BlogEdit.vue'),
    meta: { title: '发布笔记', requireAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { title: '个人中心', requireAuth: true }
  },
  {
    path: '/profile-edit',
    name: 'ProfileEdit',
    component: () => import('../views/ProfileEdit.vue'),
    meta: { title: '资料编辑', requireAuth: true }
  },
  {
    path: '/user-detail',
    name: 'UserDetail',
    component: () => import('../views/UserDetail.vue'),
    meta: { title: '达人主页' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - RateHub` : 'RateHub'

  const token = sessionStorage.getItem('token')
  if (to.meta.requireAuth && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
