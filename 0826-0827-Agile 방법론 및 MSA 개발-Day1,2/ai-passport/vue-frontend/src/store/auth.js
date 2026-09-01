import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth.js'

// .env 대신 하드코딩 — 백엔드 연동 시점에 실제 값으로 교체
const AUTH_SERVER_URL = import.meta.env.VITE_API_BASE_URL || ''
const CLIENT_ID = 'web-client'
const REDIRECT_URI = import.meta.env.VITE_REDIRECT_URI || `${window.location.origin}/callback`
const LOGIN_ENDPOINT = AUTH_SERVER_URL ? `${AUTH_SERVER_URL}/login` : '/auth-login'
const LOGOUT_ENDPOINT = AUTH_SERVER_URL ? `${AUTH_SERVER_URL}/logout` : '/auth-logout'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(sessionStorage.getItem('access_token') || null)
  const user = ref(JSON.parse(sessionStorage.getItem('user') || 'null'))

  const isAuthenticated = computed(() => !!accessToken.value)
  const isInstructor = computed(() => user.value?.role === 'INSTRUCTOR')

  function setToken(token) {
    accessToken.value = token
    sessionStorage.setItem('access_token', token)
  }

  function setUser(userData) {
    user.value = userData
    sessionStorage.setItem('user', JSON.stringify(userData))
  }

  async function fetchUser() {
    try {
      const res = await authApi.getMe()
      console.log('[AuthStore] /me response =', res.data)

      const userData = res?.data?.data ?? res?.data

      if (!userData || typeof userData !== 'object') {
        throw new Error('사용자 정보 형식이 올바르지 않습니다.')
      }

      setUser(userData)
    } catch (error) {
      console.error('[AuthStore] 사용자 정보 조회 실패:', error)
      logout(false)
    }
  }

  async function logout(redirect = true) {
    try {
      await clearAuthorizationSession()
    } catch (error) {
      console.warn('[AuthStore] 인증 서버 로그아웃 실패:', error)
    } finally {
      accessToken.value = null
      user.value = null
      sessionStorage.removeItem('access_token')
      sessionStorage.removeItem('user')

      if (redirect) {
        window.location.href = '/login'
      }
    }
  }

  async function clearAuthorizationSession() {
    await fetch(LOGOUT_ENDPOINT, {
        method: 'POST',
        credentials: 'include',
        redirect: 'manual'
    })
  }

  function createAuthorizationUrl() {
    const params = new URLSearchParams({
      response_type: 'code',
      client_id: CLIENT_ID,
      redirect_uri: REDIRECT_URI,
      scope: 'openid profile read write'
    })

    return `${AUTH_SERVER_URL}/oauth2/authorize?${params.toString()}`
  }

  // OAuth2 Authorization Code Flow
  function redirectToLogin() {
    window.location.href = createAuthorizationUrl()
  }

  // AgentPass 로그인 화면을 유지하면서 Spring Security 세션 로그인을 수행한다.
  async function login(username, password) {
    // 이전 계정의 인증 서버 세션이 남아 있으면 입력한 계정 대신 기존 계정으로 토큰이 발급된다.
    await clearAuthorizationSession()

    // 인증 요청을 먼저 보내 Spring Security가 원래 OAuth 요청을 세션에 저장하게 한다.
    const authorizationResponse = await fetch(createAuthorizationUrl(), {
      credentials: 'include',
      redirect: 'follow',
      headers: { Accept: 'text/html' }
    })

    if (!authorizationResponse.ok) {
      throw new Error('인증 세션을 시작하지 못했습니다.')
    }

    const authorizationUrl = new URL(authorizationResponse.url)
    if (authorizationUrl.pathname === '/callback' && authorizationUrl.searchParams.has('code')) {
      window.location.href = `${authorizationUrl.pathname}${authorizationUrl.search}${authorizationUrl.hash}`
      return
    }

    if (authorizationUrl.pathname !== '/auth-login') {
      throw new Error('로그인 페이지를 불러오지 못했습니다.')
    }

    // 기본 로그인 페이지는 표시하지 않고, 응답에서 제공되는 경우 CSRF 토큰만 읽는다.
    const loginDocument = new DOMParser().parseFromString(
      await authorizationResponse.text(),
      'text/html'
    )
    const csrfToken = loginDocument.querySelector('input[name="_csrf"]')?.value

    const body = new URLSearchParams({
      username,
      password
    })
    if (csrfToken) body.set('_csrf', csrfToken)

    const loginResponse = await fetch(LOGIN_ENDPOINT, {
      method: 'POST',
      credentials: 'include',
      redirect: 'follow',
      headers: {
        Accept: 'text/html',
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: body.toString()
    })

    if (!loginResponse.ok) {
      throw new Error('로그인 요청을 처리하지 못했습니다.')
    }

    const loginResultUrl = new URL(loginResponse.url)
    if (
      loginResultUrl.pathname === '/auth-login' ||
      loginResultUrl.searchParams.has('error')
    ) {
      throw new Error('이메일 또는 비밀번호가 올바르지 않습니다.')
    }

    if (loginResultUrl.pathname !== '/callback' || !loginResultUrl.searchParams.has('code')) {
      throw new Error('로그인 완료 응답이 올바르지 않습니다.')
    }

    window.location.href = `${loginResultUrl.pathname}${loginResultUrl.search}${loginResultUrl.hash}`
  }

  async function handleCallback(code) {
    const res = await authApi.exchangeCode(code)
    console.log('[AuthStore] token response =', res.data)

    const token = res?.data?.access_token

    if (!token) {
      throw new Error('액세스 토큰을 받지 못했습니다.')
    }

    setToken(token)
    await fetchUser()
  }

  return {
    accessToken,
    user,
    isAuthenticated,
    isInstructor,
    setToken,
    setUser,
    fetchUser,
    logout,
    login,
    redirectToLogin,
    handleCallback
  }
})
