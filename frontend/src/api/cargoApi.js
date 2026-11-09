import apiClient from './apiClient'

// 공개 API: 활성 화물 유형 목록 조회
export const getCargoTypes = async () => {
    const res = await apiClient.get('/api/cargo-types')

    return res.data
}
