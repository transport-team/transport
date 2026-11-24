// 고객 실시간 지도 (7-3 — 팀원 4)
// 카카오맵 위에 기사 마커를 표시. 데이터 소스 2단계:
//   1) 최초: GET /api/shipments/{id}/locations/latest 로 초기 위치
//   2) 이후: SSE 'location' 이벤트로 마커 이동 (부모가 SSE를 이미 연결해 있으면 props로 좌표를 받는다)
// 완료(COMPLETED) 후에는 위치를 숨긴다 — 개인정보 요구사항 (DRV-06, CUS-05).
// 좌표 순서 주의: 카카오맵은 (경도 lng, 위도 lat) 순서다. API는 lat, lng로 응답.
import { useEffect, useRef, useState } from 'react'
import jwtAxios from '../../util/jwtUtil'

// 카카오 SDK 로드 (index.html에 스크립트가 없으면 동적으로 로드)
const loadKakaoSdk = () =>
    new Promise((resolve, reject) => {
        if (window.kakao?.maps) return resolve(window.kakao)
        const key = import.meta.env.VITE_KAKAO_JS_KEY
        if (!key) return reject(new Error('VITE_KAKAO_JS_KEY 설정 없음'))
        const script = document.createElement('script')
        script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${key}&autoload=false`
        script.onload = () => window.kakao.maps.load(() => resolve(window.kakao))
        script.onerror = reject
        document.head.appendChild(script)
    })

// 두 위치 중 recordedAt이 더 최근인 것 (한쪽만 있으면 그것, 시각을 모르면 SSE 쪽)
const pickLatest = (fetched, live) => {
    if (!fetched) return live
    if (!live) return fetched
    return new Date(live.recordedAt) < new Date(fetched.recordedAt) ? fetched : live
}

const LiveTrackingMap = ({ shipmentId, status, sseLocation }) => {
    const mapRef = useRef(null)          // <div> DOM
    const kakaoRef = useRef(null)        // kakao 객체
    const mapObjRef = useRef(null)       // 지도 객체
    const markerRef = useRef(null)       // 기사 마커

    const [loaded, setLoaded] = useState(false)
    const [error, setError] = useState('')
    const [fetchedLoc, setFetchedLoc] = useState(null)   // latest API로 받은 위치 { lat, lng, recordedAt }
    const [now, setNow] = useState(() => Date.now())     // "n초 전" 계산 기준 (1초마다 갱신)

    const isTracking = status === 'IN_TRANSIT'
    // 보여 줄 위치: API로 받은 것과 SSE로 받은 것 중 최신 (effect로 상태에 다시 복사하지 않고 계산)
    const lastLoc = pickLatest(fetchedLoc, sseLocation?.lat != null ? sseLocation : null)
    const hasRecordedAt = Boolean(lastLoc?.recordedAt)

    // 1) SDK 로드 + 지도 생성 (1회)
    useEffect(() => {
        let cancelled = false
        loadKakaoSdk()
            .then((kakao) => {
                if (cancelled) return
                kakaoRef.current = kakao
                const container = mapRef.current
                if (!container) return
                mapObjRef.current = new kakao.maps.Map(container, {
                    center: new kakao.maps.LatLng(37.5663, 126.9779), // 임시 중심 (서울시청) — 위치 받으면 이동
                    level: 4,
                })
                setLoaded(true)
            })
            .catch(() => setError('지도를 불러올 수 없어요.'))
        return () => { cancelled = true }
    }, [])

    // 2) 최초 위치: latest API (IN_TRANSIT 진입 시 1회)
    useEffect(() => {
        if (!loaded || !shipmentId) return
        let cancelled = false
        jwtAxios.get(`/api/shipments/${shipmentId}/locations/latest`)
            .then((res) => {
                if (cancelled || !res.data) return
                setFetchedLoc(res.data)
            })
            .catch(() => {/* 204 또는 실패 — 무시, SSE를 기다린다 */})
        return () => { cancelled = true }
    }, [loaded, shipmentId, isTracking])

    // 3) "n초 전" 표시용 1초 타이머 (그릴 때 Date.now()를 부르지 않는다)
    useEffect(() => {
        if (!hasRecordedAt) return
        const timer = setInterval(() => setNow(Date.now()), 1000)
        return () => clearInterval(timer)
    }, [hasRecordedAt])

    // 4) 마커 표시·이동 (lastLoc이 바뀔 때)
    useEffect(() => {
        if (!loaded || !lastLoc || !mapObjRef.current) return
        const kakao = kakaoRef.current
        const pos = new kakao.maps.LatLng(lastLoc.lat, lastLoc.lng) // (위도, 경도) — LatLng는 lat 먼저

        if (!markerRef.current) {
            markerRef.current = new kakao.maps.Marker({ position: pos })
            markerRef.current.setMap(mapObjRef.current)
        } else {
            markerRef.current.setPosition(pos)
        }
        mapObjRef.current.setCenter(pos)
    }, [loaded, lastLoc])

    // 완료 후: 개인정보 — 위치를 아예 보여주지 않는다
    if (['COMPLETED', 'CANCELED'].includes(status)) {
        return (
            <div className="rounded-2xl bg-gray-100 p-6 text-center text-sm text-gray-500">
                배송이 끝나 기사 위치는 표시되지 않아요.
            </div>
        )
    }

    return (
        <div className="overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm">
            <div ref={mapRef} className="h-64 w-full" />
            {error && <p className="p-3 text-sm text-amber-700">{error}</p>}
            {!error && lastLoc && (
                <p className="border-t border-gray-100 p-3 text-xs text-gray-500">
                    📍 마지막 위치 {lastLoc.recordedAt
                        ? `${Math.max(0, Math.floor((now - new Date(lastLoc.recordedAt).getTime()) / 1000))}초 전`
                        : ''}
                </p>
            )}
            {!error && !lastLoc && (
                <p className="border-t border-gray-100 p-3 text-xs text-gray-400">
                    {isTracking ? '기사 위치를 기다리는 중...' : '운송이 시작되면 기사 위치가 표시돼요.'}
                </p>
            )}
        </div>
    )
}

export default LiveTrackingMap
