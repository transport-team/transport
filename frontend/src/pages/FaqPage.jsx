import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { FAQ_CATEGORIES, getFaqs } from '../api/faqApi'
import BasicLayout from '../layouts/BasicLayout'

const TABS = ['', ...FAQ_CATEGORIES]  // '' = 전체

// 자주 묻는 질문 (COM-06, 로그인 없이): 분류 탭 · 질문을 누르면 답변이 펼쳐진다
// 분류는 주소(?category=결제)에 둬서 다른 화면에서 그 분류로 바로 열 수 있다
const FaqPage = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const category = searchParams.get('category') ?? ''
  const [faqs, setFaqs] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false  // 탭을 빨리 바꾸면 늦게 온 이전 분류의 응답은 버린다
    getFaqs(category)
      .then((data) => {
        if (!ignore) setFaqs(data)
      })
      .catch((err) => {
        if (!ignore) setError(err.response?.data?.message ?? 'FAQ를 불러오지 못했어요.')
      })
    return () => {
      ignore = true
    }
  }, [category])

  const selectTab = (value) => {
    setError('')
    setSearchParams(value ? { category: value } : {})
  }

  return (
    <BasicLayout>
      <div className="mx-auto grid max-w-3xl gap-4">
        <h1 className="text-2xl font-bold">자주 묻는 질문</h1>
        <div role="group" aria-label="분류" className="flex flex-wrap gap-2">
          {TABS.map((value) => (
            <button
              key={value || 'all'} type="button" aria-pressed={category === value} onClick={() => selectTab(value)}
              className={`rounded-full px-4 py-1.5 text-sm ${
                category === value ? 'bg-emerald-700 font-medium text-white' : 'border border-gray-300 bg-white text-gray-700 hover:bg-gray-50'
              }`}
            >
              {value || '전체'}
            </button>
          ))}
        </div>

        {error && <p role="alert" className="rounded bg-red-50 p-3 text-sm text-red-800">{error}</p>}
        {!faqs && !error && <p className="text-sm text-gray-500">FAQ를 불러오는 중...</p>}

        {faqs && (
          <ul className="divide-y divide-gray-200 rounded-lg border border-gray-200 bg-white">
            {faqs.length === 0 && (
              <li className="px-4 py-8 text-center text-sm text-gray-500">등록된 질문이 없어요.</li>
            )}
            {faqs.map((faq) => (
              <li key={faq.id}>
                <details className="group">
                  <summary className="flex cursor-pointer list-none items-start gap-3 px-4 py-3 hover:bg-gray-50 [&::-webkit-details-marker]:hidden">
                    <span aria-hidden="true" className="font-bold text-emerald-700">Q</span>
                    <span className="flex-1 font-medium">{faq.question}</span>
                    {!category && <span className="shrink-0 text-xs leading-6 text-gray-500">{faq.category}</span>}
                    <span aria-hidden="true" className="text-gray-400 transition-transform group-open:rotate-180">▾</span>
                  </summary>
                  <div className="flex gap-3 bg-gray-50 px-4 py-3">
                    <span aria-hidden="true" className="font-bold text-gray-500">A</span>
                    <p className="flex-1 whitespace-pre-line leading-7 text-gray-800">{faq.answer}</p>
                  </div>
                </details>
              </li>
            ))}
          </ul>
        )}
      </div>
    </BasicLayout>
  )
}

export default FaqPage
