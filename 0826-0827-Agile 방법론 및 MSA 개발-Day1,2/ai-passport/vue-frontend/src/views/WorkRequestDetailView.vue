<template>
  <MainLayout>
    <div class="content-page" v-if="record">
      <template v-if="record.approval">
        <p class="eyebrow">{{ statusCopy.eyebrow }}</p>
        <h1>{{ statusCopy.title }}</h1>
        <p class="description">{{ statusCopy.description }}</p>

        <a-card class="steps-card" :bordered="false">
          <div class="steps-head">
            <span>발급 요청 ID {{ record.approval.requestNo }}</span>
            <a-tag :color="statusMeta.color">{{ statusMeta.label }}</a-tag>
          </div>
          <a-steps :current="currentStep" :items="stepItems" responsive />
        </a-card>

        <div class="detail-grid">
          <a-card class="detail-card" :bordered="false">
            <h3>{{ passportName }}</h3>
            <p class="passport-subtitle">이 미션에 연결된 Passport</p>
            <dl>
              <div><dt>실행 Agent</dt><dd>{{ agentSummary }}</dd></div>
              <div><dt>미션</dt><dd>{{ record.title }}</dd></div>
              <div>
                <dt>허용 권한</dt>
                <dd class="permission-tags">
                  <a-tag v-for="p in allowedPermissions" :key="p.key" color="success">{{ p.name }}</a-tag>
                </dd>
              </div>
              <div><dt>사용 기간</dt><dd>{{ usagePeriodLabel }}</dd></div>
            </dl>
          </a-card>

          <a-card class="detail-card" :bordered="false">
            <h3>발급 검토</h3>
            <dl>
              <div><dt>검토 담당자</dt><dd>{{ record.approval.approver }}</dd></div>
              <div><dt>요청 일시</dt><dd>{{ record.approval.requestedAt }}</dd></div>
              <div><dt>예상 검토 시간</dt><dd>{{ record.approval.expectedReviewTime }}</dd></div>
              <div><dt>알림 설정</dt><dd>{{ record.approval.notify }}</dd></div>
            </dl>
          </a-card>
        </div>

        <div class="actions">
          <a-button
            v-if="record.passportStatus === 'READY_FOR_APPROVAL'"
            danger
            :loading="cancelling"
            @click="handleCancel"
          >발급 요청 취소</a-button>
          <a-button type="link" @click="router.push('/')">미션 보드로 이동 →</a-button>
        </div>
      </template>

      <template v-else>
        <a-button class="back-button" @click="router.push('/')"><LeftOutlined /> 목록으로</a-button>
        <p class="eyebrow">{{ nextStepCopy.eyebrow }}</p>
        <h1>{{ record.title }}</h1>
        <p class="description">{{ nextStepCopy.description }}</p>

        <a-card class="detail-card request-detail-card" :bordered="false">
          <div class="request-status-head">
            <h3>미션 정보</h3>
            <a-tag :color="courseStatusMeta.color">{{ courseStatusMeta.label }}</a-tag>
          </div>
          <dl>
            <div><dt>미션 이름</dt><dd>{{ record.title }}</dd></div>
            <div><dt>미션 브리프</dt><dd>{{ record.description || '등록된 브리프가 없습니다.' }}</dd></div>
            <div><dt>미션 유형</dt><dd>{{ categoryLabel }}</dd></div>
            <div><dt>생성일</dt><dd>{{ record.createdAt || '-' }}</dd></div>
          </dl>

          <div v-if="record.analysis?.summary" class="analysis-preview">
            <span>권한 설계 요약</span>
            <p>{{ record.analysis.summary }}</p>
          </div>
        </a-card>

        <div class="actions next-actions">
          <a-button type="link" @click="router.push('/')">미션 보드로 이동</a-button>
          <a-button
            v-if="record.status === 'ANALYZED' && record.analysis"
            type="primary"
            size="large"
            @click="router.push(`/work-requests/${record.id}/analysis`)"
          >권한 제안 확인 · Passport 발급 요청</a-button>
          <a-button
            v-else-if="record.status === 'CREATED' || record.status === 'FAILED'"
            type="primary"
            size="large"
            :loading="analyzing"
            @click="handleAnalyze"
          >{{ record.status === 'FAILED' ? '권한 설계 다시 시작' : '권한 설계 시작' }}</a-button>
          <a-button v-else size="large" disabled loading>권한 설계 중</a-button>
        </div>
      </template>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { LeftOutlined } from '@ant-design/icons-vue'
import MainLayout from '@/layouts/MainLayout.vue'
import { courseApi } from '@/api/course.js'
import { enrollmentApi } from '@/api/enrollment.js'
import { message } from 'ant-design-vue'
import { CATEGORY_LABEL, getAgentSummary, getPassportDisplayName, normalizeAgentList, PASSPORT_STATUS_META, USAGE_PERIOD_LABEL } from '@/constants/workRequest.js'

const route = useRoute()
const router = useRouter()
const record = ref(null)
const cancelling = ref(false)
const analyzing = ref(false)

const agentSummary = computed(() => getAgentSummary(record.value?.analysis))
const passportName = computed(() => getPassportDisplayName(record.value?.title))
const stepItems = computed(() => (record.value?.approval?.steps || []).map((title) => ({ title })))
const allowedPermissions = computed(
  () => normalizeAgentList(record.value?.analysis)
    .flatMap((agent, agentIndex) => (agent.permissions || []).map((permission) => ({
      ...permission,
      name: permission.label || permission.name || permission.code,
      key: `${agent.agentCode}-${agentIndex}-${permission.code}`
    })))
)
const categoryLabel = computed(() => CATEGORY_LABEL[record.value?.category] || record.value?.category || '-')
const usagePeriodLabel = computed(() => USAGE_PERIOD_LABEL[record.value?.usagePeriod] || record.value?.usagePeriod || '-')

const courseStatusMeta = computed(() => {
  const status = record.value?.status
  if (status === 'ANALYZED') return { label: '설계 완료', color: 'success' }
  if (status === 'FAILED') return { label: '설계 실패', color: 'error' }
  if (status === 'CREATED') return { label: '설계 대기', color: 'default' }
  return { label: '설계 중', color: 'processing' }
})

const nextStepCopy = computed(() => {
  if (record.value?.status === 'ANALYZED' && record.value?.analysis) {
    return {
      eyebrow: 'READY FOR PASSPORT',
      description: '권한 설계가 완료되었습니다. 권한 제안을 확인하고 Passport 발급을 요청합니다.'
    }
  }
  if (record.value?.status === 'FAILED') {
    return { eyebrow: 'DESIGN FAILED', description: '권한 설계를 완료하지 못했습니다. 다시 시작해 다음 단계로 진행합니다.' }
  }
  if (record.value?.status === 'CREATED') {
    return { eyebrow: 'READY TO DESIGN', description: '미션이 등록되었습니다. 권한 설계를 시작합니다.' }
  }
  return { eyebrow: 'DESIGNING', description: '미션에 필요한 권한을 설계하고 있습니다. 완료 후 Passport 발급을 요청할 수 있습니다.' }
})

// 0: 업무 요청 완료, 1: 권한 분석 완료, 2: 관리자 검토 중, 3: Passport 발급
const currentStep = computed(() => (record.value?.passportStatus === 'ACTIVE' ? 3 : 2))

const statusMeta = computed(() => {
  const meta = PASSPORT_STATUS_META[record.value?.passportStatus] || PASSPORT_STATUS_META.READY_FOR_APPROVAL
  const colorByType = { active: 'success', pending: 'warning', rejected: 'error', none: 'default' }
  return { label: meta.label, color: colorByType[meta.type] }
})

const statusCopy = computed(() => {
  if (record.value?.passportStatus === 'ACTIVE') {
    return { eyebrow: 'PASSPORT READY', title: 'Passport 발급 완료', description: '발급된 권한을 바로 사용할 수 있습니다.' }
  }
  if (record.value?.passportStatus === 'REJECTED') {
    return { eyebrow: 'REVIEW RESULT', title: '발급 요청 반려', description: '반려 사유를 확인한 뒤 다시 요청할 수 있습니다.' }
  }
  return { eyebrow: 'ISSUE REVIEW', title: '발급 검토 중', description: '담당자가 발급 요청을 검토하고 있습니다.' }
})

onMounted(async () => {
  const courseRes = await courseApi.getById(route.params.id)
  let enrollment = null
  try {
    enrollment = (await enrollmentApi.getByCourse(route.params.id)).data
  } catch (error) {
    if (error.response?.status !== 400 && error.response?.status !== 404) throw error
  }

  const course = courseRes.data
  record.value = {
    ...course,
    passportStatus: enrollment?.status || 'NONE',
    passportId: enrollment?.id || null,
    approval: enrollment
      ? {
          requestNo: enrollment.id,
          steps: ['미션 등록', '권한 설계', '발급 검토', 'Passport 발급'],
          approver: enrollment.approvedBy || 'Security Admin',
          requestedAt: enrollment.createdAt || '-',
          expectedReviewTime: enrollment.status === 'READY_FOR_APPROVAL' ? '검토 진행 중' : '처리 완료',
          notify: '완료 시 앱에서 확인'
        }
      : null,
    analysis: enrollment
      ? {
          agentList: enrollment.agentList,
          riskLevel: enrollment.riskLevel,
          summary: enrollment.summary
        }
      : course.analysis
  }
})

async function handleCancel() {
  if (!record.value?.passportId) return
  cancelling.value = true
  try {
    await enrollmentApi.cancel(record.value.passportId)
    message.success('Passport 발급 요청을 취소했습니다.')
    router.push('/')
  } finally {
    cancelling.value = false
  }
}

async function handleAnalyze() {
  if (!record.value?.id) return
  analyzing.value = true
  try {
    await courseApi.analyze(record.value.id)
    message.success('권한 설계가 완료되었습니다. 권한 제안을 확인해 주세요.')
    router.push(`/work-requests/${record.value.id}/analysis`)
  } catch (error) {
    message.error(error.response?.data?.message || '권한 설계를 시작하지 못했습니다. 다시 시도해 주세요.')
  } finally {
    analyzing.value = false
  }
}
</script>

<style scoped>
.content-page { width: min(100%, 960px); margin: 0 auto; }
.back-button { margin-bottom: 28px; color: var(--forest-text); background: var(--forest-subtle); border-color: var(--forest-border-strong); }
.eyebrow { margin: 0 0 8px; color: var(--forest-primary); font-size: 12px; font-weight: 600; letter-spacing: .09em; }
h1 { margin: 0; color: var(--forest-text); font-size: 34px; }
.description { margin: 12px 0 28px; color: var(--forest-text-secondary); }

.steps-card,
.detail-card { border: 1px solid var(--forest-border) !important; background: var(--forest-surface); }
.steps-card :deep(.ant-card-body),
.detail-card :deep(.ant-card-body) { padding: 26px; }
.steps-card { margin-bottom: 20px; }
.steps-head { display: flex; align-items: center; justify-content: space-between; color: var(--forest-text); font-size: 15px; margin-bottom: 20px; }

.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 24px; }
.detail-card h3 { margin: 0 0 16px; color: var(--forest-text); font-size: 16px; }
.request-detail-card { margin-bottom: 24px; }
.request-status-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.request-status-head h3 { margin: 0; }
.passport-subtitle { margin: -9px 0 16px; color: var(--forest-text-secondary); font-size: 11px; }
.detail-card dl { margin: 0; display: flex; flex-direction: column; gap: 14px; }
.detail-card dl > div { display: flex; flex-direction: column; gap: 4px; }
.detail-card dt { color: var(--forest-text-secondary); font-size: 12px; }
.detail-card dd { margin: 0; color: var(--forest-text); font-size: 14px; }
.permission-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.analysis-preview { margin-top: 22px; padding: 18px; border: 1px solid var(--forest-border); border-radius: 10px; background: var(--forest-subtle); }
.analysis-preview span { color: var(--forest-primary); font-size: 12px; font-weight: 600; }
.analysis-preview p { margin: 8px 0 0; color: var(--forest-text); line-height: 1.65; }

.actions { display: flex; justify-content: flex-end; gap: 10px; }
.next-actions { align-items: center; }
@media (max-width: 760px) {
  .detail-grid { grid-template-columns: 1fr; }
}
</style>
