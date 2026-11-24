import jwtAxios from '../util/jwtUtil'
import apiClient from './apiClient'

// 공지 목록 { keyword, page } → PageResponseDTO (로그인 없이)
export const getNotices = async (params) => {
  const res = await apiClient.get('/api/notices', { params })
  return res.data
}

// 공지 상세 (볼 때마다 조회수 + 1)
export const getNotice = async (noticeId) => {
  const res = await apiClient.get(`/api/notices/${noticeId}`)
  return res.data
}

// 관리자: 공지 작성 { title, content } → { noticeId }
export const postNotice = async (notice) => {
  const res = await jwtAxios.post('/api/admin/notices', notice)
  return res.data
}

// 관리자: 공지 수정 → 바뀐 공지
export const putNotice = async (noticeId, notice) => {
  const res = await jwtAxios.put(`/api/admin/notices/${noticeId}`, notice)
  return res.data
}

// 관리자: 공지 삭제 (본문 없음, 204)
export const deleteNotice = async (noticeId) => {
  await jwtAxios.delete(`/api/admin/notices/${noticeId}`)
}
