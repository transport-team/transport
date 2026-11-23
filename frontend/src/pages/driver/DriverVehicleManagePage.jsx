import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getMyVehicles, getVehicleTypes, changeMyVehicleStatus, applyVehicleDeletion, getMyVehicleDeletionRequests, withdrawVehicleDeletionRequest, getMyVehicleRegistrationRequests, withdrawVehicleRegistrationRequest } from '../../api/vehicleApi'

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
    const [deletionRequests, setDeletionRequests] = useState([])
    const [registrationRequests, setRegistrationRequests] = useState([])
    const [deletionError, setDeletionError] = useState('')
    const [registrationError, setRegistrationError] = useState('')
    const [typeError, setTypeError] = useState('')
    const [withdrawingRegistrationId, setWithdrawingRegistrationId] = useState(null)


    const refreshDeletionRequests = async () => {
        try {
            const data = await getMyVehicleDeletionRequests()
            setDeletionRequests(Array.isArray(data) ? data : [])
            setDeletionError('')
        } catch (err) {
            setDeletionError(`삭제 신청 내역 조회 실패 (${err.response?.status || '네트워크 오류'}).`)
        }
    }

    const refreshRegistrationRequests = async () => {
        try {
            const data = await getMyVehicleRegistrationRequests()
            setRegistrationRequests(Array.isArray(data) ? data : [])
            setRegistrationError('')
        } catch (err) {
            setRegistrationError(`등록 신청 내역 조회 실패 (${err.response?.status || '네트워크 오류'}).`)
        }
    }

    const handleWithdrawRegistration = async (request) => {
        if (!window.confirm(`${request.plateNo} 차량 등록 신청을 철회하시겠습니까?`)) return
        setWithdrawingRegistrationId(request.id)
        setStatusError('')
        try {
            await withdrawVehicleRegistrationRequest(request.id)
            setStatusMessage('차량 등록 신청을 철회했습니다.')
            await refreshRegistrationRequests()
        } catch (err) {
            setStatusError(err.response?.data?.message || '등록 신청 철회에 실패했습니다.')
        } finally {
            setWithdrawingRegistrationId(null)
        }
    }

    const handleDeleteRequest = async (vehicle) => {
        if (changingVehicleId !== null) return
        const confirmed = window.confirm(`${vehicle.plateNo} 차량의 삭제를 신청하시겠습니까?\n\n승인 대기 중에는 차량이 정비 중으로 바뀌어 배차 신청에 사용할 수 없습니다.\n관리자가 승인하면 목록에서 숨겨지며, 운송 이력은 유지됩니다.`)
        if (!confirmed) return
        setChangingVehicleId(vehicle.vehicleId)
        setStatusError('')
        setStatusMessage('')
        try {
            await applyVehicleDeletion(vehicle.vehicleId)
            setStatusMessage('차량 삭제 신청이 접수되었습니다. 관리자 심사 전까지 이 차량은 운행할 수 없습니다.')
            await refreshDeletionRequests()
            try {
                const updatedVehicles = await getMyVehicles()
                setVehicles(Array.isArray(updatedVehicles) ? updatedVehicles : [])
            } catch {
                setStatusError('삭제 신청은 접수되었지만 차량 목록을 새로고침하지 못했습니다. 페이지를 다시 열어 주세요.')
            }
        } catch (err) {
            setStatusError(err.response?.data?.message || '차량 삭제 신청에 실패했습니다.')
        } finally { setChangingVehicleId(null) }
    }

    const handleWithdrawDelete = async (request) => {
        if (!window.confirm(`${request.plateNo} 차량 삭제 신청을 철회하시겠습니까?`)) return
        setChangingVehicleId(request.vehicleId)
        setStatusError('')
        try {
            await withdrawVehicleDeletionRequest(request.id)
            setStatusMessage('차량 삭제 신청을 철회했습니다.')
            await refreshDeletionRequests()
            try {
                const updatedVehicles = await getMyVehicles()
                setVehicles(Array.isArray(updatedVehicles) ? updatedVehicles : [])
            } catch {
                setStatusError('삭제 신청은 철회되었지만 차량 목록을 새로고침하지 못했습니다. 페이지를 다시 열어 주세요.')
            }
        } catch (err) {
            setStatusError(err.response?.data?.message || '삭제 신청 철회에 실패했습니다.')
        } finally { setChangingVehicleId(null) }
    }

    // 차량 목록·차종·신청 내역을 독립적으로 조회해 일부 API 오류가 차량 목록 전체를 가리지 않도록 한다.
    useEffect(() => {
        let active = true
        getMyVehicles()
            .then((data) => { if (active) setVehicles(Array.isArray(data) ? data : []) })
            .catch((err) => {
                if (!active) return
                console.error('내 차량 목록 조회 실패:', err)
                setError(`차량 목록 조회 실패 (${err.response?.status || '네트워크 오류'}): ${err.response?.data?.message || err.response?.data?.msg || '서버 연결을 확인해 주세요.'}`)
            })
            .finally(() => { if (active) setLoading(false) })
        getVehicleTypes()
            .then((data) => { if (active) setVehicleTypes(Array.isArray(data) ? data : []) })
            .catch((err) => { if (active) setTypeError(`차종 정보 조회 실패 (${err.response?.status || '네트워크 오류'}). 차종 번호로 표시합니다.`) })
        getMyVehicleDeletionRequests()
            .then((data) => { if (active) setDeletionRequests(Array.isArray(data) ? data : []) })
            .catch((err) => { if (active) setDeletionError(`삭제 신청 내역 조회 실패 (${err.response?.status || '네트워크 오류'}).`) })
        getMyVehicleRegistrationRequests()
            .then((data) => { if (active) setRegistrationRequests(Array.isArray(data) ? data : []) })
            .catch((err) => { if (active) setRegistrationError(`등록 신청 내역 조회 실패 (${err.response?.status || '네트워크 오류'}).`) })
        return () => { active = false }
    }, [])

    const retryVehicleList = async () => {
        setLoading(true)
        setError('')
        try {
            const data = await getMyVehicles()
            setVehicles(Array.isArray(data) ? data : [])
        } catch (err) {
            setError(`차량 목록 조회 실패 (${err.response?.status || '네트워크 오류'}): ${err.response?.data?.message || err.response?.data?.msg || '서버 연결을 확인해 주세요.'}`)
        } finally {
            setLoading(false)
        }
    }

    const combinedRequests = [
        ...registrationRequests.map((request) => ({ ...request, requestType: 'registration' })),
        ...deletionRequests.map((request) => ({ ...request, requestType: 'deletion' })),
    ].sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || '') || (b.id || 0) - (a.id || 0))

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
            vehicle.deletionPending ||
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
                    {loading ? '조회 중' : error ? '조회 실패' : `${vehicles.length}대`}
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

            {typeError && <p role="alert" className="text-xs text-amber-700">{typeError}</p>}

            {/* 차량 목록 */}
            {loading ? (
                <p className="text-gray-500">차량 정보를 불러오는 중...</p>
            ) : error ? (
                <div role="alert" className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-red-50 p-4 text-sm text-red-600">
                    <p>{error}</p>
                    <button type="button" onClick={retryVehicleList} className="rounded-lg border border-red-200 bg-white px-3 py-2 font-semibold hover:bg-red-100">다시 조회</button>
                </div>
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



                                {vehicle.deletionPending && (
                                    <p className="mt-4 rounded-lg bg-amber-50 p-3 text-xs font-semibold text-amber-700">삭제 승인 대기 중 · 상태 변경 및 배차 신청 불가</p>
                                )}

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
                                                vehicle.deletionPending ||
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
                                                vehicle.deletionPending ||
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

                                    <button
                                        type="button"
                                        onClick={() => handleDeleteRequest(vehicle)}
                                        disabled={changingVehicleId !== null || vehicle.deletionPending || vehicle.status === 'INACTIVE'}
                                        className="mt-3 w-full rounded-xl border border-red-200 px-4 py-2.5 text-sm font-semibold text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-40"
                                    >
                                        {vehicle.deletionPending ? '삭제 신청 처리 대기 중' : '차량 삭제 신청'}
                                    </button>

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
            <section className="rounded-xl border border-gray-200 bg-white p-5">
                <h2 className="mb-3 font-bold text-gray-900">차량 등록 · 삭제 신청 내역</h2>
                {registrationError && <p role="alert" className="mb-2 text-xs text-red-600">{registrationError}</p>}
                {deletionError && <p role="alert" className="mb-2 text-xs text-red-600">{deletionError}</p>}
                {combinedRequests.length === 0 ? (
                    <p className="text-sm text-gray-500">조회된 신청 내역이 없습니다.</p>
                ) : (
                    <div className="space-y-2">
                        {combinedRequests.map((request) => (
                            <div key={`${request.requestType}-${request.id}`} className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-gray-100 p-3 text-sm">
                                <div>
                                    <div className="flex flex-wrap items-center gap-2">
                                        <span className={`rounded-md px-2 py-1 text-xs font-bold ${request.requestType === 'registration' ? 'bg-blue-50 text-blue-700' : 'bg-red-50 text-red-700'}`}>
                                            {request.requestType === 'registration' ? '등록 신청' : '삭제 신청'}
                                        </span>
                                        <p className="font-semibold text-gray-900">{request.plateNo} · {request.vehicleTypeName || '차종 정보 없음'}</p>
                                    </div>
                                    <p className="mt-1 text-xs text-gray-500">신청 {request.createdAt?.replace('T', ' ').slice(0, 16) || '-'}</p>
                                    {request.reviewedAt && <p className="text-xs text-gray-500">처리 {request.reviewedAt.replace('T', ' ').slice(0, 16)}</p>}
                                    {request.rejectionReason && <p className="text-xs text-red-600">거절 사유: {request.rejectionReason}</p>}
                                </div>
                                {request.status === 'PENDING' ? (
                                    <button type="button"
                                        onClick={() => request.requestType === 'registration' ? handleWithdrawRegistration(request) : handleWithdrawDelete(request)}
                                        disabled={request.requestType === 'registration' ? withdrawingRegistrationId !== null : changingVehicleId !== null}
                                        className="rounded-lg border border-red-200 px-3 py-2 text-xs font-semibold text-red-600 hover:bg-red-50 disabled:opacity-50">
                                        {withdrawingRegistrationId === request.id && request.requestType === 'registration' ? '처리 중...' : '신청 철회'}
                                    </button>
                                ) : (
                                    <span className={`rounded-lg border px-3 py-1.5 text-xs font-bold ${request.status === 'APPROVED' ? 'border-emerald-200 bg-emerald-50 text-emerald-700' : request.status === 'REJECTED' ? 'border-red-200 bg-red-50 text-red-700' : 'border-gray-200 bg-gray-100 text-gray-600'}`}>
                                        {({ APPROVED: '승인 완료', REJECTED: '거절', WITHDRAWN: '철회 완료' })[request.status] || request.status}
                                    </span>
                                )}
                            </div>
                        ))}
                    </div>
                )}
            </section>
        </section>
    )
}

export default DriverVehicleManagePage
