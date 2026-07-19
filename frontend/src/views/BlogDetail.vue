<template>
  <div class="blog-detail-pc-page rh-container" v-loading="pageLoading">
    <!-- 顶部面包屑 -->
    <div class="breadcrumb-container">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>达人探店</el-breadcrumb-item>
        <el-breadcrumb-item>{{ blog.title || '日记详情' }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 经典左右双栏布局 -->
    <div class="blog-layout-split">
      <!-- 左边日记正文栏 -->
      <div class="blog-left-content">
        <article class="blog-main-card rh-card">
          <!-- 标题 -->
          <h1 class="blog-title">{{ blog.title }}</h1>
          <div class="blog-meta-line">
            <span class="pub-time">发布于：{{ formatTime(blog.createTime) }}</span>
            <span class="divider">|</span>
            <span class="stats"><el-icon><Star /></el-icon> {{ blog.liked || 0 }} 人点赞</span>
          </div>

          <!-- 图片精美幻灯轮播 -->
          <div class="blog-carousel-wrapper" v-if="blogImages.length > 0">
            <el-carousel trigger="click" height="420px" class="pc-blog-carousel">
              <el-carousel-item v-for="(img, idx) in blogImages" :key="idx">
                <el-image :src="img" fit="cover" class="carousel-image" :preview-src-list="blogImages" />
              </el-carousel-item>
            </el-carousel>
          </div>

          <!-- 正文内容 -->
          <div class="blog-article-content text-pretty" v-html="blog.content"></div>

          <!-- 大点赞互动区 (底座) -->
          <div class="interaction-footer">
            <el-button 
              type="primary" 
              :plain="!blog.isLike" 
              class="huge-like-btn"
              :class="{ 'is-liked': blog.isLike }"
              @click="handleLike"
            >
              <el-icon class="btn-icon">
                <StarFilled v-if="blog.isLike" />
                <Star v-else />
              </el-icon>
              <span>{{ blog.isLike ? '已赞' : '点赞支持' }} ({{ blog.liked || 0 }})</span>
            </el-button>
          </div>
        </article>

        <!-- 关联商户推荐卡片 -->
        <section v-if="shop.id" class="shop-recommend-card rh-card" @click="toShopDetail">
          <img :src="shopImage" alt="Shop cover" class="shop-cover" />
          <div class="shop-info">
            <span class="rec-tag">日记关联商户</span>
            <h3 class="shop-name">{{ shop.name }}</h3>
            <div class="rate-row">
              <el-rate v-model="shopRating" disabled size="12px" colors="#FF6633" />
              <span class="score-text">{{ (shop.score / 10).toFixed(1) }}分</span>
              <span class="price-avg">人均 ￥{{ shop.avgPrice }}/人</span>
            </div>
            <p class="address-text">{{ shop.address }}</p>
          </div>
          <el-button type="primary" plain class="go-shop-btn">
            前往商家 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </section>

        <!-- 评论区 -->
        <section class="comments-section rh-card">
          <h3>评论留言 (2)</h3>
          <div class="cmt-divider"></div>
          
          <div class="cmt-input-row">
            <el-input 
              v-model="commentText" 
              placeholder="说点什么，留下你的足迹吧..." 
              @keyup.enter="submitComment"
            />
            <el-button type="primary" @click="submitComment">发表评论</el-button>
          </div>

          <div class="cmt-list">
            <div class="cmt-item">
              <img src="https://images.unsplash.com/photo-1544005313-94ddf0286df2?q=80&w=100" alt="Avatar" />
              <div class="cmt-body">
                <div class="cmt-user-row">
                  <span class="cmt-username">米其林捕手</span>
                  <span class="cmt-time">3小时前</span>
                </div>
                <p class="cmt-text">PC版界面设计太舒服了！已经收藏了该文章，周末一定要去打卡西湖醋鱼。</p>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- 右侧信息栏 (作者、点赞流) -->
      <aside class="blog-right-side">
        <!-- 1. 作者卡片 -->
        <div class="author-info-card rh-card">
          <div class="author-header" @click="toAuthorDetail">
            <img :src="blog.icon || '/imgs/icons/default-icon.png'" alt="Author" class="author-avatar" />
            <div class="author-meta">
              <h3 class="author-name">{{ blog.name }}</h3>
              <span class="author-role">RateHub 美食达人</span>
            </div>
          </div>
          <div class="author-divider"></div>
          <p class="author-bio">
            {{ followed ? '已收录于您的关注清单中。' : '关注达人，第一时间接收 TA 的最新城市美食探店情报。' }}
          </p>
          <div class="author-action-row" v-if="!isSelf">
            <el-button 
              type="primary" 
              :plain="followed"
              :loading="followLoading"
              class="follow-action-btn"
              @click="handleFollow"
            >
              {{ followed ? '已关注' : '关注达人' }}
            </el-button>
          </div>
        </div>

        <!-- 2. 点赞动态列表 -->
        <div class="likes-avatar-card rh-card" v-if="likes.length > 0">
          <h3>点赞动态</h3>
          <div class="author-divider"></div>
          <div class="likes-flow">
            <div 
              v-for="u in likes" 
              :key="u.id" 
              class="like-user-row"
              @click="toOtherUserDetail(u.id)"
            >
              <img :src="u.icon || '/imgs/icons/default-icon.png'" alt="User" />
              <span class="like-username">{{ u.nickName }}</span>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, StarFilled, ArrowRight } from '@element-plus/icons-vue'
import request from '../utils/request'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const blogId = route.query.id
const blog = ref({})
const shop = ref({})
const likes = ref([])
const followed = ref(false)
const followLoading = ref(false)
const pageLoading = ref(false)
const commentText = ref('')

const isSelf = computed(() => {
  return userStore.userInfo && userStore.userInfo.id === blog.value.userId
})

const blogImages = computed(() => {
  if (blog.value.images) {
    return blog.value.images.split(',')
  }
  return []
})

const shopImage = computed(() => {
  if (shop.value.images) {
    return shop.value.images.split(',')[0]
  }
  return '/imgs/icons/default-icon.png'
})

const shopRating = computed(() => {
  return shop.value.score ? shop.value.score / 10 : 0
})

onMounted(() => {
  if (!blogId) {
    ElMessage.error('探店笔记不存在')
    router.back()
    return
  }
  queryBlogDetail()
})

const queryBlogDetail = async () => {
  try {
    pageLoading.value = true
    const res = await request.get(`/blog/${blogId}`)
    if (res.code === 200 && res.data) {
      blog.value = res.data
      queryShopDetail(res.data.shopId)
      queryLikeList()
      checkFollowStatus()
    }
  } catch (error) {
    console.error(error)
  } finally {
    pageLoading.value = false
  }
}

const queryShopDetail = async (shopId) => {
  try {
    const res = await request.get(`/shop/${shopId}`)
    if (res.code === 200) {
      shop.value = res.data
    }
  } catch (error) {
    console.error(error)
  }
}

const queryLikeList = async () => {
  try {
    const res = await request.get(`/blog/likes/${blogId}`)
    if (res.code === 200) {
      likes.value = res.data
    }
  } catch (error) {
    console.error(error)
  }
}

const checkFollowStatus = async () => {
  const token = sessionStorage.getItem('token')
  if (!token || isSelf.value) return
  
  try {
    const res = await request.get(`/follow/or/not/${blog.value.userId}`)
    if (res.code === 200) {
      followed.value = res.data
    }
  } catch (error) {
    console.error(error)
  }
}

const handleFollow = async () => {
  const token = sessionStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
    return
  }

  try {
    followLoading.value = true
    const targetStatus = !followed.value
    await request.put(`/follow/${blog.value.userId}/${targetStatus}`)
    followed.value = targetStatus
    ElMessage.success(targetStatus ? '已关注达人' : '已取消关注')
  } catch (error) {
    ElMessage.error('关注处理失败')
  } finally {
    followLoading.value = false
  }
}

const handleLike = async () => {
  const token = sessionStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
    return
  }

  try {
    await request.put(`/blog/like/${blogId}`)
    const res = await request.get(`/blog/${blogId}`)
    if (res.code === 200 && res.data) {
      blog.value.liked = res.data.liked
      blog.value.isLike = res.data.isLike
      queryLikeList()
    }
  } catch (error) {
    ElMessage.error('点赞处理失败')
  }
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const pad = (num) => String(num).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const toAuthorDetail = () => {
  if (isSelf.value) {
    router.push('/profile')
  } else {
    router.push(`/user-detail?id=${blog.value.userId}`)
  }
}

const toOtherUserDetail = (id) => {
  router.push(`/user-detail?id=${id}`)
}

const toShopDetail = () => {
  router.push(`/shop-detail?id=${shop.value.id}`)
}

const submitComment = () => {
  if (!commentText.value.trim()) return
  ElMessage.info('评论发表成功（演示）')
  commentText.value = ''
}
</script>

<style scoped>
.blog-detail-pc-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.breadcrumb-container {
  padding: 8px 0;
}

/* 左右双栏 */
.blog-layout-split {
  display: grid;
  grid-template-columns: 8fr 3fr;
  gap: 24px;
}

/* 左侧主要内容 */
.blog-left-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.blog-main-card {
  background: white;
  padding: 30px;
}

.blog-main-card:hover {
  transform: none;
}

.blog-title {
  font-size: 26px;
  font-weight: 800;
  color: var(--rh-text-main);
  line-height: 1.3;
}

.blog-meta-line {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: var(--rh-text-light);
  margin-top: 10px;
  margin-bottom: 24px;
}

.blog-meta-line .stats {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--rh-primary);
  font-weight: 700;
}

/* 轮播图 */
.blog-carousel-wrapper {
  margin-bottom: 24px;
  border-radius: var(--rh-radius-lg);
  overflow: hidden;
  box-shadow: var(--rh-shadow-subtle);
  border: 1px solid var(--rh-border);
}

.carousel-image {
  width: 100%;
  height: 100%;
  cursor: zoom-in;
}

/* 正文 */
.blog-article-content {
  font-size: 15px;
  color: #2c3e50;
  line-height: 1.8;
  white-space: pre-wrap;
  letter-spacing: 0.5px;
}

/* 点赞按钮 */
.interaction-footer {
  margin-top: 40px;
  border-top: 1px solid var(--rh-border);
  padding-top: 24px;
  display: flex;
  justify-content: center;
}

.huge-like-btn {
  height: 48px;
  padding: 0 32px;
  font-size: 15px;
  border-radius: 24px !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.02);
}

.huge-like-btn.is-liked {
  box-shadow: 0 4px 16px rgba(255, 102, 51, 0.25);
}

/* 推荐商铺 */
.shop-recommend-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background-color: #FAFAFC;
  cursor: pointer;
}

.shop-cover {
  width: 80px;
  height: 80px;
  border-radius: var(--rh-radius-md);
  object-fit: cover;
  flex-shrink: 0;
  background-color: #EEE;
}

.shop-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.rec-tag {
  font-size: 10px;
  font-weight: 700;
  color: var(--rh-primary);
  align-self: flex-start;
}

.shop-info .shop-name {
  font-size: 15px;
  font-weight: 800;
  color: var(--rh-text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rate-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.rate-row .score-text {
  font-weight: 700;
  color: var(--rh-primary);
}

.rate-row .price-avg {
  color: var(--rh-text-sub);
}

.address-text {
  font-size: 11px;
  color: var(--rh-text-light);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.go-shop-btn {
  height: 32px;
  font-size: 12px;
  border-radius: 16px !important;
  flex-shrink: 0;
}

/* 评论区 */
.comments-section:hover {
  transform: none;
}

.cmt-divider {
  height: 1px;
  background-color: var(--rh-border);
  margin: 12px 0 20px 0;
}

.cmt-input-row {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.cmt-input-row :deep(.el-input__wrapper) {
  border-radius: 20px;
  background-color: #FAFAFC;
}

.cmt-input-row button {
  border-radius: 20px !important;
}

.cmt-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cmt-item {
  display: flex;
  gap: 14px;
  border-bottom: 1px solid var(--rh-border);
  padding-bottom: 16px;
}

.cmt-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.cmt-item img {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.cmt-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.cmt-user-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cmt-username {
  font-size: 12px;
  font-weight: 700;
  color: var(--rh-text-main);
}

.cmt-time {
  font-size: 11px;
  color: var(--rh-text-light);
}

.cmt-text {
  font-size: 13px;
  color: #333;
  line-height: 1.5;
}

/* 右边栏 */
.blog-right-side {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.author-info-card:hover, .likes-avatar-card:hover {
  transform: none;
}

.author-header {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.author-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--rh-primary-light);
}

.author-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.author-name {
  font-size: 15px;
  font-weight: 800;
  color: var(--rh-text-main);
}

.author-role {
  font-size: 10px;
  color: var(--rh-primary);
  font-weight: 700;
}

.author-divider {
  height: 1px;
  background-color: var(--rh-border);
  margin: 14px 0;
}

.author-bio {
  font-size: 12px;
  color: var(--rh-text-sub);
  line-height: 1.5;
  background: #FAFAFC;
  padding: 10px 12px;
  border-radius: var(--rh-radius-md);
}

.author-action-row {
  margin-top: 14px;
}

.follow-action-btn {
  width: 100%;
  border-radius: 20px !important;
}

.likes-avatar-card h3 {
  font-size: 14px;
  font-weight: 800;
}

.likes-flow {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.like-user-row {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  transition: var(--rh-transition);
}

.like-user-row:hover {
  background-color: #FAFAFC;
}

.like-user-row img {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  object-fit: cover;
}

.like-username {
  font-size: 12px;
  font-weight: 700;
  color: var(--rh-text-sub);
}
</style>
