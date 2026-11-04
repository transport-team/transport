import { useEffect, useState } from 'react'
import { getMyInfo } from '../api/memberApi'
import PasswordForm from '../components/member/PasswordForm'
import PhoneForm from '../components/member/PhoneForm'
import { ROLE_NAME } from '../util/roleUtil'

// 내 정보 (고객 · 기사 · 관리자 공통, /{역할}/me): 이름 · 이메일 · 역할은 보기만, 연락처 · 비밀번호는 변경 (COM-03)
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
          <PhoneForm phone={member.phone} onSaved={setMember} />
          <PasswordForm />
        </>
      )}
    </section>
  )
}

export default MyPage
