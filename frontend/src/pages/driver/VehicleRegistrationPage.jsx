
import { useEffect, useState } from 'react'
import { applyVehicleRegistration, getVehicleTypes } from '../../api/vehicleApi'

// 기사 차량 등록 신청 페이지
const VehicleRegistrationPage = () => {
  const [vehicleTypes, setVehicleTypes] = useState([])
  const [plateNo, setPlateNo] = useState('')
  const [vehicleTypeId, setVehicleTypeId] = useState('')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

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
      } catch (err) {
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

      <p className="text-sm text-gray-500">
        ※ 차량은 기사 1명당 차종별 1대씩 등록할 수 있습니다.
        관리자 승인 후 정식 차량으로 등록됩니다.
      </p>
    </section>
  )
}

export default VehicleRegistrationPage
