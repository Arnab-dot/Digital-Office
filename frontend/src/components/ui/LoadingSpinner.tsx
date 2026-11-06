export interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg'
  label?: string
}

export function LoadingSpinner({ size = 'md', label = 'Loading' }: LoadingSpinnerProps) {
  const sizeClass = `spinner--${size}`

  return (
    <div className="loading-state">
      <span className={`spinner ${sizeClass}`} aria-label={label} />
    </div>
  )
}
