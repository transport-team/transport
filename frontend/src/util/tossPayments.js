const TOSS_SCRIPT_ID = 'toss-payments-sdk'
const TOSS_SCRIPT_URL = 'https://js.tosspayments.com/v2/standard'

const loadTossScript = () => new Promise((resolve, reject) => {
  if (window.TossPayments) {
    resolve()
    return
  }

  const existingScript = document.getElementById(TOSS_SCRIPT_ID)
  if (existingScript) {
    existingScript.addEventListener('load', resolve, { once: true })
    existingScript.addEventListener('error', () => reject(new Error('토스 결제 SDK를 불러오지 못했습니다.')), { once: true })
    return
  }

  const script = document.createElement('script')
  script.id = TOSS_SCRIPT_ID
  script.src = TOSS_SCRIPT_URL
  script.async = true
  script.onload = resolve
  script.onerror = () => reject(new Error('토스 결제 SDK를 불러오지 못했습니다.'))
  document.head.appendChild(script)
})

export const requestTossPayment = async ({ orderId, amount, cargoName }) => {
  const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY
  if (!clientKey) throw new Error('토스 클라이언트 키가 설정되지 않았습니다.')

  await loadTossScript()
  const tossPayments = window.TossPayments(clientKey)
  const payment = tossPayments.payment({ customerKey: `customer-${orderId}` })
  const baseUrl = window.location.origin

  await payment.requestPayment({
    method: 'CARD',
    amount: { currency: 'KRW', value: Number(amount) },
    orderId,
    orderName: cargoName || '운송 서비스',
    successUrl: `${baseUrl}/customer/payments/success`,
    failUrl: `${baseUrl}/customer/payments/fail`,
  })
}
