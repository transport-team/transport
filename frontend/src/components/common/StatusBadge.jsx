// 상태 배지 (팀원 4 관리) — status.md 8개 상태 매핑
// status: enum 문자열, size: sm | md
const STATUS_CONFIG = {
  PENDING_PAYMENT: { label: '결제대기', className: 'bg-warning-light text-warning-text border-warning' },
  REQUESTED: { label: '접수', className: 'bg-info-light text-info-text border-info' },
  ASSIGNED: { label: '배차완료', className: 'bg-primary-light text-primary-text border-primary' },
  PICKED_UP: { label: '상차완료', className: 'bg-primary-light text-primary-text border-primary' },
  IN_TRANSIT: { label: '운송중', className: 'bg-info-light text-info-text border-info' },
  ARRIVED: { label: '도착', className: 'bg-warning-light text-warning-text border-warning' },
  COMPLETED: { label: '배송완료', className: 'bg-success-light text-success-text border-success' },
  CANCELED: { label: '취소', className: 'bg-danger-light text-danger-text border-danger' },
}

const StatusBadge = ({ status, size = 'md' }) => {
  const config = STATUS_CONFIG[status] || { label: status, className: 'bg-gray-light text-gray-text border-gray-neutral' }

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