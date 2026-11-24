import axios from 'axios'

// 백엔드 주소: frontend/.env의 VITE_API_HOST (예: http://localhost:8080)
export const API_SERVER_HOST = import.meta.env.VITE_API_HOST

// 로그인 없이 부르는 API용. 토큰이 필요한 API는 K단계에서 만드는 jwtAxios를 쓴다
const apiClient = axios.create({ baseURL: API_SERVER_HOST })

export default apiClient
