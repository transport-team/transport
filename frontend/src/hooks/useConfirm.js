import { useCallback, useRef, useState } from 'react'

// 브라우저 기본 확인 창(window.confirm) 대신 공통 확인 창으로 묻는다 (#143)
//   const { confirm, confirmProps } = useConfirm()
//   if (!(await confirm({ title: '배차 확정', message: '…', confirmLabel: '확정', danger: false }))) return
//   화면 JSX 어딘가에 <ConfirmModal {...confirmProps} />
// 확인 → true, 취소 · Esc · 바깥 클릭 → false
const useConfirm = () => {
  const [request, setRequest] = useState(null)
  const resolveRef = useRef(null)

  const settle = useCallback((result) => {
    resolveRef.current?.(result)
    resolveRef.current = null
    setRequest(null)
  }, [])

  const confirm = useCallback((options) => new Promise((resolve) => {
    resolveRef.current?.(false)  // 이미 열린 창이 있으면 취소로 끝내고 새로 연다
    resolveRef.current = resolve
    setRequest(options)
  }), [])

  const onConfirm = useCallback(() => settle(true), [settle])
  const onCancel = useCallback(() => settle(false), [settle])

  return {
    confirm,
    confirmProps: { ...request, open: request !== null, onConfirm, onCancel },
  }
}

export default useConfirm
