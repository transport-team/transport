import { useState } from 'react'
import { putMyInfo } from '../../api/memberApi'
import { formatPhone, PHONE_PATTERN } from '../../util/formatUtil'

// 연락처 변경: 저장하면 바뀐 내 정보를 onSaved로 넘긴다
const PhoneForm = ({ phone, onSaved }) => {
  const [value, setValue] = useState(phone)
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const handleChange = (e) => {
    setValue(formatPhone(e.target.value))
    setSaved(false)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!PHONE_PATTERN.test(value)) {
      setError('연락처는 010-0000-0000 형식으로 입력해 주세요.')
      return
    }
    setSubmitting(true)
    setError('')
    try {
      onSaved(await putMyInfo({ phone: value }))
      setSaved(true)
    } catch (err) {
      setError(err.response?.data?.message ?? '연락처를 바꾸지 못했어요.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="grid gap-3 rounded-lg border border-gray-200 bg-white p-4">
      <h2 className="font-semibold">연락처 변경</h2>
      <label className="grid gap-1 text-sm">
        <span className="font-medium">연락처</span>
        <input
          name="phone" type="tel" inputMode="numeric" autoComplete="tel" required placeholder="010-1234-5678"
          value={value} onChange={handleChange}
          className="rounded border border-gray-300 bg-white px-3 py-2 focus:border-emerald-600 focus:outline-none"
        />
        <span className="text-xs text-gray-500">숫자만 입력하면 하이픈이 자동으로 들어가요</span>
      </label>
      {error && <p role="alert" className="rounded bg-red-50 p-3 text-sm text-red-800">{error}</p>}
      {saved && <p role="status" className="rounded bg-emerald-50 p-3 text-sm text-emerald-800">연락처를 바꿨어요.</p>}
      <button
        type="submit" disabled={submitting || value === phone}
        className="justify-self-start rounded bg-emerald-700 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-800 disabled:opacity-60"
      >
        {submitting ? '저장 중...' : '연락처 저장'}
      </button>
    </form>
  )
}

export default PhoneForm
