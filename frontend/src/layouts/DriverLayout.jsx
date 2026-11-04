import { Outlet } from 'react-router-dom'
import AppHeader from './AppHeader'
import MenuLink from './MenuLink'

// 기사 메뉴 — 화면을 만들면 to에 경로를 넣는다 (to가 없으면 준비 중)
const MENUS = [
  { label: '홈', to: '/driver' },
  { label: '배차' },
  { label: '근무 일정' },
  { label: '실적' },
  { label: '알림' },
]

// 기사 레이아웃: 휴대폰 기준, 아래쪽 탭 (본문은 탭에 가리지 않게 아래 여백)
const DriverLayout = () => (
  <div className="min-h-screen bg-gray-50 pb-20">
    <AppHeader />
    <main className="mx-auto max-w-md px-4 py-4">
      <Outlet />
    </main>
    <nav className="fixed inset-x-0 bottom-0 border-t border-gray-200 bg-white pb-[env(safe-area-inset-bottom)]">
      <div className="mx-auto grid max-w-md grid-cols-5 text-center text-sm">
        {MENUS.map((menu) => <MenuLink key={menu.label} menu={menu} className="py-3" />)}
      </div>
    </nav>
  </div>
)

export default DriverLayout
