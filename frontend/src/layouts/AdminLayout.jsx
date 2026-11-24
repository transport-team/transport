import { Outlet } from 'react-router-dom'
import AppHeader from './AppHeader'
import MenuLink from './MenuLink'
import { useCallback, useEffect, useState } from 'react'
import { getPendingVehicleRequests, getAllVehicleDeletionRequests } from '../api/vehicleApi'

// 관리자 메뉴 — 화면을 만들면 to에 경로를 넣는다 (to가 없으면 준비 중)
const MENU_GROUPS = [
  {
    title: '현황',
    menus: [
      { label: '대시보드', to: '/admin' },
    ],
  },
  {
    title: '운송 · 정산',
    menus: [
      { label: '배송 관리', to: '/admin/shipments' },
      { label: '배차' },
      { label: '결제 내역', to: '/admin/payments' },
      { label: '정산', to: '/admin/settlements' },
    ],
  },
  {
    title: '기사 · 차량',
    menus: [
      { label: '기사 관리', to: '/admin/drivers' },
      { label: '차량 · 차종', to: '/admin/vehicles' },
      { label: '차종 · 요금 관리', to: '/admin/vehicle-type-fares' },
      { label: '차량 등록 신청함', to: '/admin/vehicle-requests' },
      { label: '차량 삭제 신청함', to: '/admin/vehicle-deletion-requests' },
      { label: '화물 종류', to: '/admin/cargo-types' },
    ],
  },
  {
    title: '회원 · 운영',
    menus: [
      { label: '회원 관리', to: '/admin/members' },
      { label: '약관', to: '/admin/terms' },
      { label: '공지', to: '/admin/notices' },
      { label: 'FAQ', to: '/admin/faqs' },
      { label: '문의', to: '/admin/inquiries' },
    ],
  },
]

// 관리자 레이아웃: 왼쪽 메뉴 (좁은 화면에서는 보기 쉬운 두 칸 메뉴)
const AdminLayout = () => {
  const [pendingCount, setPendingCount] = useState(0)
  const [pendingDeletionCount, setPendingDeletionCount] = useState(0)

  // 승인 대기 중인 차량 등록 신청 건수 조회
  // 상태는 then 안에서 바꾼다 (effect에서 불러도 react-hooks/set-state-in-effect에 걸리지 않게, promise는 그대로 돌려준다)
  const loadPendingCount = useCallback(
    () => getPendingVehicleRequests()
      .then((requests) => setPendingCount(requests.length))
      .catch((err) => console.error('차량 등록 신청 건수 조회 실패', err)),
    [],
  )

  // 처리 대기 중인 차량 삭제 신청 건수 조회
  const loadPendingDeletionCount = useCallback(
    () => getAllVehicleDeletionRequests()
      .then((requests) => setPendingDeletionCount(Array.isArray(requests)
        ? requests.filter((request) => request.status === 'PENDING').length
        : 0))
      .catch((err) => console.error('차량 삭제 신청 건수 조회 실패', err)),
    [],
  )

  useEffect(() => {
    loadPendingCount()
    loadPendingDeletionCount()
  }, [loadPendingCount, loadPendingDeletionCount])

  return (
    <div className="min-h-screen bg-gray-50">
      <AppHeader />
      <div className="mx-auto flex max-w-7xl flex-col gap-6 px-4 py-6 md:flex-row">
        <nav
          aria-label="관리자 메뉴"
          className="w-full shrink-0 self-start rounded-2xl border border-gray-200 bg-white p-3 shadow-sm md:sticky md:top-6 md:w-56"
        >
          {MENU_GROUPS.map((group, index) => (
            <div
              key={group.title}
              className={index === 0 ? '' : 'mt-3 border-t border-gray-100 pt-3'}
            >
              <p className="mb-1 px-3 py-1 text-xs font-semibold tracking-wide text-gray-400">
                {group.title}
              </p>
              <div className="grid grid-cols-2 gap-1 sm:grid-cols-3 md:grid-cols-1">
                {group.menus.map((menu) => (
                  <MenuLink
                    key={menu.label}
                    menu={
                      menu.to === '/admin/vehicle-requests'
                        ? { ...menu, badge: pendingCount }
                        : menu.to === '/admin/vehicle-deletion-requests'
                          ? { ...menu, badge: pendingDeletionCount }
                          : menu
                    }
                    className={`flex min-h-10 items-center rounded-xl px-3 py-2 text-sm transition-colors aria-[current=page]:bg-emerald-50 aria-[current=page]:ring-1 aria-[current=page]:ring-emerald-100 ${menu.to ? 'hover:bg-gray-50' : ''}`}
                  />
                ))}
              </div>
            </div>
          ))}
        </nav>
        <main className="min-w-0 flex-1">
          <Outlet context={{ refreshPendingCount: loadPendingCount, refreshPendingDeletionCount: loadPendingDeletionCount }} />
        </main>
      </div>
    </div>
  )
}

export default AdminLayout
