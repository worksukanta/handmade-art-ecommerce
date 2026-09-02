import { forwardRef } from 'react'

interface FormErrorProps {
  message: string
  details?: string[]
}

export const FormError = forwardRef<HTMLDivElement, FormErrorProps>(
  function FormError({ details = [], message }, ref) {
    return (
      <div className="form-alert form-alert-error" ref={ref} role="alert" tabIndex={-1}>
        <p>{message}</p>
        {details.length > 0 && <ul>{details.map((detail) => <li key={detail}>{detail}</li>)}</ul>}
      </div>
    )
  },
)
