import { useDispatch, useSelector } from 'react-redux'
import { useNavigate } from 'react-router-dom'
import { loginPostAsync, logout } from '../slices/loginSlice'
import { ROLE_HOME } from '../util/roleUtil'

// 로그인 상태 · 로그인 · 로그아웃 · 역할별 이동을 한곳에서 (수업의 useCustomLogin)
const useCustomLogin = () => {
  const navigate = useNavigate()
  const dispatch = useDispatch()
  const loginState = useSelector((state) => state.loginSlice)
  const isLogin = !!loginState.memberId

  // 성공하면 회원 정보, 실패하면 서버의 {code, message}로 reject
  const doLogin = (loginParam) => dispatch(loginPostAsync(loginParam)).unwrap()

  // 로그아웃 후 메인으로 (보호된 화면에서는 RequireAuth도 메인으로 보낸다)
  const doLogout = () => {
    dispatch(logout())
    navigate('/', { replace: true })
  }

  const moveToRoleHome = () => navigate(ROLE_HOME[loginState.role] ?? '/', { replace: true })

  return { loginState, isLogin, doLogin, doLogout, moveToRoleHome }
}

export default useCustomLogin
