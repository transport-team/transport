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

// 기사: 배차 확정 전 본인의 배차 신청 취소
export const cancelDispatch = async (shipmentId) => {
  await jwtAxios.delete(
    `/api/driver/shipments/${shipmentId}/applications/my`
  )

  return true
}

// 기사: 본인의 배차 확정 목록 조회
export const getMyAssignedDispatches = async () => {
  const res = await jwtAxios.get('/api/driver/dispatches')

  return res.data
}

// 기사: 본인의 확정 배차 상세 조회
export const getMyAssignedDispatch = async (shipmentId) => {
  const res = await jwtAxios.get(`/api/driver/dispatches/${shipmentId}`)

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

// 기사: 본인의 지난 배차 신청 및 배송 내역 조회
export const getMyDispatchHistory = async () => {
  const res = await jwtAxios.get('/api/driver/dispatches/history')

  return res.data
}

// 기사: 지난 배차 내역 상세 조회
export const getMyDispatchHistoryDetail = async (applicationId) => {
  const res = await jwtAxios.get(
    `/api/driver/dispatches/history/${applicationId}`
  )

  return res.data
}