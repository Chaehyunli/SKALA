export const CREDIT_BALANCE_UPDATED = 'agentpass:credit-balance-updated'

export function publishCreditBalance(balance) {
  window.dispatchEvent(new CustomEvent(CREDIT_BALANCE_UPDATED, {
    detail: { balance: Number(balance || 0) }
  }))
}
