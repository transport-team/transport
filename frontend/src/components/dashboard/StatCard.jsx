import { Link } from 'react-router-dom'

// 대시보드 숫자 카드: 이름 · 값 · 보조 설명. to가 있으면 카드를 눌러 그 화면으로 간다
// value: 숫자는 천 단위 쉼표, 글자('4.6' · '-')는 그대로
const StatCard = ({ label, value, unit = '', note, to }) => {
  const body = (
    <>
      <p className="text-sm text-text-secondary">{label}</p>
      <p className="mt-2 break-words text-2xl font-extrabold tracking-tight text-slate-900 sm:text-3xl">
        {typeof value === 'number' ? value.toLocaleString('ko-KR') : value}
        {unit && <span className="ml-0.5 text-base font-medium text-text-secondary">{unit}</span>}
      </p>
      {note && <p className="mt-1 text-xs text-gray-neutral">{note}</p>}
    </>
  )
  const className = 'block min-w-0 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5'

  if (!to) {
    return <div className={className}>{body}</div>
  }
  return (
    <Link to={to} className={`${className} transition duration-200 hover:-translate-y-0.5 hover:border-blue-200 hover:shadow-md focus-visible:outline-2 focus-visible:outline-primary`}>
      {body}
    </Link>
  )
}

export default StatCard
