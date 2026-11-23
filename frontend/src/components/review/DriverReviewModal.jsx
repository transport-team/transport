import { useEffect, useState } from 'react'
import { getDriverReviews } from '../../api/reviewApi'

// 지원 기사 선택 전 공개 후기 확인용 (기사 ID 기반)
const DriverReviewModal = ({ driverId, driverName, onClose }) => {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    getDriverReviews(driverId)
      .then((value) => { if (active) setData(value) })
      .catch((err) => {
        if (active) setError(err.response?.data?.message || '기사 리뷰를 불러오지 못했습니다.')
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [driverId])

  const reviews = Array.isArray(data?.reviews) ? data.reviews : []
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
      <section role="dialog" aria-modal="true" aria-label={`${driverName} 기사 리뷰`} className="max-h-[80vh] w-full max-w-lg overflow-y-auto rounded-2xl bg-white p-5 shadow-xl">
        <div className="flex items-start justify-between gap-3 border-b border-gray-100 pb-3">
          <div>
            <h3 className="text-lg font-bold text-gray-900">{driverName} 기사 리뷰</h3>
            {!loading && !error && <p className="mt-1 text-sm text-gray-600">평균 ★ {Number(data?.averageRating ?? 0).toFixed(1)} · 리뷰 {reviews.length}건</p>}
          </div>
          <button type="button" onClick={onClose} className="rounded-lg border border-gray-200 px-3 py-1.5 text-sm text-gray-600" aria-label="닫기">닫기</button>
        </div>
        {loading ? <p className="py-8 text-center text-sm text-gray-500">리뷰를 불러오는 중...</p> : error ? <p role="alert" className="py-5 text-sm text-red-600">{error}</p> : reviews.length === 0 ? (
          <p className="py-8 text-center text-sm text-gray-500">아직 작성된 리뷰가 없습니다.</p>
        ) : (
          <div className="divide-y divide-gray-100">
            {reviews.map((review) => (
              <article key={review.id} className="py-4">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="text-sm font-bold text-amber-500">{'★'.repeat(Math.max(0, Math.min(5, review.rating || 0)))}<span className="text-gray-200">{'★'.repeat(5 - Math.max(0, Math.min(5, review.rating || 0)))}</span></span>
                  <span className="text-xs text-gray-400">{review.createdAt?.replace('T', ' ').slice(0, 10) || '-'}</span>
                </div>
                {review.content && <p className="mt-2 whitespace-pre-wrap break-words text-sm text-gray-700">{review.content}</p>}
                <p className="mt-1 text-xs text-gray-400">{review.customerName || '고객'}님</p>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}

export default DriverReviewModal
