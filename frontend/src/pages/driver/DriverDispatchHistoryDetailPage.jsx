
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getMyDispatchHistoryDetail } from '../../api/dispatchApi'

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

  // 지난 내역 상태별 이름
  const historyStatusNames = {
    WITHDRAWN: '신청 취소',
    REJECTED: '신청 거절',
    NOT_SELECTED: '미선정',
    COMPLETED: '배송 완료',
    CANCELED: '배송 취소',
  }

  // 상태별 배지 색상
  const historyStatusColors = {
    WITHDRAWN: 'bg-gray-100 text-gray-600',
    REJECTED: 'bg-red-50 text-red-600',
    NOT_SELECTED: 'bg-gray-100 text-gray-600',
    COMPLETED: 'bg-green-100 text-green-700',
    CANCELED: 'bg-gray-100 text-gray-600',
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
    <section className="grid gap-5">

      {/* 지난 내역 탭으로 돌아가기 */}
      <Link
        to="/driver/dispatches?tab=history"
        className="w-fit text-sm text-gray-500 transition hover:text-blue-600"
      >
        ← 지난 내역으로
      </Link>

      {/* 페이지 제목 */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">
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
        <div className="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm sm:p-7">

          {/* 화물명 및 신청 상태 */}
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-xl font-bold text-gray-900">
              {history.cargoName}
            </h2>

            <span
              className={`rounded-full px-3 py-1.5 text-xs font-semibold ${
                historyStatusColors[status] || 'bg-gray-100 text-gray-600'
              }`}
            >
              {historyStatusNames[status] || status}
            </span>
          </div>

          {/* 운송 경로 */}
          <div className="mt-7">
            <h3 className="mb-3 text-sm font-bold text-gray-800">
              운송 경로
            </h3>

            <div className="grid items-center gap-3 rounded-xl bg-gray-50 p-5 md:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)]">
              <div className="min-w-0">
                <p className="mb-2 text-xs text-gray-500">출발지</p>
                <p className="font-semibold text-gray-900">
                  {history.originAddress}
                </p>
              </div>

              <span className="text-center text-xl font-bold text-blue-500">
                <span className="md:hidden">↓</span>
                <span className="hidden md:inline">→</span>
              </span>

              <div className="min-w-0">
                <p className="mb-2 text-xs text-gray-500">도착지</p>
                <p className="font-semibold text-gray-900">
                  {history.destAddress}
                </p>
              </div>
            </div>
          </div>

          {/* 운송 상세 정보 */}
          <div className="mt-7">
            <h3 className="mb-4 text-sm font-bold text-gray-800">
              운송 상세 정보
            </h3>

            <div className="grid grid-cols-2 gap-x-5 gap-y-5 sm:grid-cols-4">
              <div>
                <p className="mb-2 text-xs text-gray-500">화물 무게</p>
                <p className="font-semibold text-gray-900">
                  {history.cargoWeightKg}kg
                </p>
              </div>

              <div>
                <p className="mb-2 text-xs text-gray-500">요청 차종</p>
                <p className="font-semibold text-gray-900">
                  차종 {history.vehicleTypeId}
                  {' '}({vehicleTypeNames[history.vehicleTypeId] || '정보 없음'})
                </p>
              </div>

              <div>
                <p className="mb-2 text-xs text-gray-500">상차 예정일</p>
                <p className="font-semibold text-gray-900">
                  {history.pickupAt
                    ? new Date(history.pickupAt).toLocaleString('ko-KR')
                    : '정보 없음'}
                </p>
              </div>

              <div>
                <p className="mb-2 text-xs text-gray-500">예상 운임</p>
                <p className="text-lg font-bold text-blue-600">
                  {history.fare?.toLocaleString('ko-KR') ?? '정보 없음'}
                  {history.fare != null ? '원' : ''}
                </p>
              </div>
            </div>
          </div>

          {/* 신청 기록 정보 */}
          <div className="mt-7 border-t border-gray-100 pt-5">
            <h3 className="mb-4 text-sm font-bold text-gray-800">
              신청 기록
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
          <div className="mt-7 rounded-xl bg-gray-50 p-4 text-center text-sm text-gray-600">
            종료된 배차 신청 또는 배송 내역입니다.
          </div>
        </div>
      )}
    </section>
  )
}

export default DriverDispatchHistoryDetailPage
