import { useSelector } from 'react-redux'
import { Navigate, useLocation } from 'react-router-dom'
import { ROLE_HOME } from '../util/roleUtil'

// 보호 라우트: 로그인을 안 했으면 로그인 화면으로(돌아올 주소를 붙여서), 역할이 다르면 내 역할 첫 화면으로
// 방금 로그아웃했으면 메인으로. 화면만 막는 것이고, 실제 권한 검사는 서버(@PreAuthorize · URL 권한)가 한다
const RequireAuth = ({ role, children }) => {
  const loginState = useSelector((state) => state.loginSlice)
  const location = useLocation()

  if (!loginState.memberId) {
    const to = loginState.loggedOut ? '/' : `/login?redirect=${encodeURIComponent(location.pathname)}`
    return <Navigate to={to} replace />
  }
  if (role && loginState.role !== role) {
    return <Navigate to={ROLE_HOME[loginState.role] ?? '/'} replace />
  }
  return children
}

export default RequireAuth
