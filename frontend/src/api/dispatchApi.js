import jwtAxios from '../util/jwtUtil'

// 기사: 배차 대기 중인 운송 요청 목록 조회
export const getAvailableShipments = async () => {
  const res = await jwtAxios.get('/api/driver/shipments')

  return res.data
}

// 기사: 운송 요청에 지원
export const applyDispatch = async (shipmentId, vehicleId) => {
  const res = await jwtAxios.post(
    `/api/driver/shipments/${shipmentId}/applications`,
    { vehicleId }
  )

  return res.data
}

// 고객: 자신의 배송에 지원한 기사 목록 조회
export const getDispatchApplicants = async (shipmentId) => {
  const res = await jwtAxios.get(
    `/api/customer/shipments/${shipmentId}/applications`
  )

  return res.data
}

// 고객: 지원 기사 선택 및 배차 확정
export const selectDispatchApplicant = async (shipmentId, applicationId) => {
  const res = await jwtAxios.post(
    `/api/customer/shipments/${shipmentId}/applications/${applicationId}/select`
  )

  return res.data
}
