import { useRef, type ReactNode } from 'react'
import { useDismiss } from '../../hooks/useDismiss'

export function NavigationGroup({ label, active, children }: { label: string; active: boolean; children: ReactNode }) {
  const ref = useRef<HTMLDetailsElement>(null)
  const close = (escape: boolean) => {
    if (!ref.current?.open) return
    ref.current.open = false
    if (escape) ref.current.querySelector('summary')?.focus()
  }
  useDismiss([ref], true, close)
  return <details ref={ref} className="nav-group" onKeyDown={(event) => {
    if (event.key === 'Escape' && ref.current?.open) {
      event.preventDefault()
      close(true)
    }
  }} onClick={(event) => {
    if ((event.target as HTMLElement).closest('a, button')) close(false)
  }}>
    <summary className={active ? 'is-active' : undefined} aria-current={active ? 'true' : undefined}>{label}</summary>
    <ul>{children}</ul>
  </details>
}
