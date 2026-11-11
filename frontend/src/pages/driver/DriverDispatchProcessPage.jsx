// 기사 배송 처리 화면 (팀원 4 — 5-3 + 5-4)
// 경로: /driver/dispatches/:shipmentId (배차 지원용 상세와 역할이 다르다 — 팀원2 화면은 /driver/shipments/:id)
// 데이터: GET /api/shipments/{id} (팀원 3 미구현 → USE_MOCK_DATA로 임시 개발, 머지되면 false만 바꾼다)
// API: 상태 변경 PATCH /{id}/status · 사진 업로드 POST /{id}/images (PHOTO_REQUIRED는 사람 말로 안내)
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getShipment, changeShipmentStatus, uploadShipmentImages } from '../../api/shipmentApi'
import StatusBadge from '../../components/common/StatusBadge'
import Button from '../../components/common/Button'

// 팀원 3 상세 API 머지 전까지 쓰는 임시 데이터 (api.md의 GET /api/shipments/{id} 예시 필드 기준)
const USE_MOCK_DATA = true
const MOCK_SHIPMENT = {
    shipmentId: 1,
    status: 'ASSIGNED',
    cargoName: '유리 진열장',
    cargoWeightKg: 600,
    memo: '엘리베이터 없음',
    originAddress: '서울특별시 강남구 OO로 12',
    originDetail: '1층 하역장',
    destAddress: '경기도 수원시 영통구 OO로 34',
    destDetail: 'B동 창고',
    pickupAt: '2026-12-03T09:00:00',
    receiverName: '김수령',
    receiverPhone: '010-0000-0000',
    fare: 98500,
}

// 현재 상태에서 기사가 할 "다음 단계" — docs/status.md 전이표 (기사 몫만)
const NEXT_STEP = {
    ASSIGNED: { next: 'PICKED_UP', label: '상차 완료 처리', photoType: 'PICKUP', photoLabel: '상차 사진' },
    PICKED_UP: { next: 'IN_TRANSIT', label: '운송 시작' },
    IN_TRANSIT: { next: 'ARRIVED', label: '도착 처리' },
    ARRIVED: { next: 'COMPLETED', label: '배송 완료 처리', photoType: 'RECEIPT', photoLabel: '인수증 사진' },
}

// PHOTO_REQUIRED 등 에러 코드 → 사람이 읽는 문장 (UI 점검표: 에러 코드 그대로 X)
const ERROR_MESSAGES = {
    PHOTO_REQUIRED: '사진이 필요해요. 아래에서 사진을 먼저 올려주세요.',
    NOT_ASSIGNED_DRIVER: '이 배송의 배정 기사가 아니에요.',
    INVALID_STATUS_CHANGE: '지금 상태에서는 이 작업을 할 수 없어요. 새로고침 후 다시 시도해 주세요.',
}

const toMessage = (err) => {
    const code = err.response?.data?.code
    return ERROR_MESSAGES[code] || err.response?.data?.message || '요청에 실패했어요. 잠시 후 다시 시도해 주세요.'
}

const DriverDispatchProcessPage = () => {
    const { shipmentId } = useParams()
    const [shipment, setShipment] = useState(null)
    const [loading, setLoading] = useState(true)
    const [loadError, setLoadError] = useState('')

    const [changing, setChanging] = useState(false)
    const [actionError, setActionError] = useState('')

    // 사진 업로드 상태 (5-4)
    const [photos, setPhotos] = useState([])        // File 목록 (미리보기용)
    const [uploading, setUploading] = useState(false)
    const [uploadError, setUploadError] = useState('')
    const [uploadedCount, setUploadedCount] = useState(0)

    const step = shipment ? NEXT_STEP[shipment.status] : null

    useEffect(() => {
        const load = async () => {
            try {
                if (USE_MOCK_DATA) {
                    // 팀원 3 상세 API 머지 전: 목데이터 (id가 다르면 못 찾은 것처럼 처리)
                    const data = String(MOCK_SHIPMENT.shipmentId) === String(shipmentId)
                        ? MOCK_SHIPMENT
                        : null
                    if (!data) setLoadError('배송 정보를 찾을 수 없어요.')
                    else setShipment(data)
                } else {
                    // 실제 API (머지 후 이 줄만 살아남는다)
                    const data = await getShipment(shipmentId)
                    setShipment(data)
                }
            } catch (err) {
                setLoadError(toMessage(err))
            } finally {
                setLoading(false)
            }
        }
        load()
    }, [shipmentId])

    // 파일 선택 → 이미지 미리보기만 (업로드는 버튼)
    const handleSelectFiles = (e) => {
        setUploadError('')
        setUploadedCount(0)
        setPhotos(Array.from(e.target.files || []))
    }

    const handleUpload = async () => {
        if (!step?.photoType || photos.length === 0 || uploading) return
        setUploading(true)
        setUploadError('')
        try {
            const saved = await uploadShipmentImages(shipmentId, photos, step.photoType)
            setUploadedCount(saved.length)
            setPhotos([])
            clearPhotoInput()
        } catch (err) {
            setUploadError(toMessage(err))
        } finally {
            setUploading(false)
        }
    }

    // input 값을 비우기 위한 헬퍼 (같은 파일 재선택 가능하게)
    const clearPhotoInput = () => {
        const el = document.getElementById('photo-input')
        if (el) el.value = ''
    }

    // "다음 단계" 버튼 → 상태 변경
    const handleNextStep = async () => {
        if (!step || changing) return
        setChanging(true)
        setActionError('')
        try {
            await changeShipmentStatus(shipmentId, step.next)
            // 성공: 화면 데이터의 상태를 바로 갱신 (상세 API 머지 후에는 재조회로 교체)
            setShipment((prev) => ({ ...prev, status: step.next }))
        } catch (err) {
            setActionError(toMessage(err))
        } finally {
            setChanging(false)
        }
    }

    if (loading) return <p className="p-6 text-gray-500">불러오는 중...</p>

    if (loadError) {
        return (
            <section className="grid gap-4 p-4">
                <Link to="/driver" className="w-fit text-sm text-gray-500 hover:text-blue-600">← 홈으로</Link>
                <p className="rounded-2xl bg-white p-6 text-gray-600 shadow-sm">{loadError}</p>
            </section>
        )
    }

    return (
        <section className="mx-auto grid max-w-md gap-4 p-4">
            <Link to="/driver" className="w-fit text-sm text-gray-500 hover:text-blue-600">← 홈으로</Link>

            {/* 제목 + 상태 배지 */}
            <div className="flex items-center justify-between gap-3">
                <h1 className="text-xl font-bold text-gray-900">배송 처리</h1>
                <StatusBadge status={shipment.status} />
            </div>

            {/* 화물 정보 */}
            <div className="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
                <p className="text-lg font-bold text-gray-900">{shipment.cargoName}</p>
                <p className="mt-1 text-sm text-gray-500">{shipment.cargoWeightKg}kg · {shipment.fare?.toLocaleString('ko-KR')}원</p>
                {shipment.memo && (
                    <p className="mt-2 rounded-lg bg-amber-50 p-2 text-sm text-amber-800">요청사항: {shipment.memo}</p>
                )}
            </div>

            {/* 경로 */}
            <div className="grid gap-2 rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
                <div>
                    <p className="text-xs font-medium text-gray-500">출발지</p>
                    <p className="text-sm font-semibold text-gray-900">{shipment.originAddress} {shipment.originDetail}</p>
                </div>
                <div className="text-blue-500">↓</div>
                <div>
                    <p className="text-xs font-medium text-gray-500">도착지</p>
                    <p className="text-sm font-semibold text-gray-900">{shipment.destAddress} {shipment.destDetail}</p>
                </div>
            </div>

            {/* 수령인 — 전화 걸기 (tel:) */}
            <div className="flex items-center justify-between rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
                <div>
                    <p className="text-xs font-medium text-gray-500">수령인</p>
                    <p className="text-sm font-semibold text-gray-900">{shipment.receiverName}</p>
                </div>
                <a
                    href={`tel:${shipment.receiverPhone}`}
                    className="rounded-xl bg-blue-600 px-4 py-2 text-sm font-bold text-white hover:bg-blue-700"
                >
                    📞 전화 걸기
                </a>
            </div>

            {/* 사진 업로드 (5-4) — 필요한 단계에서만 노출 */}
            {step?.photoType && (
                <div className="grid gap-3 rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
                    <p className="text-sm font-bold text-gray-800">
                        {step.photoLabel} 업로드 <span className="text-red-500">(필수)</span>
                    </p>
                    <input
                        id="photo-input"
                        type="file"
                        accept="image/*"
                        capture="environment"
                        multiple
                        onChange={handleSelectFiles}
                        className="w-full text-sm text-gray-600 file:mr-3 file:rounded-lg file:border-0 file:bg-blue-50 file:px-4 file:py-2 file:text-sm file:font-semibold file:text-blue-700"
                    />
                    {/* 미리보기 */}
                    {photos.length > 0 && (
                        <div className="grid grid-cols-3 gap-2">
                            {photos.map((file, i) => (
                                <img
                                    key={i}
                                    src={URL.createObjectURL(file)}
                                    alt={`미리보기 ${i + 1}`}
                                    className="h-20 w-full rounded-lg object-cover"
                                />
                            ))}
                        </div>
                    )}
                    <Button onClick={handleUpload} loading={uploading} disabled={photos.length === 0} size="sm">
                        {photos.length > 0 ? `${photos.length}장 업로드` : '사진을 선택해 주세요'}
                    </Button>
                    {uploadedCount > 0 && (
                        <p className="text-sm font-semibold text-green-700">{uploadedCount}장 저장 완료!</p>
                    )}
                    {uploadError && <p className="text-sm font-semibold text-red-600">{uploadError}</p>}
                </div>
            )}

            {/* 다음 단계 버튼 (현재 상태에 맞는 버튼 하나) */}
            {step ? (
                <Button onClick={handleNextStep} loading={changing} size="lg">
                    {step.label}
                </Button>
            ) : (
                <p className="rounded-2xl bg-gray-100 p-4 text-center text-sm text-gray-600">
                    이 배송은 처리할 단계가 없어요. (완료 · 취소됨)
                </p>
            )}

            {actionError && (
                <p className="rounded-xl bg-red-50 p-3 text-sm font-semibold text-red-600">{actionError}</p>
            )}
        </section>
    )
}

export default DriverDispatchProcessPage