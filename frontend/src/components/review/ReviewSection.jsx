// 리뷰 영역 (8-3 — 팀원 4)
// 배송 상세 하단에 넣는다. 상태·기존 리뷰에 따라 자동 분기:
//   - COMPLETED + 리뷰 없음 + 내 배송 → ReviewForm (별점 + 후기)
//   - 리뷰 있음 → ReviewView (읽기 전용)
//   - 그 외 → 아무것도 안 보임 (진행 중 배송에는 리뷰가 나오면 안 된다)
// 팀원 3 사용법: import ReviewSection from '../review/ReviewSection'
//   <ReviewSection shipmentId={id} status={shipment.status} isMine={shipment.customerId === myId} />
import { useEffect, useState, useRef } from 'react'
import { createReview, getShipmentReview, getDriverReviews } from '../../api/reviewApi'
import Input from '../common/Input'
import Button from '../common/Button'

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

// 평균 평점 표시용
const AverageRating = ({ rating, count }) => (
    <div className="flex items-center gap-2">
        <span className="text-2xl font-bold text-gray-900">{rating.toFixed(1)}</span>
        <StarRow value={Math.round(rating)} size="text-lg" />
        <span className="text-sm text-gray-500">({count}개)</span>
    </div>
)

const ReviewSection = ({ shipmentId, status, isMine }) => {
    const [review, setReview] = useState(null)      // 기존 리뷰
    const [checked, setChecked] = useState(false)   // 조회 완료 여부
    const [rating, setRating] = useState(0)
    const [content, setContent] = useState('')
    const [submitting, setSubmitting] = useState(false)
    const [error, setError] = useState('')
    const [driverRating, setDriverRating] = useState(null) // 기사 평균 평점
    const textareaRef = useRef(null)

    const showForm = isMine && status === 'COMPLETED' && !review

    // 기사 평균 평점 조회 (배송 완료 시 항상 표시)
    useEffect(() => {
        if (status !== 'COMPLETED') return
        let cancelled = false
        getDriverReviews(shipmentId) // driverId는 backend에서 shipment로 조회
            .then((data) => { if (!cancelled) setDriverRating(data) })
            .catch(() => {})
        return () => { cancelled = true }
    }, [shipmentId, status])

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

    // textarea 자동 높이 조절
    useEffect(() => {
        if (textareaRef.current) {
            textareaRef.current.style.height = 'auto'
            textareaRef.current.style.height = textareaRef.current.scrollHeight + 'px'
        }
    }, [content])

    if (status !== 'COMPLETED' && !checked) return null

    // 읽기 전용 — 리뷰가 있는 경우
    if (review) {
        return (
            <div className="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
                {driverRating && (
                    <div className="mb-4">
                        <AverageRating rating={driverRating.averageRating} count={driverRating.reviews?.length || 0} />
                    </div>
                )}
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
                {driverRating && (
                    <div className="mb-4">
                        <AverageRating rating={driverRating.averageRating} count={driverRating.reviews?.length || 0} />
                    </div>
                )}
                <p className="text-sm font-bold text-gray-800">이번 배송은 어땠나요?</p>
                <div className="mt-2">
                    <StarRow value={rating} onChange={setRating} />
                </div>
                <Input
                    ref={textareaRef}
                    as="textarea"
                    value={content}
                    onChange={(e) => setContent(e.target.value)}
                    maxLength={500}
                    rows={3}
                    placeholder="후기를 남겨주세요 (선택, 500자 이하)"
                    className="mt-3"
                    label="후기"
                />
                <div className="mt-2 flex items-center justify-between">
                    <span className="text-xs text-gray-400">{content.length}/500</span>
                    <Button onClick={handleSubmit} loading={submitting} disabled={rating === 0} variant="primary">
                        {submitting ? '저장 중...' : '리뷰 등록'}
                    </Button>
                </div>
                {error && <p className="mt-2 text-sm font-semibold text-red-600" role="alert">{error}</p>}
            </div>
        )
    }

    return null
}

export default ReviewSection
