// 페이지 번호 (공통 — 관리: 팀원 4): PageResponseDTO의 pageNumList · prev · next · current로 그린다
// onMove(page): 누른 페이지 번호로 다시 조회
const Pagination = ({ pageData, onMove }) => (
  <nav aria-label="페이지" className="flex flex-wrap items-center gap-1 text-sm">
    {pageData.prev && (
      <button type="button" onClick={() => onMove(pageData.prevPage)} className="rounded px-3 py-1 hover:bg-gray-100">이전</button>
    )}
    {pageData.pageNumList.map((num) => (
      <button
        key={num} type="button" onClick={() => onMove(num)} aria-current={num === pageData.current ? 'page' : undefined}
        className={`rounded px-3 py-1 ${num === pageData.current ? 'bg-emerald-700 text-white' : 'hover:bg-gray-100'}`}
      >
        {num}
      </button>
    ))}
    {pageData.next && (
      <button type="button" onClick={() => onMove(pageData.nextPage)} className="rounded px-3 py-1 hover:bg-gray-100">다음</button>
    )}
  </nav>
)

export default Pagination
