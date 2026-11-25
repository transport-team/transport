import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { getCargoTypes } from '../../api/cargoApi'
import { getSavedAddresses } from '../../api/addressApi'
import { estimateFare } from '../../api/fareApi'
import { getMyInfo } from '../../api/memberApi'
import { createShipment } from '../../api/shipmentApi'
import { requestTossPayment } from '../../util/tossPayments'
import { getVehicleTypes } from '../../api/vehicleApi'
import { Input } from '../../components/common'
import { CARGO_CATEGORIES } from '../../util/cargoCatalog'
import { formatPhone, PHONE_PATTERN } from '../../util/formatUtil'
import { loadKakaoMapSdk } from '../../util/kakaoMapLoader'

const initialForm = {
  vehicleTypeId: '', cargoTypeId: '', originAddress: '', originDetail: '',
  destAddress: '', destDetail: '', cargoName: '', cargoWeightKg: '',
  pickupAt: '', senderName: '', senderPhone: '', receiverName: '', receiverPhone: '', memo: '',
}
const createCargoItem = () => ({ category: '', detail: '', quantity: 1 })
const POSTCODE_SCRIPT_ID = 'daum-postcode-script'
const POSTCODE_SCRIPT_URL = 'https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js'
const getLocalDateTimeValue = () => {
  const date = new Date()
  const offset = date.getTimezoneOffset() * 60000
  return new Date(date.getTime() - offset).toISOString().slice(0, 16)
}
const getRecommendedCargoTypeId = (items, types) => {
  if (!types || types.length === 0) return ''
  const categories = items.map((item) => item.category).join(' ')
  let matchedType

  if (categories.includes('냉장') || categories.includes('음식')) {
    matchedType = types.find((type) => type.name.includes('냉동') || type.name.includes('냉장') || type.name.includes('탑차'))
  } else if (categories.includes('가구') || categories.includes('가전')) {
    matchedType = types.find((type) => type.name.includes('카고') || type.name.includes('윙바디'))
  } else {
    matchedType = types.find((type) => type.name.includes('카고') || type.name.includes('일반'))
  }

  return matchedType ? String(matchedType.id) : ''
}

const ShipmentCreatePage = () => {
  const [form, setForm] = useState(initialForm)
  const [savedAddresses, setSavedAddresses] = useState([])
  const [selectedSavedAddress, setSelectedSavedAddress] = useState({ origin: '', dest: '' })
  const [cargoItems, setCargoItems] = useState([createCargoItem()])
  const [weightManuallyEdited, setWeightManuallyEdited] = useState(false)
  const [cargoTypeManuallyEdited, setCargoTypeManuallyEdited] = useState(false)
  const [vehicleTypes, setVehicleTypes] = useState([])
  const [cargoTypes, setCargoTypes] = useState([])
  const [activeCargoCategory, setActiveCargoCategory] = useState(Object.keys(CARGO_CATEGORIES)[0])
  const [vehicleWeightWarning, setVehicleWeightWarning] = useState('')
  const autoChangedVehicleId = useRef(null)
  const [inputStep, setInputStep] = useState(1)
  const [fare, setFare] = useState(null)
  const [shipment, setShipment] = useState(null)
  const [error, setError] = useState('')
  const [vehicleTypeError, setVehicleTypeError] = useState('')
  const [cargoTypeError, setCargoTypeError] = useState('')
  const [loading, setLoading] = useState(false)
  const [originCoords, setOriginCoords] = useState(null)
  const [destCoords, setDestCoords] = useState(null)
  const [mapNotice, setMapNotice] = useState('')
  const mapContainerRef = useRef(null)
  const mapInstanceRef = useRef(null)
  const originMarkerRef = useRef(null)
  const destMarkerRef = useRef(null)

  const trimmedOriginAddress = form.originAddress?.trim() || ''
  const trimmedDestAddress = form.destAddress?.trim() || ''
  const hasAddressInput = Boolean(trimmedOriginAddress || trimmedDestAddress)
  const effectiveOriginCoords = hasAddressInput ? originCoords : null
  const effectiveDestCoords = hasAddressInput ? destCoords : null
  const effectiveMapNotice = hasAddressInput ? mapNotice : ''

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
    getVehicleTypes().then(setVehicleTypes).catch(() => setVehicleTypeError('차종 목록을 불러오지 못했습니다.'))
    getCargoTypes().then(setCargoTypes).catch(() => setCargoTypeError('화물 유형을 불러오지 못했습니다.'))

    getMyInfo().then((member) => setForm((current) => ({
      ...current,
      senderName: current.senderName || member.name || '',
      senderPhone: current.senderPhone || formatPhone(member.phone || ''),
      receiverPhone: current.receiverPhone || formatPhone(member.phone || ''),
    }))).catch(() => {})

    getSavedAddresses().then((items) => setSavedAddresses(Array.isArray(items) ? items : []))
      .catch(() => setSavedAddresses([]))
  }, [])

  const clearEstimate = () => {
    setFare(null)
    setShipment(null)
    setError('')
  }

  const handleChange = ({ target }) => {
    setForm((current) => ({ ...current, [target.name]: target.value }))

    if (target.name === 'cargoTypeId') {
      setCargoTypeManuallyEdited(true)
    }

    if (['originAddress', 'originDetail'].includes(target.name)) {
      setSelectedSavedAddress((current) => ({ ...current, origin: '' }))
    } else if (['destAddress', 'destDetail'].includes(target.name)) {
      setSelectedSavedAddress((current) => ({ ...current, dest: '' }))
    }
    clearEstimate()
  }

  const selectSavedAddress = (field, addressId) => {
    const prefix = field === 'origin' ? 'origin' : 'dest'
    if (!addressId) {
      setForm((current) => ({ ...current, [`${prefix}Address`]: '', [`${prefix}Detail`]: '' }))
      setSelectedSavedAddress((current) => ({ ...current, [field]: '' }))
      clearEstimate()
      return
    }
    const address = savedAddresses.find((item) => String(item.id) === addressId)
    if (!address) return
    setForm((current) => ({
      ...current,
      [`${prefix}Address`]: address.address,
      [`${prefix}Detail`]: address.detail || '',
    }))
    setSelectedSavedAddress((current) => ({ ...current, [field]: addressId }))
    clearEstimate()
  }

  const openPostcode = (field) => {
    if (!window.daum?.Postcode) {
      setError('주소 검색 서비스를 불러오는 중입니다. 잠시 후 다시 시도해 주세요.')
      return
    }
    new window.daum.Postcode({
      oncomplete: (data) => {
        setForm((current) => ({ ...current, [field]: data.roadAddress || data.jibunAddress }))
        setSelectedSavedAddress((current) => ({ ...current, [field === 'originAddress' ? 'origin' : 'dest']: '' }))
        clearEstimate()
      },
    }).open()
  }

  const getEstimatedWeight = (item) => {
    const detail = CARGO_CATEGORIES[item.category]?.find((option) => option.name === item.detail)
    return detail ? detail.defaultWeight * Number(item.quantity || 0) : 0
  }
  const estimatedTotalWeight = cargoItems.reduce((total, item) => total + getEstimatedWeight(item), 0)
  
  const recommendedCargoTypeId = getRecommendedCargoTypeId(cargoItems, cargoTypes)
  const effectiveCargoTypeId = cargoTypeManuallyEdited ? form.cargoTypeId : (recommendedCargoTypeId || form.cargoTypeId)

  useEffect(() => {
    const timer = window.setTimeout(() => {
      const weight = Number(form.cargoWeightKg) || estimatedTotalWeight
      const activeVehicles = vehicleTypes
        .filter((vehicleType) => vehicleType.active)
        .sort((left, right) => left.maxLoadKg - right.maxLoadKg)

      if (!weight || activeVehicles.length === 0) {
        setVehicleWeightWarning('')
        return
      }

      const suitableVehicle = activeVehicles.find((vehicleType) => vehicleType.maxLoadKg >= weight)
      if (!suitableVehicle) {
        setVehicleWeightWarning('총 무게가 5,000kg을 초과해 현재 배송 서비스로 신청할 수 없습니다.')
        if (form.vehicleTypeId) setForm((current) => ({ ...current, vehicleTypeId: '' }))
        return
      }

      const selectedVehicle = activeVehicles.find((v) => String(v.id) === String(form.vehicleTypeId))
      if (selectedVehicle && selectedVehicle.maxLoadKg >= weight) {
        if (autoChangedVehicleId.current !== selectedVehicle.id) setVehicleWeightWarning('')
        return
      }

      autoChangedVehicleId.current = suitableVehicle.id
      setForm((current) => ({ ...current, vehicleTypeId: String(suitableVehicle.id) }))
      setVehicleWeightWarning(`총 무게 ${weight.toLocaleString()}kg에 맞춰 ${suitableVehicle.name} 차종으로 변경했습니다.`)
    }, 0)

    return () => window.clearTimeout(timer)
  }, [estimatedTotalWeight, form.cargoWeightKg, form.vehicleTypeId, vehicleTypes])

  useEffect(() => {
    if (!hasAddressInput) {
      return
    }

    let cancelled = false

    loadKakaoMapSdk()
      .then((maps) => {
        if (cancelled) return
        const geocoder = new maps.services.Geocoder()

        const geocodeAddress = (address) =>
          new Promise((resolve) => {
            if (!address) {
              resolve(null)
              return
            }
            geocoder.addressSearch(address, (result, status) => {
              if (status === maps.services.Status.OK && result?.[0]) {
                resolve({ lat: Number(result[0].y), lng: Number(result[0].x) })
              } else {
                resolve(null)
              }
            })
          })

        Promise.all([
          geocodeAddress(trimmedOriginAddress),
          geocodeAddress(trimmedDestAddress),
        ]).then(([nextOrigin, nextDest]) => {
          if (cancelled) return
          setOriginCoords(nextOrigin)
          setDestCoords(nextDest)
          if ((trimmedOriginAddress && !nextOrigin) || (trimmedDestAddress && !nextDest)) {
            setMapNotice('일부 주소의 좌표를 찾지 못해 마커를 표시할 수 없습니다.')
          } else {
            setMapNotice('')
          }
        })
      })
      .catch(() => {
        if (!cancelled) {
          setMapNotice('지도를 불러올 수 없어 주소 정보만 표시합니다.')
        }
      })

    return () => {
      cancelled = true
    }
  }, [hasAddressInput, trimmedOriginAddress, trimmedDestAddress])

  useEffect(() => {
    const hasCoords = Boolean(effectiveOriginCoords || effectiveDestCoords)
    if (!hasCoords || !mapContainerRef.current || !window.kakao?.maps) return

    const maps = window.kakao.maps
    const initialCenter = effectiveOriginCoords || effectiveDestCoords
    const latLngOrigin = effectiveOriginCoords ? new maps.LatLng(effectiveOriginCoords.lat, effectiveOriginCoords.lng) : null
    const latLngDest = effectiveDestCoords ? new maps.LatLng(effectiveDestCoords.lat, effectiveDestCoords.lng) : null

    if (!mapInstanceRef.current) {
      mapInstanceRef.current = new maps.Map(mapContainerRef.current, {
        center: new maps.LatLng(initialCenter.lat, initialCenter.lng),
        level: 5,
      })
    }

    const map = mapInstanceRef.current

    if (originMarkerRef.current) {
      originMarkerRef.current.setMap(null)
      originMarkerRef.current = null
    }
    if (destMarkerRef.current) {
      destMarkerRef.current.setMap(null)
      destMarkerRef.current = null
    }

    if (latLngOrigin) {
      originMarkerRef.current = new maps.Marker({
        map,
        position: latLngOrigin,
        title: '출발',
      })
    }

    if (latLngDest) {
      destMarkerRef.current = new maps.Marker({
        map,
        position: latLngDest,
        title: '도착',
      })
    }

    if (latLngOrigin && latLngDest) {
      const bounds = new maps.LatLngBounds()
      bounds.extend(latLngOrigin)
      bounds.extend(latLngDest)
      map.setBounds(bounds, 60, 60, 60, 60)
    } else if (latLngOrigin) {
      map.setCenter(latLngOrigin)
      map.setLevel(4)
    } else if (latLngDest) {
      map.setCenter(latLngDest)
      map.setLevel(4)
    }

    return () => {
      if (originMarkerRef.current) {
        originMarkerRef.current.setMap(null)
        originMarkerRef.current = null
      }
      if (destMarkerRef.current) {
        destMarkerRef.current.setMap(null)
        destMarkerRef.current = null
      }
      mapInstanceRef.current = null
    }
  }, [effectiveOriginCoords, effectiveDestCoords, inputStep])

  const syncCargoAndWeightState = (nextItems) => {
    const hasAnySelectedItem = nextItems.some((item) => item.detail)
    const nextEstimatedWeight = nextItems.reduce((total, item) => total + getEstimatedWeight(item), 0)

    if (!hasAnySelectedItem) {
      setCargoTypeManuallyEdited(false)
      setWeightManuallyEdited(false)
      setForm((current) => ({
        ...current,
        cargoTypeId: '',
        cargoWeightKg: '',
      }))
      return
    }

    if (!weightManuallyEdited) {
      setForm((current) => ({
        ...current,
        cargoWeightKg: String(nextEstimatedWeight),
      }))
    }
  }

  const updateCargoItem = (index, field, value) => {
    const nextItems = cargoItems.map((item, itemIndex) => {
      if (itemIndex !== index) return item
      if (field === 'category') return { ...item, category: value, detail: '' }
      if (field === 'quantity') {
        const parsedQty = Number(value)
        const safeQty = Number.isFinite(parsedQty) ? Math.max(1, Math.floor(parsedQty)) : 1
        return { ...item, quantity: safeQty }
      }
      return { ...item, [field]: value }
    })
    setCargoItems(nextItems)
    clearEstimate()
    syncCargoAndWeightState(nextItems)
  }

  const removeCargoItem = (index) => {
    const filtered = cargoItems.filter((_, itemIndex) => itemIndex !== index)
    const nextItems = filtered.length ? filtered : [createCargoItem()]
    setCargoItems(nextItems)
    clearEstimate()
    syncCargoAndWeightState(nextItems)
  }

  const toggleCargoDetail = (category, detail) => {
    const index = cargoItems.findIndex((item) => item.category === category && item.detail === detail)
    if (index >= 0) {
      removeCargoItem(index)
      return
    }
    const emptyIndex = cargoItems.findIndex((item) => !item.category && !item.detail)
    const nextItems = emptyIndex >= 0
      ? cargoItems.map((item, i) => (i === emptyIndex ? { ...item, category, detail, quantity: 1 } : item))
      : [...cargoItems, { category, detail, quantity: 1 }]
    setCargoItems(nextItems)
    clearEstimate()
    syncCargoAndWeightState(nextItems)
  }

  const handleWeightChange = (event) => {
    setWeightManuallyEdited(true)
    handleChange(event)
  }

  const handleReceiverPhoneChange = (event) => {
    setForm((current) => ({ ...current, [event.target.name]: formatPhone(event.target.value) }))
    clearEstimate()
  }

  const activeCargoTypeList = cargoTypes.filter((cargoType) => cargoType.active)
  const isValidCargoTypeSelected = activeCargoTypeList.some(
    (cargoType) => String(cargoType.id) === String(effectiveCargoTypeId)
  )

  const getShipmentRequest = () => {
    const selectedItemSummary = cargoItems
      .filter((item) => item.detail)
      .map((item) => `${item.detail} ${Math.max(1, Number(item.quantity) || 1)}개`)
      .join(', ')
    const customDescription = form.cargoName.trim()
    const combinedCargoName = [selectedItemSummary, customDescription]
      .filter(Boolean)
      .join(', ')
      .slice(0, 255)

    return {
      ...form,
      cargoName: combinedCargoName,
      vehicleTypeId: Number(form.vehicleTypeId),
      cargoTypeId: isValidCargoTypeSelected ? Number(effectiveCargoTypeId) : null,
      cargoWeightKg: Number(form.cargoWeightKg),
      senderName: form.senderName.trim(),
      senderPhone: form.senderPhone.trim(),
    }
  }

  const canMoveToTransport = Boolean(
    form.vehicleTypeId &&
    isValidCargoTypeSelected &&
    Number(form.cargoWeightKg) > 0 &&
    !vehicleWeightWarning.includes('신청할 수 없습니다')
  )

  const canEstimateFare = Boolean(
    form.originAddress && form.destAddress && form.senderName && form.senderPhone && form.receiverName && form.receiverPhone && form.pickupAt && form.pickupAt >= getLocalDateTimeValue()
  )

  const handleNextInputStep = (event) => {
    event?.preventDefault()
    if (!form.vehicleTypeId) {
      setError('차종을 선택해 주세요.')
      return
    }
    if (!isValidCargoTypeSelected) {
      setError('화물 유형을 선택해 주세요.')
      return
    }
    if (!(Number(form.cargoWeightKg) > 0)) {
      setError('총 무게(kg)를 1kg 이상 입력해 주세요.')
      return
    }
    if (vehicleWeightWarning.includes('신청할 수 없습니다')) {
      setError(vehicleWeightWarning)
      return
    }
    setError('')
    setInputStep(2)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const handleEstimate = async (event) => {
    event?.preventDefault()
    const request = getShipmentRequest()

    const requiredFields = [
      ['senderName', '보내는 분 이름'],
      ['senderPhone', '보내는 분 연락처'],
      ['originAddress', '출발지 주소'],
      ['destAddress', '도착지 주소'],
      ['receiverName', '받는 분 이름'],
      ['receiverPhone', '받는 분 연락처'],
      ['pickupAt', '상차 희망 일시'],
    ]
    const missingField = requiredFields.find(([field]) => !String(form[field] || '').trim())

    const selectedVehicleType = vehicleTypes.find(
      (vehicleType) => String(vehicleType.id) === String(request.vehicleTypeId)
    )
    if (selectedVehicleType && request.cargoWeightKg > selectedVehicleType.maxLoadKg) {
      setError(`선택한 ${selectedVehicleType.name} 차종의 최대 적재 중량(${selectedVehicleType.maxLoadKg.toLocaleString()}kg)을 초과했습니다.`)
      return
    }
    if (vehicleWeightWarning.includes('신청할 수 없습니다')) {
      setError(vehicleWeightWarning)
      return
    }
    if (missingField) {
      setError(`${missingField[1]}을(를) 입력해 주세요.`)
      return
    }
    if (!request.vehicleTypeId) {
      setError('차종을 선택해 주세요.')
      return
    }
    if (!request.cargoTypeId || !isValidCargoTypeSelected) {
      setError('화물 유형을 선택해 주세요.')
      return
    }
    if (!request.cargoName) {
      setError('화물 품목을 선택하거나 추가 설명(화물명)을 입력해 주세요.')
      return
    }
    if (!(request.cargoWeightKg >= 1)) {
      setError('화물 무게는 1kg 이상 입력해 주세요.')
      return
    }
    if (form.pickupAt < getLocalDateTimeValue()) {
      setError('상차 희망 일시는 현재 시각 이후로 선택해 주세요.')
      return
    }
    if (vehicleWeightWarning.includes('신청할 수 없습니다')) {
      setError(vehicleWeightWarning)
      return
    }

    setLoading(true)
    setFare(null)
    setError('')
    try {
      setFare(await estimateFare(request))
    } catch (requestError) {
      setError(requestError.response?.data?.message || '운임 계산 중 오류가 발생했습니다. 입력값을 확인해 주세요.')
    } finally {
      setLoading(false)
    }
  }

  const handleCreateShipment = async () => {
    const request = getShipmentRequest()
    if (!request.senderName || !request.senderPhone) {
      setError('보내는 분 정보가 누락되었습니다. 화물 정보 수정 버튼을 눌러 다시 확인해 주세요.')
      return
    }

    setLoading(true)
    setError('')
    try {
      const result = await createShipment(request)
      setShipment(result)
      await requestTossPayment({
        shipmentId: result.shipmentId,
        orderId: result.orderId,
        amount: result.amount,
        cargoName: request.cargoName,
      })
    } catch (requestError) {
      console.error("토스 결제창 호출 에러 상세:", requestError);
      const rawMessage = requestError.response?.data?.message || requestError.message || ''
      const isClosedByUser =
        /cancel|close|user_cancel/i.test(String(requestError.code || '')) ||
        /취소|닫|중단/i.test(rawMessage)

      if (isClosedByUser) {
        setError('결제창이 닫혀 결제가 완료되지 않았습니다. 배송 신청 내역에서 다시 결제할 수 있습니다. (30분 미결제 시 자동 취소)')
      } else {
        setError(requestError.response?.data?.message || '배송 신청 중 오류가 발생했습니다.')
      }
    } finally {
      setLoading(false)
    }
  }

  const activeCargoItems = CARGO_CATEGORIES[activeCargoCategory] || []
  const selectedVehicle = vehicleTypes.find((vehicleType) => String(vehicleType.id) === String(form.vehicleTypeId))
  const isDistanceOnlyFare = fare && fare.durationMin == null

  return (
    <section className="mx-auto min-h-screen w-full max-w-[480px] bg-white pb-32 shadow-lg md:max-w-[768px] lg:grid lg:max-w-[1200px] lg:grid-cols-[minmax(0,650px)_380px] lg:gap-8 lg:bg-transparent lg:pb-10 lg:shadow-none">

      <header className="sticky top-0 z-25 border-b border-gray-100 bg-white/95 px-5 py-4 backdrop-blur lg:col-span-2">
        <div className="flex items-center gap-3">
          <Link className="text-2xl leading-none text-gray-700" to="/customer" aria-label="고객 홈으로">←</Link>
          <h1 className="text-lg font-bold text-gray-900">배송 신청</h1>
        </div>
        <div className="mt-4 flex items-center gap-2 text-[11px] font-semibold">
          <span
            className={`cursor-pointer transition-colors ${inputStep === 1 ? 'text-emerald-600' : 'text-gray-400 hover:text-emerald-500'}`}
            onClick={() => { if (inputStep > 1) setInputStep(1) }}
          >
            1. 물건 입력
          </span>
          <span className="h-px flex-1 bg-emerald-200" />
          <span
            className={`cursor-pointer transition-colors ${inputStep === 2 && !fare ? 'text-emerald-600' : 'text-gray-400 hover:text-emerald-500'}`}
            onClick={() => { if (canMoveToTransport) setInputStep(2) }}
          >
            2. 배송 입력
          </span>
          <span className="h-px flex-1 bg-emerald-200" />
          <span className={fare && !shipment ? 'text-emerald-600' : 'text-gray-400'}>3. 운임 확인</span>
          <span className="h-px flex-1 bg-gray-200" />
          <span className={shipment ? 'text-emerald-600' : 'text-gray-400'}>4. 결제 대기</span>
        </div>
        <div className="mt-2 h-1 overflow-hidden rounded-full bg-gray-100">
          <div className={`h-full rounded-full bg-emerald-600 transition-all ${shipment ? 'w-full' : fare ? 'w-3/4' : inputStep === 2 ? 'w-1/2' : 'w-1/4'}`} />
        </div>
      </header>

      <form id="shipment-create-form" className="space-y-7 px-5 py-6 lg:rounded-2xl lg:bg-white lg:shadow-sm" onSubmit={inputStep === 1 ? handleNextInputStep : handleEstimate}>

        {inputStep === 1 && (
          <section className="space-y-10 animate-in fade-in duration-300">
            <div>
              <div className="mb-3 flex items-end justify-between">
                <div>
                  <p className="text-xs font-semibold text-emerald-600">VEHICLE</p>
                  <h2 className="mt-1 text-xl font-bold text-gray-900">차종을 골라주세요</h2>
                </div>
                {selectedVehicle && <span className="text-xs text-gray-500">최대 {selectedVehicle.maxLoadKg.toLocaleString()}kg</span>}
              </div>
              <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
                {vehicleTypes.filter((vehicleType) => vehicleType.active).map((vehicleType, index) => (
                  <button key={vehicleType.id} type="button" onClick={() => handleChange({ target: { name: 'vehicleTypeId', value: String(vehicleType.id) } })} className={`rounded-2xl border-2 p-4 text-left transition ${String(form.vehicleTypeId) === String(vehicleType.id) ? 'border-emerald-600 bg-emerald-50' : 'border-gray-100 bg-white'}`}>
                    <span className="text-3xl">{['🛵', '🚚', '🚛', '🚒'][index % 4]}</span>
                    <span className="mt-3 block text-sm font-bold text-gray-900">{vehicleType.name}</span>
                    <span className="mt-1 block text-xs text-gray-500">최대 {vehicleType.maxLoadKg.toLocaleString()}kg</span>
                  </button>
                ))}
              </div>
              {vehicleTypeError && <p className="mt-3 text-sm font-bold text-red-500" role="alert">{vehicleTypeError}</p>}
              {vehicleWeightWarning && <p className={`mt-3 rounded-xl px-4 py-3 text-sm font-semibold ${vehicleWeightWarning.includes('신청할 수 없습니다') ? 'bg-red-50 text-red-700' : 'bg-amber-50 text-amber-700'}`}>{vehicleWeightWarning}</p>}
            </div>

            <div>
              <div className="mb-3">
                <p className="text-xs font-semibold text-emerald-600">CARGO</p>
                <h2 className="mt-1 text-xl font-bold text-gray-900">무엇을 보내나요?</h2>
              </div>
              <div className="mb-4 flex gap-2 overflow-x-auto pb-1 no-scrollbar">
                {Object.keys(CARGO_CATEGORIES).map((category) => (
                  <button key={category} type="button" onClick={() => setActiveCargoCategory(category)} className={`shrink-0 rounded-full px-4 py-2 text-sm font-semibold ${activeCargoCategory === category ? 'bg-gray-900 text-white' : 'bg-gray-100 text-gray-500'}`}>
                    {category.replace('·박ส', '')}
                  </button>
                ))}
              </div>
              <div className="grid grid-cols-2 gap-3">
                {activeCargoItems.slice(0, 8).map((detail, index) => {
                  const selectedIndex = cargoItems.findIndex((item) => item.category === activeCargoCategory && item.detail === detail.name)
                  const selectedItem = selectedIndex >= 0 ? cargoItems[selectedIndex] : null
                  return (
                    <div key={detail.name} className={`rounded-2xl border p-3 ${selectedItem ? 'border-emerald-600 bg-emerald-50' : 'border-gray-100 bg-white'}`}>
                      <button type="button" onClick={() => toggleCargoDetail(activeCargoCategory, detail.name)} className="w-full text-left">
                        <span className="text-2xl">{['🧊', '🖥️', '📺', '🛋️', '📦', '🪑'][index % 6]}</span>
                        <span className="mt-2 block min-h-10 text-xs font-bold leading-5 text-gray-800">{detail.name}</span>
                      </button>
                      {selectedItem && (
                        <div className="mt-2 flex items-center justify-between rounded-lg bg-white px-2 py-1 shadow-sm">
                          <button type="button" onClick={() => updateCargoItem(selectedIndex, 'quantity', selectedItem.quantity - 1)} className="h-7 w-7 rounded-full text-gray-500">−</button>
                          <span className="text-sm font-bold">{selectedItem.quantity}</span>
                          <button type="button" onClick={() => updateCargoItem(selectedIndex, 'quantity', selectedItem.quantity + 1)} className="h-7 w-7 rounded-full text-emerald-600">＋</button>
                        </div>
                      )}
                    </div>
                  )
                })}
              </div>

              <label className="mt-6 block text-sm font-medium text-gray-700" htmlFor="cargoTypeId">화물 유형
                <select id="cargoTypeId" name="cargoTypeId" value={effectiveCargoTypeId} onChange={handleChange} className="mt-2 w-full rounded-xl border border-gray-200 bg-white px-3 py-3 text-sm focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500" required>
                  <option value="">화물 유형을 선택하세요</option>
                  {cargoTypes.filter((cargoType) => cargoType.active).map((cargoType) => (
                    <option key={cargoType.id} value={cargoType.id}>{cargoType.name}</option>
                  ))}
                </select>
              </label>
              {cargoTypeError && <p className="mt-2 text-sm font-bold text-red-500" role="alert">{cargoTypeError}</p>}

              <div className="mt-4 grid grid-cols-2 gap-3">
                <Input id="cargoName" label="추가 설명" name="cargoName" value={form.cargoName} onChange={handleChange} placeholder="직접 입력" />
                <Input id="cargoWeightKg" label="총 무게 (kg)" name="cargoWeightKg" type="number" min="1" value={form.cargoWeightKg} onChange={handleWeightChange} placeholder={estimatedTotalWeight ? String(estimatedTotalWeight) : '예: 600'} required />
              </div>
              <p className="mt-3 rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                예상 총 무게 <strong>{estimatedTotalWeight.toLocaleString()}kg</strong>
                {!weightManuallyEdited && estimatedTotalWeight > 0 && ' · 자동 반영됨'}
              </p>
            </div>
          </section>
        )}

        {inputStep === 2 && (
          <section className="space-y-8 animate-in fade-in slide-in-from-bottom-2 duration-300">
            <div className="flex items-center">
              <button
                type="button"
                onClick={() => { setInputStep(1); window.scrollTo({ top: 0, behavior: 'smooth' }) }}
                className="flex items-center gap-1 text-sm font-bold text-emerald-700 hover:text-emerald-800 transition"
              >
                <span className="text-lg leading-none">←</span> 이전 단계로 (화물 정보 수정)
              </button>
            </div>

            <div>
              <div className="mb-3 flex items-end justify-between">
                <div>
                  <p className="text-xs font-semibold text-emerald-600">MOVE ROUTE</p>
                  <h2 className="mt-1 text-xl font-bold text-gray-900">어디서 어디로 보낼까요?</h2>
                </div>
                <button type="button" onClick={() => openPostcode('originAddress')} className="rounded-full bg-emerald-50 px-3 py-2 text-xs font-bold text-emerald-700">주소 검색</button>
              </div>

              <p className="mb-3 text-xs text-gray-500">
                저장한 배송지를 출발지와 도착지에 각각 선택하거나, 주소 검색으로 직접 입력할 수 있어요.
              </p>

              <div className="rounded-2xl border border-gray-200 bg-gray-50 p-4">
                <div className="flex gap-3">
                  <div className="flex w-5 shrink-0 flex-col items-center">
                    <span className="mt-1 h-3 w-3 rounded-full bg-emerald-600 ring-4 ring-emerald-100" />
                    <span className="my-1 w-px flex-1 border-l border-dashed border-emerald-200" />
                    <span className="mb-1 h-3 w-3 rounded-full bg-red-500 ring-4 ring-red-100" />
                  </div>
                  <div className="min-w-0 flex-1 space-y-5">
                    <label className="block" htmlFor="originAddress">
                      <span className="text-xs font-semibold text-gray-500">출발지</span>
                      <div className="mt-1 flex gap-2">
                        <input id="originAddress" name="originAddress" value={form.originAddress} readOnly placeholder="출발지 주소를 선택해 주세요" className="min-w-0 flex-1 rounded-xl border-0 bg-white px-3 py-3 text-sm shadow-sm ring-1 ring-gray-200 focus:ring-emerald-500" required />
                        <button type="button" onClick={() => openPostcode('originAddress')} className="rounded-xl bg-white px-3 text-xs font-bold text-gray-600 ring-1 ring-gray-200 hover:bg-gray-50">찾기</button>
                      </div>
                      <input name="originDetail" value={form.originDetail} onChange={handleChange} placeholder="상세주소 (동, 층, 호수)" className="mt-2 w-full bg-transparent px-1 py-1 text-xs text-gray-600 outline-none placeholder:text-gray-400" />
                    </label>
                    {savedAddresses.length > 0 && (
                      <select
                        aria-label="출발지 저장 배송지 선택"
                        value={selectedSavedAddress.origin}
                        onChange={(event) => selectSavedAddress('origin', event.target.value)}
                        className="w-full rounded-xl border border-gray-200 bg-white px-3 py-2 text-xs text-gray-700"
                      >
                        <option value="">출발지: 저장한 배송지 선택</option>
                        {savedAddresses.map((address) => (
                          <option key={address.id} value={String(address.id)}>{address.label} · {address.address}</option>
                        ))}
                      </select>
                    )}
                    <label className="block" htmlFor="destAddress">
                      <span className="text-xs font-semibold text-gray-500">도착지</span>
                      <div className="mt-1 flex gap-2">
                        <input id="destAddress" name="destAddress" value={form.destAddress} readOnly placeholder="도착지 주소를 선택해 주세요" className="min-w-0 flex-1 rounded-xl border-0 bg-white px-3 py-3 text-sm shadow-sm ring-1 ring-gray-200 focus:ring-emerald-500" required />
                        <button type="button" onClick={() => openPostcode('destAddress')} className="rounded-xl bg-white px-3 text-xs font-bold text-gray-600 ring-1 ring-gray-200 hover:bg-gray-50">찾기</button>
                      </div>
                      <input name="destDetail" value={form.destDetail} onChange={handleChange} placeholder="상세주소 (동, 층, 호수)" className="mt-2 w-full bg-transparent px-1 py-1 text-xs text-gray-600 outline-none placeholder:text-gray-400" />
                    </label>
                    {savedAddresses.length > 0 && (
                      <select
                        aria-label="도착지 저장 배송지 선택"
                        value={selectedSavedAddress.dest}
                        onChange={(event) => selectSavedAddress('dest', event.target.value)}
                        className="w-full rounded-xl border border-gray-200 bg-white px-3 py-2 text-xs text-gray-700"
                      >
                        <option value="">도착지: 저장한 배송지 선택</option>
                        {savedAddresses.map((address) => (
                          <option key={address.id} value={String(address.id)}>{address.label} · {address.address}</option>
                        ))}
                      </select>
                    )}
                  </div>
                </div>
              </div>

              {(effectiveOriginCoords || effectiveDestCoords) && (
                <div className="mt-4 overflow-hidden rounded-2xl border border-gray-200 bg-white">
                  <div ref={mapContainerRef} className="h-56 w-full" aria-label="출발지 및 도착지 위치 지도" />
                  <div className="flex items-center justify-between border-t border-gray-100 bg-gray-50 px-3.5 py-2 text-xs text-gray-600">
                    <span>{effectiveOriginCoords ? '● 출발 표시됨' : '○ 출발 미선택'}</span>
                    <span>{effectiveDestCoords ? '● 도착 표시됨' : '○ 도착 미선택'}</span>
                  </div>
                </div>
              )}
              {effectiveMapNotice && (
                <p className="mt-2 text-xs font-medium text-amber-700">{effectiveMapNotice}</p>
              )}
            </div>

            <div className="space-y-4">
              <h2 className="text-xl font-bold text-gray-900">보내는 분 · 받는 분 정보</h2>

              <div className="rounded-2xl bg-emerald-50 p-4 border border-emerald-100">
                <p className="mb-3 text-sm font-bold text-emerald-700">보내는 분</p>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <Input id="senderName" label="이름" name="senderName" value={form.senderName} onChange={handleChange} placeholder="보내는 분 이름" required />
                  <Input id="senderPhone" label="연락처" name="senderPhone" value={form.senderPhone} onChange={handleReceiverPhoneChange} pattern={PHONE_PATTERN.source} inputMode="numeric" placeholder="010-0000-0000" required />
                </div>
                <p className="mt-2 text-xs text-emerald-700">고객 정보에서 자동 입력되며 직접 수정할 수 있습니다.</p>
              </div>

              <div className="rounded-2xl border border-gray-100 bg-white p-4">
                <p className="mb-3 text-sm font-bold text-gray-800">받는 분</p>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <Input id="receiverName" label="이름" name="receiverName" value={form.receiverName} onChange={handleChange} placeholder="받는 분 이름" required />
                  <Input id="receiverPhone" label="연락처" name="receiverPhone" value={form.receiverPhone} onChange={handleReceiverPhoneChange} pattern={PHONE_PATTERN.source} inputMode="numeric" placeholder="010-0000-0000" required />
                </div>
              </div>

              <Input id="pickupAt" label="상차 희망 일시" name="pickupAt" type="datetime-local" min={getLocalDateTimeValue()} value={form.pickupAt} onChange={handleChange} required />

              <label className="block text-sm font-medium text-gray-700" htmlFor="memo">
                요청사항 (선택)
                <textarea id="memo" name="memo" value={form.memo} onChange={handleChange} maxLength={500} className="mt-2 min-h-24 w-full rounded-xl border border-gray-200 px-3 py-3 text-sm focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500" placeholder="기사님께 전달할 내용을 적어 주세요" />
              </label>
            </div>
          </section>
        )}

        {error && <p className="text-sm font-bold text-red-500" role="alert">{error}</p>}
      </form>

      <aside className="sticky top-24 m-5 hidden h-fit rounded-2xl bg-white p-6 shadow-sm lg:col-start-2 lg:row-start-2 lg:block border border-gray-100">
        <p className="text-xs font-semibold text-emerald-600">FARE SUMMARY</p>
        <h2 className="mt-1 text-xl font-bold text-gray-900">실시간 요금 계산서</h2>

        <div className="mt-6 rounded-xl bg-emerald-50 p-4">
          <p className="text-xs text-gray-500">예상 운임</p>
          <p className="mt-1 text-3xl font-black text-gray-900">{fare ? `${fare.totalFare.toLocaleString()}원` : '계산 전'}</p>
          {isDistanceOnlyFare && <p className="mt-2 inline-block rounded border border-emerald-200 bg-emerald-100 px-2 py-1 text-[11px] font-bold text-emerald-700">※ 직선거리 기준 운임</p>}
        </div>

        {fare && (
          <dl className="mt-5 space-y-3 text-sm text-gray-600">
            <div className="flex justify-between"><dt>거리</dt><dd>{fare.distanceKm}km</dd></div>
            <div className="flex justify-between"><dt>기본 운임</dt><dd>{fare.baseFare.toLocaleString()}원</dd></div>
            <div className="flex justify-between"><dt>화물 추가</dt><dd>{fare.cargoFare.toLocaleString()}원</dd></div>
          </dl>
        )}

        {fare && (
          <details className="mt-5 rounded-xl border border-emerald-100 bg-emerald-50 p-4" open>
            <summary className="cursor-pointer text-sm font-bold text-emerald-700">운임 상세 내역 보기</summary>
            <dl className="mt-4 grid grid-cols-2 gap-2 text-xs text-gray-600">
              <div>거리 <strong>{fare.distanceKm}km</strong></div>
              <div>기본 운임 <strong>{fare.baseFare.toLocaleString()}원</strong></div>
              <div>거리 추가 <strong>{fare.distanceFare.toLocaleString()}원</strong></div>
              <div>화물 추가 <strong>{fare.cargoFare.toLocaleString()}원</strong></div>
              <div>할증 <strong>{fare.surchargeFare.toLocaleString()}원</strong></div>
            </dl>
            {fare.inquiryRequired && <p className="mt-3 text-sm font-bold text-red-500">적재 중량 초과로 별도 문의가 필요합니다.</p>}
          </details>
        )}

        {!fare && (
          <button type="button" onClick={inputStep === 1 ? handleNextInputStep : handleEstimate} disabled={loading || (inputStep === 1 && !canMoveToTransport) || (inputStep === 2 && !canEstimateFare)} className="mt-6 w-full rounded-xl bg-emerald-600 px-4 py-3 font-bold text-white transition hover:bg-emerald-700 disabled:bg-gray-300 disabled:text-gray-500">
            {inputStep === 1 ? '다음: 운송 정보 입력 ▶' : '예상 운임 확인'}
          </button>
        )}

        {fare && (
          <div className="mt-6 space-y-3">
            <button type="button" onClick={handleEstimate} disabled={loading} className="w-full rounded-xl border border-emerald-600 bg-white px-4 py-3 font-bold text-emerald-700 hover:bg-emerald-50 disabled:opacity-50">
              운임 다시 계산
            </button>
            <button type="button" onClick={handleCreateShipment} disabled={loading || fare.inquiryRequired} className="w-full rounded-xl bg-emerald-600 px-4 py-3 font-bold text-white hover:bg-emerald-700 disabled:bg-gray-300">
              {fare.inquiryRequired ? '문의 필요' : '배송 신청하기'}
            </button>
          </div>
        )}
      </aside>

      {fare && (
        <details className="mx-5 mb-5 rounded-2xl border border-emerald-100 bg-emerald-50 p-4 lg:hidden" open>
          <summary className="cursor-pointer text-sm font-bold text-emerald-700">운임 상세 내역 보기</summary>
          <dl className="mt-4 grid grid-cols-2 gap-2 text-xs text-gray-600">
            <div>거리 <strong>{fare.distanceKm}km</strong></div>
            <div>기본 운임 <strong>{fare.baseFare.toLocaleString()}원</strong></div>
            <div>거리 추가 <strong>{fare.distanceFare.toLocaleString()}원</strong></div>
            <div>화물 추가 <strong>{fare.cargoFare.toLocaleString()}원</strong></div>
            <div>할증 <strong>{fare.surchargeFare.toLocaleString()}원</strong></div>
          </dl>
          {fare.inquiryRequired && <p className="mt-3 text-sm font-bold text-red-500">적재 중량 초과로 별도 문의가 필요합니다.</p>}
        </details>
      )}

      {shipment && (
        <section className="mx-5 mb-5 rounded-2xl border border-emerald-100 bg-emerald-50 p-5 shadow-sm" aria-live="polite">
          <h2 className="text-lg font-bold text-emerald-800">배송 신청이 접수되었습니다!</h2>
          <p className="mt-2 text-sm text-emerald-700">토스페이먼츠 결제를 완료하시면 기사님 배차가 시작됩니다.</p>
          <p className="mt-3 text-xs font-semibold text-emerald-600">주문번호: {shipment.orderId}</p>
        </section>
      )}

      <div className="fixed inset-x-0 bottom-0 z-30 mx-auto flex h-16 max-w-[480px] items-center justify-between gap-3 border-t border-gray-100 bg-white/95 px-5 pb-safe shadow-[0_-4px_20px_rgba(0,0,0,0.08)] backdrop-blur md:h-[72px] md:max-w-[768px] md:px-6 lg:hidden">
        <div className="min-w-0 flex-1">
          {fare ? (
            <div className="flex flex-col justify-center">
              <p className="text-[11px] font-semibold text-gray-500">결제 예상 운임</p>
              <div className="flex items-center gap-1">
                <p className="truncate text-lg font-black text-gray-900">{fare.totalFare.toLocaleString()}원</p>
                {isDistanceOnlyFare && <span className="rounded bg-emerald-100 px-1 py-0.5 text-[10px] font-bold text-emerald-700">직선거리</span>}
              </div>
            </div>
          ) : inputStep === 1 ? (
            <>
              <p className="text-[11px] font-semibold text-gray-500">화물 정보</p>
              <p className="truncate text-sm font-bold text-gray-900">차종·화물·무게를 입력해 주세요</p>
            </>
          ) : (
            <>
              <p className="text-[11px] font-semibold text-gray-500">운송 정보</p>
              <p className="truncate text-sm font-bold text-gray-900">주소와 수령인 정보를 입력해 주세요</p>
            </>
          )}
        </div>

        {fare ? (
          <button type="button" onClick={handleCreateShipment} disabled={loading || fare.inquiryRequired} className="shrink-0 rounded-xl bg-emerald-600 px-5 py-3 text-sm font-bold text-white shadow-lg shadow-emerald-200 transition hover:bg-emerald-700 disabled:opacity-50">
            {fare.inquiryRequired ? '문의 필요' : '배송 신청하기 ▶'}
          </button>
        ) : (
          <button type="submit" form="shipment-create-form" disabled={loading || (inputStep === 1 ? !canMoveToTransport : !canEstimateFare)} className="shrink-0 rounded-xl bg-emerald-600 px-5 py-3 text-sm font-bold text-white shadow-lg shadow-emerald-200 transition hover:bg-emerald-700 disabled:opacity-50 disabled:shadow-none">
            {inputStep === 1 ? '다음: 운송 정보 입력 ▶' : '다음: 예상 운임 확인 ▶'}
          </button>
        )}
      </div>
    </section>
  )
}

export default ShipmentCreatePage