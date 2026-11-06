import { useEffect, useState } from 'react'
import { getMyInquiries, postInquiry } from '../api/inquiryApi'
import Pagination from '../components/common/Pagination'
import InquiryStatusBadge from '../components/inquiry/InquiryStatusBadge'

const INPUT_CLASS = 'rounded border border-gray-300 bg-white px-3 py-2 text-sm focus:border-emerald-600 focus:outline-none'

const initForm = { title: '', content: '' }

// 1:1 문의 (고객 · 기사 공통, /{역할}/inquiries — COM-07): 새 문의 + 내 문의 목록 (누르면 내용 · 답변 펼침)
// 작성한 문의는 고치거나 지울 수 없다 (답변 확인까지만)
const InquiryPage = () => {
  const [page, setPage] = useState(1)
  const [reloadKey, setReloadKey] = useState(0)  // 문의를 남긴 뒤 목록을 다시 불러온다
  const [pageData, setPageData] = useState(null)
  const [listError, setListError] = useState('')
  const [form, setForm] = useState(initForm)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    getMyInquiries({ page })
      .then(setPageData)
      .catch((err) => setListError(err.response?.data?.message ?? '내 문의를 불러오지 못했어요.'))
  }, [page, reloadKey])

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError('')
    setMessage('')
    try {
      await postInquiry({ title: form.title.trim(), content: form.content })
      setMessage('문의를 남겼어요. 답변이 등록되면 아래 내 문의에서 볼 수 있어요.')
      setForm(initForm)
      setPage(1)  // 새 문의는 첫 페이지 맨 위에 보인다
      setReloadKey((key) => key + 1)
    } catch (err) {
      setError(err.response?.data?.message ?? '문의를 남기지 못했어요.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="grid max-w-3xl gap-6">
      <h1 className="text-xl font-bold">1:1 문의</h1>

      <form onSubmit={handleSubmit} className="grid gap-3 rounded-lg border border-gray-200 bg-white p-4">
        <h2 className="font-semibold">새 문의</h2>
        <label className="grid gap-1 text-sm">
          <span className="font-medium">제목</span>
          <input name="title" required maxLength={100} value={form.title} onChange={handleChange} className={INPUT_CLASS} />
        </label>
        <label className="grid gap-1 text-sm">
          <span className="font-medium">내용</span>
          <textarea
            name="content" required maxLength={5000} rows={5} value={form.content} onChange={handleChange}
            placeholder="배송 번호나 날짜를 함께 적어 주면 더 빨리 답할 수 있어요."
            className={INPUT_CLASS}
          />
        </label>
        {error && <p role="alert" className="rounded bg-red-50 p-3 text-sm text-red-800">{error}</p>}
        {message && <p role="status" className="rounded bg-emerald-50 p-3 text-sm text-emerald-800">{message}</p>}
        <div>
          <button
            type="submit" disabled={submitting}
            className="rounded bg-emerald-700 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-800 disabled:opacity-60"
          >
            {submitting ? '보내는 중...' : '문의 남기기'}
          </button>
        </div>
      </form>

      <div className="grid gap-3">
        <h2 className="font-semibold">내 문의</h2>
        {listError && <p role="alert" className="rounded bg-red-50 p-3 text-sm text-red-800">{listError}</p>}
        {!pageData && !listError && <p className="text-sm text-gray-500">내 문의를 불러오는 중...</p>}
        {pageData && (
          <>
            <ul className="divide-y divide-gray-200 rounded-lg border border-gray-200 bg-white">
              {pageData.dtoList.length === 0 && (
                <li className="px-4 py-8 text-center text-sm text-gray-500">아직 남긴 문의가 없어요.</li>
              )}
              {pageData.dtoList.map((inquiry) => (
                <li key={inquiry.id}>
                  <details className="group">
                    <summary className="flex cursor-pointer list-none items-center gap-3 px-4 py-3 hover:bg-gray-50 [&::-webkit-details-marker]:hidden">
                      <InquiryStatusBadge status={inquiry.status} />
                      <span className="min-w-0 flex-1 truncate font-medium">{inquiry.title}</span>
                      <span className="shrink-0 text-xs text-gray-500">{inquiry.createdAt?.slice(0, 10)}</span>
                      <span aria-hidden="true" className="text-gray-400 transition-transform group-open:rotate-180">▾</span>
                    </summary>
                    <div className="grid gap-3 bg-gray-50 px-4 py-3 text-sm">
                      <p className="whitespace-pre-line leading-7 text-gray-800">{inquiry.content}</p>
                      {inquiry.answer ? (
                        <div className="rounded border border-emerald-200 bg-white p-3">
                          <p className="mb-1 text-xs font-semibold text-emerald-800">답변 · {inquiry.answeredAt?.slice(0, 10)}</p>
                          <p className="whitespace-pre-line leading-7 text-gray-800">{inquiry.answer}</p>
                        </div>
                      ) : (
                        <p className="text-xs text-gray-500">아직 답변을 기다리는 중이에요.</p>
                      )}
                    </div>
                  </details>
                </li>
              ))}
            </ul>
            <Pagination pageData={pageData} onMove={setPage} />
          </>
        )}
      </div>
    </section>
  )
}

export default InquiryPage
