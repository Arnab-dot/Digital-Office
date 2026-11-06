import type { HTMLAttributes, ReactNode } from 'react'

export interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode
  padding?: 'sm' | 'md' | 'lg'
}

export function Card({ children, padding = 'md', className = '', ...props }: CardProps) {
  const paddingClass = `card--${padding}`
  const classes = ['card', paddingClass, className].filter(Boolean).join(' ')

  return (
    <div className={classes} {...props}>
      {children}
    </div>
  )
}

export interface CardHeaderProps {
  title: string
  action?: ReactNode
  subtitle?: string
}

export function CardHeader({ title, action, subtitle }: CardHeaderProps) {
  return (
    <div className="card-header">
      <div>
        <h3 className="card-title">{title}</h3>
        {subtitle && <p className="card-subtitle">{subtitle}</p>}
      </div>
      {action && <div className="card-action">{action}</div>}
    </div>
  )
}
