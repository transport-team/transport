// 헤더 알림 종 (6-3 — 팀원 4)
// useNotification 훅과 함께 사용. 팀원 1의 헤더(layouts)에서 아래처럼 붙인다:
//   import NotificationBell from '../components/common/NotificationBell'
//   <NotificationBell />
// 클릭한 알림의 shipmentId로 이동은 각 역할 라우트가 다르므로 onNavigate prop으로 위임한다.
import { useEffect, useRef, useState } from 'react'
import useNotification from '../../hooks/useNotification'

const NotificationBell = ({ onNavigate }) => {
    const { unreadCount, notifications, pageData, toast, loadMore, markRead } = useNotification()
    const [open, setOpen] = useState(false)
    const boxRef = useRef(null)

    // 바깥 클릭 시 드롭다운 닫기
    useEffect(() => {
        const handleClickOutside = (e) => {
            if (boxRef.current && !boxRef.current.contains(e.target)) setOpen(false)
        }
        document.addEventListener('mousedown', handleClickOutside)
        return () => document.removeEventListener('mousedown', handleClickOutside)
    }, [])

    const handleClickNotification = async (notification) => {
        const shipmentId = await markRead(notification)
        setOpen(false)
        if (onNavigate && shipmentId) onNavigate(shipmentId)
    }

    return (
        <div className="relative" ref={boxRef}>
            {/* 종 버튼 */}
            <button
                type="button"
                onClick={() => setOpen((o) => !o)}
                aria-label={`알림 ${unreadCount > 0 ? `${unreadCount}개 안 읽음` : '없음'}`}
                className="relative rounded-full p-2 text-gray-600 transition hover:bg-gray-100"
            >
                {/* 종 아이콘 (SVG — 이모지 대신 균일하게) */}
                <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M15 17h5l-1.4-1.4A2 2 0 0118 14.2V11a6 6 0 10-12 0v3.2c0 .5-.2 1-.6 1.4L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                </svg>
                {/* 안 읽은 배지 */}
                {unreadCount > 0 && (
                    <span className="absolute -top-0.5 -right-0.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-red-500 px-1 text-xs font-bold text-white">
                        {unreadCount > 99 ? '99+' : unreadCount}
                    </span>
                )}
            </button>

            {/* 드롭다운 */}
            {open && (
                <div className="absolute right-0 z-50 mt-2 w-80 max-w-[90vw] overflow-hidden rounded-xl border border-gray-200 bg-white shadow-lg">
                    <p className="border-b border-gray-100 px-4 py-3 text-sm font-bold text-gray-800">알림</p>

                    {notifications.length === 0 ? (
                        <p className="px-4 py-6 text-center text-sm text-gray-500">새 알림이 없어요.</p>
                    ) : (
                        <ul className="max-h-80 overflow-y-auto">
                            {notifications.map((n) => (
                                <li key={n.id}>
                                    <button
                                        type="button"
                                        onClick={() => handleClickNotification(n)}
                                        className={`w-full px-4 py-3 text-left transition hover:bg-gray-50 ${n.isRead ? '' : 'bg-blue-50/60'}`}
                                    >
                                        <p className={`text-sm ${n.isRead ? 'text-gray-500' : 'font-semibold text-gray-900'}`}>
                                            {n.message}
                                        </p>
                                        <p className="mt-0.5 text-xs text-gray-400">
                                            {new Date(n.createdAt).toLocaleString('ko-KR')}
                                        </p>
                                    </button>
                                </li>
                            ))}
                        </ul>
                    )}

                    {pageData?.next && (
                        <button
                            type="button"
                            onClick={loadMore}
                            className="w-full border-t border-gray-100 py-2 text-sm text-blue-600 hover:bg-gray-50"
                        >
                            더 보기
                        </button>
                    )}
                </div>
            )}

            {/* 새 알림 토스트 (헤더 오른쪽 아래) */}
            {toast && (
                <div className="fixed top-16 right-4 z-[100] w-80 max-w-[90vw] rounded-xl border border-blue-100 bg-white p-4 shadow-lg">
                    <p className="text-sm font-semibold text-gray-900">🔔 새 알림</p>
                    <p className="mt-1 text-sm text-gray-700">{toast.message}</p>
                </div>
            )}
        </div>
    )
}

export default NotificationBell
