import type { ReactNode } from 'react'

interface ErrorStateProps {
  title: string
  message: string
  onRetry?: () => void
  action?: ReactNode
}

export function ErrorState({ title, message, onRetry, action }: ErrorStateProps) {
  return (
    <section className="state-panel" role="alert">
      <h2>{title}</h2>
      <p>{message}</p>
      {onRetry && <button className="button button-primary" type="button" onClick={onRetry}>Try again</button>}
      {action}
    </section>
  )
}
