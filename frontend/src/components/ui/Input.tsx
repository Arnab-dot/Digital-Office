import type { InputHTMLAttributes } from 'react'

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  helpText?: string
}

export function Input({ label, error, helpText, id, className = '', ...props }: InputProps) {
  const inputId = id || label?.toLowerCase().replace(/\s+/g, '-')

  return (
    <div className="input-group">
      {label && <label htmlFor={inputId}>{label}</label>}
      <input
        id={inputId}
        className={`input ${className}`}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={error ? `${inputId}-error` : helpText ? `${inputId}-help` : undefined}
        {...props}
      />
      {error && (
        <span id={`${inputId}-error`} className="input-error" role="alert">
          {error}
        </span>
      )}
      {helpText && !error && (
        <span id={`${inputId}-help`} className="input-help">
          {helpText}
        </span>
      )}
    </div>
  )
}

export interface TextAreaProps extends InputHTMLAttributes<HTMLTextAreaElement> {
  label?: string
  error?: string
  helpText?: string
  rows?: number
}

export function TextArea({ label, error, helpText, id, className = '', rows = 4, ...props }: TextAreaProps) {
  const inputId = id || label?.toLowerCase().replace(/\s+/g, '-')

  return (
    <div className="input-group">
      {label && <label htmlFor={inputId}>{label}</label>}
      <textarea
        id={inputId}
        className={`input ${className}`}
        rows={rows}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={error ? `${inputId}-error` : helpText ? `${inputId}-help` : undefined}
        {...props}
      />
      {error && (
        <span id={`${inputId}-error`} className="input-error" role="alert">
          {error}
        </span>
      )}
      {helpText && !error && (
        <span id={`${inputId}-help`} className="input-help">
          {helpText}
        </span>
      )}
    </div>
  )
}

export interface SelectProps extends InputHTMLAttributes<HTMLSelectElement> {
  label?: string
  error?: string
  helpText?: string
  options: Array<{ value: string; label: string }>
}

export function Select({ label, error, helpText, id, className = '', options, ...props }: SelectProps) {
  const inputId = id || label?.toLowerCase().replace(/\s+/g, '-')

  return (
    <div className="input-group">
      {label && <label htmlFor={inputId}>{label}</label>}
      <select
        id={inputId}
        className={`input ${className}`}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={error ? `${inputId}-error` : helpText ? `${inputId}-help` : undefined}
        {...props}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error && (
        <span id={`${inputId}-error`} className="input-error" role="alert">
          {error}
        </span>
      )}
      {helpText && !error && (
        <span id={`${inputId}-help`} className="input-help">
          {helpText}
        </span>
      )}
    </div>
  )
}
