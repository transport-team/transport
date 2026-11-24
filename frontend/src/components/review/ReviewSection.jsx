// 리뷰 영역 (8-3 — 팀원 4)
// 배송 상세 하단에 넣는다. 상태·기존 리뷰에 따라 자동 분기:
//   - COMPLETED + 리뷰 없음 + 내 배송 → ReviewForm (별점 + 후기)
//   - 리뷰 있음 → ReviewView (읽기 전용)
//   - 그 외 → 아무것도 안 보임 (진행 중 배송에는 리뷰가 나오면 안 된다)
// 팀원 3 사용법: import ReviewSection from '../review/ReviewSection'
//   <ReviewSection shipmentId={id} status={shipment.status} isMine={shipment.customerId === myId} />
import { useEffect, useState } from 'react'
import { createReview, getShipmentReview } from '../../api/reviewApi'

const STARS = [1, 2, 3, 4, 5]

const StarRow = ({ value, onChange, size = 'text-2xl' }) => (
    <div className={`flex gap-1 ${size}`}>
        {STARS.map((n) => (
            <button
                key={n}
                type="button"
                disabled={!onChange}
                onClick={() => onChange(n)}
                aria-label={`${n}점`}
                className={`transition ${n <= value ? 'text-amber-400' : 'text-gray-300'} ${onChange ? 'hover:scale-110' : 'cursor-default'}`}
            >
                ★
            </button>
        ))}
    </div>
)

const ReviewSection = ({ shipmentId, status, isMine }) => {
    const [review, setReview] = useState(null)      // 기존 리뷰
    const [checked, setChecked] = useState(false)   // 조회 완료 여부
    const [rating, setRating] = useState(0)
    const [content, setContent] = useState('')
    const [submitting, setSubmitting] = useState(false)
    const [error, setError] = useState('')

    const showForm = isMine && status === 'COMPLETED' && !review

    useEffect(() => {
        if (status !== 'COMPLETED') return
        let cancelled = false
        getShipmentReview(shipmentId)
            .then((data) => { if (!cancelled) setReview(data) })
            .catch(() => {/* 조회 실패는 조용히 — 폼만 보여준다 */})
            .finally(() => { if (!cancelled) setChecked(true) })
        return () => { cancelled = true }
    }, [shipmentId, status])

    const handleSubmit = async () => {
        if (rating === 0) {
            setError('별점을 선택해 주세요.')
            return
        }
        setSubmitting(true)
        setError('')
        try {
            const saved = await createReview(shipmentId, { rating, content })
            setReview(saved)
        } catch (err) {
            const code = err.response?.data?.code
            if (code === 'REVIEW_EXISTS') setError('이미 리뷰를 작성했어요.')
            else if (code === 'REVIEW_NOT_ALLOWED') setError('배송완료 후에 리뷰를 쓸 수 있어요.')
            else if (err.response?.data?.message) setError(err.response.data.message)
            else setError('리뷰 저장에 실패했어요. 잠시 후 다시 시도해 주세요.')
        } finally {
            setSubmitting(false)
        }
    }

    if (status !== 'COMPLETED' && !checked) return null

    // 읽기 전용 — 리뷰가 있는 경우
    if (review) {
        return (
            <div className="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
                <div className="flex items-center justify-between">
                    <StarRow value={review.rating} size="text-xl" />
                    <span className="text-xs text-gray-400">
                        {new Date(review.createdAt).toLocaleDateString('ko-KR')}
                    </span>
                </div>
                {review.content && <p className="mt-2 text-sm text-gray-700">{review.content}</p>}
                <p className="mt-2 text-xs text-gray-400">{review.customerName} 님</p>
            </div>
        )
    }

    // 작성 폼 — 완료 + 내 배송 + 리뷰 없음
    if (showForm) {
        return (
            <div className="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
                <p className="text-sm font-bold text-gray-800">이번 배송은 어땠나요?</p>
                <div className="mt-2">
                    <StarRow value={rating} onChange={setRating} />
                </div>
                <textarea
                    value={content}
                    onChange={(e) => setContent(e.target.value)}
                    maxLength={500}
                    rows={3}
                    placeholder="후기를 남겨주세요 (선택, 500자 이하)"
                    className="mt-3 w-full rounded-xl border border-gray-300 p-3 text-sm focus:border-blue-500 focus:outline-none"
                />
                <div className="mt-2 flex items-center justify-between">
                    <span className="text-xs text-gray-400">{content.length}/500</span>
                    <button
                        type="button"
                        onClick={handleSubmit}
                        disabled={submitting || rating === 0}
                        className="rounded-xl bg-blue-600 px-5 py-2 text-sm font-bold text-white transition hover:bg-blue-700 disabled:opacity-50"
                    >
                        {submitting ? '저장 중...' : '리뷰 등록'}
                    </button>
                </div>
                {error && <p className="mt-2 text-sm font-semibold text-red-600">{error}</p>}
            </div>
        )
    }

    return null
}

export default ReviewSection
