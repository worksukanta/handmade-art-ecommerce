import type { ButtonHTMLAttributes } from 'react'

interface SubmitButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  isSubmitting: boolean
  idleLabel: string
  submittingLabel: string
}

export function SubmitButton({ idleLabel, isSubmitting, submittingLabel, ...buttonProps }: SubmitButtonProps) {
  return (
    <button {...buttonProps} type="submit" disabled={isSubmitting || buttonProps.disabled}>
      {isSubmitting ? submittingLabel : idleLabel}
    </button>
  )
}
