<template>
  <MainLayout>
    <div class="work-page">
      <section class="page-heading">
        <div>
          <p class="eyebrow">MY WORK REQUESTS</p>
          <h1>미션 보드</h1>
          <p class="description">등록한 미션과 권한 설계 상태를 확인합니다.</p>
        </div>
        <a-button type="primary" size="large" class="new-request-button" @click="router.push('/work-requests/new')">
          <template #icon><FormOutlined /></template>
          새 미션
        </a-button>
      </section>

      <section class="summary-grid" aria-label="미션 요약">
        <a-card v-for="stat in summaryStats" :key="stat.label" class="summary-card" :bordered="false">
          <span>{{ stat.label }}</span>
          <strong>{{ stat.value }}</strong>
          <small>{{ stat.description }}</small>
        </a-card>
      </section>

      <section class="request-panel">
        <a-alert
          v-if="loadError"
          class="load-alert"
          type="warning"
          show-icon
          :message="loadError"
        />
        <div class="panel-toolbar">
          <div class="status-tabs" role="tablist" aria-label="미션 상태">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              type="button"
              :class="['status-tab', { active: activeTab === tab.key }]"
              @click="activeTab = tab.key"
            >
              {{ tab.label }} <span class="tab-count">{{ tab.count }}</span>
            </button>
          </div>
          <a-input v-model:value="searchText" class="search-input" size="large" placeholder="미션 검색" allow-clear>
            <template #prefix><SearchOutlined /></template>
          </a-input>
        </div>

        <a-table
          class="request-table"
          :columns="columns"
          :data-source="filteredRequests"
          :loading="loading"
          :pagination="false"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'request'">
              <button class="request-link" type="button" @click="router.push(`/work-requests/${record.id}`)">
                <strong>{{ record.title }}</strong>
                <small>{{ record.id }} · {{ record.category }}</small>
              </button>
            </template>
            <template v-else-if="column.key === 'analysis'">
              <span :class="['status-pill', `analysis-${record.analysis.type}`]">
                <LoadingOutlined v-if="record.analysis.type === 'progress'" spin />
                {{ record.analysis.label }}
              </span>
            </template>
            <template v-else-if="column.key === 'agent'"><span class="agent-name">{{ record.agent }}</span></template>
            <template v-else-if="column.key === 'passport'">
              <button
                v-if="record.passport.type !== 'none'"
                class="passport-link"
                type="button"
                @click="openPassport(record)"
              >
                <strong>{{ record.passport.name }}</strong>
                <span :class="['status-pill', `passport-${record.passport.type}`]">{{ record.passport.label }}</span>
              </button>
              <button v-else class="next-step-link" type="button" @click="continueRequest(record)">
                <strong>{{ record.analysis.type === 'done' ? 'Passport 발급 요청' : '다음 단계 확인' }}</strong>
                <span class="passport-none">Passport 미발급</span>
              </button>
            </template>
            <template v-else-if="column.key === 'createdAt'"><span class="created-at">{{ record.createdAt }}</span></template>
          </template>
        </a-table>

        <div class="panel-footer">미션 {{ filteredRequests.length }}개</div>
      </section>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import { courseApi } from '@/api/course.js'
import { enrollmentApi } from '@/api/enrollment.js'
import { CATEGORY_LABEL, getAgentSummary, getPassportDisplayName, PASSPORT_STATUS_META, WORK_STATUS_META } from '@/constants/workRequest.js'
import { FormOutlined, LoadingOutlined, SearchOutlined } from '@ant-design/icons-vue'

const activeTab = ref('all')
const searchText = ref('')
const router = useRouter()
const requests = ref([])
const loading = ref(false)
const loadError = ref('')

function countByType(type) {
  return requests.value.filter((request) => request.analysis.type === type).length
}

const summaryStats = computed(() => [
  { label: '전체 미션', value: requests.value.length, description: '등록된 미션' },
  { label: '설계 완료', value: countByType('done'), description: '권한 제안 확인 가능' },
  { label: '설계 중', value: countByType('progress'), description: '권한을 설계하는 중' }
])

const tabs = computed(() => [
  { key: 'all', label: '전체', count: requests.value.length },
  { key: 'progress', label: '설계 중', count: countByType('progress') },
  { key: 'done', label: '설계 완료', count: countByType('done') },
  { key: 'failed', label: '실패', count: countByType('failed') }
])

const columns = [
  { title: '미션', key: 'request', width: 230 },
  { title: '권한 설계', key: 'analysis', width: 110 },
  { title: '실행 Agent', key: 'agent', width: 155 },
  { title: '연결된 Passport', key: 'passport', width: 185 },
  { title: '생성일', key: 'createdAt', width: 100 }
]

function toRow(item, passport) {
  const statusMeta = WORK_STATUS_META[item.status] || { label: item.status, type: 'progress' }
  const passportStatus = passport?.status || 'NONE'
  const passportMeta = PASSPORT_STATUS_META[passportStatus] || PASSPORT_STATUS_META.NONE
  return {
    id: item.id,
    title: item.title,
    category: CATEGORY_LABEL[item.category] || item.category,
    status: item.status,
    analysis: { type: statusMeta.type, label: statusMeta.label },
    agent: getAgentSummary(item.analysis || passport || item),
    passport: {
      id: passport?.id,
      name: getPassportDisplayName(item.title),
      type: passportMeta.type,
      label: passportMeta.label
    },
    createdAt: item.createdAt
  }
}

onMounted(async () => {
  loading.value = true
  const [courseResult, passportResult] = await Promise.allSettled([
    courseApi.getMyList(),
    enrollmentApi.getMyPassports()
  ])

  if (courseResult.status === 'rejected') {
    loadError.value = '미션을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.'
    loading.value = false
    return
  }

  if (passportResult.status === 'rejected') {
    loadError.value = 'Passport 상태를 불러오지 못했습니다. 미션은 계속 확인할 수 있습니다.'
  }

  const courseRes = courseResult.value
  const passportRes = passportResult.status === 'fulfilled' ? passportResult.value : { data: [] }
  const passportsByCourse = new Map(
    (passportRes.data || []).map((passport) => [String(passport.courseId), passport])
  )
  requests.value = (courseRes.data || []).map((course) =>
    toRow(course, passportsByCourse.get(String(course.id)))
  )
  loading.value = false
})

function openPassport(record) {
  if (record.passport.id) {
    router.push(`/passports/${record.passport.id}`)
  } else {
    router.push('/passports')
  }
}

function continueRequest(record) {
  if (record.analysis.type === 'done') {
    router.push(`/work-requests/${record.id}/analysis`)
    return
  }
  router.push(`/work-requests/${record.id}`)
}

const filteredRequests = computed(() => {
  const query = searchText.value.trim().toLowerCase()
  return requests.value.filter((request) => {
    const matchesTab = activeTab.value === 'all' || request.analysis.type === activeTab.value
    const matchesSearch = !query || `${request.id} ${request.title} ${request.passport.name}`.toLowerCase().includes(query)
    return matchesTab && matchesSearch
  })
})
</script>

<style scoped>
.work-page { width: min(100%, 1120px); margin: 0 auto; }
.page-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 24px; margin-bottom: 26px; }
.eyebrow { margin: 0 0 8px; color: var(--forest-primary); font-size: 12px; font-weight: 600; letter-spacing: .09em; }
.page-heading h1 { margin: 0; color: var(--forest-text); font-size: clamp(28px, 3vw, 34px); line-height: 1.25; letter-spacing: -.04em; }
.description { margin: 12px 0 0; color: var(--forest-text-secondary); font-size: 15px; }
.new-request-button { height: 46px; padding-inline: 20px; border: 1px solid var(--forest-primary); border-radius: 10px; background: var(--forest-primary); box-shadow: none; }
.summary-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; margin-bottom: 22px; }
.summary-card { min-height: 112px; border: 1px solid var(--forest-border) !important; border-radius: 11px; background: var(--forest-surface); }
.summary-card :deep(.ant-card-body) { min-height: 112px; padding: 21px 24px; display: flex; flex-direction: column; justify-content: center; }
.summary-card span { color: var(--forest-text-secondary); font-size: 14px; }
.summary-card strong { margin-top: 3px; color: var(--forest-text); font-size: 29px; line-height: 1.2; }
.summary-card small { margin-top: 5px; color: var(--forest-text-secondary); font-size: 11px; }
.request-panel { overflow: hidden; border: 1px solid var(--forest-border); border-radius: 12px; background: var(--forest-surface); }
.load-alert { margin: 16px 20px 0; }
.panel-toolbar { min-height: 76px; padding: 15px 20px 0; display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; border-bottom: 1px solid var(--forest-border); }
.status-tabs { display: flex; align-items: stretch; gap: 22px; align-self: stretch; }
.status-tab { position: relative; display: flex; align-items: center; gap: 8px; padding: 0 4px 15px; border: 0; color: var(--forest-text-secondary); background: transparent; font-size: 14px; white-space: nowrap; }
.status-tab.active { color: var(--forest-primary-dark); }
.status-tab.active::after { content: ''; position: absolute; left: 0; right: 0; bottom: 0; height: 3px; border-radius: 2px 2px 0 0; background: var(--forest-primary); box-shadow: none; }
.tab-count { min-width: 23px; height: 23px; padding: 0 6px; display: inline-grid; place-items: center; border-radius: 999px; color: var(--forest-text-secondary); background: #edf1ee; font-size: 12px; }
.status-tab.active .tab-count { color: white; background: var(--forest-primary); }
.search-input { width: 250px; background: var(--forest-subtle); }
.search-input :deep(.ant-input) { color: var(--forest-text) !important; background: var(--forest-subtle) !important; border-color: var(--forest-border-strong) !important; }
.search-input :deep(.ant-input::placeholder), .search-input :deep(.ant-input-prefix) { color: var(--forest-text-secondary); }
.request-table :deep(.ant-table), .request-table :deep(.ant-table-container), .request-table :deep(.ant-table-content) { background: transparent; }
.request-table :deep(.ant-table-thead > tr > th) { height: 58px; color: var(--forest-text-secondary); font-size: 12px; font-weight: 500; background: var(--forest-subtle); border-bottom-color: var(--forest-border); }
.request-table :deep(.ant-table-tbody > tr > td) { height: 70px; color: var(--forest-text); font-size: 12px; background: transparent; border-bottom-color: var(--forest-border); }
.request-table :deep(.ant-table-tbody > tr:hover > td) { background: rgba(72, 59, 255, .05) !important; }
.request-link, .passport-link, .next-step-link { padding: 0; display: grid; gap: 3px; border: 0; color: inherit; text-align: left; background: transparent; }
.request-link strong, .passport-link strong, .next-step-link strong { color: var(--forest-text); font-weight: 600; }
.request-link small { color: var(--forest-text-secondary); font-size: 10px; }
.request-link:hover strong, .request-link:focus-visible strong, .passport-link:hover strong, .passport-link:focus-visible strong, .next-step-link:hover strong, .next-step-link:focus-visible strong { color: var(--forest-primary); }
.next-step-link strong { color: var(--forest-primary); font-size: 12px; }
.agent-name, .created-at { color: var(--forest-text); white-space: nowrap; }
.status-pill { width: fit-content; min-height: 26px; padding: 4px 10px; display: inline-flex; align-items: center; gap: 6px; border-radius: 999px; border: 1px solid transparent; font-size: 11px; line-height: 1; white-space: nowrap; }
.analysis-done, .passport-active { color: #126b48; background: #e4f2ea; border-color: #b9d9c8; }
.analysis-progress { color: var(--forest-primary); background: #e4f2ea; border-color: #b9d9c8; }
.analysis-failed, .passport-rejected { color: #b33a3a; background: #fceded; border-color: #edc8c8; }
.passport-pending { color: #80510c; background: #fff5dc; border-color: #efdba9; }
.passport-none { color: var(--forest-text-secondary); font-size: 11px; }
.panel-footer { min-height: 70px; display: grid; place-items: center; color: var(--forest-text-secondary); font-size: 12px; }
@media (max-width: 900px) { .panel-toolbar { flex-direction: column; padding-top: 16px; } .status-tabs { width: 100%; min-height: 48px; overflow-x: auto; } .search-input { width: 100%; margin-bottom: 16px; } }
@media (max-width: 640px) { .page-heading { flex-direction: column; } .new-request-button { width: 100%; } .summary-grid { grid-template-columns: 1fr; } }
</style>
