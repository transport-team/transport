// 공통 버튼 (팀원 4 관리)
// variant: primary | danger | outline | ghost
// size: sm | md | lg
import { forwardRef } from 'react'

const Button = forwardRef((
  {
    children,
    variant = 'primary',
    size = 'md',
    disabled = false,
    loading = false,
    type = 'button',
    className = '',
    ...props
  },
  ref
) => {
  const base = 'inline-flex min-w-0 items-center justify-center rounded-xl font-semibold transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary disabled:cursor-not-allowed disabled:opacity-50'

  const variants = {
    primary: 'bg-primary text-white hover:bg-primary-hover focus:ring-primary',
    danger: 'bg-danger text-white hover:bg-danger-hover focus:ring-danger',
    outline: 'bg-transparent border-2 border-primary text-primary hover:bg-primary-light focus:ring-primary',
    ghost: 'bg-transparent text-primary hover:bg-primary-light focus:ring-primary',
  }

  const sizes = {
    sm: 'min-h-9 px-3 py-1.5 text-sm gap-1.5',
    md: 'min-h-10 px-4 py-2 text-base gap-2',
    lg: 'min-h-12 px-6 py-3 text-lg gap-2.5',
  }

  return (
    <button
      ref={ref}
      type={type}
      disabled={disabled || loading}
      className={`${base} ${variants[variant]} ${sizes[size]} ${className}`}
      {...props}
    >
      {loading && (
        <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" aria-hidden="true">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
      )}
      {children}
    </button>
  )
})

Button.displayName = 'Button'
export default Button