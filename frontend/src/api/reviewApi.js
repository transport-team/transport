import jwtAxios from '../util/jwtUtil'

// 리뷰 작성 (8-1) — { rating, content } → ReviewResponseDTO
export const createReview = async (shipmentId, { rating, content }) => {
    const res = await jwtAxios.post(`/api/shipments/${shipmentId}/review`, { rating, content })

    return res.data
}

// 배송의 리뷰 조회 — 없으면 204 → null 반환
export const getShipmentReview = async (shipmentId) => {
    try {
        const res = await jwtAxios.get(`/api/shipments/${shipmentId}/review`)

        return res.data
    } catch (err) {
        if (err.response?.status === 204) return null
        throw err
    }
}
