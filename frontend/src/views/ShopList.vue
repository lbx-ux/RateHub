<template>
  <div class="shop-list-pc-page rh-container">
    <!-- 1. PC 级大搜索框 -->
    <section class="search-hero-card rh-card">
      <div class="search-box-inner">
        <h2 class="search-title">探索城市热门好店</h2>
        <div class="input-row">
          <el-input
            v-model="searchVal"
            placeholder="输入想要寻找的商户名称...（按回车搜索）"
            clearable
            @keyup.enter="onSearch"
            class="pc-search-bar"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" class="search-confirm-btn" @click="onSearch">
            搜索商家
          </el-button>
        </div>
      </div>
    </section>

    <!-- 2. PC 条件过滤横幅 -->
    <section class="filter-panel rh-card">
      <!-- 2.1 分类行 -->
      <div class="filter-row">
        <span class="row-label">商家分类：</span>
        <div class="row-items">
          <span 
            v-for="t in types" 
            :key="t.id"
            class="filter-tag"
            :class="{ 'active': Number(params.typeId) === t.id }"
            @click="selectType(t)"
          >
            {{ t.name }}
          </span>
        </div>
      </div>
      
      <div class="divider"></div>

      <!-- 2.2 排序行 -->
      <div class="filter-row">
        <span class="row-label">智能排序：</span>
        <div class="row-items">
          <span 
            class="filter-tag"
            :class="{ 'active': params.sortBy === '' }"
            @click="changeSort('')"
          >
            距离最近
          </span>
          <span 
            class="filter-tag"
            :class="{ 'active': params.sortBy === 'comments' }"
            @click="changeSort('comments')"
          >
            人气最高 (评价数)
          </span>
          <span 
            class="filter-tag"
            :class="{ 'active': params.sortBy === 'score' }"
            @click="changeSort('score')"
          >
            好评优先 (评分)
          </span>
        </div>
      </div>
    </section>

    <!-- 3. PC 宽幅商户列表与右侧推荐栏 (双栏布局) -->
    <div class="main-split-layout">
      <!-- 左侧商铺主列表 -->
      <div class="left-list-side">
        <div class="shop-list-flow" v-loading="loading">
          <div 
            v-for="s in shops" 
            :key="s.id" 
            class="pc-shop-card rh-card"
            @click="toDetail(s.id)"
          >
            <!-- 商家大图 -->
            <img :src="s.images" alt="Shop img" class="shop-card-img" />
            
            <!-- 商家主体信息 -->
            <div class="shop-card-main">
              <div class="name-line">
                <h3 class="shop-card-name">{{ s.name }}</h3>
                <span class="shop-card-tag">{{ typeName }}</span>
              </div>

              <div class="rating-line">
                <el-rate 
                  v-model="s.rating" 
                  disabled 
                  allow-half 
                  colors="#FF6633" 
                  size="14px"
                />
                <span class="score-text">{{ (s.score / 10).toFixed(1) }}分</span>
                <span class="comments-count">{{ s.comments }}条网友点评</span>
              </div>

              <div class="meta-line">
                <span class="price">人均消费 <strong>￥{{ s.avgPrice }}</strong></span>
                <span class="separator">|</span>
                <span class="area">{{ s.area }}</span>
                <span class="address" :title="s.address">{{ s.address }}</span>
              </div>

              <!-- PC版附赠促销标签 -->
              <div class="promotion-row">
                <span class="promo-badge voucher">券</span>
                <span class="promo-text">店内有超值代金券及限量秒杀正在抢购中</span>
              </div>
            </div>

            <!-- 最右侧操作栏 -->
            <div class="shop-card-right">
              <div class="distance-info" v-if="s.distance">
                <el-icon><Location /></el-icon>
                <span>{{ formatDistance(s.distance) }}</span>
              </div>
              <el-button type="primary" class="enter-shop-btn">
                进入商家
              </el-button>
            </div>
          </div>

          <!-- 空数据状态 -->
          <el-empty v-if="shops.length === 0 && !loading" description="未找到符合条件的商家，换个筛选条件试试吧" />

          <!-- 加载更多 -->
          <div class="load-more-container" v-if="shops.length > 0">
            <el-button 
              v-if="!finished" 
              :loading="loading"
              @click="loadMore"
              class="load-more-btn"
            >
              加载更多商家
            </el-button>
            <span class="finished-text" v-else>已为您加载全部商家</span>
          </div>
        </div>
      </div>

      <!-- 右侧侧边栏 (PC 专属推荐) -->
      <aside class="right-recommend-side">
        <div class="recommend-card rh-card">
          <h3>精品好店推荐</h3>
          <div class="recommend-divider"></div>
          <div class="recommend-list">
            <div class="rec-item" @click="toDetail(2)">
              <img src="https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?q=80&w=150" alt="Rec" />
              <div class="rec-meta">
                <h4>老杭州弄堂面馆</h4>
                <p>拱墅区 · 人均￥22</p>
              </div>
            </div>
            <div class="rec-item" @click="toDetail(3)">
              <img src="https://images.unsplash.com/photo-1552566626-52f8b828add9?q=80&w=150" alt="Rec" />
              <div class="rec-meta">
                <h4>江南小灶私房菜</h4>
                <p>西湖区 · 人均￥85</p>
              </div>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const route = useRoute()
const router = useRouter()

const typeName = ref(route.query.name || '美食')
const searchVal = ref('')
const types = ref([])
const shops = ref([])
const loading = ref(false)
const finished = ref(false)

const params = reactive({
  typeId: route.query.type || '1',
  current: 1,
  sortBy: '',
  x: 120.149993,
  y: 30.334229
})

onMounted(() => {
  // 获取 GPS 定位
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        params.x = pos.coords.longitude
        params.y = pos.coords.latitude
        queryShops()
      },
      () => {
        queryShops()
      }
    )
  } else {
    queryShops()
  }
  queryTypes()
  
  // 绑定滚动加载 (PC)
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

const queryShops = async (clear = false) => {
  if (loading.value) return
  try {
    loading.value = true
    if (clear) {
      params.current = 1
      shops.value = []
      finished.value = false
    }
    const res = await request.get('/shop/of/type', { params })
    if (res.code === 200 && res.data) {
      const data = res.data
      if (data.length === 0) {
        finished.value = true
      } else {
        const mapped = data.map(s => ({
          ...s,
          rating: s.score ? s.score / 10 : 0,
          images: s.images ? s.images.split(',')[0] : '/imgs/icons/default-icon.png'
        }))
        shops.value = [...shops.value, ...mapped]
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

const selectType = (t) => {
  params.typeId = t.id
  typeName.value = t.name
  queryShops(true)
}

const changeSort = (sortField) => {
  params.sortBy = sortField
  queryShops(true)
}

const loadMore = () => {
  params.current++
  queryShops()
}

const handleWindowScroll = () => {
  const { scrollTop, scrollHeight, clientHeight } = document.documentElement
  if (scrollTop + clientHeight >= scrollHeight - 50 && !loading.value && !finished.value) {
    loadMore()
  }
}

const onSearch = async () => {
  if (!searchVal.value.trim()) return
  try {
    const res = await request.get(`/shop/of/name?name=${encodeURIComponent(searchVal.value)}`)
    if (res.code === 200 && res.data && res.data.length > 0) {
      router.push(`/shop-detail?id=${res.data[0].id}`)
    } else {
      ElMessage.warning('未找到相关商户')
    }
  } catch (error) {
    console.error(error)
  }
}

const toDetail = (id) => {
  router.push(`/shop-detail?id=${id}`)
}

const formatDistance = (d) => {
  if (d < 1000) {
    return `${d.toFixed(0)}m`
  }
  return `${(d / 1000).toFixed(1)}km`
}
</script>

<style scoped>
.shop-list-pc-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 顶部搜索框 */
.search-hero-card {
  background: linear-gradient(135deg, #FFEFE8 0%, #FFFFFF 100%);
  padding: 30px;
}

.search-hero-card:hover {
  transform: none;
}

.search-box-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  max-width: 700px;
  margin: 0 auto;
}

.search-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--rh-text-main);
}

.input-row {
  display: flex;
  width: 100%;
  gap: 12px;
}

.pc-search-bar {
  flex: 1;
}

:deep(.pc-search-bar .el-input__wrapper) {
  border-radius: 24px;
  padding: 12px 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03) !important;
}

.search-confirm-btn {
  height: 48px;
  border-radius: 24px !important;
  font-weight: 700;
  padding: 0 28px;
  box-shadow: 0 4px 12px rgba(255, 102, 51, 0.2);
}

/* 条件过滤面板 */
.filter-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 24px;
}

.filter-panel:hover {
  transform: none;
}

.filter-row {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.row-label {
  font-weight: 700;
  color: var(--rh-text-main);
  width: 90px;
  flex-shrink: 0;
}

.row-items {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-tag {
  padding: 6px 16px;
  border-radius: 16px;
  color: var(--rh-text-sub);
  cursor: pointer;
  font-weight: 500;
  transition: var(--rh-transition);
}

.filter-tag:hover {
  color: var(--rh-primary);
}

.filter-tag.active {
  background-color: var(--rh-primary);
  color: white;
  font-weight: 700;
}

.divider {
  height: 1px;
  background-color: var(--rh-border);
}

/* 双栏布局 */
.main-split-layout {
  display: grid;
  grid-template-columns: 8fr 3fr; /* PC端经典左右分栏 */
  gap: 24px;
}

/* 左边主栏 */
.left-list-side {
  display: flex;
  flex-direction: column;
}

.shop-list-flow {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* PC横向商户卡片 */
.pc-shop-card {
  display: flex;
  gap: 20px;
  padding: 20px;
  background: #FFF;
  cursor: pointer;
}

.shop-card-img {
  width: 140px;
  height: 140px;
  border-radius: var(--rh-radius-lg);
  object-fit: cover;
  flex-shrink: 0;
  background-color: #EEE;
}

.shop-card-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.name-line {
  display: flex;
  align-items: center;
  gap: 12px;
}

.shop-card-name {
  font-size: 18px;
  font-weight: 800;
  color: var(--rh-text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shop-card-tag {
  background: var(--rh-primary-light);
  color: var(--rh-primary);
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
}

.rating-line {
  display: flex;
  align-items: center;
  gap: 6px;
}

.score-text {
  font-size: 13px;
  font-weight: 700;
  color: var(--rh-primary);
}

.comments-count {
  font-size: 12px;
  color: var(--rh-text-light);
}

.meta-line {
  font-size: 13px;
  color: var(--rh-text-sub);
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
}

.meta-line .price strong {
  color: var(--rh-text-main);
  font-size: 15px;
}

.meta-line .separator {
  color: var(--rh-border);
}

.meta-line .address {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  color: var(--rh-text-light);
}

.promotion-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.promo-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 4px;
  border-radius: 4px;
}

.promo-badge.voucher {
  background-color: #FFF3E0;
  color: #E65100;
  border: 1.5px solid #FFB74D;
}

.promo-text {
  font-size: 12px;
  color: var(--rh-text-sub);
}

/* PC卡片最右侧区域 */
.shop-card-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  border-left: 1px dashed var(--rh-border);
  padding-left: 20px;
  flex-shrink: 0;
  min-width: 120px;
}

.distance-info {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--rh-primary);
  font-weight: 700;
  font-size: 13px;
}

.enter-shop-btn {
  border-radius: 16px !important;
  font-size: 12px;
}

/* 加载更多 */
.load-more-container {
  text-align: center;
  padding: 24px 0;
}

.load-more-btn {
  border-radius: 20px !important;
  font-weight: 700;
}

.finished-text {
  font-size: 13px;
  color: var(--rh-text-light);
}

/* 右边侧栏推荐卡片 */
.recommend-card {
  position: sticky;
  top: 84px; /* header(64px) + margin(20px) */
  background: white;
  padding: 20px;
}

.recommend-card h3 {
  font-size: 15px;
  font-weight: 800;
}

.recommend-divider {
  height: 1px;
  background-color: var(--rh-border);
  margin: 12px 0;
}

.recommend-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.rec-item {
  display: flex;
  gap: 12px;
  cursor: pointer;
  transition: var(--rh-transition);
}

.rec-item:hover {
  transform: translateX(4px);
}

.rec-item img {
  width: 54px;
  height: 54px;
  border-radius: var(--rh-radius-md);
  object-fit: cover;
  background-color: #EEE;
}

.rec-meta {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
}

.rec-meta h4 {
  font-size: 13px;
  font-weight: 800;
  color: var(--rh-text-main);
}

.rec-meta p {
  font-size: 11px;
  color: var(--rh-text-light);
}
</style>
