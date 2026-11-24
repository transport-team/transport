import { configureStore } from '@reduxjs/toolkit'
import loginSlice from './slices/loginSlice'

// Redux 저장소 (상태가 늘어나면 reducer에 추가)
export default configureStore({
  reducer: {
    loginSlice,
  },
})
