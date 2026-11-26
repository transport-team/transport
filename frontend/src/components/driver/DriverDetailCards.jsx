import { formatKm } from '../../util/formatUtil'

// 고객 배송 상세페이지와 통일한 기사 상세 정보 카드
const formatDateTime = (value) => {
    if (!value) return '정보 없음'
    return String(value).replace('T', ' ').slice(0, 16)
}

export const DriverRouteCard = ({
    originAddress, originDetail, destAddress, destDetail, pickupAt, durationMin, distanceKm,
}) => (
    <div className="rounded-2xl bg-white p-5 shadow-sm">
        <div className="flex items-center justify-between gap-3">
            <p className="text-xs font-semibold text-blue-600">ROUTE & SCHEDULE</p>
            {/* 거리 · 예상 시간 (고객 배송 상세와 같은 모양: "40.5km · 약 76분", 11-26) */}
            {(distanceKm > 0 || durationMin > 0) && (
                <span className="text-xs font-medium text-gray-500">
                    {[distanceKm > 0 ? formatKm(distanceKm) : null, durationMin > 0 ? `약 ${durationMin}분` : null]
                        .filter(Boolean)
                        .join(' · ')}
                </span>
            )}
        </div>

        <div className="mt-4 flex gap-3">
            <div className="flex w-4 shrink-0 flex-col items-center">
                <span className="mt-1 h-2.5 w-2.5 rounded-full bg-blue-600 ring-4 ring-blue-50" />
                <span className="my-1 w-px flex-1 border-l border-dashed border-gray-300" />
                <span className="mb-1 h-2.5 w-2.5 rounded-full bg-red-500 ring-4 ring-red-50" />
            </div>
            <div className="min-w-0 flex-1 space-y-4">
                <div>
                    <p className="text-[11px] font-semibold text-gray-400">출발지</p>
                    <p className="mt-0.5 break-words text-sm font-bold text-gray-900">{originAddress || '정보 없음'}</p>
                    {originDetail && <p className="text-xs text-gray-500">{originDetail}</p>}
                </div>
                <div>
                    <p className="text-[11px] font-semibold text-gray-400">도착지</p>
                    <p className="mt-0.5 break-words text-sm font-bold text-gray-900">{destAddress || '정보 없음'}</p>
                    {destDetail && <p className="text-xs text-gray-500">{destDetail}</p>}
                </div>
            </div>
        </div>

        <div className="mt-4 flex items-center justify-between gap-3 rounded-xl bg-gray-50 px-4 py-3 text-xs">
            <span className="shrink-0 text-gray-500">상차 예정일</span>
            <strong className="text-right text-gray-900">{formatDateTime(pickupAt)}</strong>
        </div>
    </div>
)

export const DriverCargoCard = ({
    cargoName, cargoWeightKg, vehicleTypeLabel, fare, fareLabel = '예상 운임',
}) => (
    <div className="rounded-2xl bg-white p-5 shadow-sm">
        <p className="text-xs font-semibold text-blue-600">CARGO & FARE</p>
        <h2 className="mt-1 break-words text-base font-bold text-gray-900">{cargoName || '화물 정보 없음'}</h2>
        <dl className="mt-4 space-y-2.5 border-t border-gray-100 pt-4 text-sm">
            {vehicleTypeLabel && (
                <div className="flex items-start justify-between gap-3">
                    <dt className="shrink-0 text-gray-500">요청 차종</dt>
                    <dd className="text-right font-semibold text-gray-900">{vehicleTypeLabel}</dd>
                </div>
            )}
            <div className="flex items-start justify-between gap-3">
                <dt className="shrink-0 text-gray-500">화물 무게</dt>
                <dd className="text-right font-semibold text-gray-900">
                    {cargoWeightKg != null ? `${Number(cargoWeightKg).toLocaleString('ko-KR')}kg` : '정보 없음'}
                </dd>
            </div>
            <div className="flex items-center justify-between gap-3 border-t border-dashed border-gray-200 pt-3">
                <dt className="font-bold text-gray-700">{fareLabel}</dt>
                <dd className="text-lg font-black text-blue-700">
                    {fare != null ? `${Number(fare).toLocaleString('ko-KR')}원` : '정보 없음'}
                </dd>
            </div>
        </dl>
    </div>
)

export const DriverMemoCard = ({ memo }) => (
    <div className="rounded-2xl bg-white p-5 shadow-sm">
        <p className="text-xs font-semibold text-blue-600">CUSTOMER REQUEST</p>
        <p className="mt-3 whitespace-pre-wrap break-words rounded-xl bg-gray-50 p-4 text-sm text-gray-700">
            {memo?.trim() || '요청사항 없음'}
        </p>
    </div>
)
