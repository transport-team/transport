# frontend

React 19 + Vite · React Router · Redux Toolkit · axios · Tailwind CSS v4

실행 방법은 루트 [README](../README.md)를 따른다: `cp .env.example .env` → `npm ci` → `npm run dev` (http://localhost:5173)

- 포트는 5173 고정이다. 사용 중이면 다른 포트로 뜨지 않고 멈춘다 (백엔드 CORS가 5173만 허용)
- `.env`를 바꾸면 `npm run dev`를 다시 실행해야 반영된다

## 폴더 (src/)

| 폴더 | 넣는 것 |
|---|---|
| `api/` | 백엔드 호출 함수. `apiClient.js`의 axios를 쓴다 |
| `components/common/` | 공통 컴포넌트 (버튼 · 모달 · 표 · 페이지 번호 등) |
| `layouts/` | 역할별 레이아웃 |
| `pages/` | 화면. 역할별로 `customer/` · `driver/` · `admin/` |
| `router/` | 화면 경로 (`root.jsx`) |
| `slices/` | Redux 상태 (로그인 등) |
| `hooks/` | 여러 화면이 같이 쓰는 훅 |
| `util/` | 쿠키 · JWT 같은 도구 |

## 공통 파일

직접 고치지 말고 PR로 관리자 승인을 받는다 ([docs/collaboration.md](../docs/collaboration.md))

- `package.json` · `router/root.jsx` · `layouts/` → 팀원 1
- `components/common/` · Tailwind 설정(`src/index.css`) → 팀원 4
