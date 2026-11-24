// 공통 입력 필드 (팀원 4 관리)
import { forwardRef } from 'react'

const Input = forwardRef((
  {
    label,
    error,
    id,
    className = '',
    ...props
  },
  ref
) => {
  const inputId = id || label?.replace(/\s+/g, '-').toLowerCase()

  return (
    <label className="block text-sm font-medium text-text" htmlFor={inputId}>
      {label && <span className="block mb-1">{label}</span>}
      <input
        ref={ref}
        id={inputId}
        className={`rounded-lg border px-3 py-2 text-text bg-surface placeholder-text-muted transition-colors
          ${error
            ? 'border-danger focus:border-danger focus:ring-2 focus:ring-danger-light'
            : 'border-border focus:border-primary focus:ring-2 focus:ring-primary-light'
          }
          disabled:opacity-50 disabled:cursor-not-allowed
          ${className}`}
        aria-invalid={error ? 'true' : 'false'}
        aria-describedby={error ? `${inputId}-error` : undefined}
        {...props}
      />
      {error && (
        <p id={`${inputId}-error`} className="mt-1 text-sm text-danger" role="alert">
          {error}
        </p>
      )}
    </label>
  )
})

Input.displayName = 'Input'
export default Input