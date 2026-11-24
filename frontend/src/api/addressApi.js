import jwtAxios from '../util/jwtUtil'

export const getDefaultAddress = async () => {
  const response = await jwtAxios.get('/api/members/me/default-address')
  return response.status === 204 ? null : response.data
}

export const saveDefaultAddress = async (address) => {
  const response = await jwtAxios.put('/api/members/me/default-address', address)
  return response.data
}

export const deleteDefaultAddress = async () => {
  await jwtAxios.delete('/api/members/me/default-address')
}

// 최근 주소 — 서버는 최근 순 목록을 주고, 배송 신청 화면은 가장 최근 1건만 쓴다 (없으면 null → 버튼 비활성)
export const getRecentAddress = async () => {
  const response = await jwtAxios.get('/api/members/me/recent-address')
  return Array.isArray(response.data) ? response.data[0] ?? null : response.data
}