import { useState } from 'react'
import { Navigate, useSearchParams } from 'react-router-dom'
import useCustomLogin from '../hooks/useCustomLogin'
import BasicLayout from '../layouts/BasicLayout'
import { ROLE_HOME } from '../util/roleUtil'

// 시드 계정 (data.sql, 비밀번호 1234) — 개발 서버에서만 안내
const SEED_ACCOUNTS = ['customer@transport.test', 'driver@transport.test', 'admin@transport.test']

// 로그인: 성공하면 역할별 첫 화면으로 (보호된 화면에서 왔으면 그 화면으로)
const LoginPage = () => {
  const [loginParam, setLoginParam] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [searchParams] = useSearchParams()
  const { loginState, isLogin, doLogin } = useCustomLogin()

  if (isLogin) {
    const home = ROLE_HOME[loginState.role] ?? '/'
    const redirect = searchParams.get('redirect')
    // 내 역할 화면 안의 주소로만 돌려보낸다 (다른 역할 화면 · 바깥 주소 X)
    return <Navigate to={redirect?.startsWith(home) ? redirect : home} replace />
  }

  const handleChange = (e) => setLoginParam({ ...loginParam, [e.target.name]: e.target.value })

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      await doLogin(loginParam)  // 성공하면 로그인 상태가 바뀌어 위의 Navigate로 이동
    } catch (err) {
      setError(err.message ?? '로그인하지 못했어요.')
      setSubmitting(false)
    }
  }

  return (
    <BasicLayout>
      <div className="mx-auto grid max-w-sm gap-6">
        <h1 className="text-2xl font-bold">로그인</h1>
        {searchParams.get('expired') && (
          <p className="rounded bg-amber-50 p-3 text-sm text-amber-800">로그인 시간이 끝났어요. 다시 로그인해 주세요.</p>
        )}
        <form onSubmit={handleSubmit} className="grid gap-4">
          <label htmlFor="login-email" className="grid gap-1 text-sm">
            <span className="font-medium">이메일</span>
            <input
              id="login-email" name="email" type="email" autoComplete="username" required
              value={loginParam.email} onChange={handleChange}
              className="rounded border border-gray-300 bg-white px-3 py-2 focus:border-emerald-600 focus:outline-none"
            />
          </label>
          <label htmlFor="login-password" className="grid gap-1 text-sm">
            <span className="font-medium">비밀번호</span>
            <input
              id="login-password" name="password" type="password" autoComplete="current-password" required
              value={loginParam.password} onChange={handleChange}
              className="rounded border border-gray-300 bg-white px-3 py-2 focus:border-emerald-600 focus:outline-none"
            />
          </label>
          {error && <p role="alert" className="rounded bg-red-50 p-3 text-sm text-red-800">{error}</p>}
          <button
            type="submit" disabled={submitting}
            className="rounded bg-emerald-700 py-2.5 font-medium text-white hover:bg-emerald-800 disabled:opacity-60"
          >
            {submitting ? '로그인 중...' : '로그인'}
          </button>
        </form>
        {import.meta.env.DEV && (
          <p className="text-xs text-gray-500">개발용 시드 계정 (비밀번호 1234): {SEED_ACCOUNTS.join(' · ')}</p>
        )}
      </div>
    </BasicLayout>
  )
}

export default LoginPage
