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

export const getRecentAddress = async () => {
  const response = await jwtAxios.get('/api/members/me/recent-address')
  return response.data
}