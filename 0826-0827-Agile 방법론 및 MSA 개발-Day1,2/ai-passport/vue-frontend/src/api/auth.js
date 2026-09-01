import api from './index.js'
import axios from 'axios'

// .env 대신 하드코딩 — 백엔드 연동 시점에 실제 값으로 교체
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
const CLIENT_ID = 'web-client'
const CLIENT_SECRET = 'web-secret'
const REDIRECT_URI = import.meta.env.VITE_REDIRECT_URI || `${window.location.origin}/callback`

export const authApi = {
  // OAuth2 Authorization Code -> Access Token 교환
  // CLIENT_SECRET_BASIC: Authorization 헤더에 client_id:client_secret을 Base64로 인코딩
  exchangeCode(code) {
    const clientId = CLIENT_ID
    const clientSecret = CLIENT_SECRET
    const redirectUri = REDIRECT_URI
    const credentials = btoa(`${clientId}:${clientSecret}`)

    const body = new URLSearchParams({
      grant_type: 'authorization_code',
      code,
      redirect_uri: redirectUri
    })

    return axios.post(
      `${API_BASE_URL}/oauth2/token`,
      body.toString(),
      {
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          'Authorization': `Basic ${credentials}`
        }
      }
    )
  },

  // 내 정보 조회
  getMe() {
    return api.get('/api/users/me')
  },

  // 회원가입
  register(data) {
    return api.post('/api/users/register', data)
  }
}
