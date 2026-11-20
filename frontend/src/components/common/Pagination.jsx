// 페이지 번호 (공통 — 관리: 팀원 4): PageResponseDTO 또는 page · totalPages 값을 받아 그린다
// onMove(page): 누른 페이지 번호로 다시 조회
const Pagination = ({ pageData, page, totalPages, onMove }) => {
  const current = Number(pageData?.current ?? pageData?.pageRequestDTO?.page ?? page ?? 1)
  const total = Number(pageData?.totalPage ?? totalPages ?? 0)
  if (total < 1) return null

  const start = Math.max(1, Math.min(current - 2, total - 4))
  const pageNumbers = Array.from({ length: Math.min(total, 5) }, (_, i) => start + i)
  const buttonClass = 'inline-flex h-9 min-w-9 items-center justify-center rounded-lg border px-3 text-xs font-semibold transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600 disabled:cursor-not-allowed disabled:opacity-40'
  const normalClass = `${buttonClass} border-gray-200 bg-white text-gray-600 hover:bg-gray-50`

  return (
    <nav aria-label="페이지 이동" className="flex flex-wrap items-center justify-center gap-1.5 pt-4">
      <button type="button" onClick={() => onMove(current - 1)} disabled={current <= 1} className={normalClass}>
        이전
      </button>
      {pageNumbers.map((num) => (
        <button
          key={num}
          type="button"
          onClick={() => onMove(num)}
          aria-current={num === current ? 'page' : undefined}
          className={num === current
            ? `${buttonClass} border-emerald-600 bg-emerald-600 text-white`
            : normalClass}
        >
          {num}
        </button>
      ))}
      <button type="button" onClick={() => onMove(current + 1)} disabled={current >= total} className={normalClass}>
        다음
      </button>
    </nav>
  )
}

export default Pagination
