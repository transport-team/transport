import { useState } from 'react'
import { Link } from 'react-router-dom'
import { estimateFare } from '../../api/fareApi'
import { Button, Input } from '../../components/common'

const initialForm = {
  vehicleTypeId: '',
  cargoTypeId: '',
  originAddress: '',
  destAddress: '',
  cargoName: '',
  cargoWeightKg: '',
  pickupAt: '',
  receiverName: '',
  receiverPhone: '',
  memo: '',
}

const getErrorMessage = (error) =>
  error.response?.data?.message || '운임 계산 중 오류가 발생했습니다. 입력값을 확인해 주세요.'

const ShipmentCreatePage = () => {
  const [form, setForm] = useState(initialForm)
  const [fare, setFare] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleChange = ({ target }) => {
    setForm((current) => ({ ...current, [target.name]: target.value }))
    setFare(null)
    setError('')
  }

  const handleEstimate = async (event) => {
    event.preventDefault()
    setLoading(true)
    setFare(null)
    setError('')

    try {
      const result = await estimateFare({
        ...form,
        vehicleTypeId: Number(form.vehicleTypeId),
        cargoTypeId: Number(form.cargoTypeId),
        cargoWeightKg: Number(form.cargoWeightKg),
      })
      setFare(result)
    } catch (requestError) {
      setError(getErrorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="max-w-3xl space-y-6">
      <div>
        <Link className="text-sm text-primary hover:underline" to="/customer">
          고객 홈으로
        </Link>
        <h1 className="mt-2 text-2xl font-bold text-text">배송 신청</h1>
        <p className="mt-1 text-sm text-text-muted">
          배송 정보를 입력한 뒤 예상 운임을 확인하세요.
        </p>
      </div>

      <form className="space-y-6 rounded-xl border border-border bg-surface p-6 shadow-sm" onSubmit={handleEstimate}>
        <fieldset className="grid gap-4 md:grid-cols-2">
          <legend className="mb-2 text-lg font-semibold">배송 경로</legend>
          <Input
            id="originAddress"
            label="출발지 주소"
            name="originAddress"
            value={form.originAddress}
            onChange={handleChange}
            placeholder="출발지 주소를 입력하세요"
            required
          />
          <Input
            id="destAddress"
            label="도착지 주소"
            name="destAddress"
            value={form.destAddress}
            onChange={handleChange}
            placeholder="도착지 주소를 입력하세요"
            required
          />
        </fieldset>

        <fieldset className="grid gap-4 md:grid-cols-2">
          <legend className="mb-2 text-lg font-semibold">화물 정보</legend>
          <Input
            id="vehicleTypeId"
            label="차종 ID"
            name="vehicleTypeId"
            type="number"
            min="1"
            value={form.vehicleTypeId}
            onChange={handleChange}
            placeholder="예: 1"
            required
          />
          <Input
            id="cargoTypeId"
            label="화물 유형 ID"
            name="cargoTypeId"
            type="number"
            min="1"
            value={form.cargoTypeId}
            onChange={handleChange}
            placeholder="예: 1"
            required
          />
          <Input
            id="cargoName"
            label="화물명"
            name="cargoName"
            value={form.cargoName}
            onChange={handleChange}
            placeholder="예: 사무실 의자"
            required
          />
          <Input
            id="cargoWeightKg"
            label="화물 무게 (kg)"
            name="cargoWeightKg"
            type="number"
            min="1"
            value={form.cargoWeightKg}
            onChange={handleChange}
            placeholder="예: 600"
            required
          />
          <Input
            id="pickupAt"
            label="희망 상차 일시"
            name="pickupAt"
            type="datetime-local"
            value={form.pickupAt}
            onChange={handleChange}
            required
          />
        </fieldset>

        <fieldset className="grid gap-4 md:grid-cols-2">
          <legend className="mb-2 text-lg font-semibold">수령인 정보</legend>
          <Input
            id="receiverName"
            label="수령인 이름"
            name="receiverName"
            value={form.receiverName}
            onChange={handleChange}
            placeholder="수령인 이름"
            required
          />
          <Input
            id="receiverPhone"
            label="수령인 연락처"
            name="receiverPhone"
            value={form.receiverPhone}
            onChange={handleChange}
            placeholder="010-0000-0000"
            required
          />
          <label className="text-sm font-medium md:col-span-2" htmlFor="memo">
            <span className="mb-1 block">요청사항</span>
            <textarea
              id="memo"
              name="memo"
              value={form.memo}
              onChange={handleChange}
              maxLength={500}
              className="min-h-24 w-full rounded-lg border border-border bg-surface px-3 py-2 text-text focus:border-primary focus:ring-2 focus:ring-primary-light"
              placeholder="기사님께 전달할 요청사항을 입력하세요."
            />
          </label>
        </fieldset>

        {error && <p className="text-sm text-danger" role="alert">{error}</p>}

        <Button type="submit" loading={loading} className="w-full">
          예상 운임 계산
        </Button>
      </form>

      {fare && (
        <section className="rounded-xl border border-primary bg-primary-light p-6" aria-live="polite">
          <h2 className="text-lg font-semibold">예상 운임</h2>
          <p className="mt-2 text-3xl font-bold">{fare.totalFare.toLocaleString()}원</p>
          <dl className="mt-4 grid gap-2 text-sm md:grid-cols-2">
            <div><dt className="inline text-text-muted">거리: </dt><dd className="inline">{fare.distanceKm}km</dd></div>
            <div><dt className="inline text-text-muted">기본 운임: </dt><dd className="inline">{fare.baseFare.toLocaleString()}원</dd></div>
            <div><dt className="inline text-text-muted">거리 추가: </dt><dd className="inline">{fare.distanceFare.toLocaleString()}원</dd></div>
            <div><dt className="inline text-text-muted">화물 추가: </dt><dd className="inline">{fare.cargoFare.toLocaleString()}원</dd></div>
            <div><dt className="inline text-text-muted">할증: </dt><dd className="inline">{fare.surchargeFare.toLocaleString()}원</dd></div>
          </dl>
          {fare.inquiryRequired && (
            <p className="mt-4 text-sm text-danger">적재 중량 초과로 별도 문의가 필요합니다.</p>
          )}
        </section>
      )}
    </section>
  )
}

export default ShipmentCreatePage
