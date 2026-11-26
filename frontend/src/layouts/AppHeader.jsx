import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import NotificationBell from '../components/common/NotificationBell'
import useCustomLogin from '../hooks/useCustomLogin'
import { ROLE_HOME, ROLE_NAME } from '../util/roleUtil'
import MenuLink from './MenuLink'
import jimgoLogo from '../assets/jimgo-logo.webp'

// 알림을 눌렀을 때 갈 배송 화면 (역할별). 고객 · 관리자는 배송 상세 화면이 생기면 더한다
const NOTIFICATION_TARGET = {
  DRIVER: (shipmentId) => `/driver/dispatches/${shipmentId}`,
}

// 모든 레이아웃의 윗부분: 로고 · 메뉴 · 공지사항 · FAQ · 알림 종 · 로그인 사용자(누르면 내 정보) · 로그아웃 (로그인 전에는 회원가입 · 로그인)
// menus: [{ label, to }] — to가 없으면 아직 화면이 없는 메뉴(준비 중)
const AppHeader = ({ menus = [] }) => {
  const { loginState, isLogin, doLogout } = useCustomLogin()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  // 경로가 바뀌면 모바일 메뉴는 자동으로 닫힌 상태가 된다.
  const [openedOnPath, setOpenedOnPath] = useState(null)
  const mobileMenuOpen = openedOnPath === pathname
  const toTarget = NOTIFICATION_TARGET[loginState.role]

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 shadow-sm backdrop-blur-md">
      {/* 알림 드롭다운이 잘리지 않도록 헤더 자체에 overflow를 지정하지 않는다. */}
      <div className="mx-auto max-w-6xl px-4 py-3 sm:px-6">
        <div className="flex min-h-11 items-center gap-3">
          <Link
            to={isLogin ? ROLE_HOME[loginState.role] : '/'}
            className="inline-flex shrink-0 items-center rounded-lg focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600"
            aria-label="JIMGO 짐고 홈으로"
          >
            <img src={jimgoLogo} alt="JIMGO 짐고" className="block h-8 w-auto sm:h-10" />
          </Link>

          {menus.length > 0 && (
            <nav aria-label="역할별 메뉴" className="hidden min-w-0 flex-1 items-center gap-1 overflow-x-auto whitespace-nowrap xl:flex">
              {menus.map((menu) => (
                <MenuLink
                  key={menu.label}
                  menu={menu}
                  className="shrink-0 rounded-xl px-3 py-2 text-sm transition-colors aria-[current=page]:bg-blue-50"
                />
              ))}
            </nav>
          )}

          <div className="ml-auto flex shrink-0 items-center gap-2 sm:gap-3">
            <div className="hidden items-center gap-4 text-sm xl:flex">
              <Link to="/notices" className="text-slate-600 hover:text-blue-800">공지사항</Link>
              <Link to="/faq" className="text-slate-600 hover:text-blue-800">FAQ</Link>
            </div>
            {isLogin ? (
              <>
                {/* 알림 훅이 중복 실행되지 않도록 모바일·PC에서 종은 하나만 렌더링한다. */}
                <NotificationBell onNavigate={toTarget ? (shipmentId) => navigate(toTarget(shipmentId)) : undefined} />
                <Link
                  to={`${ROLE_HOME[loginState.role]}/me`}
                  title="내 정보"
                  className="hidden max-w-48 truncate text-sm font-medium text-slate-700 hover:text-blue-800 xl:inline-block"
                >
                  {loginState.name} · {ROLE_NAME[loginState.role]}
                </Link>
                <button
                  type="button"
                  onClick={doLogout}
                  className="hidden min-h-10 rounded-xl border border-slate-200 px-3 text-sm font-medium text-slate-700 transition hover:bg-slate-50 xl:inline-flex xl:items-center"
                >
                  로그아웃
                </button>
              </>
            ) : (
              <>
                <Link to="/signup" className="hidden text-sm font-medium text-slate-600 hover:text-blue-800 xl:inline-block">회원가입</Link>
                <Link to="/login" className="inline-flex min-h-10 items-center rounded-xl bg-blue-700 px-4 text-sm font-semibold text-white transition hover:bg-blue-800">로그인</Link>
              </>
            )}
            <button
              type="button"
              aria-label={mobileMenuOpen ? '메뉴 닫기' : '메뉴 열기'}
              aria-controls="jimgo-mobile-navigation"
              aria-expanded={mobileMenuOpen}
              onClick={() => setOpenedOnPath(mobileMenuOpen ? null : pathname)}
              className="inline-flex h-10 w-10 items-center justify-center rounded-xl border border-slate-200 text-slate-700 transition hover:bg-blue-50 focus-visible:outline-2 focus-visible:outline-blue-600 xl:hidden"
            >
              {mobileMenuOpen ? (
                <svg aria-hidden="true" className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M5 5l14 14M19 5 5 19" /></svg>
              ) : (
                <svg aria-hidden="true" className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M4 7h16M4 12h16M4 17h16" /></svg>
              )}
            </button>
          </div>
        </div>

        <div id="jimgo-mobile-navigation" className={`${mobileMenuOpen ? 'mt-3 border-t border-slate-100 pt-3' : 'hidden'} xl:hidden`}>
          {menus.length > 0 && (
            <nav aria-label="모바일 역할별 메뉴" className="grid grid-cols-2 gap-2 pb-3 sm:grid-cols-3" onClick={() => setOpenedOnPath(null)}>
              {menus.map((menu) => (
                <MenuLink
                  key={menu.label}
                  menu={menu}
                  className="flex min-h-11 items-center rounded-xl border border-slate-100 bg-slate-50 px-3 text-sm aria-[current=page]:border-blue-100 aria-[current=page]:bg-blue-50"
                />
              ))}
            </nav>
          )}
          <div className="grid grid-cols-2 gap-2 border-t border-slate-100 pt-3 text-sm">
            <Link to="/notices" onClick={() => setOpenedOnPath(null)} className="flex min-h-11 items-center rounded-xl px-3 text-slate-600 hover:bg-slate-50">공지사항</Link>
            <Link to="/faq" onClick={() => setOpenedOnPath(null)} className="flex min-h-11 items-center rounded-xl px-3 text-slate-600 hover:bg-slate-50">FAQ</Link>
            {isLogin ? (
              <>
                <Link to={`${ROLE_HOME[loginState.role]}/me`} onClick={() => setOpenedOnPath(null)} className="col-span-2 flex min-h-11 items-center rounded-xl bg-blue-50 px-3 font-semibold text-blue-800">
                  내 정보 · {loginState.name} ({ROLE_NAME[loginState.role]})
                </Link>
                <button type="button" onClick={() => { setOpenedOnPath(null); doLogout() }} className="col-span-2 min-h-11 rounded-xl border border-slate-200 bg-white px-3 text-left font-medium text-slate-700">로그아웃</button>
              </>
            ) : (
              <Link to="/signup" onClick={() => setOpenedOnPath(null)} className="col-span-2 flex min-h-11 items-center rounded-xl bg-blue-50 px-3 font-semibold text-blue-800">회원가입</Link>
            )}
          </div>
        </div>
      </div>
    </header>
  )
}

export default AppHeader
