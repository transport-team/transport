import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { retryPayment } from '../../api/paymentApi'
import { getShipment } from '../../api/shipmentApi'
import { requestTossPayment } from '../../util/tossPayments'

const PaymentFailPage = () => {
  const [searchParams] = useSearchParams()
  const shipmentId = searchParams.get('shipmentId')
  const code = searchParams.get('code')
  const message = searchParams.get('message') || '결제가 취소되었거나 승인에 실패했습니다.'
  const isPaymentClosedOrCanceled =
    Boolean(code && /cancel|close|user_cancel/i.test(code)) ||
    /취소|닫|중단/i.test(message)

  const headingText = isPaymentClosedOrCanceled ? '결제창이 닫혔습니다' : '결제 실패'
  const descriptionText = isPaymentClosedOrCanceled
    ? '결제창이 닫혀 결제가 완료되지 않았습니다. 아래 버튼으로 다시 결제하거나 내 배송 내역에서 이어서 결제할 수 있습니다.'
    : message

  const [retrying, setRetrying] = useState(false)
  const [retryError, setRetryError] = useState('')

  const handleRetryPayment = async () => {
    if (!shipmentId) return
    setRetrying(true)
    setRetryError('')
    try {
      const [retried, detail] = await Promise.all([
        retryPayment(shipmentId),
        getShipment(shipmentId),
      ])
      await requestTossPayment({
        shipmentId: Number(shipmentId),
        orderId: retried.orderId,
        amount: retried.amount,
        cargoName: detail.cargoName,
      })
    } catch (error) {
      setRetryError(error.response?.data?.message || '결제 재시도 중 오류가 발생했습니다.')
    } finally {
      setRetrying(false)
    }
  }

  return (
    <section className="mx-auto min-h-screen max-w-[500px] bg-gray-50 px-5 py-10">
      <div className="relative overflow-hidden rounded-2xl bg-white p-6 shadow-md">
        <div className="absolute -left-3 top-1/2 h-6 w-6 -translate-y-1/2 rounded-full bg-gray-50" />
        <div className="absolute -right-3 top-1/2 h-6 w-6 -translate-y-1/2 rounded-full bg-gray-50" />

       <div className="border-b border-dashed border-gray-200 pb-6 text-center">
          <div
            className={`mx-auto flex h-16 w-16 items-center justify-center rounded-full text-3xl font-bold ${
              isPaymentClosedOrCanceled
                ? 'bg-amber-50 text-amber-600'
                : 'bg-red-50 text-red-500'
            }`}
          >
            {isPaymentClosedOrCanceled ? 'i' : '!'}
          </div>
          <h1 className="mt-4 text-2xl font-black text-gray-900">{headingText}</h1>
          <p className="mt-2 text-sm text-gray-600" role="alert">{descriptionText}</p>
          {code && <p className="mt-1 text-xs text-gray-400">오류 코드: {code}</p>}
        </div>

        <div className="space-y-3 py-6 text-sm text-gray-600">
          <p className="rounded-xl bg-amber-50 px-4 py-3 text-xs leading-5 text-amber-800">
            주문 접수 후 <strong>30분 이내</strong>에 결제를 완료하지 않으면 주문이 자동으로 취소됩니다.
          </p>
          {retryError && (
            <p className="rounded-xl bg-red-50 px-4 py-3 text-xs font-semibold text-red-600" role="alert">
              {retryError}
            </p>
          )}
        </div>

        <div className="space-y-2.5">
          {shipmentId && (
            <button
              type="button"
              onClick={handleRetryPayment}
              disabled={retrying}
              className="w-full rounded-xl bg-emerald-600 px-4 py-3 text-sm font-bold text-white transition hover:bg-emerald-700 disabled:opacity-50"
            >
              {retrying ? '결제창 준비 중...' : '다시 결제하기'}
            </button>
          )}
          <Link
            className="block rounded-xl border border-gray-200 bg-white px-4 py-3 text-center text-sm font-bold text-gray-700 hover:bg-gray-50"
            to={shipmentId ? `/customer/shipments/${shipmentId}` : '/customer/shipments'}
          >
            내 배송 내역 확인
          </Link>
        </div>
      </div>
    </section>
  )
}

export default PaymentFailPage