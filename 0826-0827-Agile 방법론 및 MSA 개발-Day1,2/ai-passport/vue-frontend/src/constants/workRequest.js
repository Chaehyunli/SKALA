// 업무 요청(Course) 도메인 공용 상수 — 라벨/색상 매핑을 한 곳에서 관리

export const CATEGORY_OPTIONS = [
  { code: 'CUSTOMER_ANALYSIS', label: '고객 분석' },
  { code: 'ANALYSIS_TASK', label: '분석 작업' },
  { code: 'DATA_LOOKUP', label: '데이터 조회' },
  { code: 'COMMUNICATION', label: '커뮤니케이션' },
  { code: 'DOCUMENT', label: '문서 작성' }
]

export const CATEGORY_LABEL = Object.fromEntries(
  CATEGORY_OPTIONS.map((opt) => [opt.code, opt.label])
)

// Notion "상수" 명세(PassportDuration enum)와 코드값 일치
export const USAGE_PERIOD_OPTIONS = [
  { code: 'ONE_TIME', label: '1회' },
  { code: 'HOURS_24', label: '24시간' },
  { code: 'DAYS_7', label: '7일' }
]

export const USAGE_PERIOD_LABEL = Object.fromEntries(
  USAGE_PERIOD_OPTIONS.map((opt) => [opt.code, opt.label])
)

export const DATA_SENSITIVITY_OPTIONS = ['공개', '사내 제한', '기밀']

// Notion "상수" 명세(Permission 상수 예시)와 코드/표시명/위험도 일치
export const PERMISSIONS = {
  CUSTOMER_READ: { name: 'CRM 고객정보 조회', riskLevel: 'LOW' },
  REVENUE_READ: { name: '매출 데이터 조회', riskLevel: 'LOW' },
  MAIL_DRAFT_CREATE: { name: '이메일 초안 작성', riskLevel: 'MEDIUM' },
  MAIL_SEND: { name: '이메일 직접 발송', riskLevel: 'HIGH' },
  SUPPORT_TICKET_READ: { name: '고객 문의 조회', riskLevel: 'LOW' },
  SUPPORT_REPLY_DRAFT: { name: '문의 답변 초안 작성', riskLevel: 'MEDIUM' },
  DOCUMENT_READ: { name: '사내 문서 조회', riskLevel: 'LOW' },
  DOCUMENT_DRAFT_CREATE: { name: '문서 초안 작성', riskLevel: 'MEDIUM' },
  CALENDAR_READ: { name: '일정 조회', riskLevel: 'LOW' },
  CALENDAR_EVENT_CREATE: { name: '일정 생성', riskLevel: 'HIGH' }
}

// Notion "상수" 명세(Agent 상수 예시)와 코드/표시명/허용 권한 일치
export const AGENTS = {
  REVENUE_ANALYST: {
    name: 'Revenue Analyst Agent',
    permissions: ['CUSTOMER_READ', 'REVENUE_READ', 'MAIL_DRAFT_CREATE', 'MAIL_SEND']
  },
  CUSTOMER_SUPPORT: {
    name: 'Customer Support Agent',
    permissions: ['CUSTOMER_READ', 'SUPPORT_TICKET_READ', 'SUPPORT_REPLY_DRAFT', 'MAIL_DRAFT_CREATE', 'MAIL_SEND']
  },
  COMMON_AGENT: {
    name: 'Common Agent',
    permissions: ['DOCUMENT_READ', 'DOCUMENT_DRAFT_CREATE', 'MAIL_DRAFT_CREATE']
  },
  MEETING_COORDINATOR: {
    name: 'Meeting Coordinator Agent',
    permissions: ['CALENDAR_READ', 'CALENDAR_EVENT_CREATE', 'DOCUMENT_READ', 'MAIL_DRAFT_CREATE']
  }
}

const RISK_ORDER = ['LOW', 'MEDIUM', 'HIGH']

// Notion "상수" 명세 7번 규칙: HIGH가 하나라도 있으면 HIGH, 아니면 MEDIUM 있으면 MEDIUM, 전부 읽기면 LOW
export function calcRiskLevel(permissionCodes) {
  return permissionCodes.reduce((max, code) => {
    const level = PERMISSIONS[code]?.riskLevel || 'LOW'
    return RISK_ORDER.indexOf(level) > RISK_ORDER.indexOf(max) ? level : max
  }, 'LOW')
}

// LLM 반환 형식(permissions/excludedPermissions: {code,reason}[])을 화면 표시용 단일 리스트로 합침
export function buildPermissionRows(analysis) {
  const granted = (analysis?.permissions || []).map((p) => ({ ...PERMISSIONS[p.code], ...p, allowed: true }))
  const excluded = (analysis?.excludedPermissions || []).map((p) => ({ ...PERMISSIONS[p.code], ...p, allowed: false }))
  return [...granted, ...excluded]
}

// 새 Enrollment 계약은 Passport 하나에 여러 Agent를 포함한다.
// Course 분석 응답이 아직 단일 Agent 형식인 경우에도 1개짜리 목록으로 변환해 연결한다.
export function normalizeAgentList(source) {
  if (Array.isArray(source?.agentList)) {
    return source.agentList.filter((agent) => agent?.agentCode)
  }

  if (!source?.agentCode) return []
  return [{
    agentCode: source.agentCode,
    permissions: source.permissions || [],
    excludedPermissions: source.excludedPermissions || []
  }]
}

export function getAgentName(agentCode) {
  return AGENTS[agentCode]?.name || agentCode || '-'
}

export function getAgentSummary(source) {
  const agentList = normalizeAgentList(source)
  if (!agentList.length) return '-'
  if (agentList.length === 1) return getAgentName(agentList[0].agentCode)
  return `${getAgentName(agentList[0].agentCode)} 외 ${agentList.length - 1}개`
}

export function countAgentPermissions(source) {
  return normalizeAgentList(source).reduce(
    (total, agent) => total + (agent.permissions?.length || 0),
    0
  )
}

// BE 업무 요청 분석 상태 (CREATED | ANALYZING | ANALYZED | FAILED)
export const WORK_STATUS_META = {
  CREATED: { label: '설계 대기', type: 'created' },
  ANALYZING: { label: '설계 중', type: 'progress' },
  ANALYZED: { label: '설계 완료', type: 'done' },
  FAILED: { label: '설계 실패', type: 'failed' }
}

// Passport(Enrollment) 승인 상태 — Notion EPIC 2 "[BE] Passport 승인/반려 API" 명세와 코드 일치
// 승인 API 응답의 status가 "APPROVED"가 아니라 "ACTIVE"로 바뀐다 (ENROLLMENT-06 처리: READY_FOR_APPROVAL → ACTIVE)
// NONE은 실제 BE 값이 아니라 "아직 Passport를 신청하지 않음"을 나타내는 FE 전용 상태
export const PASSPORT_STATUS_META = {
  NONE: { label: '미발급', type: 'none' },
  READY_FOR_APPROVAL: { label: '검토 대기', type: 'pending' },
  ACTIVE: { label: '사용 가능', type: 'active' },
  REJECTED: { label: '반려', type: 'rejected' },
  EXPIRED: { label: '만료', type: 'none' }
}

// 업무 요청과 Passport 목록에서 같은 이름을 사용하도록 화면 표시명을 한 곳에서 관리한다.
const PASSPORT_NAME_BY_WORK = {
  '이탈 고객 리텐션 캠페인': '리텐션 캠페인 Passport',
  'VOC 답변 초안 작성': 'VOC 답변 Passport',
  '주간 경영 보고서 요약': '경영 보고서 Passport',
  '고객 미팅 일정 조율': '고객 일정 Passport'
}

export function getPassportDisplayName(workTitle) {
  if (!workTitle) return '미션 Passport'
  return PASSPORT_NAME_BY_WORK[workTitle] || `${workTitle} Passport`
}

export const RISK_LEVEL_META = {
  LOW: { label: '낮음', color: 'success' },
  MEDIUM: { label: '보통', color: 'warning' },
  HIGH: { label: '높음', color: 'error' }
}
