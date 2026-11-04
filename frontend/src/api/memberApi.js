import jwtAxios from '../util/jwtUtil'
import apiClient from './apiClient'

// 회원가입 → { memberId } (기사는 승인 대기로 가입되어 관리자 승인 후 로그인할 수 있다)
export const signupPost = async (signupParam) => {
  const res = await apiClient.post('/api/auth/signup', signupParam)
  return res.data
}

// 로그인 → { accessToken, refreshToken, memberId, email, name, role }
export const loginPost = async (loginParam) => {
  const res = await apiClient.post('/api/auth/login', loginParam)
  return res.data
}

// 내 정보 (로그인 필요 → jwtAxios)
export const getMyInfo = async () => {
  const res = await jwtAxios.get('/api/members/me')
  return res.data
}
