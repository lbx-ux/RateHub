<template>
  <div class="profile-pc-page rh-container" v-loading="pageLoading">
    <!-- 顶部个人中心大横幅 (PC 专享) -->
    <div class="profile-hero-banner">
      <div class="banner-overlay"></div>
    </div>

    <!-- 主版面双栏布局 -->
    <div class="profile-dashboard-layout">
      <!-- 1. 左侧个人基本信息名片栏 -->
      <aside class="left-profile-sidebar">
        <div class="user-info-card rh-card">
          <div class="avatar-wrapper">
            <img :src="user.icon || '/imgs/icons/default-icon.png'" alt="Avatar" class="avatar-img" />
          </div>

          <h2 class="nickname">{{ user.nickName }}</h2>
          <div class="location-badge">
            <el-icon><Location /></el-icon>
            <span>常居：杭州市</span>
          </div>

          <div class="sidebar-divider"></div>

          <p class="introduce text-pretty">
            {{ details.introduce || '添加个人介绍，让大家更好的认识你 ✨' }}
          </p>

          <div class="sidebar-action-box">
            <el-button type="primary" plain class="edit-btn" @click="toEdit">
              <el-icon class="btn-icon"><EditPen /></el-icon>
              编辑资料
            </el-button>
            <el-button type="danger" plain class="logout-btn" @click="handleLogout">
              <el-icon class="btn-icon"><SwitchButton /></el-icon>
              退出登录
            </el-button>
          </div>
        </div>
      </aside>

      <!-- 2. 右侧主要内容卡片区 (Tabs) -->
      <div class="right-main-tabs">
        <el-tabs v-model="activeTab" class="custom-profile-tabs" @tab-change="onTabChange">
          <!-- 我的笔记 Tab -->
          <el-tab-pane label="我的笔记" name="blogs">
            <div class="tab-content-panel">
              <div class="my-blogs-grid" v-if="blogs.length > 0">
                <div 
                  v-for="b in blogs" 
                  :key="b.id" 
                  class="blog-mini-card rh-card"
                  @click="toBlogDetail(b.id)"
                >
                  <img :src="b.img" alt="Blog cover" class="blog-mini-cover" />
                  <div class="blog-mini-body">
                    <h4 class="blog-mini-title text-pretty">{{ b.title }}</h4>
                    <div class="blog-mini-footer">
                      <span class="stats-item">
                        <el-icon><Star /></el-icon>
                        {{ b.liked || 0 }}
                      </span>
                      <span class="stats-item">
                        <el-icon><ChatLineRound /></el-icon>
                        {{ b.comments || 0 }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
              
              <el-empty v-else description="您尚未发表过探店日记，点击顶部“发笔记”开启分享吧！" />
            </div>
          </el-tab-pane>

          <!-- 我的订单 Tab -->
          <el-tab-pane label="我的订单" name="orders">
            <div class="tab-content-panel" v-loading="ordersLoading">
              <div class="my-orders-list" v-if="orders.length > 0">
                <el-card v-for="order in orders" :key="order.id" class="order-card" shadow="hover">
                  <div class="order-header">
                    <span class="order-id">订单号：{{ order.id }}</span>
                    <el-tag :type="order.type === 1 ? 'danger' : 'success'" size="small">
                      {{ order.type === 1 ? '秒杀券' : '普通券' }}
                    </el-tag>
                  </div>
                  <div class="order-body">
                    <div class="order-info-left">
                      <h3 class="order-title">{{ order.title || '未知优惠券' }}</h3>
                      <div class="order-price">
                        <span class="pay-value">实付：￥{{ ((order.payValue || 0) / 100).toFixed(2) }}</span>
                        <span class="actual-value" v-if="order.actualValue"> (可抵扣：￥{{ (order.actualValue / 100).toFixed(2) }})</span>
                      </div>
                      <div class="order-time">下单时间：{{ formatTime(order.createTime) }}</div>
                    </div>
                    <div class="order-action-right">
                      <el-button type="primary" plain size="small" @click="toShop(order.shopId)">去使用</el-button>
                    </div>
                  </div>
                </el-card>
              </div>
              <el-empty v-else-if="!ordersLoading" description="暂无订单记录，去逛逛优惠券吧~" />
            </div>
          </el-tab-pane>

          <!-- 关注动态 Tab -->
          <el-tab-pane label="达人动态 (关注流)" name="followBlogs">
            <div class="tab-content-panel">
              <div class="feed-list-pc" v-if="followBlogs.length > 0">
                <div 
                  v-for="b in followBlogs" 
                  :key="b.id" 
                  class="feed-card-pc rh-card"
                  @click="toBlogDetail(b.id)"
                >
                  <!-- 头部作者 -->
                  <div class="feed-header">
                    <div class="feed-author">
                      <img :src="b.icon || '/imgs/icons/default-icon.png'" alt="Avatar" class="feed-avatar" />
                      <span class="feed-username">{{ b.name }}</span>
                    </div>
                    <el-tag type="info" size="small" class="feed-tag">已关注达人</el-tag>
                  </div>

                  <!-- 主体双栏：左文字、右大图 -->
                  <div class="feed-body-split">
                    <div class="feed-text-info">
                      <h3 class="feed-title text-pretty">{{ b.title }}</h3>
                      <p class="feed-hint">点击查看详情，与达人一同探索美味...</p>
                    </div>
                    <img :src="b.img" alt="Feed cover" class="feed-image" />
                  </div>

                  <!-- 底部互动 -->
                  <div class="feed-footer">
                    <el-button 
                      type="primary" 
                      link
                      :class="{ 'is-liked': b.isLike }"
                      @click.stop="toggleLike(b)"
                      class="feed-like-action-btn"
                    >
                      <el-icon class="btn-icon">
                        <StarFilled v-if="b.isLike" />
                        <Star v-else />
                      </el-icon>
                      <span>{{ b.liked || 0 }}</span>
                    </el-button>
                  </div>
                </div>

                <!-- 分页滚动 -->
                <div class="load-more-feed-container">
                  <el-button 
                    v-if="!followFinished" 
                    :loading="followLoading"
                    @click="queryBlogsOfFollow(false)"
                    class="load-more-feed-btn"
                  >
                    点击加载更多动态
                  </el-button>
                  <span class="finished-text" v-else>已加载全部动态</span>
                </div>
              </div>

              <el-empty v-else-if="!followLoading" description="您关注的达人暂无最新动态推送" />
            </div>
          </el-tab-pane>

          <!-- 评价 Tab -->
          <el-tab-pane label="我的评价" name="comments">
            <div class="tab-content-panel">
              <el-empty description="暂无评价记录" />
            </div>
          </el-tab-pane>

          <!-- 粉丝 Tab -->
          <el-tab-pane label="我的粉丝" name="fans">
            <div class="tab-content-panel">
              <el-empty description="暂无粉丝关注" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Location, EditPen, SwitchButton, Star, StarFilled, ChatLineRound } from '@element-plus/icons-vue'
import request from '../utils/request'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const user = ref({})
const details = ref({})
const blogs = ref([])
const activeTab = ref('blogs')
const pageLoading = ref(false)

const orders = ref([])
const ordersLoading = ref(false)

// 关注动态流分页
const followBlogs = ref([])
const followLoading = ref(false)
const followFinished = ref(false)
const followParams = reactive({
  minTime: 0,
  offset: 0
})

onMounted(() => {
  queryUserProfile()
})

const queryUserProfile = async () => {
  try {
    pageLoading.value = true
    const res = await request.get('/user/me')
    if (res.code === 200 && res.data) {
      user.value = res.data
      userStore.setUserInfo(res.data)
      queryUserDetails(res.data.id)
      queryMyBlogs()
    }
  } catch (error) {
    console.error(error)
  } finally {
    pageLoading.value = false
  }
}

const queryUserDetails = async (userId) => {
  try {
    const res = await request.get(`/user/info/${userId}`)
    if (res.code === 200 && res.data) {
      details.value = res.data
    }
  } catch (error) {
    console.error(error)
  }
}

const queryMyBlogs = async () => {
  try {
    const res = await request.get('/blog/of/me')
    if (res.code === 200 && res.data) {
      blogs.value = res.data.map(b => ({
        ...b,
        img: b.images ? b.images.split(',')[0] : '/imgs/icons/default-icon.png'
      }))
    }
  } catch (error) {
    console.error(error)
  }
}

// 达人投递流滚动分页加载
const queryBlogsOfFollow = async (clear = false) => {
  if (followLoading.value || (followFinished.value && !clear)) return
  try {
    followLoading.value = true
    if (clear) {
      followParams.offset = 0
      followParams.minTime = Date.now() + 1000
      followBlogs.value = []
      followFinished.value = false
    }

    const res = await request.get('/blog/of/follow', {
      params: {
        offset: followParams.offset,
        lastId: followParams.minTime
      }
    })

    if (res.code === 200 && res.data) {
      const { list, minTime, offset } = res.data
      if (!list || list.length === 0) {
        followFinished.value = true
      } else {
        const mapped = list.map(b => ({
          ...b,
          img: b.images ? b.images.split(',')[0] : '/imgs/icons/default-icon.png'
        }))
        followBlogs.value = clear ? mapped : [...followBlogs.value, ...mapped]
        
        followParams.minTime = minTime
        followParams.offset = offset
      }
    } else {
      followFinished.value = true
    }
  } catch (error) {
    console.error(error)
    followFinished.value = true
  } finally {
    followLoading.value = false
  }
}

const onTabChange = (name) => {
  if (name === 'followBlogs') {
    queryBlogsOfFollow(true)
  } else if (name === 'blogs') {
    queryMyBlogs()
  } else if (name === 'orders') {
    queryMyOrders()
  }
}

const queryMyOrders = async () => {
  try {
    ordersLoading.value = true
    const res = await request.get('/voucher-order/my')
    if (res.code === 200 && res.data) {
      orders.value = res.data
    }
  } catch (error) {
    console.error('Failed to fetch orders:', error)
  } finally {
    ordersLoading.value = false
  }
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  if (Array.isArray(timeStr)) {
    const [y, m, d, h = 0, min = 0, s = 0] = timeStr
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}`
  }
  return timeStr.replace('T', ' ').substring(0, 16)
}

const toggleLike = async (blog) => {
  try {
    await request.put(`/blog/like/${blog.id}`)
    const res = await request.get(`/blog/${blog.id}`)
    if (res.code === 200 && res.data) {
      blog.liked = res.data.liked
      blog.isLike = res.data.isLike
    }
  } catch (error) {
    ElMessage.error('点赞处理失败')
  }
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出当前账户登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
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

const toEdit = () => router.push('/profile-edit')
const toBlogDetail = (id) => router.push(`/blog-detail?id=${id}`)
const toShop = (shopId) => {
  if (shopId) {
    router.push(`/shop-detail?id=${shopId}`)
  } else {
    ElMessage.warning('该优惠券尚未绑定商户')
  }
}
</script>

<style scoped>
.profile-pc-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 顶部封面图 */
.profile-hero-banner {
  height: 200px;
  background: linear-gradient(135deg, #FF9900 0%, #FF6633 100%);
  border-radius: var(--rh-radius-lg);
  position: relative;
  overflow: hidden;
}

.banner-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: url('https://images.unsplash.com/photo-1498837167922-ddd27525d352?q=80&w=1200') no-repeat center/cover;
  opacity: 0.15;
}

/* 仪表盘双栏 */
.profile-dashboard-layout {
  display: grid;
  grid-template-columns: 3fr 8fr; /* 3:8 比例 */
  gap: 24px;
}

/* 左侧栏 */
.left-profile-sidebar {
  display: flex;
  flex-direction: column;
}

.user-info-card {
  background: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30px 20px;
}

.user-info-card:hover {
  transform: none;
}

.avatar-wrapper {
  margin-top: -60px; /* 头像与大背景的叠加溢出效果，极具质感 */
  width: 90px;
  height: 90px;
  border-radius: 50%;
  overflow: hidden;
  border: 4px solid white;
  box-shadow: var(--rh-shadow-md);
  background: #FFF;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.nickname {
  font-size: 18px;
  font-weight: 800;
  color: var(--rh-text-main);
  margin-top: 14px;
}

.location-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  background-color: var(--rh-bg);
  color: var(--rh-text-sub);
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 700;
  margin-top: 8px;
}

.sidebar-divider {
  width: 100%;
  height: 1px;
  background-color: var(--rh-border);
  margin: 20px 0;
}

.introduce {
  font-size: 12px;
  color: var(--rh-text-sub);
  background-color: #FAFAFC;
  padding: 12px 14px;
  border-radius: var(--rh-radius-md);
  width: 100%;
  line-height: 1.6;
  text-align: center;
}

.sidebar-action-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  margin-top: 20px;
}

.sidebar-action-box button {
  width: 100%;
  border-radius: 18px !important;
  font-size: 12px;
  height: 36px;
}

.btn-icon {
  margin-right: 4px;
}

/* 右侧栏 */
.right-main-tabs {
  background: white;
  border-radius: var(--rh-radius-lg);
  padding: 24px;
  border: 1px solid rgba(228, 231, 237, 0.6);
  box-shadow: var(--rh-shadow-subtle);
}

:deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--rh-border);
}

:deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 700;
  padding: 0 20px;
}

.tab-content-panel {
  padding-top: 20px;
}

/* 我的笔记 PC 网格布局 */
.my-blogs-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.blog-mini-card {
  padding: 0;
  overflow: hidden;
  background: #FFF;
  border: 1px solid var(--rh-border);
  cursor: pointer;
}

.blog-mini-card:hover {
  transform: translateY(-2px);
  border-color: var(--rh-primary);
}

.blog-mini-cover {
  width: 100%;
  height: 120px;
  object-fit: cover;
  background-color: #EEE;
}

.blog-mini-body {
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.blog-mini-title {
  font-size: 13px;
  font-weight: 800;
  color: var(--rh-text-main);
  line-height: 1.4;
  height: 36px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.blog-mini-footer {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: var(--rh-text-light);
}

.stats-item {
  display: flex;
  align-items: center;
  gap: 3px;
}

/* 关注达人动态流 (横幅卡片流) */
.feed-list-pc {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.feed-card-pc {
  background: white;
  border: 1px solid var(--rh-border);
  cursor: pointer;
  padding: 20px;
}

.feed-card-pc:hover {
  border-color: var(--rh-primary);
}

.feed-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.feed-author {
  display: flex;
  align-items: center;
  gap: 8px;
}

.feed-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}

.feed-username {
  font-size: 13px;
  font-weight: 800;
  color: var(--rh-text-main);
}

.feed-tag {
  font-weight: 700;
}

/* 左右分流主体 */
.feed-body-split {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
}

.feed-text-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.feed-title {
  font-size: 15px;
  font-weight: 800;
  color: var(--rh-text-main);
  line-height: 1.4;
}

.feed-hint {
  font-size: 12px;
  color: var(--rh-text-light);
}

.feed-image {
  width: 140px;
  height: 90px;
  border-radius: var(--rh-radius-md);
  object-fit: cover;
  background-color: #EEE;
  flex-shrink: 0;
}

.feed-footer {
  margin-top: 14px;
  border-top: 1px solid var(--rh-border);
  padding-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.feed-like-action-btn {
  color: var(--rh-text-sub);
}

.feed-like-action-btn.is-liked {
  color: var(--rh-primary) !important;
}

.load-more-feed-container {
  text-align: center;
  padding: 20px 0;
}

.load-more-feed-btn {
  border-radius: 20px !important;
  font-weight: 700;
}

/* 订单列表卡片样式 */
.my-orders-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  border-radius: var(--rh-radius-md);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--rh-border);
  padding-bottom: 10px;
  margin-bottom: 10px;
}

.order-id {
  font-size: 12px;
  color: var(--rh-text-light);
}

.order-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-info-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.order-action-right {
  flex-shrink: 0;
  margin-left: 16px;
}

.order-title {
  font-size: 16px;
  font-weight: bold;
  color: var(--rh-text-main);
  margin: 0;
}

.order-price {
  font-size: 14px;
}

.pay-value {
  color: #ff4d4f;
  font-weight: bold;
}

.actual-value {
  color: var(--rh-text-sub);
  font-size: 12px;
}

.order-time {
  font-size: 12px;
  color: var(--rh-text-light);
}
</style>
