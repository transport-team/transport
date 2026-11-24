// 배송 상태를 한눈에 보여주는 상단 진행 단계 (기존 상세 이력 타임라인과 별개)
const STEPS = [
  { label: '접수', icon: '1' },
  { label: '결제 완료', icon: '2' },
  { label: '배차 완료', icon: '3' },  // 상태 배지 이름과 같게 (util/shipmentStatusUtil.js)
  { label: '상차 완료', icon: '4' },
  { label: '운송 중', icon: '5' },
  { label: '기사 배송 완료', icon: '6' },
  { label: '수령 확정', icon: '7' },
]

const PROGRESS = {
  PENDING_PAYMENT: 0,
  REQUESTED: 1,
  ASSIGNED: 2,
  PICKED_UP: 3,
  IN_TRANSIT: 4,
  ARRIVED: 4,
  DELIVERED: 5,
  COMPLETED: 6,
}

const ShipmentProgress = ({ status }) => {
  const isCanceled = status === 'CANCELED'
  const current = PROGRESS[status] ?? -1

  return (
    <div className="rounded-2xl border border-gray-100 bg-white px-4 py-5 shadow-sm" aria-label="배송 진행 상황">
      <div className="mb-4 flex items-center justify-between gap-3">
        <h2 className="text-sm font-bold text-gray-900">배송 진행 상황</h2>
        {isCanceled ? (
          <span className="rounded-full bg-red-50 px-2.5 py-1 text-xs font-semibold text-red-600">배송 취소됨</span>
        ) : status === 'ARRIVED' ? (
          <span className="text-xs font-semibold text-emerald-700">목적지 도착</span>
        ) : (
          <span className="text-xs text-gray-500">현재 단계 {current + 1}/{STEPS.length}</span>
        )}
      </div>
      <div className="overflow-x-auto pb-1">
        <ol className="flex min-w-[520px] items-start" aria-label="배송 진행 단계">
          {STEPS.map((step, index) => {
            const done = !isCanceled && current >= index
            const now = !isCanceled && current === index
            return (
              <li key={step.label} className="relative min-w-0 flex-1 text-center">
                {index > 0 && (
                  <span aria-hidden="true" className={`absolute top-4 right-1/2 h-0.5 w-full ${done ? 'bg-emerald-500' : 'bg-gray-200'}`} />
                )}
                <span className={`relative z-10 mx-auto flex h-8 w-8 items-center justify-center rounded-full border-2 text-xs font-bold transition-colors ${
                  done ? 'border-emerald-600 bg-emerald-600 text-white' : 'border-gray-200 bg-white text-gray-400'
                } ${now ? 'ring-4 ring-emerald-100' : ''}`} aria-current={now ? 'step' : undefined}>
                  {done && !now ? '✓' : step.icon}
                </span>
                <span className={`mt-2 block whitespace-nowrap text-[11px] font-semibold ${done ? 'text-emerald-700' : 'text-gray-400'}`}>
                  {step.label}
                </span>
              </li>
            )
          })}
        </ol>
      </div>
      {isCanceled && <p className="mt-3 text-center text-xs text-red-500">취소된 배송은 더 이상 진행되지 않습니다.</p>}
    </div>
  )
}

export default ShipmentProgress
