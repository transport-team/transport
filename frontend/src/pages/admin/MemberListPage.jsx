import { useEffect, useState } from 'react'
import { getMembers, patchMemberEnabled } from '../../api/adminMemberApi'
import Pagination from '../../components/common/Pagination'
import { ROLE_NAME } from '../../util/roleUtil'

const INPUT_CLASS = 'rounded border border-gray-300 bg-white px-3 py-2 text-sm focus:border-emerald-600 focus:outline-none'

const initSearch = { role: '', enabled: '', keyword: '' }

// 빈 조건은 빼고 보낸다 (role · enabled · keyword가 없으면 전체)
const toParams = ({ role, enabled, keyword, page }) => ({
  page,
  ...(role && { role }),
  ...(enabled && { enabled }),
  ...(keyword.trim() && { keyword: keyword.trim() }),
})

// 관리자 회원 관리 (ADM-10): 역할 · 상태 · 이름/이메일 검색, 비활성화 · 다시 활성화 (관리자 계정은 바꿀 수 없다)
const MemberListPage = () => {
  const [search, setSearch] = useState(initSearch)                // 입력 중인 검색 조건
  const [query, setQuery] = useState({ ...initSearch, page: 1 })  // 실제로 조회한 조건
  const [pageData, setPageData] = useState(null)
  const [error, setError] = useState('')
  const [confirmId, setConfirmId] = useState(null)    // 비활성화를 확인 중인 회원
  const [changingId, setChangingId] = useState(null)  // 상태를 바꾸는 중인 회원

  useEffect(() => {
    getMembers(toParams(query))
      .then(setPageData)
      .catch((err) => setError(err.response?.data?.message ?? '회원 목록을 불러오지 못했어요.'))
  }, [query])

  const handleChange = (e) => setSearch({ ...search, [e.target.name]: e.target.value })

  const handleSearch = (e) => {
    e.preventDefault()
    setError('')
    setQuery({ ...search, page: 1 })
  }

  const movePage = (page) => {
    setError('')
    setQuery({ ...query, page })
  }

  const changeEnabled = async (member, enabled) => {
    setChangingId(member.id)
    setError('')
    try {
      const changed = await patchMemberEnabled(member.id, enabled)
      setPageData({ ...pageData, dtoList: pageData.dtoList.map((row) => (row.id === changed.id ? changed : row)) })
    } catch (err) {
      setError(err.response?.data?.message ?? '회원 상태를 바꾸지 못했어요.')
    } finally {
      setChangingId(null)
      setConfirmId(null)
    }
  }

  // 관리 칸: 관리자는 변경 불가, 비활성화는 한 번 더 확인, 다시 활성화는 바로
  const renderAction = (member) => {
    if (member.role === 'ADMIN') {
      return <span className="text-xs text-gray-400">변경 불가</span>
    }
    if (confirmId === member.id) {
      return (
        <span className="inline-flex items-center gap-2">
          <span className="text-xs text-gray-600">로그인을 막을까요?</span>
          <button
            type="button" disabled={changingId === member.id} onClick={() => changeEnabled(member, false)}
            className="rounded bg-red-600 px-2 py-1 text-xs font-medium text-white hover:bg-red-700 disabled:opacity-60"
          >
            비활성화
          </button>
          <button type="button" onClick={() => setConfirmId(null)} className="rounded border border-gray-300 px-2 py-1 text-xs hover:bg-gray-50">
            취소
          </button>
        </span>
      )
    }
    if (member.enabled) {
      return (
        <button
          type="button" onClick={() => setConfirmId(member.id)}
          className="rounded border border-red-300 px-2 py-1 text-xs text-red-700 hover:bg-red-50"
        >
          비활성화
        </button>
      )
    }
    return (
      <button
        type="button" disabled={changingId === member.id} onClick={() => changeEnabled(member, true)}
        className="rounded border border-emerald-600 px-2 py-1 text-xs text-emerald-800 hover:bg-emerald-50 disabled:opacity-60"
      >
        다시 활성화
      </button>
    )
  }

  return (
    <section className="grid gap-4">
      <h1 className="text-xl font-bold">회원 관리</h1>

      <form onSubmit={handleSearch} className="flex flex-wrap items-end gap-2">
        <select name="role" aria-label="역할" value={search.role} onChange={handleChange} className={INPUT_CLASS}>
          <option value="">전체 역할</option>
          {Object.entries(ROLE_NAME).map(([role, name]) => <option key={role} value={role}>{name}</option>)}
        </select>
        <select name="enabled" aria-label="상태" value={search.enabled} onChange={handleChange} className={INPUT_CLASS}>
          <option value="">전체 상태</option>
          <option value="true">사용 중</option>
          <option value="false">비활성</option>
        </select>
        <input
          name="keyword" aria-label="이름 · 이메일" placeholder="이름 · 이메일 일부" value={search.keyword} onChange={handleChange}
          className={`${INPUT_CLASS} w-56`}
        />
        <button type="submit" className="rounded bg-emerald-700 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-800">
          검색
        </button>
      </form>

      {error && <p role="alert" className="rounded bg-red-50 p-3 text-sm text-red-800">{error}</p>}
      {!pageData && !error && <p className="text-sm text-gray-500">회원 목록 불러오는 중...</p>}

      {pageData && (
        <>
          <p className="text-sm text-gray-600">전체 {pageData.totalCount}명</p>
          <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white">
            <table className="w-full whitespace-nowrap text-left text-sm">
              <thead className="border-b border-gray-200 bg-gray-50 text-gray-600">
                <tr>
                  <th className="px-4 py-3 font-medium">이름</th>
                  <th className="px-4 py-3 font-medium">이메일</th>
                  <th className="px-4 py-3 font-medium">연락처</th>
                  <th className="px-4 py-3 font-medium">역할</th>
                  <th className="px-4 py-3 font-medium">가입일</th>
                  <th className="px-4 py-3 font-medium">상태</th>
                  <th className="px-4 py-3 text-right font-medium">관리</th>
                </tr>
              </thead>
              <tbody>
                {pageData.dtoList.length === 0 && (
                  <tr>
                    <td colSpan={7} className="px-4 py-8 text-center text-gray-500">조건에 맞는 회원이 없어요.</td>
                  </tr>
                )}
                {pageData.dtoList.map((member) => (
                  <tr key={member.id} className="border-b border-gray-100 last:border-0">
                    <td className="px-4 py-3">{member.name}</td>
                    <td className="px-4 py-3">{member.email}</td>
                    <td className="px-4 py-3">{member.phone}</td>
                    <td className="px-4 py-3">{ROLE_NAME[member.role]}</td>
                    <td className="px-4 py-3">{member.createdAt?.slice(0, 10)}</td>
                    <td className="px-4 py-3">
                      <span
                        className={`rounded px-2 py-0.5 text-xs font-medium ${member.enabled ? 'bg-emerald-50 text-emerald-800' : 'bg-gray-100 text-gray-600'}`}
                      >
                        {member.enabled ? '사용 중' : '비활성'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">{renderAction(member)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination pageData={pageData} onMove={movePage} />
        </>
      )}
    </section>
  )
}

export default MemberListPage
