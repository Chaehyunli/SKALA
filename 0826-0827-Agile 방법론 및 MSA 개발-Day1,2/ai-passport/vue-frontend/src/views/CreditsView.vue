<template>
  <MainLayout>
    <div class="credits-page">
      <section class="page-heading">
        <p class="eyebrow">CREDITS</p>
        <h1>크레딧</h1>
        <p class="description">권한 설계에 사용할 크레딧을 관리합니다.</p>
      </section>

      <a-card class="balance-card" :bordered="false">
        <div class="credit-balance-summary">
          <span class="credit-balance-label">현재 보유</span>
          <strong class="credit-balance-value">{{ formatNumber(balance) }} <small>크레딧</small></strong>
        </div>
        <div class="credit-balance-rule" />
        <div class="credit-balance-copy">
          <p class="balance-hint">기본 권한 설계 1회 <strong>{{ creditPerAnalysis }} 크레딧</strong></p>
          <p class="balance-subhint">미션 길이와 관계없이 기본 권한 설계에는 동일한 크레딧이 사용됩니다.</p>
        </div>
      </a-card>

      <a-card class="purchase-card" :bordered="false">
        <div class="purchase-title">
          <div>
            <h2>충전 패키지</h2>
            <p>충전 크레딧과 크레딧당 가격을 비교합니다.</p>
          </div>
          <span class="recommend-explain">추천 기준: 기본 권한 설계 약 {{ Math.floor(10000 / creditPerAnalysis) }}회</span>
        </div>

        <div class="package-grid" role="radiogroup" aria-label="충전 패키지">
          <button
            v-for="plan in creditPlans"
            :key="plan.credits"
            type="button"
            :class="['package-option', { selected: selectedPlan === plan.credits }]"
            role="radio"
            :aria-checked="selectedPlan === plan.credits"
            @click="selectedPlan = plan.credits"
          >
            <span v-if="plan.badge" class="package-badge">{{ plan.badge }}</span>
            <span class="package-credits">{{ formatNumber(plan.credits) }} <small>크레딧</small></span>
            <strong>{{ formatWon(plan.price) }}</strong>
            <span class="package-unit">크레딧당 {{ formatWon(plan.price / plan.credits) }}</span>
            <span class="package-capacity">기본 권한 설계 약 {{ Math.floor(plan.credits / creditPerAnalysis) }}회</span>
          </button>
        </div>

        <div class="payment-row">
          <span class="payment-label">결제 수단</span>
          <button class="payment-method" type="button" @click="paymentModalOpen = true">
            <span class="payment-radio"><span /></span>
            <CreditCardOutlined class="payment-icon" />
            <span class="payment-name">{{ selectedPayment.name }}</span>
            <span class="payment-tail">{{ selectedPayment.detail }}</span>
            <span class="change-link">변경</span>
          </button>
        </div>

        <div class="summary-divider" />
        <div class="purchase-summary">
          <div>
            <span>충전 후 잔액</span>
            <strong>{{ formatNumber(afterBalance) }} <small>크레딧</small></strong>
          </div>
          <div class="amount-summary">
            <span>결제 금액</span>
            <strong>{{ formatWon(selectedPackage.price) }}</strong>
          </div>
        </div>

        <a-button type="primary" size="large" class="purchase-button" :loading="purchasing" @click="handlePurchase">
          {{ formatWon(selectedPackage.price) }} 결제하고 충전
        </a-button>
        <p class="purchase-note">권한 설계가 완료된 경우에만 크레딧이 차감됩니다.</p>
      </a-card>

      <p class="credits-note">추가 설계나 재설계가 필요한 경우 실행 전에 사용할 크레딧을 안내합니다.</p>
    </div>

    <a-modal v-model:open="paymentModalOpen" title="결제 수단 변경" ok-text="선택" cancel-text="취소" @ok="paymentModalOpen = false">
      <a-radio-group v-model:value="selectedMethod" class="payment-options">
        <a-radio v-for="method in paymentMethods" :key="method.key" :value="method.key">
          <CreditCardOutlined /> {{ method.name }} {{ method.detail }}
        </a-radio>
      </a-radio-group>
    </a-modal>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import MainLayout from '@/layouts/MainLayout.vue'
import { paymentApi } from '@/api/payment.js'
import { publishCreditBalance } from '@/utils/creditBalance.js'
import { CreditCardOutlined } from '@ant-design/icons-vue'

const balance = ref(0)
const creditPerAnalysis = ref(120)
const selectedPlan = ref(10000)
const paymentModalOpen = ref(false)
const selectedMethod = ref('card')
const purchasing = ref(false)

const creditPlans = [
  { credits: 5000, price: 5500 },
  { credits: 10000, price: 10000, badge: '균형형' },
  { credits: 30000, price: 27000, badge: '단가 최저' }
]

const paymentMethods = [
  { key: 'card', name: '법인카드', detail: '···· 2048' },
  { key: 'account', name: '법인계좌', detail: '국민은행 ···· 3812' }
]

const selectedPackage = computed(() => creditPlans.find((plan) => plan.credits === selectedPlan.value) ?? creditPlans[1])
const selectedPayment = computed(() => paymentMethods.find((method) => method.key === selectedMethod.value) ?? paymentMethods[0])
const afterBalance = computed(() => balance.value + selectedPackage.value.credits)

function formatNumber(value) {
  return new Intl.NumberFormat('ko-KR').format(value)
}

function formatWon(value) {
  return `₩${Number(value).toLocaleString('ko-KR', { maximumFractionDigits: 2 })}`
}

async function loadBalance() {
  try {
    await fetchBalance()
  } catch (error) {
    console.warn('[CreditsView] 잔액을 불러오지 못했습니다.', error)
  }
}

async function fetchBalance() {
  const res = await paymentApi.getBalance()
  balance.value = Number(res.data.balance || 0)
  creditPerAnalysis.value = res.data.creditPerAnalysis || 120
  publishCreditBalance(balance.value)
  return balance.value
}

function wait(ms) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

async function waitForCredit(expectedBalance) {
  for (let attempt = 0; attempt < 10; attempt += 1) {
    const res = await paymentApi.getBalance()
    const currentBalance = Number(res.data.balance || 0)
    if (currentBalance >= expectedBalance) {
      balance.value = currentBalance
      publishCreditBalance(currentBalance)
      return true
    }
    await wait(250)
  }
  return false
}

async function handlePurchase() {
  purchasing.value = true
  try {
    const expectedBalance = balance.value + selectedPackage.value.credits
    await paymentApi.charge({ credits: selectedPackage.value.credits, amount: selectedPackage.value.price })
    balance.value = expectedBalance
    publishCreditBalance(expectedBalance)
    const reflected = await waitForCredit(expectedBalance)
    message.success(`${formatWon(selectedPackage.value.price)} 결제가 완료되어 충전됐습니다.`)
    if (!reflected) {
      message.info('결제는 완료됐으며 크레딧 잔액을 반영하고 있습니다.')
    }
  } catch (error) {
    message.error(error.response?.data?.message || '결제를 처리하지 못했습니다.')
  } finally {
    purchasing.value = false
  }
}

onMounted(loadBalance)
</script>

<style scoped>
.credits-page { width: min(100%, 1120px); margin: 0 auto; }
.page-heading { margin-bottom: 24px; }
.eyebrow { margin: 0 0 8px; color: var(--forest-primary); font-size: 14px; font-weight: 600; letter-spacing: .09em; }
.page-heading h1 { margin: 0; color: var(--forest-text); font-size: clamp(30px, 3vw, 38px); line-height: 1.2; letter-spacing: -.045em; }
.description { margin: 13px 0 0; color: var(--forest-text-secondary); font-size: 16px; }
.balance-card, .purchase-card { border: 1px solid var(--forest-border) !important; border-radius: 12px; background: var(--forest-surface); box-shadow: none; }
.balance-card { margin-bottom: 22px; }
.balance-card :deep(.ant-card-body) { padding: 20px 29px; display: flex; align-items: center; gap: 20px; }
.credit-balance-summary { flex: 0 0 220px; min-width: 220px; }
.credit-balance-label { display: block; color: var(--forest-text); font-size: 14px; white-space: nowrap; }
.credit-balance-value { display: block; margin-top: 4px; color: var(--forest-primary); font-size: 34px; line-height: 1; letter-spacing: -.04em; white-space: nowrap; }
.credit-balance-value small, .purchase-summary strong small { color: var(--forest-primary); font-size: 18px; font-weight: 500; letter-spacing: 0; }
.credit-balance-rule { width: 1px; height: 44px; flex: 0 0 1px; background: var(--forest-border); }
.credit-balance-copy { min-width: 0; display: grid; gap: 4px; }
.balance-hint, .balance-subhint { margin: 0; color: var(--forest-text); font-size: 14px; line-height: 1.5; word-break: keep-all; }
.balance-subhint { color: var(--forest-text-secondary); font-size: 12px; }
.purchase-card :deep(.ant-card-body) { padding: 28px 29px 24px; }
.purchase-title { margin-bottom: 20px; display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; }
.purchase-title h2 { margin: 0; color: var(--forest-text); font-size: 19px; font-weight: 600; }
.purchase-title p { margin: 6px 0 0; color: var(--forest-text-secondary); font-size: 12px; }
.recommend-explain { color: var(--forest-primary); font-size: 11px; text-align: right; }
.package-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.package-option { position: relative; min-height: 165px; padding: 24px 18px 17px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px; overflow: hidden; border: 1px solid var(--forest-border-strong); border-radius: 11px; color: var(--forest-text-secondary); background: var(--forest-subtle); transition: border-color .18s ease, background .18s ease, box-shadow .18s ease; }
.package-option:hover { border-color: var(--forest-primary); }
.package-option.selected { border: 2px solid var(--forest-primary); background: var(--forest-primary-soft); box-shadow: none; }
.package-credits { color: var(--forest-text); font-size: 24px; font-weight: 700; letter-spacing: -.04em; }
.package-credits small { color: var(--forest-text-secondary); font-size: 13px; font-weight: 500; letter-spacing: 0; }
.package-option strong { color: var(--forest-text); font-size: 20px; line-height: 1; }
.package-unit { color: var(--forest-text-secondary); font-size: 11px; }
.package-capacity { color: var(--forest-text-muted); font-size: 10px; }
.package-badge { position: absolute; top: 0; right: 0; padding: 6px 11px 7px; border-radius: 0 0 0 9px; color: white; background: var(--forest-primary); font-size: 11px; }
.payment-row { min-height: 79px; display: grid; grid-template-columns: 135px 1fr; align-items: center; gap: 24px; }
.payment-label { color: var(--forest-text); font-size: 16px; }
.payment-method { min-height: 59px; padding: 0 17px; display: flex; align-items: center; gap: 14px; border: 1px solid var(--forest-border); border-radius: 9px; color: var(--forest-text); background: var(--forest-subtle); text-align: left; }
.payment-method:hover { border-color: var(--forest-primary); }
.payment-radio { width: 26px; height: 26px; padding: 5px; display: grid; place-items: center; border: 2px solid var(--forest-primary); border-radius: 50%; }
.payment-radio span { width: 12px; height: 12px; border-radius: 50%; background: var(--forest-primary); box-shadow: none; }
.payment-icon { color: var(--forest-text); font-size: 22px; }
.payment-name { font-size: 16px; }
.payment-tail { color: var(--forest-text-secondary); font-size: 16px; }
.change-link { margin-left: auto; color: var(--forest-primary); font-size: 15px; }
.summary-divider { height: 1px; background: var(--forest-border); }
.purchase-summary { padding: 25px 0 26px; display: flex; align-items: flex-end; justify-content: space-between; }
.purchase-summary > div { display: flex; flex-direction: column; gap: 7px; }
.purchase-summary span { color: var(--forest-text); font-size: 16px; }
.purchase-summary strong { color: var(--forest-primary); font-size: 37px; line-height: 1; letter-spacing: -.04em; }
.amount-summary { align-items: flex-end; }
.amount-summary strong { color: var(--forest-text); font-size: 36px; }
.purchase-button { width: 100%; height: 58px; border: 1px solid var(--forest-primary); border-radius: 9px; color: white; background: var(--forest-primary); box-shadow: none; font-size: 19px; }
.purchase-note { margin: 17px 0 0; color: var(--forest-text-secondary); font-size: 14px; text-align: center; }
.credits-note { margin: 18px 0 0; color: var(--forest-text-secondary); font-size: 12px; text-align: center; }
.payment-options { width: 100%; display: flex; flex-direction: column; gap: 18px; }
.payment-options :deep(.ant-radio-wrapper) { color: var(--forest-text); font-size: 15px; }
.payment-options :deep(.anticon) { margin: 0 7px 0 3px; }
@media (max-width: 900px) { .package-grid { gap: 12px; } .purchase-title { align-items: flex-start; flex-direction: column; } .recommend-explain { text-align: left; } .payment-row { grid-template-columns: 1fr; gap: 10px; padding: 17px 0; } }
@media (max-width: 620px) { .page-heading h1 { font-size: 29px; } .description { font-size: 14px; } .balance-card :deep(.ant-card-body), .purchase-card :deep(.ant-card-body) { padding: 20px 16px; } .balance-card :deep(.ant-card-body) { align-items: stretch; flex-direction: column; gap: 14px; } .credit-balance-summary { flex-basis: auto; min-width: 0; } .credit-balance-rule { width: 100%; height: 1px; flex-basis: 1px; } .credit-balance-value { font-size: 28px; } .package-grid { grid-template-columns: 1fr; } .package-option { min-height: 125px; } .purchase-summary strong, .amount-summary strong { font-size: 29px; } .payment-method { padding-inline: 11px; gap: 9px; } .payment-name, .payment-tail { font-size: 13px; } }
</style>
