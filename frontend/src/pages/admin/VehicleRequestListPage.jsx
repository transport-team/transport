import { useEffect, useState } from 'react'
import {
    getPendingVehicleRequests,
    approveVehicleRequest,
    rejectVehicleRequest,
} from '../../api/vehicleApi'
import { useOutletContext } from 'react-router-dom'

// 관리자 차량 등록 신청함
const VehicleRequestListPage = () => {
    const { refreshPendingCount } = useOutletContext()
    const [requests, setRequests] = useState([])
    const [loading, setLoading] = useState(true)
    const [processingId, setProcessingId] = useState(null)
    const [rejectId, setRejectId] = useState(null)
    const [rejectionReason, setRejectionReason] = useState('')
    const [message, setMessage] = useState('')
    const [error, setError] = useState('')

    // 승인 대기 신청 목록 조회
    // 상태는 then 안에서 바꾼다 (effect에서 불러도 react-hooks/set-state-in-effect에 걸리지 않게, 오류는 그대로 위로 전달)
    const loadRequests = () =>
        getPendingVehicleRequests().then((data) => setRequests(Array.isArray(data) ? data : []))

    useEffect(() => {
        loadRequests()
            .catch(() => setError('차량 등록 신청 목록을 불러오지 못했습니다.'))
            .finally(() => setLoading(false))
    }, [])

    // 관리자 승인
    const handleApprove = async (requestId) => {
        if (!window.confirm('이 차량 등록 신청을 승인하시겠습니까?')) return

        setProcessingId(requestId)
        setMessage('')
        setError('')

        try {
            await approveVehicleRequest(requestId)
            await loadRequests()
            await refreshPendingCount()
            setMessage('차량 등록 신청을 승인했습니다.')
        } catch (err) {
            setError(err.response?.data?.message || '승인 처리 중 오류가 발생했습니다.')
        } finally {
            setProcessingId(null)
        }
    }

    // 관리자 거절
    const handleReject = async (requestId) => {
        if (!rejectionReason.trim()) {
            setError('거절 사유를 입력해 주세요.')
            return
        }

        setProcessingId(requestId)
        setMessage('')
        setError('')

        try {
            await rejectVehicleRequest(requestId, rejectionReason.trim())
            await loadRequests()
            await refreshPendingCount()
            setRejectId(null)
            setRejectionReason('')
            setMessage('차량 등록 신청을 거절했습니다.')
        } catch (err) {
            setError(err.response?.data?.message || '거절 처리 중 오류가 발생했습니다.')
        } finally {
            setProcessingId(null)
        }
    }

    return (
        <section className="grid gap-5">
            <div>
                <h1 className="text-2xl font-bold text-gray-900">
                    차량 등록 신청함
                </h1>
                <p className="mt-2 text-sm text-gray-600">
                    기사가 신청한 차량 정보를 검토하고 승인하거나 거절할 수 있습니다.
                </p>
            </div>

            {message && (
                <p className="rounded-lg bg-green-50 p-3 text-sm text-green-700">
                    {message}
                </p>
            )}

            {error && (
                <p className="rounded-lg bg-red-50 p-3 text-sm text-red-600">
                    {error}
                </p>
            )}

            {loading ? (
                <p className="text-gray-500">신청 목록을 불러오는 중...</p>
            ) : requests.length === 0 ? (
                <div className="rounded-xl border border-gray-200 bg-white p-8 text-center text-gray-500">
                    승인 대기 중인 차량 등록 신청이 없습니다.
                </div>
            ) : (
                <div className="grid gap-4">
                    {requests.map((request) => (
                        <div
                            key={request.id}
                            className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm"
                        >
                            <div className="mb-4 flex items-center justify-between gap-3">
                                <div>
                                    <h2 className="text-lg font-bold text-gray-900">
                                        차량 등록 신청
                                    </h2>
                                    <p className="mt-1 text-xs text-gray-500">
                                        접수번호 #{request.id}
                                    </p>
                                </div>
                                <span className="rounded-full bg-yellow-100 px-3 py-1 text-xs font-semibold text-yellow-700">
                                    승인 대기
                                </span>
                            </div>

                            <div className="grid grid-cols-2 gap-x-4 gap-y-3 text-sm text-gray-700">
                                <p><strong>기사:</strong> {request.driverName}</p>
                                <p><strong>차량번호:</strong> {request.plateNo}</p>
                                <p><strong>차종:</strong> {request.vehicleTypeName}</p>
                                <p>
                                    <strong>신청일:</strong>{' '}
                                    {request.createdAt
                                        ? new Date(request.createdAt).toLocaleString('ko-KR')
                                        : '-'}
                                </p>
                            </div>

                            <div className="mt-5 flex gap-3">
                                <button
                                    type="button"
                                    onClick={() => handleApprove(request.id)}
                                    disabled={processingId !== null}
                                    className="flex-1 rounded-lg bg-blue-600 px-4 py-2 font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
                                >
                                    승인
                                </button>

                                <button
                                    type="button"
                                    onClick={() => {
                                        setError('')
                                        setRejectId(rejectId === request.id ? null : request.id)
                                        setRejectionReason('')
                                    }}
                                    disabled={processingId !== null}
                                    className="flex-1 rounded-lg border border-red-300 px-4 py-2 font-semibold text-red-600 hover:bg-red-50 disabled:opacity-50"
                                >
                                    거절
                                </button>
                            </div>

                            {rejectId === request.id && (
                                <div className="mt-4 grid gap-3 border-t border-gray-200 pt-4">
                                    <label className="text-sm font-medium text-gray-700">
                                        거절 사유
                                    </label>

                                    <textarea
                                        value={rejectionReason}
                                        onChange={(e) => setRejectionReason(e.target.value)}
                                        maxLength={300}
                                        rows={3}
                                        placeholder="거절 사유를 입력해 주세요."
                                        className="w-full rounded-lg border border-gray-300 p-3 outline-none focus:border-blue-500"
                                    />

                                    <button
                                        type="button"
                                        onClick={() => handleReject(request.id)}
                                        disabled={processingId !== null}
                                        className="rounded-lg bg-red-600 px-4 py-2 font-semibold text-white hover:bg-red-700 disabled:opacity-50"
                                    >
                                        거절 확정
                                    </button>
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            )}
        </section>
    )
}

export default VehicleRequestListPage
