import jwtAxios from '../util/jwtUtil'

// 내 알림 목록 (페이징) + 안 읽은 개수 (6-2)
// 응답: { page: PageResponseDTO, unreadCount: number }
export const getMyNotifications = async ({ page = 1, size = 10 } = {}) => {
    const res = await jwtAxios.get('/api/notifications', { params: { page, size } })

    return res.data
}

// 알림 읽음 처리 (본인만) — 204
export const markNotificationRead = async (notificationId) => {
    await jwtAxios.patch(`/api/notifications/${notificationId}/read`)

    return true
}
