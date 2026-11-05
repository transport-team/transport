import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    strictPort: true, // 백엔드 CORS가 5173만 허용하므로 포트가 사용 중이면 다른 포트로 뜨지 않고 멈춘다
  },
})
