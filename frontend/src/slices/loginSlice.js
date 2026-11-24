import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { loginPost } from '../api/memberApi'
import { getCookie, removeCookie, setCookie } from '../util/cookieUtil'

// 로그인 정보: 토큰은 쿠키에만 두고, 화면에 필요한 값만 상태로 가진다
const initState = { memberId: null, email: '', name: '', role: null }

// 새로고침해도 로그인이 유지되게 쿠키에서 읽는다
const loadMemberCookie = () => {
  const member = getCookie('member')
  if (!member) {
    return initState
  }
  return { memberId: member.memberId, email: member.email, name: member.name, role: member.role }
}

export const loginPostAsync = createAsyncThunk('loginPostAsync', async (loginParam, { rejectWithValue }) => {
  try {
    return await loginPost(loginParam)
  } catch (err) {
    // 서버 에러 응답({code, message})을 그대로 화면에 넘긴다
    return rejectWithValue(err.response?.data ?? { code: 'NETWORK_ERROR', message: '서버에 연결할 수 없어요. 백엔드가 실행 중인지 확인해 주세요.' })
  }
})

const loginSlice = createSlice({
  name: 'loginSlice',
  initialState: loadMemberCookie(),
  reducers: {
    logout: () => {
      removeCookie('member')
      return { ...initState, loggedOut: true }  // 보호 라우트가 로그인 화면 대신 메인으로 보내게 표시
    },
  },
  extraReducers: (builder) => {
    builder.addCase(loginPostAsync.fulfilled, (state, action) => {
      const member = action.payload
      setCookie('member', member, 1)  // 리프레시 토큰 유효 기간(1일)만큼
      return { memberId: member.memberId, email: member.email, name: member.name, role: member.role }
    })
  },
})

export const { logout } = loginSlice.actions

export default loginSlice.reducer
