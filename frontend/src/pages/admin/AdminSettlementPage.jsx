import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  getAdminSettlementDifferences,
  getAdminMonthlySettlements,
  getAdminMonthlyTrend,
  markAdminSettlementPaid,
} from '../../api/adminSettlementApi'
import Pagination from '../../components/common/Pagination'
import Modal from '../../components/common/Modal'

const now = new Date()
const THIS_MONTH = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
const PAGE_SIZE = 10
const INPUT = 'rounded-xl border border-gray-200 bg-white px-3 py-2.5 text-sm focus:border-blue-500 focus:outline-none'
const money = (value) => `${Number(value ?? 0).toLocaleString('ko-KR')}원`
const dateTime = (value) => value ? value.replace('T', ' ').slice(0, 16) : '-'

const changeMonth = (month, amount) => {
  const [year, number] = month.split('-').map(Number)
  const next = new Date(year, number - 1 + amount, 1)
  return `${next.getFullYear()}-${String(next.getMonth() + 1).padStart(2, '0')}`
}

const dateRange = (month) => {
  const [year, number] = month.split('-').map(Number)
  const last = new Date(year, number, 0).getDate()
  return `${year}.${String(number).padStart(2, '0')}.01 ~ ${year}.${String(number).padStart(2, '0')}.${String(last).padStart(2, '0')}`
}

const scheduledPayDate = (month) => `${changeMonth(month, 1)}-05`
// 한국 기준 지급 예정일: 현재 화면의 익월 5일. 팀 정책 확정 필요.
const koreanToday = () => new Intl.DateTimeFormat('sv-SE', {
  timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit',
}).format(new Date())

// 스프레드시트에서 사용자 텍스트가 수식으로 실행되지 않도록 보호한다.
const safeCsvCell = (value) => {
  const content = String(value ?? '')
  const escaped = /^[\s]*[=+@-]/.test(content) ? `'${content}` : content
  return `"${escaped.replace(/"/g, '""')}"`
}

// 주소의 조건 (?month=2026-10&status=PAID&keyword=김&page=2) — 모르는 값 · 미래 달은 기본값으로 (11-25, 팀원 1 목록 #152와 같은 방식)
const STATUS_FILTERS = ['NOT_CREATED', 'PENDING', 'PAID', 'LATE']
const readQuery = (params) => {
  const month = params.get('month') ?? ''
  const status = params.get('status') ?? ''
  const page = Math.floor(Number(params.get('page')))
  return {
    month: /^\d{4}-(0[1-9]|1[0-2])$/.test(month) && month <= THIS_MONTH ? month : THIS_MONTH,
    status: STATUS_FILTERS.includes(status) ? status : '',
    keyword: params.get('keyword') ?? '',
    page: page >= 1 ? page : 1,
  }
}
// 기본값은 빼고 주소에 남긴다 (이번 달 · 전체 상태 · 빈 검색어 · 1쪽)
const toUrlParams = ({ month, status, keyword, page }) => ({
  ...(month !== THIS_MONTH && { month }),
  ...(status && { status }),
  ...(keyword && { keyword }),
  ...(page > 1 && { page: String(page) }),
})

const STATUS = {
  NOT_CREATED: { label: '정산 전', className: 'bg-gray-100 text-gray-600' },
  PENDING: { label: '지급 대기', className: 'bg-amber-50 text-amber-700' },
  PAID: { label: '지급 완료', className: 'bg-emerald-50 text-emerald-700' },
  LATE: { label: '마감 후 실적', className: 'bg-red-50 text-red-700' },
}

const AdminSettlementPage = () => {
  // 정산 월 · 상태 · 검색어 · 쪽은 주소에 — 새로고침 · 뒤로 가기에도 그대로 (그래프 기준 월 · 운임/수수료 전환은 보기 설정이라 화면에만)
  const [searchParams, setSearchParams] = useSearchParams()
  const queryString = searchParams.toString()
  const query = useMemo(() => readQuery(new URLSearchParams(queryString)), [queryString])
  const { month, status, keyword, page } = query
  // 월 · 쪽 이동은 기록을 쌓고(뒤로 가기로 돌아옴), 상태 · 검색어 입력은 주소만 바꾼다(replace)
  const setQuery = (next, options) => setSearchParams(toUrlParams({ ...query, ...next }), options)
  const [graphMonth, setGraphMonth] = useState(THIS_MONTH)
  const [chartMetric, setChartMetric] = useState('fare')
  const [reloadKey, setReloadKey] = useState(0)
  // 불러온 정산 내역을 '월#다시 불러오기 번호'와 함께 둔다 — 지금 조건과 다르면 불러오는 중 (뒤로 가기에도 이전 달이 남지 않게)
  const [fetchState, setFetchState] = useState({ key: null, data: null, error: '' })
  const loading = fetchState.key !== `${month}#${reloadKey}`
  const data = loading ? null : fetchState.data
  const listError = loading ? '' : fetchState.error
  const [trend, setTrend] = useState([])
  const [trendError, setTrendError] = useState('')
  const [trendLoading, setTrendLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  // 지급 완료 기록 창 (송금 확인 메모 입력 + 확인을 한 창에서)
  const [payTarget, setPayTarget] = useState(null)
  const [payReference, setPayReference] = useState('')
  const [payError, setPayError] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [hoveredMonth, setHoveredMonth] = useState(null)
  const [difference, setDifference] = useState(null)
  const [differenceKey, setDifferenceKey] = useState(null)
  const [differenceLoading, setDifferenceLoading] = useState(false)
  const [differenceError, setDifferenceError] = useState('')

  // 정산 내역과 그래프는 서로 다른 기준월로 조회한다.
  useEffect(() => {
    let active = true
    const key = `${month}#${reloadKey}`
    getAdminMonthlySettlements(month)
      .then((result) => { if (active) setFetchState({ key, data: result, error: '' }) })
      .catch((err) => {
        if (active) setFetchState({ key, data: null, error: err?.response?.data?.message ?? '정산 내역을 불러오지 못했습니다.' })
      })
    return () => { active = false }
  }, [month, reloadKey])

  useEffect(() => {
    let active = true
    getAdminMonthlyTrend(graphMonth)
      .then((result) => {
        if (!active) return
        setTrend(Array.isArray(result) ? result : [])
        setTrendError('')
      })
      .catch(() => {
        if (!active) return
        setTrend([])
        setTrendError('월별 추이 그래프를 불러오지 못했습니다. 새로고침으로 다시 시도해 주세요.')
      })
      .finally(() => { if (active) setTrendLoading(false) })
    return () => { active = false }
  }, [graphMonth, reloadKey])

  const selectMonth = (next) => {
    if (next === month || next > THIS_MONTH || loading || saving) return
    setQuery({ month: next, page: 1 })
    setError('')
    setMessage('')
    setDifference(null)
    setDifferenceKey(null)
  }

  const moveMonth = (amount) => selectMonth(changeMonth(month, amount))

  const moveGraphMonth = (amount) => {
    const next = changeMonth(graphMonth, amount)
    if (next > THIS_MONTH || trendLoading) return
    setGraphMonth(next)
    setHoveredMonth(null)
    setTrendLoading(true)
  }

  const reload = () => {
    setDifference(null)
    setDifferenceKey(null)
    setTrendLoading(true)
    setReloadKey((value) => value + 1)
  }

  const rows = useMemo(() => {
    const items = data?.items ?? []
    const search = keyword.trim().toLowerCase()
    return items.filter((item) =>
      (!status || item.status === status)
      && (!search || `${item.driverName} ${item.driverEmail}`.toLowerCase().includes(search)),
    )
  }, [data, keyword, status])
  const totalPages = Math.ceil(rows.length / PAGE_SIZE)
  // 주소의 쪽이 끝 쪽보다 크면(?page=99) 끝 쪽을 보여 준다
  const currentPage = Math.min(page, Math.max(totalPages, 1))
  const visibleRows = rows.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE)
  // 차이 상세는 그 달 것만 (뒤로 가기로 달이 바뀌면 닫힌 것으로)
  const showDifference = differenceKey?.startsWith(`${month}:`)
  const paymentAvailable = koreanToday() >= scheduledPayDate(month)
  const hasLate = (data?.items ?? []).some((item) => item.status === 'LATE' || item.changedSinceDraft)

  // 운임과 수수료는 금액 규모가 다르므로 선택한 항목에 맞춰 Y축을 계산한다.
  const chartLeft = 82
  const chartRight = 598
  const chartTop = 45
  const chartBottom = 168
  const chartValueKey = chartMetric === 'fare' ? 'totalFare' : 'totalCommission'
  const chartLabel = chartMetric === 'fare' ? '운임' : '수수료'
  const chartColor = chartMetric === 'fare' ? '#2563EB' : '#10B981'
  const peak = Math.max(0, ...trend.map((item) => Number(item[chartValueKey] ?? 0)))
  const chartPoints = trend.map((item, index) => ({
    ...item,
    x: chartLeft + (chartRight - chartLeft) / Math.max(trend.length - 1, 1) * index,
    y: chartBottom - (Number(item[chartValueKey] ?? 0) / Math.max(peak, 1)) * (chartBottom - chartTop),
  }))

  // 필터가 적용된 모든 기사(현재 페이지 10명만이 아님)를 엑셀에서 열 수 있는 CSV로 저장한다.
  const exportCsv = () => {
    if (loading || !data || rows.length === 0) return
    const header = ['정산월', '기사명', '이메일', '완료 건수', '운임 합계', '수수료율(%)',
      '수수료 합계', '지급액', '상태', '지급 확인 시각', '확인 관리자', '송금 확인내용']
    const lines = rows.map((item) => [
      month, item.driverName, item.driverEmail, item.completedCount, item.totalFare,
      item.commissionRate ?? data.standardCommissionRate,
      item.payoutAmount == null ? '' : item.totalFare - item.payoutAmount,
      item.payoutAmount ?? '', STATUS[item.status]?.label ?? item.status,
      item.paidAt ?? '', item.paidByAdminEmail ?? '', item.transferReference ?? '',
    ])
    const csv = [header, ...lines].map((line) => line.map(safeCsvCell).join(',')).join('\r\n')
    const url = URL.createObjectURL(new Blob(['\uFEFF', csv], { type: 'text/csv;charset=utf-8' }))
    const link = document.createElement('a')
    link.href = url
    link.download = `관리자_정산내역_${month}.csv`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  }

  const toggleDifference = async (item) => {
    const key = `${month}:${item.driverId}`
    if (differenceKey === key) { setDifferenceKey(null); setDifference(null); return }
    setDifferenceKey(key)
    setDifference(null)
    setDifferenceError('')
    setDifferenceLoading(true)
    try {
      const result = await getAdminSettlementDifferences(month, item.driverId)
      setDifference(result)
    } catch (err) {
      setDifferenceError(err?.response?.data?.message ?? '차이 상세 내역을 불러오지 못했습니다.')
    } finally { setDifferenceLoading(false) }
  }

  // 지급 완료 기록 창 열기 (브라우저 입력 창 · 확인 창 2단계 대신, #143)
  const markPaid = (item) => {
    if (!paymentAvailable) { setError(`지급 예정일(${scheduledPayDate(month)})부터 완료 기록이 가능합니다.`); return }
    setPayTarget(item)
    setPayReference('')
    setPayError('')
  }

  const closePay = () => { if (!saving) setPayTarget(null) }

  const submitPaid = async (event) => {
    event.preventDefault()
    const reference = payReference.trim()
    if (!reference || reference.length > 100) {
      setPayError('실제 송금 확인 메모를 1~100자로 입력해 주세요.')
      return
    }
    const item = payTarget
    setSaving(true)
    setPayError('')
    setError('')
    setMessage('')
    try {
      await markAdminSettlementPaid(item.id, reference)
      setPayTarget(null)
      setMessage(`${item.driverName} 기사님의 지급 확인자와 송금 확인 내용을 기록했습니다.`)
      reload()
    } catch (err) {
      setPayError(err.response?.data?.message ?? '지급 완료 기록에 실패했습니다.')
    } finally { setSaving(false) }
  }

  return (
    <section className="mx-auto w-full max-w-6xl space-y-5 pb-8">
      <header>
        <h1 className="text-2xl font-bold text-gray-900">월 정산 · 지급 관리</h1>
        <p className="mt-2 text-sm text-gray-500">모든 기사에게 수수료율 10%를 적용합니다. 매월 1일 전월 금액을 자동 확정하고, 실제 송금 후 지급 완료를 기록합니다.</p>
      </header>

      {/* 기사 정산 페이지 스타일에 맞춘 6개월 운임/수수료 선택 추이 */}
      <div className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm sm:p-6">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-lg font-bold text-gray-900">월 운임 · 수수료 추이</h2>
            <p className="mt-1 text-sm text-gray-500">{changeMonth(graphMonth, -5)} ~ {graphMonth} · 선택 월까지 6개월</p>
          </div>
          <div className="flex flex-col gap-3 sm:items-end">
            <div className="flex items-center gap-3">
              <button type="button" onClick={() => moveGraphMonth(-1)} disabled={trendLoading}
                aria-label="그래프 이전 달 보기"
                className="rounded-xl border border-gray-200 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-40">◀</button>
              <strong className="min-w-32 text-center text-base font-bold text-gray-900">
                {Number(graphMonth.slice(0, 4))}년 {Number(graphMonth.slice(5, 7))}월
              </strong>
              <button type="button" onClick={() => moveGraphMonth(1)} disabled={graphMonth >= THIS_MONTH || trendLoading}
                aria-label="그래프 다음 달 보기"
                className="rounded-xl border border-gray-200 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-40">▶</button>
            </div>
            <div className="inline-flex rounded-xl border border-gray-200 bg-gray-50 p-1" role="group" aria-label="그래프 항목 선택">
              <button type="button" aria-pressed={chartMetric === 'fare'}
                onClick={() => { setChartMetric('fare'); setHoveredMonth(null) }}
                className={`rounded-lg px-4 py-2 text-sm font-semibold transition-colors ${chartMetric === 'fare' ? 'bg-blue-600 text-white shadow-sm' : 'text-gray-600 hover:bg-white'}`}>
                운임비
              </button>
              <button type="button" aria-pressed={chartMetric === 'commission'}
                onClick={() => { setChartMetric('commission'); setHoveredMonth(null) }}
                className={`rounded-lg px-4 py-2 text-sm font-semibold transition-colors ${chartMetric === 'commission' ? 'bg-emerald-600 text-white shadow-sm' : 'text-gray-600 hover:bg-white'}`}>
                수수료
              </button>
            </div>
          </div>
        </div>
        {trendLoading ? <div className="py-16 text-center text-sm text-gray-500">그래프를 불러오는 중...</div>
          : trendError ? <p className="py-12 text-center text-sm text-amber-700">{trendError}</p>
            : chartPoints.length === 0 ? <p className="py-12 text-center text-sm text-gray-500">그래프 데이터가 없습니다.</p> : (
          <svg className="mt-4 w-full" viewBox="0 0 640 215" role="group" aria-label={`최근 6개월 월 ${chartLabel} 합계 꺾은선 그래프`}>
            {[chartTop, (chartTop + chartBottom) / 2, chartBottom].map((y) => (
              <line key={y} x1={chartLeft} y1={y} x2={chartRight} y2={y} stroke="#E5E7EB" strokeDasharray="5 5" />
            ))}
            {[peak, Math.round(peak / 2), 0].map((value, index) => (
              <text key={index} x={chartLeft - 11} y={[chartTop, (chartTop + chartBottom) / 2, chartBottom][index] + 5}
                textAnchor="end" fontSize="12" fill="#6B7280">{value.toLocaleString('ko-KR')}원</text>
            ))}
            <polyline points={chartPoints.map((point) => `${point.x},${point.y}`).join(' ')}
              fill="none" stroke={chartColor} strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
            {chartPoints.map((point) => {
              // 선택한 정산월을 강조하고, 현재 선택한 항목의 실적을 점 근처에 표시한다.
              const chosen = point.month === month
              const tooltipX = Math.min(Math.max(point.x - 92, 4), 452)
              const tooltipY = point.y <= 92 ? point.y + 16 : point.y - 60
              return (
                <g key={point.month} role="button" tabIndex={0}
                  aria-label={`${point.month} 기사별 정산 내역 보기, ${chartLabel} ${money(point[chartValueKey])}`}
                  aria-pressed={chosen}
                  onMouseEnter={() => setHoveredMonth(point.month)}
                  onMouseLeave={() => setHoveredMonth(null)}
                  onFocus={() => setHoveredMonth(point.month)}
                  onBlur={() => setHoveredMonth(null)}
                  onClick={() => { setHoveredMonth(null); selectMonth(point.month) }}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                      event.preventDefault()
                      setHoveredMonth(null)
                      selectMonth(point.month)
                    }
                  }}
                  className="cursor-pointer focus:outline-none focus-visible:outline-2 focus-visible:outline-blue-500">
                  {/* 월 영역 전체에서 선택 가능하도록 클릭 범위를 넓힌다. */}
                  <rect x={point.x - 20} y="30" width="40" height="172" fill="transparent" />
                  {chosen && <circle cx={point.x} cy={point.y} r="11" fill={chartColor} opacity="0.12" pointerEvents="none" />}
                  <circle cx={point.x} cy={point.y} r={chosen ? 6 : 4} fill={chartColor} pointerEvents="none" />
                  <text x={point.x} y="201" fontSize="14" textAnchor="middle" fontWeight={chosen ? '700' : '400'}
                    fill={chosen ? '#2563EB' : '#6B7280'} pointerEvents="none">{Number(point.month.slice(5))}월</text>
                  {/* 기사 페이지와 동일하게 데이터 점 가까이에 뜨는 툴팁 */}
                  <g pointerEvents="none" opacity={hoveredMonth === point.month ? 1 : 0}>
                    <rect x={tooltipX} y={tooltipY} width="184" height="51" rx="9" fill="#1F2937" />
                    <text x={tooltipX + 92} y={tooltipY + 17} textAnchor="middle" fontSize="11" fill="#D1D5DB">
                      {point.month.slice(0, 4)}년 {Number(point.month.slice(5))}월
                    </text>
                    <text x={tooltipX + 92} y={tooltipY + 37} textAnchor="middle" fontSize="12" fontWeight="700" fill="#FFFFFF">
                      {chartLabel} {money(point[chartValueKey])}
                    </text>
                  </g>
                </g>
              )
            })}
          </svg>
        )}
      </div>

      {(error || listError) && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error || listError}</div>}
      {message && <div role="status" className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800">{message}</div>}
      {hasLate && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm font-medium text-red-700">마감 후 완료된 과거 운송이나 기존 지급 내역과의 차이가 발견됐습니다. 확정 금액은 유지되며, 이 건은 별도로 확인해야 합니다.</div>}

      {/* 정산월 · 핵심 지급 현황 · 기사별 정산 내역 통합 카드 */}
      <div className="overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm">
        <div className="p-5 sm:p-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-xs font-semibold text-gray-500">조회 정산월</p>
            <div className="mt-2 flex items-center gap-3">
              <button type="button" onClick={() => moveMonth(-1)} disabled={loading || saving}
                aria-label="이전 달 정산 보기"
                className="rounded-xl border border-gray-200 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-40">◀</button>
              <strong className="min-w-32 text-center text-xl font-bold text-gray-900">
                {Number(month.slice(0, 4))}년 {Number(month.slice(5, 7))}월
              </strong>
              <button type="button" onClick={() => moveMonth(1)} disabled={month >= THIS_MONTH || loading || saving}
                aria-label="다음 달 정산 보기"
                className="rounded-xl border border-gray-200 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-40">▶</button>
            </div>
            <p className="mt-2 text-xs text-gray-500">{dateRange(month)} 기준</p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <span className={`rounded-full px-3 py-1.5 text-xs font-semibold ${data?.closed ? 'bg-emerald-50 text-emerald-700' : 'bg-blue-50 text-blue-700'}`}>
              {loading ? '조회 중' : data?.closed ? '마감 완료 · 금액 확정' : '마감 전 · 예상 금액'}
            </span>
            <button type="button" onClick={reload} disabled={saving || loading}
              className="rounded-xl border border-gray-200 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-40">새로고침</button>
          </div>
        </div>
        <div className="mt-4 border-t border-gray-100 pt-3 text-xs leading-5 text-gray-500">
          <span>공통 수수료율 <strong className="text-gray-800">{data?.standardCommissionRate ?? 10}%</strong></span>
          <span className="mx-2 text-gray-300">·</span>
          <span>지급 예정일 <strong className="text-gray-800">{scheduledPayDate(month)}</strong></span>
          {data?.closedAt && <span className="ml-3">마감 시각 {dateTime(data.closedAt)}</span>}
          <p className="mt-1">{month === THIS_MONTH ? '이번 달은 현재까지 배송 완료된 운임을 실시간으로 집계합니다.' : data?.closed ? '이 달 정산 금액은 확정되었으며 새 완료 실적을 자동 추가하지 않습니다.' : '지난달 정산 자동 마감 대기 중입니다. 서버 상태를 확인해 주세요.'}</p>
        </div>
        </div>
        {/* 선택한 정산월의 세 가지 핵심 지급 현황 */}
        <div className="border-t border-gray-100 px-5 py-5 sm:px-6">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        {[
          { label: data?.closed ? '확정 지급액 합계' : '예상 지급액 합계', value: money(data?.totalPayout), tone: 'bg-blue-50 text-blue-700' },
          { label: data?.closed ? '지급 대기' : '정산 전', value: `${Number(data?.closed ? data.pendingCount : data?.uncreatedCount ?? 0).toLocaleString('ko-KR')}명`, tone: 'bg-amber-50 text-amber-700' },
          { label: '지급 완료', value: `${Number(data?.paidCount ?? 0).toLocaleString('ko-KR')}명`, tone: 'bg-green-50 text-green-700' },
        ].map((item) => (
          <div key={item.label} className={`rounded-2xl p-5 ${item.tone}`}>
            <p className="text-sm font-semibold">{item.label}</p>
            <p className="mt-3 text-2xl font-bold text-gray-900">{loading ? '...' : item.value}</p>
          </div>
        ))}
          </div>
        </div>
        {/* 선택한 정산월의 기사별 정산 내역 */}
        <div className="border-t border-gray-100">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-gray-100 p-5 sm:p-6">
          <div>
            <h2 className="text-lg font-bold text-gray-900">기사별 정산 내역</h2>
            <p className="mt-1 text-xs text-gray-500">조회 {rows.length}명 · {month === THIS_MONTH ? '이번 달 실적은 계속 업데이트됩니다.' : data?.closed ? '마감 완료된 금액은 변경되지 않습니다.' : '정산 마감 전입니다.'}</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <select value={status} onChange={(event) => setQuery({ status: event.target.value, page: 1 }, { replace: true })} aria-label="정산 상태" className={INPUT}>
              <option value="">전체 상태</option>
              <option value="NOT_CREATED">정산 전</option>
              <option value="PENDING">지급 대기</option>
              <option value="PAID">지급 완료</option>
              <option value="LATE">마감 후 실적</option>
            </select>
            <input value={keyword} onChange={(event) => setQuery({ keyword: event.target.value, page: 1 }, { replace: true })}
              placeholder="기사 이름 · 이메일" aria-label="기사 검색" className={INPUT} />
            <button type="button" onClick={exportCsv} disabled={loading || rows.length === 0}
              className="rounded-xl border border-gray-200 bg-white px-3 py-2.5 text-sm font-semibold text-gray-700 hover:bg-gray-50 disabled:opacity-40">
              엑셀용 CSV 다운로드
            </button>
          </div>
        </div>
        {loading ? <p className="p-10 text-center text-sm text-gray-500">정산 내역을 불러오는 중...</p> : rows.length === 0
          ? <p className="p-10 text-center text-sm text-gray-500">{data?.items?.length ? '검색 조건에 맞는 기사가 없습니다.' : '해당 월에 조회할 기사 정산 내역이 없습니다.'}</p>
          : <>
            {/* 넓은 화면: 모든 열을 카드 안에 맞춰 가로 스크롤 없이 표시 */}
            <div className="hidden xl:block">
            <table className="w-full table-fixed text-left text-xs">
              <colgroup>
                <col className="w-[20%]" />
                <col className="w-[9%]" />
                <col className="w-[12%]" />
                <col className="w-[8%]" />
                <col className="w-[12%]" />
                <col className="w-[12%]" />
                <col className="w-[13%]" />
                <col className="w-[14%]" />
              </colgroup>
              <thead className="border-b border-gray-200 bg-gray-50 text-gray-600">
                <tr>
                  <th className="px-2 py-3 sm:px-3">기사</th>
                  <th className="px-2 py-3 sm:px-3 text-right">완료 건수</th>
                  <th className="px-2 py-3 sm:px-3 text-right">운임 합계</th>
                  <th className="px-2 py-3 sm:px-3 text-right">수수료율</th>
                  <th className="px-2 py-3 sm:px-3 text-right">수수료 합계</th>
                  <th className="px-2 py-3 sm:px-3 text-right">{data?.closed ? '확정 지급액' : '예상 지급액'}</th>
                  <th className="px-2 py-3 sm:px-3">정산 상태</th>
                  <th className="px-2 py-3 sm:px-3 text-right">지급 처리</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {visibleRows.map((item) => (
                  <tr key={item.driverId} className="hover:bg-gray-50/80">
                    <td className="px-2 py-3 sm:px-3">
                      <div className="font-semibold text-gray-900">{item.driverName}</div>
                      <div className="mt-1 break-all text-[11px] text-gray-500">{item.driverEmail}</div>
                    </td>
                    <td className="px-2 py-3 sm:px-3 text-right">{item.completedCount}건</td>
                    <td className="px-2 py-3 sm:px-3 text-right font-semibold">{money(item.totalFare)}</td>
                    <td className="px-2 py-3 sm:px-3 text-right">{item.commissionRate ?? data?.standardCommissionRate ?? 10}%</td>
                    <td className="px-2 py-3 sm:px-3 text-right">{item.payoutAmount == null ? '-' : money(item.totalFare - item.payoutAmount)}</td>
                    <td className="px-2 py-3 sm:px-3 text-right font-semibold">{item.payoutAmount == null ? '-' : money(item.payoutAmount)}</td>
                    <td className="px-2 py-3 sm:px-3">
                      <span className={`inline-flex rounded-full px-2.5 py-1 font-semibold ${STATUS[item.status]?.className ?? 'bg-gray-100 text-gray-600'}`}>
                        {STATUS[item.status]?.label ?? item.status}
                      </span>
                      {item.paidAt && <div className="mt-1 text-[11px] text-gray-500">{dateTime(item.paidAt)}</div>}
                      {item.paidByAdminEmail && <div className="mt-1 break-all text-[11px] text-gray-500">확인: {item.paidByAdminEmail}</div>}
                      {item.transferReference && <div className="mt-1 break-all text-[11px] text-gray-500">증빙: {item.transferReference}</div>}
                      {item.changedSinceDraft && <button type="button" onClick={() => toggleDifference(item)}
                        className="mt-1 text-left text-[11px] font-semibold text-red-600 underline hover:text-red-800">실적 차이 상세</button>}
                    </td>
                    <td className="px-2 py-3 sm:px-3 text-right">
                      {data?.closed && item.status === 'PENDING' && item.completedCount > 0
                        ? <button type="button" disabled={saving || !paymentAvailable} onClick={() => markPaid(item)}
                          title={!paymentAvailable ? `${scheduledPayDate(month)} 이후 기록 가능` : '실제 송금 후 증빙을 입력해야 합니다.'}
                          className="w-full rounded-lg border border-emerald-300 px-1.5 py-1.5 font-semibold text-emerald-700 hover:bg-emerald-50 disabled:opacity-40">{paymentAvailable ? '지급 완료 기록' : '지급일 전'}</button>
                        : <span className="text-gray-400">-</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            </div>
            {/* 좁은 화면: 테이블 대신 카드로 모든 금액과 지급 버튼 표시 */}
            <div className="divide-y divide-gray-100 xl:hidden">
              {visibleRows.map((item) => (
                <div key={item.driverId} className="p-4 sm:p-5">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="font-semibold text-gray-900">{item.driverName}</p>
                      <p className="mt-1 break-all text-xs text-gray-500">{item.driverEmail}</p>
                    </div>
                    <div>
                      <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${STATUS[item.status]?.className ?? 'bg-gray-100 text-gray-600'}`}>
                        {STATUS[item.status]?.label ?? item.status}
                      </span>
                      {item.paidAt && <p className="mt-1 text-[11px] text-gray-500">{dateTime(item.paidAt)}</p>}
                      {item.paidByAdminEmail && <p className="mt-1 break-all text-[11px] text-gray-500">확인: {item.paidByAdminEmail}</p>}
                      {item.transferReference && <p className="mt-1 break-all text-[11px] text-gray-500">증빙: {item.transferReference}</p>}
                      {item.changedSinceDraft && <button type="button" onClick={() => toggleDifference(item)}
                        className="mt-1 text-left text-[11px] font-semibold text-red-600 underline hover:text-red-800">실적 차이 상세</button>}
                    </div>
                  </div>
                  <dl className="mt-4 grid grid-cols-2 gap-3 rounded-xl bg-gray-50 p-3 text-xs sm:grid-cols-3">
                    <div><dt className="text-gray-500">완료 건수</dt><dd className="mt-1 font-semibold text-gray-900">{item.completedCount}건</dd></div>
                    <div><dt className="text-gray-500">운임 합계</dt><dd className="mt-1 font-semibold text-gray-900">{money(item.totalFare)}</dd></div>
                    <div><dt className="text-gray-500">수수료율</dt><dd className="mt-1 font-semibold text-gray-900">{item.commissionRate ?? data?.standardCommissionRate ?? 10}%</dd></div>
                    <div><dt className="text-gray-500">수수료 합계</dt><dd className="mt-1 font-semibold text-gray-900">{item.payoutAmount == null ? '-' : money(item.totalFare - item.payoutAmount)}</dd></div>
                    <div><dt className="text-gray-500">{data?.closed ? '확정 지급액' : '예상 지급액'}</dt><dd className="mt-1 font-semibold text-gray-900">{item.payoutAmount == null ? '-' : money(item.payoutAmount)}</dd></div>
                  </dl>
                  {data?.closed && item.status === 'PENDING' && item.completedCount > 0 && (
                    <button type="button" disabled={saving || !paymentAvailable} onClick={() => markPaid(item)}
                      className="mt-3 rounded-lg border border-emerald-300 px-3 py-2 text-xs font-semibold text-emerald-700 hover:bg-emerald-50 disabled:opacity-40">
                      {paymentAvailable ? '지급 완료 기록' : `${scheduledPayDate(month)} 이후 기록 가능`}
                    </button>
                  )}
                </div>
              ))}
            </div>
          </>}
        </div>
        {showDifference && <div className="border-t border-red-100 bg-red-50/60 p-5 text-sm text-gray-700 sm:p-6">
          <div className="flex items-center justify-between gap-3">
            <h3 className="font-bold text-gray-900">마감 후 실적 차이 상세</h3>
            <button type="button" onClick={() => { setDifferenceKey(null); setDifference(null) }}
              className="rounded-lg border border-gray-200 bg-white px-3 py-1.5 text-xs">닫기</button>
          </div>
          {differenceLoading ? <p className="mt-3">차이를 확인하는 중...</p>
            : differenceError ? <p className="mt-3 text-red-700">{differenceError}</p>
            : difference && <>
              <p className="mt-2 text-xs">{difference.driverName} · {difference.month} · {difference.note}</p>
              {difference.lines.length === 0 ? <p className="mt-3">조회 가능한 개별 차이 항목이 없습니다.</p>
                : <div className="mt-3 space-y-2">
                  {difference.lines.map((line) => (
                    <div key={line.shipmentId} className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-white p-3 text-xs">
                      <span className="font-semibold">운송 #{line.shipmentId}</span>
                      <span>{line.differenceType === 'NOT_IN_CLOSED_SETTLEMENT' ? '마감 내역에 없음'
                        : line.differenceType === 'FARE_CHANGED' ? '운임 차이' : '현재 완료 내역에 없음'}</span>
                      <span>확정 {line.fixedFare == null ? '-' : money(line.fixedFare)} / 현재 {line.currentFare == null ? '-' : money(line.currentFare)}</span>
                    </div>
                  ))}
                </div>}
            </>}
        </div>}
      </div>
      {!loading && totalPages > 1 && <Pagination page={currentPage} totalPages={totalPages} onMove={(next) => setQuery({ page: next })} />}
      <p className="text-xs leading-5 text-gray-500">정산 마감 후에는 기존 금액을 다시 계산하지 않습니다. 나중에 발견된 과거 배송 실적은 별도 확인이 필요합니다. 지급 완료 기록은 실제 계좌이체가 아닌 관리자 확인 기록이며, 해당 월의 다음 달 5일 이후에만 허용됩니다. 실제 지급 예정일은 팀 정책 확정이 필요합니다. 이미 확정된 과거 정산의 수수료율은 당시 값을 유지합니다.</p>

      {/* 지급 완료 기록 — 실제 송금 뒤 확인 메모를 남긴다 (송금을 실행하지는 않음) */}
      <Modal open={payTarget !== null} onClose={closePay} title="지급 완료 기록" size="sm">
        {payTarget && (
          <form onSubmit={submitPaid} className="space-y-4">
            <dl className="grid grid-cols-[4.5rem_1fr] gap-y-1.5 rounded-xl bg-gray-50 p-4 text-sm">
              <dt className="text-gray-500">정산 월</dt>
              <dd className="font-medium text-gray-900">{month}</dd>
              <dt className="text-gray-500">기사</dt>
              <dd className="font-medium text-gray-900">{payTarget.driverName}</dd>
              <dt className="text-gray-500">지급액</dt>
              <dd className="font-bold text-gray-900">{money(payTarget.payoutAmount)}</dd>
            </dl>
            <label className="block text-sm font-semibold text-gray-700">
              송금 확인번호 · 메모
              <input
                value={payReference}
                onChange={(event) => setPayReference(event.target.value)}
                maxLength={100}
                autoFocus
                placeholder="은행 송금 확인번호 또는 확인 메모"
                className={`mt-2 block w-full ${INPUT}`}
              />
              <span className="mt-1 block text-right text-xs font-normal text-gray-400">{payReference.trim().length}/100</span>
            </label>
            <p className="text-xs leading-5 text-gray-500">
              실제로 송금한 뒤에 기록해 주세요. 입력해도 송금이 실행되지는 않고, 완료 기록 후에는 바꿀 수 없습니다.
            </p>
            {payError && <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{payError}</p>}
            <div className="flex justify-end gap-2">
              <button type="button" onClick={closePay} disabled={saving}
                className="rounded border border-gray-300 px-4 py-2 text-sm hover:bg-gray-50 disabled:opacity-60">
                취소
              </button>
              <button type="submit" disabled={saving}
                className="rounded bg-primary px-4 py-2 text-sm font-semibold text-white hover:bg-primary-hover disabled:opacity-60">
                {saving ? '기록 중...' : '지급 완료 기록'}
              </button>
            </div>
          </form>
        )}
      </Modal>
    </section>
  )
}

export default AdminSettlementPage
