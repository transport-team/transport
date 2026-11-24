import jwtAxios from '../util/jwtUtil'

// 배송 상태 변경 이력 조회 (4-4에서 만든 API)
// 권한: 본인 고객 · 배정 기사 · 관리자만 (아니면 403 FORBIDDEN)
export const getShipmentHistory = async (shipmentId) => {
  const res = await jwtAxios.get(`/api/shipments/${shipmentId}/history`)

  return res.data
}
