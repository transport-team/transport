import ShipmentProgress from '../../components/tracking/ShipmentProgress'
import ShipmentPhotoGallery from '../../components/tracking/ShipmentPhotoGallery'

import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getMyDispatchHistoryDetail } from '../../api/dispatchApi'
import { DriverRouteCard, DriverCargoCard, DriverMemoCard } from '../../components/driver/DriverDetailCards'
import { shipmentStatusBadge, shipmentStatusLabel } from '../../util/shipmentStatusUtil'

// 기사: 지난 배차 신청 및 배송 내역 상세
const DriverDispatchHistoryDetailPage = () => {
  const { applicationId } = useParams()

  const [history, setHistory] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // 차종별 이름
  const vehicleTypeNames = {
    1: '소형·다마스/라보',
    2: '1톤',
    3: '2.5톤',
    4: '5톤',
  }

  // 신청 상태 이름 · 배지 색 (선정된 신청은 배송 상태라 공통 표 util/shipmentStatusUtil.js를 쓴다)
  const applicationStatusNames = {
    WITHDRAWN: '신청 취소',
    REJECTED: '신청 거절',
    NOT_SELECTED: '미선정',
  }

  const applicationStatusColors = {
    WITHDRAWN: 'border-gray-200 bg-gray-100 text-gray-600',
    REJECTED: 'border-red-200 bg-red-50 text-red-700',
    NOT_SELECTED: 'border-gray-200 bg-gray-100 text-gray-600',
  }

  // 지난 신청 기록 상세 조회
  useEffect(() => {
    let isActive = true

    const loadHistoryDetail = async () => {
      setLoading(true)
      setError('')

      try {
        const data = await getMyDispatchHistoryDetail(applicationId)

        if (isActive) {
          setHistory(data)
        }
      } catch (err) {
        console.error('지난 배차 내역 상세 조회 실패:', err)

        if (isActive) {
          setError('지난 배차 내역을 불러오지 못했습니다.')
        }
      } finally {
        if (isActive) {
          setLoading(false)
        }
      }
    }

    loadHistoryDetail()

    return () => {
      isActive = false
    }
  }, [applicationId])

  // 선택된 기사는 배송 상태로, 그 외에는 신청 상태로 표시
  const status = history?.applicationStatus === 'SELECTED'
    ? history.shipmentStatus
    : history?.applicationStatus

  return (
    <section className="mx-auto min-h-screen w-full max-w-[640px] space-y-4 bg-gray-50 px-5 pb-24 pt-5">

      {/* 지난 내역 탭으로 돌아가기 */}
      <Link
        to="/driver/dispatches?tab=history"
        className="inline-block text-sm font-semibold text-emerald-700 hover:text-emerald-800"
      >
        ← 지난 내역으로
      </Link>

      {/* 페이지 제목 */}
      <div>
        <h1 className="text-lg font-bold text-gray-900">
          지난 배차 내역 상세
        </h1>
        <p className="mt-2 text-sm text-gray-600">
          이전에 신청하거나 진행했던 운송 내역을 확인할 수 있습니다.
        </p>
      </div>

      {loading ? (
        <p className="text-gray-500">불러오는 중...</p>
      ) : error ? (
        <div className="rounded-xl bg-red-50 p-5 text-red-600">
          {error}
        </div>
      ) : !history ? (
        <div className="rounded-xl border border-gray-100 bg-white p-8 text-center text-gray-500">
          지난 배차 내역을 찾을 수 없습니다.
        </div>
      ) : (
        <div className="space-y-4">

          {/* 화물명 및 신청 상태 */}
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-white p-5 shadow-sm">
            <h2 className="text-sm font-bold text-gray-800">
              배송 #{history.shipmentId}
            </h2>

            <span
              className={`rounded-full border px-3 py-1 text-xs font-bold ${applicationStatusColors[status] ?? shipmentStatusBadge(status)}`}
            >
              {applicationStatusNames[status] ?? shipmentStatusLabel(status)}
            </span>
          </div>

          {history.applicationStatus === 'SELECTED' && <ShipmentProgress status={history.shipmentStatus} />}

          {/* 운송 경로 */}
          <DriverRouteCard
            originAddress={history.originAddress}
            destAddress={history.destAddress}
            pickupAt={history.pickupAt}
          />

          {/* 운송 상세 정보 */}
          <DriverCargoCard
            cargoName={history.cargoName}
            cargoWeightKg={history.cargoWeightKg}
            vehicleTypeLabel={`차종 ${history.vehicleTypeId} (${vehicleTypeNames[history.vehicleTypeId] || '정보 없음'})`}
            fare={history.fare}
          />

          {/* 고객 요청사항 */}
          <DriverMemoCard memo={history.memo} />

          {/* 선정된 기사에게만 이전에 등록된 배송 증빙을 조회 전용으로 제공 */}
          {history.applicationStatus === 'SELECTED' && (
            <div className="space-y-2">
              <ShipmentPhotoGallery shipmentId={history.shipmentId} />
              <p className="text-xs text-gray-500">
                지난 운송의 사진은 조회만 가능하며 추가하거나 삭제할 수 없습니다.
              </p>
            </div>
          )}

          {/* 신청 기록 정보 */}
          <div className="rounded-2xl bg-white p-5 shadow-sm">
            <h3 className="mb-4 text-xs font-semibold text-emerald-600">
              APPLICATION HISTORY
            </h3>

            <div className="grid gap-4 text-sm sm:grid-cols-2">
              <div>
                <p className="mb-1 text-xs text-gray-500">신청 일시</p>
                <p className="font-medium text-gray-800">
                  {history.appliedAt
                    ? new Date(history.appliedAt).toLocaleString('ko-KR')
                    : '정보 없음'}
                </p>
              </div>

              <div>
                <p className="mb-1 text-xs text-gray-500">마지막 변경 일시</p>
                <p className="font-medium text-gray-800">
                  {history.updatedAt
                    ? new Date(history.updatedAt).toLocaleString('ko-KR')
                    : '정보 없음'}
                </p>
              </div>
            </div>
          </div>

          {/* 지난 내역은 조회만 가능 */}
          <div className="rounded-2xl bg-gray-100 p-4 text-center text-sm text-gray-600">
            종료된 배차 신청 또는 배송 내역입니다.
          </div>
        </div>
      )}
    </section>
  )
}

export default DriverDispatchHistoryDetailPage
