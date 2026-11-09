import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getCargoTypes } from '../../api/cargoApi'
import { getDefaultAddress } from '../../api/addressApi'
import { estimateFare } from '../../api/fareApi'
import { getMyInfo } from '../../api/memberApi'
import { createShipment } from '../../api/shipmentApi'
import { getVehicleTypes } from '../../api/vehicleApi'
import { Button, Input } from '../../components/common'
import { CARGO_CATEGORIES } from '../../util/cargoCatalog'
import { formatPhone, PHONE_PATTERN } from '../../util/formatUtil'

const initialForm = {
  vehicleTypeId: '',
  cargoTypeId: '',
  originAddress: '',
  originDetail: '',
  destAddress: '',
  destDetail: '',
  cargoName: '',
  cargoWeightKg: '',
  pickupAt: '',
  receiverName: '',
  receiverPhone: '',
  memo: '',
}

const createCargoItem = () => ({
  category: '',
  detail: '',
  quantity: 1,
})

const getErrorMessage = (error) =>
  error.response?.data?.message || '운임 계산 중 오류가 발생했습니다. 입력값을 확인해 주세요.'

const POSTCODE_SCRIPT_ID = 'daum-postcode-script'
const POSTCODE_SCRIPT_URL = 'https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js'
const ShipmentCreatePage = () => {
  const [form, setForm] = useState(initialForm)
  const [defaultAddress, setDefaultAddress] = useState(null)
  const [cargoItems, setCargoItems] = useState([createCargoItem()])
  const [weightManuallyEdited, setWeightManuallyEdited] = useState(false)
  const [vehicleTypes, setVehicleTypes] = useState([])
  const [cargoTypes, setCargoTypes] = useState([])
  const [fare, setFare] = useState(null)
  const [shipment, setShipment] = useState(null)
  const [error, setError] = useState('')
  const [vehicleTypeError, setVehicleTypeError] = useState('')
  const [cargoTypeError, setCargoTypeError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!window.daum?.Postcode) {
      const existingScript = document.getElementById(POSTCODE_SCRIPT_ID)
      const script = existingScript || document.createElement('script')

      if (!existingScript) {
        script.id = POSTCODE_SCRIPT_ID
        script.src = POSTCODE_SCRIPT_URL
        script.async = true
        document.head.appendChild(script)
      }
    }

    getVehicleTypes()
      .then(setVehicleTypes)
      .catch(() => setVehicleTypeError('차종 목록을 불러오지 못했습니다.'))
    getCargoTypes()
      .then(setCargoTypes)
      .catch(() => setCargoTypeError('화물 유형 목록을 불러오지 못했습니다.'))
    getMyInfo()
      .then((member) => {
        setForm((current) => ({
          ...current,
          receiverPhone: current.receiverPhone || formatPhone(member.phone || ''),
        }))
      })
      .catch(() => {})
    getDefaultAddress()
      .then(setDefaultAddress)
      .catch(() => setDefaultAddress(null))
  }, [])

  const selectDefaultAddress = () => {
    if (!defaultAddress) return
    setForm((current) => ({
      ...current,
      originAddress: defaultAddress.originAddress,
      originDetail: defaultAddress.originDetail,
      destAddress: defaultAddress.destAddress,
      destDetail: defaultAddress.destDetail,
    }))
    setFare(null)
    setShipment(null)
    setError('')
  }

  const openPostcode = (field) => {
    if (!window.daum?.Postcode) {
      setError('주소 검색 서비스를 불러오는 중입니다. 잠시 후 다시 시도해 주세요.')
      return
    }

    new window.daum.Postcode({
      oncomplete: (data) => {
        setForm((current) => ({
          ...current,
          [field]: data.roadAddress || data.jibunAddress,
        }))
        setFare(null)
        setShipment(null)
        setError('')
      },
    }).open()
  }

  const handleChange = ({ target }) => {
    setForm((current) => ({ ...current, [target.name]: target.value }))
    setFare(null)
    setShipment(null)
    setError('')
  }

  const getEstimatedWeight = (item) => {
    const detail = CARGO_CATEGORIES[item.category]?.find((option) => option.name === item.detail)
    return detail ? detail.defaultWeight * Number(item.quantity || 0) : 0
  }

  const estimatedTotalWeight = cargoItems.reduce(
    (total, item) => total + getEstimatedWeight(item),
    0,
  )

  const updateCargoItem = (index, field, value) => {
    const nextItems = cargoItems.map((item, itemIndex) => {
      if (itemIndex !== index) return item
      if (field === 'category') return { ...item, category: value, detail: '' }
      return { ...item, [field]: field === 'quantity' ? Math.max(1, Number(value)) : value }
    })
    const nextTotalWeight = nextItems.reduce(
      (total, item) => total + getEstimatedWeight(item),
      0,
    )
    setCargoItems(nextItems)
    setFare(null)
    setShipment(null)
    setError('')
    if (!weightManuallyEdited) {
      setForm((current) => ({ ...current, cargoWeightKg: String(nextTotalWeight) }))
    }
  }

  const handleCargoQuantityWheel = (event, index) => {
    if (event.deltaY === 0) return
    event.preventDefault()
    event.stopPropagation()
    const item = cargoItems[index]
    updateCargoItem(index, 'quantity', Number(item.quantity) + (event.deltaY < 0 ? 1 : -1))
  }

  const addCargoItem = () => {
    setCargoItems((current) => [...current, createCargoItem()])
  }

  const removeCargoItem = (index) => {
    const nextItems = cargoItems.filter((_, itemIndex) => itemIndex !== index)
    setCargoItems(nextItems)
    setFare(null)
    setShipment(null)
    if (!weightManuallyEdited) {
      const nextTotalWeight = nextItems.reduce(
        (total, item) => total + getEstimatedWeight(item),
        0,
      )
      setForm((current) => ({ ...current, cargoWeightKg: String(nextTotalWeight) }))
    }
  }

  const handleWeightChange = (event) => {
    setWeightManuallyEdited(true)
    handleChange(event)
  }

  const handleReceiverPhoneChange = (event) => {
    setForm((current) => ({ ...current, receiverPhone: formatPhone(event.target.value) }))
    setFare(null)
    setShipment(null)
    setError('')
  }

  const getShipmentRequest = () => ({
    ...form,
    cargoName: [
      cargoItems
        .filter((item) => item.detail)
        .map((item) => `${item.detail} ${item.quantity}개`)
        .join(', '),
      form.cargoName.trim(),
    ].filter(Boolean).join(', '),
    vehicleTypeId: Number(form.vehicleTypeId),
    cargoTypeId: Number(form.cargoTypeId),
    cargoWeightKg: Number(form.cargoWeightKg),
  })

  const handleEstimate = async (event) => {
    event.preventDefault()
    setLoading(true)
    setFare(null)
    setError('')

    try {
      const request = getShipmentRequest()
      const result = await estimateFare(request)
      setFare(result)
    } catch (requestError) {
      setError(getErrorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }

  const handleCreateShipment = async () => {
    setLoading(true)
    setError('')

    try {
      const result = await createShipment(getShipmentRequest())
      setShipment(result)
    } catch (requestError) {
      setError(requestError.response?.data?.message || '배송 신청 중 오류가 발생했습니다.')
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
          <label className="block text-sm font-medium text-text md:col-span-2" htmlFor="default-address">
            저장된 기본 배송지
            <select
              id="default-address"
              value=""
              onChange={selectDefaultAddress}
              className="mt-1 w-full rounded-lg border border-border bg-surface px-3 py-2 text-text"
            >
              <option value="">
                {defaultAddress ? `${defaultAddress.label} 선택` : '내 정보에서 기본 배송지를 먼저 설정하세요'}
              </option>
              {defaultAddress && <option value={defaultAddress.label}>{defaultAddress.label}</option>}
            </select>
          </label>
          <div className="space-y-2">
            <label className="block text-sm font-medium text-text" htmlFor="originAddress">
              출발지 주소
            </label>
            <div className="flex gap-2">
              <input
                id="originAddress"
                name="originAddress"
                value={form.originAddress}
                readOnly
                placeholder="주소 검색을 눌러 선택하세요"
                className="min-w-0 flex-1 rounded-lg border border-border bg-surface px-3 py-2 text-text"
                required
              />
              <Button type="button" variant="outline" onClick={() => openPostcode('originAddress')}>
                주소 검색
              </Button>
            </div>
            <Input
              id="originDetail"
              name="originDetail"
              value={form.originDetail}
              onChange={handleChange}
              placeholder="상세주소 (동, 층, 호수 등)"
            />
          </div>
          <div className="space-y-2">
            <label className="block text-sm font-medium text-text" htmlFor="destAddress">
              도착지 주소
            </label>
            <div className="flex gap-2">
              <input
                id="destAddress"
                name="destAddress"
                value={form.destAddress}
                readOnly
                placeholder="주소 검색을 눌러 선택하세요"
                className="min-w-0 flex-1 rounded-lg border border-border bg-surface px-3 py-2 text-text"
                required
              />
              <Button type="button" variant="outline" onClick={() => openPostcode('destAddress')}>
                주소 검색
              </Button>
            </div>
            <Input
              id="destDetail"
              name="destDetail"
              value={form.destDetail}
              onChange={handleChange}
              placeholder="상세주소 (동, 층, 호수 등)"
            />
          </div>
        </fieldset>

        <fieldset className="grid gap-4 md:grid-cols-2">
          <legend className="mb-2 text-lg font-semibold">화물 정보</legend>
          <label className="block text-sm font-medium text-text" htmlFor="vehicleTypeId">
            <span className="mb-1 block">차종</span>
            <select
              id="vehicleTypeId"
              name="vehicleTypeId"
              value={form.vehicleTypeId}
              onChange={handleChange}
              className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-text focus:border-primary focus:ring-2 focus:ring-primary-light"
              required
              disabled={vehicleTypes.length === 0}
            >
              <option value="">차종을 선택하세요</option>
              {vehicleTypes.filter((vehicleType) => vehicleType.active).map((vehicleType) => (
                <option key={vehicleType.id} value={vehicleType.id}>
                  {vehicleType.name} (최대 {vehicleType.maxLoadKg.toLocaleString()}kg)
                </option>
              ))}
            </select>
            {vehicleTypeError && <span className="mt-1 block text-sm text-danger">{vehicleTypeError}</span>}
          </label>
          <label className="block text-sm font-medium text-text" htmlFor="cargoTypeId">
            <span className="mb-1 block">화물 유형</span>
            <select
              id="cargoTypeId"
              name="cargoTypeId"
              value={form.cargoTypeId}
              onChange={handleChange}
              className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-text focus:border-primary focus:ring-2 focus:ring-primary-light"
              required
            >
              <option value="">화물 유형을 선택하세요</option>
              {cargoTypes.filter((cargoType) => cargoType.active).map((cargoType) => (
                <option key={cargoType.id} value={cargoType.id}>{cargoType.name}</option>
              ))}
            </select>
            {cargoTypeError && <span className="mt-1 block text-sm text-danger">{cargoTypeError}</span>}
          </label>
          <div className="space-y-3 md:col-span-2">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-text">화물 항목</span>
              <Button type="button" variant="outline" size="sm" onClick={addCargoItem}>
                화물 추가
              </Button>
            </div>
            {cargoItems.map((item, index) => (
              <div key={`cargo-item-${index}`} className="grid gap-3 rounded-lg border border-border p-3 md:grid-cols-[1fr_1fr_7rem_auto]">
                <label className="text-sm font-medium text-text" htmlFor={`cargo-category-${index}`}>
                  <span className="mb-1 block">카테고리</span>
                  <select
                    id={`cargo-category-${index}`}
                    value={item.category}
                    onChange={(event) => updateCargoItem(index, 'category', event.target.value)}
                    className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-text"
                    required
                  >
                    <option value="">카테고리 선택</option>
                    {Object.keys(CARGO_CATEGORIES).map((category) => (
                      <option key={category} value={category}>{category}</option>
                    ))}
                  </select>
                </label>
                <label className="text-sm font-medium text-text" htmlFor={`cargo-detail-${index}`}>
                  <span className="mb-1 block">세부 품목</span>
                  <select
                    id={`cargo-detail-${index}`}
                    value={item.detail}
                    onChange={(event) => updateCargoItem(index, 'detail', event.target.value)}
                    className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-text"
                    disabled={!item.category}
                    required
                  >
                    <option value="">품목 선택</option>
                    {(CARGO_CATEGORIES[item.category] || []).map((detail) => (
                      <option key={detail.name} value={detail.name}>{detail.name} (약 {detail.defaultWeight}kg)</option>
                    ))}
                  </select>
                </label>
                <label className="text-sm font-medium text-text" htmlFor={`cargo-quantity-${index}`}>
                  <span className="mb-1 block">수량</span>
                  <input
                    id={`cargo-quantity-${index}`}
                    type="number"
                    min="1"
                    step="1"
                    value={item.quantity}
                    onChange={(event) => updateCargoItem(index, 'quantity', event.target.value)}
                    onWheelCapture={(event) => handleCargoQuantityWheel(event, index)}
                    className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-text"
                    required
                  />
                </label>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => removeCargoItem(index)}
                  disabled={cargoItems.length === 1}
                >
                  삭제
                </Button>
              </div>
            ))}
          </div>
          <Input
            id="cargoName"
            label="기타 물품명"
            name="cargoName"
            value={form.cargoName}
            onChange={handleChange}
            placeholder="포함되지 않은 기타 물품을 작성하세요."
          />
          <Input
            id="cargoWeightKg"
            label="화물 무게 (kg)"
            name="cargoWeightKg"
            type="number"
            min="1"
            value={form.cargoWeightKg}
            onChange={handleWeightChange}
            placeholder={estimatedTotalWeight ? String(estimatedTotalWeight) : '예: 600'}
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
            onChange={handleReceiverPhoneChange}
            pattern={PHONE_PATTERN.source}
            inputMode="numeric"
            placeholder="내 연락처가 자동 입력됩니다."
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
              placeholder="물품명을 상세히 기재해 주세요."
            />
          </label>
        </fieldset>

        <p className="rounded-lg border border-info bg-info-light p-4 text-sm text-info-text" aria-live="polite">
          예상 총 무게: <strong>{estimatedTotalWeight.toLocaleString()}kg</strong>
          {!weightManuallyEdited && estimatedTotalWeight > 0 && ' (화물 무게에 자동 반영됨)'}
        </p>

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
            {!fare.inquiryRequired && !shipment && (
              <Button type="button" loading={loading} className="mt-6 w-full" onClick={handleCreateShipment}>
                배송 신청하기
              </Button>
            )}
        </section>
      )}
      {shipment && (
        <section className="rounded-xl border border-success bg-success-light p-6" aria-live="polite">
            <h2 className="text-lg font-semibold text-success-text">배송 신청이 완료되었습니다.</h2>
            <dl className="mt-4 grid gap-2 text-sm md:grid-cols-2">
              <div><dt className="inline text-text-muted">배송 ID: </dt><dd className="inline">{shipment.shipmentId}</dd></div>
              <div><dt className="inline text-text-muted">상태: </dt><dd className="inline">{shipment.status || 'PENDING_PAYMENT'}</dd></div>
              <div><dt className="inline text-text-muted">결제 대기 금액: </dt><dd className="inline font-semibold">{shipment.amount.toLocaleString()}원</dd></div>
              <div><dt className="inline text-text-muted">주문번호: </dt><dd className="inline break-all">{shipment.orderId}</dd></div>
            </dl>
            <p className="mt-4 text-sm text-success-text">결제를 완료하면 배송 접수가 확정됩니다.</p>
        </section>
      )}
    </section>
  )
}

export default ShipmentCreatePage
