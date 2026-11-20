import jwtAxios from '../util/jwtUtil'

const cleanParams = (params = {}) =>
  Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== '' && value !== null && value !== undefined),
  )

// ADM-02: 관리자 배송 통합 검색
export const searchAdminShipments = async (params = {}) => {
  const res = await jwtAxios.get('/api/admin/shipments', { params: cleanParams(params) })
  return res.data
}

// 관리자 배차만 해제 (결제·배송 주문 취소 아님)
export const cancelAdminShipmentDispatch = async (shipmentId, reason) => {
  await jwtAxios.post(`/api/admin/shipments/${shipmentId}/dispatch/cancel`, { reason })
}

// ADM-03: 관리자 결제 내역 검색
export const searchAdminPayments = async (params = {}) => {
  const res = await jwtAxios.get('/api/admin/payments', { params: cleanParams(params) })
  return res.data
}

// ADM-08: 관리자 화물 유형 전체 목록 조회 (비활성 포함)
export const getAdminCargoTypes = async () => {
  const res = await jwtAxios.get('/api/admin/cargo-types')
  return res.data
}

// ADM-08: 화물 유형 신규 등록
export const createAdminCargoType = async (payload) => {
  const res = await jwtAxios.post('/api/admin/cargo-types', payload)
  return res.data
}

// ADM-08: 화물 유형 수정
export const updateAdminCargoType = async (id, payload) => {
  const res = await jwtAxios.put(`/api/admin/cargo-types/${id}`, payload)
  return res.data
}

// ADM-08: 화물 유형 활성/비활성 전환
export const changeAdminCargoTypeActive = async (id, active) => {
  const res = await jwtAxios.patch(`/api/admin/cargo-types/${id}/active`, { active })
  return res.data
}