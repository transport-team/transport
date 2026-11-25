// 결제 상태 이름 · 배지 색 — 서버 PaymentStatus(READY · PAID · CANCELED · EXPIRED)와 같은 값 (11-25, #143)
// 관리자 결제 내역 · 관리자 배송 상세가 같이 쓴다 (배송 상태 이름표 util/shipmentStatusUtil.js와 같은 방식)
// 전에는 결제 내역 필터가 서버에 없는 PENDING · FAILED를 보내 400이 났고, READY · EXPIRED는 영어 그대로 보였다
export const PAYMENT_STATUS = {
  READY: { label: '결제 대기', badge: 'bg-amber-50 text-amber-700 border-amber-200' },
  PAID: { label: '결제 완료', badge: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  CANCELED: { label: '결제 취소 · 환불', badge: 'bg-red-50 text-red-600 border-red-200' },
  EXPIRED: { label: '결제 만료(30분)', badge: 'bg-gray-100 text-gray-600 border-gray-200' },
}

export const PAYMENT_STATUS_VALUES = Object.keys(PAYMENT_STATUS)

export const paymentStatusLabel = (status) => PAYMENT_STATUS[status]?.label ?? (status || '-')

export const paymentStatusBadge = (status) => PAYMENT_STATUS[status]?.badge ?? 'bg-gray-100 text-gray-600 border-gray-200'
