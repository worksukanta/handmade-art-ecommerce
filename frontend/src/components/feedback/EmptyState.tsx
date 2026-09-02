import type { ReactNode } from 'react'

interface EmptyStateProps {
  title: string
  message: string
  action?: ReactNode
}

export function EmptyState({ title, message, action }: EmptyStateProps) {
  return (
    <section className="state-panel">
      <h2>{title}</h2>
      <p>{message}</p>
      {action}
    </section>
  )
}
