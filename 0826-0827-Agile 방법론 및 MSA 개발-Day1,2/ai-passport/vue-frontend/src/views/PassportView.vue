<template>
  <MainLayout>
    <div class="passport-page">
      <section class="page-heading">
        <div>
          <p class="eyebrow">MY PASSPORT</p>
          <h1>Passport</h1>
          <p class="description">미션별로 발급된 권한과 사용 상태를 확인합니다.</p>
        </div>
        <a-button type="primary" size="large" class="new-request-button" @click="router.push('/work-requests/new')">
          새 미션
        </a-button>
      </section>

      <section class="summary-grid" aria-label="Passport 요약">
        <a-card v-for="stat in summaryStats" :key="stat.label" class="summary-card" :bordered="false">
          <span>{{ stat.label }}</span>
          <strong>{{ stat.value }}</strong>
          <small>{{ stat.description }}</small>
        </a-card>
      </section>

      <a-alert v-if="loadError" class="load-alert" type="error" show-icon :message="loadError">
        <template #action><a-button type="link" size="small" @click="loadPassports">다시 시도</a-button></template>
      </a-alert>

      <section class="passport-panel">
        <div class="panel-toolbar">
          <div class="status-tabs" role="tablist" aria-label="Passport 상태">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              type="button"
              :class="['status-tab', { active: activeTab === tab.key }]"
              role="tab"
              :aria-selected="activeTab === tab.key"
              @click="activeTab = tab.key"
            >
              {{ tab.label }} <span class="tab-count">{{ tab.count }}</span>
            </button>
          </div>
          <a-input v-model:value="searchText" class="search-input" size="large" placeholder="Passport 또는 미션 검색" allow-clear>
            <template #prefix><SearchOutlined /></template>
          </a-input>
        </div>

        <a-table
          class="passport-table"
          :columns="columns"
          :data-source="filteredPassports"
          :loading="loading"
          :pagination="false"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'passport'">
              <button class="passport-link" type="button" @click="openPassport(record.id)">
                <strong>{{ record.name }}</strong>
                <small>Passport #{{ record.id }}</small>
              </button>
            </template>
            <template v-else-if="column.key === 'agent'"><span class="agent-name">{{ record.agent }}</span></template>
            <template v-else-if="column.key === 'task'"><span class="task-name">{{ record.task }}</span></template>
            <template v-else-if="column.key === 'permissions'"><span class="permission-count">{{ record.permissions }}개</span></template>
            <template v-else-if="column.key === 'status'">
              <span :class="['status-pill', `status-${record.statusKey}`]"><i class="status-dot" />{{ record.status }}</span>
            </template>
            <template v-else-if="column.key === 'expires'">
              <span :class="['expires', { muted: ['expired', 'rejected'].includes(record.statusKey) }]">{{ record.expires }}</span>
            </template>
          </template>
        </a-table>

        <div class="panel-footer">Passport {{ filteredPassports.length }}개</div>
      </section>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import { enrollmentApi } from '@/api/enrollment.js'
import { countAgentPermissions, getAgentSummary, getPassportDisplayName, PASSPORT_STATUS_META } from '@/constants/workRequest.js'
import { SearchOutlined } from '@ant-design/icons-vue'

const router = useRouter()
const activeTab = ref('all')
const searchText = ref('')
const loading = ref(false)
const loadError = ref('')
const passports = ref([])

const columns = [
  { title: 'Passport', key: 'passport', width: 190 },
  { title: 'Agent', key: 'agent', width: 130 },
  { title: '미션', key: 'task', width: 155 },
  { title: '권한', key: 'permissions', width: 70 },
  { title: '상태', key: 'status', width: 100 },
  { title: '유효기간', key: 'expires', width: 110 }
]

function toStatusKey(status) {
  const type = PASSPORT_STATUS_META[status]?.type
  if (type === 'active') return 'active'
  if (type === 'rejected') return 'rejected'
  if (status === 'EXPIRED') return 'expired'
  return 'pending'
}

function normalizePassport(item) {
  const statusKey = toStatusKey(item.status)
  return {
    id: item.id,
    name: getPassportDisplayName(item.courseTitle),
    agent: getAgentSummary(item),
    task: item.courseTitle || '-',
    permissions: Number(item.permissionCount ?? countAgentPermissions(item)),
    status: PASSPORT_STATUS_META[item.status]?.label || (statusKey === 'expired' ? '만료' : item.status),
    statusKey,
    expires: item.validPeriod || item.statusInfo?.remainingTime || '-'
  }
}

const summaryStats = computed(() => {
  const count = (key) => passports.value.filter((passport) => passport.statusKey === key).length
  const ended = count('expired') + count('rejected')
  return [
    { label: '사용 가능', value: count('active'), description: '현재 바로 사용할 수 있음' },
    { label: '검토 대기', value: count('pending'), description: '발급 검토 중' },
    { label: '종료·반려', value: ended, description: '다시 발급 요청 가능' }
  ]
})

const tabs = computed(() => [
  { key: 'all', label: '전체', count: passports.value.length },
  { key: 'active', label: '사용 가능', count: passports.value.filter((p) => p.statusKey === 'active').length },
  { key: 'pending', label: '검토 대기', count: passports.value.filter((p) => p.statusKey === 'pending').length },
  { key: 'ended', label: '종료·반려', count: passports.value.filter((p) => ['expired', 'rejected'].includes(p.statusKey)).length }
])

async function loadPassports() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await enrollmentApi.getMyPassports()
    passports.value = (res.data || []).map(normalizePassport)
  } catch (error) {
    console.error('[PassportView] Passport 목록 조회 실패:', error)
    passports.value = []
    loadError.value = 'Passport 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function openPassport(id) {
  router.push({ name: 'PassportDetail', params: { id } })
}

const filteredPassports = computed(() => {
  const query = searchText.value.trim().toLowerCase()
  return passports.value.filter((passport) => {
    const matchesTab = activeTab.value === 'all'
      || passport.statusKey === activeTab.value
      || (activeTab.value === 'ended' && ['expired', 'rejected'].includes(passport.statusKey))
    const matchesSearch = !query || `${passport.id} ${passport.name} ${passport.agent} ${passport.task}`.toLowerCase().includes(query)
    return matchesTab && matchesSearch
  })
})

onMounted(loadPassports)
</script>

<style scoped>
.passport-page { width: min(100%, 1120px); margin: 0 auto; }
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
.load-alert { margin: -6px 0 18px; color: #b33a3a; background: #fceded; border-color: #edc8c8; }
.load-alert :deep(.ant-alert-message), .load-alert :deep(.ant-alert-icon) { color: #b33a3a; }
.load-alert :deep(.ant-btn-link) { color: #b33a3a; }
.passport-panel { overflow: hidden; border: 1px solid var(--forest-border); border-radius: 12px; background: var(--forest-surface); }
.panel-toolbar { min-height: 76px; padding: 15px 18px 0; display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; border-bottom: 1px solid var(--forest-border); }
.status-tabs { display: flex; align-items: stretch; gap: 20px; align-self: stretch; }
.status-tab { position: relative; display: flex; align-items: center; gap: 7px; padding: 0 3px 15px; border: 0; color: var(--forest-text-secondary); background: transparent; font-size: 13px; white-space: nowrap; }
.status-tab.active { color: var(--forest-primary-dark); }
.status-tab.active::after { content: ''; position: absolute; left: 0; right: 0; bottom: 0; height: 3px; border-radius: 2px 2px 0 0; background: var(--forest-primary); box-shadow: none; }
.tab-count { min-width: 20px; height: 20px; padding: 0 5px; display: inline-grid; place-items: center; border-radius: 999px; color: var(--forest-text-secondary); background: #edf1ee; font-size: 11px; }
.status-tab.active .tab-count { color: white; background: var(--forest-primary); }
.search-input { width: 250px; background: var(--forest-subtle); }
.search-input :deep(.ant-input) { color: var(--forest-text) !important; background: var(--forest-subtle) !important; border-color: var(--forest-border-strong) !important; }
.search-input :deep(.ant-input::placeholder), .search-input :deep(.ant-input-prefix) { color: var(--forest-text-secondary); }
.passport-table :deep(.ant-table), .passport-table :deep(.ant-table-container), .passport-table :deep(.ant-table-content) { background: transparent; }
.passport-table :deep(.ant-table-thead > tr > th) { height: 58px; color: var(--forest-text-secondary); font-size: 12px; font-weight: 500; background: var(--forest-subtle); border-bottom-color: var(--forest-border); }
.passport-table :deep(.ant-table-tbody > tr > td) { height: 67px; color: var(--forest-text); font-size: 12px; background: transparent; border-bottom-color: var(--forest-border); }
.passport-table :deep(.ant-table-tbody > tr:hover > td) { background: rgba(72, 59, 255, .05) !important; }
.passport-link { width: 100%; padding: 0; display: grid; gap: 3px; border: 0; color: inherit; text-align: left; background: transparent; }
.passport-link strong { color: var(--forest-text); font-size: 12px; font-weight: 600; }
.passport-link small { color: var(--forest-text-secondary); font-size: 10px; }
.passport-link:hover strong, .passport-link:focus-visible strong { color: var(--forest-primary); }
.agent-name { color: var(--forest-text); white-space: nowrap; }
.task-name { color: var(--forest-text); white-space: nowrap; }
.permission-count { color: var(--forest-text-secondary); }
.expires { color: var(--forest-primary); font-size: 11px; white-space: nowrap; }
.expires.muted { color: var(--forest-text-secondary); }
.status-pill { min-height: 25px; padding: 4px 10px; display: inline-flex; align-items: center; gap: 6px; border-radius: 999px; border: 1px solid transparent; font-size: 11px; line-height: 1; white-space: nowrap; }
.status-dot { width: 6px; height: 6px; display: inline-block; border-radius: 50%; background: currentColor; }
.status-active { color: #126b48; background: #e4f2ea; border-color: #b9d9c8; }
.status-pending { color: #80510c; background: #fff5dc; border-color: #efdba9; }
.status-expired { color: var(--forest-text-secondary); background: #eef1ef; border-color: #d8deda; }
.status-rejected { color: #b33a3a; background: #fceded; border-color: #edc8c8; }
.panel-footer { min-height: 70px; display: grid; place-items: center; color: var(--forest-text-secondary); font-size: 12px; }
@media (max-width: 900px) { .panel-toolbar { flex-direction: column; padding-top: 16px; } .status-tabs { width: 100%; min-height: 48px; overflow-x: auto; } .search-input { width: 100%; margin-bottom: 16px; } }
@media (max-width: 640px) { .page-heading { flex-direction: column; } .new-request-button { width: 100%; } .summary-grid { grid-template-columns: 1fr; } }
</style>
