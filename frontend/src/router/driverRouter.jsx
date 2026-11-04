import { lazy, Suspense } from 'react'

const Loading = <div className="p-8 text-gray-500">불러오는 중...</div>

const DriverHome = lazy(() => import('../pages/driver/DriverHomePage'))

// 기사 화면 경로 (/driver/...) — 공통 파일: 화면을 추가할 때 PR로 경로를 더한다
const driverRouter = () => [
  { path: '', element: <Suspense fallback={Loading}><DriverHome /></Suspense> },
]

export default driverRouter
