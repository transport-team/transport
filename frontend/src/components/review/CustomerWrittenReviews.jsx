import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getMyWrittenReviews } from '../../api/reviewApi'
import Modal from '../common/Modal'

// 본인이 작성한 리뷰만 조회한다. 목록은 창을 열 때마다 새로 불러온다.
const WrittenReviewsModal = ({ onClose }) => {
  const [reviews, setReviews] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    getMyWrittenReviews()
      .then((data) => {
        if (active) setReviews(Array.isArray(data) ? data : [])
      })
      .catch((err) => {
        if (active) setError(err.response?.data?.message ?? '내 리뷰를 불러오지 못했어요.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [])

  return (
    <Modal open onClose={onClose} title="내가 남긴 리뷰" size="md">
      {loading ? (
        <p className="py-8 text-center text-sm text-gray-500">리뷰를 불러오는 중...</p>
      ) : error ? (
        <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</p>
      ) : reviews.length === 0 ? (
        <p className="py-8 text-center text-sm text-gray-500">아직 작성한 리뷰가 없어요.</p>
      ) : (
        <>
          <p className="border-b border-gray-100 pb-3 text-sm text-gray-500">작성한 리뷰 {reviews.length}건</p>
          <div className="divide-y divide-gray-100">
            {reviews.map((review) => {
              const rating = Math.max(0, Math.min(5, Number(review.rating) || 0))
              return (
                <article key={review.id} className="py-4">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <span className="font-bold text-amber-500" aria-label={`별점 ${rating}점`}>
                      {'★'.repeat(rating)}<span className="text-gray-200">{'★'.repeat(5 - rating)}</span>
                    </span>
                    <span className="text-xs text-gray-400">{review.createdAt?.replace('T', ' ').slice(0, 10) || '-'}</span>
                  </div>
                  <p className="mt-2 whitespace-pre-wrap break-words text-sm text-gray-700">{review.content || '내용 없이 별점만 남긴 리뷰입니다.'}</p>
                  {review.shipmentId && (
                    <Link
                      to={`/customer/shipments/${review.shipmentId}`}
                      onClick={onClose}
                      className="mt-2 inline-block text-xs font-semibold text-emerald-700 hover:underline"
                    >
                      배송 #{review.shipmentId} 상세 보기
                    </Link>
                  )}
                </article>
              )
            })}
          </div>
        </>
      )}
    </Modal>
  )
}

const CustomerWrittenReviews = () => {
  const [open, setOpen] = useState(false)
  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="flex items-center justify-between rounded-lg border border-gray-200 bg-white px-4 py-3 text-sm font-semibold text-gray-800 hover:border-emerald-600 hover:bg-emerald-50"
      >
        <span>★ 내가 남긴 리뷰</span>
        <span aria-hidden="true" className="text-gray-400">›</span>
      </button>
      {open && <WrittenReviewsModal onClose={() => setOpen(false)} />}
    </>
  )
}

export default CustomerWrittenReviews
