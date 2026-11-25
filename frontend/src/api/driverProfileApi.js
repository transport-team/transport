import jwtAxios from '../util/jwtUtil'

// 기사 본인 정보 (기사 ID는 서버가 로그인 정보로 확인한다)
export const getMyDriverProfile = async () => {
  const res = await jwtAxios.get('/api/drivers/me')
  return res.data
}

// 수정 가능한 값: 면허 종류 · 경력 · 보유 자격증명 (승인 상태 변경 불가)
export const updateMyDriverProfile = async (values) => {
  const res = await jwtAxios.put('/api/drivers/me', values)
  return res.data
}
