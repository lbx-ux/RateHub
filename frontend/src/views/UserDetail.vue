<template>
  <div class="profile-pc-page rh-container" v-loading="pageLoading">
    <!-- 顶部大横幅 -->
    <div class="profile-hero-banner">
      <div class="banner-overlay"></div>
    </div>

    <!-- 主版面双栏布局 -->
    <div class="profile-dashboard-layout">
      <!-- 1. 左侧达人基本资料名片 -->
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
            {{ details.introduce || '这个人很懒，什么都没有留下~' }}
          </p>

          <div class="sidebar-action-box">
            <el-button 
              type="primary" 
              :plain="followed"
              :loading="followLoading"
              class="follow-btn"
              @click="handleFollow"
            >
              {{ followed ? '已关注' : '关注达人' }}
            </el-button>
            <el-button type="default" plain class="back-btn" @click="goBack">
              返回上一页
            </el-button>
          </div>
        </div>
      </aside>

      <!-- 2. 右侧达人笔记与共同关注 (Tabs) -->
      <div class="right-main-tabs">
        <el-tabs v-model="activeTab" class="custom-profile-tabs" @tab-change="onTabChange">
          <!-- 达人发布的笔记 -->
          <el-tab-pane label="达人笔记" name="blogs">
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
              
              <el-empty v-else description="该达人尚未发表过任何探店笔记" />
            </div>
          </el-tab-pane>

          <!-- 共同关注 Tab -->
          <el-tab-pane label="共同关注" name="commonFollows">
            <div class="tab-content-panel">
              <div class="common-user-grid" v-if="commonFollows.length > 0">
                <div 
                  v-for="u in commonFollows" 
                  :key="u.id" 
                  class="common-user-card-pc rh-card"
                  @click="toOtherUserDetail(u.id)"
                >
                  <img :src="u.icon || '/imgs/icons/default-icon.png'" alt="Avatar" class="cmt-avatar" />
                  <div class="cmt-meta">
                    <h4>{{ u.nickName }}</h4>
                    <p>你们都关注了该美食达人</p>
                  </div>
                  <el-button type="primary" plain size="small" class="go-user-btn">
                    去主页
                  </el-button>
                </div>
              </div>

              <el-empty v-else-if="!commonLoading" description="暂无共同关注的达人好友" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Location, Star, ChatLineRound } from '@element-plus/icons-vue'
import request from '../utils/request'

const route = useRoute()
const router = useRouter()

const targetUserId = route.query.id
const user = ref({})
const details = ref({})
const blogs = ref([])
const commonFollows = ref([])
const followed = ref(false)
const followLoading = ref(false)
const commonLoading = ref(false)
const pageLoading = ref(false)
const activeTab = ref('blogs')

onMounted(() => {
  if (!targetUserId) {
    ElMessage.error('用户ID失效')
    router.back()
    return
  }
  queryUserProfile()
  checkFollowStatus()
})

const goBack = () => router.back()

const queryUserProfile = async () => {
  try {
    pageLoading.value = true
    const userRes = await request.get(`/user/${targetUserId}`)
    if (userRes.code === 200 && userRes.data) {
      user.value = userRes.data
      queryUserDetails(targetUserId)
      queryUserBlogs()
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

const queryUserBlogs = async () => {
  try {
    const res = await request.get('/blog/of/user', {
      params: {
        id: targetUserId,
        current: 1
      }
    })
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

const checkFollowStatus = async () => {
  const token = sessionStorage.getItem('token')
  if (!token) return
  
  try {
    const res = await request.get(`/follow/or/not/${targetUserId}`)
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
    await request.put(`/follow/${targetUserId}/${targetStatus}`)
    followed.value = targetStatus
    ElMessage.success(targetStatus ? '关注成功' : '取消关注成功')
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    followLoading.value = false
  }
}

const queryCommonFollows = async () => {
  try {
    commonLoading.value = true
    const res = await request.get(`/follow/common/${targetUserId}`)
    if (res.code === 200) {
      commonFollows.value = res.data
    }
  } catch (error) {
    console.error(error)
  } finally {
    commonLoading.value = false
  }
}

const onTabChange = (name) => {
  if (name === 'commonFollows') {
    queryCommonFollows()
  } else if (name === 'blogs') {
    queryUserBlogs()
  }
}

const toOtherUserDetail = (id) => {
  router.push(`/user-detail?id=${id}`)
}

const toBlogDetail = (id) => {
  router.push(`/blog-detail?id=${id}`)
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

/* 双栏 */
.profile-dashboard-layout {
  display: grid;
  grid-template-columns: 3fr 8fr;
  gap: 24px;
}

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
  margin-top: -60px;
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

/* 右栏 */
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

/* 笔记列表 */
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

/* 共同关注列表 */
.common-user-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.common-user-card-pc {
  background: white;
  border: 1px solid var(--rh-border);
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  cursor: pointer;
}

.common-user-card-pc:hover {
  border-color: var(--rh-primary);
}

.cmt-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
}

.cmt-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.cmt-meta h4 {
  font-size: 13px;
  font-weight: 800;
  color: var(--rh-text-main);
}

.cmt-meta p {
  font-size: 11px;
  color: var(--rh-text-light);
}

.go-user-btn {
  border-radius: 14px !important;
  font-size: 11px;
}
</style>
