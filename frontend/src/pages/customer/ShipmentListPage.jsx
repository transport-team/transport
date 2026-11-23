import { useEffect, useState } from 'react'
import Pagination from '../../components/common/Pagination'
import { Link } from 'react-router-dom'
import { getMyShipments } from '../../api/shipmentApi'
import { getDispatchApplicantCounts, getDispatchApplicants } from '../../api/dispatchApi'

const STATUS_META = {
  PENDING_PAYMENT: { label: '결제 대기', badge: 'bg-amber-50 text-amber-700 border-amber-200' },
  REQUESTED: { label: '배차 대기', badge: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  ASSIGNED: { label: '배차 완료', badge: 'bg-blue-50 text-blue-700 border-blue-200' },
  PICKED_UP: { label: '상차 완료', badge: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  IN_TRANSIT: { label: '배송 중', badge: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  COMPLETED: { label: '배송 완료', badge: 'bg-gray-100 text-gray-700 border-gray-200' },
  CANCELED: { label: '취소됨', badge: 'bg-red-50 text-red-600 border-red-200' },
}

const FILTER_TABS = [
  { value: '', label: '전체' },
  { value: 'PENDING_PAYMENT', label: '결제 대기' },
  { value: 'REQUESTED', label: '배차 대기' },
  { value: 'ASSIGNED', label: '배차 완료' },
  { value: 'IN_TRANSIT', label: '배송 중' },
  { value: 'COMPLETED', label: '완료' },
  { value: 'CANCELED', label: '취소' },
]

const formatDateTime = (value) => {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

const ShipmentListPage = () => {
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage] = useState(1)
  const [applicantCounts, setApplicantCounts] = useState({})
  const [applicantError, setApplicantError] = useState('')
  const [fetchState, setFetchState] = useState({
    resolvedKey: '',
    content: [],
    totalPages: 0,
    totalCount: 0,
    error: '',
  })

  const requestKey = `${page}-${statusFilter}`
  const loading = fetchState.resolvedKey !== requestKey

  useEffect(() => {
    let active = true

    getMyShipments({ page, size: 10, status: statusFilter })
      .then((data) => {
        if (!active) return
        setFetchState({
          resolvedKey: requestKey,
          content: data.content || data.dtoList || [],
          totalPages: data.totalPages || Math.ceil((data.totalCount || 0) / 10) || 1,
          totalCount: data.totalCount ?? (data.content?.length || 0),
          error: '',
        })
      })
      .catch((err) => {
        if (!active) return
        setFetchState((prev) => ({
          ...prev,
          resolvedKey: requestKey,
          error: err.response?.data?.message || '배송 목록을 불러오지 못했습니다.',
        }))
      })

    return () => { active = false }
  }, [page, statusFilter, requestKey])

  useEffect(() => {
    if (loading || fetchState.error) return undefined
    let active = true
    const pending = fetchState.content.filter((item) => item.status === 'REQUESTED')
    // 전체 집계가 실패하거나 0을 반환해도 현재 화면의 신청은 별도 목록 API로 확인한다.
    Promise.allSettled([
      getDispatchApplicantCounts(),
      ...pending.map((item) => getDispatchApplicants(item.id)),
    ]).then((results) => {
      if (!active) return
      const merged = results[0].status === 'fulfilled' && results[0].value ? { ...results[0].value } : {}
      let failed = results[0].status === 'rejected'
      pending.forEach((item, index) => {
        if (results[index + 1].status === 'fulfilled') merged[item.id] = results[index + 1].value.length
        else failed = true
      })
      setApplicantCounts(merged)
      setApplicantError(failed ? '일부 배차 신청 건수를 불러오지 못했습니다. 새로고침 후에도 계속되면 API 상태를 확인해 주세요.' : '')
    })
    return () => { active = false }
  }, [loading, fetchState.content, fetchState.error])

  const handleSelectStatus = (nextStatus) => {
    setStatusFilter(nextStatus)
    setPage(1)
  }

  return (
    <section className="mx-auto min-h-screen w-full max-w-[640px] bg-gray-50 pb-16">
      <header className="sticky top-0 z-20 border-b border-gray-100 bg-white/95 px-5 py-4 backdrop-blur">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Link className="text-2xl leading-none text-gray-700" to="/customer" aria-label="고객 홈으로">←</Link>
            <h1 className="text-lg font-bold text-gray-900">내 배송 내역</h1>
          </div>
          <Link
            to="/customer/shipments/new"
            className="rounded-full bg-emerald-600 px-3.5 py-1.5 text-xs font-bold text-white hover:bg-emerald-700"
          >
            + 새 배송 신청
          </Link>
        </div>

        <div className="mt-4 flex gap-2 overflow-x-auto pb-1 no-scrollbar">
          {FILTER_TABS.map((tab) => (
            <button
              key={tab.value}
              type="button"
              onClick={() => handleSelectStatus(tab.value)}
              className={`shrink-0 rounded-full px-3.5 py-1.5 text-xs font-bold transition ${
                statusFilter === tab.value
                  ? 'bg-gray-900 text-white'
                  : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </header>

      <div className="space-y-3 px-5 pt-5">
        <Link to="/customer/dispatch-requests" className="flex items-center justify-between rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-bold text-emerald-800 hover:bg-emerald-100">
          <span className="flex items-center gap-2">지원한 기사 및 배차 확정
            {Object.values(applicantCounts).reduce((total, count) => total + Number(count || 0), 0) > 0 && (
              <span className="inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-red-600 px-1.5 text-xs text-white">
                {Object.values(applicantCounts).reduce((total, count) => total + Number(count || 0), 0)}
              </span>
            )}
          </span>
          <span aria-hidden="true">→</span>
        </Link>
        {applicantError && <p role="alert" className="rounded-lg bg-red-50 p-3 text-xs text-red-600">{applicantError}</p>}
        {loading && (
          <div className="rounded-2xl bg-white p-8 text-center text-sm text-gray-500 shadow-sm">
            배송 목록을 불러오는 중입니다...
          </div>
        )}

        {!loading && fetchState.error && (
          <div className="rounded-2xl border border-red-200 bg-red-50 p-5 text-sm font-semibold text-red-600" role="alert">
            {fetchState.error}
          </div>
        )}

        {!loading && !fetchState.error && fetchState.content.length === 0 && (
          <div className="rounded-2xl bg-white p-10 text-center shadow-sm">
            <p className="text-base font-bold text-gray-800">조회된 배송 내역이 없습니다.</p>
            <p className="mt-1 text-xs text-gray-500">새로운 화물 배송을 신청해 보세요.</p>
            <Link
              to="/customer/shipments/new"
              className="mt-5 inline-block rounded-xl bg-emerald-600 px-5 py-2.5 text-sm font-bold text-white hover:bg-emerald-700"
            >
              배송 신청하기
            </Link>
          </div>
        )}

        {!loading && !fetchState.error && fetchState.content.map((item) => {
          const meta = STATUS_META[item.status] || { label: item.status, badge: 'bg-gray-100 text-gray-600 border-gray-200' }
          return (
            <article key={item.id} className="rounded-2xl border border-gray-100 bg-white shadow-sm transition hover:border-emerald-200 hover:shadow-md">
              <Link to={`/customer/shipments/${item.id}`} className="block p-5">
              <div className="flex items-center justify-between gap-2">
                <div className="flex flex-wrap items-center gap-2">
                  <span className={`rounded-full border px-2.5 py-0.5 text-xs font-bold ${meta.badge}`}>
                    {meta.label}
                  </span>
                  {item.status === 'REQUESTED' && Number(applicantCounts[item.id] || 0) > 0 && (
                    <span className="inline-flex items-center gap-1.5 rounded-full bg-red-50 px-2.5 py-1 text-xs font-semibold text-red-700">
                      배차 신청 <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-red-600 px-1 text-white">{applicantCounts[item.id]}</span>
                    </span>
                  )}
                </div>
                <span className="shrink-0 text-xs text-gray-400">상차 {formatDateTime(item.pickupAt)}</span>
              </div>
              <h2 className="mt-3 truncate text-base font-bold text-gray-900">
                {item.cargoName || `배송 #${item.id}`}
              </h2>

              <div className="mt-3 space-y-1.5 rounded-xl bg-gray-50 p-3 text-xs text-gray-600">
                <div className="flex items-center gap-2">
                  <span className="h-2 w-2 shrink-0 rounded-full bg-emerald-600" />
                  <span className="truncate">{item.originAddress}</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="h-2 w-2 shrink-0 rounded-full bg-red-500" />
                  <span className="truncate">{item.destAddress}</span>
                </div>
              </div>

              <div className="mt-3 flex items-center justify-between border-t border-gray-100 pt-3">
                <span className="text-xs text-gray-400">신청일 {formatDateTime(item.createdAt)}</span>
                <span className="text-sm font-black text-gray-900">{Number(item.fare || 0).toLocaleString()}원</span>
              </div>
              </Link>
            </article>
          )
        })}

        {!loading && fetchState.totalPages > 1 && (
          <Pagination page={page} totalPages={fetchState.totalPages} onMove={setPage} />
        )}
      </div>
    </section>
  )
}

export default ShipmentListPage