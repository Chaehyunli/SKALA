import api from './index.js'
import { mockGetBalance, mockCharge } from '@/mocks/payment.js'

const USE_MOCK = false // 백엔드 연동 — mock 미사용

export const paymentApi = {
  getBalance() {
    if (USE_MOCK) return mockGetBalance().then((data) => ({ data }))
    return api.get('/api/payments/balance')
  },

  charge(payload) {
    if (USE_MOCK) return mockCharge(payload).then((data) => ({ data }))
    return api.post('/api/payments/charge', payload)
  }
}
