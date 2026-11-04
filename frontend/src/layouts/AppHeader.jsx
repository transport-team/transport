import { Link } from 'react-router-dom'
import useCustomLogin from '../hooks/useCustomLogin'
import { ROLE_HOME, ROLE_NAME } from '../util/roleUtil'
import MenuLink from './MenuLink'

// 모든 레이아웃의 윗부분: 로고 · 메뉴 · 로그인 사용자 · 로그아웃
// menus: [{ label, to }] — to가 없으면 아직 화면이 없는 메뉴(준비 중)
const AppHeader = ({ menus = [] }) => {
  const { loginState, isLogin, doLogout } = useCustomLogin()

  return (
    <header className="border-b border-gray-200 bg-white">
      <div className="mx-auto flex max-w-7xl flex-wrap items-center gap-x-6 gap-y-2 px-4 py-3">
        <Link to={isLogin ? ROLE_HOME[loginState.role] : '/'} className="text-lg font-bold text-emerald-800">
          운송서비스
        </Link>
        {menus.length > 0 && (
          <nav className="flex flex-wrap gap-x-4 gap-y-1 text-sm">
            {menus.map((menu) => <MenuLink key={menu.label} menu={menu} />)}
          </nav>
        )}
        <div className="ml-auto flex items-center gap-3 text-sm">
          {isLogin ? (
            <>
              <span className="text-gray-600">{loginState.name} · {ROLE_NAME[loginState.role]}</span>
              <button type="button" onClick={doLogout} className="rounded border border-gray-300 px-3 py-1 hover:bg-gray-50">
                로그아웃
              </button>
            </>
          ) : (
            <Link to="/login" className="rounded bg-emerald-700 px-3 py-1.5 font-medium text-white hover:bg-emerald-800">
              로그인
            </Link>
          )}
        </div>
      </div>
    </header>
  )
}

export default AppHeader
