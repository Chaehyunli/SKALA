// enrollment-service 백엔드가 준비되기 전까지 쓰는 인메모리 목데이터.
// Notion EPIC 2([BE] Passport 신청/목록/상세 조회 API) 응답 형식을 기본으로 하고,
// PassportView/PassportDetailView의 상세 UI(승인 이력, 연결된 업무 요청 등)를 위해
// 계약에 없는 표시용 필드(owner/history/linkedRequest/statusInfo 등)를 mock에서만 덧붙인다.

import { getCourseSnapshot, setCoursePassportStatus } from './courses.js'
import { AGENTS, PERMISSIONS, USAGE_PERIOD_LABEL } from '@/constants/workRequest.js'

function delay(data) {
  return new Promise((resolve) => setTimeout(() => resolve(data), 300))
}

const store = new Map()
let idSeq = 32

function withPermissionMeta(list) {
  return (list || []).map((p) => ({ ...p, name: PERMISSIONS[p.code]?.name || p.code, description: p.reason }))
}

// Agent가 가질 수 있는 전체 권한 중 이번 Passport에는 포함되지 않은 권한 = 차단된 권한
function blockedPermissionsFor(agentCode, grantedCodes) {
  const agent = AGENTS[agentCode]
  if (!agent) return []
  return agent.permissions
    .filter((code) => !grantedCodes.includes(code))
    .map((code) => ({
      code,
      name: PERMISSIONS[code]?.name || code,
      reason: '이번 미션 범위에는 필요하지 않아 Passport에서 제외했습니다.'
    }))
}

function normalizeMockAgentList(data) {
  const source = data.agentList || [{
    agentCode: data.agentCode,
    permissions: data.permissions || [],
    excludedPermissions: data.excludedPermissions
  }]

  return source.map((agent) => {
    const permissions = withPermissionMeta(agent.permissions)
    const excludedPermissions = withPermissionMeta(
      agent.excludedPermissions || blockedPermissionsFor(agent.agentCode, permissions.map((p) => p.code))
    )
    return { ...agent, permissions, excludedPermissions }
  })
}

function buildHistory(status, { requestedAt, approvedBy, approvedAt, rejectedReason }) {
  const base = [
    { label: '미션 등록', description: '미션이 등록되었습니다.', time: requestedAt, tone: 'done' },
    { label: '권한 설계 완료', description: '권한 제안이 생성되었습니다.', time: requestedAt, tone: 'done' }
  ]
  if (status === 'ACTIVE') {
    base.push({ label: 'Passport 발급', description: approvedBy, time: approvedAt, tone: 'done' })
  } else if (status === 'REJECTED') {
    base.push({ label: '발급 요청 반려', description: rejectedReason, time: approvedAt, tone: 'current' })
  } else {
    base.push({ label: '발급 검토 중', description: '담당자가 발급 요청을 검토하고 있습니다.', time: '-', tone: 'current' })
  }
  return base
}

function seed(id, courseId, data) {
  const course = getCourseSnapshot(courseId)
  const agentList = normalizeMockAgentList(data)

  store.set(id, {
    id,
    courseId,
    courseTitle: course?.courseTitle,
    task: course?.task,
    usagePeriod: data.usagePeriod,
    validPeriod: USAGE_PERIOD_LABEL[data.usagePeriod] || data.usagePeriod,
    owner: { name: '김민지', team: 'Sales Ops' },
    riskLabel: `위험도 ${data.riskLevel === 'HIGH' ? '높음' : data.riskLevel === 'MEDIUM' ? '보통' : '낮음'}`,
    statusInfo: data.statusInfo,
    linkedRequest: { title: course?.courseTitle, description: course?.task },
    agentList,
    history: buildHistory(data.status, {
      requestedAt: data.createdAt,
      approvedBy: data.approvedBy,
      approvedAt: data.approvedAt,
      rejectedReason: data.rejectedReason
    }),
    ...data,
    agentList
  })
}

seed(31, 'WR-1024', {
  agentCode: 'REVENUE_ANALYST',
  permissions: [
    { code: 'CUSTOMER_READ', reason: '고객 ID, 연락처, 구매 이력 등 기본 정보를 조회하기 위해 필요합니다.' },
    { code: 'REVENUE_READ', reason: '고객별 매출 및 최근 활동 데이터를 조회하기 위해 필요합니다.' },
    { code: 'MAIL_DRAFT_CREATE', reason: '리텐션 이메일 초안을 작성하고 저장하기 위해 필요합니다.' }
  ],
  riskLevel: 'MEDIUM',
  status: 'ACTIVE',
  approvedBy: '박준호 · Security Admin',
  approvedAt: '오늘 10:20',
  rejectedReason: null,
  reviewReason: '고객에게 이메일을 직접 발송하는 권한은 담당자 확인 후 사용해야 하므로 제외되었습니다.',
  createdAt: '오늘 10:03',
  usagePeriod: 'HOURS_24',
  statusInfo: { remainingTime: '18시간 32분 남음', progress: 24, issuer: '박준호 · Security Admin', approvedAt: '오늘 10:20' }
})

seed(30, 'WR-1023', {
  agentCode: 'CUSTOMER_SUPPORT',
  permissions: [
    { code: 'SUPPORT_TICKET_READ', reason: '접수된 VOC 내용과 유형을 확인하기 위해 필요합니다.' },
    { code: 'CUSTOMER_READ', reason: '문의한 고객의 기본 정보를 확인하기 위해 필요합니다.' },
    { code: 'SUPPORT_REPLY_DRAFT', reason: '고객에게 보낼 답변 초안을 작성하기 위해 필요합니다.' }
  ],
  riskLevel: 'MEDIUM',
  status: 'READY_FOR_APPROVAL',
  approvedBy: null,
  approvedAt: null,
  rejectedReason: null,
  reviewReason: '고객에게 이메일을 직접 발송하는 권한은 담당자 확인 후 사용해야 하므로 제외되었습니다.',
  createdAt: '오늘 08:21',
  usagePeriod: 'HOURS_24',
  statusInfo: { remainingTime: '검토 대기', progress: 0, issuer: '-', approvedAt: '-' }
})

function toListItem(record) {
  const { id, courseId, courseTitle, agentList, riskLevel, status, createdAt } = record
  const permissionCount = agentList.reduce((total, agent) => total + agent.permissions.length, 0)
  return { id, courseId, courseTitle, agentList, permissionCount, riskLevel, status, createdAt }
}

export function mockGetMyPassports() {
  return delay(
    [...store.values()]
      .sort((a, b) => b.id - a.id)
      .map(toListItem)
  )
}

export function mockGetPassport(id) {
  return delay(store.get(Number(id)) || null)
}

// POST /api/enrollments 요청 바디({courseId, agentList, riskLevel, summary})를 그대로 받아 Passport 생성.
// 응답 계약: {id, courseId, userId, agentList, riskLevel, status, createdAt}
export function mockCreatePassport({ courseId, agentList, riskLevel }) {
  const id = idSeq++
  const course = getCourseSnapshot(courseId)
  const createdAt = '방금 전'
  const normalizedAgentList = normalizeMockAgentList({ agentList })

  const record = {
    id,
    courseId,
    userId: 1,
    courseTitle: course?.courseTitle,
    task: course?.task,
    agentList: normalizedAgentList,
    riskLevel,
    status: 'READY_FOR_APPROVAL',
    approvedBy: null,
    approvedAt: null,
    rejectedReason: null,
    reviewReason: '직접 실행·발송 권한은 이번 미션 범위를 초과하므로 제외했습니다.',
    createdAt,
    usagePeriod: 'HOURS_24',
    validPeriod: USAGE_PERIOD_LABEL.HOURS_24,
    owner: { name: '김민지', team: 'Sales Ops' },
    riskLabel: `위험도 ${riskLevel === 'HIGH' ? '높음' : riskLevel === 'MEDIUM' ? '보통' : '낮음'}`,
    statusInfo: { remainingTime: '검토 대기', progress: 0, issuer: '-', approvedAt: '-' },
    linkedRequest: { title: course?.courseTitle, description: course?.task },
    history: buildHistory('READY_FOR_APPROVAL', { requestedAt: createdAt })
  }
  store.set(id, record)
  setCoursePassportStatus(courseId, 'READY_FOR_APPROVAL')
  return delay(record)
}

// GET /api/enrollments/admin/pending — READY_FOR_APPROVAL만, 오래된 신청부터 정렬
export function mockGetAdminPending() {
  return delay(
    [...store.values()]
      .filter((r) => r.status === 'READY_FOR_APPROVAL')
      .sort((a, b) => a.id - b.id)
      .map((r) => ({
        id: r.id,
        courseId: r.courseId,
        userId: 1,
        courseTitle: r.courseTitle,
        agentList: r.agentList,
        permissionCount: r.agentList.reduce((total, agent) => total + agent.permissions.length, 0),
        riskLevel: r.riskLevel,
        status: r.status,
        createdAt: r.createdAt
      }))
  )
}

// PATCH /api/enrollments/{passportId}/approve — 처리: READY_FOR_APPROVAL → ACTIVE
// 실제 응답은 {id, status, approvedBy, approvedAt}만 반환(approvedBy는 관리자 user id 숫자).
// mock은 화면에 바로 쓸 수 있게 전체 레코드를 갱신해서 반환한다.
export function mockApprovePassport(id) {
  const record = store.get(Number(id))
  if (!record) return delay(null)
  if (record.status !== 'READY_FOR_APPROVAL') return Promise.reject(new Error('이미 처리된 Passport입니다.'))

  const approvedAt = '방금 전'
  record.status = 'ACTIVE'
  record.approvedBy = '박준호 · Security Admin' // 실제 계약은 숫자 admin user id
  record.approvedAt = approvedAt
  record.statusInfo = { remainingTime: `${USAGE_PERIOD_LABEL[record.usagePeriod] || record.usagePeriod} 사용 가능`, progress: 0, issuer: record.approvedBy, approvedAt }
  record.history = buildHistory('ACTIVE', { requestedAt: record.createdAt, approvedBy: record.approvedBy, approvedAt })
  setCoursePassportStatus(record.courseId, 'ACTIVE')
  return delay(record)
}

// PATCH /api/enrollments/{passportId}/reject — body: {reason}. 처리: READY_FOR_APPROVAL → REJECTED
export function mockRejectPassport(id, reason) {
  const record = store.get(Number(id))
  if (!record) return delay(null)
  if (record.status !== 'READY_FOR_APPROVAL') return Promise.reject(new Error('이미 처리된 Passport입니다.'))

  const approvedAt = '방금 전'
  record.status = 'REJECTED'
  record.rejectedReason = reason
  record.approvedBy = '박준호 · Security Admin'
  record.approvedAt = approvedAt
  record.history = buildHistory('REJECTED', { requestedAt: record.createdAt, rejectedReason: reason, approvedAt })
  setCoursePassportStatus(record.courseId, 'REJECTED')
  return delay(record)
}

// GET /api/enrollments/admin/all — Notion에 없는 엔드포인트(가정). 관리자용 "전체 신청 이력" 화면에서 사용.
// mockGetAdminPending과 달리 상태 필터링 없이 READY_FOR_APPROVAL/ACTIVE/REJECTED 전부 반환.
export function mockGetAdminAll() {
  return delay(
    [...store.values()]
      .sort((a, b) => b.id - a.id)
      .map((r) => ({
        id: r.id,
        courseId: r.courseId,
        userId: 1,
        courseTitle: r.courseTitle,
        agentList: r.agentList,
        permissionCount: r.agentList.reduce((total, agent) => total + agent.permissions.length, 0),
        riskLevel: r.riskLevel,
        status: r.status,
        createdAt: r.createdAt,
        approvedBy: r.approvedBy,
        approvedAt: r.approvedAt,
        rejectedReason: r.rejectedReason
      }))
  )
}
