<template>
  <MainLayout>
    <div class="passport-detail-page">
      <div v-if="loading" class="loading-state"><a-spin size="large" tip="Passport 상세 정보를 불러오는 중..." /></div>
      <a-alert v-else-if="loadError" class="load-alert" type="error" show-icon :message="loadError">
        <template #action><a-button type="link" size="small" @click="loadPassport">다시 시도</a-button></template>
      </a-alert>

      <template v-else-if="passport">
        <div class="breadcrumb-row">
          <button class="back-link" type="button" @click="router.push('/passports')"><LeftOutlined /> Passport</button>
          <span>/</span>
          <span>{{ passport.name }}</span>
        </div>

        <section class="detail-heading">
          <div>
            <p class="eyebrow">MY PASSPORT</p>
            <h1>{{ passport.name }}</h1>
            <p class="description">이 미션에 허용된 권한과 발급 검토 내용을 확인합니다.</p>
          </div>
          <div class="heading-actions">
            <span :class="['status-pill', `status-${passport.statusKey}`]"><i class="status-dot" />{{ passport.status }}</span>
            <a-button class="secondary-action" size="large" @click="exportPassport">
              <template #icon><DownloadOutlined /></template>
              내보내기
            </a-button>
          </div>
        </section>

        <div class="detail-grid">
          <main class="detail-main">
            <a-card class="passport-overview" :bordered="false">
              <div class="overview-topline">
                <span class="overview-label">Passport</span>
                <span class="risk-label">{{ detail.riskLabel }}</span>
              </div>
              <h2>{{ passport.name }}</h2>
              <p class="passport-id">Passport #{{ passport.id }}</p>
              <dl class="overview-meta">
                <div><dt>요청자</dt><dd>{{ owner.name }} <span>· {{ owner.team }}</span></dd></div>
                <div><dt>실행 Agent</dt><dd>{{ passport.agentSummary }}</dd></div>
                <div><dt>사용 기간</dt><dd>{{ detail.validPeriod }}</dd></div>
              </dl>
            </a-card>

            <AgentPermissionSlider
              :agent-list="passport.agentList"
              title="발급된 Agent별 권한"
              description="좌우로 넘겨 각 Agent에 허용된 권한과 제외된 권한을 확인합니다."
              excluded-label="차단"
            />
          </main>

          <aside class="detail-side">
            <a-card class="side-card info-card" :bordered="false">
              <h2>Passport 정보</h2>
              <div class="info-list">
                <div class="info-line"><span>상태</span><strong :class="`text-${passport.statusKey}`">{{ passport.status }}</strong></div>
                <div class="info-line"><span>남은 시간</span><strong>{{ detail.statusInfo.remainingTime }}</strong></div>
                <div class="progress-line"><div class="info-line"><span>사용 기간 진행률</span><strong>{{ detail.statusInfo.progress }}%</strong></div><a-progress :percent="detail.statusInfo.progress" :show-info="false" size="small" /></div>
                <div class="info-line"><span>검토 담당자</span><strong>{{ detail.statusInfo.issuer }}</strong></div>
                <div class="info-line"><span>발급 일시</span><strong>{{ detail.statusInfo.approvedAt }}</strong></div>
              </div>
            </a-card>

            <a-card class="side-card linked-card" :bordered="false">
              <div class="side-card-title">연결 미션</div>
              <h3>{{ passport.name }}</h3>
              <p>{{ detail.linkedRequest.title }}</p>
              <button class="text-link" type="button" @click="router.push(`/work-requests/${passport.courseId}`)">미션 보기 <RightOutlined /></button>
            </a-card>

            <a-card class="side-card history-card" :bordered="false">
              <div class="side-card-title">발급 이력</div>
              <div class="history-list">
                <div v-for="(event, index) in history" :key="`${event.label}-${index}`" class="history-item">
                  <span class="history-marker" />
                  <div class="history-copy"><strong>{{ event.label }}</strong><p>{{ event.description }}</p></div>
                  <time>{{ event.time }}</time>
                </div>
              </div>
            </a-card>
          </aside>
        </div>
      </template>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import MainLayout from '@/layouts/MainLayout.vue'
import AgentPermissionSlider from '@/components/AgentPermissionSlider.vue'
import { enrollmentApi } from '@/api/enrollment.js'
import { countAgentPermissions, getAgentSummary, getPassportDisplayName, normalizeAgentList, PASSPORT_STATUS_META } from '@/constants/workRequest.js'
import { DownloadOutlined, LeftOutlined, RightOutlined } from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const loadError = ref('')
const passport = ref(null)

function toStatusKey(status) {
  const type = PASSPORT_STATUS_META[status]?.type
  if (type === 'active') return 'active'
  if (type === 'rejected') return 'rejected'
  if (status === 'EXPIRED') return 'expired'
  return 'pending'
}

function normalizePassport(item) {
  const statusKey = toStatusKey(item.status)
  const agentList = normalizeAgentList(item)
  const approvedBy = item.approvedBy ? `관리자 #${item.approvedBy}` : '-'
  const statusInfo = {
    remainingTime: item.status === 'READY_FOR_APPROVAL'
      ? '검토 대기'
      : item.status === 'ACTIVE'
        ? (item.expiredAt ? `${item.expiredAt}까지` : '사용 가능')
        : '-',
    progress: 0,
    issuer: approvedBy,
    approvedAt: item.approvedAt || '-'
  }
  const history = [
    { label: '미션 등록', description: '미션이 등록되었습니다.', time: item.createdAt || '-', tone: 'done' },
    { label: '권한 설계 완료', description: `${agentList.length}개 Agent · ${countAgentPermissions(item)}개 권한`, time: item.createdAt || '-', tone: 'done' }
  ]
  if (item.status === 'ACTIVE') {
    history.push({ label: 'Passport 발급', description: approvedBy, time: item.approvedAt || '-', tone: 'done' })
  } else if (item.status === 'REJECTED') {
    history.push({ label: '발급 요청 반려', description: item.rejectedReason || '-', time: item.approvedAt || '-', tone: 'current' })
  } else {
    history.push({ label: '발급 검토 중', description: '담당자가 발급 요청을 검토하고 있습니다.', time: '-', tone: 'current' })
  }

  return {
    id: item.id,
    courseId: item.courseId,
    name: getPassportDisplayName(item.courseTitle),
    agentList,
    agentSummary: getAgentSummary(item),
    status: PASSPORT_STATUS_META[item.status]?.label || (statusKey === 'expired' ? '만료' : item.status),
    statusKey,
    rejectedReason: item.rejectedReason,
    reviewReason: item.reviewReason,
    detail: {
      owner: item.owner,
      validPeriod: item.validPeriod || (item.expiredAt ? `${item.expiredAt}까지` : '24시간'),
      riskLabel: item.riskLabel || `위험도 ${item.riskLevel === 'HIGH' ? '높음' : item.riskLevel === 'MEDIUM' ? '보통' : '낮음'}`,
      statusInfo: item.statusInfo || statusInfo,
      linkedRequest: item.linkedRequest || {
        title: item.courseTitle,
        description: item.courseDescription
      },
      history: item.history || history
    }
  }
}

const defaultStatusInfo = { remainingTime: '-', progress: 0, issuer: '-', approvedAt: '-' }
const detail = computed(() => ({
  riskLabel: passport.value?.detail?.riskLabel ?? '검토 필요',
  validPeriod: passport.value?.detail?.validPeriod ?? '-',
  statusInfo: { ...defaultStatusInfo, ...(passport.value?.detail?.statusInfo ?? {}) },
  linkedRequest: {
    title: passport.value?.detail?.linkedRequest?.title ?? '-',
    description: passport.value?.detail?.linkedRequest?.description ?? '연결된 미션 정보가 없습니다.'
  }
}))
const owner = computed(() => passport.value?.detail?.owner ?? { name: '김민지', team: 'Sales Ops' })
const history = computed(() => passport.value?.detail?.history ?? [])

async function loadPassport() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await enrollmentApi.getPassport(route.params.id)
    if (!res.data) throw new Error('not found')
    passport.value = normalizePassport(res.data)
  } catch (error) {
    console.error('[PassportDetailView] Passport 상세 조회 실패:', error)
    passport.value = null
    loadError.value = 'Passport 상세 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function exportPassport() {
  if (!passport.value) return
  try {
    const res = await enrollmentApi.exportPassportYaml(passport.value.id)
    downloadYaml(res.data, `passport-${passport.value.id}.yml`)
    message.success('Passport를 YAML로 내보냈습니다.')
  } catch (error) {
    console.error('[PassportDetailView] Passport 내보내기 실패:', error)
    message.error('Passport 내보내기에 실패했습니다.')
  }
}

function downloadYaml(text, filename) {
  const blob = new Blob([text], { type: 'text/yaml;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.click()
  URL.revokeObjectURL(url)
}

onMounted(loadPassport)
</script>

<style scoped>
.passport-detail-page { width: min(100%, 1120px); margin: 0 auto; }
.loading-state { min-height: 420px; display: grid; place-items: center; color: var(--forest-text-secondary); }
.load-alert { margin: 28px 0; color: #b33a3a; background: #fceded; border-color: #edc8c8; }
.load-alert :deep(.ant-alert-message), .load-alert :deep(.ant-alert-icon) { color: #b33a3a; }
.load-alert :deep(.ant-btn-link) { color: #b33a3a; }
.breadcrumb-row { display: flex; align-items: center; gap: 9px; margin-bottom: 17px; color: var(--forest-text-muted); font-size: 12px; }
.back-link { display: inline-flex; align-items: center; gap: 6px; padding: 0; color: var(--forest-text-secondary); background: transparent; border: 0; font: inherit; }
.back-link:hover { color: var(--forest-primary); }
.detail-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 24px; }
.eyebrow { margin: 0 0 8px; color: var(--forest-primary); font-size: 12px; font-weight: 600; letter-spacing: .09em; }
.detail-heading h1 { margin: 0; color: var(--forest-text); font-size: clamp(28px, 3vw, 34px); line-height: 1.25; letter-spacing: -.04em; }
.description { margin: 11px 0 0; color: var(--forest-text-secondary); font-size: 14px; }
.heading-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
.secondary-action { height: 42px; color: var(--forest-text); border-color: var(--forest-border); border-radius: 9px; background: var(--forest-subtle); }
.status-pill { min-height: 27px; padding: 5px 11px; display: inline-flex; align-items: center; gap: 6px; border-radius: 999px; border: 1px solid transparent; font-size: 11px; line-height: 1; white-space: nowrap; }
.status-dot { width: 6px; height: 6px; display: inline-block; border-radius: 50%; background: currentColor; }
.status-active { color: #126b48; background: #e4f2ea; border-color: #b9d9c8; }
.status-pending { color: #80510c; background: #fff5dc; border-color: #efdba9; }
.status-expired { color: var(--forest-text-secondary); background: #eef1ef; border-color: #d8deda; }
.status-rejected { color: #b33a3a; background: #fceded; border-color: #edc8c8; }
.detail-grid { display: grid; grid-template-columns: minmax(0, 1fr) 306px; align-items: start; gap: 17px; }
.detail-main, .detail-side { min-width: 0; display: flex; flex-direction: column; gap: 16px; }
.passport-overview, .permission-card, .side-card { border: 1px solid var(--forest-border) !important; border-radius: 11px; background: var(--forest-surface); box-shadow: none; }
.passport-overview :deep(.ant-card-body) { padding: 28px 30px; }
.overview-topline { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
.overview-label { color: var(--forest-primary); font-size: 11px; font-weight: 600; letter-spacing: .14em; }
.risk-label { padding: 5px 10px; border-radius: 999px; color: #80510c; background: #fff5dc; font-size: 11px; }
.passport-overview h2 { margin: 13px 0 3px; color: var(--forest-text); font-size: 26px; letter-spacing: -.02em; }
.passport-id { margin: 0 0 20px; color: var(--forest-text-secondary); font-size: 11px; }
.overview-meta { display: grid; gap: 9px; }
.overview-meta div { display: flex; align-items: baseline; gap: 18px; }
.overview-meta dt { width: 55px; flex: 0 0 55px; color: var(--forest-text-secondary); font-size: 11px; }
.overview-meta dd { margin: 0; color: var(--forest-text); font-size: 13px; }
.overview-meta dd span { color: var(--forest-text-secondary); }
.permission-card :deep(.ant-card-body) { padding: 21px 20px 12px; }
.card-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 11px; }
.card-heading h2, .side-card h2 { margin: 0; color: var(--forest-text); font-size: 15px; font-weight: 600; }
.card-heading p { margin: 5px 0 0; color: var(--forest-text-secondary); font-size: 11px; }
.permission-total { min-width: 35px; padding: 5px 9px; border-radius: 999px; color: var(--forest-primary); background: #e4f2ea; font-size: 11px; text-align: center; }
.blocked-total { color: #b33a3a; background: #fceded; }
.permission-list { display: flex; flex-direction: column; }
.permission-row { min-height: 61px; padding: 10px 0; display: flex; align-items: center; gap: 10px; border-top: 1px solid var(--forest-border); }
.permission-copy { min-width: 0; flex: 1; display: grid; grid-template-columns: auto auto; align-items: baseline; column-gap: 9px; }
.permission-copy strong { overflow: hidden; color: var(--forest-text); font-size: 12px; font-weight: 500; text-overflow: ellipsis; white-space: nowrap; }
.permission-copy code { color: var(--forest-text-secondary); font-size: 10px; white-space: nowrap; }
.permission-copy p { grid-column: 1 / -1; margin: 2px 0 0; color: var(--forest-text-secondary); font-size: 10px; white-space: nowrap; }
.permission-state { min-width: 35px; padding: 4px 7px; border-radius: 999px; font-size: 10px; text-align: center; }
.allowed-state { color: var(--forest-primary); background: #e4f2ea; }
.blocked-state { color: #b33a3a; background: #fceded; }
.permission-empty { padding: 16px 0; }
.review-reason { margin: 0 0 10px; padding: 11px 12px; border-left: 2px solid var(--forest-primary); border-radius: 0 7px 7px 0; background: rgba(72, 59, 255, .08); }
.review-reason span { color: var(--forest-primary); font-size: 11px; font-weight: 600; }
.review-reason p { margin: 4px 0 0; color: var(--forest-text-secondary); font-size: 11px; line-height: 1.55; }
.side-card :deep(.ant-card-body) { padding: 19px 18px; }
.info-card h2 { margin-bottom: 15px; }
.info-list { display: flex; flex-direction: column; }
.info-line { min-height: 31px; display: flex; align-items: center; justify-content: space-between; gap: 12px; border-bottom: 1px solid var(--forest-subtle); }
.info-line:last-child { border-bottom: 0; }
.info-line span { color: var(--forest-text-secondary); font-size: 11px; }
.info-line strong { color: var(--forest-text); font-size: 11px; font-weight: 500; text-align: right; }
.text-active { color: var(--forest-primary) !important; }
.text-pending { color: #80510c !important; }
.text-rejected { color: #b33a3a !important; }
.text-expired { color: var(--forest-text-secondary) !important; }
.progress-line { padding: 6px 0 4px; border-bottom: 1px solid var(--forest-subtle); }
.progress-line .info-line { border-bottom: 0; }
.progress-line :deep(.ant-progress) { margin-top: 4px; }
.progress-line :deep(.ant-progress-inner) { background: var(--forest-border); }
.progress-line :deep(.ant-progress-bg) { background: var(--forest-primary); }
.side-card-title { margin-bottom: 13px; color: var(--forest-primary); font-size: 11px; font-weight: 600; }
.linked-card h3 { margin: 0; color: var(--forest-text); font-size: 13px; font-weight: 600; }
.linked-card p { margin: 7px 0 13px; color: var(--forest-text-secondary); font-size: 11px; line-height: 1.6; }
.text-link { display: inline-flex; align-items: center; gap: 7px; padding: 0; color: var(--forest-primary); background: transparent; border: 0; font-size: 11px; }
.text-link:hover { color: var(--forest-primary); }
.history-list { display: flex; flex-direction: column; }
.history-item { position: relative; min-height: 47px; padding-left: 15px; display: grid; grid-template-columns: 1fr auto; gap: 4px 8px; border-left: 1px solid var(--forest-border); }
.history-marker { position: absolute; left: -4px; top: 3px; width: 7px; height: 7px; border-radius: 50%; background: var(--forest-primary); }
.history-copy strong { color: var(--forest-text); font-size: 11px; font-weight: 600; }
.history-copy p { margin: 3px 0 12px; color: var(--forest-text-secondary); font-size: 10px; line-height: 1.45; }
.history-item time { color: var(--forest-text-muted); font-size: 10px; }
@media (max-width: 860px) { .detail-grid { grid-template-columns: 1fr; } .detail-side { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); align-items: start; } .history-card { grid-column: 1 / -1; } }
@media (max-width: 620px) { .detail-heading { flex-direction: column; align-items: flex-start; } .heading-actions { justify-content: flex-start; } .detail-side { display: flex; } .overview-meta div { gap: 10px; } .permission-copy p { white-space: normal; } }
</style>
