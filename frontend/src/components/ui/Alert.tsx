import { AlertCircle, CheckCircle2, Info, XCircle } from 'lucide-react'
import type { ReactNode } from 'react'

export interface AlertProps {
  variant?: 'info' | 'success' | 'warning' | 'error'
  title?: string
  children: ReactNode
  onClose?: () => void
}

const icons = {
  info: Info,
  success: CheckCircle2,
  warning: AlertCircle,
  error: XCircle,
}

export function Alert({ variant = 'info', title, children, onClose }: AlertProps) {
  const Icon = icons[variant]

  return (
    <div className={`alert alert--${variant}`} role="alert">
      <Icon size={20} className="alert-icon" aria-hidden="true" />
      <div className="alert-content">
        {title && <div className="alert-title">{title}</div>}
        <div className="alert-message">{children}</div>
      </div>
      {onClose && (
        <button
          type="button"
          onClick={onClose}
          className="alert-close"
          aria-label="Close alert"
        >
          <XCircle size={16} />
        </button>
      )}
    </div>
  )
}
