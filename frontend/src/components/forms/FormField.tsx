import type { InputHTMLAttributes } from 'react'

interface FormFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> {
  id: string
  label: string
  error?: string
}

export function FormField({ error, id, label, ...inputProps }: FormFieldProps) {
  const errorId = `${id}-error`

  return (
    <div className="form-field">
      <label htmlFor={id}>{label}</label>
      <input
        {...inputProps}
        id={id}
        aria-describedby={error ? errorId : undefined}
        aria-invalid={error ? true : undefined}
      />
      {error && <span className="field-error" id={errorId}>{error}</span>}
    </div>
  )
}
