import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getNotices } from '../api/noticeApi'
import Pagination from '../components/common/Pagination'
import BasicLayout from '../layouts/BasicLayout'

// 공지사항 목록 (COM-05, 로그인 없이): 제목 검색 · 페이지 — 조건을 주소(?keyword=&page=)에 둬서 상세에서 돌아와도 유지된다
const NoticeListPage = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const page = Number(searchParams.get('page')) || 1
  const keyword = searchParams.get('keyword') ?? ''
  const [input, setInput] = useState(keyword)
  const [pageData, setPageData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getNotices({ page, ...(keyword && { keyword }) })
      .then(setPageData)
      .catch((err) => setError(err.response?.data?.message ?? '공지를 불러오지 못했어요.'))
  }, [page, keyword])

  const handleSearch = (e) => {
    e.preventDefault()
    setError('')
    setSearchParams(input.trim() ? { keyword: input.trim() } : {})
  }

  const movePage = (num) => setSearchParams({ ...(keyword && { keyword }), page: String(num) })

  return (
    <BasicLayout>
      <div className="mx-auto grid max-w-3xl gap-4">
        <h1 className="text-2xl font-bold">공지사항</h1>
        <form onSubmit={handleSearch} className="flex gap-2">
          <input
            aria-label="제목 검색" placeholder="제목 검색" value={input} onChange={(e) => setInput(e.target.value)}
            className="min-w-0 flex-1 rounded border border-gray-300 bg-white px-3 py-2 text-sm focus:border-emerald-600 focus:outline-none"
          />
          <button type="submit" className="rounded bg-emerald-700 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-800">
            검색
          </button>
        </form>

        {error && <p role="alert" className="rounded bg-red-50 p-3 text-sm text-red-800">{error}</p>}
        {!pageData && !error && <p className="text-sm text-gray-500">공지를 불러오는 중...</p>}

        {pageData && (
          <>
            <ul className="divide-y divide-gray-200 rounded-lg border border-gray-200 bg-white">
              {pageData.dtoList.length === 0 && (
                <li className="px-4 py-8 text-center text-sm text-gray-500">공지가 없어요.</li>
              )}
              {pageData.dtoList.map((notice) => (
                <li key={notice.id}>
                  <Link
                    to={`/notices/${notice.id}`}
                    className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1 px-4 py-3 hover:bg-gray-50"
                  >
                    <span className="font-medium">{notice.title}</span>
                    <span className="text-xs text-gray-500">{notice.createdAt?.slice(0, 10)} · 조회 {notice.viewCount}</span>
                  </Link>
                </li>
              ))}
            </ul>
            <Pagination pageData={pageData} onMove={movePage} />
          </>
        )}
      </div>
    </BasicLayout>
  )
}

export default NoticeListPage
