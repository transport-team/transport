import { lazy, Suspense } from 'react'

const Loading = <div className="p-8 text-gray-500">불러오는 중...</div>

const AdminHome = lazy(() => import('../pages/admin/AdminHomePage'))
const MyPage = lazy(() => import('../pages/MyPage'))
const MemberList = lazy(() => import('../pages/admin/MemberListPage'))
const DriverList = lazy(() => import('../pages/admin/DriverListPage'))
const TermManage = lazy(() => import('../pages/admin/TermManagePage'))
const NoticeManage = lazy(() => import('../pages/admin/NoticeManagePage'))
const FaqManage = lazy(() => import('../pages/admin/FaqManagePage'))
const InquiryManage = lazy(() => import('../pages/admin/InquiryManagePage'))
const VehicleEditPage = lazy(() => import('../pages/admin/VehicleEditPage'))
const VehicleListPage = lazy(() => import('../pages/admin/VehicleListPage'))
const VehicleRegisterPage = lazy(() => import('../pages/admin/VehicleRegisterPage'))
const VehicleRequestListPage = lazy(() => import('../pages/admin/VehicleRequestListPage'))
const VehicleDeletionRequestListPage = lazy(() => import('../pages/admin/VehicleDeletionRequestListPage'))

// 팀원 3 관리자 화면
const ShipmentManagePage = lazy(() => import('../pages/admin/ShipmentManagePage'))
const AdminShipmentDetailPage = lazy(() => import('../pages/admin/AdminShipmentDetailPage'))
const PaymentManagePage = lazy(() => import('../pages/admin/PaymentManagePage'))
const AdminSettlementPage = lazy(() => import('../pages/admin/AdminSettlementPage'))
const CargoTypeManagePage = lazy(() => import('../pages/admin/CargoTypeManagePage'))
const VehicleTypeFareManagePage = lazy(() => import('../pages/admin/VehicleTypeFareManagePage'))

// 관리자 화면 경로 (/admin/...) — 공통 파일: 화면을 추가할 때 PR로 경로를 더한다
const adminRouter = () => [
  { path: '', element: <Suspense fallback={Loading}><AdminHome /></Suspense> },
  { path: 'me', element: <Suspense fallback={Loading}><MyPage /></Suspense> },
  { path: 'shipments', element: <Suspense fallback={Loading}><ShipmentManagePage /></Suspense> },
  { path: 'shipments/:id', element: <Suspense fallback={Loading}><AdminShipmentDetailPage /></Suspense> },
  { path: 'payments', element: <Suspense fallback={Loading}><PaymentManagePage /></Suspense> },
  { path: 'settlements', element: <Suspense fallback={Loading}><AdminSettlementPage /></Suspense> },
  { path: 'cargo-types', element: <Suspense fallback={Loading}><CargoTypeManagePage /></Suspense> },
  { path: 'vehicle-type-fares', element: <Suspense fallback={Loading}><VehicleTypeFareManagePage /></Suspense> },
  { path: 'vehicles', element: <Suspense fallback={Loading}><VehicleListPage /></Suspense> },
  { path: 'vehicles/new', element: <Suspense fallback={Loading}><VehicleRegisterPage /></Suspense> },
  { path: 'vehicles/:id/edit', element: <Suspense fallback={Loading}><VehicleEditPage /></Suspense> },
  { path: 'members', element: <Suspense fallback={Loading}><MemberList /></Suspense> },
  { path: 'drivers', element: <Suspense fallback={Loading}><DriverList /></Suspense> },
  { path: 'terms', element: <Suspense fallback={Loading}><TermManage /></Suspense> },
  { path: 'notices', element: <Suspense fallback={Loading}><NoticeManage /></Suspense> },
  { path: 'faqs', element: <Suspense fallback={Loading}><FaqManage /></Suspense> },
  { path: 'inquiries', element: <Suspense fallback={Loading}><InquiryManage /></Suspense> },
  { path: 'vehicle-requests', element: <Suspense fallback={Loading}><VehicleRequestListPage /></Suspense> },
  { path: 'vehicle-deletion-requests', element: <Suspense fallback={Loading}><VehicleDeletionRequestListPage /></Suspense> },
]

export default adminRouter