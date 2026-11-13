import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { getAvailableShipments, applyDispatch, cancelDispatch } from '../../api/dispatchApi'
import { getMyVehicles } from '../../api/vehicleApi'

// 기사: 운송 요청 상세 페이지
const DriverShipmentDetailPage = () => {
    const { shipmentId } = useParams()
    const [searchParams] = useSearchParams()

    // 어느 목록에서 상세 페이지로 들어왔는지 확인
    const fromMyDispatches = searchParams.get('from') === 'my-dispatches'

    const backPath = fromMyDispatches
        ? '/driver/dispatches?tab=pending'
        : '/driver/shipments'
    const [shipment, setShipment] = useState(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')

    // 기사 본인 차량 목록 및 선택한 차량
    const [myVehicles, setMyVehicles] = useState([])
    const [selectedVehicleId, setSelectedVehicleId] = useState('')
    const [vehiclesLoading, setVehiclesLoading] = useState(true)
    const [vehicleError, setVehicleError] = useState('')

    // 배차 신청 처리 상태
    const [applying, setApplying] = useState(false)
    const [applyError, setApplyError] = useState('')
    const [applySuccess, setApplySuccess] = useState(false)

    // 배차 신청 취소 처리 상태
    const [cancelling, setCancelling] = useState(false)
    const [cancelError, setCancelError] = useState('')
    const [cancelSuccess, setCancelSuccess] = useState(false)

    // 차종별 이름 (목록 화면과 동일)
    const vehicleTypeNames = {
        1: '소형·다마스/라보',
        2: '1톤',
        3: '2.5톤',
        4: '5톤',
    }

    useEffect(() => {
        const loadShipment = async () => {
            try {
                // 현재는 목록 API에서 해당 운송 요청을 찾아 사용
                const data = await getAvailableShipments()
                const selected = data.find(
                    (item) => String(item.shipmentId) === shipmentId
                )

                setShipment(selected || null)
            } catch (err) {
                console.error('운송 요청 상세 조회 실패:', err)
                setError('운송 요청 정보를 불러오지 못했습니다.')
            } finally {
                setLoading(false)
            }
        }

        loadShipment()
    }, [shipmentId])

    // 로그인한 기사 본인 차량 목록 조회
    useEffect(() => {
        const loadMyVehicles = async () => {
            try {
                const data = await getMyVehicles()
                setMyVehicles(data)
            } catch (err) {
                console.error('내 차량 목록 조회 실패:', err)
                setVehicleError('차량 목록을 불러오지 못했습니다.')
            } finally {
                setVehiclesLoading(false)
            }
        }

        loadMyVehicles()
    }, [])

    // 요청 차종과 일치하고 운행 가능한 차량만 선택
    const availableVehicles = shipment
        ? myVehicles.filter(
            (vehicle) =>
                vehicle.vehicleTypeId === shipment.vehicleTypeId &&
                vehicle.status === 'AVAILABLE'
        )
        : []

    // 기사 배차 신청
    const handleApplyDispatch = async () => {
        if (!shipment?.canApply || !selectedVehicleId || applying || applySuccess) {
            return
        }

        setApplying(true)
        setApplyError('')

        try {
            await applyDispatch(Number(shipmentId), Number(selectedVehicleId))
            setApplySuccess(true)
            setShipment((prev) => prev ? { ...prev, alreadyApplied: true, canApply: false } : prev)
        } catch (err) {
            console.error('배차 신청 실패:', err)
            setApplyError(
                err.response?.data?.message || '배차 신청 중 오류가 발생했습니다.'
            )
        } finally {
            setApplying(false)
        }
    }

    // 기사 배차 신청 취소
    const handleCancelDispatch = async () => {
        if (!shipment?.alreadyApplied || cancelling) {
            return
        }

        const confirmed = window.confirm(
            '배차 신청을 취소하시겠습니까?\n배차 확정 전까지만 취소할 수 있습니다.'
        )

        if (!confirmed) return

        setCancelling(true)
        setCancelError('')

        try {
            await cancelDispatch(Number(shipmentId))

            setApplySuccess(false)
            setCancelSuccess(true)

            // 취소 성공 후 이전 신청 상태 해제
            setShipment((prev) => prev ? {
                ...prev,
                alreadyApplied: false,
                canApply: false,
                blockReasons: prev.blockReasons?.filter(
                    (reason) => reason !== '이미 배차 신청한 운송 요청입니다.'
                ) || [],
            } : prev)

            // 서버에서 신청 가능 여부를 다시 계산해 가져오기
            try {
                const data = await getAvailableShipments()
                const updated = data.find(
                    (item) => String(item.shipmentId) === String(shipmentId)
                )

                if (updated) {
                    setShipment(updated)
                }
            } catch (refreshError) {
                console.error('신청 취소 후 목록 갱신 실패:', refreshError)
            }

        } catch (err) {
            console.error('배차 신청 취소 실패:', err)
            setCancelError(
                err.response?.data?.message || '배차 신청 취소 중 오류가 발생했습니다.'
            )
        } finally {
            setCancelling(false)
        }
    }

    return (
        <section className="grid gap-5">
            {/* 목록으로 돌아가기 */}
            <Link
                to={backPath}
                className="w-fit text-sm text-gray-500 transition hover:text-blue-600"
            >
                {fromMyDispatches ? '← 신청 대기 목록으로' : '← 운송 요청 목록으로'}
            </Link>

            <div>
                <h1 className="text-2xl font-bold text-gray-900">운송 요청 상세</h1>
                <p className="mt-1 text-sm text-gray-500">
                    운송 정보와 배차 신청 가능 여부를 확인할 수 있습니다.
                </p>
            </div>

            {loading ? (
                <p className="text-gray-500">불러오는 중...</p>
            ) : error ? (
                <p className="text-red-500">{error}</p>
            ) : !shipment ? (
                <p className="rounded-2xl bg-white p-6 text-gray-500 shadow-sm">
                    현재 확인할 수 없는 운송 요청입니다.
                </p>
            ) : (
                <div className="rounded-2xl border border-gray-200 bg-white p-5 shadow-md sm:p-7">

                    {/* 화물명 및 신청 가능 여부 */}
                    <div className="flex flex-wrap items-center justify-between gap-4">
                        <div>
                            <p className="mb-1 text-xs font-medium text-gray-500">
                                운송 정보
                            </p>
                            <h2 className="text-2xl font-bold text-gray-900">
                                {shipment.cargoName}
                            </h2>
                        </div>

                        <span
                            className={`rounded-full px-3 py-1.5 text-xs font-semibold ${shipment.alreadyApplied
                                ? 'bg-green-100 text-green-700'
                                : shipment.canApply
                                    ? 'bg-blue-50 text-blue-700'
                                    : 'bg-gray-100 text-gray-600'
                                }`}
                        >
                            {shipment.alreadyApplied ? '이미 신청함' : shipment.canApply ? '배차 신청 가능' : '배차 신청 불가'}
                        </span>
                    </div>

                    {/* 운송 경로 */}
                    <div className="mt-7">
                        <h3 className="mb-3 text-sm font-bold text-gray-800">
                            운송 경로
                        </h3>

                        <div className="grid items-center gap-3 rounded-xl border border-gray-200 bg-gray-100 p-5 md:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)]">
                            <div className="min-w-0">
                                <p className="mb-2 text-xs font-medium text-gray-500">
                                    출발지
                                </p>
                                <p className="text-base font-semibold leading-relaxed text-gray-900">
                                    {shipment.originAddress}
                                </p>
                            </div>

                            <div className="text-xl font-semibold text-blue-500">
                                <span className="md:hidden">↓</span>
                                <span className="hidden md:inline">→</span>
                            </div>

                            <div className="min-w-0">
                                <p className="mb-2 text-xs font-medium text-gray-500">
                                    도착지
                                </p>
                                <p className="text-base font-semibold leading-relaxed text-gray-900">
                                    {shipment.destAddress}
                                </p>
                            </div>
                        </div>
                    </div>

                    {/* 운송 상세 정보 */}
                    <div className="mt-7">
                        <h3 className="mb-4 text-sm font-bold text-gray-800">
                            운송 상세 정보
                        </h3>

                        <div className="grid grid-cols-2 gap-x-5 gap-y-5 sm:grid-cols-4">
                            <div>
                                <p className="mb-2 text-xs text-gray-500">화물 무게</p>
                                <p className="font-semibold text-gray-900">
                                    {shipment.cargoWeightKg}kg
                                </p>
                            </div>

                            <div>
                                <p className="mb-2 text-xs text-gray-500">요청 차종</p>
                                <p className="font-semibold text-gray-900">
                                    차종 {shipment.vehicleTypeId}
                                    {' '}({vehicleTypeNames[shipment.vehicleTypeId] || '정보 없음'})
                                </p>
                            </div>

                            <div>
                                <p className="mb-2 text-xs text-gray-500">상차 예정일</p>
                                <p className="font-semibold text-gray-900">
                                    {new Date(shipment.pickupAt).toLocaleString('ko-KR')}
                                </p>
                            </div>

                            <div>
                                <p className="mb-2 text-xs text-gray-500">예상 운송 시간</p>
                                <p className="font-semibold text-gray-900">
                                    {shipment.durationMin > 0
                                        ? `${shipment.durationMin}분`
                                        : '확인 필요'}
                                </p>
                            </div>
                        </div>
                    </div>

                    {/* 예상 운임 */}
                    <div className="mt-7 flex flex-wrap items-center justify-between gap-3 border-t border-gray-100 pt-5">
                        <span className="text-sm font-semibold text-gray-700">
                            예상 운임
                        </span>
                        <strong className="text-2xl font-bold text-blue-600">
                            {shipment.fare.toLocaleString('ko-KR')}원
                        </strong>
                    </div>

                    {/* 배차 신청 불가 사유 */}
                    {!shipment.alreadyApplied && shipment.blockReasons?.length > 0 && (
                        <div className="mt-5 space-y-2 rounded-xl border border-red-100 bg-red-50 p-4">
                            {shipment.blockReasons.map((reason, index) => (
                                <p key={index} className="text-sm font-semibold text-red-700">
                                    ⛔ {reason}
                                </p>
                            ))}
                        </div>
                    )}

                    {/* 주의사항 */}
                    {shipment.warningMessages?.length > 0 && (
                        <div className="mt-3 space-y-2 rounded-xl border border-amber-100 bg-amber-50 p-4">
                            {shipment.warningMessages.map((message, index) => (
                                <p key={index} className="text-sm font-medium text-amber-800">
                                    ⚠️ {message}
                                </p>
                            ))}
                        </div>
                    )}

                    {/* 배차 신청 버튼 */}
                    <>
                        {/* 신청 취소 완료 안내 */}
                        {cancelSuccess && (
                            <div className="mt-6 rounded-xl border border-green-100 bg-green-50 p-4 text-center text-sm font-semibold text-green-800">
                                배차 신청이 취소되었습니다.
                            </div>
                        )}

                        {shipment.alreadyApplied ? (
                            <div className="mt-6 space-y-3">

                                {/* 이미 신청한 요청 안내 */}
                                <div className="rounded-xl border border-green-100 bg-green-50 p-4 text-center text-sm font-semibold text-green-800">
                                    {applySuccess
                                        ? '배차 신청이 완료되었습니다. 고객의 선택을 기다려 주세요.'
                                        : '이미 신청한 운송 요청입니다. 고객의 선택을 기다려 주세요.'}
                                </div>

                                {/* 배차 신청 취소 버튼 */}
                                <button
                                    type="button"
                                    onClick={handleCancelDispatch}
                                    disabled={cancelling}
                                    className="w-full rounded-xl border border-red-200 bg-white px-6 py-3.5 text-sm font-semibold text-red-600 transition hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
                                >
                                    {cancelling ? '취소 처리 중...' : '배차 신청 취소'}
                                </button>

                                {/* 배차 신청 취소 실패 */}
                                {cancelError && (
                                    <p className="text-center text-sm font-semibold text-red-600">
                                        {cancelError}
                                    </p>
                                )}
                            </div>
                        ) : (
                            <div className="mt-6 border-t border-gray-100 pt-6">
                                {/* 배차에 사용할 차량 선택 */}
                                <label className="mb-2 block text-sm font-semibold text-gray-700">
                                    배차에 사용할 내 차량
                                </label>

                                <select
                                    value={selectedVehicleId}
                                    onChange={(e) => setSelectedVehicleId(e.target.value)}
                                    disabled={!shipment.canApply || vehiclesLoading || availableVehicles.length === 0}
                                    className="mb-4 h-12 w-full rounded-xl border border-gray-200 bg-gray-50 px-4 text-sm text-gray-700 disabled:cursor-not-allowed disabled:opacity-60"
                                >
                                    <option value="">
                                        {vehiclesLoading ? '차량 불러오는 중...' : '차량을 선택해 주세요'}
                                    </option>

                                    {availableVehicles.map((vehicle) => (
                                        <option key={vehicle.vehicleId} value={vehicle.vehicleId}>
                                            {vehicle.plateNo} (차종 {vehicle.vehicleTypeId})
                                        </option>
                                    ))}
                                </select>

                                {vehicleError && (
                                    <p className="mb-3 text-sm text-red-600">
                                        {vehicleError}
                                    </p>
                                )}

                                {shipment.canApply &&
                                    !vehiclesLoading &&
                                    !vehicleError &&
                                    availableVehicles.length === 0 && (
                                        <p className="mb-3 text-sm text-red-600">
                                            이 운송 요청에 사용할 수 있는 차량이 없습니다.
                                        </p>
                                    )}

                                {/* 배차 신청 */}
                                <button
                                    type="button"
                                    onClick={handleApplyDispatch}
                                    disabled={!shipment.canApply || !selectedVehicleId || applying || applySuccess}
                                    className={`w-full rounded-xl px-6 py-3.5 text-base font-bold transition ${shipment.canApply && selectedVehicleId && !applying && !applySuccess
                                        ? 'cursor-pointer bg-blue-600 text-white hover:bg-blue-700'
                                        : 'cursor-not-allowed bg-gray-200 text-gray-400'
                                        }`}
                                >
                                    {applying ? '신청 처리 중...' : applySuccess ? '신청 완료' : '배차 신청'}
                                </button>

                                {/* 배차 신청 성공 */}
                                {applySuccess && (
                                    <p className="mt-3 text-center text-sm font-semibold text-green-700">
                                        배차 신청이 완료되었습니다. 고객의 선택을 기다려 주세요.
                                    </p>
                                )}

                                {/* 배차 신청 실패 */}
                                {applyError && (
                                    <p className="mt-3 text-center text-sm font-semibold text-red-600">
                                        {applyError}
                                    </p>
                                )}
                            </div>
                        )}
                    </>

                </div>
            )}
        </section>
    )
}

export default DriverShipmentDetailPage
