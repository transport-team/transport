import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { API_SERVER_HOST } from '../api/apiClient'
import { getHealth } from '../api/healthApi'
import useCustomLogin from '../hooks/useCustomLogin'
import BasicLayout from '../layouts/BasicLayout'
import { ROLE_HOME, ROLE_NAME } from '../util/roleUtil'

// 이용 흐름 (docs/status.md 상태 흐름을 고객 말로)
const FLOW = ['배송 신청', '결제', '기사 배차', '상차 · 운송', '실시간 위치 확인', '수령 확정 · 리뷰']

// 역할별로 할 수 있는 일
const ROLES = [
  {
    role: '고객',
    items: ['출발지 · 도착지 · 화물로 예상 운임 확인', '토스페이먼츠로 결제', '지원한 기사의 평점 · 리뷰를 보고 선택', '운송 중 기사 위치 · 상태 알림', '인수증 사진 확인 후 수령 확정'],
  },
  {
    role: '기사',
    items: ['차량 등록 · 근무 가능 시간 설정', '운송 요청 지원', '상차 · 인수증 사진으로 단계 처리', '운송 중 위치 공유', '월별 실적 · 평점 확인'],
  },
  {
    role: '관리자',
    items: ['기사 승인 · 중지, 차량 · 차종 요금 관리', '배송 통합 검색 · 배차 취소 · 환불', '결제 내역 · 월 정산 · 지급 처리', '대시보드 통계', '공지 · FAQ · 문의 답변'],
  },
]

// 개발 모드(npm run dev)에서만 보이는 서버 연결 확인 — 배포 화면에는 나오지 않는다
const DevHealth = () => {
  const [health, setHealth] = useState(API_SERVER_HOST ? 'checking' : 'noEnv')

  useEffect(() => {
    if (!API_SERVER_HOST) return
    getHealth()
      .then((data) => setHealth(data.status === 'ok' ? 'ok' : 'fail'))
      .catch(() => setHealth('fail'))
  }, [])

  const text = {
    checking: '서버 확인 중...',
    ok: `서버 연결 정상 (${API_SERVER_HOST})`,
    fail: '서버 연결 실패 — 백엔드(8080) 실행과 frontend/.env의 VITE_API_HOST를 확인하세요',
    noEnv: 'VITE_API_HOST 설정이 없어요 — frontend/.env.example을 .env로 복사하세요',
  }[health]

  return <p className={`mt-10 text-center text-xs ${health === 'ok' ? 'text-gray-400' : 'text-red-600'}`}>개발 모드 · {text}</p>
}

// 메인 화면: 서비스 소개 · 이용 흐름 · 역할별 기능, 로그인 전에는 로그인 · 회원가입, 로그인 뒤에는 내 화면으로
const MainPage = () => {
  const { loginState, isLogin } = useCustomLogin()

  return (
    <BasicLayout>
      <div className="mx-auto max-w-4xl">
        <section className="rounded-2xl bg-emerald-800 px-6 py-12 text-center text-white sm:px-10">
          <p className="text-sm font-semibold text-emerald-200">화물 운송 접수부터 정산까지</p>
          <h1 className="mt-3 text-3xl font-bold sm:text-4xl">운송서비스</h1>
          <p className="mx-auto mt-4 max-w-xl text-sm leading-6 text-emerald-50 sm:text-base">
            배송을 신청하고 결제하면 기사가 지원하고, 운송 중에는 기사 위치를 실시간으로 확인할 수 있어요.
          </p>
          <div className="mt-8 flex flex-wrap justify-center gap-3">
            {isLogin ? (
              <Link to={ROLE_HOME[loginState.role]} className="rounded-lg bg-white px-5 py-2.5 text-sm font-bold text-emerald-800 hover:bg-emerald-50">
                {ROLE_NAME[loginState.role]} 화면으로
              </Link>
            ) : (
              <>
                <Link to="/login" className="rounded-lg bg-white px-5 py-2.5 text-sm font-bold text-emerald-800 hover:bg-emerald-50">
                  로그인
                </Link>
                <Link to="/signup" className="rounded-lg border border-emerald-300 px-5 py-2.5 text-sm font-bold text-white hover:bg-emerald-700">
                  회원가입
                </Link>
              </>
            )}
          </div>
        </section>

        <section className="mt-10">
          <h2 className="text-lg font-bold text-gray-900">이용 흐름</h2>
          <ol className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
            {FLOW.map((step, index) => (
              <li key={step} className="rounded-xl border border-gray-200 bg-white p-4 text-center">
                <span className="mx-auto flex h-7 w-7 items-center justify-center rounded-full bg-emerald-600 text-xs font-bold text-white">{index + 1}</span>
                <p className="mt-2 text-sm font-semibold text-gray-800">{step}</p>
              </li>
            ))}
          </ol>
        </section>

        <section className="mt-10 grid gap-4 md:grid-cols-3">
          {ROLES.map(({ role, items }) => (
            <article key={role} className="rounded-xl border border-gray-200 bg-white p-5">
              <h2 className="text-base font-bold text-emerald-800">{role}</h2>
              <ul className="mt-3 grid gap-2 text-sm text-gray-700">
                {items.map((item) => (
                  <li key={item} className="flex gap-2"><span aria-hidden="true" className="text-emerald-600">✓</span>{item}</li>
                ))}
              </ul>
            </article>
          ))}
        </section>

        {import.meta.env.DEV && <DevHealth />}
      </div>
    </BasicLayout>
  )
}

export default MainPage
