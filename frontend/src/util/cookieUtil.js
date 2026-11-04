import { Cookies } from 'react-cookie'

const cookies = new Cookies()

// 쿠키 저장 (days일 동안 유지, 객체는 JSON으로 저장된다)
export const setCookie = (name, value, days) => {
  const expires = new Date()
  expires.setUTCDate(expires.getUTCDate() + days)
  return cookies.set(name, value, { path: '/', expires })
}

export const getCookie = (name) => cookies.get(name)

export const removeCookie = (name, path = '/') => cookies.remove(name, { path })
