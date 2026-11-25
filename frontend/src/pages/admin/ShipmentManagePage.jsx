import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { searchAdminShipments } from '../../api/adminTransportApi'
import Pagination from '../../components/common/Pagination'
import StatusBadge from '../../components/common/StatusBadge'
import { SHIPMENT_STATUS } from '../../util/shipmentStatusUtil'

// 상태 필터: 8개 상태 전부, 진행 순서대로 (값 · 이름은 배지와 같은 공통 표)
const STATUS_OPTIONS = [
  { value: '', label: '전체 상태' },
  ...Object.entries(SHIPMENT_STATUS).map(([value, { label }]) => ({ value, label })),
]

const formatDateTime = (value) => (value ? value.replace('T', ' ').slice(0, 16) : '-')

const ShipmentManagePage = () => {
  const navigate = useNavigate()
  const [formInputs, setFormInputs] = useState({ status: '', from: '', to: '', keyword: '' })
  const [appliedFilter, setAppliedFilter] = useState({ page: 1, status: '', from: '', to: '', keyword: '' })
  const [fetchState, setFetchState] = useState({
    resolvedKey: '',
    data: { dtoList: [], pageNumList: [], prev: false, next: false, prevPage: 0, nextPage: 0, totalCount: 0, totalPage: 0 },
    error: '',
  })

  const requestKey = JSON.stringify(appliedFilter)
  const loading = fetchState.resolvedKey !== requestKey

  useEffect(() => {
    let active = true

    searchAdminShipments({ ...appliedFilter, size: 10 })
      .then((res) => {
        if (!active) return
        setFetchState({ resolvedKey: requestKey, data: res, error: '' })
      })
      .catch((err) => {
        if (!active) return
        setFetchState((prev) => ({
          ...prev,
          resolvedKey: requestKey,
          error: err.response?.data?.message || '배송 목록 조회에 실패했습니다.',
        }))
      })

    return () => { active = false }
  }, [appliedFilter, requestKey])

  const handleSearch = (e) => {
    e.preventDefault()
    setAppliedFilter({ ...formInputs, page: 1 })
  }

  const handleReset = () => {
    const empty = { status: '', from: '', to: '', keyword: '' }
    setFormInputs(empty)
    setAppliedFilter({ ...empty, page: 1 })
  }

  const { data, error } = fetchState

  return (
    <section className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-gray-900">배송 통합 검색</h1>
          <p className="mt-0.5 text-xs text-gray-500">전체 고객의 배송 신청 내역과 진행 상태를 통합 조회합니다.</p>
        </div>
        <span className="rounded-lg bg-white px-3 py-1.5 text-xs font-semibold text-gray-600 shadow-xs">
          총 <strong className="text-blue-700">{data.totalCount}</strong>건
        </span>
      </div>

      {/* 검색 필터 바 */}
      <form onSubmit={handleSearch} className="rounded-xl border border-gray-200 bg-white p-4 shadow-xs">
        <div className="grid grid-cols-1 gap-3 md:grid-cols-5">
          <select
            value={formInputs.status}
            onChange={(e) => setFormInputs((prev) => ({ ...prev, status: e.target.value }))}
            className="rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
          >
            {STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>

          <input
            type="date"
            value={formInputs.from}
            onChange={(e) => setFormInputs((prev) => ({ ...prev, from: e.target.value }))}
            aria-label="시작일"
            className="rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
          />

          <input
            type="date"
            value={formInputs.to}
            onChange={(e) => setFormInputs((prev) => ({ ...prev, to: e.target.value }))}
            aria-label="종료일"
            className="rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
          />

          <input
            type="text"
            value={formInputs.keyword}
            onChange={(e) => setFormInputs((prev) => ({ ...prev, keyword: e.target.value }))}
            placeholder="배송번호, 화물, 주소, 고객명/이메일, 기사명/ID"
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

      {/* 테이블 결과 */}
      <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-xs">
        {loading ? (
          <div className="p-10 text-center text-sm text-gray-500">배송 내역을 불러오는 중입니다...</div>
        ) : error ? (
          <div className="p-8 text-center text-sm font-semibold text-red-600">{error}</div>
        ) : data.dtoList.length === 0 ? (
          <div className="p-10 text-center text-sm text-gray-500">조건에 맞는 배송 내역이 없습니다.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="border-b border-gray-200 bg-gray-50 text-gray-600">
                <tr>
                  <th className="px-4 py-3 font-semibold">배송번호</th>
                  <th className="px-4 py-3 font-semibold">상태</th>
                  <th className="px-4 py-3 font-semibold">신청 고객</th>
                  <th className="px-4 py-3 font-semibold">화물 · 차종</th>
                  <th className="px-4 py-3 font-semibold">출발지 → 도착지</th>
                  <th className="px-4 py-3 text-right font-semibold">운임</th>
                  <th className="px-4 py-3 font-semibold">상차 일시</th>
                  <th className="px-4 py-3 font-semibold">신청 일시</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {data.dtoList.map((item) => (
                  <tr
                    key={item.id}
                    onClick={() => navigate(`/admin/shipments/${item.id}`)}
                    onKeyDown={(event) => {
                      if (event.target === event.currentTarget && (event.key === 'Enter' || event.key === ' ')) {
                        event.preventDefault()
                        navigate(`/admin/shipments/${item.id}`)
                      }
                    }}
                    tabIndex={0}
                    role="link"
                    aria-label={`배송 ${item.id} 상세 보기`}
                    className="cursor-pointer hover:bg-blue-50/60 focus-visible:bg-blue-50 focus-visible:outline-2 focus-visible:outline-blue-500"
                  >
                    <td className="px-4 py-3 font-bold">
                      <Link
                        to={`/admin/shipments/${item.id}`}
                        onClick={(event) => event.stopPropagation()}
                        className="text-blue-700 underline-offset-2 hover:underline focus-visible:underline"
                        aria-label={`배송 ${item.id} 상세 보기`}
                      >
                        #{item.id}
                      </Link>
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={item.status} size="sm" />
                    </td>
                    <td className="px-4 py-3">
                      <div className="font-semibold text-gray-900">{item.customerName}</div>
                      <div className="text-[11px] text-gray-400">{item.customerEmail}</div>
                    </td>
                    <td className="px-4 py-3">
                      <div className="font-semibold text-gray-900">{item.cargoName}</div>
                      <div className="text-[11px] text-gray-500">
                        {item.cargoTypeName} · {item.vehicleTypeName} ({item.cargoWeightKg}kg)
                      </div>
                    </td>
                    <td className="max-w-[240px] px-4 py-3">
                      <div className="truncate text-gray-800" title={item.originAddress}>출: {item.originAddress}</div>
                      <div className="truncate text-gray-500" title={item.destAddress}>도: {item.destAddress}</div>
                    </td>
                    <td className="px-4 py-3 text-right font-bold text-gray-900">
                      {Number(item.fare).toLocaleString()}원
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-gray-600">{formatDateTime(item.pickupAt)}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-gray-400">{formatDateTime(item.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* 페이지네이션 */}
      {!loading && data.pageNumList?.length > 0 && (
        <Pagination pageData={data} onMove={(page) => setAppliedFilter((prev) => ({ ...prev, page }))} />
      )}
    </section>
  )
}

export default ShipmentManagePage