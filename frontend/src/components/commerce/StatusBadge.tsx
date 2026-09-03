import { humanizeStatus } from '../../utils/format'

interface StatusBadgeProps { kind: 'order' | 'payment' | 'shipment' | 'request' | 'quotation'; value: string }

export function StatusBadge({ kind, value }: StatusBadgeProps) {
  return <span className={`status-badge status-${kind} status-${value.toLowerCase()}`}>{humanizeStatus(value)}</span>
}
