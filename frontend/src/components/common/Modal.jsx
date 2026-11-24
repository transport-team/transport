// 공통 모달 (팀원 4 관리)
// open: boolean, onClose: () => void, title: string, children: ReactNode, size: sm | md | lg | xl
// 어두운 배경 안에 창을 넣어 가운데에 띄운다 — 배경을 누르거나 Esc를 누르면 닫히고, 창 안을 눌러서는 닫히지 않는다
// (11-23 고침: 창이 배경 밖에 있어 가려지던 것, createPortal에 넣을 곳이 없어 열면 오류가 나던 것 — #110, #94)
import { useEffect, useId } from 'react'
import { createPortal } from 'react-dom'

const SIZES = {
  sm: 'max-w-md',
  md: 'max-w-lg',
  lg: 'max-w-2xl',
  xl: 'max-w-4xl',
}

const Modal = ({ open, onClose, title, children, size = 'md' }) => {
  const titleId = useId()  // 창이 여러 개여도 제목 id가 겹치지 않게

  useEffect(() => {
    if (!open) return undefined
    const handleEsc = (e) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handleEsc)
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', handleEsc)
      document.body.style.overflow = ''
    }
  }, [open, onClose])

  if (!open) return null

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      onClick={onClose}
      role="presentation"
    >
      <div
        className={`${SIZES[size] ?? SIZES.md} max-h-[90vh] w-full overflow-y-auto rounded-xl bg-surface shadow-xl`}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby={title ? titleId : undefined}
      >
        {title && (
          <div className="flex items-center justify-between border-b border-border px-6 py-4">
            <h2 id={titleId} className="text-lg font-semibold text-text">{title}</h2>
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg p-1 text-text-muted transition-colors hover:bg-gray-light hover:text-text"
              aria-label="닫기"
            >
              <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        )}
        <div className="p-6">{children}</div>
      </div>
    </div>,
    document.body
  )
}

export default Modal
