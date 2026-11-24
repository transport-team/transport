import { useSearchParams } from 'react-router-dom'

// 쪽 번호를 주소(?page=2)에 남긴다 — 새로고침 · 뒤로 가기에도 그 쪽이 그대로 보인다
// 사용: const [page, setPage] = useUrlPage()  (useState(1) 대신, 주소의 다른 조건은 건드리지 않는다)
// 잘못된 값(?page=abc · 0)은 1쪽, 1쪽이면 주소에서 page를 뺀다
const useUrlPage = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const raw = Math.floor(Number(searchParams.get('page')))
  const page = raw >= 1 ? raw : 1

  const setPage = (next) => {
    setSearchParams((previous) => {
      const params = new URLSearchParams(previous)
      if (next > 1) params.set('page', String(next))
      else params.delete('page')
      return params
    })
  }

  return [page, setPage]
}

export default useUrlPage
