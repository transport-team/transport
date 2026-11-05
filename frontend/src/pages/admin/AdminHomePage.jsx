import MyInfoCard from '../../components/member/MyInfoCard'

// 관리자 홈 (임시) — 대시보드(T단계)가 생기면 바꾼다
const AdminHomePage = () => (
  <section className="grid max-w-xl gap-4">
    <h1 className="text-xl font-bold">관리자 대시보드</h1>
    <p className="text-sm text-gray-600">오늘 접수 · 매출 같은 대시보드가 이 자리에 들어와요. 아래는 로그인 토큰으로 불러온 내 정보예요.</p>
    <MyInfoCard />
  </section>
)

export default AdminHomePage
