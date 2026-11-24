import jwtAxios from '../util/jwtUtil'

// 관리자 기사 목록 { status, keyword, page } → PageResponseDTO (조건을 빼면 전체, 최신 가입 순)
export const getDrivers = async (params) => {
  const res = await jwtAxios.get('/api/admin/drivers', { params })
  return res.data
}

// 기사 승인 · 다시 승인(ACTIVE), 거절 · 중지(INACTIVE) → 바뀐 기사 정보 (승인 대기로는 되돌릴 수 없다)
export const patchDriverStatus = async (driverId, status) => {
  const res = await jwtAxios.patch(`/api/admin/drivers/${driverId}/status`, { status })
  return res.data
}

// 관리자: 기사별 차량번호 · 차종 · 등록일 확인
export const getAdminDriverVehicles = async (driverId) => {
  const res = await jwtAxios.get(`/api/admin/drivers/${driverId}/vehicles`)
  return res.data
}

// 기사 리뷰 목록 + 평균 평점 (팀원 4 API, driverId는 driver.id) → { reviews, averageRating }
export const getDriverReviews = async (driverId) => {
  const res = await jwtAxios.get(`/api/drivers/${driverId}/reviews`)
  return res.data
}
