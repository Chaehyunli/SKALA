<template>
  <a-config-provider :theme="themeConfig">
    <div class="login-shell">
      <header class="login-header">
        <router-link to="/" class="header-brand">AgentPass</router-link>
        <router-link to="/" class="header-link">홈으로</router-link>
      </header>

      <main class="login-main">
        <section class="login-container fade-in-up">
          <div class="login-brand">
            <span class="brand-dot" aria-hidden="true"></span>
            <div>
              <h1>AgentPass</h1>
              <p>미션부터 Passport 발급까지 한곳에서 관리합니다.</p>
            </div>
          </div>

          <div class="login-card">
            <a-tabs v-model:active-key="activeTab" class="login-tabs" centered animated @change="clearMessages">
              <a-tab-pane key="login" tab="계정 로그인">
                <a-form :model="loginForm" class="auth-form" layout="vertical" @finish="handleOAuth">
                  <a-form-item name="username" :rules="[{ required: true, message: '이메일은 필수입니다.' }]">
                    <a-input v-model:value="loginForm.username" size="large" placeholder="이메일" autocomplete="username">
                      <template #prefix><UserOutlined /></template>
                    </a-input>
                  </a-form-item>

                  <a-form-item name="password" :rules="[{ required: true, message: '비밀번호는 필수입니다.' }]">
                    <a-input-password v-model:value="loginForm.password" size="large" placeholder="비밀번호" autocomplete="current-password">
                      <template #prefix><LockOutlined /></template>
                    </a-input-password>
                  </a-form-item>

                  <div class="form-options">
                    <a-checkbox v-model:checked="loginForm.remember">자동 로그인</a-checkbox>
                    <button type="button" class="link-button">비밀번호 찾기</button>
                  </div>

                  <a-alert v-if="error" class="form-alert" type="error" show-icon :message="error" />

                  <a-button type="primary" html-type="submit" size="large" block class="submit-button" :loading="loading">
                    로그인
                  </a-button>
                  <a-alert class="oauth-alert" type="info" show-icon message="AgentPass에서 안전하게 로그인합니다." />
                </a-form>
              </a-tab-pane>

              <a-tab-pane key="register" tab="회원가입">
                <a-form :model="registerForm" class="auth-form" layout="vertical" @finish="handleRegister">
                  <a-form-item name="name" :rules="[{ required: true, message: '이름은 필수입니다.' }]">
                    <a-input v-model:value="registerForm.name" size="large" placeholder="이름" autocomplete="name">
                      <template #prefix><UserOutlined /></template>
                    </a-input>
                  </a-form-item>

                  <a-form-item name="email" :rules="emailRules">
                    <a-input v-model:value="registerForm.email" size="large" placeholder="이메일" autocomplete="email">
                      <template #prefix><MailOutlined /></template>
                    </a-input>
                  </a-form-item>

                  <a-form-item name="password" :rules="passwordRules">
                    <a-input-password v-model:value="registerForm.password" size="large" placeholder="비밀번호 (8자 이상)" autocomplete="new-password">
                      <template #prefix><LockOutlined /></template>
                    </a-input-password>
                  </a-form-item>

                  <a-form-item name="role">
                    <a-select v-model:value="registerForm.role" size="large" :options="roleOptions" />
                  </a-form-item>

                  <a-alert v-if="error" class="form-alert" type="error" show-icon :message="error" />
                  <a-alert v-if="success" class="form-alert" type="success" show-icon :message="success" />

                  <a-button type="primary" html-type="submit" size="large" block class="submit-button" :loading="loading">
                    회원가입
                  </a-button>
                </a-form>
              </a-tab-pane>
            </a-tabs>

            <div class="card-footer">
              <span>{{ activeTab === 'login' ? '계정이 없으신가요?' : '이미 계정이 있으신가요?' }}</span>
              <button type="button" class="link-button" @click="toggleTab">
                {{ activeTab === 'login' ? '회원가입' : '로그인' }}
              </button>
            </div>
          </div>
        </section>
      </main>

      <footer class="login-footer">AgentPass · © 2026 All rights reserved.</footer>
    </div>
  </a-config-provider>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { theme } from 'ant-design-vue'
import { LockOutlined, MailOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useAuthStore } from '@/store/auth.js'
import { authApi } from '@/api/auth.js'

const auth = useAuthStore()
const activeTab = ref('login')
const loading = ref(false)
const error = ref('')
const success = ref('')
const loginForm = reactive({ username: '', password: '', remember: true })
const registerForm = reactive({ name: '', email: '', password: '', role: 'STUDENT' })
const roleOptions = [
  { value: 'STUDENT', label: '사용자' },
  { value: 'INSTRUCTOR', label: '관리자' }
]
const emailRules = [
  { required: true, message: '이메일은 필수입니다.' },
  { type: 'email', message: '올바른 이메일 형식이 아닙니다.' }
]
const passwordRules = [
  { required: true, message: '비밀번호는 필수입니다.' },
  { min: 8, message: '비밀번호는 8자 이상이어야 합니다.' }
]
const themeConfig = {
  algorithm: theme.defaultAlgorithm,
  token: {
    colorPrimary: '#483bff',
    colorInfo: '#483bff',
    colorSuccess: '#147a50',
    colorBgBase: '#f3f3f3',
    colorBgContainer: '#ffffff',
    colorBgElevated: '#ffffff',
    colorBorder: '#e8e8e8',
    colorText: '#171a18',
    colorTextSecondary: '#747474',
    borderRadius: 10,
    controlHeightLG: 46,
    fontFamily: "'Plus Jakarta Sans', 'Noto Sans KR', sans-serif"
  },
  components: {
    Input: { activeBorderColor: '#483bff', hoverBorderColor: '#655cff', colorBgContainer: '#f7f7f7' },
    Select: { colorBgContainer: '#f7f7f7', optionSelectedBg: '#eeecff' },
    Tabs: { itemActiveColor: '#3529d9', itemColor: '#7b837e', itemHoverColor: '#483bff', inkBarColor: '#483bff' }
  }
}

function clearMessages() {
  error.value = ''
  success.value = ''
}

function toggleTab() {
  activeTab.value = activeTab.value === 'login' ? 'register' : 'login'
  clearMessages()
}

async function handleOAuth() {
  clearMessages()
  loading.value = true
  try {
    await auth.login(loginForm.username, loginForm.password)
  } catch (e) {
    error.value = e.message || '로그인에 실패했습니다.'
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  clearMessages()
  loading.value = true
  try {
    await authApi.register({ ...registerForm })
    success.value = '회원가입이 완료되었습니다. 로그인할 수 있습니다.'
    Object.assign(registerForm, { name: '', email: '', password: '', role: 'STUDENT' })
    window.setTimeout(() => {
      activeTab.value = 'login'
      clearMessages()
    }, 2000)
  } catch (e) {
    error.value = e.response?.data?.message || '회원가입에 실패했습니다.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  color: var(--forest-text);
  padding: 16px;
  background: var(--forest-canvas);
}
.login-header {
  width: min(1480px, 100%);
  height: 64px;
  margin: 0 auto;
  padding: 0 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 0;
  border-radius: 22px 22px 0 0;
  background: #fff;
}
.header-brand { color: var(--forest-text); font-size: 21px; font-weight: 700; letter-spacing: -.03em; }
.header-link { color: var(--forest-text-secondary); font-size: 13px; transition: color .2s ease; }
.header-link:hover { color: var(--forest-primary); }
.login-main { width: min(1480px, 100%); flex: 1; margin: 0 auto; padding: 44px 24px 32px; display: grid; place-items: center; background: var(--forest-subtle); }
.login-container { width: min(100%, 390px); }
.login-brand { margin-bottom: 24px; display: flex; align-items: center; justify-content: center; gap: 11px; }
.brand-dot { width: 20px; height: 20px; border: 5px solid var(--forest-primary); border-radius: 50%; }
.login-brand h1 { margin: 0; color: var(--forest-text); font-size: 26px; line-height: 1.15; letter-spacing: -.04em; }
.login-brand p { margin: 5px 0 0; color: var(--forest-text-secondary); font-size: 12px; }
.login-card {
  overflow: hidden;
  border: 1px solid var(--forest-border);
  border-radius: 16px;
  background: var(--forest-surface);
  box-shadow: 0 10px 30px rgba(25, 25, 25, .06);
}
.login-tabs { padding: 22px 28px 0; }
.login-tabs :deep(.ant-tabs-nav) { margin-bottom: 24px; }
.login-tabs :deep(.ant-tabs-nav-list) { width: 100%; }
.login-tabs :deep(.ant-tabs-tab) { flex: 1; justify-content: center; padding: 10px 0 13px; }
.login-tabs :deep(.ant-tabs-ink-bar) { height: 3px; border-radius: 3px 3px 0 0; box-shadow: none; }
.auth-form :deep(.ant-form-item) { margin-bottom: 16px; }
.auth-form :deep(.ant-input-affix-wrapper),
.auth-form :deep(.ant-select-selector) { border-color: var(--forest-border-strong); background: var(--forest-subtle) !important; }
.auth-form :deep(.ant-input) { background: transparent; }
.auth-form :deep(.ant-input-prefix) { margin-right: 9px; color: var(--forest-text-muted); }
.form-options { margin: -2px 0 18px; display: flex; align-items: center; justify-content: space-between; }
.link-button { padding: 0; border: 0; color: var(--forest-primary); background: transparent; font-size: 12px; }
.link-button:hover { color: var(--forest-primary); }
.submit-button {
  height: 46px;
  border-color: var(--forest-primary);
  background: var(--forest-primary);
  box-shadow: none;
  font-weight: 600;
}
.submit-button:hover { border-color: var(--forest-primary-dark) !important; background: var(--forest-primary-dark) !important; }
.oauth-alert, .form-alert { margin-top: 16px; }
.oauth-alert { border-color: #d8d4ff; background: #eeecff; }
.oauth-alert :deep(.ant-alert-message), .form-alert :deep(.ant-alert-message) { font-size: 11px; }
.card-footer {
  min-height: 52px;
  padding: 14px 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border-top: 1px solid var(--forest-border);
  color: var(--forest-text-secondary);
  font-size: 12px;
  background: var(--forest-subtle);
}
.login-footer { width: min(1480px, 100%); min-height: 54px; margin: 0 auto; padding: 16px 24px; color: var(--forest-text-muted); background: #fff; border-radius: 0 0 22px 22px; font-size: 11px; text-align: center; }
@media (max-width: 620px) {
  .login-shell { padding: 0; }
  .login-header, .login-footer { border-radius: 0; }
  .login-header { padding: 0 20px; }
  .login-main { padding: 34px 16px 24px; align-items: start; }
  .login-brand { justify-content: flex-start; }
  .login-tabs { padding-right: 20px; padding-left: 20px; }
  .card-footer { padding-right: 20px; padding-left: 20px; }
}
</style>
