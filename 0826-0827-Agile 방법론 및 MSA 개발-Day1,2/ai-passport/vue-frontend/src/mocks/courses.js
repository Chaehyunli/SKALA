// course-service 백엔드가 준비되기 전까지 쓰는 인메모리 목데이터.
// api/course.js가 이 store를 통해 create → analyze → get 흐름을 그대로 시뮬레이션한다.
// 백엔드 연동 시에는 이 파일과 api/course.js의 mock 분기만 걷어내면 된다.
//
// 권한/에이전트 코드와 위험도 계산은 Notion "상수 ? 정해야함 같이" 명세를 그대로 따른다.

import { AGENTS, PERMISSIONS, calcRiskLevel, CATEGORY_LABEL } from '@/constants/workRequest.js'

function delay(data) {
  return new Promise((resolve) => setTimeout(() => resolve(data), 300))
}

// 업무 유형 → 담당 Agent. 실제로는 LLM이 판단하지만 mock에서는 카테고리로 대체.
const CATEGORY_AGENT = {
  CUSTOMER_ANALYSIS: 'REVENUE_ANALYST',
  ANALYSIS_TASK: 'REVENUE_ANALYST',
  DATA_LOOKUP: 'REVENUE_ANALYST',
  COMMUNICATION: 'CUSTOMER_SUPPORT',
  DOCUMENT: 'COMMON_AGENT'
}

const APPROVAL_STEPS = ['미션 등록', '권한 설계 완료', '발급 검토', 'Passport 발급']

function buildAnalysis(agentCode, title, category, excludeReason) {
  const agent = AGENTS[agentCode]
  // mock 전용 규칙: HIGH 위험 권한은 기본 제외, 나머지는 허용 — Notion 명세 예시(REVENUE_ANALYST에서 MAIL_SEND 제외)와 동일한 패턴
  const granted = agent.permissions.filter((code) => PERMISSIONS[code].riskLevel !== 'HIGH')
  const excluded = agent.permissions.filter((code) => PERMISSIONS[code].riskLevel === 'HIGH')

  return {
    agentCode,
    fitScore: 90 + Math.floor(Math.random() * 8), // 실제 LLM 반환 형식엔 없는 mock 전용 표시값
    permissions: granted.map((code) => ({ code, reason: `${title} 미션 수행에 필요한 정보입니다.` })),
    excludedPermissions: excluded.map((code) => ({ code, reason: excludeReason })),
    riskLevel: calcRiskLevel(granted),
    summary: `${agent.name}가 ${CATEGORY_LABEL[category] || ''} 미션을 수행하기 위한 최소 권한입니다.`
  }
}

function buildApproval(requestNo) {
  return {
    requestNo,
    steps: APPROVAL_STEPS,
    currentStep: 2,
    approver: '박준호 · Security Admin',
    requestedAt: '오늘',
    expectedReviewTime: '평균 2시간 이내',
    notify: '완료 시 이메일과 앱 알림'
  }
}

const store = new Map()
let idSeq = 1025

function seed(id, data) {
  store.set(id, { id, ...data })
}

seed('WR-1024', {
  title: '이탈 고객 리텐션 캠페인',
  description: '이번 달 고객 매출과 최근 활동을 비교해 이탈 가능성이 높은 고객을 찾고, 담당자가 검토할 리텐션 이메일 초안을 작성해줘.',
  category: 'CUSTOMER_ANALYSIS',
  status: 'ANALYZED',
  createdAt: '오늘 10:02',
  passportStatus: 'ACTIVE',
  passportId: 31,
  analysis: buildAnalysis(
    'REVENUE_ANALYST',
    '이탈 고객 리텐션 캠페인',
    'CUSTOMER_ANALYSIS',
    '고객에게 이메일을 직접 발송하는 권한은 이번 미션 범위를 초과합니다.'
  ),
  approval: buildApproval('WR-1024')
})

seed('WR-1023', {
  title: 'VOC 답변 초안 작성',
  description: '최근 접수된 VOC를 분석해 유형을 분류하고, 고객에게 보낼 답변 초안을 작성해줘.',
  category: 'COMMUNICATION',
  status: 'ANALYZED',
  createdAt: '오늘 08:20',
  passportStatus: 'READY_FOR_APPROVAL',
  passportId: 30,
  analysis: buildAnalysis(
    'CUSTOMER_SUPPORT',
    'VOC 답변 초안 작성',
    'COMMUNICATION',
    '고객에게 이메일을 직접 발송하는 권한은 이번 미션 범위를 초과합니다.'
  ),
  approval: buildApproval('WR-1023')
})

seed('WR-1022', {
  title: '주간 경영 보고서 요약',
  description: '이번 주 부서별 실적 데이터를 취합해 경영 보고용 요약본을 작성해줘.',
  category: 'DOCUMENT',
  status: 'ANALYZING',
  createdAt: '어제',
  passportStatus: 'NONE',
  analysis: null,
  approval: null
})

seed('WR-1021', {
  title: '고객 미팅 일정 조율',
  description: '주요 고객사와의 다음 미팅 일정을 담당자 캘린더와 비교해 조율해줘.',
  category: 'COMMUNICATION',
  status: 'FAILED',
  createdAt: '2026.08.22',
  passportStatus: 'NONE',
  analysis: null,
  approval: null
})

function toListItem(record) {
  const { id, title, category, status, createdAt, passportStatus } = record
  return { id, title, category, status, createdAt, agentCode: record.analysis?.agentCode || null, passportStatus, passportId: record.passportId }
}

export function mockGetMyList() {
  return delay([...store.values()].map(toListItem))
}

export function mockGetById(id) {
  return delay(store.get(id) || null)
}

export function mockCreate({ title, description, category }) {
  const id = `WR-${idSeq++}`
  const record = {
    id,
    title,
    description,
    category,
    status: 'CREATED',
    createdAt: '방금 전',
    passportStatus: 'NONE',
    analysis: null,
    approval: null
  }
  store.set(id, record)
  return delay(record)
}

export function mockAnalyze(id) {
  const record = store.get(id)
  if (!record) return delay(null)

  const agentCode = CATEGORY_AGENT[record.category] || 'REVENUE_ANALYST'
  record.status = 'ANALYZED'
  record.analysis = buildAnalysis(
    agentCode,
    record.title,
    record.category,
    '직접 실행·발송 권한은 이번 미션 범위를 초과합니다.'
  )
  record.approval = buildApproval(id)
  return delay(record)
}

// mocks/passports.js(enrollment 도메인)에서 Passport 신청/승인 결과를 course 쪽에도 반영하기 위한 헬퍼.
// 실제로는 두 서비스가 별도 DB를 쓰므로 FE가 두 응답을 조합해야 하지만, mock 데모 편의상 직접 동기화한다.
export function getCourseSnapshot(id) {
  const record = store.get(id)
  return record ? { courseId: record.id, courseTitle: record.title, task: record.description } : null
}

export function setCoursePassportStatus(id, status) {
  const record = store.get(id)
  if (record) record.passportStatus = status
}
