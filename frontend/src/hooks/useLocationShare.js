// 위치 공유 훅 (7-2 — 팀원 4)
// 기사 배송 처리 화면에서 IN_TRANSIT일 때만 watchPosition으로 위치를 받아
// 30초에 한 번 서버로 전송한다. 상태가 바뀌거나 화면을 나가면 clearWatch.
// 사용: const { sharing, lastSentAt, error, secondsAgo } = useLocationShare(shipmentId, shipment.status)
import { useCallback, useEffect, useRef, useState } from 'react'
import jwtAxios from '../util/jwtUtil'

const SEND_INTERVAL_MS = 30_000 // 30초에 한 번 (계획서·decisions 기준)
const GEO_SUPPORTED = typeof navigator !== 'undefined' && 'geolocation' in navigator

const useLocationShare = (shipmentId, status) => {
    const [sharing, setSharing] = useState(false)       // 좌표를 받는 중 (첫 좌표가 오면 true)
    const [lastSentAt, setLastSentAt] = useState(null)  // 마지막 전송 성공 시각 (Date)
    const [watchError, setWatchError] = useState('')    // 위치 권한 · 신호 문제 (좌표가 다시 오면 지운다)
    const [sendError, setSendError] = useState('')      // 서버 전송 문제 (전송에 성공하면 지운다)
    const [secondsAgo, setSecondsAgo] = useState(0)     // "n초 전 전송" 표시용

    const watchIdRef = useRef(null)
    const lastSentRef = useRef(null)                    // 마지막 전송 시각 (ref — 타이머/전송 로직 공용)
    const latestPosRef = useRef(null)                   // 가장 최근 좌표 (30초 타이머가 전송)
    const sendingRef = useRef(false)

    const isTracking = status === 'IN_TRANSIT'

    // 감시 멈추기 (sendNow가 쓰므로 먼저 선언한다)
    const stopWatch = useCallback(() => {
        if (watchIdRef.current !== null) {
            navigator.geolocation.clearWatch(watchIdRef.current)
            watchIdRef.current = null
        }
        latestPosRef.current = null
        setSharing(false)
    }, [])

    // 서버 전송 (IN_TRANSIT + 좌표 있을 때만)
    const sendNow = useCallback(async () => {
        const pos = latestPosRef.current
        if (!pos || sendingRef.current) return
        sendingRef.current = true
        try {
            await jwtAxios.post(`/api/shipments/${shipmentId}/locations`, {
                lat: pos.lat,
                lng: pos.lng,
            })
            lastSentRef.current = new Date()
            setLastSentAt(lastSentRef.current)
            setSendError('')
        } catch (err) {
            const code = err.response?.data?.code
            if (code === 'NOT_IN_TRANSIT') {
                // 상태가 이미 바뀜 (완료 등) — 감시를 멈추는 신호로 본다
                setSendError('')
                stopWatch()
            } else if (code === 'NOT_ASSIGNED_DRIVER') {
                setSendError('이 배송의 배정 기사가 아니에요.')
                stopWatch()
            } else {
                setSendError('위치 전송에 실패했어요. 30초 뒤 다시 시도해요.')
            }
        } finally {
            sendingRef.current = false
        }
    }, [shipmentId, stopWatch])

    // 상태가 IN_TRANSIT일 때만 감시 시작 — 상태가 바뀌거나 화면을 나가면 정리 함수가 멈춘다
    // (effect 안에서 바로 setState 하지 않는다: sharing · 오류는 위치 콜백에서 바꾼다)
    useEffect(() => {
        if (!isTracking || !GEO_SUPPORTED) return

        watchIdRef.current = navigator.geolocation.watchPosition(
            (pos) => {
                // 좌표만 갱신 — 전송은 30초 타이머가 한다 (API 호출 절약)
                latestPosRef.current = { lat: pos.coords.latitude, lng: pos.coords.longitude }
                setSharing(true)
                setWatchError('')
            },
            (err) => {
                if (err.code === err.PERMISSION_DENIED) {
                    setWatchError('위치 권한이 거부됐어요. 브라우저 설정에서 위치 권한을 허용해 주세요.')
                } else if (err.code === err.POSITION_UNAVAILABLE) {
                    setWatchError('위치 신호를 찾을 수 없어요. GPS 상태를 확인해 주세요.')
                } else {
                    setWatchError('위치를 가져오지 못했어요. 다시 시도해 주세요.')
                }
                setSharing(false)
            },
            { enableHighAccuracy: true, maximumAge: 15_000, timeout: 20_000 }
        )

        return () => stopWatch()
    }, [isTracking, shipmentId, stopWatch])

    // 30초 타이머 — 좌표가 있으면 전송
    useEffect(() => {
        if (status !== 'IN_TRANSIT') return
        const timer = setInterval(() => {
            if (latestPosRef.current) sendNow()
        }, SEND_INTERVAL_MS)
        return () => clearInterval(timer)
    }, [status, shipmentId, sendNow])

    // 첫 좌표를 받자마자 1회 전송 (지도가 바로 그려지도록)
    useEffect(() => {
        if (status !== 'IN_TRANSIT') return
        const timer = setTimeout(() => {
            if (latestPosRef.current && !lastSentRef.current) sendNow()
        }, 3_000) // GPS 안정화 3초 대기
        return () => clearTimeout(timer)
    }, [status, shipmentId, sendNow])

    // "n초 전 전송" 표시용 1초 카운터
    useEffect(() => {
        const timer = setInterval(() => {
            if (lastSentRef.current) {
                setSecondsAgo(Math.floor((Date.now() - lastSentRef.current.getTime()) / 1000))
            }
        }, 1000)
        return () => clearInterval(timer)
    }, [])

    // 지원하지 않는 브라우저 안내는 상태로 두지 않고 계산한다
    const unsupportedError = isTracking && !GEO_SUPPORTED ? '이 브라우저는 위치 기능을 지원하지 않아요.' : ''
    const error = unsupportedError || watchError || sendError

    return { sharing, lastSentAt, secondsAgo, error }
}

export default useLocationShare
