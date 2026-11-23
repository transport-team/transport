import { useEffect, useMemo, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { getAdminDriverVehicles, getDriverReviews, getDrivers, patchDriverStatus } from '../../api/adminDriverApi'
import Pagination from '../../components/common/Pagination'

const INPUT_CLASS = 'rounded border border-gray-300 bg-white px-3 py-2 text-sm focus:border-emerald-600 focus:outline-none'

const STATUS_NAME = { PENDING: '승인 대기', ACTIVE: '승인됨', INACTIVE: '중지' }
const STATUS_CLASS = {
  PENDING: 'bg-amber-50 text-amber-800',
  ACTIVE: 'bg-emerald-50 text-emerald-800',
  INACTIVE: 'bg-gray-100 text-gray-600',
}

// 거절 · 중지는 한 번 더 확인한다 (둘 다 INACTIVE로 바꾼다)
const CONFIRM_TEXT = {
  reject: {
    title: '기사 거절',
    verb: '거절',
    message: "거절하면 이 기사는 로그인할 수 없어요. 나중에 '다시 승인'으로 승인할 수 있어요.",
  },
  suspend: {
    title: '기사 중지',
    verb: '중지',
    message: "중지하면 이 기사는 로그인할 수 없고 운송 요청에도 지원할 수 없어요. 진행 중인 배송이 있으면 끝난 뒤에 중지해 주세요. 나중에 '다시 승인'으로 되돌릴 수 있어요.",
  },
}

// 주소의 검색 조건 (?status=PENDING&keyword=김&page=2) — 모르는 값은 기본값으로
const readQuery = (searchParams) => {
  const status = searchParams.get('status') ?? ''
  const page = Math.floor(Number(searchParams.get('page')))
  return {
    status: Object.hasOwn(STATUS_NAME, status) ? status : '',
    keyword: searchParams.get('keyword') ?? '',
    page: page >= 1 ? page : 1,
  }
}

// 빈 조건은 빼고 주소에 남긴다 (status · keyword가 없으면 전체, 1쪽이면 page도 뺀다)
const toParams = ({ status, keyword, page }) => ({
  ...(status && { status }),
  ...(keyword.trim() && { keyword: keyword.trim() }),
  ...(page > 1 && { page: String(page) }),
})

const date = (dateTime) => dateTime?.slice(0, 10) ?? '-'

// 관리자 기사 관리 (ADM-05): 상태 · 이름/이메일 검색, 승인 · 거절 · 중지 · 다시 승인, 기사별 리뷰 · 평균 평점
// 검색 조건은 주소에 남긴다 — 대시보드 '기사 승인 대기' 카드가 ?status=PENDING으로 연다
const DriverListPage = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const queryString = searchParams.toString()
  const query = useMemo(() => readQuery(new URLSearchParams(queryString)), [queryString])

  const [reloadKey, setReloadKey] = useState(0)       // 같은 조건으로 다시 검색해도 새로 불러온다
  const [pageData, setPageData] = useState(null)
  const [error, setError] = useState('')
  const [confirm, setConfirm] = useState(null)        // 확인 중인 거절 · 중지 { driver, action }
  const [changingId, setChangingId] = useState(null)  // 상태를 바꾸는 중인 기사
  const [review, setReview] = useState(null)          // 리뷰 창 { seq, driver, data, error }
  const reviewSeq = useRef(0)                         // 창을 닫았거나 다른 기사를 연 뒤에 온 응답은 버린다
  const [vehicleModal, setVehicleModal] = useState(null)
  const [detail, setDetail] = useState(null)
  const vehicleSeq = useRef(0)

  useEffect(() => {
    let ignore = false
    getDrivers({ ...toParams(query), page: query.page })
      .then((data) => {
        if (!ignore) {
          setPageData(data)
          setError('')
        }
      })
      .catch((err) => {
        if (!ignore) setError(err.response?.data?.message ?? '기사 목록을 불러오지 못했어요.')
      })
    return () => {
      ignore = true
    }
  }, [query, reloadKey])

  // 창이 열려 있으면 Esc로 닫는다
  useEffect(() => {
    if (!confirm && !review && !vehicleModal && !detail) return
    const closeOnEsc = (e) => {
      if (e.key === 'Escape') {
        setConfirm(null)
        setReview(null)
        setVehicleModal(null)
        setDetail(null)
      }
    }
    document.addEventListener('keydown', closeOnEsc)
    return () => document.removeEventListener('keydown', closeOnEsc)
  }, [confirm, review, vehicleModal, detail])

  const handleSearch = (e) => {
    e.preventDefault()
    const form = new FormData(e.currentTarget)
    setError('')
    setReloadKey((key) => key + 1)
    setSearchParams(toParams({ status: form.get('status'), keyword: form.get('keyword'), page: 1 }))
  }

  const movePage = (page) => {
    setError('')
    setSearchParams(toParams({ ...query, page }))
  }

  const changeStatus = async (driver, status) => {
    setChangingId(driver.id)
    setError('')
    try {
      const changed = await patchDriverStatus(driver.id, status)
      setPageData((prev) => ({ ...prev, dtoList: prev.dtoList.map((row) => (row.id === changed.id ? changed : row)) }))
      setDetail((current) => (current?.id === changed.id ? changed : current))
    } catch (err) {
      setError(err.response?.data?.message ?? '기사 상태를 바꾸지 못했어요.')
    } finally {
      setChangingId(null)
      setConfirm(null)
    }
  }

  const openReview = (driver) => {
    reviewSeq.current += 1
    const seq = reviewSeq.current
    setReview({ seq, driver, data: null, error: '' })
    getDriverReviews(driver.id)
      .then((data) => setReview((current) => (current?.seq === seq ? { ...current, data } : current)))
      .catch((err) => {
        const message = err.response?.data?.message ?? '리뷰를 불러오지 못했어요.'
        setReview((current) => (current?.seq === seq ? { ...current, error: message } : current))
      })
  }

  // 기사 차량 보기 (진행 중인 조회가 다른 기사 모달에 영향을 주지 않게 순번 검사)
  const openVehicles = (driver) => {
    vehicleSeq.current += 1
    const seq = vehicleSeq.current
    setVehicleModal({ seq, driver, vehicles: null, error: '' })
    getAdminDriverVehicles(driver.id)
      .then((vehicles) => setVehicleModal((current) =>
        current?.seq === seq ? { ...current, vehicles: Array.isArray(vehicles) ? vehicles : [] } : current))
      .catch((err) => setVehicleModal((current) =>
        current?.seq === seq ? { ...current, error: err.response?.data?.message || '차량 조회에 실패했습니다.' } : current))
  }

  // 더보기 창: 승인 대기는 승인 · 거절, 승인됨은 중지, 중지는 다시 승인 (거절 · 중지만 한 번 더 확인)
  const renderAction = (driver) => {
    const busy = changingId === driver.id
    if (driver.status === 'PENDING') {
      return (
        <div className="flex justify-end gap-1">
          <button
            type="button" disabled={busy} onClick={() => changeStatus(driver, 'ACTIVE')}
            className="rounded bg-emerald-700 px-2 py-1 text-xs font-medium text-white hover:bg-emerald-800 disabled:opacity-60"
          >
            승인
          </button>
          <button
            type="button" disabled={busy} onClick={() => setConfirm({ driver, action: 'reject' })}
            className="rounded border border-red-300 px-2 py-1 text-xs text-red-700 hover:bg-red-50 disabled:opacity-60"
          >
            거절
          </button>
        </div>
      )
    }
    if (driver.status === 'ACTIVE') {
      return (
        <button
          type="button" disabled={busy} onClick={() => setConfirm({ driver, action: 'suspend' })}
          className="rounded border border-red-300 px-2 py-1 text-xs text-red-700 hover:bg-red-50 disabled:opacity-60"
        >
          중지
        </button>
      )
    }
    return (
      <button
        type="button" disabled={busy} onClick={() => changeStatus(driver, 'ACTIVE')}
        className="rounded border border-emerald-600 px-2 py-1 text-xs text-emerald-800 hover:bg-emerald-50 disabled:opacity-60"
      >
        다시 승인
      </button>
    )
  }

  const confirmText = confirm && CONFIRM_TEXT[confirm.action]

  return (
    <section className="grid gap-4">
      <div>
        <h1 className="text-xl font-bold">기사 관리</h1>
        <p className="mt-1 text-sm text-gray-600">새로 가입한 기사는 승인해야 로그인할 수 있어요.</p>
      </div>

      <form key={queryString} onSubmit={handleSearch} className="flex flex-wrap items-end gap-2">
        <select name="status" aria-label="상태" defaultValue={query.status} className={INPUT_CLASS}>
          <option value="">전체 상태</option>
          {Object.entries(STATUS_NAME).map(([status, name]) => <option key={status} value={status}>{name}</option>)}
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
      {!pageData && !error && <p className="text-sm text-gray-500">기사 목록 불러오는 중...</p>}

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
                  <th className="px-4 py-3 font-medium">상태</th>
                  <th className="px-4 py-3 font-medium">리뷰</th>
                  <th className="px-4 py-3 font-medium">차량</th>
                  <th className="px-4 py-3 text-right font-medium">정보</th>
                </tr>
              </thead>
              <tbody>
                {pageData.dtoList.length === 0 && (
                  <tr>
                    <td colSpan={7} className="px-4 py-8 text-center text-gray-500">조건에 맞는 기사가 없어요.</td>
                  </tr>
                )}
                {pageData.dtoList.map((driver) => (
                  <tr key={driver.id} className="border-b border-gray-100 last:border-0">
                    <td className="px-4 py-3">{driver.name}</td>
                    <td className="px-4 py-3">{driver.email}</td>
                    <td className="px-4 py-3">{driver.phone}</td>
                    <td className="px-4 py-3">
                      <span className={`rounded px-2 py-0.5 text-xs font-medium ${STATUS_CLASS[driver.status]}`}>
                        {STATUS_NAME[driver.status]}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      {driver.status === 'PENDING' ? (
                        <span className="text-xs text-gray-400">-</span>
                      ) : (
                        <button
                          type="button" onClick={() => openReview(driver)}
                          className="rounded border border-gray-300 px-2 py-1 text-xs hover:bg-gray-50"
                        >
                          보기
                        </button>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <button type="button" onClick={() => openVehicles(driver)}
                        className="rounded border border-gray-300 px-2 py-1 text-xs hover:bg-gray-50">
                        보기
                      </button>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <button type="button" onClick={() => setDetail(driver)}
                        className="rounded border border-gray-300 px-2 py-1 text-xs hover:bg-gray-50">
                        더보기
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination pageData={pageData} onMove={movePage} />
        </>
      )}

      {detail && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 p-4" onClick={() => setDetail(null)} role="presentation">
          <div role="dialog" aria-modal="true" aria-labelledby="driver-detail-title"
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-lg rounded-xl bg-white p-6 shadow-xl">
            <div className="flex items-center justify-between gap-3 border-b border-gray-200 pb-3">
              <h2 id="driver-detail-title" className="text-base font-semibold">{detail.name} 기사 정보</h2>
              <button type="button" onClick={() => setDetail(null)} className="rounded border border-gray-300 px-2 py-1 text-xs hover:bg-gray-50">닫기</button>
            </div>
            <dl className="mt-4 grid grid-cols-1 gap-4 text-sm sm:grid-cols-2">
              {[
                ['이름', detail.name],
                ['이메일', detail.email],
                ['연락처', detail.phone],
                ['면허', detail.licenseType],
                ['경력', detail.careerYears != null ? `${detail.careerYears}년` : '-'],
                ['가입일', date(detail.createdAt)],
                ['승인일', date(detail.approvedAt)],
                ['상태', STATUS_NAME[detail.status] || detail.status],
              ].map(([label, value]) => (
                <div key={label}>
                  <dt className="text-xs text-gray-500">{label}</dt>
                  <dd className="mt-1 break-words font-medium text-gray-900">{value || '-'}</dd>
                </div>
              ))}
            </dl>
            <div className="mt-5 flex flex-wrap items-center justify-between gap-3 border-t border-gray-100 pt-4">
              <span className="text-xs text-gray-500">기사 계정 승인·거절·중지 관리</span>
              {renderAction(detail)}
            </div>
          </div>
        </div>
      )}

      {confirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 p-4">
          <div
            role="dialog" aria-modal="true" aria-labelledby="driver-confirm-title"
            className="w-full max-w-sm rounded-lg bg-white p-6 shadow-xl"
          >
            <h2 id="driver-confirm-title" className="text-base font-semibold">{confirmText.title}</h2>
            <p className="mt-2 text-sm font-medium text-gray-800">{confirm.driver.name} 기사를 {confirmText.verb}할까요?</p>
            <p className="mt-2 text-sm text-gray-600">{confirmText.message}</p>
            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button" onClick={() => setConfirm(null)}
                className="rounded border border-gray-300 px-4 py-2 text-sm hover:bg-gray-50"
              >
                취소
              </button>
              <button
                type="button" disabled={changingId === confirm.driver.id} onClick={() => changeStatus(confirm.driver, 'INACTIVE')}
                className="rounded bg-red-600 px-4 py-2 text-sm text-white hover:bg-red-700 disabled:opacity-60"
              >
                {confirmText.verb}
              </button>
            </div>
          </div>
        </div>
      )}

      {vehicleModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 p-4" onClick={() => setVehicleModal(null)} role="presentation">
          <div role="dialog" aria-modal="true" aria-labelledby="driver-vehicles-title"
            onClick={(e) => e.stopPropagation()}
            className="flex max-h-[80vh] w-full max-w-lg flex-col rounded-xl bg-white shadow-xl">
            <div className="flex items-center justify-between border-b border-gray-200 px-5 py-3">
              <h2 id="driver-vehicles-title" className="text-base font-semibold">{vehicleModal.driver.name} 기사 보유 차량</h2>
              <button type="button" onClick={() => setVehicleModal(null)} className="rounded px-2 py-1 text-sm text-gray-500 hover:bg-gray-100">닫기</button>
            </div>
            <div className="overflow-auto p-5 text-sm">
              {vehicleModal.error && <p role="alert" className="rounded bg-red-50 p-3 text-red-700">{vehicleModal.error}</p>}
              {!vehicleModal.vehicles && !vehicleModal.error && <p className="text-gray-500">차량 조회 중...</p>}
              {vehicleModal.vehicles?.length === 0 && <p className="text-gray-500">등록된 차량이 없습니다.</p>}
              {vehicleModal.vehicles?.length > 0 && (
                <table className="w-full text-left text-xs">
                  <thead className="border-b border-gray-200 bg-gray-50 text-gray-600">
                    <tr><th className="px-3 py-2">차량번호</th><th className="px-3 py-2">차종</th><th className="px-3 py-2">등록일</th><th className="px-3 py-2">상태</th></tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {vehicleModal.vehicles.map((vehicle) => (
                      <tr key={vehicle.id}>
                        <td className="px-3 py-3 font-semibold">{vehicle.plateNo}</td>
                        <td className="px-3 py-3">{vehicle.vehicleTypeName}</td>
                        <td className="whitespace-nowrap px-3 py-3">{date(vehicle.createdAt)}</td>
                        <td className="px-3 py-3 text-gray-600">{{ AVAILABLE: '운행 가능', MAINTENANCE: '정비 중', INACTIVE: '비활성' }[vehicle.status] || vehicle.status}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </div>
      )}

      {review && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 p-4" onClick={() => setReview(null)} role="presentation">
          <div
            role="dialog" aria-modal="true" aria-labelledby="driver-review-title" onClick={(e) => e.stopPropagation()}
            className="flex max-h-[80vh] w-full max-w-md flex-col rounded-lg bg-white shadow-xl"
          >
            <div className="flex items-center justify-between border-b border-gray-200 px-5 py-3">
              <h2 id="driver-review-title" className="text-base font-semibold">{review.driver.name} 기사 리뷰</h2>
              <button type="button" onClick={() => setReview(null)} className="rounded px-2 py-1 text-sm text-gray-500 hover:bg-gray-100">
                닫기
              </button>
            </div>
            <div className="overflow-y-auto px-5 py-4 text-sm">
              {review.error && <p role="alert" className="rounded bg-red-50 p-3 text-red-800">{review.error}</p>}
              {!review.data && !review.error && <p className="text-gray-500">리뷰 불러오는 중...</p>}
              {review.data?.reviews.length === 0 && <p className="text-gray-500">아직 리뷰가 없어요.</p>}
              {review.data?.reviews.length > 0 && (
                <>
                  <p className="mb-3 font-medium">
                    평균 {review.data.averageRating.toFixed(1)}점 · 리뷰 {review.data.reviews.length}개
                  </p>
                  <ul className="grid gap-3">
                    {review.data.reviews.map((item) => (
                      <li key={item.id} className="rounded border border-gray-200 p-3">
                        <p className="text-xs text-gray-500">{item.rating}점 · {item.customerName} · {date(item.createdAt)}</p>
                        {item.content && <p className="mt-1 whitespace-pre-wrap text-gray-800">{item.content}</p>}
                      </li>
                    ))}
                  </ul>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </section>
  )
}

export default DriverListPage
