interface LoadingStateProps {
  label: string
  cards?: boolean
}

export function LoadingState({ label, cards = false }: LoadingStateProps) {
  return (
    <div className={cards ? 'loading-grid' : 'state-panel'} role="status" aria-live="polite">
      <span className="sr-only">{label}</span>
      {cards
        ? Array.from({ length: 6 }, (_, index) => <div className="skeleton-card" key={index} aria-hidden="true" />)
        : <div className="loading-pulse" aria-hidden="true" />}
    </div>
  )
}
