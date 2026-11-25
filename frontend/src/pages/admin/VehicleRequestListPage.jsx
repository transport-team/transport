import { Fragment, useEffect, useMemo, useState } from 'react'
import {
    getAllVehicleRequests,
    approveVehicleRequest,
    rejectVehicleRequest,
} from '../../api/vehicleApi'
import { useOutletContext } from 'react-router-dom'
import Badge from '../../components/common/Badge'
import ConfirmModal from '../../components/common/ConfirmModal'
import useConfirm from '../../hooks/useConfirm'
import useUrlFilters from '../../hooks/useUrlFilters'

// 주소의 필터 값 확인 (?progress=PENDING&result=APPROVED&date=2026-11-25) — 모르는 값은 전체로
const REQUEST_FILTERS = {
  progress: ['PENDING', 'PROCESSED'],
  result: ['APPROVED', 'REJECTED', 'WITHDRAWN'],
  // 있는 날짜만 (2026-13-99 같은 값은 전체로) — 날짜로 바꿨다가 다시 글자로 바꿔 같으면 있는 날짜
  date: (value) => /^\d{4}-\d{2}-\d{2}$/.test(value)
    && !Number.isNaN(Date.parse(value)) && new Date(value).toISOString().slice(0, 10) === value,
}

const formatDateTime = (value) => value ? value.replace('T', ' ').slice(0, 16) : '-'
const STATUS_LABEL = { PENDING: '처리 전', APPROVED: '승인', REJECTED: '거절', WITHDRAWN: '기사 철회' }
// 처리 결과 배지 색 (모양은 공통 Badge)
const RESULT_TONE = { APPROVED: 'green', REJECTED: 'red', WITHDRAWN: 'gray' }

// 관리자 차량 등록 신청함
const VehicleRequestListPage = () => {
    const { refreshPendingCount } = useOutletContext()
    const { confirm, confirmProps } = useConfirm()
    const [requests, setRequests] = useState([])
    const [loading, setLoading] = useState(true)
    const [processingId, setProcessingId] = useState(null)
    const [rejectId, setRejectId] = useState(null)
    const [rejectionReason, setRejectionReason] = useState('')
    const [message, setMessage] = useState('')
    const [error, setError] = useState('')
    const [filters, setFilter, resetFilters] = useUrlFilters(REQUEST_FILTERS)  // 필터는 주소에 남긴다 (새로고침 · 뒤로 가기에도 그대로)

    // 전체 등록 신청 목록 조회 (처리 완료 건도 남겨두고 신청일 최신순 정렬)
    const loadRequests = () =>
        getAllVehicleRequests().then((data) => setRequests(Array.isArray(data) ? data : []))

    useEffect(() => {
        loadRequests()
            .catch(() => setError('차량 등록 신청 목록을 불러오지 못했습니다.'))
            .finally(() => setLoading(false))
    }, [])

    const visibleRequests = useMemo(() => requests.filter((request) => {
        const processed = request.status !== 'PENDING'
        const created = request.createdAt?.slice(0, 10) || ''
        if (filters.progress === 'PENDING' && processed) return false
        if (filters.progress === 'PROCESSED' && !processed) return false
        if (filters.result && request.status !== filters.result) return false
        if (filters.date && created !== filters.date) return false
        return true
    }), [requests, filters])

    // 관리자 승인
    const handleApprove = async (requestId) => {
        const plateNo = requests.find((request) => request.id === requestId)?.plateNo
        if (!(await confirm({
            title: '차량 등록 승인',
            message: `${plateNo ? `${plateNo} ` : '이 '}차량 등록 신청을 승인하시겠습니까?`,
            confirmLabel: '승인',
        }))) return

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
        <section className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                    <h1 className="text-xl font-bold text-gray-900">차량 등록 신청함</h1>
                    <p className="mt-1 text-xs text-gray-500">처리 완료된 신청도 이력으로 유지합니다.</p>
                </div>
                <span className="rounded-lg bg-white px-3 py-1.5 text-xs font-semibold text-gray-600 shadow-sm">
                    조회 <strong className="text-blue-700">{visibleRequests.length}</strong>건
                </span>
            </div>

            {/* 신청일 및 처리 상태 필터 */}
            <div className="grid grid-cols-2 gap-2 rounded-xl border border-gray-200 bg-white p-3 shadow-sm md:grid-cols-4">
                <label className="text-xs text-gray-600">
                    처리 상태
                    <select value={filters.progress} onChange={(e) => setFilter('progress', e.target.value)}
                        className="mt-1 block w-full rounded-lg border border-gray-200 px-2 py-2 text-sm">
                        <option value="">전체</option>
                        <option value="PENDING">처리 전</option>
                        <option value="PROCESSED">처리 완료</option>
                    </select>
                </label>
                <label className="text-xs text-gray-600">
                    처리 결과
                    <select value={filters.result} onChange={(e) => setFilter('result', e.target.value)}
                        className="mt-1 block w-full rounded-lg border border-gray-200 px-2 py-2 text-sm">
                        <option value="">전체</option>
                        <option value="APPROVED">승인</option>
                        <option value="REJECTED">거절</option>
                        <option value="WITHDRAWN">기사 철회</option>
                    </select>
                </label>
                <label className="text-xs text-gray-600">
                    신청일
                    <input type="date" value={filters.date} onChange={(e) => setFilter('date', e.target.value)}
                        className="mt-1 block w-full rounded-lg border border-gray-200 px-2 py-2 text-sm" />
                </label>
                <div className="flex items-end">
                    <button type="button" onClick={resetFilters}
                        className="w-full rounded-lg border border-gray-200 px-3 py-2 text-xs font-semibold text-gray-700 hover:bg-gray-50">
                        필터 초기화
                    </button>
                </div>
            </div>

            {message && <p role="status" className="rounded-lg bg-green-50 p-3 text-sm text-green-700">{message}</p>}
            {error && <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-600">{error}</p>}

            <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
                {loading ? (
                    <p className="p-10 text-center text-sm text-gray-500">신청 목록을 불러오는 중...</p>
                ) : visibleRequests.length === 0 ? (
                    <p className="p-10 text-center text-sm text-gray-500">조건에 맞는 차량 등록 신청이 없습니다.</p>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="w-full min-w-[760px] text-left text-xs">
                            <thead className="border-b border-gray-200 bg-gray-50 text-gray-600">
                                <tr>
                                    <th className="px-4 py-3 font-semibold">신청번호</th>
                                    <th className="px-4 py-3 font-semibold">기사</th>
                                    <th className="px-4 py-3 font-semibold">차량번호 · 차종</th>
                                    <th className="px-4 py-3 font-semibold">신청일</th>
                                    <th className="px-4 py-3 font-semibold">승인/거절일</th>
                                    <th className="px-4 py-3 text-right font-semibold">처리</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                                {visibleRequests.map((request) => {
                                    const processed = request.status !== 'PENDING'
                                    return (
                                        <Fragment key={request.id}>
                                            <tr className={processed ? 'bg-gray-100/90 text-gray-500' : 'bg-white text-gray-900'}>
                                                <td className="px-4 py-3 font-semibold">#{request.id}</td>
                                                <td className="px-4 py-3">{request.driverName}</td>
                                                <td className="px-4 py-3">
                                                    <p className="font-semibold">{request.plateNo}</p>
                                                    <p className="mt-1 text-[11px]">{request.vehicleTypeName}</p>
                                                </td>
                                                <td className="whitespace-nowrap px-4 py-3">{formatDateTime(request.createdAt)}</td>
                                                <td className="whitespace-nowrap px-4 py-3">{formatDateTime(request.reviewedAt)}</td>
                                                <td className="px-4 py-3 text-right">
                                                    {processed ? (
                                                        <span title={request.rejectionReason || undefined}><Badge tone={RESULT_TONE[request.status] ?? 'gray'} size="sm">
                                                            {STATUS_LABEL[request.status] || request.status}
                                                        </Badge></span>
                                                    ) : (
                                                        <div className="flex justify-end gap-1.5">
                                                            <button type="button" disabled={processingId !== null}
                                                                onClick={() => handleApprove(request.id)}
                                                                className="rounded-lg bg-blue-600 px-3 py-1.5 font-semibold text-white hover:bg-blue-700 disabled:opacity-50">
                                                                승인
                                                            </button>
                                                            <button type="button" disabled={processingId !== null}
                                                                onClick={() => { setRejectId(rejectId === request.id ? null : request.id); setRejectionReason(''); setError('') }}
                                                                className="rounded-lg border border-red-200 bg-white px-3 py-1.5 font-semibold text-red-700 hover:bg-red-50 disabled:opacity-50">
                                                                거절
                                                            </button>
                                                        </div>
                                                    )}
                                                </td>
                                            </tr>
                                            {rejectId === request.id && !processed && (
                                                <tr className="bg-red-50/50">
                                                    <td colSpan={6} className="px-4 py-3">
                                                        <label htmlFor={`reject-reason-${request.id}`} className="block text-xs font-semibold text-gray-700">거절 사유</label>
                                                        <textarea id={`reject-reason-${request.id}`} value={rejectionReason}
                                                            onChange={(e) => setRejectionReason(e.target.value)} maxLength={300} rows={2}
                                                            placeholder="거절 사유를 입력해 주세요."
                                                            className="mt-2 w-full rounded-lg border border-gray-200 p-2 text-sm focus:border-red-400 focus:outline-none" />
                                                        <div className="mt-2 flex justify-end gap-2">
                                                            <button type="button" onClick={() => setRejectId(null)} className="rounded-lg px-3 py-1.5 text-xs text-gray-500 hover:bg-gray-100">닫기</button>
                                                            <button type="button" disabled={processingId !== null} onClick={() => handleReject(request.id)}
                                                                className="rounded-lg bg-red-600 px-3 py-1.5 text-xs font-bold text-white hover:bg-red-700 disabled:opacity-50">거절 확정</button>
                                                        </div>
                                                    </td>
                                                </tr>
                                            )}
                                        </Fragment>
                                    )
                                })}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            <ConfirmModal {...confirmProps} />
        </section>
    )
}

export default VehicleRequestListPage
