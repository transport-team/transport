import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { getMembers, patchMemberEnabled } from '../../api/adminMemberApi'
import Modal from '../../components/common/Modal'
import Pagination from '../../components/common/Pagination'
import { ROLE_NAME } from '../../util/roleUtil'

const INPUT_CLASS = 'rounded border border-gray-300 bg-white px-3 py-2 text-sm focus:border-emerald-600 focus:outline-none'

const ENABLED_VALUES = ['true', 'false']

// 주소의 검색 조건 (?role=DRIVER&enabled=false&keyword=김&page=2) — 모르는 값은 기본값으로 (기사 관리와 같은 방식)
const readQuery = (searchParams) => {
  const role = searchParams.get('role') ?? ''
  const enabled = searchParams.get('enabled') ?? ''
  const page = Math.floor(Number(searchParams.get('page')))
  return {
    role: Object.hasOwn(ROLE_NAME, role) ? role : '',
    enabled: ENABLED_VALUES.includes(enabled) ? enabled : '',
    keyword: searchParams.get('keyword') ?? '',
    page: page >= 1 ? page : 1,
  }
}

// 빈 조건은 빼고 주소에 남긴다 (role · enabled · keyword가 없으면 전체, 1쪽이면 page도 뺀다)
const toUrlParams = ({ role, enabled, keyword, page }) => ({
  ...(role && { role }),
  ...(enabled && { enabled }),
  ...(keyword.trim() && { keyword: keyword.trim() }),
  ...(page > 1 && { page: String(page) }),
})

// 관리자 회원 관리 (ADM-10): 역할 · 상태 · 이름/이메일 검색, 비활성화 · 다시 활성화 (관리자 계정은 바꿀 수 없다)
// 검색 조건 · 쪽 번호는 주소에 남긴다 — 새로고침 · 뒤로 가기에도 그대로
const MemberListPage = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const queryString = searchParams.toString()
  const query = useMemo(() => readQuery(new URLSearchParams(queryString)), [queryString])

  const [reloadKey, setReloadKey] = useState(0)       // 같은 조건으로 다시 검색해도 새로 불러온다
  const [pageData, setPageData] = useState(null)
  const [error, setError] = useState('')
  const [confirmId, setConfirmId] = useState(null)    // 비활성화를 확인 중인 회원
  const [changingId, setChangingId] = useState(null)  // 상태를 바꾸는 중인 회원

  useEffect(() => {
    let active = true
    getMembers({ ...toUrlParams(query), page: query.page })
      .then((data) => { if (active) { setPageData(data); setError('') } })
      .catch((err) => { if (active) setError(err.response?.data?.message ?? '회원 목록을 불러오지 못했어요.') })
    return () => { active = false }
  }, [query, reloadKey])

  // 선택 필터는 바꾸면 바로 적용하고, 검색어는 [검색]을 눌러야 적용한다
  const handleFilterChange = (e) => {
    const { name, value } = e.target
    setError('')
    setPageData(null)
    setSearchParams(toUrlParams({ ...query, [name]: value, page: 1 }))
  }

  const handleSearch = (e) => {
    e.preventDefault()
    const keyword = String(new FormData(e.currentTarget).get('keyword') ?? '')
    setError('')
    setPageData(null)
    setReloadKey((key) => key + 1)
    setSearchParams(toUrlParams({ ...query, keyword, page: 1 }))
  }

  const movePage = (page) => {
    setError('')
    setSearchParams(toUrlParams({ ...query, page }))
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

  // 비활성화를 확인 중인 회원 (확인 창은 공통 Modal 하나로 — Esc · 배경 누르기로도 닫힌다)
  const confirmMember = pageData?.dtoList.find((row) => row.id === confirmId) ?? null
  const closeConfirm = () => {
    if (changingId === null) setConfirmId(null)  // 바꾸는 중에는 닫지 않는다
  }

  // 관리 칸: 관리자는 변경 불가, 비활성화는 한 번 더 확인, 다시 활성화는 바로
  const renderAction = (member) => {
    if (member.role === 'ADMIN') {
      return <span className="text-xs text-gray-400">변경 불가</span>
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

      <form key={queryString} onSubmit={handleSearch} className="flex flex-wrap items-end gap-2">
        <select name="role" aria-label="역할" value={query.role} onChange={handleFilterChange} className={INPUT_CLASS}>
          <option value="">전체 역할</option>
          {Object.entries(ROLE_NAME).map(([role, name]) => <option key={role} value={role}>{name}</option>)}
        </select>
        <select name="enabled" aria-label="상태" value={query.enabled} onChange={handleFilterChange} className={INPUT_CLASS}>
          <option value="">전체 상태</option>
          <option value="true">사용 중</option>
          <option value="false">비활성</option>
        </select>
        <input
          name="keyword" aria-label="이름 · 이메일" placeholder="이름 · 이메일 일부" defaultValue={query.keyword}
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

      <Modal open={confirmMember !== null} onClose={closeConfirm} title="회원 비활성화" size="sm">
        <p className="mb-5 text-sm font-medium text-gray-800">
          {confirmMember?.name} 회원의 로그인을 막을까요?
        </p>
        <div className="flex justify-end gap-2">
          <button
            type="button"
            onClick={closeConfirm}
            disabled={changingId !== null}
            className="rounded border border-gray-300 px-4 py-2 text-sm hover:bg-gray-50 disabled:opacity-60"
          >
            취소
          </button>
          <button
            type="button"
            disabled={changingId !== null}
            onClick={() => confirmMember && changeEnabled(confirmMember, false)}
            className="rounded bg-red-600 px-4 py-2 text-sm text-white hover:bg-red-700 disabled:opacity-60"
          >
            {changingId !== null ? '바꾸는 중...' : '비활성화'}
          </button>
        </div>
      </Modal>
    </section>
  )
}

export default MemberListPage
