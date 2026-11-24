
import { useEffect, useState } from 'react'
import { applyVehicleRegistration, getVehicleTypes, getMyVehicleRegistrationRequests, withdrawVehicleRegistrationRequest } from '../../api/vehicleApi'
import ConfirmModal from '../../components/common/ConfirmModal'
import useConfirm from '../../hooks/useConfirm'

// 기사 차량 등록 신청 페이지
const VehicleRegistrationPage = () => {
  const { confirm, confirmProps } = useConfirm()
  const [vehicleTypes, setVehicleTypes] = useState([])
  const [plateNo, setPlateNo] = useState('')
  const [vehicleTypeId, setVehicleTypeId] = useState('')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [myRequests, setMyRequests] = useState([])
  const [historyError, setHistoryError] = useState('')
  const [withdrawingId, setWithdrawingId] = useState(null)

  const refreshRequests = async () => {
    try {
      const requests = await getMyVehicleRegistrationRequests()
      setMyRequests(Array.isArray(requests) ? requests : [])
      setHistoryError('')
    } catch {
      setHistoryError('차량 등록 신청 내역을 불러오지 못했습니다.')
    }
  }

  useEffect(() => { getMyVehicleRegistrationRequests()
    .then((requests) => setMyRequests(Array.isArray(requests) ? requests : []))
    .catch(() => setHistoryError('차량 등록 신청 내역을 불러오지 못했습니다.'))
  }, [])

  const handleWithdraw = async (request) => {
    if (!(await confirm({
      title: '등록 신청 철회',
      message: `${request.plateNo} 차량 등록 신청을 철회하시겠습니까?`,
      confirmLabel: '철회',
      danger: true,
    }))) return
    setWithdrawingId(request.id)
    try {
      await withdrawVehicleRegistrationRequest(request.id)
      await refreshRequests()
    } catch (err) {
      setHistoryError(err.response?.data?.message || '신청 철회에 실패했습니다.')
    } finally { setWithdrawingId(null) }
  }

  // 등록 가능한 차종 목록 조회
  useEffect(() => {
    const fetchVehicleTypes = async () => {
      try {
        const data = await getVehicleTypes()
        setVehicleTypes(
          Array.isArray(data)
            ? data.filter((type) => type.active !== false)
            : []
        )
      } catch {
        setError('차종 목록을 불러오지 못했습니다.')
      }
    }

    fetchVehicleTypes()
  }, [])

  // 차량 등록 신청
  const handleSubmit = async (e) => {
    e.preventDefault()
    setMessage('')
    setError('')

    if (!plateNo.trim() || !vehicleTypeId) {
      setError('차량번호와 차종을 모두 입력해 주세요.')
      return
    }

    try {
      setLoading(true)

      const requestId = await applyVehicleRegistration({
        plateNo: plateNo.trim(),
        vehicleTypeId: Number(vehicleTypeId),
      })

      setMessage(`차량 등록 신청이 완료되었습니다. (신청 번호: ${requestId})`)
      setPlateNo('')
      setVehicleTypeId('')
      await refreshRequests()
    } catch (err) {
      setError(
        err.response?.data?.message ||
        '차량 등록 신청 중 오류가 발생했습니다.'
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="grid gap-5">
      <div>
        <h1 className="text-xl font-bold text-gray-900">차량 등록 신청</h1>
        <p className="mt-2 text-sm text-gray-600">
          차량 정보를 입력하면 관리자 검토 후 등록됩니다.
        </p>
      </div>

      <form
        onSubmit={handleSubmit}
        className="grid gap-4 rounded-xl border border-gray-200 bg-white p-5"
      >
        <div>
          <label className="mb-2 block text-sm font-medium">
            차량번호
          </label>
          <input
            type="text"
            value={plateNo}
            onChange={(e) => setPlateNo(e.target.value)}
            placeholder="예: 12가3456"
            maxLength={20}
            className="w-full rounded-lg border border-gray-300 px-3 py-3 outline-none focus:border-blue-500"
          />
        </div>

        <div>
          <label className="mb-2 block text-sm font-medium">
            차종
          </label>
          <select
            value={vehicleTypeId}
            onChange={(e) => setVehicleTypeId(e.target.value)}
            className="w-full rounded-lg border border-gray-300 bg-white px-3 py-3 outline-none focus:border-blue-500"
          >
            <option value="">차종을 선택해 주세요</option>
            {vehicleTypes.map((type) => (
              <option key={type.id} value={type.id}>
                {type.name}
              </option>
            ))}
          </select>
        </div>

        {error && (
          <p className="text-sm text-red-600">{error}</p>
        )}

        {message && (
          <p className="text-sm text-green-600">{message}</p>
        )}

        <button
          type="submit"
          disabled={loading}
          className="rounded-lg bg-blue-600 py-3 font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {loading ? '신청 중...' : '차량 등록 신청'}
        </button>
      </form>

      <section className="rounded-xl border border-gray-200 bg-white p-5">
        <h2 className="mb-4 font-bold text-gray-900">내 차량 등록 신청 내역</h2>
        {historyError && <p role="alert" className="mb-3 text-sm text-red-600">{historyError}</p>}
        {myRequests.length === 0 ? <p className="text-sm text-gray-500">등록 신청 내역이 없습니다.</p> : (
          <div className="space-y-2">
            {myRequests.map((request) => (
              <div key={request.id} className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-gray-100 p-3 text-sm">
                <div>
                  <p className="font-semibold text-gray-900">{request.plateNo} · {request.vehicleTypeName}</p>
                  <p className="mt-1 text-xs text-gray-500">신청 {request.createdAt?.replace('T', ' ').slice(0, 16) || '-'}</p>
                  {request.reviewedAt && <p className="mt-1 text-xs text-gray-500">처리 {request.reviewedAt.replace('T', ' ').slice(0, 16)}</p>}
                  {request.rejectionReason && <p className="mt-1 text-xs text-red-600">거절 사유: {request.rejectionReason}</p>}
                </div>
                {request.status === 'PENDING' ? (
                  <button type="button" disabled={withdrawingId !== null} onClick={() => handleWithdraw(request)} className="rounded-lg border border-red-200 px-3 py-2 text-xs font-semibold text-red-600 hover:bg-red-50 disabled:opacity-50">{withdrawingId === request.id ? '처리 중...' : '신청 철회'}</button>
                ) : (
                  <span className={`rounded-lg px-3 py-1.5 text-xs font-bold ${request.status === 'APPROVED' ? 'bg-emerald-50 text-emerald-700' : 'bg-gray-100 text-gray-600'}`}>
                    {({ APPROVED: '승인', REJECTED: '거절', WITHDRAWN: '철회 완료' })[request.status] || request.status}
                  </span>
                )}
              </div>
            ))}
          </div>
        )}
      </section>

      <p className="text-sm text-gray-500">
        ※ 차량은 기사 1명당 차종별 1대씩 등록할 수 있습니다.
        관리자 승인 후 정식 차량으로 등록됩니다.
      </p>

      <ConfirmModal {...confirmProps} />
    </section>
  )
}

export default VehicleRegistrationPage
