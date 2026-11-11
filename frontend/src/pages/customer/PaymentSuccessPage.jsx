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
  const [success, setSuccess] = useState(false)

  useEffect(() => {
    if (!hasPaymentParams) return

    confirmPayment({ paymentKey, orderId, amount })
      .then(() => {
        setSuccess(true)
        setMessage('결제가 완료되었습니다. 배송 접수가 확정되었습니다.')
      })
      .catch((error) => {
        if (
          error.response?.status === 409
          && error.response?.data?.code === 'PAYMENT_ALREADY_COMPLETED'
        ) {
          setSuccess(true)
          setMessage('결제가 이미 완료되었습니다. 배송 접수가 확정되었습니다.')
          return
        }
        setMessage(error.response?.data?.message || '결제 승인에 실패했습니다.')
      })
  }, [amount, hasPaymentParams, orderId, paymentKey])

  return (
    <section className="mx-auto max-w-xl rounded-xl border border-border bg-surface p-8 text-center">
      <h1 className="text-2xl font-bold">{success ? '결제 완료' : '결제 처리'}</h1>
      <p className="mt-4 text-text-muted" role="status">{message}</p>
      <Link className="mt-6 inline-block text-primary hover:underline" to="/customer">
        고객 홈으로
      </Link>
    </section>
  )
}

export default PaymentSuccessPage
