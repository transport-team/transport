// 세로 막대 차트 (계열 하나): 날짜처럼 순서가 있는 값을 막대로 보여 준다
// 가장 큰 값만 막대 위에 쓰고, 나머지 값은 눈금 · 마우스 올림(또는 Tab) · 표로 본다
// items: [{ key, label: 툴팁 이름(11월 6일 (금)), axisLabel: 아래 눈금(11/6), value }]
// format: 값 표시 함수(3건, 12,000원), dimmed: 새 기간을 불러오는 동안 이전 그림을 흐리게 둔다

// 눈금 간격: 1 · 2 · 5 × 10ⁿ 중 세 칸 안에 들어가는 가장 작은 값 (정수 눈금)
const niceStep = (max) => {
  const raw = max / 3
  const power = 10 ** Math.floor(Math.log10(raw))
  const step = [1, 2, 5, 10].map((multiple) => multiple * power).find((value) => value >= raw)
  return Math.max(1, step)
}

// 눈금 글자: 1만 미만은 그대로(2,000), 넘으면 줄여서(200만)
const compact = new Intl.NumberFormat('ko-KR', { notation: 'compact' })
const formatTick = (value) => (value >= 10000 ? compact.format(value) : value.toLocaleString('ko-KR'))

// 날짜가 많으면 아래 눈금은 오늘부터 거꾸로 몇 칸마다 하나씩만 쓴다 (글자가 겹치지 않게)
const MAX_AXIS_LABELS = 8

const ColumnChart = ({ title, items, format, dimmed = false }) => {
  const max = Math.max(0, ...items.map((item) => item.value))

  if (max === 0) {
    return (
      <figure className="rounded-lg border border-border bg-surface p-4">
        <figcaption className="text-sm font-medium text-text">{title}</figcaption>
        <p className={`flex h-40 items-center justify-center text-sm text-text-secondary ${dimmed ? 'opacity-50' : ''}`}>
          이 기간에는 결제 완료가 없어요.
        </p>
      </figure>
    )
  }

  const step = niceStep(max)
  const top = Math.ceil(max / step) * step
  const ticks = Array.from({ length: Math.round(top / step) + 1 }, (_, index) => index * step)
  const maxIndex = items.findIndex((item) => item.value === max)
  const labelEvery = Math.ceil(items.length / MAX_AXIS_LABELS)
  const percent = (value) => `${(value / top) * 100}%`

  return (
    <figure className="rounded-lg border border-border bg-surface p-4">
      <figcaption className="text-sm font-medium text-text">{title}</figcaption>
      <div className={`mt-5 flex gap-2 transition-opacity ${dimmed ? 'opacity-50' : ''}`}>
        {/* 세로 눈금 */}
        <div className="relative h-40 w-12 shrink-0 text-right text-xs tabular-nums text-text-secondary" aria-hidden="true">
          {ticks.map((tick) => (
            <span key={tick} className="absolute right-0 translate-y-1/2" style={{ bottom: percent(tick) }}>
              {formatTick(tick)}
            </span>
          ))}
        </div>

        <div className="min-w-0 flex-1">
          <div className="relative h-40">
            {ticks.map((tick) => (
              <div key={tick} className="absolute inset-x-0 border-t border-border" style={{ bottom: percent(tick) }} aria-hidden="true" />
            ))}

            {/* 막대: 칸 전체가 마우스 · Tab 대상, 막대는 최대 24px · 위쪽만 둥글게 · 이웃 막대와 2px 띄움 */}
            <div className="absolute inset-0 flex items-end gap-0.5">
              {items.map((item, index) => (
                <div
                  key={item.key}
                  role="img"
                  tabIndex={0}
                  aria-label={`${item.label} ${format(item.value)}`}
                  className="group relative flex h-full min-w-0 flex-1 items-end justify-center rounded-sm outline-none focus-visible:ring-2 focus-visible:ring-primary"
                >
                  <div
                    className="w-full max-w-6 rounded-t bg-primary transition-colors group-hover:bg-primary-hover group-focus-visible:bg-primary-hover"
                    style={{ height: percent(item.value) }}
                  />
                  {index === maxIndex && (
                    <span
                      className="absolute whitespace-nowrap text-xs font-medium text-text group-hover:invisible group-focus-visible:invisible"
                      style={{ bottom: `calc(${percent(item.value)} + 4px)` }}
                      aria-hidden="true"
                    >
                      {format(item.value)}
                    </span>
                  )}
                  <div
                    className={`pointer-events-none absolute bottom-full z-10 mb-2 hidden whitespace-nowrap rounded border border-border bg-surface px-2 py-1 shadow-sm group-hover:block group-focus-visible:block ${index < items.length / 2 ? 'left-0' : 'right-0'}`}
                  >
                    <p className="text-sm font-semibold text-text">{format(item.value)}</p>
                    <p className="text-xs text-text-secondary">{item.label}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* 가로 눈금 (날짜) */}
          <div className="mt-1 flex gap-0.5 text-xs text-text-secondary" aria-hidden="true">
            {items.map((item, index) => (
              <span key={item.key} className="min-w-0 flex-1 whitespace-nowrap text-center">
                {(items.length - 1 - index) % labelEvery === 0 ? item.axisLabel : ''}
              </span>
            ))}
          </div>
        </div>
      </div>
    </figure>
  )
}

export default ColumnChart
