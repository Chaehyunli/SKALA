<template>
  <AdminLayout>
    <div class="admin-page">
      <section class="page-heading">
        <p class="eyebrow">ADMIN CONSOLE</p>
        <h1>발급 검토</h1>
        <p class="description">Passport 발급 요청의 권한 범위를 검토합니다.</p>
      </section>

      <a-alert v-if="loadError" class="load-alert" type="error" show-icon :message="loadError">
        <template #action>
          <a-button type="link" size="small" @click="loadPending">다시 시도</a-button>
        </template>
      </a-alert>

      <section class="admin-panel">
        <a-table
          class="admin-table"
          :columns="columns"
          :data-source="pending"
          :loading="loading"
          :pagination="false"
          :custom-row="customRow"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'passport'">
              <button class="passport-cell passport-link" type="button" @click.stop="openDetail(record)">
                <span class="row-icon"><ClockCircleOutlined /></span>
                <div class="passport-id">
                  <strong>{{ record.courseTitle }}</strong>
                  <span>Passport #{{ record.id }}</span>
                </div>
              </button>
            </template>

            <template v-else-if="column.key === 'agent'">
              <span class="agent-name">{{ getAgentSummary(record) }}</span>
            </template>

            <template v-else-if="column.key === 'riskLevel'">
              <a-tag :color="RISK_LEVEL_META[record.riskLevel]?.color">{{ RISK_LEVEL_META[record.riskLevel]?.label }}</a-tag>
            </template>
            <template v-else-if="column.key === 'permissions'">
              <span>{{ record.permissionCount }}개</span>
            </template>
            <template v-else-if="column.key === 'createdAt'">
              <span>{{ formatDate(record.createdAt) }}</span>
            </template>
          </template>

          <template #emptyText>
            <div class="empty-state">검토 대기 중인 Passport가 없습니다.</div>
          </template>
        </a-table>
      </section>
    </div>

  </AdminLayout>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { enrollmentApi } from '@/api/enrollment.js'
import { getAgentSummary, RISK_LEVEL_META } from '@/constants/workRequest.js'
import { ClockCircleOutlined } from '@ant-design/icons-vue'

const pending = ref([])
const router = useRouter()
const loading = ref(false)
const loadError = ref('')

const columns = [
  { title: 'Passport', key: 'passport', width: 240 },
  { title: 'Agent', key: 'agent', width: 170 },
  { title: '권한', key: 'permissions', width: 70 },
  { title: '위험도', key: 'riskLevel', width: 90 },
  { title: '요청일', key: 'createdAt', width: 120 }
]

function openDetail(record) {
  router.push(`/admin/passports/${record.id}`)
}

function customRow(record) {
  return {
    class: 'clickable-row',
    onClick: () => openDetail(record)
  }
}

function formatDate(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

async function loadPending() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await enrollmentApi.getAdminPending()
    pending.value = res.data
  } catch (error) {
    console.error('[AdminPendingView] 승인 대기 목록 조회 실패:', error)
    pending.value = []
    loadError.value = '발급 검토 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(loadPending)
</script>

<style scoped>
.admin-page { width: min(100%, 1000px); margin: 0 auto; }
.eyebrow { margin: 0 0 8px; color: var(--forest-primary); font-size: 12px; font-weight: 600; letter-spacing: .09em; }
.page-heading h1 { margin: 0; color: var(--forest-text); font-size: clamp(28px, 3vw, 34px); letter-spacing: -.04em; }
.description { margin: 12px 0 24px; color: var(--forest-text-secondary); font-size: 15px; }

.load-alert { margin: -6px 0 18px; color: #b33a3a; background: #fceded; border-color: #edc8c8; }
.load-alert :deep(.ant-alert-message), .load-alert :deep(.ant-alert-icon) { color: #b33a3a; }
.load-alert :deep(.ant-btn-link) { color: #b33a3a; }

.admin-panel { overflow: hidden; border: 1px solid var(--forest-border); border-radius: 12px; background: var(--forest-surface); }

.admin-table :deep(.ant-table), .admin-table :deep(.ant-table-container), .admin-table :deep(.ant-table-content) { background: transparent; }
.admin-table :deep(.ant-table-thead > tr > th) { height: 58px; color: var(--forest-text-secondary); font-size: 12px; font-weight: 500; background: var(--forest-subtle); border-bottom-color: var(--forest-border); }
.admin-table :deep(.ant-table-tbody > tr > td) { height: 67px; color: var(--forest-text); font-size: 12px; background: transparent; border-bottom-color: var(--forest-border); }
.admin-table :deep(.ant-table-tbody > tr:hover > td) { background: rgba(72, 59, 255, .05) !important; }
.admin-table :deep(.clickable-row) { cursor: pointer; }

.passport-cell { display: flex; align-items: center; gap: 11px; }
.passport-link { width: 100%; padding: 0; border: 0; color: inherit; text-align: left; background: transparent; cursor: pointer; }
.row-icon { width: 28px; height: 28px; flex: 0 0 28px; display: grid; place-items: center; border-radius: 8px; font-size: 16px; color: #80510c; background: #fff5dc; }
.passport-id { display: flex; flex-direction: column; gap: 1px; }
.passport-id strong { color: var(--forest-text); font-size: 12px; font-weight: 600; }
.passport-id span { color: var(--forest-text-secondary); font-size: 11px; }
.agent-name { color: var(--forest-text); }

.empty-state { padding: 40px 0; text-align: center; color: var(--forest-text-secondary); font-size: 13px; }

@media (max-width: 640px) {
  .admin-panel { overflow-x: auto; }
}
</style>
