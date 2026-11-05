import { Fragment, useEffect, useRef, useState } from 'react'
import { changeTermActive, deleteTerm, getAllTerms, postTerm, reviseTerm } from '../../api/termApi'

const INPUT_CLASS = 'rounded border border-gray-300 bg-white px-3 py-2 text-sm focus:border-emerald-600 focus:outline-none'
const SMALL_BUTTON = 'rounded border border-gray-300 px-2 py-1 text-xs hover:bg-gray-50'
const DANGER_BUTTON = 'rounded border border-red-300 px-2 py-1 text-xs text-red-700 hover:bg-red-50'

const initForm = { title: '', content: '', required: true, version: 'v1.0' }

// 개정할 때 버전 제안: v1.0 → v1.1 (형식이 다르면 비워 둔다)
const nextVersion = (version) => {
  const match = /^v(\d+)\.(\d+)$/.exec(version)
  return match ? `v${match[1]}.${Number(match[2]) + 1}` : ''
}

// 관리자 약관 관리 (ADM-11): 약관은 회원 동의 이력의 근거라 내용을 고치지 않는다
// 바꾸려면 새 버전으로 개정 → 새 버전은 사용 중, 이전 버전은 사용 중지 (동의 이력과 함께 남는다)
const TermManagePage = () => {
  const [terms, setTerms] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)    // 저장 · 상태 변경 뒤 목록을 다시 불러온다
  const [form, setForm] = useState(initForm)
  const [revising, setRevising] = useState(null)  // 개정 중인 약관 (없으면 새 약관)
  const [openId, setOpenId] = useState(null)      // 내용을 펼친 약관
  const [confirm, setConfirm] = useState(null)    // 한 번 더 확인 중인 동작 { id, type: 'deactivate' | 'delete' }
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const versionRef = useRef(null)

  useEffect(() => {
    getAllTerms()
      .then(setTerms)
      .catch((err) => setError(err.response?.data?.message ?? '약관을 불러오지 못했어요.'))
  }, [reloadKey])

  // 서버가 사용 중(가입 화면 순서) → 지난 버전 순으로 준다
  const activeTerms = terms?.filter((term) => term.active) ?? []
  const pastTerms = terms?.filter((term) => !term.active) ?? []

  // 필수 체크박스는 checked, 나머지는 value
  const handleChange = (e) => {
    const { name, type, value, checked } = e.target
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value })
  }

  const clearNotice = () => {
    setError('')
    setMessage('')
  }

  const startRevise = (term) => {
    setRevising(term)
    setForm({ title: term.title, content: term.content, required: term.required, version: nextVersion(term.version) })
    clearNotice()
    versionRef.current?.focus()  // 위쪽 폼으로 올라가 새 버전부터 입력
  }

  const cancelRevise = () => {
    setRevising(null)
    setForm(initForm)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    clearNotice()
    const term = { ...form, title: form.title.trim(), version: form.version.trim() }
    try {
      if (revising) {
        await reviseTerm(revising.id, term)
        setMessage(`${term.title} ${term.version} 버전으로 개정했어요. 이전 ${revising.version} 버전은 사용 중지돼 지난 버전에 남아요.`)
      } else {
        await postTerm(term)
        setMessage('약관을 올렸어요. 가입 화면에 바로 보여요.')
      }
      cancelRevise()
      setReloadKey((key) => key + 1)
    } catch (err) {
      setError(err.response?.data?.message ?? '약관을 저장하지 못했어요.')
    } finally {
      setSubmitting(false)
    }
  }

  const changeActive = async (term, active) => {
    clearNotice()
    try {
      await changeTermActive(term.id, active)
      setMessage(active
        ? `${term.title} ${term.version} 버전을 다시 사용해요. 가입 화면에 보여요.`
        : `${term.title} ${term.version} 버전을 사용 중지했어요. 가입 화면에서 빠졌어요.`)
      if (revising?.id === term.id) {
        cancelRevise()
      }
      setReloadKey((key) => key + 1)
    } catch (err) {
      setError(err.response?.data?.message ?? '약관 상태를 바꾸지 못했어요.')
    } finally {
      setConfirm(null)
    }
  }

  const remove = async (term) => {
    clearNotice()
    try {
      await deleteTerm(term.id)
      setMessage(`${term.title} ${term.version} 버전을 삭제했어요.`)
      setReloadKey((key) => key + 1)
    } catch (err) {
      setError(err.response?.data?.message ?? '약관을 삭제하지 못했어요.')
    } finally {
      setConfirm(null)
    }
  }

  // 관리 칸: 보기 · 개정 · 사용 중지 / 다시 사용 · 삭제 (사용 중지 · 삭제는 같은 줄에서 한 번 더 확인)
  const renderAction = (term) => {
    if (confirm?.id === term.id) {
      const isDelete = confirm.type === 'delete'
      return (
        <span className="inline-flex items-center gap-2">
          <span className="text-xs text-gray-600">{isDelete ? '삭제할까요?' : '사용 중지할까요?'}</span>
          <button
            type="button" onClick={() => (isDelete ? remove(term) : changeActive(term, false))}
            className="rounded bg-red-600 px-2 py-1 text-xs font-medium text-white hover:bg-red-700"
          >
            {isDelete ? '삭제' : '사용 중지'}
          </button>
          <button type="button" onClick={() => setConfirm(null)} className={SMALL_BUTTON}>
            취소
          </button>
        </span>
      )
    }
    return (
      <span className="inline-flex gap-2">
        <button
          type="button" aria-expanded={openId === term.id} onClick={() => setOpenId(openId === term.id ? null : term.id)}
          className={SMALL_BUTTON}
        >
          {openId === term.id ? '닫기' : '보기'}
        </button>
        {term.active ? (
          <>
            <button type="button" onClick={() => startRevise(term)} className={SMALL_BUTTON}>
              개정
            </button>
            <button type="button" onClick={() => setConfirm({ id: term.id, type: 'deactivate' })} className={DANGER_BUTTON}>
              사용 중지
            </button>
          </>
        ) : (
          <>
            <button type="button" onClick={() => changeActive(term, true)} className={SMALL_BUTTON}>
              다시 사용
            </button>
            {term.agreementCount === 0 && (
              <button type="button" onClick={() => setConfirm({ id: term.id, type: 'delete' })} className={DANGER_BUTTON}>
                삭제
              </button>
            )}
          </>
        )}
      </span>
    )
  }

  const renderTable = (list, emptyText) => (
    <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white">
      <table className="w-full whitespace-nowrap text-left text-sm">
        <thead className="border-b border-gray-200 bg-gray-50 text-gray-600">
          <tr>
            <th className="px-4 py-3 font-medium">약관</th>
            <th className="px-4 py-3 font-medium">버전</th>
            <th className="px-4 py-3 font-medium">구분</th>
            <th className="px-4 py-3 font-medium">동의</th>
            <th className="px-4 py-3 font-medium">등록일</th>
            <th className="px-4 py-3 text-right font-medium">관리</th>
          </tr>
        </thead>
        <tbody className="[&>tr:last-child]:border-0">
          {list.length === 0 && (
            <tr>
              <td colSpan={6} className="px-4 py-8 text-center text-gray-500">{emptyText}</td>
            </tr>
          )}
          {list.map((term) => (
            <Fragment key={term.id}>
              <tr className={`border-b border-gray-100 ${revising?.id === term.id ? 'bg-emerald-50' : ''}`}>
                <td className="max-w-xs truncate px-4 py-3">{term.title}</td>
                <td className="px-4 py-3">{term.version}</td>
                <td className="px-4 py-3">
                  {term.required ? <span className="font-medium text-emerald-700">필수</span> : <span className="text-gray-500">선택</span>}
                </td>
                <td className="px-4 py-3">{term.agreementCount}명</td>
                <td className="px-4 py-3">{term.createdAt?.slice(0, 10)}</td>
                <td className="px-4 py-3 text-right">{renderAction(term)}</td>
              </tr>
              {openId === term.id && (
                <tr className="border-b border-gray-100">
                  <td colSpan={6} className="bg-gray-50 px-4 py-3">
                    <p className="max-h-60 overflow-y-auto whitespace-pre-line text-xs leading-6 text-gray-700">{term.content}</p>
                  </td>
                </tr>
              )}
            </Fragment>
          ))}
        </tbody>
      </table>
    </div>
  )

  return (
    <section className="grid gap-6">
      <div className="grid gap-1">
        <h1 className="text-xl font-bold">약관 관리</h1>
        <p className="text-sm text-gray-600">
          약관은 회원 동의 이력의 근거라 내용을 고치지 않아요. 바꾸려면 개정하세요 — 새 버전이 가입 화면에 쓰이고, 이전 버전은 동의 이력과 함께 남아요.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="grid gap-3 rounded-lg border border-gray-200 bg-white p-4">
        <h2 className="font-semibold">{revising ? `개정: ${revising.title} ${revising.version} → 새 버전` : '새 약관'}</h2>
        {revising && (
          <p className="text-xs text-gray-600">
            저장하면 새 버전이 가입 화면에 쓰이고, 지금 버전({revising.version} · 동의 {revising.agreementCount}명)은 사용 중지돼 지난 버전에 남아요.
          </p>
        )}
        <div className="grid gap-3 sm:grid-cols-[1fr_10rem_auto] sm:items-end">
          <label className="grid gap-1 text-sm">
            <span className="font-medium">약관 이름</span>
            <input name="title" required maxLength={100} value={form.title} onChange={handleChange} className={INPUT_CLASS} />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="font-medium">버전</span>
            <input
              ref={versionRef} name="version" required maxLength={20} placeholder="예: v1.1" value={form.version} onChange={handleChange}
              className={INPUT_CLASS}
            />
          </label>
          <label className="flex items-center gap-2 py-2 text-sm">
            <input type="checkbox" name="required" checked={form.required} onChange={handleChange} className="size-4 accent-emerald-700" />
            <span className="font-medium">필수 동의</span>
          </label>
        </div>
        <label className="grid gap-1 text-sm">
          <span className="font-medium">내용</span>
          <textarea
            name="content" required maxLength={15000} rows={10} value={form.content} onChange={handleChange}
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
            {submitting ? '저장 중...' : revising ? '새 버전으로 개정' : '약관 올리기'}
          </button>
          {revising && (
            <button type="button" onClick={cancelRevise} className="rounded border border-gray-300 px-4 py-2 text-sm hover:bg-gray-50">
              취소
            </button>
          )}
        </div>
      </form>

      {!terms && !error && <p className="text-sm text-gray-500">약관을 불러오는 중...</p>}
      {terms && (
        <>
          <div className="grid gap-3">
            <h2 className="font-semibold">
              사용 중인 약관 <span className="text-sm font-normal text-gray-500">(가입 화면 순서: 필수 먼저)</span>
            </h2>
            {renderTable(activeTerms, '사용 중인 약관이 없어요.')}
          </div>
          <div className="grid gap-3">
            <h2 className="font-semibold">
              지난 버전 · 사용 중지 <span className="text-sm font-normal text-gray-500">(동의한 회원이 없는 약관만 삭제할 수 있어요)</span>
            </h2>
            {renderTable(pastTerms, '지난 버전이 없어요.')}
          </div>
        </>
      )}
    </section>
  )
}

export default TermManagePage
