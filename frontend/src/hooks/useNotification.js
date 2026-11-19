// 알림 훅 (6-3 — 팀원 4)
// SSE 구독 + 안 읽은 개수 + 새 알림 토스트 상태를 한 번에 내려준다.
// 사용: const { unreadCount, notifications, toast, markRead, loadMore } = useNotification()
// 멀티탭 지원: BroadcastChannel로 탭 간 알림 상태 동기화 (SSE 연결은 1개 탭만, 나머지는 수신만)
import { useCallback, useEffect, useRef, useState } from 'react'
import { getCookie } from '../util/cookieUtil'
import { API_SERVER_HOST } from '../api/apiClient'
import { getMyNotifications, markNotificationRead } from '../api/notificationApi'

const CHANNEL_NAME = 'transport-notifications'

const useNotification = () => {
    const member = getCookie('member')
    const memberId = member?.memberId // 로그인 쿠키의 키는 memberId (slices/loginSlice와 같다)
    const accessToken = member?.accessToken

    const [unreadCount, setUnreadCount] = useState(0)
    const [notifications, setNotifications] = useState([])   // 드롭다운에 보여줄 최근 목록
    const [pageData, setPageData] = useState(null)            // 페이징 정보 (더보기용)
    const [toast, setToast] = useState(null)                  // 새 알림 토스트 { id, message, shipmentId }
    const [connected, setConnected] = useState(false)

    const eventSourceRef = useRef(null)
    const reconnectTimer = useRef(null)
    const reconnectAttempt = useRef(0)
    const channelRef = useRef(null)
    const isLeaderRef = useRef(false) // 이 탭이 SSE 연결을 맡을지 여부

    // 목록 1페이지 응답을 상태에 넣는다 (첫 조회 · 새로고침이 같이 쓴다)
    const applyFirstPage = useCallback((data) => {
        setNotifications(data.page?.dtoList || [])
        setPageData(data.page || null)
        setUnreadCount(data.unreadCount || 0)
    }, [])

    // 목록 1페이지 + 안 읽은 개수 조회
    const refresh = useCallback(async () => {
        try {
            applyFirstPage(await getMyNotifications({ page: 1, size: 10 }))
        } catch (err) {
            console.error('알림 목록 조회 실패:', err)
        }
    }, [applyFirstPage])

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
                // 다른 탭에도 동기화
                channelRef.current?.postMessage({ type: 'MARK_READ', notificationId: notification.id })
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

    // BroadcastChannel 초기화 (탭 간 동기화)
    useEffect(() => {
        if (!memberId) return

        const channel = new BroadcastChannel(CHANNEL_NAME)
        channelRef.current = channel

        channel.onmessage = (event) => {
            const msg = event.data
            switch (msg.type) {
                case 'NEW_NOTIFICATION':
                    // 리더 탭이 새 알림을 받으면 전체 탭에 브로드캐스트
                    setToast(msg.payload)
                    setUnreadCount((c) => c + 1)
                    setNotifications((prev) => [{ ...msg.payload, isRead: false }, ...prev])
                    break
                case 'MARK_READ':
                    // 다른 탭에서 읽음 처리 시 동기화
                    setUnreadCount((c) => Math.max(0, c - 1))
                    setNotifications((prev) =>
                        prev.map((n) => (n.id === msg.notificationId ? { ...n, isRead: true } : n))
                    )
                    break
                case 'SYNC_STATE':
                    // 탭이 새로 열렸을 때 리더 탭에게 현재 상태 요청
                    if (isLeaderRef.current) {
                        channel.postMessage({
                            type: 'STATE_RESPONSE',
                            unreadCount,
                            notifications,
                        })
                    }
                    break
                case 'STATE_RESPONSE':
                    // 리더 탭에서 받은 상태로 동기화
                    setUnreadCount(msg.unreadCount)
                    setNotifications(msg.notifications)
                    break
            }
        }

        return () => {
            channel.close()
            channelRef.current = null
        }
    }, [memberId, unreadCount, notifications])

    // 로그인 상태에서만 SSE 연결 + 초기 조회
    useEffect(() => {
        if (!memberId || !accessToken) return

        // 첫 조회 — 결과는 응답 콜백에서 넣는다 (effect 안에서 바로 setState 하지 않기)
        getMyNotifications({ page: 1, size: 10 })
            .then(applyFirstPage)
            .catch((err) => console.error('알림 목록 조회 실패:', err))

        // 리더 선출: storage 이벤트로 탭 간 협의
        const electLeader = () => {
            try {
                const existing = localStorage.getItem('notification-leader')
                const now = Date.now()
                if (!existing || now - JSON.parse(existing).timestamp > 5000) {
                    // 리더 없거나 5초 이상 갱신 안 됨 → 내가 리더
                    localStorage.setItem('notification-leader', JSON.stringify({ timestamp: now }))
                    isLeaderRef.current = true
                } else {
                    isLeaderRef.current = false
                }
            } catch {
                isLeaderRef.current = true // 에러 시 기본 리더
            }
        }

        // 주기적 리더 갱신 (하트비트)
        const leaderTimer = setInterval(() => {
            if (isLeaderRef.current) {
                localStorage.setItem('notification-leader', JSON.stringify({ timestamp: Date.now() }))
            } else {
                electLeader()
            }
        }, 3000)

        electLeader()

        // SSE 연결 (리더 탭만)
        const connect = () => {
            if (!isLeaderRef.current) {
                // 팔로워 탭: 리더에게 현재 상태 요청
                channelRef.current?.postMessage({ type: 'SYNC_STATE' })
                return
            }

            try {
                const es = new EventSourcePolyfillSafe(accessToken)
                eventSourceRef.current = es

                es.onopen = () => {
                    setConnected(true)
                    reconnectAttempt.current = 0 // 성공 시 재시도 카운터 리셋
                }

                es.addEventListener('notification', (e) => {
                    try {
                        const payload = JSON.parse(e.data)
                        // 로컬 상태 업데이트
                        setToast(payload)
                        setUnreadCount((c) => c + 1)
                        setNotifications((prev) => [{ ...payload, isRead: false }, ...prev])
                        // 다른 탭에 브로드캐스트
                        channelRef.current?.postMessage({ type: 'NEW_NOTIFICATION', payload })
                    } catch (parseErr) {
                        console.error('알림 파싱 실패:', parseErr)
                    }
                })

                es.onerror = () => {
                    setConnected(false)
                    // 재연결 — 지수 백오프 (1s, 2s, 4s, 8s, 16s, 30s 상한)
                    if (reconnectTimer.current) clearTimeout(reconnectTimer.current)
                    const attempt = (reconnectAttempt.current || 0) + 1
                    reconnectAttempt.current = attempt
                    const delay = Math.min(1000 * Math.pow(2, attempt - 1), 30000)
                    reconnectTimer.current = setTimeout(() => {
                        es.close()
                        connect()
                    }, delay)
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

        // 탭이 포커스될 때 리더 재선출
        const onFocus = () => electLeader()
        window.addEventListener('focus', onFocus)

        return () => {
            clearInterval(leaderTimer)
            window.removeEventListener('focus', onFocus)
            if (reconnectTimer.current) clearTimeout(reconnectTimer.current)
            if (eventSourceRef.current) {
                eventSourceRef.current.close()
                eventSourceRef.current = null
            }
            // 리더였다면 권한 해제
            if (isLeaderRef.current) {
                localStorage.removeItem('notification-leader')
            }
            setConnected(false)
        }
    }, [memberId, accessToken, applyFirstPage])

    return { unreadCount, notifications, pageData, toast, connected, refresh, loadMore, markRead }
}

export default useNotification
