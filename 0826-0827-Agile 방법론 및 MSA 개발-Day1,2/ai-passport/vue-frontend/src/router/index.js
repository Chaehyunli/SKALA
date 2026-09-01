import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth.js'

const routes = [
  {
    path: '/',
    name: 'WorkRequests',
    component: () => import('@/views/WorkRequestView.vue')
  },
  {
    path: '/work-requests/new',
    name: 'NewWorkRequest',
    component: () => import('@/views/NewWorkRequestView.vue')
  },
  {
    path: '/work-requests/:id/analysis',
    name: 'WorkRequestAnalysis',
    component: () => import('@/views/WorkRequestAnalysisView.vue')
  },
  {
    path: '/work-requests/:id',
    name: 'WorkRequestDetail',
    component: () => import('@/views/WorkRequestDetailView.vue')
  },
  {
    path: '/passports',
    name: 'Passports',
    component: () => import('@/views/PassportView.vue')
  },
  {
    path: '/passports/:id',
    name: 'PassportDetail',
    component: () => import('@/views/PassportDetailView.vue')
  },
  {
    path: '/credits',
    name: 'Credits',
    component: () => import('@/views/CreditsView.vue')
  },
  {
    path: '/admin',
    name: 'AdminPending',
    component: () => import('@/views/AdminPendingView.vue')
  },
  {
    path: '/admin/history',
    name: 'AdminHistory',
    component: () => import('@/views/AdminHistoryView.vue')
  },
  {
    path: '/admin/passports/:id',
    name: 'AdminPassportDetail',
    component: () => import('@/views/AdminPassportDetailView.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { guestOnly: true }
  },
  {
    path: '/callback',
    name: 'Callback',
    component: () => import('@/views/CallbackView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// 인증/권한 가드
router.beforeEach((to) => {
  const auth = useAuthStore()
  const requiresAuth = !['Login', 'Callback'].includes(to.name)
  const isAdminRoute = to.path.startsWith('/admin')

  if (requiresAuth && !auth.isAuthenticated) {
    return { name: 'Login' }
  }

  if (to.meta.guestOnly && auth.isAuthenticated) {
    return { name: auth.isInstructor ? 'AdminPending' : 'WorkRequests' }
  }

  if (requiresAuth && auth.isInstructor && !isAdminRoute) {
    return { name: 'AdminPending' }
  }

  if (requiresAuth && !auth.isInstructor && isAdminRoute) {
    return { name: 'WorkRequests' }
  }
})

export default router
