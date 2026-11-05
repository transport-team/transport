// 페이지 번호 (공통 — 관리: 팀원 4): PageResponseDTO의 pageNumList · prev · next · current로 그린다
// onMove(page): 누른 페이지 번호로 다시 조회
const Pagination = ({ pageData, onMove }) => (
  <nav aria-label="페이지" className="flex flex-wrap items-center gap-1 text-sm">
    {pageData.prev && (
      <button type="button" onClick={() => onMove(pageData.prevPage)} className="rounded px-3 py-1 border border-border bg-surface text-text hover:bg-gray-light transition-colors">이전</button>
    )}
    {pageData.pageNumList.map((num) => (
      <button
        key={num} type="button" onClick={() => onMove(num)} aria-current={num === pageData.current ? 'page' : undefined}
        className={`rounded px-3 py-1 border transition-colors
          ${num === pageData.current
            ? 'bg-primary border-primary text-white hover:bg-primary-hover'
            : 'bg-surface border-border text-text hover:bg-gray-light'
          }`}
      >
        {num}
      </button>
    ))}
    {pageData.next && (
      <button type="button" onClick={() => onMove(pageData.nextPage)} className="rounded px-3 py-1 border border-border bg-surface text-text hover:bg-gray-light transition-colors">다음</button>
    )}
  </nav>
)

export default Pagination
