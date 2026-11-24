import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { getNotice } from '../api/noticeApi'
import BasicLayout from '../layouts/BasicLayout'

// 공지 상세 (로그인 없이): 열 때마다 조회수 + 1
const NoticeDetailPage = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const [notice, setNotice] = useState(null)
  const [error, setError] = useState('')
  const loadedId = useRef(null)

  useEffect(() => {
    if (loadedId.current === id) return  // 개발 모드(StrictMode)는 effect를 두 번 실행 → 조회수가 두 번 오르지 않게
    loadedId.current = id
    getNotice(id)
      .then(setNotice)
      .catch((err) => setError(err.response?.data?.message ?? '공지를 불러오지 못했어요.'))
  }, [id])

  // 목록에서 왔으면 뒤로(페이지 · 검색 유지), 주소로 바로 들어왔으면 목록으로
  const goList = () => (location.key === 'default' ? navigate('/notices') : navigate(-1))

  return (
    <BasicLayout>
      <article className="mx-auto grid max-w-3xl gap-4">
        {error && <p role="alert" className="rounded bg-red-50 p-3 text-sm text-red-800">{error}</p>}
        {!notice && !error && <p className="text-sm text-gray-500">공지를 불러오는 중...</p>}
        {notice && (
          <>
            <header className="grid gap-1 border-b border-gray-200 pb-4">
              <h1 className="text-2xl font-bold">{notice.title}</h1>
              <p className="text-sm text-gray-500">
                {notice.writerName} · {notice.createdAt?.slice(0, 10)} · 조회 {notice.viewCount}
              </p>
            </header>
            <p className="whitespace-pre-line leading-7 text-gray-800">{notice.content}</p>
          </>
        )}
        <button
          type="button" onClick={goList}
          className="justify-self-start rounded border border-gray-300 bg-white px-4 py-2 text-sm hover:bg-gray-50"
        >
          목록으로
        </button>
      </article>
    </BasicLayout>
  )
}

export default NoticeDetailPage
