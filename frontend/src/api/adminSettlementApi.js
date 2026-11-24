import jwtAxios from '../util/jwtUtil'

// ADM-09: 월 마감은 자동 스케줄러만 실행한다. 실제 계좌이체는 별도 시스템에서 수행.
export const getAdminMonthlySettlements = async (month) => {
  const res = await jwtAxios.get('/api/admin/settlements', { params: { month } })
  return res.data
}

export const getAdminMonthlyTrend = async (endMonth) => {
  const res = await jwtAxios.get('/api/admin/settlements/trend', { params: { endMonth } })
  return res.data
}

export const getAdminSettlementDifferences = async (month, driverId) => {
  const res = await jwtAxios.get('/api/admin/settlements/differences', { params: { month, driverId } })
  return res.data
}

export const markAdminSettlementPaid = async (id, transferReference) => {
  const res = await jwtAxios.post(`/api/admin/settlements/${id}/paid`, { transferReference })
  return res.data
}
