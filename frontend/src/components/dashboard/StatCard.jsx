import { Link } from 'react-router-dom'

// 대시보드 숫자 카드: 이름 · 값 · 보조 설명. to가 있으면 카드를 눌러 그 화면으로 간다
const StatCard = ({ label, value, unit = '', note, to }) => {
  const body = (
    <>
      <p className="text-sm text-text-secondary">{label}</p>
      <p className="mt-1 text-2xl font-semibold text-text">
        {value.toLocaleString('ko-KR')}
        {unit && <span className="ml-0.5 text-base font-medium text-text-secondary">{unit}</span>}
      </p>
      {note && <p className="mt-1 text-xs text-gray-neutral">{note}</p>}
    </>
  )
  const className = 'block rounded-lg border border-border bg-surface p-4'

  if (!to) {
    return <div className={className}>{body}</div>
  }
  return (
    <Link to={to} className={`${className} transition-colors hover:border-primary focus-visible:outline-2 focus-visible:outline-primary`}>
      {body}
    </Link>
  )
}

export default StatCard
