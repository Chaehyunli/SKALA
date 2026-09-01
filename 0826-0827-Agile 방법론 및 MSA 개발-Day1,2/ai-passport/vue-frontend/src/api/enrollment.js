import api from './index.js'
import {
  mockGetMyPassports,
  mockGetPassport,
  mockCreatePassport,
  mockGetAdminPending,
  mockApprovePassport,
  mockRejectPassport,
  mockGetAdminAll
} from '@/mocks/passports.js'

const USE_MOCK = false // 백엔드 연동 — mock 미사용
// All runtime API calls must use the real backend. Keep this disabled unless a
// deliberately isolated UI demo is required.
const USE_PASSPORT_REUSE_MOCK = false
const PASSPORT_REUSE_PREVIEW_ID = 'reuse-preview'

export const enrollmentApi = {
  // POST /api/enrollments — Passport 신청. body: {courseId, agentList, riskLevel, summary}
  enroll(payload) {
    if (USE_MOCK) return mockCreatePassport(payload).then((data) => ({ data }))
    return api.post('/api/enrollments', payload)
  },

  // GET /api/enrollments/my — 내 Passport 목록
  getMyPassports() {
    if (USE_MOCK) return mockGetMyPassports().then((data) => ({ data }))
    return api.get('/api/enrollments/my')
  },

  // GET /api/enrollments/{passportId} — Passport 상세
  getPassport(id) {
    if (USE_PASSPORT_REUSE_MOCK && String(id) === PASSPORT_REUSE_PREVIEW_ID) {
      return mockGetPassport(31).then((data) => ({ data }))
    }
    if (USE_MOCK) return mockGetPassport(id).then((data) => ({ data }))
    return api.get(`/api/enrollments/${id}`)
  },

  // GET /api/enrollments/my/course/{courseId} — 업무 요청과 연결된 내 Passport
  getByCourse(courseId) {
    return api.get(`/api/enrollments/my/course/${courseId}`)
  },

  // GET /api/enrollments/admin/ready-for-approval — 관리자 승인 대기 목록 (READY_FOR_APPROVAL만)
  getAdminPending() {
    if (USE_MOCK) return mockGetAdminPending().then((data) => ({ data }))
    return api.get('/api/enrollments/admin/ready-for-approval')
  },

  // GET /api/enrollments/admin/{passportId} — 관리자 Passport 상세
  getAdminPassport(id) {
    return api.get(`/api/enrollments/admin/${id}`)
  },

  // GET /api/enrollments/{passportId}/passport.yml — 에이전트용 권한 매니페스트 YAML
  exportPassportYaml(id) {
    return api.get(`/api/enrollments/${id}/passport.yml`, { responseType: 'text' })
  },

  // GET /api/enrollments/admin/{passportId}/passport.yml — 관리자용 권한 매니페스트 YAML
  exportAdminPassportYaml(id) {
    return api.get(`/api/enrollments/admin/${id}/passport.yml`, { responseType: 'text' })
  },

  // PUT /api/enrollments/{passportId}/approve — READY_FOR_APPROVAL → ACTIVE
  approve(id) {
    if (USE_MOCK) return mockApprovePassport(id).then((data) => ({ data }))
    return api.put(`/api/enrollments/${id}/approve`)
  },

  // PUT /api/enrollments/{passportId}/reject — body: {reason}. READY_FOR_APPROVAL → REJECTED
  reject(id, reason) {
    if (USE_MOCK) return mockRejectPassport(id, reason).then((data) => ({ data }))
    return api.put(`/api/enrollments/${id}/reject`, { reason })
  },

  // GET /api/enrollments/admin/all — Notion에 없는 엔드포인트(가정). 관리자 "전체 신청 이력" 화면용.
  getAdminAll() {
    if (USE_MOCK) return mockGetAdminAll().then((data) => ({ data }))
    return api.get('/api/enrollments/admin/all')
  },

  cancel(enrollmentId) {
    return api.delete(`/api/enrollments/${enrollmentId}`)
  },

  // GET /api/recommend/passport-reuse/{missionId} — 기존 Passport 재사용 가능 여부
  checkPassportReuse(missionId) {
    if (USE_PASSPORT_REUSE_MOCK) {
      return Promise.resolve({
        data: {
          missionId,
          decision: 'REUSE',
          reusable: true,
          passportId: PASSPORT_REUSE_PREVIEW_ID,
          message: '요청한 Agent와 권한을 포함한 기존 Passport를 사용할 수 있습니다.'
        }
      })
    }
    return api.get(`/api/recommend/passport-reuse/${missionId}`)
  }
}
