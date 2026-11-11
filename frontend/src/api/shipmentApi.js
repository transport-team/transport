import jwtAxios from '../util/jwtUtil'

// 배송 신청: 서버가 운임을 재계산하고 결제 대기 주문을 생성한다.
export const createShipment = async (shipmentRequest) => {
    const res = await jwtAxios.post('/api/shipments', shipmentRequest)

    return res.data
}

// ===== 기사 배송 처리 (팀원 4 — 5-3, 5-4) =====

// 배송 상세 조회 (본인 고객 · 배정 기사 · 관리자)
// NOTE: 백엔드 GET /api/shipments/{id}는 팀원 3 담당(6주차 예정)이라 아직 없다.
//       머지되면 이 함수 하나로 화면이 바로 동작한다.
export const getShipment = async (shipmentId) => {
    const res = await jwtAxios.get(`/api/shipments/${shipmentId}`)

    return res.data
}

// 배송 상태 변경 (배정 기사만) — { nextStatus, memo } → 204
// 400 PHOTO_REQUIRED: 상차는 PICKUP 사진, 완료는 RECEIPT 사진 먼저 업로드
export const changeShipmentStatus = async (shipmentId, nextStatus, memo = '') => {
    await jwtAxios.patch(`/api/shipments/${shipmentId}/status`, { nextStatus, memo })

    return true
}

// 배송 사진 업로드 (배정 기사만) — multipart: files(여러 장), type = PICKUP | RECEIPT | ETC
// 응답: [{ id, type, fileName, url }]
export const uploadShipmentImages = async (shipmentId, files, type) => {
    const formData = new FormData()
    files.forEach((file) => formData.append('files', file))
    formData.append('type', type)

    const res = await jwtAxios.post(`/api/shipments/${shipmentId}/images`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
    })

    return res.data
}
