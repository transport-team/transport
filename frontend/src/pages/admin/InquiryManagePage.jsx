import { useEffect, useState } from 'react'
import { answerInquiry, deleteInquiry, getInquiries } from '../../api/inquiryApi'
import Pagination from '../../components/common/Pagination'
import InquiryStatusBadge from '../../components/inquiry/InquiryStatusBadge'
import { ROLE_NAME } from '../../util/roleUtil'

const INPUT_CLASS = 'rounded border border-gray-300 bg-white px-3 py-2 text-sm focus:border-emerald-600 focus:outline-none'

const TABS = [
  { value: 'WAITING', label: '답변 대기' },
  { value: 'ANSWERED', label: '답변 완료' },
  { value: '', label: '전체' },
]

// 관리자 1:1 문의 (ADM-11): 상태 탭(답변 대기가 기본) · 제목을 누르면 내용 + 답변 입력/수정 · 삭제(같은 줄에서 확인)
// 첫 답변이면 서버가 작성자에게 알림을 보낸다 (답변을 고칠 때는 알림 없음)
const InquiryManagePage = () => {
  const [status, setStatus] = useState('WAITING')
  const [page, setPage] = useState(1)
  const [reloadKey, setReloadKey] = useState(0)    // 답변 · 삭제 뒤 목록을 다시 불러온다
  const [pageData, setPageData] = useState(null)
  const [openId, setOpenId] = useState(null)      // 내용을 펼친 문의
  const [answer, setAnswer] = useState('')        // 펼친 문의의 답변 입력값
  const [confirmId, setConfirmId] = useState(null)  // 삭제를 확인 중인 문의
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    getInquiries({ page, ...(status && { status }) })
      .then(setPageData)
      .catch((err) => setError(err.response?.data?.message ?? '문의를 불러오지 못했어요.'))
  }, [status, page, reloadKey])

  const clearNotice = () => {
    setError('')
    setMessage('')
  }

  const selectTab = (value) => {
    setStatus(value)
    setPage(1)
    setOpenId(null)
    clearNotice()
  }

  const toggle = (inquiry) => {
    clearNotice()
    if (openId === inquiry.id) {
      setOpenId(null)
      return
    }
    setOpenId(inquiry.id)
    setAnswer(inquiry.answer ?? '')
  }

  const saveAnswer = async (inquiry) => {
    setSaving(true)
    clearNotice()
    try {
      await answerInquiry(inquiry.id, answer)
      setMessage(inquiry.status === 'WAITING'
        ? '답변을 등록했어요. 작성자에게 알림이 가고, 답변 완료 탭으로 옮겨졌어요.'
        : '답변을 고쳤어요.')
      setOpenId(null)
      setReloadKey((key) => key + 1)
    } catch (err) {
      setError(err.response?.data?.message ?? '답변을 저장하지 못했어요.')
    } finally {
      setSaving(false)
    }
  }

  const remove = async (inquiry) => {
    clearNotice()
    try {
      await deleteInquiry(inquiry.id)
      setMessage('문의를 삭제했어요.')
      if (openId === inquiry.id) {
        setOpenId(null)
      }
      setReloadKey((key) => key + 1)
    } catch (err) {
      setError(err.response?.data?.message ?? '문의를 삭제하지 못했어요.')
    } finally {
      setConfirmId(null)
    }
  }

  // 삭제는 같은 줄에서 한 번 더 확인 (스팸 정리용)
  const renderDelete = (inquiry) => {
    if (confirmId === inquiry.id) {
      return (
        <span className="inline-flex items-center gap-2">
          <span className="text-xs text-gray-600">삭제할까요?</span>
          <button
            type="button" onClick={() => remove(inquiry)}
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
      <button
        type="button" onClick={() => setConfirmId(inquiry.id)}
        className="rounded border border-red-300 px-2 py-1 text-xs text-red-700 hover:bg-red-50"
      >
        삭제
      </button>
    )
  }

  return (
    <section className="grid gap-6">
      <h1 className="text-xl font-bold">1:1 문의</h1>

      <div role="group" aria-label="답변 상태" className="flex flex-wrap gap-2">
        {TABS.map((tab) => (
          <button
            key={tab.label} type="button" aria-pressed={status === tab.value} onClick={() => selectTab(tab.value)}
            className={`rounded-full px-4 py-1.5 text-sm ${
              status === tab.value ? 'bg-emerald-700 font-medium text-white' : 'border border-gray-300 bg-white text-gray-700 hover:bg-gray-50'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {error && <p role="alert" className="rounded bg-red-50 p-3 text-sm text-red-800">{error}</p>}
      {message && <p role="status" className="rounded bg-emerald-50 p-3 text-sm text-emerald-800">{message}</p>}
      {!pageData && !error && <p className="text-sm text-gray-500">문의를 불러오는 중...</p>}

      {pageData && (
        <div className="grid gap-3">
          <p className="text-sm text-gray-600">{pageData.totalCount}개</p>
          <ul className="divide-y divide-gray-200 rounded-lg border border-gray-200 bg-white">
            {pageData.dtoList.length === 0 && (
              <li className="px-4 py-8 text-center text-sm text-gray-500">문의가 없어요.</li>
            )}
            {pageData.dtoList.map((inquiry) => (
              <li key={inquiry.id} className={openId === inquiry.id ? 'bg-emerald-50/40' : ''}>
                <div className="flex flex-wrap items-center gap-x-3 gap-y-1 px-4 py-3">
                  <InquiryStatusBadge status={inquiry.status} />
                  <button
                    type="button" aria-expanded={openId === inquiry.id} onClick={() => toggle(inquiry)}
                    className="min-w-0 flex-1 truncate text-left text-sm font-medium hover:text-emerald-800"
                  >
                    {inquiry.title}
                  </button>
                  <span className="text-xs text-gray-500">
                    {inquiry.memberName} · {ROLE_NAME[inquiry.memberRole]} · {inquiry.createdAt?.slice(0, 10)}
                  </span>
                  {renderDelete(inquiry)}
                </div>
                {openId === inquiry.id && (
                  <div className="grid gap-3 border-t border-gray-100 bg-gray-50 px-4 py-3 text-sm">
                    <p className="whitespace-pre-line leading-7 text-gray-800">{inquiry.content}</p>
                    <p className="text-xs text-gray-500">작성자 이메일: {inquiry.memberEmail}</p>
                    <label className="grid gap-1">
                      <span className="font-medium">
                        {inquiry.answer ? `답변 수정 (마지막 저장 ${inquiry.answeredAt?.slice(0, 10)})` : '답변'}
                      </span>
                      <textarea
                        rows={5} maxLength={5000} value={answer} onChange={(e) => setAnswer(e.target.value)}
                        className={INPUT_CLASS}
                      />
                    </label>
                    <div className="flex gap-2">
                      <button
                        type="button" onClick={() => saveAnswer(inquiry)} disabled={saving || !answer.trim()}
                        className="rounded bg-emerald-700 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-800 disabled:opacity-60"
                      >
                        {saving ? '저장 중...' : inquiry.answer ? '답변 수정' : '답변 등록'}
                      </button>
                      <button type="button" onClick={() => setOpenId(null)} className="rounded border border-gray-300 px-4 py-2 text-sm hover:bg-gray-50">
                        닫기
                      </button>
                    </div>
                  </div>
                )}
              </li>
            ))}
          </ul>
          <Pagination pageData={pageData} onMove={setPage} />
        </div>
      )}
    </section>
  )
}

export default InquiryManagePage
