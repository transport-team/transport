import { useState } from 'react'
import { patchMyPassword } from '../../api/memberApi'

const INPUT_CLASS = 'rounded border border-gray-300 bg-white px-3 py-2 focus:border-emerald-600 focus:outline-none'

const initForm = { currentPassword: '', newPassword: '', newPasswordConfirm: '' }

// 라벨 + 입력칸 (공통 Input 컴포넌트가 생기면 그것으로 바꾼다)
const Field = ({ label, hint, children }) => (
  <label className="grid content-start gap-1 text-sm">
    <span className="font-medium">{label}</span>
    {children}
    {hint && <span className="text-xs text-gray-500">{hint}</span>}
  </label>
)

// 비밀번호 변경: 현재 비밀번호를 확인한 뒤 바꾼다 (로그인은 그대로 유지)
const PasswordForm = () => {
  const [form, setForm] = useState(initForm)
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value })
    setSaved(false)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (form.newPassword !== form.newPasswordConfirm) {
      setError('새 비밀번호 확인이 일치하지 않아요.')
      return
    }
    setSubmitting(true)
    setError('')
    try {
      await patchMyPassword({ currentPassword: form.currentPassword, newPassword: form.newPassword })
      setForm(initForm)
      setSaved(true)
    } catch (err) {
      // 400 WRONG_PASSWORD(현재 비밀번호 틀림) · INVALID_INPUT → 서버 문구 그대로
      setError(err.response?.data?.message ?? '비밀번호를 바꾸지 못했어요.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="grid gap-3 rounded-lg border border-gray-200 bg-white p-4">
      <h2 className="font-semibold">비밀번호 변경</h2>
      <Field label="현재 비밀번호">
        <input
          name="currentPassword" type="password" autoComplete="current-password" required
          value={form.currentPassword} onChange={handleChange} className={INPUT_CLASS}
        />
      </Field>
      <Field label="새 비밀번호" hint="8~64자">
        <input
          name="newPassword" type="password" autoComplete="new-password" required minLength={8} maxLength={64}
          value={form.newPassword} onChange={handleChange} className={INPUT_CLASS}
        />
      </Field>
      <Field label="새 비밀번호 확인">
        <input
          name="newPasswordConfirm" type="password" autoComplete="new-password" required
          value={form.newPasswordConfirm} onChange={handleChange} className={INPUT_CLASS}
        />
      </Field>
      {error && <p role="alert" className="rounded bg-red-50 p-3 text-sm text-red-800">{error}</p>}
      {saved && (
        <p role="status" className="rounded bg-emerald-50 p-3 text-sm text-emerald-800">
          비밀번호를 바꿨어요. 다음 로그인부터 새 비밀번호를 써 주세요.
        </p>
      )}
      <button
        type="submit" disabled={submitting}
        className="justify-self-start rounded bg-emerald-700 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-800 disabled:opacity-60"
      >
        {submitting ? '변경 중...' : '비밀번호 변경'}
      </button>
    </form>
  )
}

export default PasswordForm
