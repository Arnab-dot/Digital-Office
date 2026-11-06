import type { ReactNode } from 'react'

export interface BadgeProps {
  variant?: 'default' | 'success' | 'warning' | 'error' | 'info'
  children: ReactNode
  className?: string
}

export function Badge({ variant = 'default', children, className = '' }: BadgeProps) {
  const variantClass = variant !== 'default' ? `badge--${variant}` : ''
  const classes = ['badge', variantClass, className].filter(Boolean).join(' ')

  return <span className={classes}>{children}</span>
}
