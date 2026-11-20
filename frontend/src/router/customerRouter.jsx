import { lazy, Suspense } from 'react'

const Loading = <div className="p-8 text-gray-500">불러오는 중...</div>

const CustomerHome = lazy(() => import('../pages/customer/CustomerHomePage'))
const ShipmentCreate = lazy(() => import('../pages/customer/ShipmentCreatePage'))
const ShipmentList = lazy(() => import('../pages/customer/ShipmentListPage'))
const ShipmentDetail = lazy(() => import('../pages/customer/ShipmentDetailPage'))
const CustomerDispatchRequests = lazy(() => import('../pages/customer/CustomerDispatchRequestsPage'))
const MyPage = lazy(() => import('../pages/MyPage'))
const Inquiry = lazy(() => import('../pages/InquiryPage'))
const PaymentSuccess = lazy(() => import('../pages/customer/PaymentSuccessPage'))
const PaymentFail = lazy(() => import('../pages/customer/PaymentFailPage'))

// 고객 화면 경로 (/customer/...) — 공통 파일: 화면을 추가할 때 PR로 경로를 더한다
const customerRouter = () => [
  { path: '', element: <Suspense fallback={Loading}><CustomerHome /></Suspense> },
  { path: 'shipments', element: <Suspense fallback={Loading}><ShipmentList /></Suspense> },
  { path: 'shipments/new', element: <Suspense fallback={Loading}><ShipmentCreate /></Suspense> },
  { path: 'shipments/:id', element: <Suspense fallback={Loading}><ShipmentDetail /></Suspense> },
  { path: 'dispatch-requests', element: <Suspense fallback={Loading}><CustomerDispatchRequests /></Suspense> },
  { path: 'me', element: <Suspense fallback={Loading}><MyPage /></Suspense> },
  { path: 'inquiries', element: <Suspense fallback={Loading}><Inquiry /></Suspense> },
  { path: 'payments/success', element: <Suspense fallback={Loading}><PaymentSuccess /></Suspense> },
  { path: 'payments/fail', element: <Suspense fallback={Loading}><PaymentFail /></Suspense> },
]

export default customerRouter