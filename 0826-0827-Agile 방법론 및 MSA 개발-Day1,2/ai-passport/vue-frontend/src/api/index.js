import axios from 'axios'
import { useAuthStore } from '@/store/auth.js'

const api = axios.create({
  baseURL: '',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

api.interceptors.response.use(
  (res) => {
    const payload = res.data
    if (
      payload
      && typeof payload === 'object'
      && Object.prototype.hasOwnProperty.call(payload, 'success')
      && Object.prototype.hasOwnProperty.call(payload, 'data')
    ) {
      res.apiMessage = payload.message
      res.data = payload.data
    }
    return res
  },
  (err) => {
    if (err.response?.status === 401) {
      console.error('[API] 401 Unauthorized')
      console.error('[API] response data =', err.response?.data)
      console.error('[API] request url =', err.config?.url)
      // 디버깅 중에는 자동 로그아웃/리다이렉트 잠시 비활성화
      // const auth = useAuthStore()
      // auth.logout()
      // window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export default api
