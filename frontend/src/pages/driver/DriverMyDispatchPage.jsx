import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import {
    getAvailableShipments,
    getMyAssignedDispatches,
    getMyDispatchHistory,
} from '../../api/dispatchApi'

// 기사: 내가 신청한 운송 요청 및 배차 내역 관리
const DriverMyDispatchPage = () => {
    // 현재 선택한 탭을 URL에 저장
    const [searchParams, setSearchParams] = useSearchParams()

    const tabParam = searchParams.get('tab')
    const activeTab = ['pending', 'assigned', 'history'].includes(tabParam)
        ? tabParam
        : 'pending'
    const [appliedShipments, setAppliedShipments] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')

    // 배차 확정 목록
    const [assignedDispatches, setAssignedDispatches] = useState([])
    const [assignedLoading, setAssignedLoading] = useState(false)
    const [assignedError, setAssignedError] = useState('')

    // 지난 배차 신청 및 배송 내역
    const [dispatchHistory, setDispatchHistory] = useState([])
    const [historyLoading, setHistoryLoading] = useState(false)
    const [historyError, setHistoryError] = useState('')

    // 현재 API에서 조회 가능한 신청 대기 목록
    useEffect(() => {
        const loadMyApplications = async () => {
            try {
                const data = await getAvailableShipments()

                // 로그인한 기사가 이미 신청한 REQUESTED 요청만 표시
                const myApplications = data.filter(
                    (shipment) => shipment.alreadyApplied === true
                )

                setAppliedShipments(myApplications)
            } catch (err) {
                console.error('내 배차 신청 목록 조회 실패:', err)
                setError('배차 신청 목록을 불러오지 못했습니다.')
            } finally {
                setLoading(false)
            }
        }

        loadMyApplications()
    }, [])

    // 기사: 본인의 지난 배차 내역 조회
    useEffect(() => {
        if (activeTab !== 'history') return

        let isActive = true

        const loadDispatchHistory = async () => {
            setHistoryLoading(true)
            setHistoryError('')

            try {
                const data = await getMyDispatchHistory()

                if (isActive) {
                    setDispatchHistory(data)
                }
            } catch (err) {
                console.error('지난 배차 내역 조회 실패:', err)

                if (isActive) {
                    setHistoryError('지난 배차 내역을 불러오지 못했습니다.')
                }
            } finally {
                if (isActive) {
                    setHistoryLoading(false)
                }
            }
        }

        loadDispatchHistory()

        return () => {
            isActive = false
        }
    }, [activeTab])

    // 기사: 본인의 배차 확정 목록 조회
    useEffect(() => {
        if (activeTab !== 'assigned') return

        let isActive = true

        const loadAssignedDispatches = async () => {
            setAssignedLoading(true)
            setAssignedError('')

            try {
                const data = await getMyAssignedDispatches()

                if (isActive) {
                    setAssignedDispatches(data)
                }
            } catch (err) {
                console.error('배차 확정 목록 조회 실패:', err)

                if (isActive) {
                    setAssignedError('배차 확정 목록을 불러오지 못했습니다.')
                }
            } finally {
                if (isActive) {
                    setAssignedLoading(false)
                }
            }
        }

        loadAssignedDispatches()

        return () => {
            isActive = false
        }
    }, [activeTab])

    const tabs = [
        { id: 'pending', label: '신청 대기' },
        { id: 'assigned', label: '배차 확정' },
        { id: 'history', label: '지난 내역' },
    ]

    // 배송 진행 상태별 이름
    const dispatchStatusNames = {
        ASSIGNED: '배차 확정',
        PICKED_UP: '상차 완료',
        IN_TRANSIT: '운송 중',
        ARRIVED: '도착',
    }

    // 차종별 이름 (운송 요청 목록과 동일)
    const vehicleTypeNames = {
        1: '소형·다마스/라보',
        2: '1톤',
        3: '2.5톤',
        4: '5톤',
    }

    // 지난 배차 내역 상태별 이름
    const historyStatusNames = {
        WITHDRAWN: '신청 취소',
        REJECTED: '신청 거절',
        NOT_SELECTED: '미선정',
        COMPLETED: '배송 완료',
        CANCELED: '배송 취소',
    }

    // 지난 내역 상태별 배지 색상
    const historyStatusColors = {
        WITHDRAWN: 'bg-gray-100 text-gray-600',
        REJECTED: 'bg-red-50 text-red-600',
        NOT_SELECTED: 'bg-gray-100 text-gray-600',
        COMPLETED: 'bg-green-100 text-green-700',
        CANCELED: 'bg-gray-100 text-gray-600',
    }

    return (
        <section className="grid gap-5">
            {/* 기사 홈으로 돌아가기 */}
            <Link
                to="/driver"
                className="w-fit text-sm text-gray-500 hover:text-blue-600"
            >
                ← 기사 홈으로
            </Link>

            {/* 페이지 제목 */}
            <div>
                <h1 className="text-2xl font-bold text-gray-900">
                    내 배차 관리
                </h1>
                <p className="mt-2 text-sm text-gray-600">
                    내가 신청한 운송 요청과 배차 현황을 확인할 수 있습니다.
                </p>
            </div>

            {/* 배차 상태별 탭 */}
            <div className="grid grid-cols-3 gap-2 rounded-xl bg-gray-100 p-1">
                {tabs.map((tab) => (
                    <button
                        key={tab.id}
                        type="button"
                        onClick={() => setSearchParams({ tab: tab.id })}
                        className={`rounded-lg px-2 py-3 text-sm font-semibold transition ${activeTab === tab.id
                            ? 'bg-white text-blue-600 shadow-sm'
                            : 'text-gray-500 hover:text-gray-800'
                            }`}
                    >
                        {tab.label}
                    </button>
                ))}
            </div>

            {/* 신청 대기 목록 */}
            {activeTab === 'pending' && (
                <div className="grid gap-4">
                    {loading ? (
                        <p className="text-gray-500">불러오는 중...</p>
                    ) : error ? (
                        <p className="rounded-xl bg-red-50 p-5 text-red-600">
                            {error}
                        </p>
                    ) : appliedShipments.length === 0 ? (
                        <div className="rounded-xl border bg-white p-8 text-center">
                            <p className="text-gray-600">
                                현재 신청 대기 중인 운송 요청이 없습니다.
                            </p>
                            <Link
                                to="/driver/shipments"
                                className="mt-4 inline-block text-sm font-semibold text-blue-600"
                            >
                                운송 요청 찾아보기 →
                            </Link>
                        </div>
                    ) : (
                        appliedShipments.map((shipment) => (
                            <Link
                                key={shipment.shipmentId}
                                to={`/driver/shipments/${shipment.shipmentId}?from=my-dispatches`}
                                className="block w-full rounded-xl border border-green-100 bg-green-50 p-5 text-inherit no-underline shadow-sm transition-shadow hover:shadow-md"
                            >
                                <div className="grid gap-4 lg:grid-cols-[minmax(0,1.5fr)_minmax(0,1fr)] lg:items-center">

                                    {/* 왼쪽: 화물명 및 운송 경로 */}
                                    <div className="min-w-0">
                                        <div className="mb-3 flex flex-wrap items-center gap-3">
                                            <h2 className="text-lg font-bold text-gray-900">
                                                {shipment.cargoName}
                                            </h2>

                                            <span className="rounded-full bg-green-200 px-3 py-1 text-xs font-semibold text-green-800">
                                                신청 대기 중
                                            </span>
                                        </div>

                                        <div className="flex flex-wrap items-center gap-3 rounded-lg bg-white/80 p-3">
                                            <div className="min-w-0 flex-1">
                                                <p className="mb-1 text-xs text-gray-500">출발지</p>
                                                <p className="text-sm font-semibold text-gray-900">
                                                    {shipment.originAddress}
                                                </p>
                                            </div>

                                            <span className="text-lg font-bold text-blue-500">→</span>

                                            <div className="min-w-0 flex-1">
                                                <p className="mb-1 text-xs text-gray-500">도착지</p>
                                                <p className="text-sm font-semibold text-gray-900">
                                                    {shipment.destAddress}
                                                </p>
                                            </div>
                                        </div>
                                    </div>

                                    {/* 오른쪽: 운송 정보 및 운임 */}
                                    <div className="grid grid-cols-2 gap-3 text-sm lg:border-l lg:border-green-100 lg:pl-5">
                                        <div>
                                            <p className="text-xs text-gray-500">화물 무게</p>
                                            <p className="font-semibold text-gray-800">
                                                {shipment.cargoWeightKg}kg
                                            </p>
                                        </div>

                                        <div>
                                            <p className="text-xs text-gray-500">요청 차종</p>
                                            <p className="font-semibold text-gray-800">
                                                차종 {shipment.vehicleTypeId}
                                                {' '}({vehicleTypeNames[shipment.vehicleTypeId] || '정보 없음'})
                                            </p>
                                        </div>

                                        <div>
                                            <p className="text-xs text-gray-500">상차 예정일</p>
                                            <p className="font-semibold text-gray-800">
                                                {shipment.pickupAt
                                                    ? new Date(shipment.pickupAt).toLocaleString('ko-KR')
                                                    : '정보 없음'}
                                            </p>
                                        </div>

                                        <div>
                                            <p className="text-xs text-gray-500">예상 운임</p>
                                            <p className="text-lg font-bold text-blue-600">
                                                {shipment.fare.toLocaleString('ko-KR')}원
                                            </p>
                                        </div>
                                    </div>
                                </div>

                                {/* 상세 페이지 이동 안내 */}
                                <p className="mt-4 border-t border-green-100 pt-3 text-right text-sm font-semibold text-green-700">
                                    운송 요청 상세 보기 →
                                </p>
                            </Link>
                        ))

                    )}
                </div>
            )}


            {/* 배차 확정 목록 */}
            {activeTab === 'assigned' && (
                <div className="grid gap-4">
                    {assignedLoading ? (
                        <p className="text-gray-500">불러오는 중...</p>
                    ) : assignedError ? (
                        <p className="rounded-xl bg-red-50 p-5 text-red-600">
                            {assignedError}
                        </p>
                    ) : assignedDispatches.length === 0 ? (
                        <div className="rounded-xl border bg-white p-8 text-center">
                            <p className="text-gray-600">
                                현재 배차 확정된 운송 요청이 없습니다.
                            </p>
                        </div>
                    ) : (

                        assignedDispatches.map((dispatch) => (
                            <Link
                                key={dispatch.dispatchId}
                                to={`/driver/dispatches/${dispatch.shipmentId}`}
                                className="block w-full rounded-xl border border-gray-200 bg-white p-5 text-inherit no-underline shadow-sm transition-shadow hover:shadow-md"
                            >
                                <div className="grid gap-4 lg:grid-cols-[minmax(0,1.5fr)_minmax(0,1fr)] lg:items-center">

                                    {/* 왼쪽: 화물명 및 운송 경로 */}
                                    <div className="min-w-0">
                                        <div className="mb-3 flex flex-wrap items-center gap-3">
                                            <h2 className="text-lg font-bold text-gray-900">
                                                {dispatch.cargoName}
                                            </h2>

                                            <span className="rounded-full bg-blue-100 px-3 py-1 text-xs font-semibold text-blue-700">
                                                {dispatchStatusNames[dispatch.status] || dispatch.status}
                                            </span>
                                        </div>

                                        <div className="flex flex-wrap items-center gap-3 rounded-lg bg-gray-50 p-3">
                                            <div className="min-w-0 flex-1">
                                                <p className="mb-1 text-xs text-gray-500">출발지</p>
                                                <p className="text-sm font-semibold text-gray-900">
                                                    {dispatch.originAddress}
                                                </p>
                                            </div>

                                            <span className="text-lg font-bold text-blue-500">→</span>

                                            <div className="min-w-0 flex-1">
                                                <p className="mb-1 text-xs text-gray-500">도착지</p>
                                                <p className="text-sm font-semibold text-gray-900">
                                                    {dispatch.destAddress}
                                                </p>
                                            </div>
                                        </div>
                                    </div>

                                    {/* 오른쪽: 운송 정보 및 운임 */}
                                    <div className="grid grid-cols-2 gap-3 text-sm lg:border-l lg:border-gray-200 lg:pl-5">
                                        <div>
                                            <p className="text-xs text-gray-500">화물 무게</p>
                                            <p className="font-semibold text-gray-800">
                                                {dispatch.cargoWeightKg}kg
                                            </p>
                                        </div>

                                        <div>
                                            <p className="text-xs text-gray-500">요청 차종</p>
                                            <p className="font-semibold text-gray-800">
                                                차종 {dispatch.vehicleTypeId}
                                                {' '}({vehicleTypeNames[dispatch.vehicleTypeId] || '정보 없음'})
                                            </p>
                                        </div>

                                        <div>
                                            <p className="text-xs text-gray-500">상차 예정일</p>
                                            <p className="font-semibold text-gray-800">
                                                {dispatch.pickupAt
                                                    ? new Date(dispatch.pickupAt).toLocaleString('ko-KR')
                                                    : '정보 없음'}
                                            </p>
                                        </div>

                                        <div>
                                            <p className="text-xs text-gray-500">예상 운임</p>
                                            <p className="text-lg font-bold text-blue-600">
                                                {dispatch.fare.toLocaleString('ko-KR')}원
                                            </p>
                                        </div>
                                    </div>
                                </div>

                                {/* 배송 진행 상세 페이지 이동 */}
                                <p className="mt-4 border-t border-gray-100 pt-3 text-right text-sm font-semibold text-blue-600">
                                    배송 진행 상세 보기 →
                                </p>
                            </Link>
                        ))

                    )}
                </div>
            )}


            {/* 지난 배차 신청 및 배송 내역 */}
            {activeTab === 'history' && (
                <div className="grid gap-4">
                    {historyLoading ? (
                        <p className="text-gray-500">불러오는 중...</p>
                    ) : historyError ? (
                        <p className="rounded-xl bg-red-50 p-5 text-red-600">
                            {historyError}
                        </p>
                    ) : dispatchHistory.length === 0 ? (
                        <div className="rounded-xl border border-gray-100 bg-white p-8 text-center shadow-sm">
                            <p className="text-gray-500">
                                지난 배차 내역이 없습니다.
                            </p>
                        </div>
                    ) : (
                        dispatchHistory.map((history) => {
                            const status = history.applicationStatus === 'SELECTED'
                                ? history.shipmentStatus
                                : history.applicationStatus

                            return (
                                <Link
                                    key={history.applicationId}
                                    to={`/driver/dispatches/history/${history.applicationId}`}
                                    className="block rounded-xl border border-gray-200 bg-white p-5 text-inherit no-underline shadow-sm transition-shadow hover:shadow-md"
                                >
                                    <div className="grid gap-4 lg:grid-cols-[minmax(0,1.5fr)_minmax(0,1fr)] lg:items-center">

                                        {/* 왼쪽: 화물명 및 운송 경로 */}
                                        <div className="min-w-0">
                                            <div className="mb-3 flex flex-wrap items-center gap-3">
                                                <h2 className="text-lg font-bold text-gray-900">
                                                    {history.cargoName}
                                                </h2>

                                                <span
                                                    className={`rounded-full px-3 py-1 text-xs font-semibold ${historyStatusColors[status] || 'bg-gray-100 text-gray-600'
                                                        }`}
                                                >
                                                    {historyStatusNames[status] || status}
                                                </span>
                                            </div>

                                            <div className="flex flex-wrap items-center gap-3 rounded-lg bg-gray-50 p-3">
                                                <div className="min-w-0 flex-1">
                                                    <p className="mb-1 text-xs text-gray-500">출발지</p>
                                                    <p className="text-sm font-semibold text-gray-900">
                                                        {history.originAddress}
                                                    </p>
                                                </div>

                                                <span className="text-lg font-bold text-blue-500">→</span>

                                                <div className="min-w-0 flex-1">
                                                    <p className="mb-1 text-xs text-gray-500">도착지</p>
                                                    <p className="text-sm font-semibold text-gray-900">
                                                        {history.destAddress}
                                                    </p>
                                                </div>
                                            </div>
                                        </div>

                                        {/* 오른쪽: 운송 정보 */}
                                        <div className="grid grid-cols-2 gap-3 text-sm lg:border-l lg:border-gray-200 lg:pl-5">
                                            <div>
                                                <p className="text-xs text-gray-500">화물 무게</p>
                                                <p className="font-semibold text-gray-800">
                                                    {history.cargoWeightKg}kg
                                                </p>
                                            </div>

                                            <div>
                                                <p className="text-xs text-gray-500">요청 차종</p>
                                                <p className="font-semibold text-gray-800">
                                                    차종 {history.vehicleTypeId}
                                                    {' '}({vehicleTypeNames[history.vehicleTypeId] || '정보 없음'})
                                                </p>
                                            </div>

                                            <div>
                                                <p className="text-xs text-gray-500">상차 예정일</p>
                                                <p className="font-semibold text-gray-800">
                                                    {history.pickupAt
                                                        ? new Date(history.pickupAt).toLocaleString('ko-KR')
                                                        : '정보 없음'}
                                                </p>
                                            </div>

                                            <div>
                                                <p className="text-xs text-gray-500">예상 운임</p>
                                                <p className="text-lg font-bold text-blue-600">
                                                    {history.fare?.toLocaleString('ko-KR') ?? '0'}원
                                                </p>
                                            </div>
                                        </div>
                                    </div>

                                    {/* 신청 기록 정보 */}
                                    <p className="mt-4 border-t border-gray-100 pt-3 text-right text-xs text-gray-500">
                                        {history.applicationStatus === 'SELECTED' &&
                                            history.shipmentStatus === 'COMPLETED'
                                            ? '배송 완료 일시'
                                            : '최근 변경 일시'}: {' '}

                                        {(history.applicationStatus === 'SELECTED' &&
                                            history.shipmentStatus === 'COMPLETED'
                                            ? history.completedAt
                                            : history.updatedAt)
                                            ? new Date(
                                                history.applicationStatus === 'SELECTED' &&
                                                    history.shipmentStatus === 'COMPLETED'
                                                    ? history.completedAt
                                                    : history.updatedAt
                                            ).toLocaleString('ko-KR')
                                            : '정보 없음'}
                                    </p>

                                    <p className="mt-2 text-right text-sm font-semibold text-blue-600">
                                        지난 내역 상세 보기 →
                                    </p>
                                </Link>
                            )
                        })
                    )}
                </div>
            )}

        </section>
    )
}

export default DriverMyDispatchPage
