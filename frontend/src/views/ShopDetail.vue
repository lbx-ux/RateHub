<template>
  <div class="shop-detail-pc-page rh-container" v-loading="pageLoading">
    <!-- 面包屑导航 -->
    <div class="breadcrumb-container">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/shop-list' }">商户探索</el-breadcrumb-item>
        <el-breadcrumb-item>{{ shop.name || '商户详情' }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 主体双栏布局 -->
    <div class="detail-split-layout">
      <!-- 左侧主要信息栏 -->
      <div class="left-main-side">
        <!-- 1. 商家头牌卡片 -->
        <section class="shop-hero-card rh-card">
          <div class="hero-header">
            <h1 class="shop-name">{{ shop.name }}</h1>
            <div class="share-btn-box">
              <el-button :icon="Share" circle @click="handleShare" />
            </div>
          </div>

          <div class="rate-line">
            <el-rate v-model="shopRating" disabled colors="#FF6633" size="16px" />
            <span class="score-text">{{ (shop.score / 10).toFixed(1) }}分</span>
            <span class="comments-count">{{ shop.comments || 0 }}条网友点评</span>
            <span class="price-val">人均 ￥{{ shop.avgPrice || 0 }}</span>
          </div>

          <div class="details-row">
            <span>口味分: 4.8</span>
            <span>环境分: 4.7</span>
            <span>服务分: 4.8</span>
            <span class="tag-badge">{{ shop.area }}好评榜第3名</span>
          </div>

          <!-- 招牌相册 (多图并列展示) -->
          <div class="shop-images-gallery">
            <div 
              v-for="(img, index) in shopImages.slice(0, 3)" 
              :key="index"
              class="gallery-image-wrapper"
            >
              <el-image 
                :src="img" 
                fit="cover" 
                :preview-src-list="shopImages"
                class="gallery-img"
              />
            </div>
          </div>
        </section>

        <!-- 2. 代金券和限时秒杀特惠卡片 (Vouchers) -->
        <section class="vouchers-section rh-card">
          <div class="section-header">
            <el-icon class="section-icon"><Ticket /></el-icon>
            <h3>优惠促销与秒杀特惠</h3>
          </div>

          <div class="voucher-grid-pc">
            <div 
              v-for="v in vouchers" 
              :key="v.id" 
              class="pc-voucher-card"
              :class="{ 'is-seckill': v.type === 1 }"
            >
              <!-- 券左侧：大额折扣信息 -->
              <div class="v-left-discount">
                <span class="discount-value">{{ (v.payValue / 100).toFixed(0) }}</span>
                <span class="discount-unit">元</span>
              </div>

              <!-- 券中部：信息详情 -->
              <div class="v-middle-info">
                <div class="v-badge" :class="v.type === 1 ? 'type-seckill' : 'type-normal'">
                  {{ v.type === 1 ? '限时秒杀' : '代金券' }}
                </div>
                <h4 class="v-title">{{ v.title }}</h4>
                <p class="v-subtitle">{{ v.subTitle }}</p>
                <div class="v-price-row">
                  <span class="v-price-original">原价 ￥{{ (v.actualValue / 100).toFixed(0) }}</span>
                  <span class="v-discount-ratio">{{ ((v.payValue / v.actualValue) * 10).toFixed(1) }} 折优惠</span>
                </div>
              </div>

              <!-- 券右侧：抢购状态与动作 -->
              <div class="v-right-action">
                <!-- 普通券直接购买 -->
                <div v-if="v.type === 0" class="normal-panel">
                  <el-button type="primary" class="voucher-action-btn" @click="buyNormalVoucher(v)">
                    立即购买
                  </el-button>
                </div>

                <!-- 秒杀券核心交互 -->
                <div v-else class="seckill-panel">
                  <div class="seckill-stock">剩余 <strong>{{ v.stock }}</strong> 张</div>
                  <el-button 
                    type="primary" 
                    class="voucher-action-btn seckill-action-btn"
                    :disabled="isNotBegin(v) || isEnd(v) || v.stock < 1"
                    @click="handleSeckill(v)"
                  >
                    {{ getSeckillBtnText(v) }}
                  </el-button>
                  <span class="seckill-time-hint" :title="formatTimeRange(v)">
                    {{ formatTimeRange(v) }}
                  </span>
                </div>
              </div>
            </div>
            
            <el-empty 
              v-if="vouchers.length === 0" 
              description="该商户当前暂无可用促销代金券" 
              image-size="80px"
            />
          </div>
        </section>

        <!-- 3. 精美评价面板 -->
        <section class="comments-section rh-card">
          <div class="section-header">
            <el-icon class="section-icon"><ChatLineRound /></el-icon>
            <h3>网友点评 (119条)</h3>
          </div>

          <div class="pc-comment-list">
            <div class="comment-item">
              <div class="cmt-author-line">
                <img src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=100" alt="Avatar" class="cmt-avatar" />
                <div class="cmt-author-meta">
                  <h4>叶小乙</h4>
                  <el-rate v-model="cmtRate" disabled colors="#FF6633" size="12px" />
                </div>
                <span class="cmt-date">2026年07月03日 12:30</span>
              </div>
              <p class="cmt-text text-pretty">
                这家店味道确实很赞！特别是牛肉口感极佳。在 RateHub 抢到了秒杀优惠券，便宜了好多，简直是工作餐的最佳配置。环境雅致、停车也很方便，下次还会带朋友一起来。
              </p>
              <div class="cmt-images">
                <el-image src="https://images.unsplash.com/photo-1544025162-d76694265947?q=80&w=200" fit="cover" class="cmt-img" />
                <el-image src="https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?q=80&w=200" fit="cover" class="cmt-img" />
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- 右侧侧边商家信息栏 -->
      <aside class="right-meta-side">
        <!-- 营业时间卡片 -->
        <div class="meta-info-card rh-card">
          <div class="meta-title">
            <el-icon class="title-icon"><Clock /></el-icon>
            <h3>营业状态</h3>
          </div>
          <div class="meta-divider"></div>
          <div class="meta-item">
            <span class="label">营业时间：</span>
            <span class="val">{{ shop.openHours || '10:00 - 22:00' }}</span>
          </div>
        </div>

        <!-- 商家地址与电话 -->
        <div class="meta-info-card rh-card address-card">
          <div class="meta-title">
            <el-icon class="title-icon"><LocationInformation /></el-icon>
            <h3>商家地址</h3>
          </div>
          <div class="meta-divider"></div>
          <div class="meta-item">
            <span class="label">详细地址：</span>
            <span class="val address-val">{{ shop.address }}</span>
          </div>
          <div class="mock-map">
            <div class="map-indicator">
              <el-icon class="map-marker"><LocationFilled /></el-icon>
              <span>RateHub 地图定位已标记</span>
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
import { Share, Ticket, ChatLineRound, Clock, LocationInformation, LocationFilled } from '@element-plus/icons-vue'
import request from '../utils/request'

const route = useRoute()
const router = useRouter()

const shopId = route.query.id
const shop = ref({})
const vouchers = ref([])
const pageLoading = ref(false)
const cmtRate = ref(5)

const shopRating = computed(() => {
  return shop.value.score ? shop.value.score / 10 : 0
})

const shopImages = computed(() => {
  if (shop.value.images) {
    return shop.value.images.split(',')
  }
  return ['/imgs/icons/default-icon.png']
})

onMounted(() => {
  if (!shopId) {
    ElMessage.error('商户ID失效')
    router.back()
    return
  }
  queryShopDetail()
  queryVouchers()
})

const goBack = () => router.back()

const queryShopDetail = async () => {
  try {
    pageLoading.value = true
    const res = await request.get(`/shop/${shopId}`)
    if (res.code === 200) {
      shop.value = res.data
    }
  } catch (error) {
    console.error(error)
  } finally {
    pageLoading.value = false
  }
}

const queryVouchers = async () => {
  try {
    const res = await request.get(`/voucher/list/${shopId}`)
    if (res.code === 200) {
      vouchers.value = res.data
    }
  } catch (error) {
    console.error(error)
  }
}

const buyNormalVoucher = (v) => {
  ElMessage.info(`普通代金券支付接口联调中，券ID: ${v.id}`)
}

// 秒杀抢购
const isNotBegin = (v) => {
  return new Date(v.beginTime).getTime() > Date.now()
}

const isEnd = (v) => {
  return new Date(v.endTime).getTime() < Date.now()
}

const getSeckillBtnText = (v) => {
  if (isNotBegin(v)) return '尚未开始'
  if (isEnd(v)) return '抢购已结束'
  if (v.stock < 1) return '已抢光'
  return '立即抢购'
}

const handleSeckill = async (v) => {
  const token = sessionStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
    return
  }

  if (isNotBegin(v)) {
    ElMessage.warning('抢购活动尚未开始！')
    return
  }
  if (isEnd(v)) {
    ElMessage.warning('抢购活动已经结束！')
    return
  }
  if (v.stock < 1) {
    ElMessage.warning('该优惠券已售罄！')
    return
  }

  try {
    const res = await request.post(`/voucher-order/seckill/${v.id}`)
    if (res.code === 200) {
      ElMessageBox.alert(`恭喜您，优惠券秒杀成功！创建订单 ID: ${res.data}`, '秒杀成功', {
        confirmButtonText: '查看我的订单',
        type: 'success'
      })
      queryVouchers()
    }
  } catch (error) {
    ElMessage.error(error || '抢购失败，系统繁忙')
  }
}

const formatTimeRange = (v) => {
  const b = new Date(v.beginTime)
  const pad = (num) => String(num).padStart(2, '0')
  return `${b.getMonth() + 1}月${b.getDate()}日 ${pad(b.getHours())}:${pad(b.getMinutes())} 开始`
}

const handleShare = () => {
  if (navigator.clipboard) {
    navigator.clipboard.writeText(window.location.href)
    ElMessage.success('商户链接已成功复制至剪贴板，快分享给好友吧！')
  } else {
    ElMessage.info('当前页面地址：' + window.location.href)
  }
}
</script>

<style scoped>
.shop-detail-pc-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.breadcrumb-container {
  padding: 8px 0;
}

/* PC 主体双栏 */
.detail-split-layout {
  display: grid;
  grid-template-columns: 8fr 3fr;
  gap: 24px;
}

/* 左侧栏 */
.left-main-side {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.shop-hero-card {
  background: white;
}

.shop-hero-card:hover {
  transform: none;
}

.hero-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.shop-name {
  font-size: 24px;
  font-weight: 800;
  color: var(--rh-text-main);
}

.rate-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.score-text {
  font-size: 15px;
  font-weight: 800;
  color: var(--rh-primary);
}

.comments-count {
  font-size: 13px;
  color: var(--rh-text-light);
}

.price-val {
  font-size: 14px;
  font-weight: 700;
  color: var(--rh-text-sub);
  margin-left: 10px;
}

.details-row {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: var(--rh-text-sub);
  margin-top: 12px;
}

.tag-badge {
  background: var(--rh-primary-light);
  color: var(--rh-primary);
  font-size: 11px;
  font-weight: 700;
  padding: 2px 10px;
  border-radius: 4px;
  margin-left: auto;
}

/* 招牌相册平铺 */
.shop-images-gallery {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 24px;
}

.gallery-image-wrapper {
  height: 160px;
  border-radius: var(--rh-radius-lg);
  overflow: hidden;
  box-shadow: var(--rh-shadow-subtle);
  border: 1px solid var(--rh-border);
}

.gallery-img {
  width: 100%;
  height: 100%;
  cursor: pointer;
  transition: transform 0.4s;
}

.gallery-img:hover {
  transform: scale(1.05);
}

/* 优惠促销卡片 (PC 专用横幅宽券) */
.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--rh-border);
  padding-bottom: 12px;
}

.section-icon {
  font-size: 20px;
  color: var(--rh-primary);
}

.section-header h3 {
  font-size: 17px;
  font-weight: 800;
  color: var(--rh-text-main);
}

.vouchers-section :deep(.el-empty) {
  padding: 10px 0;
}

.vouchers-section:hover {
  transform: none;
}

.voucher-grid-pc {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 券的主体设计 */
.pc-voucher-card {
  background-color: #FAFAFC;
  border-radius: 12px;
  border: 1px solid var(--rh-border);
  display: flex;
  overflow: hidden;
  align-items: center;
  height: 110px;
  transition: var(--rh-transition);
}

.pc-voucher-card:hover {
  border-color: var(--rh-primary);
  box-shadow: var(--rh-shadow-subtle);
}

.pc-voucher-card.is-seckill {
  border-left: 4px solid var(--rh-primary);
}

/* 折扣块 */
.v-left-discount {
  width: 130px;
  background-color: #FFF5F2;
  color: var(--rh-primary);
  display: flex;
  align-items: baseline;
  justify-content: center;
  height: 100%;
  border-right: 1px dashed var(--rh-border);
  flex-shrink: 0;
}

.discount-value {
  font-size: 40px;
  font-weight: 900;
}

.discount-unit {
  font-size: 14px;
  font-weight: 700;
  margin-left: 2px;
}

/* 核心信息 */
.v-middle-info {
  flex: 1;
  padding: 0 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.v-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
  align-self: flex-start;
}

.v-badge.type-seckill {
  background-color: var(--rh-primary-light);
  color: var(--rh-primary);
}

.v-badge.type-normal {
  background-color: #E8F5E9;
  color: #2E7D32;
}

.v-title {
  font-size: 15px;
  font-weight: 800;
  color: var(--rh-text-main);
}

.v-subtitle {
  font-size: 12px;
  color: var(--rh-text-light);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.v-price-row {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 11px;
}

.v-price-original {
  color: var(--rh-text-light);
  text-decoration: line-through;
}

.v-discount-ratio {
  color: var(--rh-accent);
  font-weight: 700;
}

/* 右边抢购按钮 */
.v-right-action {
  width: 160px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-left: 1px dashed var(--rh-border);
  height: 100%;
  flex-shrink: 0;
}

.voucher-action-btn {
  border-radius: 20px !important;
  font-weight: 700;
  padding: 8px 20px;
}

.seckill-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.seckill-stock {
  font-size: 11px;
  color: var(--rh-text-sub);
}

.seckill-stock strong {
  color: var(--rh-primary);
  font-size: 13px;
}

.seckill-time-hint {
  font-size: 10px;
  color: var(--rh-text-light);
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 评论区 */
.comments-section:hover {
  transform: none;
}

.comment-item {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.cmt-author-line {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cmt-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  object-fit: cover;
}

.cmt-author-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.cmt-author-meta h4 {
  font-size: 13px;
  font-weight: 800;
}

.cmt-date {
  font-size: 12px;
  color: var(--rh-text-light);
}

.cmt-text {
  font-size: 13px;
  color: #333;
  line-height: 1.6;
  padding-left: 50px;
}

.cmt-images {
  display: flex;
  gap: 12px;
  padding-left: 50px;
}

.cmt-img {
  width: 90px;
  height: 90px;
  border-radius: var(--rh-radius-md);
  box-shadow: var(--rh-shadow-subtle);
}

/* 右侧侧栏 */
.right-meta-side {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.meta-info-card {
  background: white;
  padding: 20px;
}

.meta-info-card:hover {
  transform: none;
}

.meta-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  font-size: 16px;
  color: var(--rh-primary);
}

.meta-title h3 {
  font-size: 14px;
  font-weight: 800;
}

.meta-divider {
  height: 1px;
  background-color: var(--rh-border);
  margin: 12px 0;
}

.meta-item {
  font-size: 13px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-item .label {
  color: var(--rh-text-light);
}

.meta-item .val {
  font-weight: 700;
  color: var(--rh-text-main);
}

/* 模拟地图 */
.mock-map {
  margin-top: 14px;
  height: 120px;
  background-color: #E8F5E9;
  border-radius: var(--rh-radius-md);
  border: 1px solid #C8E6C9;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2E7D32;
  font-size: 12px;
  font-weight: 700;
}

.map-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.map-marker {
  font-size: 24px;
  color: #D32F2F;
}
</style>
