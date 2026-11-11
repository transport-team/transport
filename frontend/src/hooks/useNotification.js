// 알림 훅 (6-3 — 팀원 4)
// SSE 구독 + 안 읽은 개수 + 새 알림 토스트 상태를 한 번에 내려준다.
// 사용: const { unreadCount, notifications, toast, markRead, loadMore } = useNotification()
import { useCallback, useEffect, useRef, useState } from 'react'
import { getCookie } from '../util/cookieUtil'
import { API_SERVER_HOST } from '../api/apiClient'
import { getMyNotifications, markNotificationRead } from '../api/notificationApi'

const useNotification = () => {
    const member = getCookie('member')
    const memberId = member?.id
    const accessToken = member?.accessToken

    const [unreadCount, setUnreadCount] = useState(0)
    const [notifications, setNotifications] = useState([])   // 드롭다운에 보여줄 최근 목록
    const [pageData, setPageData] = useState(null)            // 페이징 정보 (더보기용)
    const [toast, setToast] = useState(null)                  // 새 알림 토스트 { id, message, shipmentId }
    const [connected, setConnected] = useState(false)

    const eventSourceRef = useRef(null)
    const reconnectTimer = useRef(null)

    // 목록 1페이지 + 안 읽은 개수 조회
    const refresh = useCallback(async () => {
        try {
            const data = await getMyNotifications({ page: 1, size: 10 })
            setNotifications(data.page?.dtoList || [])
            setPageData(data.page || null)
            setUnreadCount(data.unreadCount || 0)
        } catch (err) {
            console.error('알림 목록 조회 실패:', err)
        }
    }, [])

    // 더보기 (드롭다운에서 다음 페이지)
    const loadMore = useCallback(async () => {
        if (!pageData?.next) return
        const nextPage = (pageData.current || 1) + 1
        try {
            const data = await getMyNotifications({ page: nextPage, size: 10 })
            setNotifications((prev) => [...prev, ...(data.page?.dtoList || [])])
            setPageData(data.page)
        } catch (err) {
            console.error('알림 더보기 실패:', err)
        }
    }, [pageData])

    // 읽음 처리 → 배지 줄이고 목록에서도 갱신
    const markRead = useCallback(async (notification) => {
        if (!notification.isRead) {
            try {
                await markNotificationRead(notification.id)
                setUnreadCount((c) => Math.max(0, c - 1))
                setNotifications((prev) =>
                    prev.map((n) => (n.id === notification.id ? { ...n, isRead: true } : n))
                )
            } catch (err) {
                console.error('읽음 처리 실패:', err)
            }
        }
        return notification.shipmentId
    }, [])

    // 토스트 자동 닫기
    useEffect(() => {
        if (!toast) return
        const timer = setTimeout(() => setToast(null), 4000)
        return () => clearTimeout(timer)
    }, [toast])

    // 로그인 상태에서만 SSE 연결 + 초기 조회
    useEffect(() => {
        if (!memberId || !accessToken) return

        refresh()

        // event-source-polyfill은 아직 없다(제안 상태) — 네이티브 EventSource로 먼저 연결.
        // NOTE: 네이티브 EventSource는 Authorization 헤더를 못 붙인다.
        //       임시로 쿼리스트링 토큰 방식 대신, polyfill 설치 전까지는 연결 실패해도 화면은 동작(폴링 아님, 수동 새로고침).
        //       package.json에 event-source-polyfill 추가(PR 제안) 후 헤더 방식으로 교체한다.
        const connect = () => {
            try {
                const es = new EventSourcePolyfillSafe(accessToken)
                eventSourceRef.current = es

                es.onopen = () => setConnected(true)

                es.addEventListener('notification', (e) => {
                    try {
                        const payload = JSON.parse(e.data)
                        setToast(payload)
                        setUnreadCount((c) => c + 1)
                        setNotifications((prev) => [{ ...payload, isRead: false }, ...prev])
                    } catch (parseErr) {
                        console.error('알림 파싱 실패:', parseErr)
                    }
                })

                es.onerror = () => {
                    setConnected(false)
                    // 재연결 (5초 뒤) — EventSource 기본 재연결이 있지만, 연결이 완전히 끊긴 경우 대비
                    if (reconnectTimer.current) clearTimeout(reconnectTimer.current)
                    reconnectTimer.current = setTimeout(() => {
                        es.close()
                        connect()
                    }, 5000)
                }
            } catch (err) {
                console.warn('SSE 연결 실패 (polyfill 미설치일 수 있음):', err)
            }
        }

        // polyfill이 있으면 헤더 방식, 없으면 네이티브 (헤더 없이 — 서버 401 → onerror)
        function EventSourcePolyfillSafe(token) {
            if (window.EventSourcePolyfill) {
                return new window.EventSourcePolyfill(`${API_SERVER_HOST}/api/notifications/subscribe`, {
                    headers: { Authorization: `Bearer ${token}` },
                    heartbeatTimeout: 120000,
                })
            }
            return new EventSource(`${API_SERVER_HOST}/api/notifications/subscribe`)
        }

        connect()

        return () => {
            if (reconnectTimer.current) clearTimeout(reconnectTimer.current)
            if (eventSourceRef.current) {
                eventSourceRef.current.close()
                eventSourceRef.current = null
            }
            setConnected(false)
        }
    }, [memberId, accessToken, refresh])

    return { unreadCount, notifications, pageData, toast, connected, refresh, loadMore, markRead }
}

export default useNotification
