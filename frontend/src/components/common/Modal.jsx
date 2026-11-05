// 공통 모달 (팀원 4 관리)
// open: boolean, onClose: () => void, title: string, children: ReactNode, size: sm | md | lg | xl
import { Fragment, useEffect } from 'react'
import { createPortal } from 'react-dom'

const Modal = ({ open, onClose, title, children, size = 'md' }) => {
  useEffect(() => {
    const handleEsc = (e) => e.key === 'Escape' && onClose()
    if (open) {
      document.addEventListener('keydown', handleEsc)
      document.body.style.overflow = 'hidden'
    }
    return () => {
      document.removeEventListener('keydown', handleEsc)
      document.body.style.overflow = ''
    }
  }, [open, onClose])

  if (!open) return null

  const sizes = {
    sm: 'max-w-md',
    md: 'max-w-lg',
    lg: 'max-w-2xl',
    xl: 'max-w-4xl',
  }

  return createPortal(
    <Fragment>
      <div
        className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4"
        onClick={onClose}
        role="presentation"
      />
      <div
        className={`${sizes[size]} w-full bg-surface rounded-xl shadow-xl overflow-hidden animate-fade-in`}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby={title ? 'modal-title' : undefined}
      >
        {title && (
          <div className="flex items-center justify-between px-6 py-4 border-b border-border">
            <h2 id="modal-title" className="text-lg font-semibold text-text">{title}</h2>
            <button
              type="button"
              onClick={onClose}
              className="p-1 rounded-lg text-text-muted hover:text-text hover:bg-gray-light transition-colors"
              aria-label="닫기"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        )}
        <div className="p-6">{children}</div>
      </div>
    </Fragment>
  )
}

export default Modal