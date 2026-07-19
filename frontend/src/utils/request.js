import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 5000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = sessionStorage.getItem('token')
    if (token) {
      config.headers['authorization'] = token
    }
    return config
  },
  error => {
    console.error(error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    // 如果业务逻辑失败
    if (res.code !== 200) {
      return Promise.reject(res.message || '业务处理失败')
    }
    return res
  },
  error => {
    console.error('API Error:', error)
    if (error.response) {
      if (error.response.status === 401) {
        ElMessage.error('请先登录')
        sessionStorage.removeItem('token')
        sessionStorage.removeItem('userInfo')
        // 延迟跳转登录页 (Hash 路由模式下)
        setTimeout(() => {
          window.location.href = '#/login'
        }, 800)
        return Promise.reject('请先登录')
      }
    }
    ElMessage.error(error.message || '系统繁忙，请稍后再试')
    return Promise.reject(error)
  }
)

export default request
