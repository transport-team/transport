import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { cancelPayment, retryPayment } from '../../api/paymentApi'
import { getShipment } from '../../api/shipmentApi'
import { requestTossPayment } from '../../util/tossPayments'
import Timeline from '../../components/tracking/Timeline'
import LiveTrackingMap from '../../components/tracking/LiveTrackingMap'
import ReviewSection from '../../components/review/ReviewSection'
import { SHIPMENT_LOCATION_EVENT } from '../../hooks/useNotification'

const STATUS_META = {
  PENDING_PAYMENT: { label: '결제 대기', badge: 'bg-amber-50 text-amber-700 border-amber-200' },
  REQUESTED: { label: '배차 대기', badge: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  ASSIGNED: { label: '배차 완료', badge: 'bg-blue-50 text-blue-700 border-blue-200' },
  PICKED_UP: { label: '상차 완료', badge: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  IN_TRANSIT: { label: '배송 중', badge: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  ARRIVED: { label: '도착', badge: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  COMPLETED: { label: '배송 완료', badge: 'bg-gray-100 text-gray-700 border-gray-200' },
  CANCELED: { label: '취소됨', badge: 'bg-red-50 text-red-600 border-red-200' },
}

// 배차 뒤부터 도착까지 지도를 보여 준다 (기사 위치는 운송 중에만 — LiveTrackingMap이 처리)
const TRACKING_STATUSES = ['ASSIGNED', 'PICKED_UP', 'IN_TRANSIT', 'ARRIVED']

const formatDateTime = (value) => {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

const ShipmentDetailPage = () => {
  const { id } = useParams()
  const [refreshCount, setRefreshCount] = useState(0)
  const [fetchState, setFetchState] = useState({
    resolvedKey: '',
    shipment: null,
    error: '',
  })
  const [actionLoading, setActionLoading] = useState(false)
  const [actionError, setActionError] = useState('')
  const [cancelModalOpen, setCancelModalOpen] = useState(false)
  const [cancelReason, setCancelReason] = useState('')
  const [liveLocation, setLiveLocation] = useState(null)  // SSE로 받은 이 배송의 최신 기사 위치

  const requestKey = `${id}-${refreshCount}`
  const loading = fetchState.resolvedKey !== requestKey

  useEffect(() => {
    let active = true

    getShipment(id)
      .then((data) => {
        if (!active) return
        setFetchState({ resolvedKey: requestKey, shipment: data, error: '' })
      })
      .catch((err) => {
        if (!active) return
        setFetchState({
          resolvedKey: requestKey,
          shipment: null,
          error: err.response?.data?.message || '배송 상세 정보를 불러오지 못했습니다.',
        })
      })

    return () => { active = false }
  }, [id, requestKey])

  // 기사 위치 실시간: 알림 SSE 연결(useNotification)이 넘겨주는 위치 중 이 배송 것만 받는다
  useEffect(() => {
    const handleLocation = (e) => {
      if (String(e.detail?.shipmentId) === String(id)) setLiveLocation(e.detail)
    }
    window.addEventListener(SHIPMENT_LOCATION_EVENT, handleLocation)
    return () => window.removeEventListener(SHIPMENT_LOCATION_EVENT, handleLocation)
  }, [id])

  const { shipment, error } = fetchState

  const handlePayNow = async () => {
    if (!shipment) return
    setActionLoading(true)
    setActionError('')
    try {
      const retried = await retryPayment(shipment.id)
      await requestTossPayment({
        shipmentId: shipment.id,
        orderId: retried.orderId,
        amount: retried.amount,
        cargoName: shipment.cargoName,
      })
    } catch (err) {
      const rawMessage = err.response?.data?.message || err.message || ''
      const isClosedByUser =
        /cancel|close|user_cancel/i.test(String(err.code || '')) ||
        /취소|닫|중단/i.test(rawMessage)

      if (isClosedByUser) {
        setActionError('결제창이 닫혀 결제가 완료되지 않았습니다. 결제하기 버튼을 눌러 다시 시도해 주세요. (30분 미결제 시 자동 취소)')
      } else {
        setActionError(err.response?.data?.message || '결제창 호출 중 오류가 발생했습니다.')
      }
    } finally {
      setActionLoading(false)
    }
  }

  const handleCancelSubmit = async (event) => {
    event.preventDefault()
    if (!shipment) return
    setActionLoading(true)
    setActionError('')
    try {
      await cancelPayment(shipment.id, cancelReason.trim() || '고객 요청 취소')
      setCancelModalOpen(false)
      setCancelReason('')
      setRefreshCount((c) => c + 1)
    } catch (err) {
      setActionError(err.response?.data?.message || '취소 처리 중 오류가 발생했습니다.')
    } finally {
      setActionLoading(false)
    }
  }

  if (loading) {
    return (
      <section className="mx-auto min-h-screen max-w-[640px] bg-gray-50 p-6">
        <div className="rounded-2xl bg-white p-8 text-center text-sm text-gray-500 shadow-sm">
          상세 정보를 불러오는 중입니다...
        </div>
      </section>
    )
  }

  if (error || !shipment) {
    return (
      <section className="mx-auto min-h-screen max-w-[640px] bg-gray-50 p-6">
        <div className="rounded-2xl border border-red-200 bg-red-50 p-6 text-sm font-semibold text-red-600">
          {error || '배송 정보를 찾을 수 없습니다.'}
        </div>
        <Link className="mt-4 inline-block text-sm font-bold text-emerald-700" to="/customer/shipments">
          ← 내 배송 목록으로
        </Link>
      </section>
    )
  }

  const meta = STATUS_META[shipment.status] || { label: shipment.status, badge: 'bg-gray-100 text-gray-700 border-gray-200' }
  const canPay = shipment.status === 'PENDING_PAYMENT'
  const canCancel = shipment.status === 'PENDING_PAYMENT' || shipment.status === 'REQUESTED'

  return (
    <section className="mx-auto min-h-screen w-full max-w-[640px] bg-gray-50 pb-28">
      <header className="sticky top-0 z-20 border-b border-gray-100 bg-white/95 px-5 py-4 backdrop-blur">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Link className="text-2xl leading-none text-gray-700" to="/customer/shipments" aria-label="내 배송 목록으로">←</Link>
            <h1 className="text-lg font-bold text-gray-900">배송 상세 #{shipment.id}</h1>
          </div>
          <span className={`rounded-full border px-3 py-1 text-xs font-bold ${meta.badge}`}>
            {meta.label}
          </span>
        </div>
      </header>

      <div className="space-y-4 px-5 pt-5">
        {shipment.status === 'REQUESTED' && (
          <Link
            to={`/customer/dispatch-requests?shipmentId=${shipment.id}`}
            className="flex items-center justify-between rounded-2xl border border-emerald-200 bg-emerald-50 p-5 text-sm font-bold text-emerald-800 hover:bg-emerald-100"
          >
            <span>지원한 기사 확인 및 배차 확정</span>
            <span aria-hidden="true">→</span>
          </Link>
        )}
        {actionError && (
          <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-xs font-semibold text-red-600" role="alert">
            {actionError}
          </div>
        )}

        <div className="rounded-2xl bg-white p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <p className="text-xs font-semibold text-emerald-600">ROUTE & SCHEDULE</p>
            <span className="text-xs font-semibold text-gray-500">
              {shipment.distanceKm}km{shipment.durationMin != null ? ` · 약 ${shipment.durationMin}분` : ''}
            </span>
          </div>

          <div className="mt-4 flex gap-3">
            <div className="flex w-4 shrink-0 flex-col items-center">
              <span className="mt-1 h-2.5 w-2.5 rounded-full bg-emerald-600 ring-4 ring-emerald-50" />
              <span className="my-1 w-px flex-1 border-l border-dashed border-gray-300" />
              <span className="mb-1 h-2.5 w-2.5 rounded-full bg-red-500 ring-4 ring-red-50" />
            </div>
            <div className="min-w-0 flex-1 space-y-4">
              <div>
                <p className="text-[11px] font-semibold text-gray-400">출발지</p>
                <p className="mt-0.5 text-sm font-bold text-gray-900">{shipment.originAddress}</p>
                {shipment.originDetail && <p className="text-xs text-gray-500">{shipment.originDetail}</p>}
              </div>
              <div>
                <p className="text-[11px] font-semibold text-gray-400">도착지</p>
                <p className="mt-0.5 text-sm font-bold text-gray-900">{shipment.destAddress}</p>
                {shipment.destDetail && <p className="text-xs text-gray-500">{shipment.destDetail}</p>}
              </div>
            </div>
          </div>

          <div className="mt-4 flex items-center justify-between rounded-xl bg-gray-50 px-4 py-3 text-xs">
            <span className="text-gray-500">상차 희망 일시</span>
            <strong className="text-gray-900">{formatDateTime(shipment.pickupAt)}</strong>
          </div>
        </div>

        {TRACKING_STATUSES.includes(shipment.status) && (
          <div className="rounded-2xl bg-white p-5 shadow-sm">
            <p className="text-xs font-semibold text-emerald-600">LIVE TRACKING</p>
            <div className="mt-3">
              <LiveTrackingMap
                shipmentId={shipment.id}
                status={shipment.status}
                sseLocation={liveLocation?.shipmentId === shipment.id ? liveLocation : null}
              />
            </div>
          </div>
        )}

        <div className="rounded-2xl bg-white p-5 shadow-sm">
          <p className="text-xs font-semibold text-emerald-600">CARGO & FARE</p>
          <h2 className="mt-1 text-base font-bold text-gray-900">{shipment.cargoName}</h2>

          <dl className="mt-4 space-y-2.5 border-t border-gray-100 pt-4 text-sm">
            <div className="flex justify-between">
              <dt className="text-gray-500">요청 차종</dt>
              <dd className="font-semibold text-gray-900">{shipment.vehicleTypeName}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-gray-500">화물 유형 · 중량</dt>
              <dd className="font-semibold text-gray-900">{shipment.cargoTypeName} · {Number(shipment.cargoWeightKg).toLocaleString()}kg</dd>
            </div>
            <div className="flex justify-between border-t border-dashed border-gray-200 pt-3">
              <dt className="font-bold text-gray-700">총 운임</dt>
              <dd className="text-lg font-black text-emerald-700">{Number(shipment.fare).toLocaleString()}원</dd>
            </div>
          </dl>
        </div>

        <div className="rounded-2xl bg-white p-5 shadow-sm">
          <p className="text-xs font-semibold text-emerald-600">CONTACTS</p>
          <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div className="rounded-xl bg-gray-50 p-3.5 text-xs">
              <p className="font-bold text-gray-500">보내는 분</p>
              <p className="mt-1 text-sm font-bold text-gray-900">{shipment.senderName}</p>
              <p className="mt-0.5 text-gray-600">{shipment.senderPhone}</p>
            </div>
            <div className="rounded-xl bg-gray-50 p-3.5 text-xs">
              <p className="font-bold text-gray-500">받는 분</p>
              <p className="mt-1 text-sm font-bold text-gray-900">{shipment.receiverName}</p>
              <p className="mt-0.5 text-gray-600">{shipment.receiverPhone}</p>
            </div>
          </div>
          {shipment.memo && (
            <div className="mt-3 rounded-xl border border-gray-100 bg-gray-50 p-3.5 text-xs text-gray-700">
              <span className="font-bold text-gray-500">요청사항: </span>{shipment.memo}
            </div>
          )}
        </div>

        <div className="rounded-2xl bg-white p-5 shadow-sm">
          <p className="text-xs font-semibold text-emerald-600">STATUS TIMELINE</p>
          <div className="mt-4">
            <Timeline history={shipment.history || []} />
          </div>
        </div>

        {/* 배송 완료면 리뷰 쓰기 · 보기 (이 화면은 본인 고객만 열 수 있어서 isMine) */}
        {shipment.status === 'COMPLETED' && (
          <ReviewSection shipmentId={shipment.id} status={shipment.status} isMine />
        )}
      </div>

      {(canPay || canCancel) && (
        <div className="fixed inset-x-0 bottom-0 z-30 mx-auto flex max-w-[640px] gap-3 border-t border-gray-100 bg-white/95 px-5 py-3.5 shadow-[0_-4px_20px_rgba(0,0,0,0.06)] backdrop-blur">
          {canCancel && (
            <button
              type="button"
              onClick={() => { setActionError(''); setCancelModalOpen(true) }}
              disabled={actionLoading}
              className="flex-1 rounded-xl border border-red-200 bg-white px-4 py-3 text-sm font-bold text-red-600 hover:bg-red-50 disabled:opacity-50"
            >
              {shipment.status === 'REQUESTED' ? '배송 취소 · 환불' : '주문 취소'}
            </button>
          )}
          {canPay && (
            <button
              type="button"
              onClick={handlePayNow}
              disabled={actionLoading}
              className="flex-1 rounded-xl bg-emerald-600 px-4 py-3 text-sm font-bold text-white hover:bg-emerald-700 disabled:opacity-50"
            >
              {actionLoading ? '결제창 준비 중...' : '결제하기'}
            </button>
          )}
        </div>
      )}

      {cancelModalOpen && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 px-5">
          <form onSubmit={handleCancelSubmit} className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
            <h2 className="text-lg font-bold text-gray-900">
              {shipment.status === 'REQUESTED' ? '배송 취소 및 결제 환불' : '배송 주문 취소'}
            </h2>
            <p className="mt-1 text-xs text-gray-500">
              {shipment.status === 'REQUESTED'
                ? '취소 시 결제된 운임 전액이 환불되며 기사 배차 요청이 마감됩니다.'
                : '결제 대기 주문을 취소합니다.'}
            </p>
            <label className="mt-4 block text-xs font-bold text-gray-700" htmlFor="cancelReason">
              취소 사유 (선택)
              <input
                id="cancelReason"
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                placeholder="예: 일정 변경으로 인한 취소"
                className="mt-1.5 w-full rounded-xl border border-gray-200 px-3 py-2.5 text-sm focus:border-emerald-500 focus:outline-none"
              />
            </label>
            <div className="mt-5 flex gap-2.5">
              <button
                type="button"
                onClick={() => setCancelModalOpen(false)}
                disabled={actionLoading}
                className="flex-1 rounded-xl border border-gray-200 py-2.5 text-sm font-bold text-gray-600"
              >
                닫기
              </button>
              <button
                type="submit"
                disabled={actionLoading}
                className="flex-1 rounded-xl bg-red-600 py-2.5 text-sm font-bold text-white hover:bg-red-700 disabled:opacity-50"
              >
                {actionLoading ? '처리 중...' : '취소 확정'}
              </button>
            </div>
          </form>
        </div>
      )}
    </section>
  )
}

export default ShipmentDetailPage