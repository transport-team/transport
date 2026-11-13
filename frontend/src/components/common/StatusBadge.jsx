
// 상태 배지 (팀원 4 관리) — status.md 8개 상태 매핑
// status: enum 문자열, size: sm | md
const STATUS_CONFIG = {
  PENDING_PAYMENT: { label: '결제대기', className: 'bg-warning-light text-warning-text border-amber-200' },
  REQUESTED: { label: '접수', className: 'bg-info-light text-info-text border-blue-200' },
  ASSIGNED: { label: '배차완료', className: 'bg-primary-light text-primary-text border-green-200' },
  PICKED_UP: { label: '상차완료', className: 'bg-primary-light text-primary-text border-green-200' },
  IN_TRANSIT: { label: '운송중', className: 'bg-info-light text-info-text border-blue-200' },
  ARRIVED: { label: '도착', className: 'bg-warning-light text-warning-text border-amber-200' },
  COMPLETED: { label: '배송완료', className: 'bg-success-light text-success-text border-green-200' },
  CANCELED: { label: '취소', className: 'bg-danger-light text-danger-text border-red-200' },
}

const StatusBadge = ({ status, size = 'md' }) => {
  const config = STATUS_CONFIG[status] || { label: status, className: 'bg-gray-light text-gray-text border-gray-200' }

  const sizes = {
    sm: 'px-2 py-0.5 text-xs',
    md: 'px-2.5 py-0.5 text-sm',
  }

  return (
    <span
      className={`inline-flex items-center rounded-full border font-medium ${config.className} ${sizes[size]}`}
      aria-label={`배송 상태: ${config.label}`}
    >
      {config.label}
    </span>
  )
}

export default StatusBadge
