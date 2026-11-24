import { useState } from 'react'

// 가입 약관 동의: 전체 동의 · 약관별 동의 · 내용 보기
// terms: GET /api/terms 결과, agreedIds: 동의한 약관 id 배열, onChange(바뀐 id 배열)
const TermAgreement = ({ terms, agreedIds, onChange }) => {
  const [openId, setOpenId] = useState(null)  // 내용을 펼친 약관 id
  const allAgreed = terms.length > 0 && terms.every((term) => agreedIds.includes(term.id))

  const toggleAll = (e) => onChange(e.target.checked ? terms.map((term) => term.id) : [])

  const toggle = (id) => onChange(agreedIds.includes(id) ? agreedIds.filter((agreedId) => agreedId !== id) : [...agreedIds, id])

  return (
    <fieldset>
      <legend className="mb-1 text-sm font-medium">약관 동의</legend>
      <div className="grid gap-3 rounded border border-gray-300 bg-white p-4 text-sm">
        <label className="flex items-center gap-2 border-b border-gray-200 pb-3 font-medium">
          <input type="checkbox" checked={allAgreed} onChange={toggleAll} className="size-4 accent-emerald-700" />
          전체 동의 (선택 약관 포함)
        </label>
        {terms.map((term) => (
          <div key={term.id}>
            <div className="flex items-center gap-2">
              <label className="flex flex-1 items-center gap-2">
                <input
                  type="checkbox" checked={agreedIds.includes(term.id)} onChange={() => toggle(term.id)}
                  className="size-4 accent-emerald-700"
                />
                <span className={term.required ? 'font-medium text-emerald-700' : 'text-gray-500'}>
                  [{term.required ? '필수' : '선택'}]
                </span>
                {term.title}
              </label>
              <button
                type="button" aria-expanded={openId === term.id}
                onClick={() => setOpenId(openId === term.id ? null : term.id)}
                className="shrink-0 text-xs text-gray-500 underline hover:text-gray-800"
              >
                {openId === term.id ? '닫기' : '보기'}
              </button>
            </div>
            {openId === term.id && (
              <p className="mt-2 max-h-40 overflow-y-auto whitespace-pre-line rounded bg-gray-50 p-3 text-xs text-gray-600">
                {term.content}
              </p>
            )}
          </div>
        ))}
      </div>
    </fieldset>
  )
}

export default TermAgreement
