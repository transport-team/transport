import { Link } from 'react-router-dom'
import MyInfoCard from '../../components/member/MyInfoCard'

// 기사 홈 (임시) — 오늘 배차 · 근무 일정 화면이 생기면 바꾼다
const DriverHomePage = () => (
  <section className="grid gap-4">
    <h1 className="text-xl font-bold">기사 홈</h1>
    <p className="text-sm text-gray-600">오늘 배차 · 근무 일정 화면이 이 자리에 들어와요. 아래는 로그인 토큰으로 불러온 내 정보예요.</p>
    <MyInfoCard />

    {/* 차량 등록 신청 페이지 이동 */}
    <Link
      to="/driver/vehicles/register"
      className="block rounded-xl border border-blue-200 bg-white p-4 text-center font-semibold text-blue-600 hover:bg-blue-50"
    >
      + 차량 등록 신청
    </Link>
    
    {/* 운송 요청 목록 페이지 이동 */}
    <Link
      to="/driver/shipments"
      className="block rounded-xl border border-blue-200 bg-white p-4 text-center font-semibold text-blue-600 hover:bg-blue-50"
    >
      🚛 운송 요청 목록 보기
    </Link>

  </section>
)

export default DriverHomePage