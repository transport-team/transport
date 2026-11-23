// 공통 배지 (팀원 4 관리) — 상태 배지 모양을 한 곳에서 정한다 (둥근 · 테두리 · 굵게)
// tone: gray | green | blue | indigo | amber | red, size: sm(목록 · 이력) | md(상세 제목 옆 · 카드)
// className: 색을 직접 줄 때 (tone 대신, 예: 배송 상태 표 util/shipmentStatusUtil.js의 색)
// 배송 상태는 StatusBadge를 쓰고, 회원 · 기사 · 문의 · 차량 신청 같은 다른 상태는 이 Badge에 tone을 골라 쓴다 (#143)
const BADGE_TONES = {
  gray: 'border-gray-200 bg-gray-100 text-gray-700',
  green: 'border-emerald-200 bg-emerald-50 text-emerald-700',
  blue: 'border-blue-200 bg-blue-50 text-blue-700',
  indigo: 'border-indigo-200 bg-indigo-50 text-indigo-700',
  amber: 'border-amber-200 bg-amber-50 text-amber-700',
  red: 'border-red-200 bg-red-50 text-red-700',
}

const SIZES = {
  sm: 'px-2.5 py-0.5 text-xs',
  md: 'px-3 py-1 text-xs',
}

const Badge = ({ tone = 'gray', size = 'md', className, children }) => (
  <span
    className={`inline-flex shrink-0 items-center whitespace-nowrap rounded-full border font-bold ${className ?? BADGE_TONES[tone] ?? BADGE_TONES.gray} ${SIZES[size] ?? SIZES.md}`}
  >
    {children}
  </span>
)

export default Badge
