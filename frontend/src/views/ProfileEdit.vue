<template>
  <div class="profile-edit-pc-page rh-container">
    <!-- 面包屑导航 -->
    <div class="breadcrumb-container">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/profile' }">个人中心</el-breadcrumb-item>
        <el-breadcrumb-item>资料编辑</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 表单卡片居中 -->
    <div class="form-layout">
      <div class="edit-card rh-card">
        <h2 class="form-title">编辑个人资料</h2>
        <div class="form-divider"></div>

        <!-- 头像上传 -->
        <div class="avatar-upload-box">
          <div class="avatar-container" @click="triggerAvatarUpload">
            <img :src="user.icon || '/imgs/icons/default-icon.png'" alt="Avatar" class="avatar-img" />
            <div class="camera-mask">
              <el-icon><Camera /></el-icon>
            </div>
          </div>
          <span class="upload-hint">更换个人头像</span>
        </div>

        <!-- Element Plus 表单 -->
        <el-form :model="info" label-position="left" label-width="100px" class="edit-form">
          <el-form-item label="用户昵称">
            <el-input v-model="user.nickName" placeholder="请填写您的新昵称" />
          </el-form-item>

          <el-form-item label="性别">
            <el-radio-group v-model="info.gender">
              <el-radio value="男">男</el-radio>
              <el-radio value="女">女</el-radio>
              <el-radio value="保密">保密</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="出生日期">
            <el-date-picker
              v-model="info.birthday"
              type="date"
              placeholder="选择您的生日"
              value-format="YYYY-MM-DD"
              class="date-picker"
            />
          </el-form-item>

          <el-form-item label="常居城市">
            <el-select v-model="info.city" placeholder="请选择常居城市">
              <el-option label="杭州" value="杭州" />
              <el-option label="北京" value="北京" />
              <el-option label="上海" value="上海" />
              <el-option label="深圳" value="深圳" />
              <el-option label="广州" value="广州" />
            </el-select>
          </el-form-item>

          <el-form-item label="个人介绍">
            <el-input 
              v-model="info.introduce" 
              type="textarea" 
              placeholder="用一两句话向大家介绍一下你自己吧，让大家更好的认识你~"
              :rows="3"
            />
          </el-form-item>

          <div class="form-divider"></div>

          <!-- PC 特权展示 -->
          <el-form-item label="我的等级" class="vip-item">
            <span class="vip-text">RateHub 普通用户 (Lv1) <a href="javascript:;" class="vip-link">升级为 VIP 享专属权益</a></span>
          </el-form-item>

          <!-- 底部操作按钮 -->
          <div class="action-row">
            <el-button @click="goBack" class="cancel-btn">取消返回</el-button>
            <el-button type="primary" :loading="saveLoading" class="save-btn" @click="handleSave">
              保存更改
            </el-button>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'
import request from '../utils/request'

const router = useRouter()

const user = ref({
  nickName: '',
  icon: ''
})
const info = ref({
  introduce: '',
  gender: '',
  city: '',
  birthday: ''
})
const saveLoading = ref(false)

onMounted(() => {
  checkLoginAndLoadData()
})

const goBack = () => router.back()

const checkLoginAndLoadData = async () => {
  try {
    const res = await request.get('/user/me')
    if (res.code === 200 && res.data) {
      user.value = res.data
      
      const cachedInfo = sessionStorage.getItem('userInfo')
      if (cachedInfo) {
        info.value = JSON.parse(cachedInfo)
      } else {
        queryUserDetails(res.data.id)
      }
    }
  } catch (error) {
    console.error(error)
  }
}

const queryUserDetails = async (userId) => {
  try {
    const res = await request.get(`/user/info/${userId}`)
    if (res.code === 200 && res.data) {
      info.value = res.data
      sessionStorage.setItem('userInfo', JSON.stringify(res.data))
    }
  } catch (error) {
    console.error(error)
  }
}

const triggerAvatarUpload = () => {
  ElMessage.info('个人头像上传接口联调中')
}

const handleSave = () => {
  if (!user.value.nickName.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  
  saveLoading.value = true
  setTimeout(() => {
    saveLoading.value = false
    sessionStorage.setItem('userInfo', JSON.stringify(info.value))
    ElMessage.success('个人资料保存成功')
    setTimeout(() => {
      router.replace('/profile')
    }, 600)
  }, 800)
}
</script>

<style scoped>
.profile-edit-pc-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.breadcrumb-container {
  padding: 8px 0;
}

/* 居中面板 */
.form-layout {
  display: flex;
  justify-content: center;
}

.edit-card {
  width: 100%;
  max-width: 680px;
  background: white;
  padding: 30px 40px;
}

.edit-card:hover {
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

/* 头像修改 */
.avatar-upload-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin-bottom: 30px;
}

.avatar-container {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  box-shadow: var(--rh-shadow-subtle);
  border: 1px solid var(--rh-border);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.camera-mask {
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
  font-size: 22px;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-container:hover .camera-mask {
  opacity: 1;
}

.upload-hint {
  font-size: 12px;
  color: var(--rh-text-light);
}

/* 表单细节 */
.edit-form :deep(.el-form-item__label) {
  font-weight: 700;
  color: var(--rh-text-main);
}

.date-picker {
  width: 100% !important;
}

.vip-item :deep(.el-form-item__content) {
  display: flex;
  align-items: center;
}

.vip-text {
  font-size: 13px;
  color: var(--rh-text-sub);
}

.vip-link {
  color: var(--rh-primary);
  font-weight: 700;
  margin-left: 10px;
}

/* 按钮 */
.action-row {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 24px;
}

.cancel-btn {
  border-radius: 20px !important;
  font-weight: 700;
}

.save-btn {
  border-radius: 20px !important;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(255, 102, 51, 0.2);
}
</style>
