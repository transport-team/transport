import { useEffect, useState } from 'react'
import { getAvailableShipments } from '../../api/dispatchApi'

// 기사: 배차 대기 중인 운송 요청 목록 화면
const DriverShipmentListPage = () => {
  const [shipments, setShipments] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

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

  return (
    <section className="grid gap-4">
      <h1 className="text-xl font-bold">운송 요청 목록</h1>

      <p className="text-sm text-gray-600">
        현재 지원 가능한 운송 요청을 확인할 수 있습니다.
      </p>

      {loading ? (
        <p className="text-gray-500">불러오는 중...</p>
      ) : error ? (
        <p className="text-red-500">{error}</p>
      ) : shipments.length === 0 ? (
        <div className="rounded-xl border bg-white p-6 text-center text-gray-500">
          현재 지원 가능한 운송 요청이 없습니다.
        </div>
      ) : (
        <div className="grid gap-4">
          {shipments.map((shipment) => (
            <div
              key={shipment.shipmentId}
              className="rounded-xl border bg-white p-5"
            >
              <h2 className="mb-3 font-bold">
                운송 요청 #{shipment.shipmentId}
              </h2>

              <div className="grid gap-2 text-sm text-gray-700">
                <p>출발지: {shipment.originAddress}</p>
                <p>도착지: {shipment.destAddress}</p>
                <p>화물명: {shipment.cargoName}</p>
                <p>화물 무게: {shipment.cargoWeightKg}kg</p>
                <p>요청 차종 ID: {shipment.vehicleTypeId}</p>
                <p>
                  상차 예정일: {new Date(shipment.pickupAt).toLocaleString('ko-KR')}
                </p>
                <p className="font-bold text-blue-600">
                  운임: {shipment.fare.toLocaleString('ko-KR')}원
                </p>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

export default DriverShipmentListPage
