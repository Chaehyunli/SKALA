<template>
  <AdminLayout>
    <div class="detail-page">
      <button class="back-link" type="button" @click="router.push('/admin')">
        <LeftOutlined /> 발급 검토
      </button>

      <div v-if="loading" class="loading-state">
        <a-spin size="large" tip="Passport 상세 정보를 불러오는 중..." />
      </div>

      <a-alert v-else-if="loadError" class="load-alert" type="error" show-icon :message="loadError">
        <template #action><a-button type="link" size="small" @click="loadPassport">다시 시도</a-button></template>
      </a-alert>

      <template v-else-if="passport">
        <section class="page-heading">
          <div>
            <p class="eyebrow">PASSPORT REVIEW</p>
            <h1>{{ passportName }}</h1>
            <p class="description">미션과 권한 제안을 검토해 Passport 발급 여부를 결정합니다.</p>
          </div>
          <div class="heading-actions">
            <span class="status-pill">{{ statusLabel }}</span>
            <a-button class="secondary-action" size="large" @click="exportPassport">
              <template #icon><DownloadOutlined /></template>
              내보내기
            </a-button>
          </div>
        </section>

        <div class="detail-grid">
          <main class="detail-main">
            <a-card class="overview-card" :bordered="false">
              <div class="card-topline">
                <span>Passport #{{ passport.id }}</span>
                <a-tag :color="riskMeta.color">위험도 {{ riskMeta.label }}</a-tag>
              </div>
              <h2>{{ passportName }}</h2>
              <p class="passport-id">Passport #{{ passport.id }}</p>
              <dl class="meta-list">
                <div><dt>요청자</dt><dd>사용자 #{{ passport.userId }}</dd></div>
                <div><dt>실행 Agent</dt><dd>{{ agentSummary }}</dd></div>
                <div><dt>미션</dt><dd>#{{ passport.courseId }} · {{ passport.courseTitle }}</dd></div>
                <div><dt>요청 일시</dt><dd>{{ formatDate(passport.createdAt) }}</dd></div>
              </dl>
            </a-card>

            <a-card class="summary-card" :bordered="false">
              <h2>권한 설계 요약</h2>
              <p>{{ passport.summary || '권한 설계 요약이 없습니다.' }}</p>
              <div v-if="passport.courseDescription" class="work-description">
                <span>미션 브리프</span>
                <p>{{ passport.courseDescription }}</p>
              </div>
            </a-card>

            <AgentPermissionSlider
              :agent-list="agentList"
              title="Agent별 권한 검토"
              description="좌우로 넘겨 모든 Agent의 포함 권한과 제외 사유를 검토합니다."
              allowed-label="포함"
            />
          </main>

          <aside class="action-side">
            <a-card class="side-card info-card" :bordered="false">
              <h2>Passport 정보</h2>
              <div class="info-list">
                <div class="info-line"><span>상태</span><strong class="status-text">{{ statusLabel }}</strong></div>
                <div class="info-line"><span>요청자</span><strong>사용자 #{{ passport.userId }}</strong></div>
                <div class="info-line"><span>미션</span><strong>#{{ passport.courseId }}</strong></div>
                <div class="info-line"><span>요청 일시</span><strong>{{ formatDate(passport.createdAt) }}</strong></div>
              </div>
            </a-card>

            <a-card v-if="passport.status === 'READY_FOR_APPROVAL'" class="side-card action-card" :bordered="false">
              <h2>검토 결정</h2>
              <p>발급하면 제안된 권한으로 Passport가 즉시 활성화됩니다.</p>
              <a-button type="primary" size="large" block :loading="approving" @click="approvePassport">Passport 발급</a-button>
              <a-button danger size="large" block :disabled="approving" @click="openReject">발급 요청 반려</a-button>
            </a-card>

            <a-card class="side-card linked-card" :bordered="false">
              <div class="side-card-title">연결 미션</div>
              <h3>{{ passport.courseTitle }}</h3>
              <p>{{ passport.courseDescription || '미션 브리프가 없습니다.' }}</p>
            </a-card>

            <a-card class="side-card history-card" :bordered="false">
              <div class="side-card-title">발급 이력</div>
              <div class="history-list">
                <div v-for="(event, index) in reviewHistory" :key="`${event.label}-${index}`" class="history-item">
                  <span class="history-marker" :class="{ current: event.current }" />
                  <div><strong>{{ event.label }}</strong><p>{{ event.description }}</p></div>
                  <time>{{ event.time }}</time>
                </div>
              </div>
            </a-card>
          </aside>
        </div>
      </template>
    </div>

    <a-modal
      v-model:open="rejectModalOpen"
      title="발급 요청 반려"
      ok-text="반려"
      cancel-text="취소"
      :ok-button-props="{ danger: true, disabled: !rejectReason.trim() }"
      :confirm-loading="rejecting"
      @ok="rejectPassport"
    >
      <p class="reject-target">Passport #{{ passport?.id }} · {{ passport?.courseTitle }}</p>
      <a-textarea v-model:value="rejectReason" :rows="4" placeholder="반려 사유 입력 (필수)" />
    </a-modal>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { DownloadOutlined, LeftOutlined } from '@ant-design/icons-vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import AgentPermissionSlider from '@/components/AgentPermissionSlider.vue'
import { enrollmentApi } from '@/api/enrollment.js'
import { countAgentPermissions, getAgentSummary, getPassportDisplayName, normalizeAgentList, PASSPORT_STATUS_META, RISK_LEVEL_META } from '@/constants/workRequest.js'

const route = useRoute()
const router = useRouter()
const passport = ref(null)
const loading = ref(true)
const loadError = ref('')
const approving = ref(false)
const rejecting = ref(false)
const rejectModalOpen = ref(false)
const rejectReason = ref('')

const passportName = computed(() => getPassportDisplayName(passport.value?.courseTitle))
const agentList = computed(() => normalizeAgentList(passport.value))
const agentSummary = computed(() => getAgentSummary(passport.value))
const riskMeta = computed(() => RISK_LEVEL_META[passport.value?.riskLevel] || { label: passport.value?.riskLevel || '-', color: 'default' })
const statusLabel = computed(() => PASSPORT_STATUS_META[passport.value?.status]?.label || passport.value?.status || '-')
const reviewHistory = computed(() => {
  if (!passport.value) return []
  const events = [
    { label: '미션 등록', description: passport.value.courseTitle, time: formatDate(passport.value.createdAt) },
    { label: '권한 설계 완료', description: `${agentList.value.length}개 Agent · ${countAgentPermissions(passport.value)}개 권한`, time: formatDate(passport.value.createdAt) }
  ]

  if (passport.value.status === 'ACTIVE') {
    events.push({ label: 'Passport 발급', description: `관리자 #${passport.value.approvedBy}`, time: formatDate(passport.value.approvedAt) })
  } else if (passport.value.status === 'REJECTED') {
    events.push({ label: '발급 요청 반려', description: passport.value.rejectedReason || '-', time: formatDate(passport.value.approvedAt) })
  } else {
    events.push({ label: '발급 검토 중', description: '검토 결과를 기다리고 있습니다.', time: '-', current: true })
  }
  return events
})

function formatDate(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

async function exportPassport() {
  if (!passport.value) return
  try {
    const res = await enrollmentApi.exportAdminPassportYaml(passport.value.id)
    const blob = new Blob([res.data], { type: 'text/yaml;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = `passport-${passport.value.id}.yml`
    anchor.click()
    URL.revokeObjectURL(url)
    message.success('Passport를 YAML로 내보냈습니다.')
  } catch (error) {
    console.error('[AdminPassportDetailView] Passport 내보내기 실패:', error)
    message.error('Passport 내보내기에 실패했습니다.')
  }
}

async function loadPassport() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await enrollmentApi.getAdminPassport(route.params.id)
    passport.value = res.data
  } catch (error) {
    console.error('[AdminPassportDetailView] Passport 상세 조회 실패:', error)
    passport.value = null
    loadError.value = 'Passport 상세 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function approvePassport() {
  approving.value = true
  try {
    await enrollmentApi.approve(passport.value.id)
    message.success(`Passport #${passport.value.id}을 발급했습니다.`)
    router.replace('/admin')
  } catch (error) {
    console.error('[AdminPassportDetailView] Passport 승인 실패:', error)
    message.error('Passport를 발급하지 못했습니다.')
  } finally {
    approving.value = false
  }
}

function openReject() {
  rejectReason.value = ''
  rejectModalOpen.value = true
}

async function rejectPassport() {
  if (!rejectReason.value.trim()) return
  rejecting.value = true
  try {
    await enrollmentApi.reject(passport.value.id, rejectReason.value.trim())
    message.success(`Passport #${passport.value.id}을 반려했습니다.`)
    rejectModalOpen.value = false
    router.replace('/admin')
  } catch (error) {
    console.error('[AdminPassportDetailView] Passport 반려 실패:', error)
    message.error('Passport 반려에 실패했습니다.')
  } finally {
    rejecting.value = false
  }
}

onMounted(loadPassport)
</script>

<style scoped>
.detail-page { width: min(100%, 1120px); margin: 0 auto; }
.back-link { margin-bottom: 22px; padding: 0; display: inline-flex; align-items: center; gap: 7px; color: var(--forest-text-secondary); border: 0; background: transparent; font-size: 13px; }
.back-link:hover { color: var(--forest-primary); }
.loading-state { min-height: 420px; display: grid; place-items: center; }
.load-alert { margin-top: 20px; background: #fceded; border-color: #edc8c8; }
.page-heading { margin-bottom: 24px; display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; }
.heading-actions { display: flex; align-items: center; justify-content: flex-end; gap: 10px; }
.eyebrow { margin: 0 0 8px; color: var(--forest-primary); font-size: 12px; font-weight: 600; letter-spacing: .09em; }
.page-heading h1 { margin: 0; color: var(--forest-text); font-size: clamp(28px, 3vw, 34px); letter-spacing: -.04em; }
.description { margin: 10px 0 0; color: var(--forest-text-secondary); font-size: 14px; }
.status-pill { padding: 7px 12px; color: #80510c; border: 1px solid #efdba9; border-radius: 999px; background: #fff5dc; font-size: 12px; white-space: nowrap; }
.secondary-action { color: var(--forest-text); border-color: var(--forest-border); border-radius: 9px; background: var(--forest-subtle); }
.detail-grid { display: grid; grid-template-columns: minmax(0, 1fr) 280px; align-items: start; gap: 18px; }
.detail-main { min-width: 0; display: flex; flex-direction: column; gap: 16px; }
.overview-card, .summary-card, .permission-card, .side-card { border: 1px solid var(--forest-border) !important; border-radius: 11px; background: var(--forest-surface); }
.overview-card :deep(.ant-card-body), .summary-card :deep(.ant-card-body), .action-card :deep(.ant-card-body) { padding: 25px; }
.card-topline, .card-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 15px; }
.card-topline { color: var(--forest-text-secondary); font-size: 12px; }
.overview-card h2 { margin: 15px 0 3px; color: var(--forest-text); font-size: 23px; }
.passport-id { margin: 0 0 20px; color: var(--forest-text-secondary); font-size: 11px; }
.meta-list { margin: 0; display: grid; gap: 11px; }
.meta-list div { display: grid; grid-template-columns: 90px 1fr; gap: 12px; }
.meta-list dt { color: var(--forest-text-secondary); font-size: 12px; }
.meta-list dd { margin: 0; color: var(--forest-text); font-size: 13px; }
.summary-card h2, .card-heading h2, .action-card h2 { margin: 0; color: var(--forest-text); font-size: 16px; }
.summary-card > :deep(.ant-card-body) > p { margin: 13px 0 0; color: var(--forest-text-secondary); line-height: 1.7; }
.work-description { margin-top: 20px; padding-top: 17px; border-top: 1px solid var(--forest-border); }
.work-description span { color: var(--forest-text-secondary); font-size: 11px; }
.work-description p { margin: 7px 0 0; color: var(--forest-text-secondary); line-height: 1.65; }
.permission-card :deep(.ant-card-body) { padding: 22px 20px 12px; }
.card-heading { margin-bottom: 13px; }
.card-heading p { margin: 5px 0 0; color: var(--forest-text-secondary); font-size: 11px; }
.count-badge { padding: 5px 9px; border-radius: 999px; font-size: 11px; }
.allowed { color: var(--forest-primary); background: #e4f2ea; }
.blocked { color: #b33a3a; background: #fceded; }
.permission-list { border-top: 1px solid var(--forest-border); }
.permission-row { padding: 15px 3px; display: flex; align-items: center; justify-content: space-between; gap: 20px; border-bottom: 1px solid var(--forest-border); }
.permission-row strong { margin-right: 9px; color: var(--forest-text); font-size: 13px; }
.permission-row code { color: var(--forest-text-secondary); font-size: 10px; }
.permission-row p { margin: 5px 0 0; color: var(--forest-text-secondary); font-size: 11px; }
.permission-state { padding: 5px 8px; border-radius: 7px; font-size: 10px; white-space: nowrap; }
.action-side { position: sticky; top: 92px; display: flex; flex-direction: column; gap: 16px; }
.side-card :deep(.ant-card-body) { padding: 21px 20px; }
.info-card h2 { margin-bottom: 16px; }
.info-list { display: flex; flex-direction: column; gap: 12px; }
.info-line { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; font-size: 11px; }
.info-line span { color: var(--forest-text-secondary); }
.info-line strong { color: var(--forest-text); font-weight: 500; text-align: right; }
.info-line .status-text { color: #80510c; }
.action-card h2 { margin-bottom: 9px; }
.action-card p { margin: 0 0 20px; color: var(--forest-text-secondary); font-size: 12px; line-height: 1.6; }
.action-card :deep(.ant-btn + .ant-btn) { margin-top: 10px; }
.side-card-title { margin-bottom: 12px; color: var(--forest-text-secondary); font-size: 11px; font-weight: 600; letter-spacing: .05em; }
.linked-card h3 { margin: 0 0 7px; color: var(--forest-text); font-size: 14px; }
.linked-card p { margin: 0; color: var(--forest-text-secondary); font-size: 11px; line-height: 1.6; }
.history-list { display: flex; flex-direction: column; gap: 15px; }
.history-item { position: relative; padding-left: 17px; display: grid; grid-template-columns: 1fr auto; gap: 2px 8px; }
.history-marker { position: absolute; top: 5px; left: 0; width: 7px; height: 7px; border-radius: 50%; background: var(--forest-primary); box-shadow: 0 0 0 3px rgba(72, 59, 255, .14); }
.history-marker.current { background: #80510c; box-shadow: 0 0 0 3px rgba(172, 111, 13, .16); }
.history-item strong { color: var(--forest-text); font-size: 11px; font-weight: 500; }
.history-item p { margin: 3px 0 0; color: var(--forest-text-secondary); font-size: 10px; line-height: 1.4; }
.history-item time { color: var(--forest-text-muted); font-size: 9px; white-space: nowrap; }
.reject-target { color: var(--forest-text); }
@media (max-width: 820px) { .detail-grid { grid-template-columns: 1fr; } .action-side { position: static; } }
@media (max-width: 560px) { .page-heading { align-items: flex-start; flex-direction: column; } .meta-list div { grid-template-columns: 1fr; gap: 3px; } }
</style>
