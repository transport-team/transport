import { useEffect, useState } from 'react'
import Pagination from '../../components/common/Pagination'
import DriverReviewModal from '../../components/review/DriverReviewModal'
import StatusBadge from '../../components/common/StatusBadge'
import Badge from '../../components/common/Badge'
import ConfirmModal from '../../components/common/ConfirmModal'
import useConfirm from '../../hooks/useConfirm'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { getMyShipments, getShipment } from '../../api/shipmentApi'
import {
  getDispatchApplicants,
  selectDispatchApplicant,
  rejectDispatchApplicant,
} from '../../api/dispatchApi'

const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

const getErrorMessage = (error, fallback) =>
  error.response?.data?.message || error.response?.data?.msg || fallback

// 배차 대기 중인 배송의 지원 기사 목록 및 확정/거절
const DispatchApplicantDetails = ({ shipmentId }) => {
  const navigate = useNavigate()
  const { confirm, confirmProps } = useConfirm()
  const [reload, setReload] = useState(0)
  const [fetchState, setFetchState] = useState({ key: '', shipment: null, applicants: [], error: '' })
  const [workingId, setWorkingId] = useState(null)
  const [actionError, setActionError] = useState('')
  const [actionNotice, setActionNotice] = useState('')
  const [reviewDriver, setReviewDriver] = useState(null)

  const requestKey = `${shipmentId}-${reload}`
  const loading = fetchState.key !== requestKey

  useEffect(() => {
    let active = true

    Promise.all([getShipment(shipmentId), getDispatchApplicants(shipmentId)])
      .then(([shipment, applicants]) => {
        if (!active) return
        setFetchState({
          key: requestKey,
          shipment,
          applicants: Array.isArray(applicants) ? applicants : [],
          error: '',
        })
      })
      .catch((error) => {
        if (!active) return
        setFetchState({
          key: requestKey,
          shipment: null,
          applicants: [],
          error: getErrorMessage(error, '배차 신청 정보를 불러오지 못했습니다.'),
        })
      })

    return () => { active = false }
  }, [shipmentId, requestKey])

  const handleSelect = async (application) => {
    if (!(await confirm({
      title: '배차 확정',
      message: `${application.driverName || `기사 #${application.driverId}`}님으로 배차를 확정할까요?\n확정하면 다른 기사들의 대기 중 신청은 자동 마감됩니다.`,
      confirmLabel: '배차 확정',
    }))) return

    setWorkingId(application.applicationId)
    setActionError('')
    setActionNotice('')

    try {
      await selectDispatchApplicant(shipmentId, application.applicationId)
      navigate(`/customer/shipments/${shipmentId}`)
    } catch (error) {
      setActionError(getErrorMessage(error, '배차 확정에 실패했습니다. 다시 시도해 주세요.'))
      setReload((count) => count + 1)
    } finally {
      setWorkingId(null)
    }
  }

  const handleReject = async (application) => {
    if (!(await confirm({
      title: '배차 신청 거절',
      message: `${application.driverName || `기사 #${application.driverId}`}님의 배차 신청을 거절할까요?\n거절하면 해당 기사는 이 배송에 다시 지원할 수 없습니다.`,
      confirmLabel: '거절',
      danger: true,
    }))) return

    setWorkingId(application.applicationId)
    setActionError('')
    setActionNotice('')

    try {
      await rejectDispatchApplicant(shipmentId, application.applicationId)
      setActionNotice('기사의 배차 신청을 거절했습니다.')
      setReload((count) => count + 1)
    } catch (error) {
      setActionError(getErrorMessage(error, '배차 신청 거절에 실패했습니다.'))
      setReload((count) => count + 1)
    } finally {
      setWorkingId(null)
    }
  }

  if (loading) {
    return <div className="rounded-2xl bg-white p-8 text-center text-sm text-gray-500 shadow-sm">지원 기사 목록을 불러오는 중...</div>
  }

  if (fetchState.error) {
    return <div className="rounded-2xl border border-red-200 bg-red-50 p-5 text-sm text-red-700" role="alert">{fetchState.error}</div>
  }

  const { shipment, applicants } = fetchState
  if (!shipment) return null

  return (
    <div className="space-y-5">
      <div className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm sm:p-6">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="text-xs font-semibold text-blue-700">배송 #{shipment.id}</p>
            <h2 className="mt-1 text-lg font-bold text-gray-900">{shipment.cargoName}</h2>
            <p className="mt-2 text-sm text-gray-600">{shipment.originAddress} → {shipment.destAddress}</p>
            <p className="mt-2 text-xs text-gray-500">상차 희망 {formatDateTime(shipment.pickupAt)}</p>
          </div>
          <Link to={`/customer/shipments/${shipment.id}`} className="rounded-lg border border-gray-200 px-3 py-2 text-xs font-semibold text-gray-700 hover:bg-gray-50">
            배송 상세 보기
          </Link>
        </div>
        <div className="mt-4 flex flex-wrap gap-3 border-t border-gray-100 pt-4 text-sm">
          <span className="font-semibold text-gray-700">운임 {Number(shipment.fare ?? 0).toLocaleString('ko-KR')}원</span>
          <span className="text-gray-400">·</span>
          <span className="text-gray-600">필요 차종: {shipment.vehicleTypeName}</span>
        </div>
      </div>

      {actionError && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{actionError}</div>}
      {actionNotice && <div role="status" className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800">{actionNotice}</div>}

      {shipment.status !== 'REQUESTED' ? (
        <div className="rounded-2xl border border-gray-200 bg-white p-8 text-center text-sm text-gray-600">
          현재 배차 대기 상태가 아닌 배송입니다. 배송 상세에서 진행 상태를 확인해 주세요.
        </div>
      ) : applicants.length === 0 ? (
        <div className="rounded-2xl border border-gray-200 bg-white p-10 text-center shadow-sm">
          <p className="font-semibold text-gray-900">아직 지원한 기사가 없어요.</p>
          <p className="mt-2 text-sm text-gray-500">기사가 배차 신청을 하면 여기에서 확인할 수 있습니다.</p>
          <button
            type="button"
            onClick={() => { setActionNotice(''); setReload((count) => count + 1) }}
            className="mt-5 rounded-xl border border-gray-200 px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50"
          >
            새로고침
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          <h3 className="px-1 text-base font-bold text-gray-900">지원 기사 {applicants.length}명</h3>
          {applicants.map((application) => (
            <div key={application.applicationId} className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm sm:p-6">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="flex min-w-0 items-center gap-3">
                  <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-xl" aria-hidden="true">🚚</div>
                  <div className="min-w-0">
                    <h4 className="font-bold text-gray-900">{application.driverName || `기사 #${application.driverId}`}</h4>
                    <p className="mt-1 text-xs text-gray-500">면허 {application.licenseType || '-'} · 경력 {application.careerYears ?? 0}년</p>
                  </div>
                </div>
                <Badge tone="amber">선택 대기</Badge>
              </div>
              <div className="mt-4 grid gap-3 rounded-xl bg-gray-50 p-4 text-sm sm:grid-cols-2">
                <div><span className="text-gray-500">차량번호 </span><span className="font-semibold text-gray-900">{application.plateNo || '-'}</span></div>
                <div><span className="text-gray-500">차종 </span><span className="font-semibold text-gray-900">{application.vehicleTypeName || `차종 #${application.vehicleTypeId}`}</span></div>
              </div>
              <div className="mt-4 flex flex-col gap-2 sm:flex-row sm:justify-end">
                <button type="button" onClick={() => setReviewDriver({ id: application.driverId, name: application.driverName || `기사 #${application.driverId}` })}
                  className="rounded-xl border border-gray-200 bg-white px-5 py-2.5 text-sm font-semibold text-gray-700 hover:bg-gray-50">
                  ★ 리뷰 보기
                </button>
                <button
                  type="button"
                  onClick={() => handleReject(application)}
                  disabled={workingId !== null}
                  className="rounded-xl border border-red-200 px-5 py-2.5 text-sm font-semibold text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  거절
                </button>
                <button
                  type="button"
                  onClick={() => handleSelect(application)}
                  disabled={workingId !== null}
                  className="rounded-xl bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {workingId === application.applicationId ? '처리 중...' : '이 기사로 배차 확정'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
      {reviewDriver && <DriverReviewModal driverId={reviewDriver.id} driverName={reviewDriver.name} onClose={() => setReviewDriver(null)} />}
      <ConfirmModal {...confirmProps} />
    </div>
  )
}

const CustomerDispatchRequestsPage = () => {
  const [searchParams] = useSearchParams()
  const shipmentId = searchParams.get('shipmentId')
  const [page, setPage] = useState(1)
  const [listState, setListState] = useState({ key: '', items: [], totalPages: 1, totalCount: 0, error: '' })
  const [applicantCounts, setApplicantCounts] = useState({})
  const [countsError, setCountsError] = useState('')
  const requestKey = String(page)
  const loading = listState.key !== requestKey

  useEffect(() => {
    if (shipmentId) return undefined
    let active = true

    getMyShipments({ page, size: 10, status: 'REQUESTED' })
      .then((data) => {
        if (!active) return
        setListState({
          key: requestKey,
          items: data.dtoList || data.content || [],
          totalPages: Math.max(1, data.totalPage || data.totalPages || 1),
          totalCount: data.totalCount ?? 0,
          error: '',
        })
      })
      .catch((error) => {
        if (!active) return
        setListState({
          key: requestKey,
          items: [],
          totalPages: 1,
          totalCount: 0,
          error: getErrorMessage(error, '배차 요청 목록을 불러오지 못했습니다.'),
        })
      })

    return () => { active = false }
  }, [shipmentId, page, requestKey])

  useEffect(() => {
    if (shipmentId || loading || listState.error) return undefined
    let active = true
    const waiting = listState.items.filter((item) => item.status === 'REQUESTED')
    Promise.allSettled(waiting.map((item) => getDispatchApplicants(item.id)))
      .then((results) => {
        if (!active) return
        const counts = {}
        results.forEach((result, index) => {
          if (result.status === 'fulfilled') counts[waiting[index].id] = result.value.length
        })
        setApplicantCounts(counts)
        setCountsError(results.some((result) => result.status === 'rejected') ? '일부 지원자 수를 조회하지 못했습니다.' : '')
      })
    return () => { active = false }
  }, [shipmentId, loading, listState.items, listState.error])

  return (
    <section className="mx-auto w-full max-w-3xl space-y-5 pb-12">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">배차 요청</h1>
          <p className="mt-2 text-sm text-gray-500">기사의 배차 신청을 확인하고 배차를 확정할 수 있습니다.</p>
        </div>
        {shipmentId && (
          <Link to="/customer/dispatch-requests" className="rounded-xl border border-gray-200 bg-white px-4 py-2.5 text-sm font-semibold text-gray-700 hover:bg-gray-50">
            ← 배송 목록
          </Link>
        )}
      </div>

      {shipmentId ? (
        <DispatchApplicantDetails key={shipmentId} shipmentId={shipmentId} />
      ) : (
        <div className="space-y-3">
          <p className="px-1 text-sm font-semibold text-gray-700">배차 대기 중인 배송 {listState.totalCount}건</p>
          {countsError && <p role="alert" className="text-xs text-red-600">{countsError}</p>}

          {loading ? (
            <div className="rounded-2xl bg-white p-10 text-center text-sm text-gray-500 shadow-sm">배송 목록을 불러오는 중...</div>
          ) : listState.error ? (
            <div role="alert" className="rounded-2xl border border-red-200 bg-red-50 p-5 text-sm text-red-700">{listState.error}</div>
          ) : listState.items.length === 0 ? (
            <div className="rounded-2xl border border-gray-200 bg-white p-10 text-center shadow-sm">
              <p className="font-semibold text-gray-900">현재 배차 대기 중인 배송이 없어요.</p>
              <Link to="/customer/shipments" className="mt-4 inline-block text-sm font-semibold text-blue-700">내 배송 내역 보기</Link>
            </div>
          ) : (
            listState.items.map((item) => (
              <Link
                key={item.id}
                to={`/customer/dispatch-requests?shipmentId=${item.id}`}
                className="block rounded-2xl border border-gray-200 bg-white p-5 shadow-sm transition hover:border-blue-300 hover:shadow-md"
              >
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="flex items-center gap-2">
                    <StatusBadge status={item.status} />
                    {Number(applicantCounts[item.id] || 0) > 0 && <span className="flex min-h-6 min-w-6 items-center justify-center rounded-full bg-red-600 px-1.5 text-xs font-bold text-white" aria-label={`지원 기사 ${applicantCounts[item.id]}명`}>{applicantCounts[item.id]}</span>}
                  </span>
                  <span className="text-xs text-gray-500">상차 {formatDateTime(item.pickupAt)}</span>
                </div>
                <h2 className="mt-3 text-base font-bold text-gray-900">{item.cargoName || `배송 #${item.id}`}</h2>
                <p className="mt-2 text-sm text-gray-600">{item.originAddress} → {item.destAddress}</p>
                <div className="mt-4 flex items-center justify-between border-t border-gray-100 pt-3">
                  <span className="text-sm font-semibold text-gray-800">{Number(item.fare ?? 0).toLocaleString('ko-KR')}원</span>
                  <span className="text-sm font-semibold text-blue-700">지원 기사 확인 →</span>
                </div>
              </Link>
            ))
          )}

          {!loading && !listState.error && listState.totalPages > 1 && (
            <Pagination page={page} totalPages={listState.totalPages} onMove={setPage} />
          )}
        </div>
      )}
    </section>
  )
}

export default CustomerDispatchRequestsPage
