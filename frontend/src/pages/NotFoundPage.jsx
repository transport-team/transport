import { useSelector } from 'react-redux'
import { Link } from 'react-router-dom'
import BasicLayout from '../layouts/BasicLayout'
import { ROLE_HOME } from '../util/roleUtil'

// 없는 주소 (404): router/root.jsx의 path: '*'와 ErrorPage가 쓴다
// "홈으로"는 로그인했으면 내 역할 첫 화면, 아니면 메인으로 간다
const NotFoundPage = () => {
  const role = useSelector((state) => state.loginSlice.role)
  const home = ROLE_HOME[role] ?? '/'

  return (
    <BasicLayout>
      <section className="mx-auto grid max-w-md justify-items-center gap-3 py-16 text-center">
        <p className="text-sm font-semibold text-primary">404</p>
        <h1 className="text-2xl font-bold">페이지를 찾을 수 없어요</h1>
        <p className="text-sm text-text-secondary">주소가 바뀌었거나 없는 페이지예요. 주소를 다시 확인해 주세요.</p>
        <Link
          to={home}
          className="mt-2 rounded-lg bg-primary px-4 py-2 font-medium text-white hover:bg-primary-hover"
        >
          홈으로
        </Link>
      </section>
    </BasicLayout>
  )
}

export default NotFoundPage
