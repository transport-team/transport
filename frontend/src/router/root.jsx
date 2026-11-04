import { lazy, Suspense } from 'react'
import { createBrowserRouter } from 'react-router-dom'
import adminRouter from './adminRouter'
import customerRouter from './customerRouter'
import driverRouter from './driverRouter'
import RequireAuth from './RequireAuth'

// 화면 경로 (공통 파일 — 경로 추가는 PR로, 관리: 팀원 1)
// 공개: / · /login · /signup · /notices | 역할별: /customer · /driver · /admin (로그인 + 역할 확인, 하위 경로는 역할별 router 파일)
const Loading = <div className="p-8 text-gray-500">불러오는 중...</div>

const Main = lazy(() => import('../pages/MainPage'))
const Login = lazy(() => import('../pages/LoginPage'))
const Signup = lazy(() => import('../pages/SignupPage'))
const NoticeList = lazy(() => import('../pages/NoticeListPage'))
const NoticeDetail = lazy(() => import('../pages/NoticeDetailPage'))
const CustomerLayout = lazy(() => import('../layouts/CustomerLayout'))
const DriverLayout = lazy(() => import('../layouts/DriverLayout'))
const AdminLayout = lazy(() => import('../layouts/AdminLayout'))

const root = createBrowserRouter([
  {
    path: '',
    element: <Suspense fallback={Loading}><Main /></Suspense>,
  },
  {
    path: 'login',
    element: <Suspense fallback={Loading}><Login /></Suspense>,
  },
  {
    path: 'signup',
    element: <Suspense fallback={Loading}><Signup /></Suspense>,
  },
  {
    path: 'notices',
    element: <Suspense fallback={Loading}><NoticeList /></Suspense>,
  },
  {
    path: 'notices/:id',
    element: <Suspense fallback={Loading}><NoticeDetail /></Suspense>,
  },
  {
    path: 'customer',
    element: <RequireAuth role="CUSTOMER"><Suspense fallback={Loading}><CustomerLayout /></Suspense></RequireAuth>,
    children: customerRouter(),
  },
  {
    path: 'driver',
    element: <RequireAuth role="DRIVER"><Suspense fallback={Loading}><DriverLayout /></Suspense></RequireAuth>,
    children: driverRouter(),
  },
  {
    path: 'admin',
    element: <RequireAuth role="ADMIN"><Suspense fallback={Loading}><AdminLayout /></Suspense></RequireAuth>,
    children: adminRouter(),
  },
])

export default root
