import MyInfoCard from '../../components/member/MyInfoCard'

// 고객 홈 (임시) — 배송 신청 · 내 배송 화면이 생기면 바꾼다
const CustomerHomePage = () => (
  <section className="grid max-w-xl gap-4">
    <h1 className="text-xl font-bold">고객 홈</h1>
    <p className="text-sm text-gray-600">배송 신청 · 내 배송 화면이 이 자리에 들어와요. 아래는 로그인 토큰으로 불러온 내 정보예요.</p>
    <MyInfoCard />
  </section>
)

export default CustomerHomePage
