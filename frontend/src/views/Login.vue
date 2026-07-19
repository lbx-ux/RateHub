<template>
  <div class="login-page-container">
    <!-- 装饰背景 -->
    <div class="bg-decoration">
      <div class="glow-orb orb-1"></div>
      <div class="glow-orb orb-2"></div>
    </div>

    <!-- 登录主卡片 -->
    <div class="login-card-box rh-card">
      <div class="brand-info">
        <div class="logo-circle">RH</div>
        <h2 class="brand-name">RateHub</h2>
        <p class="brand-slogan">探索城市街角美味</p>
      </div>

      <el-tabs v-model="loginMode" class="login-tabs">
        <!-- 验证码登录 -->
        <el-tab-pane label="验证码登录" name="code">
          <el-form :model="form" class="login-form">
            <el-form-item>
              <el-input
                v-model="form.phone"
                placeholder="请输入手机号"
                :prefix-icon="Phone"
                clearable
                class="custom-input"
              />
            </el-form-item>
            <el-form-item class="code-form-item">
              <el-input
                v-model="form.code"
                placeholder="请输入验证码"
                :prefix-icon="Key"
                class="custom-input code-input"
              />
              <el-button
                type="primary"
                plain
                :disabled="codeDisabled"
                @click="sendCode"
                class="send-code-btn"
              >
                {{ codeBtnText }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 密码登录 -->
        <el-tab-pane label="密码登录" name="password">
          <el-form :model="form" class="login-form">
            <el-form-item>
              <el-input
                v-model="form.phone"
                placeholder="请输入手机号"
                :prefix-icon="Phone"
                clearable
                class="custom-input"
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                show-password
                class="custom-input"
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <!-- 协议勾选 -->
      <div class="agreement-row">
        <el-checkbox v-model="radio">
          <span class="agreement-label">
            我已阅读并同意 <a href="javascript:;" class="link">《用户服务协议》</a> 与 <a href="javascript:;" class="link">《隐私政策》</a>
          </span>
        </el-checkbox>
      </div>

      <!-- 登录提交 -->
      <el-button
        type="primary"
        block
        :loading="loading"
        @click="handleLogin"
        class="submit-btn"
      >
        立 即 登 录
      </el-button>

      <div class="form-hint">未注册的手机号验证后将自动创建账户</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Phone, Key, Lock } from '@element-plus/icons-vue'
import request from '../utils/request'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginMode = ref('code')
const radio = ref(false)
const loading = ref(false)
const codeDisabled = ref(false)
const codeBtnText = ref('获取验证码')

// 阿里云图形验证码 appId（请替换为控制台创建方案后生成的 appId）
const CAPTCHA_APP_ID = '6b4d5628fe38e926dc609b22d95ea1ec'
let captchaInstance = null

const form = reactive({
  phone: '',
  code: '',
  password: ''
})

// 初始化阿里云图形验证码
onMounted(() => {
  if (window.initAlicom4) {
    window.initAlicom4({
      captchaId: CAPTCHA_APP_ID,
      product: 'bind',
    }, (instance) => {
      captchaInstance = instance

      // 验证成功回调
      captchaInstance.onSuccess(() => {
        const validateData = captchaInstance.getValidate()
        doSendCode(validateData)
      })

      // 验证出错回调
      captchaInstance.onError(() => {
        codeDisabled.value = false
        ElMessage.warning('图形验证未完成，请重试')
      })

      // 用户关闭弹窗回调
      captchaInstance.onClose(() => {
        codeDisabled.value = false
      })
    })
  }
})

onBeforeUnmount(() => {
  if (captchaInstance) {
    captchaInstance.reset()
    captchaInstance = null
  }
})

// 点击"获取验证码"按钮：校验手机号后弹出图形验证码
const sendCode = () => {
  if (!form.phone) {
    ElMessage.warning('请输入手机号')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    ElMessage.warning('手机号格式不正确')
    return
  }

  codeDisabled.value = true
  if (captchaInstance) {
    captchaInstance.showCaptcha()
  } else {
    ElMessage.error('验证码服务未就绪，请刷新页面后重试')
    codeDisabled.value = false
  }
}

// 图形验证通过后，实际发送短信验证码请求
const doSendCode = async (validateData) => {
  try {
    await request.post('/user/code', {
      phone: form.phone,
      lotNumber: validateData.lot_number,
      captchaOutput: validateData.captcha_output,
      passToken: validateData.pass_token,
      genTime: validateData.gen_time,
    })
    ElMessage.success('验证码发送成功')

    let countdown = 60
    codeBtnText.value = `${countdown}s 后重发`
    const timer = setInterval(() => {
      countdown--
      if (countdown <= 0) {
        clearInterval(timer)
        codeBtnText.value = '获取验证码'
        codeDisabled.value = false
      } else {
        codeBtnText.value = `${countdown}s 后重发`
      }
    }, 1000)
  } catch (error) {
    codeDisabled.value = false
    ElMessage.error(typeof error === 'string' ? error : '验证码发送失败')
  }
}

const handleLogin = async () => {
  if (!radio.value) {
    ElMessage.warning('请先同意服务协议和隐私政策')
    return
  }
  if (!form.phone) {
    ElMessage.warning('请输入手机号')
    return
  }
  if (loginMode.value === 'code' && !form.code) {
    ElMessage.warning('请输入验证码')
    return
  }
  if (loginMode.value === 'password' && !form.password) {
    ElMessage.warning('请输入密码')
    return
  }

  try {
    loading.value = true
    const submitData = { phone: form.phone }
    if (loginMode.value === 'code') {
      submitData.code = form.code
    } else {
      submitData.password = form.password
    }

    const res = await request.post('/user/login', submitData)
    if (res.code === 200 && res.data) {
      userStore.setToken(res.data)
      
      // 成功后获取用户信息
      const infoRes = await request.get('/user/me')
      if (infoRes.code === 200) {
        userStore.setUserInfo(infoRes.data)
      }
      
      ElMessage.success('登录成功')
      const redirect = route.query.redirect || '/'
      setTimeout(() => {
        router.replace(redirect)
      }, 500)
    }
  } catch (error) {
    ElMessage.error(error || '登录失败，请检查输入')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page-container {
  min-height: calc(100vh - 64px);
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background-color: #FAFAFC;
  overflow: hidden;
  padding: 40px 20px;
}

/* 霓虹背景圆团装饰 */
.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

.glow-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.5;
}

.orb-1 {
  width: 400px;
  height: 400px;
  background: rgba(255, 102, 51, 0.15);
  top: -100px;
  right: -50px;
}

.orb-2 {
  width: 350px;
  height: 350px;
  background: rgba(255, 153, 0, 0.12);
  bottom: -80px;
  left: -50px;
}

/* 登录卡片 */
.login-card-box {
  width: 100%;
  max-width: 420px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  z-index: 5;
  padding: 36px 32px;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.05);
}

.login-card-box:hover {
  transform: none; /* 登录页卡片不浮动 */
}

/* 品牌头部 */
.brand-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  margin-bottom: 28px;
}

.logo-circle {
  width: 52px;
  height: 52px;
  background: linear-gradient(135deg, var(--rh-primary), #FF8855);
  color: white;
  font-size: 22px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  box-shadow: 0 6px 16px rgba(255, 102, 51, 0.25);
  margin-bottom: 12px;
}

.brand-name {
  font-size: 22px;
  font-weight: 800;
  color: var(--rh-text-main);
  letter-spacing: 0.5px;
}

.brand-slogan {
  font-size: 13px;
  color: var(--rh-text-light);
  margin-top: 4px;
}

/* Tabs */
.login-tabs :deep(.el-tabs__header) {
  margin-bottom: 24px;
}

:deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--rh-border);
}

:deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 600;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

:deep(.custom-input .el-input__wrapper) {
  border-radius: 8px;
  padding: 10px 14px;
  box-shadow: 0 0 0 1px var(--rh-border) inset !important;
}

:deep(.custom-input .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--rh-text-light) inset !important;
}

:deep(.custom-input .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--rh-primary) inset !important;
}

/* 验证码输入框组合 */
.code-form-item :deep(.el-form-item__content) {
  display: flex;
  gap: 12px;
}

.code-input {
  flex: 1;
}

.send-code-btn {
  height: 42px;
  border-radius: 8px !important;
  font-weight: 600;
  padding: 0 14px;
}

/* 协议协议 */
.agreement-row {
  margin-top: 18px;
  margin-bottom: 24px;
}

.agreement-label {
  font-size: 12px;
  color: var(--rh-text-sub);
  white-space: normal;
  line-height: 1.4;
}

.link {
  color: var(--rh-primary);
  font-weight: 500;
}

/* 登录按钮 */
.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 22px !important;
  background: linear-gradient(135deg, var(--rh-primary), #FF8855);
  border: none;
  box-shadow: 0 6px 20px rgba(255, 102, 51, 0.25);
  margin-bottom: 16px;
}

.submit-btn:hover {
  opacity: 0.9;
}

.form-hint {
  text-align: center;
  font-size: 11px;
  color: var(--rh-text-light);
}
</style>
