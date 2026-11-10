import { useEffect, useState } from 'react'
import { getAvailableShipments } from '../../api/dispatchApi'
import { Link } from 'react-router-dom'

// 기사: 배차 대기 중인 운송 요청 목록 화면
const DriverShipmentListPage = () => {
  const [shipments, setShipments] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [availabilityFilter, setAvailabilityFilter] = useState('all')
  const [vehicleTypeFilter, setVehicleTypeFilter] = useState('all')
  const [pickupDateFilter, setPickupDateFilter] = useState('')

  useEffect(() => {
    const loadShipments = async () => {
      try {
        const data = await getAvailableShipments()
        setShipments(data)
      } catch (err) {
        console.error('운송 요청 목록 조회 실패:', err)
        setError('운송 요청 목록을 불러오지 못했습니다.')
      } finally {
        setLoading(false)
      }
    }

    loadShipments()
  }, [])

  // 차종 필터 선택지
  const vehicleTypeIds = [1, 2, 3, 4]

  // 차종별 이름
  const vehicleTypeNames = {
    1: '소형·다마스/라보',
    2: '1톤',
    3: '2.5톤',
    4: '5톤',
  }

  // 신청 가능 여부에 따라 운송 요청 필터링
  const filteredShipments = shipments.filter((shipment) => {

    // 상차 예정일 필터
    if (pickupDateFilter &&
      shipment.pickupAt?.slice(0, 10) !== pickupDateFilter) {
      return false
    }
    // 선택한 차종과 일치하는 운송 요청만 표시
    if (vehicleTypeFilter !== 'all' &&
      String(shipment.vehicleTypeId) !== vehicleTypeFilter) {
      return false
    }

    if (availabilityFilter === 'available') {
      return shipment.canApply === true
    }

    if (availabilityFilter === 'unavailable') {
      return shipment.canApply === false
    }

    return true
  })

  return (
    <section className="grid gap-4">
      <h1 className="text-xl font-bold">운송 요청 목록</h1>

      <p className="text-sm text-gray-600">
        현재 지원 가능한 운송 요청을 확인할 수 있습니다.
      </p>


      {/* 운송 요청 필터 */}
      <div className="grid grid-cols-1 gap-4 rounded-2xl bg-white p-5 shadow-sm ring-1 ring-gray-100 md:grid-cols-3">
        {/* 신청 가능 여부 필터 */}
        <div>
          <label className="mb-2 block text-sm font-medium text-gray-600">
            신청 가능 여부
          </label>
          <select
            value={availabilityFilter}
            onChange={(e) => setAvailabilityFilter(e.target.value)}
            className="h-11 w-full rounded-xl border border-gray-200 bg-gray-50 px-3 text-sm text-gray-700 outline-none transition focus:border-blue-400 focus:bg-white focus:ring-2 focus:ring-blue-100"
          >
            <option value="all">전체</option>
            <option value="available">신청 가능</option>
            <option value="unavailable">신청 불가</option>
          </select>
        </div>

        {/* 차종 필터 */}
        <div>
          <label className="mb-2 block text-sm font-medium text-gray-600">
            요청 차종
          </label>
          <select
            value={vehicleTypeFilter}
            onChange={(e) => setVehicleTypeFilter(e.target.value)}
            className="h-11 w-full rounded-xl border border-gray-200 bg-gray-50 px-3 text-sm text-gray-700 outline-none transition focus:border-blue-400 focus:bg-white focus:ring-2 focus:ring-blue-100"
          >
            <option value="all">전체 차종</option>
            {vehicleTypeIds.map((typeId) => (
              <option key={typeId} value={String(typeId)}>
                차종 {typeId} ({vehicleTypeNames[typeId]})
              </option>
            ))}
          </select>
        </div>

        {/* 상차 예정일 필터 */}
        <div>
          <label className="mb-2 block text-sm font-medium text-gray-600">
            상차 예정일
          </label>
          <input
            type="date"
            value={pickupDateFilter}
            onChange={(e) => setPickupDateFilter(e.target.value)}
            className="h-11 w-full min-w-0 rounded-xl border border-gray-200 bg-gray-50 px-3 text-sm text-gray-700 outline-none transition focus:border-blue-400 focus:bg-white focus:ring-2 focus:ring-blue-100"
          />
        </div>
      </div>


      {loading ? (
        <p className="text-gray-500">불러오는 중...</p>
      ) : error ? (
        <p className="text-red-500">{error}</p>
      ) : filteredShipments.length === 0 ? (
        <div className="rounded-xl border bg-white p-6 text-center text-gray-500">
          선택한 조건에 맞는 운송 요청이 없습니다.
        </div>
      ) : (
        <div className="grid gap-4">

          {filteredShipments.map((shipment) => (
            <Link
              key={shipment.shipmentId}
              to={`/driver/shipments/${shipment.shipmentId}`}
              className={`relative block w-full cursor-pointer overflow-hidden rounded-xl border p-5 text-inherit no-underline shadow-sm transition-shadow hover:shadow-md ${shipment.canApply
                ? 'border-gray-200 bg-white'
                : 'border-gray-300 bg-gray-100'
                }`}
            >
              {/* 신청 불가 카드에 회색 반투명 막 표시 */}
              {!shipment.canApply && (
                <div
                  aria-hidden="true"
                  className="pointer-events-none absolute inset-0 z-10 rounded-xl bg-gray-300/35"
                />
              )}

              <div className="grid gap-4 lg:grid-cols-[minmax(0,1.5fr)_minmax(0,1fr)] lg:items-center">

                {/* 왼쪽: 화물명 및 운송 경로 */}
                <div className="min-w-0">
                  <div className="mb-3 flex flex-wrap items-center gap-3">
                    <h2 className="text-lg font-bold text-gray-900">
                      {shipment.cargoName}
                    </h2>

                    <span
                      className={`relative z-20 rounded-full px-3 py-1 text-xs font-semibold ${shipment.canApply
                        ? 'bg-green-100 text-green-700'
                        : 'bg-gray-300 text-gray-700'
                        }`}
                    >
                      {shipment.canApply ? '배차 신청 가능' : '배차 신청 불가'}
                    </span>
                  </div>

                  <div className="flex flex-wrap items-center gap-3 rounded-lg bg-white/70 p-3">
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
                <div className="grid grid-cols-2 gap-3 text-sm lg:border-l lg:border-gray-300 lg:pl-5">
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
                      {new Date(shipment.pickupAt).toLocaleString('ko-KR')}
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

              {/* 배차 신청 불가 사유 */}
              {shipment.blockReasons?.length > 0 && (
                <div className="relative z-20 mt-4 space-y-1">
                  {shipment.blockReasons.map((reason, index) => (
                    <p key={index} className="text-sm font-bold text-red-600">
                      ⛔ {reason}
                    </p>
                  ))}
                </div>
              )}

              {/* 주의사항 */}
              {shipment.warningMessages?.length > 0 && (
                <div className="relative z-20 mt-2 space-y-1">
                  {shipment.warningMessages.map((message, index) => (
                    <p key={index} className="text-sm font-bold text-amber-700">
                      ⚠️ {message}
                    </p>
                  ))}
                </div>
              )}

            </Link>
          ))}

        </div>
      )}
    </section>
  )
}

export default DriverShipmentListPage
