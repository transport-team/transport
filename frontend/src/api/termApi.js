import apiClient from './apiClient'

// 가입 화면 약관 목록 → [{ id, title, content, required, version }] (required = 필수 동의)
export const getTerms = async () => {
  const res = await apiClient.get('/api/terms')
  return res.data
}
