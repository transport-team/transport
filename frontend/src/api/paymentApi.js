import jwtAxios from '../util/jwtUtil'

export const confirmPayment = async ({ paymentKey, orderId, amount }) => {
  const response = await jwtAxios.post('/api/payments/confirm', {
    paymentKey,
    orderId,
    amount: Number(amount),
  })
  return response.data
}

export const cancelPayment = async (shipmentId, cancelReason) => {
  const response = await jwtAxios.post(`/api/payments/${shipmentId}/cancel`, {
    cancelReason,
  })
  return response.data
}

export const retryPayment = async (shipmentId) => {
  const response = await jwtAxios.post(`/api/payments/${shipmentId}/retry`)
  return response.data
}