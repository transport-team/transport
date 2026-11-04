import MyInfoCard from '../../components/member/MyInfoCard'

// 기사 홈 (임시) — 오늘 배차 · 근무 일정 화면이 생기면 바꾼다
const DriverHomePage = () => (
  <section className="grid gap-4">
    <h1 className="text-xl font-bold">기사 홈</h1>
    <p className="text-sm text-gray-600">오늘 배차 · 근무 일정 화면이 이 자리에 들어와요. 아래는 로그인 토큰으로 불러온 내 정보예요.</p>
    <MyInfoCard />
  </section>
)

export default DriverHomePage
