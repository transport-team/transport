import jwtAxios from '../util/jwtUtil'

// 리뷰 작성 (8-1) — { rating, content } → ReviewResponseDTO
export const createReview = async (shipmentId, { rating, content }) => {
    const res = await jwtAxios.post(`/api/shipments/${shipmentId}/review`, { rating, content })

    return res.data
}

// 배송의 리뷰 조회 — 아직 없으면 204 → null 반환
// (axios는 204를 오류로 보지 않으므로 응답 상태로 구분한다 — addressApi.getDefaultAddress와 같은 방식)
export const getShipmentReview = async (shipmentId) => {
    const res = await jwtAxios.get(`/api/shipments/${shipmentId}/review`)

    return res.status === 204 ? null : res.data
}

// 기사 리뷰 목록 + 평균 평점 (8-2) — 팀원 2가 배차 정렬에 씀
// 반환: { reviews: ReviewResponseDTO[], averageRating: number }
export const getDriverReviews = async (driverId) => {
    const res = await jwtAxios.get(`/api/drivers/${driverId}/reviews`)
    return res.data
}
