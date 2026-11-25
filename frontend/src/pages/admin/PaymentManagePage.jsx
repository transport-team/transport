import { useEffect, useMemo, useState } from 'react'
import { searchAdminPayments } from '../../api/adminTransportApi'
import { useSearchParams } from 'react-router-dom'
import Pagination from '../../components/common/Pagination'
import { readSearchQuery, toSearchParams } from '../../util/searchQuery'
import { PAYMENT_STATUS_VALUES, paymentStatusBadge, paymentStatusLabel } from '../../util/paymentStatusUtil'

// 결제 상태 필터 — 서버 PaymentStatus 값 그대로 (전에는 서버에 없는 PENDING · FAILED를 보내 400, util/paymentStatusUtil.js)
const PAYMENT_STATUS_OPTIONS = [
  { value: '', label: '전체 결제상태' },
  ...PAYMENT_STATUS_VALUES.map((value) => ({ value, label: paymentStatusLabel(value) })),
]

const formatDateTime = (value) => (value ? value.replace('T', ' ').slice(0, 16) : '-')

const EMPTY_PAGE = { dtoList: [], pageNumList: [], prev: false, next: false, prevPage: 0, nextPage: 0, totalCount: 0 }

const PaymentManagePage = () => {
  // 검색 조건 · 쪽은 주소에 남긴다 (?status=&from=&to=&keyword=&page=) — 새로고침 · 뒤로 가기에도 그대로 (11-25, 팀원 1 목록 #152와 같은 방식)
  const [searchParams, setSearchParams] = useSearchParams()
  const queryString = searchParams.toString()
  const appliedFilter = useMemo(() => readSearchQuery(new URLSearchParams(queryString), PAYMENT_STATUS_VALUES), [queryString])
  const [reloadKey, setReloadKey] = useState(0)  // 같은 조건으로 [검색]을 다시 눌러도 새로 불러오고, [초기화]는 입력 칸도 비운다
  const [fetchState, setFetchState] = useState({
    resolvedKey: '',
    data: EMPTY_PAGE,
    error: '',
  })

  const requestKey = `${JSON.stringify(appliedFilter)}#${reloadKey}`
  const loading = fetchState.resolvedKey !== requestKey

  useEffect(() => {
    let active = true

    searchAdminPayments({ ...appliedFilter, size: 10 })
      .then((res) => {
        if (!active) return
        setFetchState({ resolvedKey: requestKey, data: res, error: '' })
      })
      .catch((err) => {
        if (!active) return
        setFetchState((prev) => ({
          ...prev,
          resolvedKey: requestKey,
          data: EMPTY_PAGE,  // 오류 때는 이전 검색의 건수 · 쪽 버튼을 남기지 않는다
          error: err.response?.data?.message || '결제 내역 조회에 실패했습니다.',
        }))
      })

    return () => { active = false }
  }, [appliedFilter, requestKey])

  // 검색: 입력 칸 값을 주소로 (1쪽부터)
  const handleSearch = (e) => {
    e.preventDefault()
    const form = new FormData(e.currentTarget)
    setReloadKey((key) => key + 1)
    setSearchParams(toSearchParams({
      status: String(form.get('status') ?? ''),
      from: String(form.get('from') ?? ''),
      to: String(form.get('to') ?? ''),
      keyword: String(form.get('keyword') ?? ''),
      page: 1,
    }))
  }

  const handleReset = () => {
    setReloadKey((key) => key + 1)
    setSearchParams({})
  }

  const { data, error } = fetchState

  return (
    <section className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-gray-900">결제 내역 관리</h1>
          <p className="mt-0.5 text-xs text-gray-500">주문번호, 결제 상태, 승인·취소 일시 및 환불 사유를 조회합니다.</p>
        </div>
        <span className="rounded-lg bg-white px-3 py-1.5 text-xs font-semibold text-gray-600 shadow-xs">
          총 <strong className="text-blue-700">{data.totalCount}</strong>건
        </span>
      </div>

      {/* 검색 필터 바 */}
      {/* 주소가 바뀌면(뒤로 가기 · 초기화) 폼을 새로 만들어 주소의 조건으로 채운다 */}
      <form key={`${queryString}#${reloadKey}`} onSubmit={handleSearch} className="rounded-xl border border-gray-200 bg-white p-4 shadow-xs">
        <div className="grid grid-cols-1 gap-3 md:grid-cols-5">
          <select
            name="status"
            defaultValue={appliedFilter.status}
            className="rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
          >
            {PAYMENT_STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>

          <input
            type="date"
            name="from"
            defaultValue={appliedFilter.from}
            aria-label="시작일"
            className="rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
          />

          <input
            type="date"
            name="to"
            defaultValue={appliedFilter.to}
            aria-label="종료일"
            className="rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
          />

          <input
            type="text"
            name="keyword"
            defaultValue={appliedFilter.keyword}
            placeholder="주문번호, 배송번호, 고객명, 화물명"
            className="rounded-lg border border-gray-200 px-3 py-2 text-sm md:col-span-2 focus:border-blue-500 focus:outline-none"
          />
        </div>

        <div className="mt-3 flex justify-end gap-2">
          <button
            type="button"
            onClick={handleReset}
            className="rounded-lg border border-gray-200 px-4 py-1.5 text-xs font-semibold text-gray-600 hover:bg-gray-50"
          >
            초기화
          </button>
          <button
            type="submit"
            className="rounded-lg bg-blue-600 px-4 py-1.5 text-xs font-bold text-white hover:bg-blue-700"
          >
            검색
          </button>
        </div>
      </form>

      {/* 결제 테이블 */}
      <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-xs">
        {loading ? (
          <div className="p-10 text-center text-sm text-gray-500">결제 내역을 불러오는 중입니다...</div>
        ) : error ? (
          <div className="p-8 text-center text-sm font-semibold text-red-600">{error}</div>
        ) : data.dtoList.length === 0 ? (
          <div className="p-10 text-center text-sm text-gray-500">조건에 맞는 결제 내역이 없습니다.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="border-b border-gray-200 bg-gray-50 text-gray-600">
                <tr>
                  <th className="px-4 py-3 font-semibold">결제 / 배송</th>
                  <th className="px-4 py-3 font-semibold">주문번호</th>
                  <th className="px-4 py-3 font-semibold">결제 상태</th>
                  <th className="px-4 py-3 font-semibold">고객 · 화물</th>
                  <th className="px-4 py-3 text-right font-semibold">결제 금액</th>
                  <th className="px-4 py-3 font-semibold">승인 / 취소 일시</th>
                  <th className="px-4 py-3 font-semibold">비고 (취소사유)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {data.dtoList.map((item) => (
                  <tr key={item.id} className="hover:bg-gray-50/70">
                    <td className="px-4 py-3">
                      <div className="font-bold text-gray-900">결제 #{item.id}</div>
                      <div className="text-[11px] text-blue-700">배송 #{item.shipmentId}</div>
                    </td>
                    <td className="max-w-[170px] truncate px-4 py-3 font-mono text-[11px] text-gray-700" title={item.orderId}>
                      {item.orderId}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-block rounded-full border px-2.5 py-0.5 font-bold ${paymentStatusBadge(item.status)}`}>
                        {paymentStatusLabel(item.status)}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="font-semibold text-gray-900">{item.customerName}</div>
                      <div className="text-[11px] text-gray-500">{item.cargoName}</div>
                    </td>
                    <td className="px-4 py-3 text-right font-bold text-gray-900">
                      {Number(item.amount).toLocaleString()}원
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-gray-600">
                      <div>승인: {formatDateTime(item.paidAt)}</div>
                      {item.canceledAt && <div className="text-red-500">취소: {formatDateTime(item.canceledAt)}</div>}
                    </td>
                    <td className="max-w-[180px] truncate px-4 py-3 text-gray-500" title={item.cancelReason || ''}>
                      {item.cancelReason || '-'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* 페이지네이션 */}
      {!loading && data.pageNumList?.length > 0 && (
        <Pagination pageData={data} onMove={(page) => setSearchParams(toSearchParams({ ...appliedFilter, page }))} />
      )}
    </section>
  )
}

export default PaymentManagePage