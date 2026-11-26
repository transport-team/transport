import { Link } from 'react-router-dom'

const QUICK_LINKS = [
  { to: '/customer/shipments/new', icon: '📦', title: '새 배송 신청', description: '출발지와 도착지를 입력하고 운임을 확인해요.', tone: 'bg-orange-50 text-orange-700' },
  { to: '/customer/shipments', icon: '🚚', title: '내 배송 현황', description: '배차부터 수령 완료까지 진행 상황을 확인해요.', tone: 'bg-blue-50 text-blue-700' },
  { to: '/customer/me', icon: '👤', title: '내 정보 관리', description: '저장한 배송지와 내 정보, 리뷰를 관리해요.', tone: 'bg-slate-100 text-slate-700' },
]

const CustomerHomePage = () => (
  <section className="min-w-0 space-y-7 pb-4 sm:space-y-9">
    <div className="relative isolate overflow-hidden rounded-[26px] bg-gradient-to-br from-[#09265b] via-[#123c91] to-[#255bb7] px-5 py-9 text-white shadow-lg shadow-blue-950/10 sm:rounded-[30px] sm:px-9 sm:py-12 lg:px-12">
      <div aria-hidden="true" className="pointer-events-none absolute -right-16 -top-28 -z-10 h-72 w-72 rounded-full border-[44px] border-white/5 sm:h-96 sm:w-96" />
      <div aria-hidden="true" className="pointer-events-none absolute -bottom-36 right-1/4 -z-10 h-72 w-72 rounded-full bg-blue-400/10 blur-3xl" />
      <div className="max-w-xl">
        <p className="inline-flex rounded-full border border-white/20 bg-white/10 px-3 py-1 text-xs font-semibold tracking-wide text-blue-100">JIMGO · 고객 서비스</p>
        <h1 className="mt-4 text-[clamp(1.65rem,5vw,2.5rem)] font-extrabold leading-tight tracking-tight">화물 운송, 이제
          <span className="block">짐고에서 간편하게.</span>
        </h1>
        <p className="mt-4 max-w-md text-sm leading-7 text-blue-100 sm:text-base">
          출발지와 도착지를 입력하면 예상 운임을 확인하고 배송을 신청할 수 있어요.
        </p>
        <Link
          to="/customer/shipments/new"
          className="mt-7 inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-brand-orange px-6 text-sm font-bold text-white shadow-md shadow-blue-950/10 transition hover:bg-brand-orange-hover focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white sm:w-auto"
        >
          배송 신청하기 <span aria-hidden="true">→</span>
        </Link>
      </div>
    </div>

    <div aria-labelledby="quick-links-heading">
      <div className="mb-4 flex flex-wrap items-end justify-between gap-2">
        <h2 id="quick-links-heading" className="text-xl font-extrabold tracking-tight text-slate-900 sm:text-2xl">무엇을 도와드릴까요?</h2>
        <p className="text-xs text-slate-500 sm:text-sm">필요한 메뉴로 빠르게 이동하세요.</p>
      </div>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {QUICK_LINKS.map(({ to, icon, title, description, tone }) => (
          <Link
            key={to}
            to={to}
            className="group flex min-w-0 items-start gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition duration-200 hover:-translate-y-0.5 hover:border-blue-200 hover:shadow-lg hover:shadow-blue-900/5 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-700 lg:flex-col"
          >
            <span aria-hidden="true" className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl text-2xl ${tone}`}>{icon}</span>
            <span className="min-w-0 flex-1">
              <span className="flex items-center justify-between gap-2 text-base font-bold text-slate-900">
                {title} <span aria-hidden="true" className="text-blue-600 transition group-hover:translate-x-1">→</span>
              </span>
              <span className="mt-1 block text-sm leading-6 text-slate-600">{description}</span>
            </span>
          </Link>
        ))}
      </div>
    </div>

    <div className="rounded-2xl border border-slate-200 bg-white px-5 py-6 shadow-sm sm:px-7" aria-labelledby="customer-steps-heading">
      <h2 id="customer-steps-heading" className="text-lg font-bold text-slate-900">배송 이용 안내</h2>
      <div className="mt-5 grid gap-5 sm:grid-cols-3 sm:gap-4">
        {[
          ['01', '배송 정보 입력', '출발지, 도착지, 화물 정보를 입력해요.'],
          ['02', '운임 확인·결제', '서버에서 계산한 예상 운임을 확인해요.'],
          ['03', '배송 현황 확인', '배차부터 배송완료까지 확인할 수 있어요.'],
        ].map(([number, title, description]) => (
          <div key={number} className="flex items-start gap-3 sm:block">
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-xs font-extrabold text-blue-700 sm:mb-3">{number}</span>
            <div>
              <h3 className="font-semibold text-slate-900">{title}</h3>
              <p className="mt-1 text-sm leading-6 text-slate-600">{description}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  </section>
)

export default CustomerHomePage
