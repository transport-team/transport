import { Outlet } from 'react-router-dom'
import AppHeader from './AppHeader'

// 고객 메뉴 — 화면을 만들면 to에 경로를 넣는다 (to가 없으면 준비 중)
const MENUS = [
  { label: '홈', to: '/customer' },
  { label: '배송 신청' },
  { label: '내 배송' },
  { label: '알림' },
  { label: '문의', to: '/customer/inquiries' },
]

// 고객 레이아웃: 위쪽 메뉴
const CustomerLayout = () => (
  <div className="min-h-screen bg-gray-50">
    <AppHeader menus={MENUS} />
    <main className="mx-auto max-w-7xl px-4 py-6">
      <Outlet />
    </main>
  </div>
)

export default CustomerLayout
