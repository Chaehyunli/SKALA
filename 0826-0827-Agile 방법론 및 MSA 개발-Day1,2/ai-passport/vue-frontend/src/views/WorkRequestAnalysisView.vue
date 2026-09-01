<template>
  <MainLayout>
    <div class="content-page" v-if="record && analysis">
      <p class="eyebrow">PERMISSION REVIEW</p>
      <h1>권한 설계 결과</h1>
      <p class="description">권한 제안을 확인하고 Passport 발급을 요청합니다.</p>

      <div class="summary-row">
        <a-card class="summary-card" :bordered="false">
          <span class="card-label">미션</span>
          <h2>{{ record.title }}</h2>
          <p>{{ record.description }}</p>
        </a-card>

        <a-card class="summary-card agent-card" :bordered="false">
          <span class="card-label">실행 Agent · {{ analysisAgents.length }}개</span>
          <div class="agent-row">
            <div class="agent-info">
              <h2>{{ agentSummary }}</h2>
              <span class="agent-code">각 Agent별 최소 권한을 확인하세요.</span>
            </div>
          </div>
        </a-card>
      </div>

      <div class="risk-row"><a-tag :color="riskMeta.color">전체 위험도 {{ riskMeta.label }}</a-tag></div>
      <AgentPermissionSlider
        class="permission-slider"
        :agent-list="analysisAgents"
        title="Agent별 권한 제안"
        description="좌우로 넘겨 Agent마다 포함할 권한과 제외 사유를 확인합니다."
        allowed-label="포함"
      />

      <button
        v-if="reuseRecommendation"
        type="button"
        class="reuse-notice"
        @click="router.push(`/passports/${reuseRecommendation.passportId}`)"
      >
        <CheckCircleOutlined class="reuse-icon" />
        <span class="reuse-copy">
          <strong>요청한 권한을 포함한 Passport가 이미 있습니다.</strong>
          <span>기존 Passport로 이 미션을 수행할 수 있으며, 필요하면 새 Passport를 별도로 발급할 수 있습니다.</span>
        </span>
        <span class="reuse-link">Passport 상세 보기 <RightOutlined /></span>
      </button>

      <div class="actions">
        <a-button size="large" @click="router.push('/work-requests/new')">미션 수정</a-button>
        <a-button
          type="primary"
          size="large"
          :loading="requesting"
          :disabled="!analysisAgents.length"
          @click="handleRequestPassport"
        >
          Passport 발급 요청
        </a-button>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CheckCircleOutlined, RightOutlined } from '@ant-design/icons-vue'
import MainLayout from '@/layouts/MainLayout.vue'
import AgentPermissionSlider from '@/components/AgentPermissionSlider.vue'
import { courseApi } from '@/api/course.js'
import { enrollmentApi } from '@/api/enrollment.js'
import { getAgentSummary, normalizeAgentList, RISK_LEVEL_META } from '@/constants/workRequest.js'

const route = useRoute()
const router = useRouter()
const record = ref(null)
const requesting = ref(false)
const reuseRecommendation = ref(null)

const analysis = computed(() => record.value?.analysis || null)
const analysisAgents = computed(() => normalizeAgentList(analysis.value))
const agentSummary = computed(() => getAgentSummary(analysis.value))
const riskMeta = computed(
  () => RISK_LEVEL_META[analysis.value?.riskLevel] || { label: analysis.value?.riskLevel, color: 'default' }
)

onMounted(async () => {
  const res = await courseApi.getById(route.params.id)
  record.value = res.data

  try {
    const reuseRes = await enrollmentApi.checkPassportReuse(route.params.id)
    if (reuseRes.data?.reusable && reuseRes.data?.passportId) {
      reuseRecommendation.value = reuseRes.data
    }
  } catch {
    reuseRecommendation.value = null
  }
})

async function handleRequestPassport() {
  requesting.value = true
  try {
    // POST /api/enrollments 요청 형식: {courseId, agentList, riskLevel, summary}
    await enrollmentApi.enroll({
      courseId: route.params.id,
      agentList: analysisAgents.value.map((agent) => ({
        agentCode: agent.agentCode,
        permissions: (agent.permissions || []).map((permission) => ({
          code: permission.code,
          label: permission.label || permission.name || permission.code,
          description: permission.description,
          reason: permission.reason
        })),
        excludedPermissions: (agent.excludedPermissions || []).map((permission) => ({
          code: permission.code,
          label: permission.label || permission.name || permission.code,
          description: permission.description,
          reason: permission.reason
        }))
      })),
      riskLevel: analysis.value.riskLevel,
      summary: analysis.value.summary
    })
    router.push(`/work-requests/${route.params.id}`)
  } finally {
    requesting.value = false
  }
}
</script>

<style scoped>
.content-page { width: min(100%, 960px); margin: 0 auto; }
.eyebrow { margin: 0 0 8px; color: var(--forest-primary); font-size: 12px; font-weight: 600; letter-spacing: .09em; }
h1 { margin: 0; color: var(--forest-text); font-size: 34px; letter-spacing: -.04em; }
.description { margin: 12px 0 28px; color: var(--forest-text-secondary); }

.summary-row { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 20px; }
.summary-card { border: 1px solid var(--forest-border) !important; background: var(--forest-surface); }
.summary-card :deep(.ant-card-body) { padding: 24px; }
.card-label { color: var(--forest-text-secondary); font-size: 12px; letter-spacing: .04em; }
.summary-card h2 { margin: 10px 0 8px; color: var(--forest-text); font-size: 19px; }
.summary-card p { margin: 0; color: var(--forest-text-secondary); font-size: 14px; line-height: 1.5; }

.agent-row { display: flex; align-items: center; gap: 14px; margin-top: 10px; }
.agent-icon { font-size: 30px; color: var(--forest-primary); }
.agent-info { flex: 1; }
.agent-info h2 { margin: 0; }
.agent-code { color: var(--forest-text-secondary); font-size: 12px; }

.permission-slider { margin-bottom: 24px; }
.risk-row { margin: -5px 0 12px; display: flex; justify-content: flex-end; }
.permission-card { border: 1px solid var(--forest-border) !important; background: var(--forest-surface); margin-bottom: 24px; }
.permission-card :deep(.ant-card-body) { padding: 24px; }
.permission-header { display: flex; align-items: center; justify-content: space-between; color: var(--forest-text); font-size: 16px; font-weight: 600; margin-bottom: 16px; }

.permission-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 12px; }
.permission-list li {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
  border: 1px solid var(--forest-border);
  border-radius: 10px;
  background: var(--forest-subtle);
}
.permission-list li.excluded { opacity: .72; }

.permission-index {
  flex: 0 0 26px;
  height: 26px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #edf1ee;
  color: var(--forest-text-secondary);
  font-size: 12px;
}

.permission-body { flex: 1; }
.permission-title { display: flex; align-items: baseline; gap: 8px; }
.permission-title strong { color: var(--forest-text); font-size: 14px; }
.permission-code { color: var(--forest-text-secondary); font-size: 12px; }
.permission-body p { margin: 4px 0 0; color: var(--forest-text-secondary); font-size: 13px; }

.reuse-notice {
  width: 100%;
  margin: 0 0 16px;
  padding: 16px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  border: 1px solid #b9dcc7;
  border-radius: 12px;
  background: #f1f8f4;
  color: var(--forest-text);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color .2s ease, background .2s ease;
}
.reuse-notice:hover { border-color: var(--forest-primary); background: #e9f4ed; }
.reuse-notice:focus-visible { outline: 2px solid var(--forest-primary); outline-offset: 2px; }
.reuse-icon { flex: 0 0 auto; color: var(--forest-primary); font-size: 22px; }
.reuse-copy { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.reuse-copy strong { font-size: 15px; }
.reuse-copy span { color: var(--forest-text-secondary); font-size: 13px; line-height: 1.5; }
.reuse-link { display: inline-flex; align-items: center; gap: 5px; color: var(--forest-primary); font-size: 13px; font-weight: 600; white-space: nowrap; }

.actions { display: flex; justify-content: flex-end; gap: 10px; }
@media (max-width: 760px) {
  .summary-row { grid-template-columns: 1fr; }
  .reuse-notice { align-items: flex-start; flex-wrap: wrap; }
  .reuse-copy { flex-basis: calc(100% - 40px); }
  .reuse-link { margin-left: 36px; }
  .actions > * { flex: 1; }
}
</style>
