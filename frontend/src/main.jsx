import { EventSourcePolyfill } from 'event-source-polyfill'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Provider } from 'react-redux'
import './index.css'
import App from './App.jsx'
import store from './store'

// 실시간 알림(SSE)에 로그인 토큰을 붙이려고 쓴다 — 브라우저 기본 EventSource는 Authorization 헤더를 못 붙인다
// hooks/useNotification(팀원 4)이 window.EventSourcePolyfill을 찾는다. 훅이 import로 바뀌면 이 줄은 지운다
window.EventSourcePolyfill = EventSourcePolyfill

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <Provider store={store}>
      <App />
    </Provider>
  </StrictMode>,
)
