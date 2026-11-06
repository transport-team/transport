import { Link } from 'react-router-dom'

const CustomerHomePage = () => (
  <section className="space-y-6">
    <div className="rounded-2xl bg-gradient-to-r from-emerald-700 to-teal-600 p-8 text-white shadow-sm">
      <p className="text-sm font-medium text-emerald-100">안전하고 편리한 화물 운송</p>
      <h1 className="mt-2 text-3xl font-bold">어떤 배송을 시작할까요?</h1>
      <p className="mt-3 max-w-xl text-sm text-emerald-50">
        출발지와 도착지를 입력하면 예상 운임을 확인하고 배송을 신청할 수 있어요.
      </p>
      <Link
        to="/customer/shipments/new"
        className="mt-6 inline-flex rounded-lg bg-white px-5 py-3 text-sm font-semibold text-emerald-800 shadow-sm hover:bg-emerald-50"
      >
        배송 신청하기
      </Link>
    </div>

    <div className="grid gap-4 md:grid-cols-3">
      <Link
        to="/customer/shipments/new"
        className="rounded-xl border border-emerald-100 bg-emerald-50 p-5 transition hover:-translate-y-0.5 hover:shadow-sm"
      >
        <span className="text-2xl" aria-hidden="true">📦</span>
        <h2 className="mt-3 font-semibold text-emerald-900">새 배송 신청</h2>
        <p className="mt-1 text-sm text-emerald-700">배송 정보를 입력하고 운임을 확인하세요.</p>
      </Link>
      <div className="rounded-xl border border-blue-100 bg-blue-50 p-5">
        <span className="text-2xl" aria-hidden="true">🚚</span>
        <h2 className="mt-3 font-semibold text-blue-900">배송 현황</h2>
        <p className="mt-1 text-sm text-blue-700">내 배송 메뉴에서 진행 상황을 확인하세요.</p>
      </div>
      <Link
        to="/customer/me"
        className="rounded-xl border border-slate-200 bg-white p-5 transition hover:-translate-y-0.5 hover:shadow-sm"
      >
        <span className="text-2xl" aria-hidden="true">👤</span>
        <h2 className="mt-3 font-semibold text-slate-900">내 정보 관리</h2>
        <p className="mt-1 text-sm text-slate-600">연락처와 비밀번호를 관리하세요.</p>
      </Link>
    </div>

    <div className="rounded-xl border border-border bg-surface p-6">
      <h2 className="text-lg font-semibold text-text">배송 이용 안내</h2>
      <div className="mt-4 grid gap-4 text-sm text-text-secondary md:grid-cols-3">
        <div>
          <strong className="block text-text">01. 배송 정보 입력</strong>
          <span>출발지, 도착지, 화물 정보를 입력해요.</span>
        </div>
        <div>
          <strong className="block text-text">02. 운임 확인·결제</strong>
          <span>서버에서 계산한 예상 운임을 확인해요.</span>
        </div>
        <div>
          <strong className="block text-text">03. 배송 현황 확인</strong>
          <span>배차부터 배송완료까지 확인할 수 있어요.</span>
        </div>
      </div>
    </div>
  </section>
)

export default CustomerHomePage
