import { lazy, Suspense } from 'react'
import { createBrowserRouter } from 'react-router-dom'

// 화면 경로 (공통 파일 — 경로 추가는 PR로, 관리: 팀원 1)
// 역할별 경로(/customer · /driver · /admin)와 레이아웃은 K단계에서 추가한다
const Loading = <div className="p-8 text-gray-500">불러오는 중...</div>

const Main = lazy(() => import('../pages/MainPage'))

const root = createBrowserRouter([
  {
    path: '',
    element: <Suspense fallback={Loading}><Main /></Suspense>,
  },
])

export default root
