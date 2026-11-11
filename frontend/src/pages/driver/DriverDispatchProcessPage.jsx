// 疫꿸퀣沅?獄쏄퀣??筌ｌ꼶???遺얇늺 (????4 ??5-3 + 5-4)
// 野껋럥以? /driver/dispatches/:shipmentId (獄쏄퀣媛?筌왖?癒?뒠 ?怨멸쉭?? ??釉????삘뀮???????? ?遺얇늺?? /driver/shipments/:id)
// ?怨쀬뵠?? GET /api/shipments/{id} (????3 沃섎㈇?????USE_MOCK_DATA嚥??袁⑸뻻 揶쏆뮆而? ?믩챷???롢늺 false筌?獄쏅떽???
// API: ?怨밴묶 癰궰野?PATCH /{id}/status 夷???彛???낆쨮??POST /{id}/images (PHOTO_REQUIRED??????筌띾Ŧ以???덇땀)
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getShipment, changeShipmentStatus, uploadShipmentImages } from '../../api/shipmentApi'
import StatusBadge from '../../components/common/StatusBadge'
import useLocationShare from '../../hooks/useLocationShare'
import Button from '../../components/common/Button'

// ????3 ?怨멸쉭 API ?믩챷? ?袁㏉돱筌왖 ?怨뺣뮉 ?袁⑸뻻 ?怨쀬뵠??(api.md??GET /api/shipments/{id} ??됰뻻 ?袁⑤굡 疫꿸퀣?)
const USE_MOCK_DATA = true
const MOCK_SHIPMENT = {
    shipmentId: 1,
    status: 'ASSIGNED',
    cargoName: '?醫듼봺 筌욊쑴肉??,
    cargoWeightKg: 600,
    memo: '??롡봺甕곗쥙?????곸벉',
    originAddress: '??뽰뒻?諛명??揶쏅베沅볠뤃?OO嚥?12',
    originDetail: '1筌???뤿열??,
    destAddress: '野껋럡由????륁뜚???怨밸꽰??OO嚥?34',
    destDetail: 'B??筌≪럡??,
    pickupAt: '2026-12-03T09:00:00',
    receiverName: '繹먃??롮죯',
    receiverPhone: '010-0000-0000',
    fare: 98500,
}

// ?袁⑹삺 ?怨밴묶?癒?퐣 疫꿸퀣沅쀥첎? ??"??쇱벉 ??ｍ? ??docs/status.md ?袁⑹뵠??(疫꿸퀣沅?筌뤵꺂彛?
const NEXT_STEP = {
    ASSIGNED: { next: 'PICKED_UP', label: '?怨멸컧 ?袁⑥┷ 筌ｌ꼶??, photoType: 'PICKUP', photoLabel: '?怨멸컧 ??彛? },
    PICKED_UP: { next: 'IN_TRANSIT', label: '??곷꽊 ??뽰삂' },
    IN_TRANSIT: { next: 'ARRIVED', label: '?袁⑷컩 筌ｌ꼶?? },
    ARRIVED: { next: 'COMPLETED', label: '獄쏄퀣???袁⑥┷ 筌ｌ꼶??, photoType: 'RECEIPT', photoLabel: '?紐꾨땾筌???彛? },
}

// PHOTO_REQUIRED ???癒?쑎 ?꾨뗀諭??????????덈뮉 ?얜챷??(UI ?癒??? ?癒?쑎 ?꾨뗀諭?域밸챶?嚥?X)
const ERROR_MESSAGES = {
    PHOTO_REQUIRED: '??彛???袁⑹뒄??곸뒄. ?袁⑥삋?癒?퐣 ??彛???믪눘? ????틠?깃쉭??',
    NOT_ASSIGNED_DRIVER: '??獄쏄퀣???獄쏄퀣??疫꿸퀣沅쀥첎? ?袁⑤빍?癒?뒄.',
    INVALID_STATUS_CHANGE: '筌왖疫??怨밴묶?癒?퐣?????臾믩씜????????곷선?? ??덉쨮?⑥쥙臾?????쇰뻻 ??뺣즲??雅뚯눘苑??',
}

const toMessage = (err) => {
    const code = err.response?.data?.code
    return ERROR_MESSAGES[code] || err.response?.data?.message || '?遺욧퍕????쎈솭??됰선?? ?醫롫뻻 ????쇰뻻 ??뺣즲??雅뚯눘苑??'
}

const DriverDispatchProcessPage = () => {
    const { shipmentId } = useParams()
    const [shipment, setShipment] = useState(null)
    const [loading, setLoading] = useState(true)
    const [loadError, setLoadError] = useState('')

    const [changing, setChanging] = useState(false)
    const [actionError, setActionError] = useState('')

    // ??彛???낆쨮???怨밴묶 (5-4)
    const [photos, setPhotos] = useState([])        // File 筌뤴뫖以?(沃섎챶?곮퉪?용┛??
    const [uploading, setUploading] = useState(false)
    const [uploadError, setUploadError] = useState('')
    const [uploadedCount, setUploadedCount] = useState(0)

    const step = shipment ? NEXT_STEP[shipment.status] : null

    // ?꾩튂 怨듭쑀 (7-2) ??IN_TRANSIT???뚮쭔 watchPosition + 30珥?媛꾧꺽 ?꾩넚
    const { sharing, secondsAgo, error: locationError } = useLocationShare(shipmentId, shipment?.status)

    useEffect(() => {
        const load = async () => {
            try {
                if (USE_MOCK_DATA) {
                    // ????3 ?怨멸쉭 API ?믩챷? ?? 筌뤴뫖???꾧숲 (id揶쎛 ??삘뀮筌?筌?筌≪뼚? 野껉퍔荑??筌ｌ꼶??
                    const data = String(MOCK_SHIPMENT.shipmentId) === String(shipmentId)
                        ? MOCK_SHIPMENT
                        : null
                    if (!data) setLoadError('獄쏄퀣???類ｋ궖??筌≪뼚??????곷선??')
                    else setShipment(data)
                } else {
                    // ??쇱젫 API (?믩챷? ????餓κ쑬彛???곷툡??ㅻ뮉??
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

    // ???뵬 ?醫뤾문 ?????筌왖 沃섎챶?곮퉪?용┛筌?(??낆쨮??뺣뮉 甕곌쑵??
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

    // input 揶쏅?????쑴??묾??袁る립 ????(揶쏆늿? ???뵬 ??苑??揶쎛?館釉?칰?
    const clearPhotoInput = () => {
        const el = document.getElementById('photo-input')
        if (el) el.value = ''
    }

    // "??쇱벉 ??ｍ? 甕곌쑵?????怨밴묶 癰궰野?    const handleNextStep = async () => {
        if (!step || changing) return
        setChanging(true)
        setActionError('')
        try {
            await changeShipmentStatus(shipmentId, step.next)
            // ?源껊궗: ?遺얇늺 ?怨쀬뵠?怨쀬벥 ?怨밴묶??獄쏅뗀以?揶쏄퉮??(?怨멸쉭 API ?믩챷? ?袁⑸퓠????????쨮 ?대Ŋ猿?
            setShipment((prev) => ({ ...prev, status: step.next }))
        } catch (err) {
            setActionError(toMessage(err))
        } finally {
            setChanging(false)
        }
    }

    if (loading) return <p className="p-6 text-gray-500">?븍뜄???삳뮉 餓?..</p>

    if (loadError) {
        return (
            <section className="grid gap-4 p-4">
                <Link to="/driver" className="w-fit text-sm text-gray-500 hover:text-blue-600">????됱몵嚥?/Link>
                <p className="rounded-2xl bg-white p-6 text-gray-600 shadow-sm">{loadError}</p>
            </section>
        )
    }

    return (
        <section className="mx-auto grid max-w-md gap-4 p-4">
            <Link to="/driver" className="w-fit text-sm text-gray-500 hover:text-blue-600">????됱몵嚥?/Link>

            {/* ??뺛걠 + ?怨밴묶 獄쏄퀣? */}
            <div className="flex items-center justify-between gap-3">
                <h1 className="text-xl font-bold text-gray-900">獄쏄퀣??筌ｌ꼶??/h1>
                <StatusBadge status={shipment.status} />
            </div>

            {/* ?遺압??類ｋ궖 */}
            <div className="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
                <p className="text-lg font-bold text-gray-900">{shipment.cargoName}</p>
                <p className="mt-1 text-sm text-gray-500">{shipment.cargoWeightKg}kg 夷?{shipment.fare?.toLocaleString('ko-KR')}??/p>
                {shipment.memo && (
                    <p className="mt-2 rounded-lg bg-amber-50 p-2 text-sm text-amber-800">?遺욧퍕??鍮? {shipment.memo}</p>
                )}
            </div>

            {/* 野껋럥以?*/}
            <div className="grid gap-2 rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
                <div>
                    <p className="text-xs font-medium text-gray-500">?곗뮆而삼쭪?</p>
                    <p className="text-sm font-semibold text-gray-900">{shipment.originAddress} {shipment.originDetail}</p>
                </div>
                <div className="text-blue-500">??/div>
                <div>
                    <p className="text-xs font-medium text-gray-500">?袁⑷컩筌왖</p>
                    <p className="text-sm font-semibold text-gray-900">{shipment.destAddress} {shipment.destDetail}</p>
                </div>
            </div>

            {/* ??롮죯?????袁れ넅 椰꾨㈇由?(tel:) */}
            <div className="flex items-center justify-between rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
                <div>
                    <p className="text-xs font-medium text-gray-500">??롮죯??/p>
                    <p className="text-sm font-semibold text-gray-900">{shipment.receiverName}</p>
                </div>
                <a
                    href={`tel:${shipment.receiverPhone}`}
                    className="rounded-xl bg-blue-600 px-4 py-2 text-sm font-bold text-white hover:bg-blue-700"
                >
                    ?諭??袁れ넅 椰꾨㈇由?                </a>
            </div>

            {/* ??彛???낆쨮??(5-4) ???袁⑹뒄????ｍ?癒?퐣筌??紐꾪뀱 */}
            {step?.photoType && (
                <div className="grid gap-3 rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
                    <p className="text-sm font-bold text-gray-800">
                        {step.photoLabel} ??낆쨮??<span className="text-red-500">(?袁⑸땾)</span>
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
                    {/* 沃섎챶?곮퉪?용┛ */}
                    {photos.length > 0 && (
                        <div className="grid grid-cols-3 gap-2">
                            {photos.map((file, i) => (
                                <img
                                    key={i}
                                    src={URL.createObjectURL(file)}
                                    alt={`沃섎챶?곮퉪?용┛ ${i + 1}`}
                                    className="h-20 w-full rounded-lg object-cover"
                                />
                            ))}
                        </div>
                    )}
                    <Button onClick={handleUpload} loading={uploading} disabled={photos.length === 0} size="sm">
                        {photos.length > 0 ? `${photos.length}????낆쨮?? : '??彛???醫뤾문??雅뚯눘苑??}
                    </Button>
                    {uploadedCount > 0 && (
                        <p className="text-sm font-semibold text-green-700">{uploadedCount}???????袁⑥┷!</p>
                    )}
                    {uploadError && <p className="text-sm font-semibold text-red-600">{uploadError}</p>}
                </div>
            )}

            {/* ??쇱벉 ??ｍ?甕곌쑵??(?袁⑹삺 ?怨밴묶??筌띿쉶??甕곌쑵????롪돌) */}
            {step ? (
                <Button onClick={handleNextStep} loading={changing} size="lg">
                    {step.label}
                </Button>
            ) : (
                <p className="rounded-2xl bg-gray-100 p-4 text-center text-sm text-gray-600">
                    ??獄쏄퀣??? 筌ｌ꼶?????ｍ롥첎? ??곷선?? (?袁⑥┷ 夷??띯뫁???
                </p>
            )}

            {/* 위치 공유 상태 표시 (7-2) — 운송중에만 */}
            {shipment?.status === 'IN_TRANSIT' && (
                <div className={`rounded-2xl border p-4 text-sm ${locationError ? 'border-amber-200 bg-amber-50 text-amber-800' : 'border-green-200 bg-green-50 text-green-800'}`}>
                    {locationError ? (
                        <p>📍 {locationError}</p>
                    ) : sharing ? (
                        <p>📍 공유 중 · {secondsAgo}초 전 전송</p>
                    ) : (
                        <p>📍 위치 신호를 기다리는 중...</p>
                    )}
                </div>
            )}
            {actionError && (
                <p className="rounded-xl bg-red-50 p-3 text-sm font-semibold text-red-600">{actionError}</p>
            )}
        </section>
    )
}

export default DriverDispatchProcessPage