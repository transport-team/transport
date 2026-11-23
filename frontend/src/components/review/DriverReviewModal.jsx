import { useEffect, useState } from 'react'
import { getDriverReviews } from '../../api/reviewApi'
import Modal from '../common/Modal'

// 지원 기사 선택 전 공개 후기 확인용 (기사 ID 기반)
// 창은 공통 Modal — Esc · 배경 누르기 · 닫기 버튼으로 닫히고, 열린 동안 뒤 화면 스크롤을 막는다
// 쓰는 곳이 열 때만 그리므로 open은 항상 true
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
    <Modal open onClose={onClose} title={`${driverName} 기사 리뷰`} size="md">
      {!loading && !error && (
        <p className="border-b border-gray-100 pb-3 text-sm text-gray-600">
          평균 ★ {Number(data?.averageRating ?? 0).toFixed(1)} · 리뷰 {reviews.length}건
        </p>
      )}
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
    </Modal>
  )
}

export default DriverReviewModal
