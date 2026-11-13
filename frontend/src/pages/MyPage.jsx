import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getMyInfo } from '../api/memberApi'
import PasswordForm from '../components/member/PasswordForm'
import PhoneForm from '../components/member/PhoneForm'
import DefaultAddressForm from '../components/member/DefaultAddressForm'
import { ROLE_HOME, ROLE_NAME } from '../util/roleUtil'

// 내 정보 (고객 · 기사 · 관리자 공통, /{역할}/me): 이름 · 이메일 · 역할은 보기만, 연락처 · 비밀번호는 변경 (COM-03)
// 고객 · 기사는 1:1 문의로 가는 링크 (기사는 아래 탭이 꽉 차서 여기서 들어간다)
const MyPage = () => {
  const [member, setMember] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getMyInfo()
      .then(setMember)
      .catch((err) => setError(err.response?.data?.message ?? '내 정보를 불러오지 못했어요.'))
  }, [])

  return (
    <section className="grid max-w-xl gap-4">
      <h1 className="text-xl font-bold">내 정보</h1>
      {error && <p role="alert" className="rounded border border-red-200 bg-red-50 p-4 text-sm text-red-800">{error}</p>}
      {!member && !error && <p className="text-sm text-gray-500">내 정보 불러오는 중...</p>}
      {member && (
        <>
          <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2 rounded-lg border border-gray-200 bg-white p-4 text-sm">
            <dt className="text-gray-500">이름</dt>
            <dd>{member.name}</dd>
            <dt className="text-gray-500">이메일</dt>
            <dd className="break-all">{member.email}</dd>
            <dt className="text-gray-500">역할</dt>
            <dd>{ROLE_NAME[member.role]}</dd>
          </dl>
          {/* 기사 전용 차량 관리 메뉴 */}
          {member.role === 'DRIVER' && (
            <Link
              to="/driver/vehicles"
              className="flex items-center justify-between rounded-lg border border-gray-200 bg-white p-4 text-sm hover:border-blue-300 hover:bg-blue-50"
            >
              <span>
                <span className="block font-semibold">
                  🚚 내 차량 관리
                </span>
                <span className="text-gray-500">
                  등록 차량 확인 및 운행 상태 관리
                </span>
              </span>
              <span aria-hidden="true" className="text-gray-400">
                ›
              </span>
            </Link>
          )}

          <PhoneForm phone={member.phone} onSaved={setMember} />
          {member.role === 'CUSTOMER' && <DefaultAddressForm />}
          <PasswordForm />

          {member.role !== 'ADMIN' && (
            <Link
              to={`${ROLE_HOME[member.role]}/inquiries`}
              className="flex items-center justify-between rounded-lg border border-gray-200 bg-white p-4 text-sm hover:border-emerald-600"
            >
              <span>
                <span className="block font-semibold">1:1 문의</span>
                <span className="text-gray-500">궁금한 점을 남기면 관리자가 답해 드려요.</span>
              </span>
              <span aria-hidden="true" className="text-gray-400">›</span>
            </Link>
          )}
        </>
      )}
    </section>
  )
}

export default MyPage
