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
// 고객의 주소록 — 한 항목은 출발·도착 세트가 아니라 주소 한 곳이다.
export const getSavedAddresses = async () => {
  const response = await jwtAxios.get('/api/members/me/saved-addresses')
  return response.data
}

export const createSavedAddress = async (address) => {
  const response = await jwtAxios.post('/api/members/me/saved-addresses', address)
  return response.data
}

export const updateSavedAddress = async (id, address) => {
  const response = await jwtAxios.put(`/api/members/me/saved-addresses/${id}`, address)
  return response.data
}

export const deleteSavedAddress = async (id) => {
  await jwtAxios.delete(`/api/members/me/saved-addresses/${id}`)
}
