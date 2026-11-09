import { Link, useSearchParams } from 'react-router-dom'

const PaymentFailPage = () => {
  const [searchParams] = useSearchParams()
  const message = searchParams.get('message') || '결제가 취소되었거나 승인에 실패했습니다.'

  return (
    <section className="mx-auto max-w-xl rounded-xl border border-danger bg-surface p-8 text-center">
      <h1 className="text-2xl font-bold text-danger">결제 실패</h1>
      <p className="mt-4 text-text-muted" role="alert">{message}</p>
      <Link className="mt-6 inline-block text-primary hover:underline" to="/customer/shipments/new">
        배송 신청으로 돌아가기
      </Link>
    </section>
  )
}

export default PaymentFailPage
