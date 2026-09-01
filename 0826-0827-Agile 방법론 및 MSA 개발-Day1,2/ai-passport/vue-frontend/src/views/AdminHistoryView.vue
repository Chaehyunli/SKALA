<template>
  <AdminLayout>
    <div class="admin-page">
      <section class="page-heading">
        <p class="eyebrow">ADMIN CONSOLE</p>
        <h1>검토 이력</h1>
        <p class="description">모든 Passport 발급 요청과 처리 결과를 확인합니다.</p>
      </section>

      <a-alert v-if="loadError" class="load-alert" type="error" show-icon :message="loadError">
        <template #action>
          <a-button type="link" size="small" @click="loadHistory">다시 시도</a-button>
        </template>
      </a-alert>

      <section class="admin-panel">
        <div class="panel-toolbar">
          <div class="status-tabs" role="tablist" aria-label="상태 필터">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              type="button"
              :class="['status-tab', { active: activeTab === tab.key }]"
              @click="activeTab = tab.key"
            >
              {{ tab.label }}
              <span class="tab-count">{{ tab.count }}</span>
            </button>
          </div>
        </div>

        <a-table
          class="admin-table"
          :columns="columns"
          :data-source="filteredHistory"
          :loading="loading"
          :pagination="false"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'passport'">
              <div class="passport-cell">
                <div class="passport-id">
                  <strong>{{ record.id }}</strong>
                  <span>{{ record.courseTitle }}</span>
                </div>
              </div>
            </template>

            <template v-else-if="column.key === 'agent'">
              <span class="agent-name">{{ getAgentSummary(record) }}</span>
            </template>

            <template v-else-if="column.key === 'riskLevel'">
              <a-tag :color="RISK_LEVEL_META[record.riskLevel]?.color">{{ RISK_LEVEL_META[record.riskLevel]?.label }}</a-tag>
            </template>

            <template v-else-if="column.key === 'status'">
              <span :class="['status-pill', `status-${statusMeta(record.status).type}`]">{{ statusMeta(record.status).label }}</span>
            </template>

            <template v-else-if="column.key === 'processedBy'">
              <span v-if="record.status === 'READY_FOR_APPROVAL'" class="muted">-</span>
              <span v-else>{{ record.approvedBy }}</span>
            </template>

            <template v-else-if="column.key === 'note'">
              <span v-if="record.status === 'REJECTED'" class="reject-reason">{{ record.rejectedReason }}</span>
              <span v-else class="muted">-</span>
            </template>
          </template>

          <template #emptyText>
            <div class="empty-state">검토 이력이 없습니다.</div>
          </template>
        </a-table>
      </section>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { enrollmentApi } from '@/api/enrollment.js'
import { getAgentSummary, PASSPORT_STATUS_META, RISK_LEVEL_META } from '@/constants/workRequest.js'

const history = ref([])
const loading = ref(false)
const loadError = ref('')
const activeTab = ref('all')

const columns = [
  { title: 'Passport', key: 'passport', width: 190 },
  { title: 'Agent', key: 'agent', width: 120 },
  { title: '위험도', key: 'riskLevel', width: 70 },
  { title: '상태', key: 'status', width: 85 },
  { title: '요청일', dataIndex: 'createdAt', key: 'createdAt', width: 90 },
  { title: '검토 담당자', key: 'processedBy', width: 115 },
  { title: '반려 사유', key: 'note', width: 140 }
]

function statusMeta(status) {
  const meta = PASSPORT_STATUS_META[status] || PASSPORT_STATUS_META.NONE
  const colorByType = { active: 'active', pending: 'pending', rejected: 'rejected', none: 'none' }
  return { label: meta.label, type: colorByType[meta.type] }
}

const tabs = computed(() => [
  { key: 'all', label: '전체', count: history.value.length },
  { key: 'READY_FOR_APPROVAL', label: '검토 대기', count: history.value.filter((h) => h.status === 'READY_FOR_APPROVAL').length },
  { key: 'ACTIVE', label: '발급 완료', count: history.value.filter((h) => h.status === 'ACTIVE').length },
  { key: 'REJECTED', label: '반려', count: history.value.filter((h) => h.status === 'REJECTED').length }
])

const filteredHistory = computed(() =>
  activeTab.value === 'all' ? history.value : history.value.filter((h) => h.status === activeTab.value)
)

async function loadHistory() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await enrollmentApi.getAdminAll()
    history.value = res.data
  } catch (error) {
    console.error('[AdminHistoryView] 전체 이력 조회 실패:', error)
    history.value = []
    loadError.value = '검토 이력을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(loadHistory)
</script>

<style scoped>
.admin-page { width: min(100%, 1120px); margin: 0 auto; }
.eyebrow { margin: 0 0 8px; color: var(--forest-primary); font-size: 12px; font-weight: 600; letter-spacing: .09em; }
.page-heading h1 { margin: 0; color: var(--forest-text); font-size: clamp(28px, 3vw, 34px); letter-spacing: -.04em; }
.description { margin: 12px 0 24px; color: var(--forest-text-secondary); font-size: 15px; }

.load-alert { margin: -6px 0 18px; color: #b33a3a; background: #fceded; border-color: #edc8c8; }
.load-alert :deep(.ant-alert-message), .load-alert :deep(.ant-alert-icon) { color: #b33a3a; }
.load-alert :deep(.ant-btn-link) { color: #b33a3a; }

.admin-panel { overflow: hidden; border: 1px solid var(--forest-border); border-radius: 12px; background: var(--forest-surface); }

.panel-toolbar { min-height: 62px; padding: 15px 18px 0; border-bottom: 1px solid var(--forest-border); }
.status-tabs { display: flex; align-items: stretch; gap: 20px; }
.status-tab { position: relative; display: flex; align-items: center; gap: 7px; padding: 0 3px 15px; border: 0; color: var(--forest-text-secondary); background: transparent; font-size: 13px; white-space: nowrap; }
.status-tab.active { color: var(--forest-primary-dark); }
.status-tab.active::after { content: ''; position: absolute; left: 0; right: 0; bottom: 0; height: 3px; border-radius: 2px 2px 0 0; background: var(--forest-primary); box-shadow: none; }
.tab-count { min-width: 20px; height: 20px; padding: 0 5px; display: inline-grid; place-items: center; border-radius: 999px; color: var(--forest-text-secondary); background: #edf1ee; font-size: 11px; }
.status-tab.active .tab-count { color: white; background: var(--forest-primary); }

.admin-table :deep(.ant-table), .admin-table :deep(.ant-table-container), .admin-table :deep(.ant-table-content) { background: transparent; }
.admin-table :deep(.ant-table-thead > tr > th) { height: 58px; color: var(--forest-text-secondary); font-size: 12px; font-weight: 500; background: var(--forest-subtle); border-bottom-color: var(--forest-border); }
.admin-table :deep(.ant-table-tbody > tr > td) { height: 62px; color: var(--forest-text); font-size: 12px; background: transparent; border-bottom-color: var(--forest-border); }

.passport-cell { display: flex; align-items: center; gap: 11px; }
.passport-id { display: flex; flex-direction: column; gap: 1px; }
.passport-id strong { color: var(--forest-text); font-size: 12px; font-weight: 600; }
.passport-id span { color: var(--forest-text-secondary); font-size: 11px; }
.agent-name { color: var(--forest-text); }
.muted { color: var(--forest-text-muted); }
.reject-reason { color: #b33a3a; }

.status-pill { min-height: 25px; padding: 4px 10px; display: inline-flex; align-items: center; border-radius: 999px; border: 1px solid transparent; font-size: 11px; line-height: 1; white-space: nowrap; }
.status-active { color: #126b48; background: #e4f2ea; border-color: #b9d9c8; }
.status-pending { color: #80510c; background: #fff5dc; border-color: #efdba9; }
.status-rejected { color: #b33a3a; background: #fceded; border-color: #edc8c8; }
.status-none { color: var(--forest-text-secondary); background: #eef1ef; border-color: #d8deda; }

.empty-state { padding: 40px 0; text-align: center; color: var(--forest-text-secondary); font-size: 13px; }

@media (max-width: 900px) {
  .status-tabs { overflow-x: auto; }
}
</style>
