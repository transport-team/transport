import jwtAxios from '../util/jwtUtil'
import apiClient from './apiClient'

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
