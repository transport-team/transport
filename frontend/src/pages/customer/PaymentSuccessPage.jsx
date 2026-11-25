import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { confirmPayment } from '../../api/paymentApi'

const PaymentSuccessPage = () => {
  const [searchParams] = useSearchParams()
  const paymentKey = searchParams.get('paymentKey')
  const orderId = searchParams.get('orderId')
  const amount = searchParams.get('amount')
  const hasPaymentParams = paymentKey && orderId && amount
  const [message, setMessage] = useState(
    hasPaymentParams ? '결제를 승인하는 중입니다...' : '결제 승인에 필요한 정보가 없습니다.',
  )
  // 승인 결과: pending(승인 중) · success · failed — 실패해도 '…' · '결제 처리'로 남던 것 (#143)
  const [result, setResult] = useState(hasPaymentParams ? 'pending' : 'failed')
  const success = result === 'success'
  const failed = result === 'failed'

  useEffect(() => {
    if (!hasPaymentParams) return

    confirmPayment({ paymentKey, orderId, amount })
      .then(() => {
        setResult('success')
        setMessage('결제가 완료되었습니다. 배송 접수가 확정되었습니다.')
      })
      .catch((error) => {
        if (
          error.response?.status === 409
          && error.response?.data?.code === 'PAYMENT_ALREADY_COMPLETED'
        ) {
          setResult('success')
          setMessage('결제가 이미 완료되었습니다. 배송 접수가 확정되었습니다.')
          return
        }
        setResult('failed')
        setMessage(error.response?.data?.message || '결제 승인에 실패했습니다.')
      })
  }, [amount, hasPaymentParams, orderId, paymentKey])

  return (
    <section className="mx-auto min-h-screen max-w-[500px] bg-gray-50 px-5 py-10">
      <div className="relative overflow-hidden rounded-2xl bg-white p-6 shadow-md">
        <div className="absolute -left-3 top-1/2 h-6 w-6 -translate-y-1/2 rounded-full bg-gray-50" />
        <div className="absolute -right-3 top-1/2 h-6 w-6 -translate-y-1/2 rounded-full bg-gray-50" />
        <div className="border-b border-dashed border-gray-200 pb-6 text-center">
          <div className={`mx-auto flex h-16 w-16 items-center justify-center rounded-full text-3xl ${success ? 'bg-emerald-50 text-emerald-600' : failed ? 'bg-red-50 text-red-600' : 'bg-gray-100'}`} aria-hidden="true">
            {success ? '✓' : failed ? '✕' : '…'}
          </div>
          <h1 className="mt-4 text-2xl font-black text-gray-900">{success ? '결제 완료' : failed ? '결제 승인 실패' : '결제 승인 중'}</h1>
          <p className={`mt-2 text-sm ${failed ? 'text-red-600' : 'text-gray-500'}`} role={failed ? 'alert' : 'status'}>{message}</p>
          {failed && (
            <p className="mt-2 text-xs leading-5 text-gray-500">
              결제가 완료되지 않았어요. 내 배송에서 결제 대기 중인 배송을 열어 다시 결제할 수 있어요. (신청 30분 안)
            </p>
          )}
        </div>
        <div className="space-y-3 py-6 text-sm">
          <div className="flex justify-between"><span className="text-gray-500">주문번호</span><strong className="max-w-[210px] truncate text-gray-900">{orderId || '-'}</strong></div>
          <div className="flex justify-between"><span className="text-gray-500">결제 금액</span><strong className="text-gray-900">{amount ? `${Number(amount).toLocaleString()}원` : '-'}</strong></div>
          <div className="mt-5 h-10 bg-[repeating-linear-gradient(90deg,#111_0,#111_2px,transparent_2px,transparent_5px,#111_5px,#111_6px,transparent_6px,transparent_9px)] opacity-70" aria-label="결제 영수증 바코드" />
        </div>
        {/* 버튼 색은 #169 브랜드 테마(blue)를 따른다 */}
        {failed ? (
          <div className="grid grid-cols-2 gap-2">
            <Link className="block rounded-xl bg-blue-600 px-4 py-3 text-center text-sm font-bold text-white" to="/customer/shipments">내 배송으로</Link>
            <Link className="block rounded-xl border border-gray-200 px-4 py-3 text-center text-sm font-bold text-gray-700" to="/customer">고객 홈으로</Link>
          </div>
        ) : (
          <Link className="block rounded-xl bg-blue-600 px-4 py-3 text-center text-sm font-bold text-white" to="/customer">고객 홈으로</Link>
        )}
      </div>
    </section>
  )
}

export default PaymentSuccessPage
