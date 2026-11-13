import { useEffect, useState } from 'react'
import { getDailyStats, getDashboard } from '../../api/dashboardApi'
import Button from '../../components/common/Button'
import Loading from '../../components/common/Loading'
import Table from '../../components/common/Table'
import BarChart from '../../components/dashboard/BarChart'
import ColumnChart from '../../components/dashboard/ColumnChart'
import StatCard from '../../components/dashboard/StatCard'

const RANGES = [7, 14, 30]  // 일별 차트 기간(일)
const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']
const ALERT_CLASS = 'rounded bg-danger-light px-3 py-2 text-sm text-danger-text'

const won = (value) => `${value.toLocaleString('ko-KR')}원`
const count = (value) => `${value.toLocaleString('ko-KR')}건`
const rating = (value) => value.toFixed(1)  // 4 → '4.0'

// 기사 순위 표: 완료 건수가 같으면 같은 순위, 평점은 전체 기간 리뷰 (리뷰가 없으면 0.0이 온다)
const RANK_COLUMNS = [
  { key: 'rank', header: '순위', render: (value) => `${value}위` },
  { key: 'driverName', header: '기사' },
  { key: 'completedCount', header: '완료', className: 'tabular-nums', render: count },
  { key: 'averageRating', header: '평점', className: 'tabular-nums', render: (value) => (value > 0 ? rating(value) : '리뷰 없음') },
]

// 브라우저 날짜로 YYYY-MM-DD (toISOString은 UTC라 오전 9시 전에는 하루 전 날짜가 나온다)
const toDateParam = (date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

// 오늘까지 days일
const dailyRange = (days) => {
  const today = new Date()
  const from = new Date(today.getFullYear(), today.getMonth(), today.getDate() - (days - 1))
  return { from: toDateParam(from), to: toDateParam(today) }
}

// '2026-11-06' → { year: 2026, month: 11, day: 6, axis: '11/6', full: '11월 6일 (금)' }
const parseDate = (value) => {
  const [year, month, day] = value.split('-').map(Number)
  const weekday = WEEKDAYS[new Date(year, month - 1, day).getDay()]
  return { year, month, day, axis: `${month}/${day}`, full: `${month}월 ${day}일 (${weekday})` }
}

// 표로 보기: 차트 값을 마우스 없이 읽는 곳
const StatTable = ({ head, rows }) => (
  <details className="mt-3 text-sm">
    <summary className="cursor-pointer text-text-secondary">표로 보기</summary>
    <table className="mt-2 w-full">
      <thead>
        <tr className="border-b border-border text-left text-text-secondary">
          {head.map((title, index) => (
            <th key={title} className={`py-1 font-medium ${index > 0 ? 'text-right' : ''}`}>{title}</th>
          ))}
        </tr>
      </thead>
      <tbody className="tabular-nums">
        {rows.map((row) => (
          <tr key={row.key} className="border-b border-border last:border-0">
            {row.cells.map((cell, index) => (
              <td key={head[index]} className={`py-1 ${index > 0 ? 'text-right' : ''}`}>{cell}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  </details>
)

// 오늘 · 지금 · 이번 달 숫자, 평균 평점, 처리할 일, 이번 달 차종별 · 기사 순위
const DashboardSummary = ({ dashboard }) => {
  const { month } = parseDate(dashboard.baseDate)
  const vehicleTypeStats = dashboard.vehicleTypeStats
  const hasMonthPayments = vehicleTypeStats.some((stat) => stat.count > 0)
  const hasReviews = dashboard.reviewCount > 0

  return (
    <>
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        <StatCard label="오늘 접수" value={dashboard.todayReceivedCount} unit="건" note={`오늘 매출 ${won(dashboard.todayRevenue)}`} />
        <StatCard label="오늘 완료" value={dashboard.todayCompletedCount} unit="건" note="기사가 배송완료로 처리한 건" />
        <StatCard label="배차 대기" value={dashboard.waitingDispatchCount} unit="건" note="결제가 끝나고 배차 전" />
        <StatCard label="진행 중" value={dashboard.inProgressCount} unit="건" note="배차 완료부터 도착까지" />
        <StatCard
          label="이번 달 매출"
          value={dashboard.monthRevenue}
          unit="원"
          note={`${month}월 1일부터 · 결제 ${count(dashboard.monthPaidCount)}`}
        />
        <StatCard
          label="평균 평점"
          value={hasReviews ? rating(dashboard.averageRating) : '-'}
          unit={hasReviews ? '점' : ''}
          note={hasReviews ? `리뷰 ${dashboard.reviewCount.toLocaleString('ko-KR')}개 · 전체 기간` : '아직 리뷰가 없어요'}
        />
      </div>

      <section aria-labelledby="todo-title">
        <h2 id="todo-title" className="text-base font-semibold">처리할 일</h2>
        <div className="mt-2 grid gap-3 sm:grid-cols-3">
          <StatCard label="기사 승인 대기" value={dashboard.pendingDriverCount} unit="명" note="기사 관리 화면은 준비 중이에요" />
          <StatCard
            label="차량 등록 신청"
            value={dashboard.pendingVehicleRequestCount}
            unit="건"
            note="눌러서 신청함으로"
            to="/admin/vehicle-requests"
          />
          <StatCard
            label="답변 대기 문의"
            value={dashboard.waitingInquiryCount}
            unit="건"
            note="눌러서 문의 관리로"
            to="/admin/inquiries"
          />
        </div>
      </section>

      <div className="grid gap-6 lg:grid-cols-2">
        <section aria-labelledby="vehicle-title" className="rounded-lg border border-border bg-surface p-4">
          <h2 id="vehicle-title" className="text-sm font-medium text-text">{month}월 차종별 접수</h2>
          {hasMonthPayments ? (
            <>
              <div className="mt-4">
                <BarChart
                  items={vehicleTypeStats.map((stat) => ({
                    key: stat.vehicleTypeId,
                    label: stat.vehicleTypeName,
                    value: stat.count,
                    detail: `${count(stat.count)} · ${won(stat.revenue)}`,
                  }))}
                  format={count}
                />
              </div>
              <StatTable
                head={['차종', '건수', '매출']}
                rows={vehicleTypeStats.map((stat) => ({
                  key: stat.vehicleTypeId,
                  cells: [stat.vehicleTypeName, count(stat.count), won(stat.revenue)],
                }))}
              />
            </>
          ) : (
            <p className="mt-3 text-sm text-text-secondary">이번 달에는 결제 완료가 없어요.</p>
          )}
        </section>

        <section aria-labelledby="rank-title" className="rounded-lg border border-border bg-surface p-4">
          <h2 id="rank-title" className="text-sm font-medium text-text">{month}월 기사 순위</h2>
          <p className="mt-1 text-xs text-gray-neutral">배송완료가 많은 기사 5명 · 평점은 전체 기간 리뷰</p>
          <Table
            className="mt-3"
            columns={RANK_COLUMNS}
            data={dashboard.driverRanks}
            keyField="driverId"
            emptyMessage={`${month}월에는 아직 배송완료가 없어요.`}
          />
        </section>
      </div>
    </>
  )
}

// 관리자 대시보드 (ADM-01): 오늘 · 지금 · 이번 달 숫자, 평균 평점, 처리할 일, 이번 달 차종별 · 기사 순위, 일별 결제 완료
// 접수 · 매출은 결제 완료(PAID) 기준이고 환불된 결제는 빠진다. 완료는 상태 이력 기준 (docs/decisions.md)
const AdminHomePage = () => {
  const [dashboard, setDashboard] = useState(null)
  const [error, setError] = useState('')
  const [range, setRange] = useState({ days: 14, id: 0 })      // id: 같은 기간을 다시 눌러도 다시 불러오게
  const [daily, setDaily] = useState({ id: null, list: [] })  // list가 어느 요청의 값인지 함께 둔다
  const [dailyError, setDailyError] = useState('')

  useEffect(() => {
    getDashboard()
      .then(setDashboard)
      .catch((err) => setError(err.response?.data?.message ?? '대시보드를 불러오지 못했어요.'))
  }, [])

  useEffect(() => {
    let ignore = false  // 기간을 빨리 바꿔도 마지막으로 고른 기간의 값만 쓴다
    getDailyStats(dailyRange(range.days))
      .then((list) => {
        if (!ignore) setDaily({ id: range.id, list })
      })
      .catch((err) => {
        if (!ignore) setDailyError(err.response?.data?.message ?? '일별 통계를 불러오지 못했어요.')
      })
    return () => {
      ignore = true
    }
  }, [range])

  const selectRange = (days) => {
    setDailyError('')
    setRange((current) => ({ days, id: current.id + 1 }))
  }

  // 새 기간을 불러오는 동안에는 이전 차트를 흐리게 둔다 (처음 한 번만 로딩 표시)
  const dailyLoading = daily.id !== range.id && !dailyError
  const dailyItems = (field) => daily.list.map((stat) => {
    const date = parseDate(stat.date)
    return { key: stat.date, label: date.full, axisLabel: date.axis, value: stat[field] }
  })
  const baseDate = dashboard && parseDate(dashboard.baseDate)

  return (
    <section className="grid gap-6">
      <header>
        <h1 className="text-xl font-bold">관리자 대시보드</h1>
        <p className="mt-1 text-sm text-text-secondary">
          {baseDate && `${baseDate.year}년 ${baseDate.month}월 ${baseDate.day}일 기준 · `}
          접수와 매출은 결제 완료 기준이에요. 환불된 결제는 빠져요.
        </p>
      </header>

      {error && <p role="alert" className={ALERT_CLASS}>{error}</p>}
      {!dashboard && !error && <Loading text="대시보드를 불러오는 중..." />}
      {dashboard && <DashboardSummary dashboard={dashboard} />}

      <section aria-labelledby="daily-title" className="grid gap-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 id="daily-title" className="text-base font-semibold">일별 결제 완료</h2>
          <div className="flex gap-1" role="group" aria-label="기간">
            {RANGES.map((days) => (
              <Button
                key={days}
                size="sm"
                variant={days === range.days ? 'primary' : 'ghost'}
                aria-pressed={days === range.days}
                onClick={() => selectRange(days)}
              >
                최근 {days}일
              </Button>
            ))}
          </div>
        </div>

        {dailyError && <p role="alert" className={ALERT_CLASS}>{dailyError}</p>}
        {daily.id === null && !dailyError && <Loading text="일별 통계를 불러오는 중..." />}
        {daily.id !== null && !dailyError && (
          <>
            <ColumnChart title="접수 건수" items={dailyItems('count')} format={count} dimmed={dailyLoading} />
            <ColumnChart title="매출" items={dailyItems('revenue')} format={won} dimmed={dailyLoading} />
            <StatTable
              head={['날짜', '건수', '매출']}
              rows={daily.list.map((stat) => ({
                key: stat.date,
                cells: [parseDate(stat.date).full, count(stat.count), won(stat.revenue)],
              }))}
            />
          </>
        )}
      </section>
    </section>
  )
}

export default AdminHomePage
