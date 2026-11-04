import { lazy, Suspense } from 'react'

const Loading = <div className="p-8 text-gray-500">불러오는 중...</div>

const AdminHome = lazy(() => import('../pages/admin/AdminHomePage'))
const MyPage = lazy(() => import('../pages/MyPage'))

// 차량 관리 페이지
const VehicleListPage = lazy(() => import('../pages/admin/VehicleListPage'))
// 차량 등록 페이지
const VehicleRegisterPage = lazy(() => import('../pages/admin/VehicleRegisterPage'))

// 관리자 화면 경로 (/admin/...) — 공통 파일: 화면을 추가할 때 PR로 경로를 더한다
const adminRouter = () => [
  { path: '', element: <Suspense fallback={Loading}><AdminHome /></Suspense> },
  { path: 'me', element: <Suspense fallback={Loading}><MyPage /></Suspense> },
  { path: 'vehicles', element: <Suspense fallback={Loading}><VehicleListPage /></Suspense> }, // 차량 관리 경로
  { path: 'vehicles/new', element: <Suspense fallback={Loading}><VehicleRegisterPage /></Suspense> },
]

export default adminRouter
