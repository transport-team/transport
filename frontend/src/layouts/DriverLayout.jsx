import { Outlet } from 'react-router-dom'
import AppHeader from './AppHeader'

// 기사 메뉴 — 화면을 만들면 to에 경로를 넣는다 (to가 없으면 준비 중). 알림은 헤더의 종에서 본다
const MENUS = [
  { label: '홈', to: '/driver' },
  { label: '운송 요청', to: '/driver/shipments' },
  { label: '내 배차', to: '/driver/dispatches' },
  { label: '실적', to: '/driver/settlements' },
]

// 기사 레이아웃: 고객·관리자와 동일한 상단 메뉴 사용
const DriverLayout = () => (
  <div className="min-h-screen bg-gray-50">
    <AppHeader menus={MENUS} />
    <main className="mx-auto w-full max-w-6xl px-4 py-6">
      <Outlet />
    </main>
  </div>
)

export default DriverLayout
