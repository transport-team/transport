import { lazy, Suspense } from 'react'
import { createBrowserRouter } from 'react-router-dom'
import ErrorPage from '../pages/ErrorPage'
import NotFoundPage from '../pages/NotFoundPage'
import adminRouter from './adminRouter'
import customerRouter from './customerRouter'
import driverRouter from './driverRouter'
import RequireAuth from './RequireAuth'

// 화면 경로 (공통 파일 — 경로 추가는 PR로, 관리: 팀원 1)
// 공개: / · /login · /signup · /notices · /faq | 역할별: /customer · /driver · /admin (로그인 + 역할 확인, 하위 경로는 역할별 router 파일)
// 그 밖의 주소는 404 화면(NotFoundPage), 화면 오류는 오류 화면(ErrorPage) — 이 둘은 화면 파일을 못 불러올 때도 떠야 해서 lazy로 나누지 않는다
const Loading = <div className="p-8 text-gray-500">불러오는 중...</div>

const Main = lazy(() => import('../pages/MainPage'))
const Login = lazy(() => import('../pages/LoginPage'))
const Signup = lazy(() => import('../pages/SignupPage'))
const NoticeList = lazy(() => import('../pages/NoticeListPage'))
const NoticeDetail = lazy(() => import('../pages/NoticeDetailPage'))
const Faq = lazy(() => import('../pages/FaqPage'))
const CustomerLayout = lazy(() => import('../layouts/CustomerLayout'))
const DriverLayout = lazy(() => import('../layouts/DriverLayout'))
const AdminLayout = lazy(() => import('../layouts/AdminLayout'))

const routes = [
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
    path: 'faq',
    element: <Suspense fallback={Loading}><Faq /></Suspense>,
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
  {
    path: '*', // 위 경로에 없는 주소 (역할 경로 아래의 없는 주소도 여기로 온다)
    element: <NotFoundPage />,
  },
]

// 최상위 경로 하나로 감싼다: 어느 화면에서 오류가 나도 errorElement(ErrorPage)가 받는다
const root = createBrowserRouter([{ errorElement: <ErrorPage />, children: routes }])

export default root
