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
        <h2>热门探店日记</h2>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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

onMounted(() => {
  queryTypes()
  queryHotBlogs()
  
  // 绑定全局滚动监听 (针对 PC 宽屏)
  window.addEventListener('scroll', handleWindowScroll)
})

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

/* 分类金刚区 */
.categories-section {
  display: grid;
  grid-template-columns: repeat(10, 1fr); /* PC 宽屏 10列排版 */
  gap: 16px;
  background: #FFF;
  padding: 24px;
  justify-items: center;
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

/* 探店日记 */
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
  font-size: 15px;
  font-weight: 800;
  color: var(--rh-text-main);
  line-height: 1.5;
  height: 45px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
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
</style>
