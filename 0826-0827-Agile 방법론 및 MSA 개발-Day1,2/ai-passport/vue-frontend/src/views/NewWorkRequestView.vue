<template>
  <MainLayout>
    <div class="content-page">
      <p class="eyebrow">NEW MISSION</p>
      <h1>새 미션 만들기</h1>
      <p class="description">미션의 목적과 범위를 바탕으로 실행 Agent와 최소 권한을 설계합니다.</p>

      <a-card class="form-card" :bordered="false">
        <a-form layout="vertical">
          <a-form-item label="미션 이름">
            <a-input v-model:value="title" size="large" placeholder="예: 이탈 가능 고객 리텐션 캠페인" />
          </a-form-item>

          <a-form-item label="미션 브리프">
            <a-textarea
              v-model:value="description"
              :rows="6"
              placeholder="예: 이번 달 고객 매출과 최근 활동을 비교해 이탈 가능성이 높은 고객을 찾고, 담당자가 검토할 리텐션 이메일 초안을 작성해줘."
            />
            <p class="hint">목적과 실행 범위를 구체적으로 작성하면 더 적합한 권한을 제안할 수 있습니다.</p>
          </a-form-item>

          <a-form-item label="미션 유형">
            <a-radio-group v-model:value="category" button-style="solid">
              <a-radio-button v-for="opt in CATEGORY_OPTIONS" :key="opt.code" :value="opt.code">
                {{ opt.label }}
              </a-radio-button>
            </a-radio-group>
          </a-form-item>

          <a-form-item label="희망 사용 기간">
            <a-radio-group v-model:value="usagePeriod" button-style="solid">
              <a-radio-button v-for="opt in USAGE_PERIOD_OPTIONS" :key="opt.code" :value="opt.code">
                {{ opt.label }}
              </a-radio-button>
            </a-radio-group>
          </a-form-item>

          <a-form-item label="데이터 민감도" class="sensitivity-item">
            <a-select v-model:value="dataSensitivity" size="large">
              <a-select-option v-for="opt in DATA_SENSITIVITY_OPTIONS" :key="opt" :value="opt">{{ opt }}</a-select-option>
            </a-select>
          </a-form-item>

          <div class="actions">
            <a-button size="large" @click="router.push('/')">임시 저장</a-button>
            <a-button
              type="primary"
              size="large"
              :loading="loadingCredit || submitting"
              :disabled="!title.trim() || !description.trim()"
              @click="handleSubmit"
            >
              저장하고 권한 설계
            </a-button>
          </div>
        </a-form>
      </a-card>

      <a-modal
        v-model:open="analysisConfirmOpen"
        title="권한 설계 크레딧 사용"
        ok-text="120 크레딧 사용하고 요청"
        cancel-text="취소"
        :confirm-loading="submitting"
        :ok-button-props="{ disabled: !hasEnoughCredit }"
        @ok="requestAnalysis"
      >
        <div class="credit-confirmation">
          <p>AI 권한 설계 요청에 크레딧이 사용됩니다.</p>
          <dl>
            <div><dt>현재 보유 크레딧</dt><dd>{{ formatCredit(currentCredit) }} 크레딧</dd></div>
            <div><dt>차감 크레딧</dt><dd class="deduct-credit">-{{ formatCredit(analysisCredit) }} 크레딧</dd></div>
            <div class="remaining-credit"><dt>요청 후 잔액</dt><dd>{{ formatCredit(remainingCredit) }} 크레딧</dd></div>
          </dl>
          <a-alert
            v-if="!hasEnoughCredit"
            type="warning"
            show-icon
            message="크레딧이 부족합니다"
            description="크레딧 페이지에서 충전한 뒤 권한 설계를 요청해주세요."
          />
        </div>
      </a-modal>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import MainLayout from '@/layouts/MainLayout.vue'
import { courseApi } from '@/api/course.js'
import { paymentApi } from '@/api/payment.js'
import { CATEGORY_OPTIONS, USAGE_PERIOD_OPTIONS, DATA_SENSITIVITY_OPTIONS } from '@/constants/workRequest.js'
import { publishCreditBalance } from '@/utils/creditBalance.js'

const router = useRouter()
const title = ref('')
const description = ref('')
const category = ref(CATEGORY_OPTIONS[0].code)
const usagePeriod = ref(USAGE_PERIOD_OPTIONS[1].code)
const dataSensitivity = ref(DATA_SENSITIVITY_OPTIONS[1])
const submitting = ref(false)
const loadingCredit = ref(false)
const analysisConfirmOpen = ref(false)
const currentCredit = ref(0)
const analysisCredit = ref(120)

const hasEnoughCredit = computed(() => currentCredit.value >= analysisCredit.value)
const remainingCredit = computed(() => Math.max(0, currentCredit.value - analysisCredit.value))

async function handleSubmit() {
  loadingCredit.value = true
  try {
    const balanceResponse = await paymentApi.getBalance()
    currentCredit.value = Number(balanceResponse.data.balance || 0)
    analysisCredit.value = Number(balanceResponse.data.creditPerAnalysis || 120)
    analysisConfirmOpen.value = true
  } catch (error) {
    message.error(error.response?.data?.message || '현재 크레딧을 불러오지 못했습니다.')
  } finally {
    loadingCredit.value = false
  }
}

async function requestAnalysis() {
  if (!hasEnoughCredit.value) return

  submitting.value = true
  try {
    const created = await courseApi.create({
      title: title.value,
      description: description.value,
      category: category.value,
      usagePeriod: usagePeriod.value,
      dataSensitivity: dataSensitivity.value
    })
    await courseApi.analyze(created.data.id)
    const updatedBalance = await paymentApi.getBalance()
    publishCreditBalance(Number(updatedBalance.data.balance || 0))
    analysisConfirmOpen.value = false
    router.push(`/work-requests/${created.data.id}/analysis`)
  } catch (error) {
    message.error(error.response?.data?.message || '권한 설계 요청을 처리하지 못했습니다.')
  } finally {
    submitting.value = false
  }
}

function formatCredit(value) {
  return new Intl.NumberFormat('ko-KR').format(Number(value || 0))
}
</script>

<style scoped>
.content-page { width: min(100%, 920px); margin: 0 auto; }
.eyebrow { margin: 0 0 8px; color: var(--forest-primary); font-size: 12px; font-weight: 600; letter-spacing: .09em; }
h1 { margin: 0; color: var(--forest-text); font-size: 34px; letter-spacing: -.04em; }
.description { margin: 8px 0 18px; color: var(--forest-text-secondary); }
.form-card { border: 1px solid var(--forest-border) !important; background: var(--forest-surface); }
.form-card :deep(.ant-card-body) { padding: 22px 24px; }
.form-card :deep(.ant-form-item-label label) { color: var(--forest-text); font-size: 15px; }
.form-card :deep(input),
.form-card :deep(textarea) { color: var(--forest-text); background: var(--forest-subtle); border-color: var(--forest-border-strong); resize: vertical; }
.form-card :deep(.ant-select-selector) { color: var(--forest-text) !important; background: var(--forest-subtle) !important; border-color: var(--forest-border-strong) !important; }
.hint { margin: 8px 0 0; color: var(--forest-text-secondary); font-size: 13px; }
.sensitivity-item :deep(.ant-select) { max-width: 260px; }
.actions { display: flex; justify-content: flex-end; gap: 10px; }
.credit-confirmation > p { margin: 0 0 18px; color: var(--forest-text-secondary); }
.credit-confirmation dl { margin: 0 0 18px; border-top: 1px solid var(--forest-border); }
.credit-confirmation dl > div { display: flex; justify-content: space-between; gap: 16px; padding: 12px 0; border-bottom: 1px solid var(--forest-border); }
.credit-confirmation dt { color: var(--forest-text-secondary); }
.credit-confirmation dd { margin: 0; color: var(--forest-text); font-weight: 650; }
.credit-confirmation .deduct-credit { color: #c64747; }
.credit-confirmation .remaining-credit dd { color: var(--forest-primary); }
@media (max-width: 640px) { .form-card :deep(.ant-card-body) { padding: 20px; } .actions > * { flex: 1; } }
</style>
