// 배송 상태(ShipmentStatus)의 화면 이름 · 배지 색 — 모든 화면이 이 표 하나를 쓴다 (이름 · 색을 바꿀 때는 여기만)
// 키는 서버 enum과 같아야 한다: 검색 필터 값으로도 보내서, 없는 값이면 400이다 (#126 DELIVERED)
// 상태 · 전이 규칙은 docs/status.md. 화면에서는 띄어 쓰고, 고객이 알기 쉽게 접수(REQUESTED)는 '배차 대기', 취소는 '취소됨'
export const SHIPMENT_STATUS = {
  PENDING_PAYMENT: { label: '결제 대기', badge: 'border-amber-200 bg-amber-50 text-amber-700' },
  REQUESTED: { label: '배차 대기', badge: 'border-emerald-200 bg-emerald-50 text-emerald-700' },
  ASSIGNED: { label: '배차 완료', badge: 'border-blue-200 bg-blue-50 text-blue-700' },
  PICKED_UP: { label: '상차 완료', badge: 'border-indigo-200 bg-indigo-50 text-indigo-700' },
  IN_TRANSIT: { label: '운송 중', badge: 'border-indigo-200 bg-indigo-50 text-indigo-700' },
  ARRIVED: { label: '도착', badge: 'border-indigo-200 bg-indigo-50 text-indigo-700' },
  COMPLETED: { label: '배송 완료', badge: 'border-gray-200 bg-gray-100 text-gray-700' },
  CANCELED: { label: '취소됨', badge: 'border-red-200 bg-red-50 text-red-700' },
}

const UNKNOWN_BADGE = 'border-gray-200 bg-gray-100 text-gray-700'

// 표에 없는 값은 받은 그대로 보여 준다 (화면이 깨지지 않게)
export const shipmentStatusLabel = (status) => SHIPMENT_STATUS[status]?.label ?? status

export const shipmentStatusBadge = (status) => SHIPMENT_STATUS[status]?.badge ?? UNKNOWN_BADGE
