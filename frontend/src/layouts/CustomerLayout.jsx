import { Outlet } from 'react-router-dom'
import AppHeader from './AppHeader'

// 고객 메뉴 — 화면을 만들면 to에 경로를 넣는다 (to가 없으면 준비 중). 알림은 헤더의 종에서 본다
const MENUS = [
  { label: '홈', to: '/customer' },
  { label: '배송 신청', to: '/customer/shipments/new' },
  { label: '내 배송', to: '/customer/shipments' },
  { label: '배차 요청', to: '/customer/dispatch-requests' },
  { label: '문의', to: '/customer/inquiries' },
]

// 고객 레이아웃: 위쪽 메뉴
const CustomerLayout = () => (
  <div className="min-h-screen bg-gray-50">
    <AppHeader menus={MENUS} />
    <main className="mx-auto w-full min-w-0 max-w-6xl px-4 py-5 sm:px-6 sm:py-7">
      <Outlet />
    </main>
  </div>
)

export default CustomerLayout
