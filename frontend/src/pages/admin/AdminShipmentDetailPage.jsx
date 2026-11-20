import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getShipment } from '../../api/shipmentApi'
import { cancelAdminShipmentDispatch } from '../../api/adminTransportApi'

// 관리자: 고객·기사의 조작 버튼 없이 배송 정보를 조회하는 상세 화면
const STATUS_META = {
  PENDING_PAYMENT: { label: '결제 대기', color: 'bg-amber-50 text-amber-700 border-amber-200' },
  REQUESTED: { label: '배차 대기', color: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  ASSIGNED: { label: '배차 완료', color: 'bg-blue-50 text-blue-700 border-blue-200' },
  PICKED_UP: { label: '상차 완료', color: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  IN_TRANSIT: { label: '배송 중', color: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  ARRIVED: { label: '도착', color: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  COMPLETED: { label: '배송 완료', color: 'bg-gray-100 text-gray-700 border-gray-200' },
  CANCELED: { label: '취소됨', color: 'bg-red-50 text-red-700 border-red-200' },
}

const formatDateTime = (value) => value ? value.replace('T', ' ').slice(0, 16) : '-'
const formatFare = (value) => `${Number(value ?? 0).toLocaleString('ko-KR')}원`

const AdminShipmentDetailPage = () => {
  const { id } = useParams()
  const [fetchState, setFetchState] = useState({ id: null, shipment: null, error: '' })
  const loading = fetchState.id !== id
  const { shipment, error } = fetchState
  const [showCancelForm, setShowCancelForm] = useState(false)
  const [cancelReason, setCancelReason] = useState('')
  const [cancelling, setCancelling] = useState(false)
  const [cancelError, setCancelError] = useState('')
  const [cancelSuccess, setCancelSuccess] = useState('')

  const handleCancelDispatch = async () => {
    const reason = cancelReason.trim()
    if (!reason || reason.length > 180 || cancelling) {
      setCancelError('180자 이내의 배차 취소 사유를 입력해 주세요.')
      return
    }
    if (!window.confirm('선택한 배송의 배차만 해제하고 다시 배차 대기로 변경하시겠습니까?\n결제와 주문 자체는 취소되지 않습니다.')) return

    setCancelling(true)
    setCancelError('')
    setCancelSuccess('')
    try {
      await cancelAdminShipmentDispatch(id, reason)
      setCancelSuccess('배차가 해제되었습니다. 배송은 배차 대기 상태로 변경되었습니다.')
      setCancelReason('')
      setShowCancelForm(false)
      try {
        const updated = await getShipment(id)
        setFetchState({ id, shipment: updated, error: '' })
      } catch {
        setCancelError('해제는 완료되었지만 화면 갱신에 실패했습니다. 새로고침해 주세요.')
      }
    } catch (err) {
      setCancelError(err.response?.data?.message || '배차 해제에 실패했습니다.')
    } finally {
      setCancelling(false)
    }
  }

  useEffect(() => {
    let active = true

    getShipment(id)
      .then((data) => {
        if (!active) return
        setFetchState({ id, shipment: data, error: '' })
      })
      .catch((err) => {
        if (!active) return
        setFetchState({
          id,
          shipment: null,
          error: err.response?.data?.message || '배송 상세 정보를 불러오지 못했습니다.',
        })
      })

    return () => { active = false }
  }, [id])

  const status = shipment && STATUS_META[shipment.status]
  const canCancelDispatch = shipment?.status === 'ASSIGNED'

  return (
    <section className="mx-auto w-full max-w-4xl space-y-4 pb-10">
      <Link to="/admin/shipments" className="inline-block text-sm text-gray-500 hover:text-emerald-700">
        ← 배송 통합 검색으로
      </Link>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-xl font-bold text-gray-900">배송 상세 #{id}</h1>
        {!loading && shipment && (
          <span className={`rounded-full border px-3 py-1 text-xs font-bold ${status?.color || 'border-gray-200 bg-gray-100 text-gray-700'}`}>
            {status?.label || shipment.status}
          </span>
        )}
      </div>

      {loading ? (
        <div className="rounded-2xl bg-white p-8 text-center text-sm text-gray-500 shadow-sm">배송 정보를 불러오는 중입니다...</div>
      ) : error || !shipment ? (
        <div role="alert" className="rounded-2xl border border-red-200 bg-red-50 p-5 text-sm text-red-700">
          {error || '배송 정보를 찾을 수 없습니다.'}
        </div>
      ) : (
        <>
          {/* 관리자 배차 취소: 배차 완료일 때 활성화, 그 외 상태도 사유를 안내한다 */}
          <div className="rounded-2xl border border-red-100 bg-white p-5 shadow-sm sm:p-6">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <h2 className="text-sm font-bold text-gray-900">관리자 배차 관리</h2>
                <p className="mt-2 text-xs leading-5 text-gray-600">
                  배차만 해제하고 다시 배차 대기로 변경합니다. 결제와 주문은 유지되며 고객·기사에게 알림이 전송됩니다.
                </p>
              </div>
              <button
                type="button"
                disabled={!canCancelDispatch || cancelling}
                onClick={() => { setShowCancelForm((open) => !open); setCancelError('') }}
                className="shrink-0 rounded-lg border border-red-300 bg-red-50 px-4 py-2 text-sm font-semibold text-red-700 hover:bg-red-100 disabled:cursor-not-allowed disabled:border-gray-200 disabled:bg-gray-100 disabled:text-gray-400"
                title={!canCancelDispatch ? '배차 완료(상차 전) 상태에서만 취소할 수 있습니다.' : undefined}
              >
                배차 취소
              </button>
            </div>
            {!canCancelDispatch && (
              <p className="mt-3 rounded-lg bg-gray-50 px-3 py-2 text-xs text-gray-600">
                현재 상태: {status?.label || shipment.status}. 배차 완료 후 상차 전인 배송만 배차 취소할 수 있습니다.
              </p>
            )}
            {canCancelDispatch && showCancelForm && (
              <div className="mt-4 border-t border-gray-100 pt-4">
                <label htmlFor="admin-cancel-reason" className="block text-xs font-semibold text-gray-700">
                  배차 취소 사유 (필수)
                </label>
                <textarea
                  id="admin-cancel-reason"
                  value={cancelReason}
                  onChange={(event) => setCancelReason(event.target.value)}
                  maxLength={180}
                  rows={2}
                  placeholder="배차를 취소하는 이유를 입력해 주세요."
                  className="mt-2 w-full rounded-xl border border-gray-200 p-3 text-sm outline-none focus:border-red-400"
                />
                <div className="mt-3 flex justify-end gap-2">
                  <button type="button" disabled={cancelling} onClick={() => { setShowCancelForm(false); setCancelReason(''); setCancelError('') }}
                    className="rounded-lg border border-gray-200 px-4 py-2 text-sm font-semibold text-gray-600 hover:bg-gray-50">
                    닫기
                  </button>
                  <button
                    type="button"
                    disabled={cancelling || !cancelReason.trim()}
                    onClick={handleCancelDispatch}
                    className="rounded-lg bg-red-600 px-4 py-2 text-sm font-semibold text-white hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {cancelling ? '취소 처리 중...' : '배차 취소 확정'}
                  </button>
                </div>
              </div>
            )}
            {cancelError && <p role="alert" className="mt-3 text-sm text-red-700">{cancelError}</p>}
          </div>
          {cancelSuccess && <p role="status" className="rounded-xl bg-emerald-50 p-3 text-sm text-emerald-800">{cancelSuccess}</p>}

          {/* 운송 경로 */}
          <div className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm sm:p-6">
            <h2 className="text-sm font-bold text-emerald-700">ROUTE &amp; SCHEDULE</h2>
            <div className="mt-4 space-y-4">
              <div>
                <p className="text-xs text-gray-500">출발지</p>
                <p className="mt-1 font-semibold text-gray-900">{shipment.originAddress} {shipment.originDetail || ''}</p>
              </div>
              <div className="h-5 border-l-2 border-dashed border-gray-200 ml-1" aria-hidden="true" />
              <div>
                <p className="text-xs text-gray-500">도착지</p>
                <p className="mt-1 font-semibold text-gray-900">{shipment.destAddress} {shipment.destDetail || ''}</p>
              </div>
            </div>
            <div className="mt-4 grid gap-3 rounded-xl bg-gray-50 p-4 text-sm sm:grid-cols-3">
              <div><p className="text-xs text-gray-500">상차 예정일</p><p className="mt-1 font-semibold">{formatDateTime(shipment.pickupAt)}</p></div>
              <div><p className="text-xs text-gray-500">예상 거리</p><p className="mt-1 font-semibold">{shipment.distanceKm ?? '-'}km</p></div>
              <div><p className="text-xs text-gray-500">예상 운송 시간</p><p className="mt-1 font-semibold">{shipment.durationMin != null ? `${shipment.durationMin}분` : '-'}</p></div>
            </div>
          </div>

          {/* 화물 및 운임 */}
          <div className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm sm:p-6">
            <h2 className="text-sm font-bold text-emerald-700">CARGO &amp; FARE</h2>
            <p className="mt-3 text-lg font-bold text-gray-900">{shipment.cargoName}</p>
            <div className="mt-3 grid gap-3 text-sm sm:grid-cols-3">
              <div><p className="text-xs text-gray-500">화물 종류</p><p className="mt-1 font-medium">{shipment.cargoTypeName || '-'}</p></div>
              <div><p className="text-xs text-gray-500">요청 차종</p><p className="mt-1 font-medium">{shipment.vehicleTypeName || '-'}</p></div>
              <div><p className="text-xs text-gray-500">화물 무게</p><p className="mt-1 font-medium">{shipment.cargoWeightKg}kg</p></div>
            </div>
            <div className="mt-4 flex items-center justify-between border-t border-gray-100 pt-4">
              <span className="text-sm font-semibold text-gray-700">운임</span>
              <strong className="text-xl text-emerald-700">{formatFare(shipment.fare)}</strong>
            </div>
          </div>

          {/* 연락처 */}
          <div className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm sm:p-6">
            <h2 className="text-sm font-bold text-emerald-700">CONTACTS</h2>
            <div className="mt-3 grid gap-3 sm:grid-cols-2">
              <div className="rounded-xl bg-gray-50 p-4">
                <p className="text-xs text-gray-500">보내는 분</p>
                <p className="mt-1 font-semibold">{shipment.senderName || '정보 없음'}</p>
                <p className="text-sm text-gray-600">{shipment.senderPhone || '연락처 없음'}</p>
              </div>
              <div className="rounded-xl bg-gray-50 p-4">
                <p className="text-xs text-gray-500">받는 분</p>
                <p className="mt-1 font-semibold">{shipment.receiverName || '정보 없음'}</p>
                <p className="text-sm text-gray-600">{shipment.receiverPhone || '연락처 없음'}</p>
              </div>
            </div>
          </div>

          {/* 고객 요청사항 */}
          <div className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm sm:p-6">
            <h2 className="text-sm font-bold text-emerald-700">CUSTOMER REQUEST</h2>
            <p className="mt-3 rounded-xl bg-gray-50 p-4 text-sm text-gray-800">{shipment.memo?.trim() || '요청사항 없음'}</p>
          </div>

          {/* 상태 변경 이력 */}
          <div className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm sm:p-6">
            <h2 className="text-sm font-bold text-emerald-700">STATUS HISTORY</h2>
            {shipment.history?.length > 0 ? (
              <ol className="mt-4 divide-y divide-gray-100">
                {shipment.history.map((entry) => (
                  <li key={entry.id} className="flex flex-wrap items-start justify-between gap-2 py-3">
                    <div>
                      <p className="text-sm font-semibold text-gray-900">
                        {STATUS_META[entry.toStatus]?.label || entry.toStatus || '상태 변경'}
                      </p>
                      {entry.memo && <p className="mt-1 text-xs text-gray-600">{entry.memo}</p>}
                      {entry.changedByName && <p className="mt-1 text-xs text-gray-400">처리자: {entry.changedByName}</p>}
                    </div>
                    <time className="text-xs text-gray-500">{formatDateTime(entry.changedAt)}</time>
                  </li>
                ))}
              </ol>
            ) : (
              <p className="mt-3 text-sm text-gray-500">등록된 상태 변경 이력이 없습니다.</p>
            )}
          </div>
        </>
      )}
    </section>
  )
}

export default AdminShipmentDetailPage
