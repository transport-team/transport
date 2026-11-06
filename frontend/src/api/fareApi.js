import jwtAxios from '../util/jwtUtil'

// 고객 배송 신청에 필요한 예상 운임 조회
export const estimateFare = async (fareRequest) => {
    const res = await jwtAxios.post('/api/fares/estimate', fareRequest)

    return res.data
}