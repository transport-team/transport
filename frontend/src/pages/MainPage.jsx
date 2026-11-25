import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { API_SERVER_HOST } from '../api/apiClient'
import { getHealth } from '../api/healthApi'
import useCustomLogin from '../hooks/useCustomLogin'
import BasicLayout from '../layouts/BasicLayout'
import { ROLE_HOME, ROLE_NAME } from '../util/roleUtil'
import truckHero from '../assets/jimgo-highway-truck.webp'

const BENEFITS = [
  { title: '편리한 기사 매칭', description: '운송 요청에 지원한 기사를 확인하고 선택할 수 있어요.', icon: 'bolt', accent: true },
  { title: '실시간 운송 확인', description: '기사 위치와 배송 진행 상황을 한눈에 확인해요.', icon: 'pin' },
  { title: '안전한 수령 확인', description: '인수증 사진과 수령 확정으로 운송을 마무리해요.', icon: 'shield' },
  { title: '체계적인 정산 관리', description: '운송 내역과 정산 현황을 간편하게 관리해요.', icon: 'chart', accent: true },
]

// 역할별로 할 수 있는 일
const ROLES = [
  {
    role: '고객', sub: '화주', icon: 'box', tone: 'bg-blue-50',
    items: ['화물 운송 요청 및 결제', '기사 지원 내역 확인 및 선택', '운송 진행 상황 실시간 조회'],
  },
  {
    role: '기사', sub: '운송기사', icon: 'truck', tone: 'bg-orange-50',
    items: ['차량 등록 및 운행 관리', '운송 요청 지원 및 배차', '운송 진행 및 인수증 등록'],
  },
  {
    role: '관리자', sub: '', icon: 'chart', tone: 'bg-indigo-50',
    items: ['회원 및 기사 승인 관리', '운송 현황 및 정산 관리', '차량 · 요금 등 서비스 운영'],
  },
]

// 외부 아이콘 패키지 없이 메인 화면에 필요한 선형 아이콘만 사용
const FlowIcon = ({ name, className = 'h-8 w-8' }) => {
  const paths = {
    truck: <><path d="M2.5 6.5h12v11h-12zM14.5 10h4l3 3v4.5h-7" /><circle cx="7" cy="18" r="1.8" /><circle cx="18" cy="18" r="1.8" /></>,
    pin: <><path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 1 1 14 0Z" /><circle cx="12" cy="10" r="2.4" /></>,
    bolt: <path d="m13 2-9 11h7l-1 9 10-12h-7l1-8Z" />,
    shield: <><path d="M12 2.5 20 6v6.5c0 4.5-3.6 7.8-8 9-4.4-1.2-8-4.5-8-9V6l8-3.5Z" /><path d="m8.5 12 2.5 2.5 4.5-5" /></>,
    chart: <><rect x="3" y="4" width="18" height="16" rx="2" /><path d="M7 16v-3M12 16V9M17 16v-5" /></>,
    box: <><path d="m12 2 9 5v10l-9 5-9-5V7l9-5Z" /><path d="m3 7 9 5 9-5M12 12v10" /></>,
  }

  return (
    <svg aria-hidden="true" className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      {paths[name]}
    </svg>
  )
}

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

// 메인 화면: 서비스 소개 · 장점 · 역할별 기능, 로그인 뒤에는 역할별 화면으로
const MainPage = () => {
  const { loginState, isLogin } = useCustomLogin()
  const isCustomer = isLogin && loginState.role === 'CUSTOMER'
  const roleHome = ROLE_HOME[loginState.role] || '/'
  const requestTo = isLogin ? (isCustomer ? '/customer/shipments/new' : roleHome) : '/login'
  const trackingTo = isLogin ? (isCustomer ? '/customer/shipments' : roleHome) : '/login'
  const requestLabel = isLogin && !isCustomer ? `${ROLE_NAME[loginState.role] || '내'} 화면으로` : '배송 신청하기'

  return (
    <BasicLayout>
      <div className="mx-auto max-w-6xl pb-8">
        <section className="relative isolate flex min-h-[360px] items-center overflow-hidden rounded-3xl bg-blue-950 text-white shadow-sm sm:min-h-[420px] lg:min-h-[450px]">
          <img src={truckHero} alt="" className="absolute inset-0 -z-20 h-full w-full object-cover object-[68%_60%] sm:object-[65%_58%]" />
          <div className="absolute inset-0 -z-10 bg-gradient-to-r from-[#082051]/95 via-[#0c337c]/70 via-[42%] to-transparent to-[82%] max-sm:bg-[#082051]/75" />
          <div className="w-full px-6 py-10 sm:px-10 lg:px-14">
            <div className="max-w-lg">
              <p className="text-sm font-semibold tracking-wide text-orange-300">화물 운송 매칭 플랫폼</p>
              <h1 className="mt-4 text-3xl font-extrabold leading-tight tracking-tight sm:text-4xl lg:text-5xl">
                짐고와 함께<br />빠르고 안전한 <span className="text-orange-400">화물 운송</span>
              </h1>
              <p className="mt-5 max-w-md text-sm leading-7 text-blue-50 sm:text-base">
                배송 신청부터 기사 매칭, 운송 진행, 수령 확인까지 한 번에 이용하세요.
              </p>
              <div className="mt-7 flex flex-wrap gap-3">
                <Link to={requestTo} className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-brand-orange px-6 py-3 text-sm font-bold text-white shadow-md transition hover:bg-brand-orange-hover focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white">
                  <FlowIcon name="truck" className="h-5 w-5" />{requestLabel}
                </Link>
                <Link to={trackingTo} className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-white px-6 py-3 text-sm font-bold text-blue-900 shadow-sm transition hover:bg-blue-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white">
                  <FlowIcon name="pin" className="h-5 w-5" />{isLogin && !isCustomer ? '내 화면 확인' : '운송 조회하기'}
                </Link>
              </div>
            </div>
          </div>
        </section>

        <section className="mt-12 rounded-3xl bg-gradient-to-r from-blue-50 to-indigo-50 px-5 py-8 sm:px-8" aria-labelledby="benefits-heading">
          <h2 id="benefits-heading" className="text-2xl font-extrabold text-blue-950">왜 JIMGO인가요?</h2>
          <p className="mt-2 text-sm text-slate-600">화물 운송을 더 편리하고, 더 안심할 수 있도록.</p>
          <div className="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {BENEFITS.map(({ title, description, icon, accent }) => (
              <article key={title} className="rounded-2xl border border-blue-100 bg-white px-5 py-6 text-center shadow-sm">
                <FlowIcon name={icon} className={`mx-auto h-9 w-9 ${accent ? 'text-brand-orange' : 'text-blue-700'}`} />
                <h3 className="mt-4 font-bold text-blue-950">{title}</h3>
                <p className="mt-2 text-sm leading-6 text-slate-600">{description}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="mt-12" aria-labelledby="roles-heading">
          <div className="flex flex-wrap items-end justify-between gap-2">
            <div>
              <h2 id="roles-heading" className="text-2xl font-extrabold text-blue-950">이용 대상</h2>
              <span className="mt-2 block h-1 w-9 rounded-full bg-blue-700" />
            </div>
            <p className="text-sm text-slate-500">고객, 기사, 관리자 모두를 위한 플랫폼이에요.</p>
          </div>
          <div className="mt-5 grid gap-4 md:grid-cols-3">
            {ROLES.map(({ role, sub, icon, tone, items }) => (
              <article key={role} className={`rounded-2xl border border-slate-200 p-5 shadow-sm ${tone}`}>
                <div className="flex items-center gap-3">
                  <span className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-white text-blue-800 shadow-sm">
                    <FlowIcon name={icon} className="h-8 w-8" />
                  </span>
                  <h3 className="text-xl font-extrabold text-blue-950">{role}{sub && <span className="ml-1 text-sm font-semibold text-blue-800">({sub})</span>}</h3>
                </div>
                <ul className="mt-5 space-y-2.5 text-sm text-slate-700">
                  {items.map((item) => (
                    <li key={item} className="flex items-start gap-2">
                      <span aria-hidden="true" className="mt-0.5 font-bold text-blue-700">✓</span>
                      {item}
                    </li>
                  ))}
                </ul>
              </article>
            ))}
          </div>
        </section>

        <section className="mt-12 rounded-3xl bg-gradient-to-r from-blue-950 to-blue-800 px-6 py-9 text-white sm:flex sm:items-center sm:justify-between sm:gap-8 sm:px-10" aria-labelledby="start-heading">
          <div>
            <h2 id="start-heading" className="text-2xl font-extrabold sm:text-3xl">지금 바로 <span className="text-orange-400">JIMGO</span>를 시작해보세요!</h2>
            <p className="mt-3 max-w-lg text-sm leading-6 text-blue-100">화물 운송이 필요하다면 간편하게 요청하고 기사 매칭부터 수령 확인까지 한 번에!</p>
          </div>
          <div className="mt-6 flex shrink-0 flex-wrap gap-3 sm:mt-0">
            <Link to={requestTo} className="inline-flex min-h-11 items-center justify-center rounded-xl bg-brand-orange px-5 py-3 text-sm font-bold text-white transition hover:bg-brand-orange-hover">
              {requestLabel} <span aria-hidden="true" className="ml-2">→</span>
            </Link>
            {!isLogin && <Link to="/signup" className="inline-flex min-h-11 items-center justify-center rounded-xl bg-white px-5 py-3 text-sm font-bold text-blue-900 transition hover:bg-blue-50">회원가입 <span aria-hidden="true" className="ml-2">→</span></Link>}
          </div>
        </section>

        {import.meta.env.DEV && <DevHealth />}
      </div>
    </BasicLayout>
  )
}

export default MainPage
