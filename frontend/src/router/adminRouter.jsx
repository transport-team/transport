import { lazy, Suspense } from 'react'

const Loading = <div className="p-8 text-gray-500">불러오는 중...</div>

const AdminHome = lazy(() => import('../pages/admin/AdminHomePage'))

// 관리자 화면 경로 (/admin/...) — 공통 파일: 화면을 추가할 때 PR로 경로를 더한다
const adminRouter = () => [
  { path: '', element: <Suspense fallback={Loading}><AdminHome /></Suspense> },
]

export default adminRouter
