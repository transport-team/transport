import { StatusBadge, Loading, Button } from '../common'

// 배송 상태 변경 타임라인 (팀원 4 — 4-5)
// 배송 상세(팀원 3 소유)에서 import해서 사용
// props:
//   history: GET /api/shipments/{id}/history 응답 배열
//            [{ id, fromStatus, toStatus, changedByName, memo, changedAt }]
//   loading: 로딩 중 여부
//   error: 에러 객체 (있으면 에러 안내 표시)
//   onRetry: 에러 시 다시 호출할 콜백 (error가 있는데 없으면 버튼 숨김)

// 시각 표시: 2026. 11. 9. 14:30
const formatDateTime = (isoString) => {
  if (!isoString) return ''
  const d = new Date(isoString)
  if (Number.isNaN(d.getTime())) return isoString
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}. ${d.getMonth() + 1}. ${d.getDate()}. ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const Timeline = ({ history = [], loading = false, error = null, onRetry }) => {
  // 로딩 중
  if (loading) {
    return (
      <div className="py-8">
        <Loading text="이력을 불러오는 중..." />
      </div>
    )
  }

  // 에러
  if (error) {
    return (
      <div
        className="rounded-lg border border-danger bg-danger-light p-4 text-sm text-danger-text"
        role="alert"
      >
        <p>이력을 불러오지 못했어요. {error.message && `(${error.message})`}</p>
        {onRetry && (
          <div className="mt-3">
            <Button size="sm" variant="outline" onClick={onRetry}>
              다시 시도
            </Button>
          </div>
        )}
      </div>
    )
  }

  // 빈 목록
  if (!history.length) {
    return (
      <p className="py-8 text-center text-sm text-text-secondary" aria-live="polite">
        아직 상태 변경 이력이 없습니다.
      </p>
    )
  }

  return (
    <ol className="relative space-y-6 border-l-2 border-dashed border-gray-neutral pl-6" aria-label="배송 상태 변경 이력">
      {history.map((item) => (
        <li key={item.id} className="relative">
          {/* 타임라인 마커 */}
          <span
            className="absolute -left-[31px] top-1 flex h-4 w-4 items-center justify-center rounded-full border-2 border-primary bg-white"
            aria-hidden="true"
          >
            <span className="h-1.5 w-1.5 rounded-full bg-primary" />
          </span>

          <div className="flex flex-wrap items-center gap-2">
            <StatusBadge status={item.toStatus} size="sm" />
            <time className="text-xs text-text-secondary" dateTime={item.changedAt}>
              {formatDateTime(item.changedAt)}
            </time>
          </div>

          <p className="mt-1.5 text-sm text-text-primary">
            {item.fromStatus ? `${statusLabel(item.fromStatus)} →` : '신청'}
            <span className="sr-only"> </span>
            {item.changedByName ? ` · ${item.changedByName}` : ' · 시스템'}
          </p>

          {item.memo && <p className="mt-1 text-sm text-text-secondary">{item.memo}</p>}
        </li>
      ))}
    </ol>
  )
}

// fromStatus 한글 표시용 (StatusBadge의 매핑과 동일)
const statusLabel = (status) => {
  const labels = {
    PENDING_PAYMENT: '결제대기',
    REQUESTED: '접수',
    ASSIGNED: '배차완료',
    PICKED_UP: '상차완료',
    IN_TRANSIT: '운송중',
    ARRIVED: '도착',
    COMPLETED: '배송완료',
    CANCELED: '취소',
  }
  return labels[status] || status
}

export default Timeline
