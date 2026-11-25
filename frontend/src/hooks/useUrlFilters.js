import { useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'

// 목록 필터를 주소(?progress=PENDING&date=2026-11-25)에 남긴다 — 새로고침 · 뒤로 가기에도 그대로 (11-25, useUrlPage와 짝)
//   const RULES = { progress: ['PENDING', 'PROCESSED'], date: (value) => /^\d{4}-\d{2}-\d{2}$/.test(value) }  ← 컴포넌트 밖 상수로
//   const [filters, setFilter, resetFilters] = useUrlFilters(RULES)
//   filters.progress → 허용 값이 아니면 '' (전체), setFilter('progress', 'PENDING'), resetFilters()
// 필터를 바꿀 때는 기록을 쌓지 않는다(replace) — 고를 때마다 뒤로 가기가 쌓이지 않게. 바꾸면 쪽 번호(page)는 1쪽으로, 주소의 다른 값은 그대로
const useUrlFilters = (rules) => {
  const [searchParams, setSearchParams] = useSearchParams()
  const queryString = searchParams.toString()

  const filters = useMemo(() => {
    const params = new URLSearchParams(queryString)
    return Object.fromEntries(Object.entries(rules).map(([name, rule]) => {
      const value = params.get(name) ?? ''
      const allowed = Array.isArray(rule) ? rule.includes(value) : rule(value)
      return [name, value && allowed ? value : '']
    }))
  }, [queryString, rules])

  const update = (change) => {
    setSearchParams((previous) => {
      const params = new URLSearchParams(previous)
      change(params)
      params.delete('page')
      return params
    }, { replace: true })
  }

  const setFilter = (name, value) => update((params) => {
    if (value) params.set(name, value)
    else params.delete(name)
  })

  const resetFilters = () => update((params) => Object.keys(rules).forEach((name) => params.delete(name)))

  return [filters, setFilter, resetFilters]
}

export default useUrlFilters
