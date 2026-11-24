import apiClient from './apiClient'

// 서버 동작 확인 → { status: 'ok' }
export const getHealth = async () => {
  const res = await apiClient.get('/api/health')
  return res.data
}
