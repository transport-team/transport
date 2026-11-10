import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getMyVehicles, getVehicleTypes, changeMyVehicleStatus } from '../../api/vehicleApi'

// 기사: 내 차량 관리 페이지
const DriverVehicleManagePage = () => {
    const [vehicles, setVehicles] = useState([])
    const [vehicleTypes, setVehicleTypes] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')
    // 차량 상태 변경 처리
    const [changingVehicleId, setChangingVehicleId] = useState(null)
    const [statusMessage, setStatusMessage] = useState('')
    const [statusError, setStatusError] = useState('')

    // 기사 본인 차량 및 차종 목록 조회
    useEffect(() => {
        const loadVehicles = async () => {
            try {
                const [vehicleData, typeData] = await Promise.all([
                    getMyVehicles(),
                    getVehicleTypes(),
                ])

                setVehicles(Array.isArray(vehicleData) ? vehicleData : [])
                setVehicleTypes(Array.isArray(typeData) ? typeData : [])
            } catch (err) {
                console.error('내 차량 목록 조회 실패:', err)
                setError('차량 정보를 불러오지 못했습니다.')
            } finally {
                setLoading(false)
            }
        }

        loadVehicles()
    }, [])

    // 차종 ID로 차종 이름 표시
    const getVehicleTypeName = (vehicleTypeId) => {
        const type = vehicleTypes.find(
            (item) => item.id === vehicleTypeId
        )

        return type?.name || `차종 ${vehicleTypeId}`
    }

    // 차량 상태별 표시 정보
    const getStatusInfo = (status) => {
        if (status === 'AVAILABLE') {
            return {
                label: '운행 가능',
                className: 'bg-green-100 text-green-700',
            }
        }

        if (status === 'MAINTENANCE') {
            return {
                label: '정비 중',
                className: 'bg-amber-100 text-amber-700',
            }
        }

        return {
            label: status || '상태 확인 필요',
            className: 'bg-gray-100 text-gray-600',
        }
    }

    // 기사 본인 차량 상태 변경
    const handleChangeStatus = async (vehicle, nextStatus) => {
        if (
            changingVehicleId !== null ||
            vehicle.status === nextStatus ||
            !['AVAILABLE', 'MAINTENANCE'].includes(vehicle.status) ||
            !['AVAILABLE', 'MAINTENANCE'].includes(nextStatus)
        ) {
            return
        }

        // 상태 변경 전 사용자에게 한 번 더 확인
        const nextStatusName =
            nextStatus === 'AVAILABLE' ? '운행 가능' : '정비 중'

        const confirmed = window.confirm(
            nextStatus === 'MAINTENANCE'
                ? `${vehicle.plateNo} 차량을 '정비 중' 상태로 변경하시겠습니까?\n\n⚠️ 정비 중으로 변경하면 해당 차량으로 배차 신청을 할 수 없습니다.`
                : `${vehicle.plateNo} 차량을 '운행 가능' 상태로 변경하시겠습니까?`
        )

        if (!confirmed) {
            return
        }

        setChangingVehicleId(vehicle.vehicleId)
        setStatusMessage('')
        setStatusError('')

        try {
            const updatedVehicle = await changeMyVehicleStatus(
                vehicle.vehicleId,
                nextStatus
            )

            // 변경 성공 후 선택된 버튼과 차량 상태 갱신
            setVehicles((prev) =>
                prev.map((item) =>
                    item.vehicleId === updatedVehicle.vehicleId
                        ? updatedVehicle
                        : item
                )
            )

            setStatusMessage(
                `${updatedVehicle.plateNo} 차량이 ${nextStatusName} 상태로 변경되었습니다.`
            )
        } catch (err) {
            console.error('차량 상태 변경 실패:', err)

            setStatusError(
                err.response?.data?.message ||
                err.response?.data?.msg ||
                '차량 상태 변경 중 오류가 발생했습니다.'
            )
        } finally {
            setChangingVehicleId(null)
        }
    }

    return (
        <section className="grid gap-6">
            {/* 페이지 제목 */}
            <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900">
                        내 차량 관리
                    </h1>
                    <p className="mt-2 text-sm text-gray-500">
                        등록된 차량과 현재 운행 상태를 확인할 수 있습니다.
                    </p>
                </div>

                <Link
                    to="/driver/vehicles/register"
                    className="rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-blue-700"
                >
                    + 차량 등록 신청
                </Link>
            </div>

            {/* 등록 차량 수 */}
            <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
                <p className="text-sm text-gray-500">내 등록 차량</p>
                <p className="mt-1 text-2xl font-bold text-gray-900">
                    {vehicles.length}대
                </p>
            </div>

            {/* 차량 상태 변경 결과 */}
            {statusMessage && (
                <p className="rounded-xl bg-green-50 p-4 text-sm font-medium text-green-700">
                    {statusMessage}
                </p>
            )}

            {statusError && (
                <p className="rounded-xl bg-red-50 p-4 text-sm font-medium text-red-600">
                    {statusError}
                </p>
            )}

            {/* 차량 목록 */}
            {loading ? (
                <p className="text-gray-500">차량 정보를 불러오는 중...</p>
            ) : error ? (
                <p className="text-sm text-red-600">{error}</p>
            ) : vehicles.length === 0 ? (
                <div className="rounded-xl border border-gray-200 bg-white p-8 text-center text-gray-500">
                    등록된 차량이 없습니다.
                </div>
            ) : (
                <div className="grid gap-4 md:grid-cols-2">
                    {vehicles.map((vehicle) => {
                        const statusInfo = getStatusInfo(vehicle.status)

                        return (
                            <div
                                key={vehicle.vehicleId}
                                className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm"
                            >
                                {/* 차량번호 및 상태 */}
                                <div className="flex items-start justify-between gap-3">
                                    <div>
                                        <p className="mb-1 text-xs text-gray-500">
                                            등록 차량
                                        </p>
                                        <h2 className="text-xl font-bold text-gray-900">
                                            {vehicle.plateNo}
                                        </h2>
                                    </div>

                                    <span
                                        className={`rounded-full px-3 py-1.5 text-xs font-semibold ${statusInfo.className}`}
                                    >
                                        {statusInfo.label}
                                    </span>
                                </div>

                                {/* 차량 정보 */}
                                <div className="mt-5 rounded-xl bg-gray-50 p-4">
                                    <p className="mb-1 text-xs text-gray-500">
                                        차종
                                    </p>
                                    <p className="font-semibold text-gray-900">
                                        {getVehicleTypeName(vehicle.vehicleTypeId)}
                                    </p>
                                </div>



                                {/* 운행 상태 변경 버튼 */}
                                <div className="mt-5 border-t border-gray-100 pt-4">
                                    <p className="mb-3 text-sm font-semibold text-gray-700">
                                        운행 상태
                                    </p>

                                    <div className="grid grid-cols-2 gap-3">
                                        {/* 운행 가능 버튼 */}
                                        <button
                                            type="button"
                                            onClick={() => handleChangeStatus(vehicle, 'AVAILABLE')}
                                            disabled={
                                                changingVehicleId !== null ||
                                                vehicle.status === 'INACTIVE' ||
                                                vehicle.status === 'AVAILABLE'
                                            }
                                            className={`rounded-xl border px-4 py-3 text-sm font-semibold transition ${vehicle.status === 'AVAILABLE'
                                                ? 'border-blue-600 bg-blue-600 text-white'
                                                : 'border-gray-200 bg-white text-gray-700 hover:bg-gray-50'
                                                } disabled:cursor-not-allowed`}
                                        >
                                            운행 가능
                                        </button>

                                        {/* 정비 중 버튼 */}
                                        <button
                                            type="button"
                                            onClick={() => handleChangeStatus(vehicle, 'MAINTENANCE')}
                                            disabled={
                                                changingVehicleId !== null ||
                                                vehicle.status === 'INACTIVE' ||
                                                vehicle.status === 'MAINTENANCE'
                                            }
                                            className={`rounded-xl border px-4 py-3 text-sm font-semibold transition ${vehicle.status === 'MAINTENANCE'
                                                ? 'border-blue-600 bg-blue-600 text-white'
                                                : 'border-gray-200 bg-white text-gray-700 hover:bg-gray-50'
                                                } disabled:cursor-not-allowed`}
                                        >
                                            정비 중
                                        </button>
                                    </div>

                                    {changingVehicleId === vehicle.vehicleId && (
                                        <p className="mt-2 text-center text-xs text-gray-500">
                                            상태 변경 중...
                                        </p>
                                    )}
                                </div>


                            </div>
                        )
                    })}
                </div>
            )}
        </section>
    )
}

export default DriverVehicleManagePage
