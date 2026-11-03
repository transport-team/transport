import { useEffect, useState } from 'react'
import { API_SERVER_HOST } from '../api/apiClient'
import { getHealth } from '../api/healthApi'

const HEALTH_URL = `${API_SERVER_HOST}/api/health`

// 연결 상태별 안내 문구 · 색
const STATUS = {
  checking: {
    title: '서버 확인 중...',
    detail: `${HEALTH_URL} 호출 중`,
    box: 'border-gray-200 bg-gray-50 text-gray-700',
  },
  ok: {
    title: '서버 연결 정상',
    detail: `${HEALTH_URL} → ok`,
    box: 'border-green-200 bg-green-50 text-green-800',
  },
  fail: {
    title: '서버 연결 실패',
    detail: '백엔드(8080)가 실행 중인지, frontend/.env의 VITE_API_HOST가 맞는지 확인하세요.',
    box: 'border-red-200 bg-red-50 text-red-800',
  },
  noEnv: {
    title: 'VITE_API_HOST 설정이 없어요',
    detail: 'frontend/.env.example을 .env로 복사한 뒤 npm run dev를 다시 실행하세요.',
    box: 'border-amber-200 bg-amber-50 text-amber-800',
  },
}

// 임시 메인 화면: 백엔드 연결 확인 (K단계에서 실제 메인 화면으로 바꾼다)
const MainPage = () => {
  const [health, setHealth] = useState('checking')

  useEffect(() => {
    if (!API_SERVER_HOST) return
    getHealth()
      .then((data) => setHealth(data.status === 'ok' ? 'ok' : 'fail'))
      .catch(() => setHealth('fail'))
  }, [])

  const status = STATUS[API_SERVER_HOST ? health : 'noEnv']

  return (
    <div className="mx-auto max-w-xl p-8">
      <h1 className="text-2xl font-bold">운송서비스</h1>
      <p className="mt-1 text-gray-500">화물 운송 접수 · 결제 · 배차 · 실시간 추적 · 정산</p>

      <div className={`mt-6 rounded-lg border p-4 ${status.box}`}>
        <p className="font-semibold">{status.title}</p>
        <p className="mt-1 break-all text-sm">{status.detail}</p>
      </div>
    </div>
  )
}

export default MainPage
