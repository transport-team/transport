import axios from 'axios'
import { API_SERVER_HOST } from '../api/apiClient'
import { getCookie, removeCookie, setCookie } from './cookieUtil'

// 로그인이 필요한 API용 axios (수업의 jwtAxios)
// 요청마다 쿠키의 액세스 토큰을 붙이고, 만료(401 TOKEN_EXPIRED)되면 재발급받아 원래 요청을 한 번 다시 보낸다
const jwtAxios = axios.create({ baseURL: API_SERVER_HOST })

const refreshJWT = async (refreshToken) => {
  const res = await axios.post(`${API_SERVER_HOST}/api/auth/refresh`, { refreshToken })
  return res.data
}

const beforeReq = (config) => {
  const member = getCookie('member')
  if (member?.accessToken) {
    config.headers.Authorization = `Bearer ${member.accessToken}`
  }
  return config
}

const responseFail = async (err) => {
  const original = err.config
  if (err.response?.data?.code !== 'TOKEN_EXPIRED' || original._retry) {
    return Promise.reject(err)
  }
  original._retry = true

  const member = getCookie('member')
  try {
    const result = await refreshJWT(member?.refreshToken)
    setCookie('member', { ...member, ...result }, 1)
    original.headers.Authorization = `Bearer ${result.accessToken}`
    return jwtAxios(original)
  } catch (refreshErr) {
    // 리프레시 토큰도 만료 · 잘못됨 → 로그아웃하고 로그인 화면으로
    removeCookie('member')
    window.location.href = '/login?expired=true'
    return Promise.reject(refreshErr)
  }
}

jwtAxios.interceptors.request.use(beforeReq)
jwtAxios.interceptors.response.use((res) => res, responseFail)

export default jwtAxios
