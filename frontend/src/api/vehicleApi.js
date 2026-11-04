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
