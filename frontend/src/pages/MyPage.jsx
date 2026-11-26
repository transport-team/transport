import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getMyInfo } from '../api/memberApi'
import PasswordForm from '../components/member/PasswordForm'
import DriverProfileSection from '../components/driver/DriverProfileSection'
import PhoneForm from '../components/member/PhoneForm'
import DefaultAddressForm from '../components/member/DefaultAddressForm'
import CustomerWrittenReviews from '../components/review/CustomerWrittenReviews'
import { ROLE_HOME, ROLE_NAME } from '../util/roleUtil'

// 내 정보 (고객 · 기사 · 관리자 공통, /{역할}/me): 이름 · 이메일 · 역할은 보기만, 연락처 · 비밀번호는 변경 (COM-03)
// 고객 · 기사는 1:1 문의로 가는 링크 (기사는 아래 탭이 꽉 차서 여기서 들어간다)
const MyPage = () => {
  const [member, setMember] = useState(null)
  const [error, setError] = useState('')
  const [activeEdit, setActiveEdit] = useState(null)

  const toggleEdit = (section) => {
    setActiveEdit((current) => current === section ? null : section)
  }

  useEffect(() => {
    getMyInfo()
      .then(setMember)
      .catch((err) => setError(err.response?.data?.message ?? '내 정보를 불러오지 못했어요.'))
  }, [])

  return (
    <section className="mx-auto grid w-full min-w-0 max-w-6xl gap-4 sm:gap-5">
      <h1 className="text-2xl font-extrabold tracking-tight text-slate-900">내 정보</h1>
      {error && <p role="alert" className="rounded border border-red-200 bg-red-50 p-4 text-sm text-red-800">{error}</p>}
      {!member && !error && <p className="text-sm text-gray-500">내 정보 불러오는 중...</p>}
      {member && (
        <>
          <dl className="grid min-w-0 grid-cols-[auto_minmax(0,1fr)] gap-x-5 gap-y-3 rounded-2xl border border-slate-200 bg-white p-5 text-sm shadow-sm sm:gap-x-8 sm:p-6">
            <dt className="text-gray-500">이름</dt>
            <dd>{member.name}</dd>
            <dt className="text-gray-500">이메일</dt>
            <dd className="break-all">{member.email}</dd>
            <dt className="text-gray-500">역할</dt>
            <dd>{ROLE_NAME[member.role]}</dd>
          </dl>
          {member.role === 'DRIVER' && <DriverProfileSection />}
          {/* 기사 전용 차량 관리 메뉴 */}
          {member.role === 'DRIVER' && (
            <Link
              to="/driver/vehicles"
              className="flex min-w-0 items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white p-4 text-sm shadow-sm transition hover:border-blue-300 hover:bg-blue-50"
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

          {member.role === 'CUSTOMER' && <CustomerWrittenReviews />}
          {member.role === 'CUSTOMER' && <DefaultAddressForm />}

          <div className="grid grid-cols-2 gap-2" aria-label="내 정보 변경 메뉴">
            {[
              { id: 'phone', label: '연락처 변경' },
              { id: 'password', label: '비밀번호 변경' },
            ].map(({ id, label }) => (
              <button
                key={id}
                type="button"
                onClick={() => toggleEdit(id)}
                aria-expanded={activeEdit === id}
                className={`min-h-11 rounded-xl border px-2 py-2.5 text-sm font-semibold transition-colors sm:px-3 ${activeEdit === id
                  ? 'border-blue-700 bg-blue-700 text-white'
                  : 'border-gray-200 bg-white text-gray-700 hover:border-blue-500 hover:text-blue-700'}`}
              >
                {label} {activeEdit === id ? '접기' : ''}
              </button>
            ))}
          </div>
          {activeEdit === 'phone' && <PhoneForm phone={member.phone} onSaved={setMember} />}
          {activeEdit === 'password' && <PasswordForm />}

          {member.role !== 'ADMIN' && (
            <Link
              to={`${ROLE_HOME[member.role]}/inquiries`}
              className="flex min-w-0 items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white p-4 text-sm shadow-sm transition hover:border-blue-600"
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
