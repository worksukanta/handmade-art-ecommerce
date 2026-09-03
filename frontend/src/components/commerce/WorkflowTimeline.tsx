import type { CustomRequestStatus } from '../../types/customArtwork'
import { humanizeStatus } from '../../utils/format'

const stages: CustomRequestStatus[] = ['REQUESTED', 'UNDER_REVIEW', 'QUOTED', 'APPROVED', 'IN_PRODUCTION', 'COMPLETED', 'SHIPPED', 'DELIVERED']
const terminal = new Set<CustomRequestStatus>(['REJECTED', 'QUOTATION_EXPIRED', 'CANCELLED'])

export function WorkflowTimeline({ status }: { status: CustomRequestStatus }) {
  if (terminal.has(status)) return <p className="workflow-terminal"><strong>Workflow closed:</strong> {humanizeStatus(status)}</p>
  const normalized = status === 'CUSTOMER_APPROVAL_PENDING' ? 'QUOTED' : status === 'ADVANCE_PAYMENT_PENDING' ? 'APPROVED' : status
  const current = stages.indexOf(normalized)
  return <ol className="workflow-timeline" aria-label="Custom artwork progress">{stages.map((stage, index) => <li className={index < current ? 'complete' : index === current ? 'current' : ''} key={stage} aria-current={index === current ? 'step' : undefined}><span aria-hidden="true">{index < current ? '✓' : index + 1}</span>{humanizeStatus(stage)}</li>)}</ol>
}
