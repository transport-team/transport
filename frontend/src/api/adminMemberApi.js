import jwtAxios from '../util/jwtUtil'

// 관리자 회원 목록 { role, enabled, keyword, page } → PageResponseDTO (조건을 빼면 전체)
export const getMembers = async (params) => {
  const res = await jwtAxios.get('/api/admin/members', { params })
  return res.data
}

// 회원 비활성화(false) · 다시 활성화(true) → 바뀐 회원 정보
export const patchMemberEnabled = async (memberId, enabled) => {
  const res = await jwtAxios.patch(`/api/admin/members/${memberId}/enabled`, { enabled })
  return res.data
}
