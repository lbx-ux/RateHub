<template>
  <div class="home-pc-container rh-container" @scroll="onScroll">
    <!-- 1. 宽屏横幅轮播 (PC 尺寸) -->
    <section class="banner-section">
      <el-carousel height="340px" class="pc-carousel" motion-blur>
        <el-carousel-item>
          <div class="banner-slide slide1">
            <div class="slide-content">
              <span class="slide-tag">今日推荐</span>
              <h2>城市周末寻味指南</h2>
              <p>探索那些隐匿在街角深处的美味小店与真实点评分享</p>
              <el-button type="primary" class="slide-btn" @click="toCategory(1, '美食')">立即探索</el-button>
            </div>
          </div>
        </el-carousel-item>
        <el-carousel-item>
          <div class="banner-slide slide2">
            <div class="slide-content">
              <span class="slide-tag">特惠福利</span>
              <h2>限量大额代金券抢购</h2>
              <p>全场通用优惠券每日十点限时秒杀，抢完为止</p>
              <el-button type="warning" class="slide-btn" @click="toCategory(1, '美食')">前往抢购</el-button>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <!-- 1.5 每日签到模块 -->
    <section class="sign-in-section rh-card" v-if="isLoggedIn">
      <div class="sign-left">
        <el-icon class="sign-icon"><Calendar /></el-icon>
        <div class="sign-text">
          <h3>每日签到</h3>
          <p>本月已连续签到 <span class="highlight">{{ signCount }}</span> 天，累计签到 <span class="highlight">{{ totalSignCount }}</span> 天，坚持获取专属福利！</p>
        </div>
      </div>
      <el-button 
        :type="isSigned ? 'success' : 'warning'" 
        class="sign-btn" 
        @click="handleSign"
      >
        {{ isSigned ? '查看签到数据' : '立即打卡签到' }}
      </el-button>
    </section>

    <!-- 2. PC 功能分类网格 (原 types) -->
    <section class="categories-section rh-card">
      <div 
        v-for="t in types" 
        :key="t.id" 
        class="category-item-card"
        @click="toCategory(t.id, t.name)"
      >
        <div class="cat-icon-container" :style="getCategoryStyle(t.name)">
          <img :src="'/imgs/' + t.icon" :alt="t.name" class="cat-img" />
        </div>
        <span class="cat-name">{{ t.name }}</span>
      </div>
    </section>

    <!-- 3. PC 探店日记瀑布网格 -->
    <section class="blogs-section">
      <div class="rh-section-title">
        <div class="title-left">
          <h2>热门探店日记</h2>
          <span class="title-sub">TRENDING DIARIES</span>
        </div>
        <span class="section-desc">探店达人真实试吃与心水安利</span>
      </div>

      <!-- 三栏瀑布布局 -->
      <div class="blogs-grid">
        <div 
          v-for="b in blogs" 
          :key="b.id" 
          class="blog-pc-card rh-card"
          @click="toBlogDetail(b.id)"
        >
          <div class="blog-image-box">
            <img :src="b.img" alt="Blog cover" class="blog-cover" />
            <!-- 精致浮动点赞标记 -->
            <div class="blog-like-badge" @click.stop="handleLike(b)">
              <el-icon :class="{ 'liked': b.isLike }" class="like-badge-icon">
                <StarFilled v-if="b.isLike" />
                <Star v-else />
              </el-icon>
              <span>{{ b.liked }}</span>
            </div>
          </div>

          <div class="blog-main-info">
            <h3 class="blog-card-title text-pretty">{{ b.title }}</h3>
            
            <div class="author-info-line">
              <div class="author-meta">
                <img :src="b.icon || '/imgs/icons/default-icon.png'" alt="Author avatar" class="author-avatar" />
                <span class="author-name">{{ b.name }}</span>
              </div>
              <span class="view-detail-hint">
                阅读全文 <el-icon><ArrowRight /></el-icon>
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div class="loading-bar">
        <el-icon class="loading-icon-spin" v-if="loading"><Loading /></el-icon>
        <span class="load-more-btn" v-else-if="!finished" @click="loadMore">加载更多日记</span>
        <span class="finished-text" v-else>已显示全部日记</span>
      </div>
    </section>

    <!-- 签到成功炫酷弹窗 -->
    <div class="sign-success-overlay" v-if="showSignSuccessModal" @click="showSignSuccessModal = false">
      <div class="sign-success-box" @click.stop>
        <div class="confetti-container">
          <div class="confetti" v-for="i in 12" :key="i" :style="{ left: (i * 7.5 + 5) + '%', animationDelay: (i * 0.1) + 's', backgroundColor: ['#FF3366', '#33CCFF', '#FFCC00', '#33FF66', '#CC33FF'][i % 5] }"></div>
        </div>
        <div class="sign-success-icon-wrap">
          <el-icon class="sign-success-icon"><Trophy /></el-icon>
        </div>
        <h2 class="sign-success-title">签到成功！</h2>
        <p class="sign-success-desc">
          本月已连续打卡 <span class="highlight-days">{{ signCount }}</span> 天<br>
          累计打卡 <span class="highlight-days">{{ totalSignCount }}</span> 天
        </p>
        <el-button type="primary" round class="sign-success-btn" @click="showSignSuccessModal = false">开心收下</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star, StarFilled, ArrowRight, Loading, Calendar, Trophy } from '@element-plus/icons-vue'
import request from '../utils/request'

const router = useRouter()

const getCategoryStyle = (name) => {
  const colorMap = {
    '美食': { bg: 'linear-gradient(135deg, #FFF6F3 0%, #FFEFEA 100%)', border: 'rgba(255, 102, 51, 0.2)', shadow: 'rgba(255, 102, 51, 0.35)' },
    'KTV': { bg: 'linear-gradient(135deg, #F8F5FF 0%, #F1EAFF 100%)', border: 'rgba(127, 0, 255, 0.2)', shadow: 'rgba(127, 0, 255, 0.35)' },
    '丽人·美发': { bg: 'linear-gradient(135deg, #FFF3F8 0%, #FFE5F1 100%)', border: 'rgba(255, 20, 147, 0.2)', shadow: 'rgba(255, 20, 147, 0.35)' },
    '美发美睫': { bg: 'linear-gradient(135deg, #FFF3F5 0%, #FFE5EC 100%)', border: 'rgba(248, 87, 166, 0.2)', shadow: 'rgba(248, 87, 166, 0.35)' },
    '美甲美睫': { bg: 'linear-gradient(135deg, #FFF3F5 0%, #FFE5EC 100%)', border: 'rgba(248, 87, 166, 0.2)', shadow: 'rgba(248, 87, 166, 0.35)' },
    '按摩·足疗': { bg: 'linear-gradient(135deg, #F3FAF6 0%, #E8F7EE 100%)', border: 'rgba(17, 153, 142, 0.2)', shadow: 'rgba(17, 153, 142, 0.35)' },
    '美容SPA': { bg: 'linear-gradient(135deg, #F0FBFC 0%, #E0F7F9 100%)', border: 'rgba(0, 188, 212, 0.2)', shadow: 'rgba(0, 188, 212, 0.35)' },
    '亲子游乐': { bg: 'linear-gradient(135deg, #FFFDF0 0%, #FFF9E0 100%)', border: 'rgba(255, 193, 7, 0.25)', shadow: 'rgba(255, 193, 7, 0.35)' },
    '酒吧': { bg: 'linear-gradient(135deg, #F5F7FA 0%, #ECEFF1 100%)', border: 'rgba(96, 125, 139, 0.25)', shadow: 'rgba(96, 125, 139, 0.35)' },
    '轰趴馆': { bg: 'linear-gradient(135deg, #FAF5FC 0%, #F3E5F5 100%)', border: 'rgba(156, 39, 176, 0.2)', shadow: 'rgba(156, 39, 176, 0.35)' },
    '健身运动': { bg: 'linear-gradient(135deg, #F0F8FF 0%, #E6F2FF 100%)', border: 'rgba(33, 150, 243, 0.2)', shadow: 'rgba(33, 150, 243, 0.35)' }
  }
  const config = colorMap[name] || colorMap['美食']
  return {
    '--cat-bg': config.bg,
    '--cat-border': config.border,
    '--cat-shadow': config.shadow
  }
}

const types = ref([])
const blogs = ref([])
const current = ref(1)
const loading = ref(false)
const finished = ref(false)

// 签到相关状态
const isLoggedIn = ref(false)
const isSigned = ref(false)
const signCount = ref(0)
const totalSignCount = ref(0)
const showSignSuccessModal = ref(false)

onMounted(() => {
  const token = sessionStorage.getItem('token')
  if (token) {
    isLoggedIn.value = true
    checkSignStatus()
  }

  queryTypes()
  queryHotBlogs()
  
  // 绑定全局滚动监听 (针对 PC 宽屏)
  window.addEventListener('scroll', handleWindowScroll)
})

onActivated(() => {
  // 当从博文详情页等路由返回首页时（由于 keep-alive 页面缓存），触发 onActivated 静默对齐点赞状态
  refreshBlogsStatus()
})

const refreshBlogsStatus = async () => {
  if (blogs.value.length === 0) return
  try {
    // 重新拉取当前已加载页面的热门博文，静默校准列表中已存在博文的最新点赞数与高亮状态，不打断用户滚动位置
    for (let page = 1; page <= current.value; page++) {
      const res = await request.get(`/blog/hot?current=${page}`)
      if (res.code === 200 && res.data) {
        const latestMap = new Map(res.data.map(item => [item.id, item]))
        blogs.value.forEach(b => {
          if (latestMap.has(b.id)) {
            const latest = latestMap.get(b.id)
            b.liked = latest.liked
            b.isLike = latest.isLike
          }
        })
      }
    }
  } catch (error) {
    console.error(error)
  }
}

const queryTypes = async () => {
  try {
    const res = await request.get('/shop-type/list')
    if (res.code === 200) {
      types.value = res.data
    }
  } catch (error) {
    console.error(error)
  }
}

// 签到相关方法
const checkSignStatus = async () => {
  try {
    const resCount = await request.get('/user/sign/count')
    if (resCount.code === 200) {
      signCount.value = resCount.data
    }
    const resTotal = await request.get('/user/sign/total')
    if (resTotal.code === 200) {
      totalSignCount.value = resTotal.data
    }
  } catch (error) {
    console.error(error)
  }
}

const handleSign = async () => {
  if (isSigned.value) {
    showSignSuccessModal.value = true
    return
  }

  try {
    const res = await request.post('/user/sign')
    // 如果没有抛出异常，说明签到成功 (拦截器已经处理了非200的抛出)
    isSigned.value = true
    showSignSuccessModal.value = true
    checkSignStatus() // 重新获取连签天数
  } catch (error) {
    // 拦截器如果遇到业务错误(code!=200)会 reject 出错误信息字符串
    const errMsg = (error && error.message) ? error.message : String(error)
    if (errMsg.includes('已经签到')) {
      // 容错处理：如果刷新页面后是第一次点，但后端查出已签到
      isSigned.value = true
      showSignSuccessModal.value = true
      checkSignStatus()
    } else {
      ElMessage.error(errMsg || '签到失败')
    }
  }
}

const queryHotBlogs = async () => {
  if (loading.value || finished.value) return
  try {
    loading.value = true
    const res = await request.get(`/blog/hot?current=${current.value}`)
    if (res.code === 200 && res.data) {
      const data = res.data
      if (data.length === 0) {
        finished.value = true
      } else {
        const mapped = data.map(b => ({
          ...b,
          img: b.images ? b.images.split(',')[0] : '/imgs/icons/default-icon.png'
        }))
        blogs.value = [...blogs.value, ...mapped]
      }
    } else {
      finished.value = true
    }
  } catch (error) {
    console.error(error)
    finished.value = true
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  current.value++
  queryHotBlogs()
}

const handleLike = async (blog) => {
  const token = sessionStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    await request.put(`/blog/like/${blog.id}`)
    const res = await request.get(`/blog/${blog.id}`)
    if (res.code === 200 && res.data) {
      blog.liked = res.data.liked
      blog.isLike = res.data.isLike
    }
  } catch (error) {
    ElMessage.error('点赞失败')
  }
}

const handleWindowScroll = () => {
  const { scrollTop, scrollHeight, clientHeight } = document.documentElement
  if (scrollTop + clientHeight >= scrollHeight - 50 && !loading.value && !finished.value) {
    loadMore()
  }
}

const toCategory = (id, name) => {
  router.push(`/shop-list?type=${id}&name=${encodeURIComponent(name)}`)
}

const toBlogDetail = (id) => {
  router.push(`/blog-detail?id=${id}`)
}
</script>

<style scoped>
.home-pc-container {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

/* 轮播大图 */
.banner-section {
  border-radius: var(--rh-radius-lg);
  overflow: hidden;
  box-shadow: var(--rh-shadow-subtle);
}

.banner-slide {
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 60px;
  color: white;
}

.slide1 {
  background: linear-gradient(90deg, rgba(26, 26, 26, 0.9) 0%, rgba(26, 26, 26, 0.2) 100%), url('https://images.unsplash.com/photo-1504674900247-0877df9cc836?q=80&w=1200&auto=format&fit=crop') no-repeat center/cover;
}

.slide2 {
  background: linear-gradient(90deg, rgba(26, 26, 26, 0.9) 0%, rgba(26, 26, 26, 0.2) 100%), url('https://images.unsplash.com/photo-1555396273-367ea4eb4db5?q=80&w=1200&auto=format&fit=crop') no-repeat center/cover;
}

.slide-content {
  max-width: 500px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
}

.slide-tag {
  background-color: var(--rh-primary);
  font-size: 11px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.slide-content h2 {
  font-size: 32px;
  font-weight: 800;
  line-height: 1.2;
}

.slide-content p {
  font-size: 14px;
  opacity: 0.85;
}

.slide-btn {
  height: 40px;
  border-radius: 20px !important;
  font-weight: 700;
  padding: 0 24px;
  margin-top: 8px;
}

/* 签到模块 */
.sign-in-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 32px;
  background: linear-gradient(135deg, #FFFDF8 0%, #FFF 100%);
  border-left: 4px solid #FFC107;
}

.sign-in-section:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.05);
}

.sign-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.sign-icon {
  font-size: 36px;
  color: #FFC107;
  background: #FFF9E0;
  padding: 10px;
  border-radius: 14px;
}

.sign-text h3 {
  font-size: 18px;
  font-weight: 800;
  color: var(--rh-text-main);
  margin-bottom: 4px;
}

.sign-text p {
  font-size: 13px;
  color: var(--rh-text-sub);
}

.sign-text .highlight {
  color: #FF6633;
  font-size: 18px;
  font-weight: 800;
  margin: 0 2px;
}

.sign-btn {
  border-radius: 24px !important;
  font-weight: 700;
  padding: 0 32px;
  height: 44px;
  font-size: 15px;
  letter-spacing: 1px;
}

/* 分类金刚区 */
.categories-section {
  display: grid;
  grid-template-columns: repeat(10, 1fr); /* PC 宽屏 10列排版 */
  gap: 16px;
  background: #FFF;
  padding: 24px;
  justify-items: center;
  /* 声明默认 CSS 变量以消除 IDE 未知变量报错，实际渲染由 Vue 动态内联注入覆盖 */
  --cat-bg: var(--rh-primary-light);
  --cat-border: rgba(255, 102, 51, 0.15);
  --cat-shadow: rgba(255, 102, 51, 0.35);
}

.categories-section:hover {
  transform: none; /* 容器不悬浮 */
}

.category-item-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  width: 100%;
}

.cat-icon-container {
  width: 60px;
  height: 60px;
  background: var(--cat-bg, var(--rh-primary-light));
  border: 1px solid var(--cat-border, rgba(255, 102, 51, 0.15));
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--rh-spring-transition);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);
}

.category-item-card:hover .cat-icon-container {
  transform: translateY(-6px) scale(1.08);
  box-shadow: 0 10px 20px -5px var(--cat-shadow, rgba(255, 102, 51, 0.35));
  border-color: transparent;
}

.cat-img {
  width: 32px;
  height: 32px;
  object-fit: contain;
  transition: var(--rh-spring-transition);
}

.category-item-card:hover .cat-img {
  transform: scale(1.12) rotate(3deg);
}

.cat-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--rh-text-main);
  transition: var(--rh-transition);
}

.category-item-card:hover .cat-name {
  color: var(--rh-primary);
}

/* 探店日记标题层级 */
.title-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.title-sub {
  font-size: 13px;
  font-weight: 800;
  color: var(--rh-primary);
  letter-spacing: 1.5px;
  text-transform: uppercase;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

.section-desc {
  font-size: 13px;
  color: var(--rh-text-light);
}

.blogs-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr); /* PC 端三栏排版 */
  gap: 24px;
}

.blog-pc-card {
  padding: 0;
  overflow: hidden;
  background: #FFF;
  display: flex;
  flex-direction: column;
}

.blog-image-box {
  position: relative;
  width: 100%;
  padding-top: 62.5%; /* 16:10 电影感比例 */
  background: #EEEEF0;
  overflow: hidden;
}

.blog-cover {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s;
}

.blog-pc-card:hover .blog-cover {
  transform: scale(1.06);
}

.blog-like-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(26, 26, 26, 0.7);
  backdrop-filter: blur(4px);
  color: white;
  border-radius: 14px;
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 4px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
  transition: var(--rh-transition);
}

.blog-like-badge:hover {
  background: var(--rh-primary);
  transform: scale(1.05);
}

.like-badge-icon {
  font-size: 13px;
}

.like-badge-icon.liked {
  color: #FFEB3B;
}

.blog-main-info {
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
}

.blog-card-title {
  font-size: 16px;
  font-weight: 800;
  color: var(--rh-text-main);
  line-height: 1.45;
  min-height: 46px;
  max-height: 46px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  transition: color 0.2s;
}

.blog-pc-card:hover .blog-card-title {
  color: var(--rh-primary);
}

.author-info-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid var(--rh-border);
  padding-top: 12px;
  margin-top: auto;
}

.author-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
}

.author-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--rh-text-sub);
}

.view-detail-hint {
  font-size: 12px;
  color: var(--rh-primary);
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 2px;
  opacity: 0;
  transform: translateX(-4px);
  transition: var(--rh-spring-transition);
}

.blog-pc-card:hover .view-detail-hint {
  opacity: 1;
  transform: translateX(0);
}

/* 加载更多 */
.loading-bar {
  text-align: center;
  padding: 40px 0;
}

.loading-icon-spin {
  font-size: 24px;
  color: var(--rh-primary);
  animation: el-icon-rotate 1.5s linear infinite;
}

.load-more-btn {
  background-color: white;
  border: 1px solid var(--rh-border);
  color: var(--rh-text-sub);
  padding: 10px 24px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: var(--rh-shadow-subtle);
  transition: var(--rh-transition);
}

.load-more-btn:hover {
  border-color: var(--rh-primary);
  color: var(--rh-primary);
}

.finished-text {
  font-size: 13px;
  color: var(--rh-text-light);
}

@keyframes el-icon-rotate {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* 签到成功炫酷弹窗 */
.sign-success-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fadeIn 0.3s ease-out;
}

.sign-success-box {
  background: white;
  width: 320px;
  border-radius: 24px;
  padding: 40px 20px;
  text-align: center;
  position: relative;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  animation: popIn 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards;
  overflow: hidden;
}

.sign-success-icon-wrap {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #FFD700 0%, #FF8C00 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  box-shadow: 0 10px 20px rgba(255, 140, 0, 0.3);
  animation: bounceIcon 2s infinite ease-in-out;
}

.sign-success-icon {
  font-size: 40px;
  color: white;
}

.sign-success-title {
  font-size: 24px;
  font-weight: 900;
  color: var(--rh-text-main);
  margin-bottom: 10px;
}

.sign-success-desc {
  font-size: 15px;
  color: var(--rh-text-sub);
  margin-bottom: 24px;
}

.highlight-days {
  color: #FF6633;
  font-size: 24px;
  font-weight: 900;
  margin: 0 4px;
}

.sign-success-btn {
  width: 80%;
  height: 44px;
  font-size: 16px;
  font-weight: bold;
  background: linear-gradient(90deg, #FF6633 0%, #FF8C00 100%);
  border: none;
  transition: transform 0.2s, box-shadow 0.2s;
  color: white;
}

.sign-success-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(255, 102, 51, 0.3);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes popIn {
  from { opacity: 0; transform: scale(0.8) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

@keyframes bounceIcon {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.confetti-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.confetti {
  position: absolute;
  width: 10px;
  height: 10px;
  top: -20px;
  opacity: 0;
  animation: confettiFall 2s ease-out forwards;
}

@keyframes confettiFall {
  0% { transform: translateY(0) rotate(0deg); opacity: 1; }
  100% { transform: translateY(350px) rotate(720deg); opacity: 0; }
}
</style>
