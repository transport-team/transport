// 목록 검색 조건(상태 · 기간 · 검색어 · 쪽)을 주소에 남길 때 쓰는 도우미 (11-25) — 관리자 배송 검색 · 결제 내역
// 쓰는 법: const query = readSearchQuery(new URLSearchParams(queryString), STATUS_VALUES)
//          setSearchParams(toSearchParams({ ...query, page: 2 }))

// 있는 날짜(YYYY-MM-DD)인지 — 2026-02-30 · 2026-13-01 같은 값은 아니다
export const isDateParam = (value) => /^\d{4}-\d{2}-\d{2}$/.test(value)
  && !Number.isNaN(Date.parse(value)) && new Date(value).toISOString().slice(0, 10) === value

// 주소 → 조건. 허용하지 않는 상태 · 없는 날짜 · page=abc는 기본값(전체 · 빈 값 · 1쪽)
export const readSearchQuery = (params, allowedStatuses) => {
  const status = params.get('status') ?? ''
  const from = params.get('from') ?? ''
  const to = params.get('to') ?? ''
  const page = Math.floor(Number(params.get('page')))
  return {
    status: allowedStatuses.includes(status) ? status : '',
    from: isDateParam(from) ? from : '',
    to: isDateParam(to) ? to : '',
    keyword: (params.get('keyword') ?? '').trim(),
    page: page >= 1 ? page : 1,
  }
}

// 조건 → 주소. 빈 조건과 1쪽은 뺀다
export const toSearchParams = ({ status, from, to, keyword, page }) => ({
  ...(status && { status }),
  ...(from && { from }),
  ...(to && { to }),
  ...(keyword?.trim() && { keyword: keyword.trim() }),
  ...(page > 1 && { page: String(page) }),
})
