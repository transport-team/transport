import { useEffect, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { signupPost } from '../api/memberApi'
import { getTerms } from '../api/termApi'
import TermAgreement from '../components/member/TermAgreement'
import useCustomLogin from '../hooks/useCustomLogin'
import BasicLayout from '../layouts/BasicLayout'
import { formatPhone, PHONE_PATTERN } from '../util/formatUtil'
import { ROLE_HOME } from '../util/roleUtil'

const ROLE_OPTIONS = [
  { value: 'CUSTOMER', label: '고객', desc: '화물 운송을 맡겨요' },
  { value: 'DRIVER', label: '기사', desc: '화물을 운송해요 (관리자 승인 후 이용)' },
]

// 기사 면허 종류 (서버는 20자 이하 문자열로 받는다)
const LICENSE_TYPES = ['1종 대형', '1종 보통', '1종 특수', '2종 보통']

const INPUT_CLASS = 'rounded border border-gray-300 bg-white px-3 py-2 focus:border-emerald-600 focus:outline-none'

const initForm = { email: '', password: '', passwordConfirm: '', name: '', phone: '', licenseType: '', careerYears: '' }

// 라벨 + 입력칸 (공통 Input 컴포넌트가 생기면 그것으로 바꾼다)
// content-start: 옆 칸이 더 높아도(안내 문구) 입력칸이 늘어나지 않게
const Field = ({ label, hint, children }) => (
  <label className="grid content-start gap-1 text-sm">
    <span className="font-medium">{label}</span>
    {children}
    {hint && <span className="text-xs text-gray-500">{hint}</span>}
  </label>
)

// 회원가입: 고객 · 기사(면허 · 경력 추가) + 약관 동의 → 성공하면 로그인 화면으로 (기사는 관리자 승인 후 로그인)
const SignupPage = () => {
  const [role, setRole] = useState('CUSTOMER')
  const [form, setForm] = useState(initForm)
  const [terms, setTerms] = useState(null)  // 불러오기 전에는 null
  const [agreedIds, setAgreedIds] = useState([])
  const [termsError, setTermsError] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const navigate = useNavigate()
  const { loginState, isLogin } = useCustomLogin()

  useEffect(() => {
    getTerms()
      .then(setTerms)
      .catch(() => setTermsError('약관을 불러오지 못했어요. 백엔드가 실행 중인지 확인해 주세요.'))
  }, [])

  if (isLogin) {
    return <Navigate to={ROLE_HOME[loginState.role] ?? '/'} replace />
  }

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const handlePhoneChange = (e) => setForm({ ...form, phone: formatPhone(e.target.value) })

  // 서버와 같은 규칙으로 먼저 확인한다 (최종 검사는 서버). 문제가 없으면 null
  const validate = () => {
    if (form.password !== form.passwordConfirm) return '비밀번호 확인이 일치하지 않아요.'
    if (!PHONE_PATTERN.test(form.phone)) return '연락처는 010-0000-0000 형식으로 입력해 주세요.'
    if (terms.some((term) => term.required && !agreedIds.includes(term.id))) return '필수 약관에 모두 동의해 주세요.'
    return null
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const invalid = validate()
    if (invalid) {
      setError(invalid)
      return
    }
    setSubmitting(true)
    setError('')
    try {
      await signupPost({
        email: form.email,
        password: form.password,
        name: form.name.trim(),
        phone: form.phone,
        role,
        agreedTermIds: agreedIds,
        // 기사만 면허 · 경력을 보낸다
        ...(role === 'DRIVER' && { licenseType: form.licenseType, careerYears: Number(form.careerYears) }),
      })
      // 이메일은 주소창(쿼리)에 남기지 않고 router state로 넘긴다
      navigate('/login', { replace: true, state: { signupRole: role, email: form.email } })
    } catch (err) {
      // 409 DUPLICATE_EMAIL · 400 TERMS_REQUIRED · INVALID_INPUT → 서버 문구 그대로
      setError(err.response?.data?.message ?? '서버에 연결할 수 없어요. 백엔드가 실행 중인지 확인해 주세요.')
      setSubmitting(false)
    }
  }

  return (
    <BasicLayout>
      <div className="mx-auto grid max-w-md gap-6">
        <h1 className="text-2xl font-bold">회원가입</h1>
        <form onSubmit={handleSubmit} className="grid gap-4">
          <fieldset>
            <legend className="mb-1 text-sm font-medium">회원 유형</legend>
            <div className="grid grid-cols-2 gap-2">
              {ROLE_OPTIONS.map((option) => (
                <label
                  key={option.value}
                  className={`cursor-pointer rounded border p-3 text-sm has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-emerald-600 ${role === option.value ? 'border-emerald-600 bg-emerald-50 text-emerald-900' : 'border-gray-300 bg-white hover:bg-gray-50'}`}
                >
                  <input
                    type="radio" name="role" value={option.value}
                    checked={role === option.value} onChange={() => setRole(option.value)}
                    className="sr-only"
                  />
                  <span className="block font-medium">{option.label}</span>
                  <span className="mt-0.5 block text-xs text-gray-500">{option.desc}</span>
                </label>
              ))}
            </div>
          </fieldset>
          <Field label="이메일">
            <input
              name="email" type="email" autoComplete="email" required maxLength={100}
              value={form.email} onChange={handleChange} className={INPUT_CLASS}
            />
          </Field>
          <Field label="비밀번호" hint="8~64자">
            <input
              name="password" type="password" autoComplete="new-password" required minLength={8} maxLength={64}
              value={form.password} onChange={handleChange} className={INPUT_CLASS}
            />
          </Field>
          <Field label="비밀번호 확인">
            <input
              name="passwordConfirm" type="password" autoComplete="new-password" required
              value={form.passwordConfirm} onChange={handleChange} className={INPUT_CLASS}
            />
          </Field>
          <Field label="이름">
            <input
              name="name" autoComplete="name" required maxLength={50}
              value={form.name} onChange={handleChange} className={INPUT_CLASS}
            />
          </Field>
          <Field label="연락처" hint="숫자만 입력하면 하이픈이 자동으로 들어가요">
            <input
              name="phone" type="tel" inputMode="numeric" autoComplete="tel" required placeholder="010-1234-5678"
              value={form.phone} onChange={handlePhoneChange} className={INPUT_CLASS}
            />
          </Field>
          {role === 'DRIVER' && (
            <div className="grid gap-4 rounded border border-emerald-200 bg-emerald-50/50 p-4 sm:grid-cols-2">
              <p className="text-sm text-emerald-900 sm:col-span-2">기사는 가입 후 관리자 승인을 받아야 로그인할 수 있어요.</p>
              <Field label="면허 종류">
                <select name="licenseType" required value={form.licenseType} onChange={handleChange} className={INPUT_CLASS}>
                  <option value="">선택해 주세요</option>
                  {LICENSE_TYPES.map((type) => <option key={type} value={type}>{type}</option>)}
                </select>
              </Field>
              <Field label="경력 (년)" hint="0~60년">
                <input
                  name="careerYears" type="number" inputMode="numeric" required min={0} max={60}
                  value={form.careerYears} onChange={handleChange} className={INPUT_CLASS}
                />
              </Field>
            </div>
          )}
          {terms && <TermAgreement terms={terms} agreedIds={agreedIds} onChange={setAgreedIds} />}
          {!terms && !termsError && <p className="text-sm text-gray-500">약관을 불러오는 중...</p>}
          {termsError && <p role="alert" className="rounded bg-red-50 p-3 text-sm text-red-800">{termsError}</p>}
          {error && <p role="alert" className="rounded bg-red-50 p-3 text-sm text-red-800">{error}</p>}
          <button
            type="submit" disabled={!terms || submitting}
            className="rounded bg-emerald-700 py-2.5 font-medium text-white hover:bg-emerald-800 disabled:opacity-60"
          >
            {submitting ? '가입 중...' : '가입하기'}
          </button>
        </form>
        <p className="text-sm text-gray-600">
          이미 회원인가요? <Link to="/login" className="font-medium text-emerald-700 hover:underline">로그인</Link>
        </p>
      </div>
    </BasicLayout>
  )
}

export default SignupPage
