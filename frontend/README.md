# frontend

React 19 + Vite · React Router · Redux Toolkit · axios · Tailwind CSS v4

실행 방법은 루트 [README](../README.md)를 따른다: `cp .env.example .env` → `npm ci` → `npm run dev` (http://localhost:5173)

- 포트는 5173 고정이다. 사용 중이면 다른 포트로 뜨지 않고 멈춘다 (백엔드 CORS가 5173만 허용)
- `.env`를 바꾸면 `npm run dev`를 다시 실행해야 반영된다

## 폴더 (src/)

| 폴더 | 넣는 것 |
|---|---|
| `api/` | 백엔드 호출 함수. 로그인 없이 부르면 `apiClient`, 로그인이 필요하면 `util/jwtUtil.js`의 `jwtAxios` |
| `components/common/` | 공통 컴포넌트 (버튼 · 모달 · 표 · 페이지 번호 등) |
| `layouts/` | 역할별 레이아웃 |
| `pages/` | 화면. 역할별로 `customer/` · `driver/` · `admin/` |
| `router/` | 화면 경로. 공개는 `root.jsx`, 역할별은 `customerRouter.jsx` · `driverRouter.jsx` · `adminRouter.jsx` |
| `slices/` | Redux 상태 (로그인 등) |
| `hooks/` | 여러 화면이 같이 쓰는 훅 |
| `util/` | 쿠키 · JWT · 입력 형식(`formatUtil.js`) 같은 도구 |

## 로그인 · API 호출

- 공개 화면: `/` 메인 · `/login` 로그인 · `/signup` 회원가입 (고객 · 기사 + 약관 동의, 기사는 관리자 승인 후 로그인)
- 로그인 정보: `useCustomLogin()` → `loginState`(memberId · email · name · role), `isLogin`, `doLogout()`
- 로그인이 필요한 API는 `jwtAxios`로 부른다. 토큰을 자동으로 붙이고, 만료(401 `TOKEN_EXPIRED`)되면 재발급해서 다시 보낸다
  ```js
  import jwtAxios from '../util/jwtUtil'
  export const getMyShipments = async (params) => (await jwtAxios.get('/api/shipments/my', { params })).data
  ```
- 토큰과 회원 정보는 쿠키 `member`에 1일 동안 저장된다 (새로고침해도 로그인 유지)
- `/customer` · `/driver` · `/admin` 아래 화면은 로그인 + 역할이 맞아야 열린다 (`router/RequireAuth.jsx`). 화면만 막는 것이고 실제 권한은 서버가 검사한다
- 내 정보(연락처 · 비밀번호 변경): `/customer/me` · `/driver/me` · `/admin/me` — 세 역할이 `pages/MyPage.jsx`를 같이 쓰고, 오른쪽 위 이름을 누르면 이동한다
- 화면을 만들면: 역할별 router 파일에 경로를 추가하고, 레이아웃 메뉴(`layouts/*Layout.jsx`의 `MENUS`)에 `to`를 넣는다 → 공통 파일이라 PR로

## 입력 형식

- 연락처: `util/formatUtil.js`의 `formatPhone()`으로 숫자만 입력해도 `010-1234-5678`이 되게 하고, `PHONE_PATTERN`으로 검사한다 (서버 검사와 같은 규칙). 배송 신청 · 내 정보 수정에서도 같이 쓴다

## 공통 파일

직접 고치지 말고 PR로 관리자 승인을 받는다 ([docs/collaboration.md](../docs/collaboration.md))

- `package.json` · `router/` · `layouts/` → 팀원 1
- `components/common/` · Tailwind 설정(`src/index.css`) → 팀원 4
