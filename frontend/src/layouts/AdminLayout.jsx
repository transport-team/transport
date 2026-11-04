import { Outlet } from 'react-router-dom'
import AppHeader from './AppHeader'
import MenuLink from './MenuLink'

// 관리자 메뉴 — 화면을 만들면 to에 경로를 넣는다 (to가 없으면 준비 중)
const MENUS = [
  { label: '대시보드', to: '/admin' },
  { label: '배송 관리' },
  { label: '배차' },
  { label: '기사 관리' },
  { label: '차량 · 차종', to: '/admin/vehicles' },
  { label: '정산' },
  { label: '회원 관리', to: '/admin/members' },
  { label: '공지 · FAQ · 문의', to: '/admin/notices' },
]

// 관리자 레이아웃: 왼쪽 메뉴 (좁은 화면에서는 위쪽 가로 메뉴)
const AdminLayout = () => (
  <div className="min-h-screen bg-gray-50">
    <AppHeader />
    <div className="mx-auto flex max-w-7xl flex-col gap-6 px-4 py-6 md:flex-row">
      <nav className="flex gap-1 overflow-x-auto md:w-48 md:shrink-0 md:flex-col">
        {MENUS.map((menu) => (
          <MenuLink key={menu.label} menu={menu} className="whitespace-nowrap rounded px-3 py-2 text-sm" />
        ))}
      </nav>
      <main className="min-w-0 flex-1">
        <Outlet />
      </main>
    </div>
  </div>
)

export default AdminLayout
