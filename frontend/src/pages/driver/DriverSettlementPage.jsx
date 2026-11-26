import { useEffect, useState } from 'react'
import {
    getDriverMonthlySettlement,
    getDriverMyReviews,
    getDriverSettlementRange,
    getDriverSettlementRangeItems,
} from '../../api/dispatchApi'
import Badge from '../../components/common/Badge'
import { formatDateTime } from '../../util/formatUtil'

// 리뷰 카드에 처음 보여 줄 개수 (나머지는 '모두 보기')
const REVIEW_PREVIEW_COUNT = 3

// 월 정산 상태 → 배지 · 설명 (서버 settlementStatus: OPEN · PENDING · PAID · NONE)
const getPayoutInfo = (item) => {
    // 지난달까지인데 마감 전이고 완료 0건 = 정산할 것이 없는 달 (자동 마감은 바로 전달만 한다, 11-26 점검)
    if (item.settlementStatus === 'OPEN' && item.completedCount === 0 && item.month < getCurrentMonth()) {
        return { label: '정산 없음', tone: 'gray', detail: '완료한 배송이 없어 정산할 금액이 없어요' }
    }
    switch (item.settlementStatus) {
        case 'PAID':
            return {
                label: '지급 완료',
                tone: 'green',
                detail: `${item.paidAt ? item.paidAt.slice(5, 10) : ''} 지급 · 수수료 ${item.commissionRate}%`,
            }
        case 'PENDING':
            return { label: '지급 대기', tone: 'amber', detail: `수수료 ${item.commissionRate}%를 뺀 지급 예정액` }
        case 'NONE':
            return { label: '정산 없음', tone: 'gray', detail: '완료한 배송이 없어 정산하지 않았어요' }
        default:
            return { label: '마감 전', tone: 'gray', detail: '관리자가 월 마감을 하면 지급액이 나와요' }
    }
}

// 현재 월을 YYYY-MM 형식으로 반환
const getCurrentMonth = () => {
    const now = new Date()
    const year = now.getFullYear()
    const month = String(now.getMonth() + 1).padStart(2, '0')

    return `${year}-${month}`
}

// 오늘 날짜를 YYYY-MM-DD 형식으로 반환
const getTodayDate = () => {
    const now = new Date()

    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

// 선택한 월의 말일 반환 (이번 달은 오늘까지)
const getMonthEndDate = (month) => {
    const [year, monthNumber] = month.split('-').map(Number)
    const lastDay = new Date(year, monthNumber, 0).getDate()
    const lastDate = `${month}-${String(lastDay).padStart(2, '0')}`

    return lastDate > getTodayDate() ? getTodayDate() : lastDate
}

// 선택한 월을 포함한 최근 6개월
const getRecentMonths = (month) => {
    const [year, monthNumber] = month.split('-').map(Number)

    return Array.from({ length: 6 }, (_, index) => {
        const date = new Date(year, monthNumber - 6 + index, 1)

        return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
    })
}

const DriverSettlementPage = () => {
    const [startDate, setStartDate] = useState(() => `${getCurrentMonth()}-01`)
    const [endDate, setEndDate] = useState(getTodayDate)
    const [trendEndMonth, setTrendEndMonth] = useState(getCurrentMonth)
    const [settlement, setSettlement] = useState(null)
    const [fareItems, setFareItems] = useState([])
    const [monthlyTrend, setMonthlyTrend] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')
    const [tooltipMonth, setTooltipMonth] = useState(null)

    // 내 평균 평점 · 리뷰 (DRV-07) — 정산과 따로 불러와서 한쪽이 실패해도 다른 쪽은 보인다
    const [reviewSummary, setReviewSummary] = useState(null)
    const [reviewError, setReviewError] = useState('')
    const [showAllReviews, setShowAllReviews] = useState(false)

    useEffect(() => {
        let isActive = true

        getDriverMyReviews()
            .then((data) => {
                if (!isActive) return
                setReviewSummary(data)
                setReviewError('')
            })
            .catch((err) => {
                if (!isActive) return
                console.error('평점 · 리뷰 조회 실패:', err)
                setReviewError('평점 · 리뷰를 불러오지 못했습니다.')
            })

        return () => {
            isActive = false
        }
    }, [])

    // 선택한 월의 정산 요약 및 상세 내역 조회
    useEffect(() => {
        let isActive = true

        Promise.all([
            getDriverSettlementRange(startDate, endDate),
            getDriverSettlementRangeItems(startDate, endDate),
        ])
            .then(([summary, items]) => {
                if (!isActive) return

                setSettlement(summary)
                setFareItems(Array.isArray(items) ? items : [])
                setError('')
            })
            .catch((err) => {
                if (!isActive) return
                console.error('정산 내역 조회 실패:', err)
                setSettlement(null)
                setFareItems([])
                setError('정산 내역을 불러오지 못했습니다.')
            })
            .finally(() => {
                if (isActive) setLoading(false)
            })

        return () => {
            isActive = false
        }
    }, [startDate, endDate])

    // 최근 6개월 운임 추이 조회
    useEffect(() => {
        let isActive = true
        const months = getRecentMonths(trendEndMonth)

        Promise.all(
            months.map((value) => getDriverMonthlySettlement(value))
        )
            .then((summaries) => {
                if (!isActive) return

                setMonthlyTrend(
                    months.map((value, index) => ({
                        month: value,
                        totalFare: Number(summaries[index]?.totalFare ?? 0),
                        completedCount: Number(summaries[index]?.completedCount ?? 0),
                        // 그 달 정산 상태 · 지급액 (관리자 월 마감 뒤)
                        settlementStatus: summaries[index]?.settlementStatus ?? 'OPEN',
                        commissionRate: summaries[index]?.commissionRate ?? null,
                        payoutAmount: summaries[index]?.payoutAmount ?? null,
                        paidAt: summaries[index]?.paidAt ?? null,
                    }))
                )
            })
            .catch((err) => {
                if (!isActive) return
                console.error('월별 운임 추이 조회 실패:', err)
            })

        return () => {
            isActive = false
        }
    }, [trendEndMonth])

    // 그래프에서 선택한 월의 정산 기간으로 변경
    const selectMonth = (nextMonth) => {
        const nextStart = `${nextMonth}-01`
        const nextEnd = getMonthEndDate(nextMonth)

        if (startDate === nextStart && endDate === nextEnd) return

        setStartDate(nextStart)
        setEndDate(nextEnd)
        setError('')
        setLoading(true)
    }

    // 시작일 변경
    const handleStartDateChange = (event) => {
        const value = event.target.value
        if (!value) return

        setStartDate(value)
        if (value > endDate) setEndDate(value)
        setError('')
        setLoading(true)
    }

    // 종료일 변경
    const handleEndDateChange = (event) => {
        const value = event.target.value
        if (!value) return

        setEndDate(value)
        if (value < startDate) setStartDate(value)
        setError('')
        setLoading(true)
    }

    // 그래프 표시 기간을 6개월씩 이동
    const moveTrendWindow = (amount) => {
        const [year, monthNumber] = trendEndMonth.split('-').map(Number)
        const date = new Date(year, monthNumber - 1 + amount, 1)
        const nextMonth = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`

        if (nextMonth > getCurrentMonth()) return

        setTrendEndMonth(nextMonth)
    }

    // 조회 기간이 한 달 안에 있으면 그래프에서 해당 월 강조
    const selectedMonth = startDate.slice(0, 7) === endDate.slice(0, 7)
        ? startDate.slice(0, 7)
        : ''

    // 월별 운임 꺾은선 그래프 좌표
    const peakFare = Math.max(0, ...monthlyTrend.map((item) => item.totalFare))

    const chartLeft = 76
    const chartRight = 596
    const chartTop = 45
    const chartBottom = 165

    const chartPoints = monthlyTrend.map((item, index) => ({
        ...item,
        x: chartLeft + (chartRight - chartLeft) / Math.max(monthlyTrend.length - 1, 1) * index,
        y: chartBottom - (item.totalFare / Math.max(peakFare, 1)) * (chartBottom - chartTop),
    }))

    const yTicks = [
        peakFare,
        Math.round(peakFare / 2),
        0,
    ]

    return (
        <div className="mx-auto w-full max-w-5xl px-4 py-8 sm:px-6">
            {/* 페이지 제목 */}
            <div className="mb-8">
                <h1 className="text-2xl font-bold text-gray-900">
                    월별 정산 내역
                </h1>
                <p className="mt-2 text-sm text-gray-500">
                    배송 완료 건수 · 운임 합계와 받은 평점 · 리뷰를 확인할 수 있습니다.
                </p>
            </div>

            {/* 평균 평점 · 리뷰 (DRV-07) */}
            <div className="mb-5 rounded-2xl border border-gray-200 bg-white p-5 shadow-sm sm:p-6">
                <div className="flex flex-wrap items-end justify-between gap-3">
                    <div>
                        <h2 className="text-lg font-bold text-gray-900">평점 · 리뷰</h2>
                        <p className="mt-1 text-sm text-gray-500">고객이 배송 완료 뒤 남긴 리뷰예요. 고객 이름은 가려서 보여요.</p>
                    </div>
                    {reviewSummary && (
                        <p className="text-right">
                            <span className="text-2xl font-bold text-amber-500" aria-hidden="true">★</span>
                            <span className="ml-1 text-2xl font-bold text-gray-900">{Number(reviewSummary.averageRating ?? 0).toFixed(1)}</span>
                            <span className="ml-2 text-sm text-gray-500">리뷰 {reviewSummary.reviewCount}개</span>
                        </p>
                    )}
                </div>

                {reviewError && (
                    <p role="alert" className="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{reviewError}</p>
                )}

                {!reviewError && !reviewSummary && (
                    <p className="mt-4 text-sm text-gray-500">평점 · 리뷰를 불러오는 중...</p>
                )}

                {reviewSummary?.reviewCount === 0 && (
                    <p className="mt-4 rounded-xl bg-gray-50 p-4 text-center text-sm text-gray-500">아직 받은 리뷰가 없어요.</p>
                )}

                {reviewSummary?.reviewCount > 0 && (
                    <>
                        <ul className="mt-4 divide-y divide-gray-100">
                            {(showAllReviews ? reviewSummary.reviews : reviewSummary.reviews.slice(0, REVIEW_PREVIEW_COUNT)).map((review) => (
                                <li key={review.id} className="py-3">
                                    <div className="flex flex-wrap items-center justify-between gap-2">
                                        <span className="text-sm font-semibold text-amber-500" aria-label={`별점 ${review.rating}점`}>
                                            {'★'.repeat(review.rating)}
                                            <span className="text-gray-300">{'★'.repeat(Math.max(0, 5 - review.rating))}</span>
                                        </span>
                                        <span className="text-xs text-gray-400">
                                            {review.customerName} · {review.createdAt?.slice(0, 10)}
                                        </span>
                                    </div>
                                    {review.content && <p className="mt-1 text-sm text-gray-700">{review.content}</p>}
                                </li>
                            ))}
                        </ul>
                        {reviewSummary.reviewCount > REVIEW_PREVIEW_COUNT && (
                            <button
                                type="button"
                                onClick={() => setShowAllReviews((open) => !open)}
                                className="mt-2 text-sm font-semibold text-blue-600 hover:text-blue-700"
                            >
                                {showAllReviews ? '접기' : `리뷰 ${reviewSummary.reviewCount}개 모두 보기`}
                            </button>
                        )}
                    </>
                )}
            </div>

            {/* 조회 오류 */}
            {error && (
                <p
                    role="alert"
                    className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm font-medium text-red-700"
                >
                    {error}
                </p>
            )}

            {/* 로딩 */}
            {loading && !settlement && (
                <p className="rounded-2xl border border-gray-200 bg-white p-8 text-center text-sm text-gray-500">
                    정산 내역을 불러오는 중입니다...
                </p>
            )}

            {/* 월별 정산 요약 */}
            {!error && settlement && (
                <div className="space-y-5">
                    <div className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm sm:p-6">

                        <div className="mb-5">
                            <h2 className="text-lg font-bold text-gray-900">
                                기간별 정산 요약
                            </h2>
                            <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
                                <label className="block text-sm font-semibold text-gray-700">
                                    시작일
                                    <input
                                        type="date"
                                        value={startDate}
                                        max={getTodayDate()}
                                        onChange={handleStartDateChange}
                                        className="mt-2 block w-full min-w-0 rounded-xl border border-gray-200 bg-white px-4 py-3 text-sm focus:border-blue-500 focus:outline-none"
                                    />
                                </label>

                                <label className="block text-sm font-semibold text-gray-700">
                                    종료일
                                    <input
                                        type="date"
                                        value={endDate}
                                        max={getTodayDate()}
                                        onChange={handleEndDateChange}
                                        className="mt-2 block w-full min-w-0 rounded-xl border border-gray-200 bg-white px-4 py-3 text-sm focus:border-blue-500 focus:outline-none"
                                    />
                                </label>
                            </div>
                        </div>

                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                            {/* 배송 완료 건수 */}
                            <div className="rounded-xl bg-blue-50 p-5">
                                <p className="text-sm font-medium text-blue-700">
                                    배송 완료 건수
                                </p>
                                <p className="mt-3 text-3xl font-bold text-gray-900">
                                    {loading ? '...' : Number(settlement.completedCount ?? 0).toLocaleString('ko-KR')}
                                    <span className="ml-1 text-base font-medium">건</span>
                                </p>
                            </div>

                            {/* 운임 합계 */}
                            <div className="rounded-xl bg-green-50 p-5">
                                <p className="text-sm font-medium text-green-700">
                                    총 운임 합계
                                </p>
                                <p className="mt-3 text-3xl font-bold text-gray-900">
                                    {loading ? '...' : Number(settlement.totalFare ?? 0).toLocaleString('ko-KR')}
                                    <span className="ml-1 text-base font-medium">원</span>
                                </p>
                            </div>
                        </div>
                    </div>

                    {/* 최근 6개월 운임 추이 */}
                    <div className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm sm:p-6">

                        <div className="flex flex-wrap items-center justify-between gap-3">
                            <div>
                                <h2 className="text-lg font-bold text-gray-900">
                                    6개월 운임 추이
                                </h2>
                                <p className="mt-1 text-sm text-gray-500">
                                    {getRecentMonths(trendEndMonth)[0]} ~ {trendEndMonth}
                                </p>
                            </div>

                            <div className="flex items-center gap-2">
                                <button
                                    type="button"
                                    onClick={() => moveTrendWindow(-1)}
                                    aria-label="이전 1개월 보기"
                                    className="rounded-lg border border-gray-200 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
                                >
                                    ◀
                                </button>
                                <button
                                    type="button"
                                    onClick={() => moveTrendWindow(1)}
                                    disabled={trendEndMonth >= getCurrentMonth()}
                                    aria-label="다음 1개월 보기"
                                    className="rounded-lg border border-gray-200 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-30"
                                >
                                    ▶
                                </button>
                            </div>
                        </div>


                        <svg
                            className="mt-4 w-full"
                            viewBox="0 0 640 220"
                            role="group"
                            aria-label="최근 6개월 월별 운임 꺾은선 그래프"
                        >
                            {/* 가로 점선 */}
                            {[45, 105, 165].map((y) => (
                                <line
                                    key={y}
                                    x1={chartLeft}
                                    y1={y}
                                    x2={chartRight}
                                    y2={y}
                                    stroke="#E5E7EB"
                                    strokeDasharray="5 5"
                                />
                            ))}

                            {/* 왼쪽 금액 눈금 */}
                            {yTicks.map((value, index) => {
                                const y = [45, 105, 165][index]

                                return (
                                    <text
                                        key={index}
                                        x={chartLeft - 10}
                                        y={y + 5}
                                        textAnchor="end"
                                        fontSize="13"
                                        fill="#6B7280"
                                    >
                                        {value.toLocaleString('ko-KR')}원
                                    </text>
                                )
                            })}

                            {/* 선 */}
                            <polyline
                                points={chartPoints.map((point) => `${point.x},${point.y}`).join(' ')}
                                fill="none"
                                stroke="#2563EB"
                                strokeWidth="3"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                            />

                            {/* 점 + 아래 월 표시 */}
                            {chartPoints.map((point) => {
                                const isSelected = point.month === selectedMonth
                                const tooltipX = Math.min(Math.max(point.x - 74, 6), 486)
                                const tooltipY = point.y <= 80 ? point.y + 18 : point.y - 64

                                return (
                                    <g
                                        key={point.month}
                                        role="button"
                                        tabIndex={0}
                                        aria-pressed={isSelected}
                                        aria-label={`${point.month} 운임 내역 보기`}
                                        onMouseEnter={() => setTooltipMonth(point.month)}
                                        onMouseLeave={() => setTooltipMonth(null)}
                                        onFocus={() => setTooltipMonth(point.month)}
                                        onBlur={() => setTooltipMonth(null)}
                                        onClick={() => {
                                            setTooltipMonth(null)
                                            selectMonth(point.month)
                                        }}
                                        onKeyDown={(event) => {
                                            if (event.key === 'Enter' || event.key === ' ') {
                                                event.preventDefault()
                                                setTooltipMonth(null)
                                                selectMonth(point.month)
                                            }
                                        }}
                                        className="cursor-pointer focus:outline-none focus-visible:outline-2 focus-visible:outline-blue-500"
                                    >

                                        {/* 클릭 영역 확대 */}
                                        <circle
                                            cx={point.x}
                                            cy={point.y}
                                            r="20"
                                            fill="transparent"
                                        />

                                        {/* 선택된 월 강조 */}
                                        {isSelected && (
                                            <circle
                                                cx={point.x}
                                                cy={point.y}
                                                r="10"
                                                fill="#2563EB"
                                                opacity="0.12"
                                                pointerEvents="none"
                                            />
                                        )}

                                        <circle
                                            cx={point.x}
                                            cy={point.y}
                                            r={isSelected ? 6 : 4}
                                            fill="#2563EB"
                                            pointerEvents="none"
                                        />
                                        <text
                                            x={point.x}
                                            y="200"
                                            textAnchor="middle"
                                            fontSize="15"
                                            fontWeight={isSelected ? '700' : '400'}
                                            fill={isSelected ? '#2563EB' : '#6B7280'}
                                            pointerEvents="none"
                                        >
                                            {Number(point.month.slice(5))}월
                                        </text>

                                        {/* 운임 그래프 커스텀 툴팁 */}
                                        <g className={`pointer-events-none transition-opacity duration-150 ${tooltipMonth === point.month ? 'opacity-100' : 'opacity-0'
                                            }`}>
                                            <rect
                                                x={tooltipX}
                                                y={tooltipY}
                                                width="148"
                                                height="50"
                                                rx="9"
                                                fill="#1F2937"
                                            />

                                            <text
                                                x={tooltipX + 74}
                                                y={tooltipY + 19}
                                                textAnchor="middle"
                                                fontSize="12"
                                                fill="#D1D5DB"
                                            >
                                                {point.month.slice(0, 4)}년 {Number(point.month.slice(5))}월
                                            </text>

                                            <text
                                                x={tooltipX + 74}
                                                y={tooltipY + 38}
                                                textAnchor="middle"
                                                fontSize="16"
                                                fontWeight="700"
                                                fill="#FFFFFF"
                                            >
                                                {point.totalFare.toLocaleString('ko-KR')}원
                                            </text>
                                        </g>

                                    </g>
                                )
                            })}
                        </svg>

                        {/* 월별 지급 상태 — 관리자 월 마감 뒤 수수료를 뺀 지급액 · 지급일 */}
                        {monthlyTrend.length > 0 && (
                            <div className="mt-4 border-t border-gray-100 pt-4">
                                <h3 className="text-sm font-semibold text-gray-700">월별 지급 상태</h3>
                                <ul className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-3">
                                    {monthlyTrend.map((item) => {
                                        const info = getPayoutInfo(item)

                                        return (
                                            <li key={item.month} className="rounded-xl bg-gray-50 p-3">
                                                <div className="flex flex-wrap items-center justify-between gap-2">
                                                    <span className="text-sm font-semibold text-gray-900">
                                                        {item.month.replace('-', '.')}
                                                    </span>
                                                    <Badge tone={info.tone} size="sm">{info.label}</Badge>
                                                </div>
                                                <p className="mt-2 text-base font-bold text-gray-900">
                                                    {item.payoutAmount != null
                                                        ? `${Number(item.payoutAmount).toLocaleString('ko-KR')}원`
                                                        : '—'}
                                                </p>
                                                <p className="mt-0.5 text-xs text-gray-500">{info.detail}</p>
                                            </li>
                                        )
                                    })}
                                </ul>
                            </div>
                        )}

                    </div>

                    {/* 월별 운임 상세 내역 */}
                    <div className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm sm:p-6">
                        <h2 className="text-lg font-bold text-gray-900">
                            운임 상세 내역 · {startDate} ~ {endDate}
                        </h2>

                        <div className="relative mt-3 min-h-[160px]">
                            <div className={`divide-y divide-gray-100 ${loading ? 'invisible' : ''}`}>
                                {fareItems.length > 0 ? (
                                    fareItems.map((item) => (
                                        <div
                                            key={`${item.shipmentId}-${item.completedAt}`}
                                            className="flex items-center justify-between gap-3 py-4"
                                        >
                                            <div className="flex min-w-0 items-center gap-3">
                                                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-xl">
                                                    🚛
                                                </div>

                                                <div className="min-w-0">
                                                    <p className="truncate font-semibold text-gray-900">
                                                        {item.cargoName}
                                                    </p>

                                                    <p className="mt-1 text-xs text-gray-500">
                                                        {formatDateTime(item.completedAt, '완료일 정보 없음')}
                                                    </p>
                                                </div>
                                            </div>

                                            <p className="shrink-0 font-bold text-green-700">
                                                +{Number(item.fare ?? 0).toLocaleString('ko-KR')}원
                                            </p>
                                        </div>
                                    ))
                                ) : (
                                    <p className="py-8 text-center text-sm text-gray-500">
                                        해당 월에 완료된 배송 내역이 없습니다.
                                    </p>
                                )}
                            </div>

                            {loading && (
                                <p className="absolute inset-0 flex items-center justify-center text-sm text-gray-500">
                                    운임 내역을 불러오는 중...
                                </p>
                            )}
                        </div>
                    </div>

                    <p className="px-1 text-xs leading-5 text-gray-500">
                        ※ 표시된 운임 합계는 수수료 차감 전 금액입니다.
                        실제 지급액과 지급일은 관리자가 월 마감을 한 뒤 위 '월별 지급 상태'에서 확인할 수 있습니다.
                    </p>

                </div>
            )}
        </div>
    )
}

export default DriverSettlementPage
