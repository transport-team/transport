import { useEffect, useState } from 'react'
import { deleteFaq, FAQ_CATEGORIES, getAllFaqs, postFaq, putFaq } from '../../api/faqApi'

const INPUT_CLASS = 'rounded border border-gray-300 bg-white px-3 py-2 text-sm focus:border-emerald-600 focus:outline-none'

const initForm = { category: FAQ_CATEGORIES[0], question: '', answer: '', sortOrder: '', active: true }

// 관리자 FAQ 관리 (ADM-11): 숨긴 FAQ까지 전체 목록 · 작성 · 수정(숨기기 = 노출 끄기) · 삭제
const FaqManagePage = () => {
  const [faqs, setFaqs] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)     // 저장 · 삭제 뒤 목록을 다시 불러온다
  const [form, setForm] = useState(initForm)
  const [editingId, setEditingId] = useState(null)  // 수정 중인 FAQ (없으면 새 FAQ)
  const [confirmId, setConfirmId] = useState(null)  // 삭제를 확인 중인 FAQ
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    getAllFaqs()
      .then(setFaqs)
      .catch((err) => setError(err.response?.data?.message ?? 'FAQ를 불러오지 못했어요.'))
  }, [reloadKey])

  // 순서를 비워 두면 맨 뒤 (지금 가장 큰 순서 + 1)
  const nextOrder = faqs?.length ? Math.max(...faqs.map((faq) => faq.sortOrder)) + 1 : 1

  // 노출 체크박스는 checked, 나머지는 value
  const handleChange = (e) => {
    const { name, type, value, checked } = e.target
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value })
  }

  const startEdit = (faq) => {
    setEditingId(faq.id)
    setForm({
      category: faq.category, question: faq.question, answer: faq.answer, sortOrder: String(faq.sortOrder), active: faq.active,
    })
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
    const faq = { ...form, sortOrder: form.sortOrder === '' ? nextOrder : Number(form.sortOrder) }
    try {
      if (editingId) {
        await putFaq(editingId, faq)
        setMessage('FAQ를 수정했어요.')
      } else {
        await postFaq(faq)
        setMessage('FAQ를 올렸어요.')
      }
      cancelEdit()
      setReloadKey((key) => key + 1)
    } catch (err) {
      setError(err.response?.data?.message ?? 'FAQ를 저장하지 못했어요.')
    } finally {
      setSubmitting(false)
    }
  }

  const remove = async (faqId) => {
    setError('')
    setMessage('')
    try {
      await deleteFaq(faqId)
      setMessage('FAQ를 삭제했어요.')
      if (editingId === faqId) {
        cancelEdit()
      }
      setReloadKey((key) => key + 1)
    } catch (err) {
      setError(err.response?.data?.message ?? 'FAQ를 삭제하지 못했어요.')
    } finally {
      setConfirmId(null)
    }
  }

  // 관리 칸: 수정 · 삭제 (삭제는 같은 줄에서 한 번 더 확인)
  const renderAction = (faq) => {
    if (confirmId === faq.id) {
      return (
        <span className="inline-flex items-center gap-2">
          <span className="text-xs text-gray-600">삭제할까요?</span>
          <button
            type="button" onClick={() => remove(faq.id)}
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
        <button type="button" onClick={() => startEdit(faq)} className="rounded border border-gray-300 px-2 py-1 text-xs hover:bg-gray-50">
          수정
        </button>
        <button
          type="button" onClick={() => setConfirmId(faq.id)}
          className="rounded border border-red-300 px-2 py-1 text-xs text-red-700 hover:bg-red-50"
        >
          삭제
        </button>
      </span>
    )
  }

  return (
    <section className="grid gap-6">
      <h1 className="text-xl font-bold">FAQ 관리</h1>

      <form onSubmit={handleSubmit} className="grid gap-3 rounded-lg border border-gray-200 bg-white p-4">
        <h2 className="font-semibold">{editingId ? 'FAQ 수정' : '새 FAQ'}</h2>
        <div className="grid gap-3 sm:grid-cols-[10rem_12rem_1fr] sm:items-end">
          <label className="grid gap-1 text-sm">
            <span className="font-medium">분류</span>
            <select name="category" value={form.category} onChange={handleChange} className={INPUT_CLASS}>
              {FAQ_CATEGORIES.map((category) => <option key={category}>{category}</option>)}
            </select>
          </label>
          <label className="grid gap-1 text-sm">
            <span className="font-medium">순서 <span className="font-normal text-gray-500">(작을수록 위 · 비우면 맨 뒤)</span></span>
            <input
              type="number" name="sortOrder" min={0} step={1} placeholder={String(nextOrder)} value={form.sortOrder} onChange={handleChange}
              className={INPUT_CLASS}
            />
          </label>
          <label className="flex items-center gap-2 py-2 text-sm">
            <input type="checkbox" name="active" checked={form.active} onChange={handleChange} className="size-4 accent-emerald-700" />
            <span className="font-medium">FAQ 화면에 보이기</span>
          </label>
        </div>
        <label className="grid gap-1 text-sm">
          <span className="font-medium">질문</span>
          <input name="question" required maxLength={255} value={form.question} onChange={handleChange} className={INPUT_CLASS} />
        </label>
        <label className="grid gap-1 text-sm">
          <span className="font-medium">답변</span>
          <textarea
            name="answer" required maxLength={5000} rows={5} value={form.answer} onChange={handleChange}
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
            {submitting ? '저장 중...' : editingId ? '수정 저장' : 'FAQ 올리기'}
          </button>
          {editingId && (
            <button type="button" onClick={cancelEdit} className="rounded border border-gray-300 px-4 py-2 text-sm hover:bg-gray-50">
              취소
            </button>
          )}
        </div>
      </form>

      {!faqs && !error && <p className="text-sm text-gray-500">FAQ를 불러오는 중...</p>}
      {faqs && (
        <div className="grid gap-3">
          <p className="text-sm text-gray-600">
            전체 {faqs.length}개 · 숨김 {faqs.filter((faq) => !faq.active).length}개
          </p>
          <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white">
            <table className="w-full whitespace-nowrap text-left text-sm">
              <thead className="border-b border-gray-200 bg-gray-50 text-gray-600">
                <tr>
                  <th className="px-4 py-3 font-medium">분류</th>
                  <th className="px-4 py-3 font-medium">질문</th>
                  <th className="px-4 py-3 font-medium">순서</th>
                  <th className="px-4 py-3 font-medium">노출</th>
                  <th className="px-4 py-3 text-right font-medium">관리</th>
                </tr>
              </thead>
              <tbody>
                {faqs.length === 0 && (
                  <tr>
                    <td colSpan={5} className="px-4 py-8 text-center text-gray-500">FAQ가 없어요.</td>
                  </tr>
                )}
                {faqs.map((faq) => (
                  <tr key={faq.id} className={`border-b border-gray-100 last:border-0 ${editingId === faq.id ? 'bg-emerald-50' : ''}`}>
                    <td className="px-4 py-3">{faq.category}</td>
                    <td className={`max-w-xs truncate px-4 py-3 ${faq.active ? '' : 'text-gray-400'}`}>{faq.question}</td>
                    <td className="px-4 py-3">{faq.sortOrder}</td>
                    <td className="px-4 py-3">
                      {faq.active
                        ? <span className="rounded bg-emerald-50 px-2 py-0.5 text-xs text-emerald-800">노출</span>
                        : <span className="rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-600">숨김</span>}
                    </td>
                    <td className="px-4 py-3 text-right">{renderAction(faq)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </section>
  )
}

export default FaqManagePage
