// 배송 상태 배지 (팀원 4 관리) — 이름 · 색은 util/shipmentStatusUtil.js 한 곳에서, 모양은 공통 Badge에서 정한다
// status: enum 문자열, size: sm(목록 · 이력) | md(상세 제목 옆 · 카드)
import Badge from './Badge'
import { shipmentStatusBadge, shipmentStatusLabel } from '../../util/shipmentStatusUtil'

const StatusBadge = ({ status, size = 'md' }) => (
  <Badge size={size} className={shipmentStatusBadge(status)}>
    <span className="sr-only">배송 상태: </span>
    {shipmentStatusLabel(status)}
  </Badge>
)

export default StatusBadge
