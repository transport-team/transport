import jwtAxios from '../util/jwtUtil'

// 배송 신청: 서버가 운임을 재계산하고 결제 대기 주문을 생성한다.
export const createShipment = async (shipmentRequest) => {
    const res = await jwtAxios.post('/api/shipments', shipmentRequest)

    return res.data
}
