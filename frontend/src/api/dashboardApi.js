import jwtAxios from '../util/jwtUtil'

// 관리자 대시보드: 오늘 · 지금 · 이번 달 숫자, 처리할 일, 이번 달 차종별 → DashboardDTO
export const getDashboard = async () => {
  const res = await jwtAxios.get('/api/admin/dashboard')
  return res.data
}

// 관리자 일별 결제 완료 건수 · 매출 { from, to } (YYYY-MM-DD, 최대 92일) → [{ date, count, revenue }] (결제가 없는 날은 0)
export const getDailyStats = async (params) => {
  const res = await jwtAxios.get('/api/admin/stats/daily', { params })
  return res.data
}
