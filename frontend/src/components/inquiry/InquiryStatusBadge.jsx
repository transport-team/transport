// 1:1 문의 상태 배지 (서버 InquiryStatus: WAITING · ANSWERED)
const STATUS = {
  WAITING: { label: '답변 대기', className: 'bg-amber-50 text-amber-800' },
  ANSWERED: { label: '답변 완료', className: 'bg-emerald-50 text-emerald-800' },
}

const InquiryStatusBadge = ({ status }) => {
  const { label, className } = STATUS[status] ?? { label: status, className: 'bg-gray-100 text-gray-600' }
  return <span className={`shrink-0 rounded px-2 py-0.5 text-xs font-medium ${className}`}>{label}</span>
}

export default InquiryStatusBadge
