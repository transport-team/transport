import { useEffect, useState } from 'react'
import { deleteNotice, getNotices, postNotice, putNotice } from '../../api/noticeApi'
import Pagination from '../../components/common/Pagination'

const INPUT_CLASS = 'rounded border border-gray-300 bg-white px-3 py-2 text-sm focus:border-emerald-600 focus:outline-none'

const initForm = { title: '', content: '' }

// 관리자 공지 관리 (ADM-11): 작성 · 수정 · 삭제 — 목록은 공개 목록 API를 같이 쓴다 (내용까지 와서 바로 수정 폼에 넣는다)
const NoticeManagePage = () => {
  const [page, setPage] = useState(1)
  const [reloadKey, setReloadKey] = useState(0)     // 저장 · 삭제 뒤 목록을 다시 불러온다
  const [pageData, setPageData] = useState(null)
  const [form, setForm] = useState(initForm)
  const [editingId, setEditingId] = useState(null)  // 수정 중인 공지 (없으면 새 공지)
  const [confirmId, setConfirmId] = useState(null)  // 삭제를 확인 중인 공지
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    getNotices({ page })
      .then(setPageData)
      .catch((err) => setError(err.response?.data?.message ?? '공지를 불러오지 못했어요.'))
  }, [page, reloadKey])

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const startEdit = (notice) => {
    setEditingId(notice.id)
    setForm({ title: notice.title, content: notice.content })
    setError('')
    setMessage('')
  }

  const cancelEdit = () => {
    setEditingId(null)
    setForm(initForm)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError('')
    setMessage('')
    try {
      if (editingId) {
        await putNotice(editingId, form)
        setMessage('공지를 수정했어요.')
      } else {
        await postNotice(form)
        setMessage('공지를 올렸어요.')
        setPage(1)  // 새 공지는 첫 페이지 맨 위에 보인다
      }
      cancelEdit()
      setReloadKey((key) => key + 1)
    } catch (err) {
      setError(err.response?.data?.message ?? '공지를 저장하지 못했어요.')
    } finally {
      setSubmitting(false)
    }
  }

  const remove = async (noticeId) => {
    setError('')
    setMessage('')
    try {
      await deleteNotice(noticeId)
      setMessage('공지를 삭제했어요.')
      if (editingId === noticeId) {
        cancelEdit()
      }
      setReloadKey((key) => key + 1)
    } catch (err) {
      setError(err.response?.data?.message ?? '공지를 삭제하지 못했어요.')
    } finally {
      setConfirmId(null)
    }
  }

  // 관리 칸: 수정 · 삭제 (삭제는 같은 줄에서 한 번 더 확인)
  const renderAction = (notice) => {
    if (confirmId === notice.id) {
      return (
        <span className="inline-flex items-center gap-2">
          <span className="text-xs text-gray-600">삭제할까요?</span>
          <button
            type="button" onClick={() => remove(notice.id)}
            className="rounded bg-red-600 px-2 py-1 text-xs font-medium text-white hover:bg-red-700"
          >
            삭제
          </button>
          <button type="button" onClick={() => setConfirmId(null)} className="rounded border border-gray-300 px-2 py-1 text-xs hover:bg-gray-50">
            취소
          </button>
        </span>
      )
    }
    return (
      <span className="inline-flex gap-2">
        <button type="button" onClick={() => startEdit(notice)} className="rounded border border-gray-300 px-2 py-1 text-xs hover:bg-gray-50">
          수정
        </button>
        <button
          type="button" onClick={() => setConfirmId(notice.id)}
          className="rounded border border-red-300 px-2 py-1 text-xs text-red-700 hover:bg-red-50"
        >
          삭제
        </button>
      </span>
    )
  }

  return (
    <section className="grid gap-6">
      <h1 className="text-xl font-bold">공지 관리</h1>

      <form onSubmit={handleSubmit} className="grid gap-3 rounded-lg border border-gray-200 bg-white p-4">
        <h2 className="font-semibold">{editingId ? '공지 수정' : '새 공지'}</h2>
        <label className="grid gap-1 text-sm">
          <span className="font-medium">제목</span>
          <input name="title" required maxLength={100} value={form.title} onChange={handleChange} className={INPUT_CLASS} />
        </label>
        <label className="grid gap-1 text-sm">
          <span className="font-medium">내용</span>
          <textarea
            name="content" required maxLength={5000} rows={6} value={form.content} onChange={handleChange}
            className={INPUT_CLASS}
          />
        </label>
        {error && <p role="alert" className="rounded bg-red-50 p-3 text-sm text-red-800">{error}</p>}
        {message && <p role="status" className="rounded bg-emerald-50 p-3 text-sm text-emerald-800">{message}</p>}
        <div className="flex gap-2">
          <button
            type="submit" disabled={submitting}
            className="rounded bg-emerald-700 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-800 disabled:opacity-60"
          >
            {submitting ? '저장 중...' : editingId ? '수정 저장' : '공지 올리기'}
          </button>
          {editingId && (
            <button type="button" onClick={cancelEdit} className="rounded border border-gray-300 px-4 py-2 text-sm hover:bg-gray-50">
              취소
            </button>
          )}
        </div>
      </form>

      {!pageData && !error && <p className="text-sm text-gray-500">공지를 불러오는 중...</p>}
      {pageData && (
        <div className="grid gap-3">
          <p className="text-sm text-gray-600">전체 {pageData.totalCount}개</p>
          <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white">
            <table className="w-full whitespace-nowrap text-left text-sm">
              <thead className="border-b border-gray-200 bg-gray-50 text-gray-600">
                <tr>
                  <th className="px-4 py-3 font-medium">제목</th>
                  <th className="px-4 py-3 font-medium">작성일</th>
                  <th className="px-4 py-3 font-medium">조회</th>
                  <th className="px-4 py-3 text-right font-medium">관리</th>
                </tr>
              </thead>
              <tbody>
                {pageData.dtoList.length === 0 && (
                  <tr>
                    <td colSpan={4} className="px-4 py-8 text-center text-gray-500">공지가 없어요.</td>
                  </tr>
                )}
                {pageData.dtoList.map((notice) => (
                  <tr key={notice.id} className={`border-b border-gray-100 last:border-0 ${editingId === notice.id ? 'bg-emerald-50' : ''}`}>
                    <td className="max-w-xs truncate px-4 py-3">{notice.title}</td>
                    <td className="px-4 py-3">{notice.createdAt?.slice(0, 10)}</td>
                    <td className="px-4 py-3">{notice.viewCount}</td>
                    <td className="px-4 py-3 text-right">{renderAction(notice)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination pageData={pageData} onMove={setPage} />
        </div>
      )}
    </section>
  )
}

export default NoticeManagePage
