import { isRouteErrorResponse, useRouteError } from 'react-router-dom'
import NotFoundPage from './NotFoundPage'

// 화면 오류 (router/root.jsx의 errorElement): 화면을 그리다 난 오류, 화면 파일을 못 불러온 경우(새 버전 배포 직후 열려 있던 탭 등)
// 고장 난 곳에 기대지 않게 레이아웃 · 로그인 상태를 쓰지 않고, 이동도 라우터 대신 주소 이동(새로 불러오기)으로 한다
// 오류 원인은 React Router가 콘솔에 남긴다 — 화면에는 안내만 보여준다
const ErrorPage = () => {
  const error = useRouteError()

  // 경로가 없어서 난 404는 404 화면으로 (보통은 '*' 경로가 먼저 받는다)
  if (isRouteErrorResponse(error) && error.status === 404) {
    return <NotFoundPage />
  }

  return (
    <main className="grid min-h-screen place-items-center bg-gray-50 px-4">
      <section
        role="alert"
        className="grid max-w-md justify-items-center gap-3 rounded-lg border border-border bg-surface p-8 text-center"
      >
        <h1 className="text-2xl font-bold">화면을 불러오지 못했어요</h1>
        <p className="text-sm text-text-secondary">
          잠시 후 다시 시도해 주세요. 새 버전이 올라온 직후라면 새로고침하면 해결돼요.
        </p>
        <div className="mt-2 flex gap-2">
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="rounded-lg bg-primary px-4 py-2 font-medium text-white hover:bg-primary-hover"
          >
            새로고침
          </button>
          <a
            href="/"
            className="rounded-lg border border-border bg-surface px-4 py-2 font-medium text-text-secondary hover:bg-gray-50"
          >
            메인으로
          </a>
        </div>
      </section>
    </main>
  )
}

export default ErrorPage
