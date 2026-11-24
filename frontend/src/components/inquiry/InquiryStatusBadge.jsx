// 1:1 문의 상태 배지 (서버 InquiryStatus: WAITING · ANSWERED) — 모양은 공통 Badge
import Badge from '../common/Badge'

const STATUS = {
  WAITING: { label: '답변 대기', tone: 'amber' },
  ANSWERED: { label: '답변 완료', tone: 'green' },
}

const InquiryStatusBadge = ({ status }) => {
  const { label, tone } = STATUS[status] ?? { label: status, tone: 'gray' }
  return <Badge tone={tone} size="sm">{label}</Badge>
}

export default InquiryStatusBadge
