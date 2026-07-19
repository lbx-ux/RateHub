<template>
  <div class="blog-edit-pc-page rh-container">
    <!-- 面包屑导航 -->
    <div class="breadcrumb-container">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>发布探店日记</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 主编辑框 -->
    <div class="edit-main-layout">
      <div class="edit-form-card rh-card">
        <h2 class="form-title">分享你的探店日记</h2>
        <div class="form-divider"></div>

        <el-form :model="params" label-position="top">
          <!-- 标题 -->
          <el-form-item label="日记标题">
            <el-input 
              v-model="params.title" 
              placeholder="起个吸引人的标题吧，最多30字~"
              maxlength="30"
              show-word-limit
              class="title-input"
            />
          </el-form-item>

          <!-- 图片上传 -->
          <el-form-item label="上传照片 (支持上传多张，第一张为封面)">
            <input 
              type="file" 
              accept="image/*" 
              ref="fileInput" 
              style="display: none" 
              @change="onFileSelected" 
            />
            
            <div class="upload-grid">
              <!-- 拖动/点击上传块 -->
              <div class="upload-trigger-btn" @click="triggerFileInput" v-if="fileList.length < 9">
                <el-icon class="upload-icon"><Plus /></el-icon>
                <span>点击上传照片</span>
              </div>

              <!-- 图片展板 -->
              <div 
                v-for="(img, idx) in fileList" 
                :key="idx" 
                class="img-preview-box"
              >
                <el-image :src="img" fit="cover" class="preview-img" :preview-src-list="fileList" />
                <div class="badge-cover-tag" v-if="idx === 0">封面</div>
                <div class="delete-btn-mask" @click="deletePic(idx)">
                  <el-icon><Delete /></el-icon>
                </div>
              </div>
            </div>
            <p class="upload-hint">支持 JPG/PNG 格式，单张图片不超过 5MB，最多可上传 9 张照片。</p>
          </el-form-item>

          <!-- 正文 -->
          <el-form-item label="体验详情">
            <el-input 
              v-model="params.content"
              type="textarea"
              placeholder="最近打卡了什么地方？菜品味道如何？服务和环境有什么特别的？来跟小伙伴们好好分享吧！"
              :rows="8"
              maxlength="1000"
              show-word-limit
            />
          </el-form-item>

          <!-- 关联商户 -->
          <el-form-item label="关联商户">
            <div class="shop-association-row">
              <div class="associated-info" v-if="selectedShop.name">
                <el-icon class="shop-marker-icon"><Shop /></el-icon>
                <div class="text-meta">
                  <span class="s-name">{{ selectedShop.name }}</span>
                  <span class="s-addr">{{ selectedShop.address }}</span>
                </div>
                <span class="s-area">{{ selectedShop.area }}</span>
              </div>
              <span class="empty-association" v-else>暂无关联商户，请点击右侧按钮选择</span>
              
              <el-button type="primary" plain @click="showShopDialog = true">
                {{ selectedShop.name ? '重新选择' : '去选择商户' }}
              </el-button>
            </div>
          </el-form-item>

          <!-- 操作条 -->
          <div class="action-footer-line">
            <el-button @click="goBack" class="cancel-btn">取消</el-button>
            <el-button type="primary" :loading="submitLoading" class="submit-btn" @click="submitBlog">
              确认发布日记
            </el-button>
          </div>
        </el-form>
      </div>
    </div>

    <!-- 关联商户 PC 对话弹窗 (ElDialog) -->
    <el-dialog 
      v-model="showShopDialog" 
      title="关联商户" 
      width="560px"
      align-center
      class="custom-shop-dialog"
    >
      <div class="dialog-body">
        <el-search-bar>
          <el-input
            v-model="shopNameQuery"
            placeholder="请输入商户名称进行精确/模糊搜索"
            clearable
            @keyup.enter="queryShops"
            @update:model-value="onSearchQueryChange"
            class="dialog-search"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-search-bar>

        <div class="shop-list-container" v-loading="dialogLoading">
          <div 
            v-for="s in shops" 
            :key="s.id" 
            class="dialog-shop-item"
            :class="{ 'active': selectedShop.id === s.id }"
            @click="selectShop(s)"
          >
            <div class="shop-info">
              <h4>{{ s.name }}</h4>
              <p>{{ s.address }}</p>
            </div>
            <span class="shop-area-tag">{{ s.area }}</span>
          </div>

          <el-empty v-if="shops.length === 0 && !dialogLoading" description="未搜索到匹配商家" image-size="60px" />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Shop, Search } from '@element-plus/icons-vue'
import request from '../utils/request'

const router = useRouter()

const fileInput = ref(null)
const fileList = ref([])
const showShopDialog = ref(false)
const shopNameQuery = ref('')
const shops = ref([])
const selectedShop = ref({})
const submitLoading = ref(false)
const dialogLoading = ref(false)

const params = reactive({
  title: '',
  content: '',
  shopId: null
})

onMounted(() => {
  checkLogin()
  queryShops()
})

const goBack = () => router.back()

const checkLogin = () => {
  const token = sessionStorage.getItem('token')
  if (!token) {
    ElMessage.warning('发日记前请先完成登录')
    router.replace('/login')
  }
}

// 模糊查询商户
const queryShops = async () => {
  try {
    dialogLoading.value = true
    const res = await request.get(`/shop/of/name?name=${encodeURIComponent(shopNameQuery.value)}`)
    if (res.code === 200) {
      shops.value = res.data
    }
  } catch (error) {
    console.error(error)
  } finally {
    dialogLoading.value = false
  }
}

let debounceTimer = null
const onSearchQueryChange = () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    queryShops()
  }, 400)
}

const selectShop = (s) => {
  selectedShop.value = s
  params.shopId = s.id
  showShopDialog.value = false
}

// 选择文件
const triggerFileInput = () => {
  fileInput.value.click()
}

const onFileSelected = async () => {
  const file = fileInput.value.files[0]
  if (!file) return

  const formData = new FormData()
  formData.append('file', file)

  try {
    const res = await request.post('/upload/blog', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    if (res.code === 200 && res.data) {
      const completePath = '/imgs' + res.data
      fileList.value.push(completePath)
      ElMessage.success('图片上传成功')
    }
  } catch (error) {
    ElMessage.error(error || '图片上传失败')
  } finally {
    fileInput.value.value = ''
  }
}

const deletePic = async (index) => {
  const imgUrl = fileList.value[index]
  const nameParam = imgUrl.startsWith('/imgs') ? imgUrl.replace('/imgs', '') : imgUrl

  try {
    const res = await request.get(`/upload/blog/delete?name=${encodeURIComponent(nameParam)}`)
    if (res.code === 200) {
      fileList.value.splice(index, 1)
      ElMessage.success('删除成功')
    }
  } catch (error) {
    // 降级处理：无论后端删没删成功，前端在展示层直接去掉
    fileList.value.splice(index, 1)
  }
}

const submitBlog = async () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请至少上传一张照片作为封面图')
    return
  }
  if (!params.title.trim()) {
    ElMessage.warning('请填写日记标题')
    return
  }
  if (!params.content.trim()) {
    ElMessage.warning('请填写日记的正文详情')
    return
  }
  if (!params.shopId) {
    ElMessage.warning('请选择要关联的商户')
    return
  }

  try {
    submitLoading.value = true
    const submitData = {
      title: params.title,
      content: params.content,
      shopId: params.shopId,
      images: fileList.value.join(',')
    }
    const res = await request.post('/blog', submitData)
    if (res.code === 200) {
      ElMessage.success('发布成功！')
      setTimeout(() => {
        router.replace('/profile')
      }, 600)
    }
  } catch (error) {
    ElMessage.error(error || '发布失败')
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped>
.blog-edit-pc-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.breadcrumb-container {
  padding: 8px 0;
}

/* 主表单 */
.edit-main-layout {
  display: flex;
  justify-content: center;
}

.edit-form-card {
  width: 100%;
  max-width: 800px;
  background: white;
  padding: 30px 40px;
}

.edit-form-card:hover {
  transform: none;
}

.form-title {
  font-size: 20px;
  font-weight: 800;
  color: var(--rh-text-main);
}

.form-divider {
  height: 1px;
  background-color: var(--rh-border);
  margin: 16px 0 24px 0;
}

.title-input :deep(.el-input__wrapper) {
  padding: 10px 14px;
}

/* 图片上传格子网 */
.upload-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr); /* PC端排五列 */
  gap: 14px;
  width: 100%;
}

.upload-trigger-btn {
  height: 110px;
  border: 1.5px dashed var(--rh-border);
  border-radius: 8px;
  background-color: #FAFAFC;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--rh-text-light);
  gap: 8px;
  cursor: pointer;
  transition: var(--rh-transition);
}

.upload-trigger-btn:hover {
  border-color: var(--rh-primary);
  background-color: var(--rh-primary-light);
  color: var(--rh-primary);
}

.upload-icon {
  font-size: 20px;
}

.upload-trigger-btn span {
  font-size: 11px;
  font-weight: 700;
}

.img-preview-box {
  position: relative;
  height: 110px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--rh-border);
}

.preview-img {
  width: 100%;
  height: 100%;
}

.badge-cover-tag {
  position: absolute;
  top: 6px;
  left: 6px;
  background-color: var(--rh-primary);
  color: white;
  font-size: 9px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 4px;
}

.delete-btn-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
  opacity: 0;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.img-preview-box:hover .delete-btn-mask {
  opacity: 1;
}

.upload-hint {
  font-size: 12px;
  color: var(--rh-text-light);
  margin-top: 10px;
}

/* 关联商户栏 */
.shop-association-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid var(--rh-border);
  background-color: #FAFAFC;
  padding: 16px;
  border-radius: 8px;
  width: 100%;
}

.associated-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.shop-marker-icon {
  font-size: 20px;
  color: var(--rh-primary);
}

.text-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.text-meta .s-name {
  font-size: 14px;
  font-weight: 800;
  color: var(--rh-text-main);
}

.text-meta .s-addr {
  font-size: 11px;
  color: var(--rh-text-light);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.s-area {
  font-size: 10px;
  color: var(--rh-primary);
  background: var(--rh-primary-light);
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 700;
  margin-left: 14px;
}

.empty-association {
  font-size: 13px;
  color: var(--rh-text-light);
}

/* 底部按钮 */
.action-footer-line {
  margin-top: 32px;
  border-top: 1px solid var(--rh-border);
  padding-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 16px;
}

.cancel-btn {
  border-radius: 20px !important;
  font-weight: 700;
  padding: 10px 24px;
}

.submit-btn {
  border-radius: 20px !important;
  font-weight: 700;
  padding: 10px 28px;
  box-shadow: 0 4px 12px rgba(255, 102, 51, 0.2);
}

/* Dialog */
.custom-shop-dialog :deep(.el-dialog) {
  border-radius: 12px;
  overflow: hidden;
}

.dialog-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dialog-search {
  width: 100%;
}

.shop-list-container {
  max-height: 320px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}

.dialog-shop-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-radius: 8px;
  border: 1px solid var(--rh-border);
  background-color: #FAFAFC;
  cursor: pointer;
  transition: all 0.2s;
}

.dialog-shop-item:hover {
  border-color: var(--rh-primary);
  background-color: var(--rh-primary-light);
}

.dialog-shop-item.active {
  border-color: var(--rh-primary);
  background-color: var(--rh-primary-light);
}

.dialog-shop-item .shop-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.dialog-shop-item h4 {
  font-size: 13px;
  font-weight: 800;
  color: var(--rh-text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dialog-shop-item p {
  font-size: 11px;
  color: var(--rh-text-light);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shop-area-tag {
  font-size: 10px;
  color: var(--rh-primary);
  background: var(--rh-primary-light);
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 700;
  margin-left: 12px;
}
</style>
