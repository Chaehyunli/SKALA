// payment-service 백엔드가 준비되기 전까지 쓰는 인메모리 목데이터.
// Notion "Payment 객체 변경" 스펙이 아직 비어있어 정식 계약이 없다 — 엔드포인트/필드명은 잠정치.
// 스펙이 나오면 이 파일과 api/payment.js의 mock 분기만 걷어내면 된다.

function delay(data) {
  return new Promise((resolve) => setTimeout(() => resolve(data), 300))
}

let balance = 10000
const CREDIT_PER_ANALYSIS = 120

export function mockGetBalance() {
  return delay({ balance, creditPerAnalysis: CREDIT_PER_ANALYSIS })
}

export function mockCharge({ credits, amount }) {
  balance += credits
  return delay({
    transactionId: `TXN-${Date.now()}`,
    credits,
    amount,
    balance,
    status: 'COMPLETED'
  })
}
