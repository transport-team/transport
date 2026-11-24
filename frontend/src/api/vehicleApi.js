import jwtAxios from '../util/jwtUtil'

// 관리자 차량 목록 조회
export const getVehicleList = async (page = 1, size = 10) => {
    const res = await jwtAxios.get('/api/admin/vehicles', {
        params: { page, size }
    })

    return res.data
}

// 관리자 차량 등록
export const createVehicle = async (vehicleData) => {
    const res = await jwtAxios.post('/api/admin/vehicles', vehicleData)

    return res.data
}

// 관리자 차량 정보 수정
export const updateVehicle = async (id, vehicleData) => {
    const res = await jwtAxios.put(`/api/admin/vehicles/${id}`, vehicleData)

    return res.data
}

// 관리자 차량 비활성화 (DB에서 삭제하지 않음)
export const deactivateVehicle = async (id) => {
    const res = await jwtAxios.delete(`/api/admin/vehicles/${id}`)

    return res.data
}

// 등록된 차종 목록 조회
export const getVehicleTypes = async () => {
    const res = await jwtAxios.get('/api/vehicle-types')

    return res.data
}

// 활성 상태인 기사 목록 조회
export const getActiveDrivers = async () => {
    const res = await jwtAxios.get('/api/admin/drivers', {
        params: {
            page: 1,
            size: 100,
            status: 'ACTIVE',
        },
    })

    return res.data.dtoList ?? []
}

// 기사 차량 등록 신청
export const applyVehicleRegistration = async (vehicleData) => {
    const res = await jwtAxios.post('/api/driver/vehicles/requests', vehicleData)

    return res.data
}

// 관리자: 처리 완료 포함 전체 차량 신청 목록
export const getAllVehicleRequests = async () => {
    const res = await jwtAxios.get('/api/admin/vehicle-requests/all')
    return res.data
}

// 관리자: 승인 대기 중인 차량 등록 신청 목록 조회
export const getPendingVehicleRequests = async () => {
    const res = await jwtAxios.get('/api/admin/vehicle-requests')

    return res.data
}

// 관리자: 차량 등록 신청 승인
export const approveVehicleRequest = async (requestId) => {
    const res = await jwtAxios.patch(`/api/admin/vehicle-requests/${requestId}/approve`)

    return res.data
}

// 관리자: 차량 등록 신청 거절
export const rejectVehicleRequest = async (requestId, rejectionReason) => {
    const res = await jwtAxios.patch(`/api/admin/vehicle-requests/${requestId}/reject`, {
        rejectionReason
    })

    return res.data
}

// 기사: 본인 차량 목록 조회
export const getMyVehicles = async () => {
  const res = await jwtAxios.get('/api/driver/vehicles')

  return res.data
}

// 기사: 본인 차량 운행 상태 변경
export const changeMyVehicleStatus = async (vehicleId, status) => {
  const res = await jwtAxios.patch(
    `/api/driver/vehicles/${vehicleId}/status`,
    { status }
  )
  return res.data
}

// 기사: 나의 차량 등록 신청 내역 조회 및 승인 전 철회
export const getMyVehicleRegistrationRequests = async () => {
  const res = await jwtAxios.get('/api/driver/vehicles/requests')
  return res.data
}

export const withdrawVehicleRegistrationRequest = async (requestId) => {
  await jwtAxios.delete(`/api/driver/vehicles/requests/${requestId}`)
}

// 기사: 차량 삭제 요청 및 내역 (실제 차량은 DB에서 삭제되지 않음)
export const getMyVehicleDeletionRequests = async () => {
  const res = await jwtAxios.get('/api/driver/vehicles/deletion-requests')
  return res.data
}

export const applyVehicleDeletion = async (vehicleId) => {
  const res = await jwtAxios.post(`/api/driver/vehicles/deletion-requests/${vehicleId}`)
  return res.data
}

export const withdrawVehicleDeletionRequest = async (requestId) => {
  await jwtAxios.delete(`/api/driver/vehicles/deletion-requests/${requestId}`)
}

// 관리자: 차량 삭제 요청 목록과 승인·거절
export const getAllVehicleDeletionRequests = async () => {
  const res = await jwtAxios.get('/api/admin/vehicle-deletion-requests')
  return res.data
}

export const approveVehicleDeletion = async (requestId) => {
  await jwtAxios.patch(`/api/admin/vehicle-deletion-requests/${requestId}/approve`)
}

export const rejectVehicleDeletion = async (requestId, rejectionReason) => {
  await jwtAxios.patch(`/api/admin/vehicle-deletion-requests/${requestId}/reject`, { rejectionReason })
}
