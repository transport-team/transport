import jwtAxios from '../util/jwtUtil'

// 1:1 문의 등록 { title, content } → { inquiryId } (고객 · 기사)
export const postInquiry = async (inquiry) => {
  const res = await jwtAxios.post('/api/inquiries', inquiry)
  return res.data
}

// 내 문의 { page } → PageResponseDTO (최신순, 답변 포함)
export const getMyInquiries = async (params) => {
  const res = await jwtAxios.get('/api/inquiries/my', { params })
  return res.data
}

// 관리자: 문의 목록 { status, page } → PageResponseDTO (status가 없으면 전체)
export const getInquiries = async (params) => {
  const res = await jwtAxios.get('/api/admin/inquiries', { params })
  return res.data
}

// 관리자: 답변 등록 · 수정 → 바뀐 문의 (첫 답변이면 작성자에게 알림)
export const answerInquiry = async (inquiryId, answer) => {
  const res = await jwtAxios.patch(`/api/admin/inquiries/${inquiryId}/answer`, { answer })
  return res.data
}

// 관리자: 삭제 (스팸 정리, 본문 없음 204)
export const deleteInquiry = async (inquiryId) => {
  await jwtAxios.delete(`/api/admin/inquiries/${inquiryId}`)
}
