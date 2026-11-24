// 가로 막대 차트 (계열 하나): 차종처럼 이름이 있는 값을 막대로 보여 준다
// 이름은 막대 위에, 값은 막대 끝에 쓰고, 마우스를 올리면(또는 Tab) detail이 보인다
// items: [{ key, label, value, detail? }], format: 값 표시 함수(40건)

const VALUE_SPACE = '5rem'  // 막대 끝 값이 들어갈 자리 (가장 긴 막대도 값이 잘리지 않게)

const BarChart = ({ items, format }) => {
  const max = Math.max(0, ...items.map((item) => item.value))
  const width = (value) => `calc((100% - ${VALUE_SPACE}) * ${max ? value / max : 0})`

  return (
    <ul className="grid gap-3">
      {items.map((item) => (
        <li
          key={item.key}
          tabIndex={0}
          aria-label={`${item.label} ${format(item.value)}${item.detail ? `, ${item.detail}` : ''}`}
          className="group relative rounded-sm outline-none focus-visible:ring-2 focus-visible:ring-primary"
        >
          <p className="text-sm text-text-secondary">{item.label}</p>
          {/* 막대: 두께 20px · 끝만 둥글게 */}
          <div className="relative mt-1 h-5">
            <div
              className="h-full rounded-r bg-primary transition-colors group-hover:bg-primary-hover group-focus-visible:bg-primary-hover"
              style={{ width: width(item.value) }}
            />
            <span
              className="absolute top-1/2 -translate-y-1/2 whitespace-nowrap pl-2 text-sm text-text"
              style={{ left: width(item.value) }}
            >
              {format(item.value)}
            </span>
          </div>
          {item.detail && (
            <div className="pointer-events-none absolute bottom-full right-0 z-10 mb-1 hidden whitespace-nowrap rounded border border-border bg-surface px-2 py-1 shadow-sm group-hover:block group-focus-visible:block">
              <p className="text-sm font-semibold text-text">{item.detail}</p>
              <p className="text-xs text-text-secondary">{item.label}</p>
            </div>
          )}
        </li>
      ))}
    </ul>
  )
}

export default BarChart
