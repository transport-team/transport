// 배송 상태 배지 (팀원 4 관리) — 이름 · 색은 util/shipmentStatusUtil.js 한 곳에서 정한다
// status: enum 문자열, size: sm(목록 · 이력) | md(상세 제목 옆 · 카드)
import { shipmentStatusBadge, shipmentStatusLabel } from '../../util/shipmentStatusUtil'

const SIZES = {
  sm: 'px-2.5 py-0.5 text-xs',
  md: 'px-3 py-1 text-xs',
}

const StatusBadge = ({ status, size = 'md' }) => (
  <span
    className={`inline-flex shrink-0 items-center whitespace-nowrap rounded-full border font-bold ${shipmentStatusBadge(status)} ${SIZES[size] ?? SIZES.md}`}
  >
    <span className="sr-only">배송 상태: </span>
    {shipmentStatusLabel(status)}
  </span>
)

export default StatusBadge
