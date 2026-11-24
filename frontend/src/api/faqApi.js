import jwtAxios from '../util/jwtUtil'
import apiClient from './apiClient'

// FAQ 분류 — 서버 FaqRequestDTO의 검사(배송 · 결제 · 회원)와 같은 값
export const FAQ_CATEGORIES = ['배송', '결제', '회원']

// FAQ 목록 (로그인 없이, 노출 중인 것만 · 순서대로) — category가 없으면 전체
export const getFaqs = async (category) => {
  const res = await apiClient.get('/api/faqs', { params: category ? { category } : {} })
  return res.data
}

// 관리자: FAQ 전체 (숨긴 것 포함)
export const getAllFaqs = async () => {
  const res = await jwtAxios.get('/api/admin/faqs')
  return res.data
}

// 관리자: FAQ 작성 { category, question, answer, sortOrder, active } → { faqId }
export const postFaq = async (faq) => {
  const res = await jwtAxios.post('/api/admin/faqs', faq)
  return res.data
}

// 관리자: FAQ 수정 (숨기기는 active: false) → 바뀐 FAQ
export const putFaq = async (faqId, faq) => {
  const res = await jwtAxios.put(`/api/admin/faqs/${faqId}`, faq)
  return res.data
}

// 관리자: FAQ 삭제 (본문 없음, 204)
export const deleteFaq = async (faqId) => {
  await jwtAxios.delete(`/api/admin/faqs/${faqId}`)
}
