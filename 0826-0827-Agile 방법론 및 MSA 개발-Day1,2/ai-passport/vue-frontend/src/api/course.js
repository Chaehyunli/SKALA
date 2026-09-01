import api from './index.js'
import { mockGetMyList, mockGetById, mockCreate, mockAnalyze } from '@/mocks/courses.js'

const USE_MOCK = false // 백엔드 연동 — mock 미사용

export const courseApi = {
  // POST /api/courses
  create(data) {
    if (USE_MOCK) return mockCreate(data).then((data) => ({ data }))
    return api.post('/api/courses', data)
  },

  // POST /api/courses/{id}/analyze
  analyze(id) {
    if (USE_MOCK) return mockAnalyze(id).then((data) => ({ data }))
    // Local Ollama performs two sequential analysis calls, so it can exceed the
    // short timeout used by ordinary UI requests.
    return api.post(`/api/courses/${id}/analyze`, null, { timeout: 130000 })
  },

  // GET /api/courses/my
  getMyList() {
    if (USE_MOCK) return mockGetMyList().then((data) => ({ data }))
    return api.get('/api/courses/my')
  },

  // GET /api/courses/{id}
  getById(id) {
    if (USE_MOCK) return mockGetById(id).then((data) => ({ data }))
    return api.get(`/api/courses/${id}`)
  }
}
