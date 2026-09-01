<template>
  <a-config-provider :theme="themeConfig">
    <a-layout class="agent-shell">
      <a-layout-sider v-model:collapsed="collapsed" class="sidebar" :width="220" :collapsed-width="0" breakpoint="lg" @breakpoint="handleBreakpoint">
        <div class="sidebar-inner">
          <button class="brand-home" type="button" @click="goTo('/admin')"><span class="brand-dot"></span><span class="brand-name">AgentPass</span></button>
          <span class="nav-label">ADMIN</span>
          <a-menu :selected-keys="selectedKeys" class="side-menu" mode="inline" theme="light" @click="handleMenuClick">
            <a-menu-item key="pending"><ClockCircleOutlined /><span>발급 검토</span></a-menu-item>
            <a-menu-item key="history"><HistoryOutlined /><span>검토 이력</span></a-menu-item>
          </a-menu>
          <div class="admin-note"><span>관리자 메뉴</span><strong>Passport 발급</strong></div>
        </div>
      </a-layout-sider>

      <a-layout class="workspace-shell">
        <a-layout-header class="top-header">
          <div class="header-leading">
            <button class="mobile-trigger" type="button" aria-label="메뉴 열기" @click="mobileOpen = !mobileOpen"><MenuOutlined /></button>
          </div>
          <div class="header-actions">

          <a-dropdown placement="bottomRight" :trigger="['click']">
            <a-badge :count="unreadCount" :overflow-count="9" color="#483bff">
              <button class="header-icon-button" type="button" aria-label="알림 보기">
                <BellOutlined />
              </button>
            </a-badge>
            <template #overlay>
              <a-menu class="notification-menu" @click="handleNotificationClick">
                <div class="notification-heading">
                  <strong>알림</strong>
                  <span>{{ unreadCount ? `${unreadCount}개 새 알림` : '새 알림 없음' }}</span>
                </div>
                <a-menu-item v-for="notification in notifications" :key="notification.key">
                  <div class="notification-item">
                    <strong>{{ notification.title }}</strong>
                    <span>{{ notification.description }}</span>
                    <time>{{ notification.time }}</time>
                  </div>
                </a-menu-item>
                <a-menu-item v-if="!notifications.length" key="empty" disabled>
                  <span class="notification-empty">표시할 알림이 없습니다.</span>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>

          <div class="profile-summary"><strong>{{ adminName }}</strong><span>ADMIN</span></div>
          <a-dropdown placement="bottomRight" :trigger="['click']">
            <button class="profile-avatar-button" type="button" aria-label="프로필 메뉴 열기">
              <a-avatar :size="36" class="profile-avatar"><UserOutlined /></a-avatar>
            </button>
            <template #overlay>
              <a-menu class="profile-menu" @click="handleProfileMenuClick">
                <a-menu-item key="logout" :disabled="loggingOut">
                  <LogoutOutlined />
                  <span>{{ loggingOut ? '로그아웃 중...' : '로그아웃' }}</span>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
          </div>
        </a-layout-header>
        <a-layout-content class="main-content"><div class="content-surface"><slot /></div></a-layout-content>
      </a-layout>

      <a-drawer
        v-model:open="mobileOpen"
        root-class-name="mobile-navigation"
        placement="left"
        :width="248"
        :closable="false"
      >
        <div class="mobile-sidebar-inner">
          <button class="brand-home" type="button" @click="goTo('/admin')"><span class="brand-dot"></span><span class="brand-name">AgentPass</span></button>
          <span class="nav-label">ADMIN</span>
          <a-menu
            :selected-keys="selectedKeys"
            class="side-menu"
            mode="inline"
            theme="light"
            @click="handleMenuClick"
          >
            <a-menu-item key="pending"><ClockCircleOutlined /><span>발급 검토</span></a-menu-item>
            <a-menu-item key="history"><HistoryOutlined /><span>검토 이력</span></a-menu-item>
          </a-menu>
        </div>
      </a-drawer>
    </a-layout>
  </a-config-provider>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { theme } from 'ant-design-vue'
import { useAuthStore } from '@/store/auth.js'
import { enrollmentApi } from '@/api/enrollment.js'
import {
  BellOutlined,
  ClockCircleOutlined,
  HistoryOutlined,
  LogoutOutlined,
  MenuOutlined,
  UserOutlined
} from '@ant-design/icons-vue'

const collapsed = ref(false)
const mobileOpen = ref(false)
const loggingOut = ref(false)
const unreadCount = ref(0)
const notifications = ref([])
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const adminName = computed(() => auth.user?.name || '관리자')

const selectedKeys = computed(() => {
  if (route.path.startsWith('/admin/history')) return ['history']
  return ['pending']
})

const menuRoutes = {
  pending: '/admin',
  history: '/admin/history'
}

onMounted(async () => {
  try {
    const res = await enrollmentApi.getAdminPending()
    notifications.value = (res.data || []).slice(0, 5).map((item) => ({
      key: String(item.id),
      title: '새 Passport 발급 요청',
      description: `${item.courseTitle} · ${item.permissionCount}개 권한 검토가 필요합니다.`,
      time: formatNotificationTime(item.createdAt),
      path: `/admin/passports/${item.id}`
    }))
    unreadCount.value = notifications.value.length
  } catch (error) {
    console.warn('[AdminLayout] 관리자 알림을 불러오지 못했습니다.', error)
  }
})

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
    fontFamily: "'Plus Jakarta Sans', 'Noto Sans KR', sans-serif"
  }
}

function handleBreakpoint(broken) {
  collapsed.value = broken
  if (!broken) mobileOpen.value = false
}

function goTo(path) {
  mobileOpen.value = false
  router.push(path)
}

function handleMenuClick({ key }) {
  goTo(menuRoutes[key])
}

function formatNotificationTime(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('ko-KR', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}

function handleNotificationClick({ key }) {
  if (key === 'empty') return
  const notification = notifications.value.find((item) => item.key === String(key))
  unreadCount.value = 0
  if (notification) goTo(notification.path)
}

async function handleProfileMenuClick({ key }) {
  if (key !== 'logout' || loggingOut.value) return

  loggingOut.value = true
  await auth.logout()
}
</script>

<style scoped>
.agent-shell { width: 100%; height: 100dvh; min-height: 0; margin: 0; overflow: hidden; color: var(--forest-text); background: #fff; }
.sidebar { height: calc(100dvh - 24px); min-height: 0; margin: 12px 0 12px 12px; overflow: hidden; background: #f7f7f7 !important; border-right: 0; border-radius: 16px; }
.sidebar-inner, .mobile-sidebar-inner { height: 100%; padding: 24px 14px 16px; display: flex; flex-direction: column; }
.brand-home, .header-leading, .header-actions { display: flex; align-items: center; }
.brand-home { gap: 10px; padding: 0 10px; color: inherit; background: transparent; border: 0; }
.brand-dot { width: 18px; height: 18px; border: 5px solid var(--forest-primary); border-radius: 50%; box-shadow: inset 0 0 0 2px #f7f7f7; }
.brand-name { color: var(--forest-text); font-size: 20px; font-weight: 750; letter-spacing: -.045em; }
.nav-label { margin: 42px 16px 10px; color: var(--forest-text-muted); font-size: 10px; font-weight: 650; letter-spacing: .12em; }
.side-menu { flex: 1; background: transparent !important; border-inline-end: 0 !important; }
.side-menu :deep(.ant-menu-item) { position: relative; width: 100%; height: 44px; margin: 3px 0; padding-inline: 16px !important; display: flex; align-items: center; border-radius: 10px; color: var(--forest-text-secondary); font-size: 13px; }
.side-menu :deep(.ant-menu-item .anticon) { font-size: 17px; }
.side-menu :deep(.ant-menu-item-selected) { color: var(--forest-primary-dark) !important; background: var(--forest-primary-soft) !important; font-weight: 650; }
.side-menu :deep(.ant-menu-item-selected::before) { position: absolute; left: -14px; width: 4px; height: 26px; border-radius: 0 5px 5px 0; background: var(--forest-primary); content: ''; }
.admin-note { padding: 16px; display: grid; gap: 4px; color: #fff; background: var(--forest-primary-dark); border-radius: 13px; }
.admin-note span { color: rgba(255,255,255,.7); font-size: 10px; }
.admin-note strong { font-size: 13px; }
.workspace-shell { min-width: 0; min-height: 0; height: 100dvh; padding: 12px 12px 12px 10px; overflow: hidden; background: #fff; }
.top-header { height: 48px; padding: 0; display: flex; align-items: center; justify-content: flex-end; line-height: normal; background: #fff; border: 0; }
.header-actions { min-height: 48px; padding: 5px 8px 5px 10px; gap: 10px; background: var(--forest-subtle); border-radius: 15px; }
.header-icon-button, .mobile-trigger { color: var(--forest-text); background: #fff; border: 0; }
.header-icon-button { width: 38px; height: 38px; display: grid; place-items: center; border-radius: 50%; font-size: 18px; }
.profile-summary { display: grid; gap: 2px; padding-left: 4px; line-height: 1.3; text-align: right; }
.profile-summary strong { color: var(--forest-text); font-size: 12px; }
.profile-summary span { color: var(--forest-text-secondary); font-size: 10px; }
.profile-avatar-button { padding: 0; border: 0; border-radius: 50%; background: transparent; line-height: 0; }
.profile-avatar-button:focus-visible { outline: 2px solid var(--forest-primary); outline-offset: 3px; }
.profile-avatar { color: var(--forest-primary-dark); background: var(--forest-primary-soft); border: 1px solid #d8d4ff; }
.profile-menu { min-width: 142px; }
.mobile-trigger { display: none; width: 38px; height: 38px; border-radius: 50%; font-size: 18px; }
.main-content { min-width: 0; min-height: 0; height: calc(100dvh - 72px); padding-top: 8px; overflow: hidden; background: #fff; }
.content-surface { height: 100%; min-height: 0; padding: 20px 28px 24px; overflow-x: hidden; overflow-y: auto; overscroll-behavior: contain; scrollbar-color: #d3d3d3 transparent; scrollbar-width: thin; background: #fff; -webkit-overflow-scrolling: touch; }

.notification-menu { width: 320px; padding: 8px !important; }
.notification-heading { padding: 8px 10px 10px; display: flex; justify-content: space-between; border-bottom: 1px solid var(--forest-border); }
.notification-heading strong { color: var(--forest-text); }
.notification-heading span { color: var(--forest-text-secondary); font-size: 11px; }
.notification-menu :deep(.ant-dropdown-menu-item) { padding: 10px; }
.notification-item { display: grid; gap: 3px; white-space: normal; }
.notification-item strong { color: var(--forest-text); font-size: 12px; }
.notification-item span { color: var(--forest-text-secondary); font-size: 11px; line-height: 1.45; }
.notification-item time { color: var(--forest-text-muted); font-size: 10px; }
.notification-empty { color: var(--forest-text-muted); font-size: 11px; }

@media (max-width: 991px) {
  .agent-shell { width: 100%; height: 100dvh; }
  .workspace-shell { padding: 0; }
  .sidebar { margin: 0; border-radius: 0; }
  .top-header { padding: 0 18px; justify-content: space-between; border-radius: 0; }
  .mobile-trigger { display: inline-grid; place-items: center; }
  .sidebar { display: none; }
  .main-content { height: calc(100dvh - 56px); }
  .main-content { padding-top: 0; }
  .content-surface { padding: 20px 18px 24px; border-radius: 0; }
}
@media (min-width: 992px) and (max-width: 1199px) {
  .sidebar { flex: 0 0 190px !important; width: 190px !important; min-width: 190px !important; max-width: 190px !important; }
  .content-surface { padding-right: 20px; padding-left: 20px; }
}

:global(.mobile-navigation .ant-drawer-content),
:global(.mobile-navigation .ant-drawer-body) { background: var(--forest-subtle); }
:global(.mobile-navigation .ant-drawer-body) { padding: 0; }
.mobile-sidebar-inner { min-height: 100%; height: auto; }
.mobile-sidebar-inner .side-menu { min-height: calc(100vh - 150px); }

@media (max-width: 620px) {
  .profile-summary { display: none; }
  .brand-name { font-size: 18px; }
  .header-actions { gap: 8px; }
  .content-surface { padding: 22px 14px 34px; }
}
@media (max-height: 760px) and (min-width: 992px) {
  .sidebar-inner { padding-top: 18px; padding-bottom: 12px; }
  .nav-label { margin-top: 28px; }
  .content-surface { padding: 14px 24px 18px; }
}
</style>
