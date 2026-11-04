import { lazy, Suspense } from 'react'

const Loading = <div className="p-8 text-gray-500">불러오는 중...</div>

const CustomerHome = lazy(() => import('../pages/customer/CustomerHomePage'))

// 고객 화면 경로 (/customer/...) — 공통 파일: 화면을 추가할 때 PR로 경로를 더한다
const customerRouter = () => [
  { path: '', element: <Suspense fallback={Loading}><CustomerHome /></Suspense> },
]

export default customerRouter
