import { Cookies } from 'react-cookie'

const cookies = new Cookies()

// 쿠키 저장 (days일 동안 유지, 객체는 JSON으로 저장된다)
// sameSite strict: 다른 사이트에서 넘어온 요청에는 붙이지 않는다 · secure: HTTPS(배포)에서는 HTTPS로만 보낸다 (로컬 http에서는 끔)
export const setCookie = (name, value, days) => {
  const expires = new Date()
  expires.setUTCDate(expires.getUTCDate() + days)
  return cookies.set(name, value, {
    path: '/',
    expires,
    sameSite: 'strict',
    secure: window.location.protocol === 'https:',
  })
}

export const getCookie = (name) => cookies.get(name)

export const removeCookie = (name, path = '/') => cookies.remove(name, { path })
