import jwtAxios from '../util/jwtUtil'

export const confirmPayment = async ({ paymentKey, orderId, amount }) => {
  const response = await jwtAxios.post('/api/payments/confirm', {
    paymentKey,
    orderId,
    amount: Number(amount),
  })
  return response.data
}
