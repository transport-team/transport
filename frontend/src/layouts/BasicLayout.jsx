import AppHeader from './AppHeader'

// 공개 화면 레이아웃 (메인 · 로그인 · 회원가입)
const BasicLayout = ({ children }) => (
  <div className="min-h-screen bg-gray-50">
    <AppHeader />
    <main className="mx-auto w-full min-w-0 max-w-6xl px-4 py-6 sm:px-6 sm:py-8">{children}</main>
  </div>
)

export default BasicLayout
