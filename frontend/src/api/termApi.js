import jwtAxios from '../util/jwtUtil'
import apiClient from './apiClient'

// 가입 화면 약관 목록 (필수 먼저) → [{ id, title, content, required, version }] (required = 필수 동의)
export const getTerms = async () => {
  const res = await apiClient.get('/api/terms')
  return res.data
}

// 관리자: 약관 전체 (지난 버전 · 사용 중지 포함) → [{ ...약관, active, agreementCount, createdAt }]
export const getAllTerms = async () => {
  const res = await jwtAxios.get('/api/admin/terms')
  return res.data
}

// 관리자: 새 약관 { title, content, required, version } → { termId }
export const postTerm = async (term) => {
  const res = await jwtAxios.post('/api/admin/terms', term)
  return res.data
}

// 관리자: 새 버전으로 개정 (이전 버전은 사용 중지, 동의 이력과 함께 남는다) → { termId }
export const reviseTerm = async (termId, term) => {
  const res = await jwtAxios.post(`/api/admin/terms/${termId}/revisions`, term)
  return res.data
}

// 관리자: 사용 중지 · 다시 사용 → 바뀐 약관
export const changeTermActive = async (termId, active) => {
  const res = await jwtAxios.patch(`/api/admin/terms/${termId}/active`, { active })
  return res.data
}

// 관리자: 삭제 (동의한 회원이 없을 때만, 본문 없음 204)
export const deleteTerm = async (termId) => {
  await jwtAxios.delete(`/api/admin/terms/${termId}`)
}
