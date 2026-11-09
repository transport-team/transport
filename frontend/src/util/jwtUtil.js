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

// 지금 진행 중인 재발급 (없으면 null)
// 동시에 만료된 요청들이 재발급을 하나씩 부르지 않고, 이 하나의 결과(새 액세스 토큰)를 함께 기다린다
let refreshing = null

const refreshOnce = () => {
  if (!refreshing) {
    const member = getCookie('member')
    refreshing = refreshJWT(member?.refreshToken)
      .then((result) => {
        setCookie('member', { ...member, ...result }, 1)
        return result.accessToken
      })
      .catch((refreshErr) => {
        // 리프레시 토큰도 만료 · 잘못됨 → 로그아웃하고 로그인 화면으로 (기다리던 요청이 여러 개여도 한 번만)
        removeCookie('member')
        window.location.href = '/login?expired=true'
        throw refreshErr
      })
      .finally(() => {
        refreshing = null
      })
  }
  return refreshing
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

  // 다른 요청이 이미 새 토큰을 받아 두었으면 재발급 없이 그 토큰으로 다시 보낸다
  const sentToken = original.headers.Authorization?.replace('Bearer ', '')
  const currentToken = getCookie('member')?.accessToken
  const accessToken = currentToken && currentToken !== sentToken ? currentToken : await refreshOnce()

  original.headers.Authorization = `Bearer ${accessToken}`
  return jwtAxios(original)
}

jwtAxios.interceptors.request.use(beforeReq)
jwtAxios.interceptors.response.use((res) => res, responseFail)

export default jwtAxios
