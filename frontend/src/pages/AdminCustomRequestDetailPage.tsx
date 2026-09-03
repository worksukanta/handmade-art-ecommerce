import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { StatusBadge } from '../components/commerce/StatusBadge'
import { WorkflowTimeline } from '../components/commerce/WorkflowTimeline'
import { ReferenceImageList } from '../components/customArtwork/ReferenceImageList'
import { ErrorState } from '../components/feedback/ErrorState'
import { LoadingState } from '../components/feedback/LoadingState'
import { customArtworkService } from '../services/customArtworkService'
import type { Payment, Shipment } from '../types/commerce'
import type { CustomArtworkRequest, Quotation, QuotationCreateRequest, ShipmentCreateRequest } from '../types/customArtwork'
import { normalizeApiError } from '../utils/apiError'
import { formatCurrency, formatDate, formatDateTime, humanizeStatus } from '../utils/format'

type Mutation = 'review' | 'reject' | 'quotation' | 'production' | 'shipment' | 'shipment-status' | null

export function AdminCustomRequestDetailPage() {
  const id = Number(useParams().id)
  const [request, setRequest] = useState<CustomArtworkRequest | null>(null)
  const [quotation, setQuotation] = useState<Quotation | null>(null)
  const [payments, setPayments] = useState<Payment[]>([])
  const [shipment, setShipment] = useState<Shipment | null>(null)
  const [loading, setLoading] = useState(true)
  const [pageError, setPageError] = useState<string | null>(null)
  const [relatedError, setRelatedError] = useState<string | null>(null)
  const [mutation, setMutation] = useState<Mutation>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const load = useCallback(async () => {
    if (!Number.isSafeInteger(id) || id <= 0) { setPageError('This custom request address is invalid.'); setLoading(false); return }
    setLoading(true); setPageError(null); setRelatedError(null)
    try {
      const current = await customArtworkService.adminGet(id)
      setRequest(current)
      const [quotationResult, paymentsResult, shipmentResult] = await Promise.allSettled([
        customArtworkService.adminGetQuotation(id), customArtworkService.adminListPayments(id), customArtworkService.adminGetShipment(id),
      ])
      if (quotationResult.status === 'fulfilled') setQuotation(quotationResult.value)
      else { setQuotation(null); const error = normalizeApiError(quotationResult.reason); if (error.status !== 404) setRelatedError(error.message) }
      if (paymentsResult.status === 'fulfilled') setPayments(paymentsResult.value)
      else setRelatedError(normalizeApiError(paymentsResult.reason).message)
      if (shipmentResult.status === 'fulfilled') setShipment(shipmentResult.value)
      else { setShipment(null); const error = normalizeApiError(shipmentResult.reason); if (error.status !== 404) setRelatedError(error.message) }
    } catch (cause) { setPageError(normalizeApiError(cause).message) } finally { setLoading(false) }
  }, [id])
  useEffect(() => { void Promise.resolve().then(load) }, [load])

  const runAction = async (kind: Exclude<Mutation, null>, confirmation: string, action: () => Promise<unknown>, success: string) => {
    if (mutation || !window.confirm(confirmation)) return
    setMutation(kind); setActionError(null); setNotice(null)
    try { await action(); setNotice(success); await load() } catch (cause) { const error = normalizeApiError(cause); setActionError(error.status === 409 ? `${error.message} The request has been refreshed because its workflow state may have changed.` : error.message); await load() } finally { setMutation(null) }
  }

  if (loading) return <LoadingState label="Loading custom request management" />
  if (pageError || !request) return <ErrorState title="We couldn't load this custom request" message={pageError ?? 'Request not found.'} onRetry={() => void load()} action={<Link className="state-link" to="/admin/custom-requests">Back to custom requests</Link>} />

  return <section className="commerce-page"><Link className="back-link" to="/admin/custom-requests">← Admin custom requests</Link><div className="page-heading custom-detail-heading"><p className="eyebrow">Admin custom artwork</p><h1>Request #{request.id}</h1><p>Customer user #{request.userId} · submitted <time dateTime={request.createdAt}>{formatDateTime(request.createdAt)}</time> · updated <time dateTime={request.updatedAt}>{formatDateTime(request.updatedAt)}</time></p><StatusBadge kind="request" value={request.status} /></div>{notice && <p className="form-alert form-alert-success" role="status">{notice}</p>}{actionError && <p className="form-alert form-alert-error" role="alert">{actionError}</p>}{relatedError && <p className="form-alert form-alert-error" role="alert">Some related workflow data could not be loaded: {relatedError}</p>}<section className="detail-panel"><h2>Workflow state</h2><WorkflowTimeline status={request.status} /></section><div className="custom-detail-grid"><div className="custom-detail-main"><RequestPanel request={request} /><ReferencePanel request={request} /><ReviewPanel request={request} mutation={mutation} runAction={runAction} /><QuotationPanel request={request} quotation={quotation} mutation={mutation} runAction={runAction} /><PaymentsPanel payments={payments} /></div><aside className="admin-workflow-sidebar"><ProductionPanel request={request} mutation={mutation} runAction={runAction} /><ShipmentPanel request={request} shipment={shipment} mutation={mutation} runAction={runAction} /></aside></div></section>
}

function RequestPanel({ request }: { request: CustomArtworkRequest }) {
  const values = [['Product type', request.productType], ['Design theme', request.designTheme], ['Preferred colors', request.preferredColors], ['Dimensions/size', request.dimensionsSize], ['Budget range', request.budgetRange], ['Required delivery', request.requiredDeliveryDate ? formatDate(request.requiredDeliveryDate) : null], ['Additional instructions', request.additionalInstructions], ['Reviewed by user', request.reviewedByUserId ? `User #${request.reviewedByUserId}` : null], ['Review notes', request.reviewNotes]]
  return <section className="detail-panel"><h2>Customer requirements</h2><p className="request-description-full">{request.description}</p><dl className="request-details">{values.filter((entry) => entry[1]).map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl></section>
}

function ReferencePanel({ request }: { request: CustomArtworkRequest }) {
  return <section className="detail-panel"><h2>Reference images</h2>{request.images.length ? <ReferenceImageList images={request.images} /> : <p>No reference images were supplied.</p>}</section>
}

function ReviewPanel({ request, mutation, runAction }: { request: CustomArtworkRequest; mutation: Mutation; runAction: ActionRunner }) {
  const [notes, setNotes] = useState(request.reviewNotes ?? '')
  if (request.status !== 'REQUESTED' && request.status !== 'UNDER_REVIEW') return <section className="detail-panel"><h2>Review</h2><p>Review is complete. The authoritative decision is reflected by the current request state.</p>{request.reviewNotes && <p><strong>Notes:</strong> {request.reviewNotes}</p>}</section>
  const accept = () => runAction('review', 'Begin review of this customer request?', () => customArtworkService.adminReview(request.id, { decision: 'ACCEPT', notes: notes.trim() || undefined }), 'Request moved into review.')
  const reject = () => runAction('reject', 'Reject this request? This is a terminal workflow decision.', () => customArtworkService.adminReview(request.id, { decision: 'REJECT', notes: notes.trim() || undefined }), 'Request rejected.')
  return <section className="detail-panel"><h2>Review request</h2><label className="stacked-field" htmlFor="review-notes">Review notes (optional)<textarea id="review-notes" rows={3} value={notes} onChange={(event) => setNotes(event.target.value)} disabled={mutation !== null} /></label><div className="form-actions">{request.status === 'REQUESTED' && <button className="button button-primary" type="button" disabled={mutation !== null} onClick={() => void accept()}>{mutation === 'review' ? 'Starting review…' : 'Begin review'}</button>}{request.status === 'UNDER_REVIEW' && <button className="button button-secondary" type="button" disabled={mutation !== null} onClick={() => void reject()}>{mutation === 'reject' ? 'Rejecting…' : 'Reject request'}</button>}</div></section>
}

function QuotationPanel({ request, quotation, mutation, runAction }: { request: CustomArtworkRequest; quotation: Quotation | null; mutation: Mutation; runAction: ActionRunner }) {
  if (quotation) return <section className="detail-panel"><div className="section-heading-inline"><h2>Quotation</h2><StatusBadge kind="quotation" value={quotation.status} /></div><dl className="request-details"><div><dt>Quoted amount</dt><dd>{formatCurrency(quotation.quotedAmount)}</dd></div><div><dt>Fixed advance</dt><dd>{quotation.advanceAmount === null ? 'Not defined' : formatCurrency(quotation.advanceAmount)}</dd></div><div><dt>Expires</dt><dd>{formatDateTime(quotation.expiryAt)}</dd></div>{quotation.estimatedDeliveryDate && <div><dt>Estimated delivery</dt><dd>{formatDate(quotation.estimatedDeliveryDate)}</dd></div>}{quotation.notesTerms && <div><dt>Notes and terms</dt><dd>{quotation.notesTerms}</dd></div>}{quotation.decidedAt && <div><dt>Customer decision time</dt><dd>{formatDateTime(quotation.decidedAt)}</dd></div>}</dl><p className="detail-note">DEC-004 permits one quotation per request; it cannot be edited or recreated.</p></section>
  if (request.status !== 'UNDER_REVIEW') return <section className="detail-panel"><h2>Quotation</h2><p>{request.status === 'REQUESTED' ? 'Begin review before preparing a quotation.' : 'No quotation is associated with this request.'}</p></section>
  return <QuotationForm requestId={request.id} mutation={mutation} runAction={runAction} />
}

function QuotationForm({ requestId, mutation, runAction }: { requestId: number; mutation: Mutation; runAction: ActionRunner }) {
  const [form, setForm] = useState({ quotedAmount: '', advanceAmount: '', estimatedDeliveryDate: '', expiryAt: '', notesTerms: '' })
  const [error, setError] = useState<string | null>(null)
  const submit = (event: FormEvent) => { event.preventDefault(); const quotedAmount = Number(form.quotedAmount); const advanceAmount = Number(form.advanceAmount); if (!Number.isFinite(quotedAmount) || quotedAmount <= 0) { setError('Quoted amount must be greater than zero.'); return } if (!Number.isFinite(advanceAmount) || advanceAmount <= 0) { setError('Advance amount must be greater than zero.'); return } if (advanceAmount > quotedAmount) { setError('Advance amount cannot exceed the quoted amount.'); return } if (!form.expiryAt || new Date(form.expiryAt).getTime() <= Date.now()) { setError('Choose an expiry date and time in the future.'); return } setError(null); const payload: QuotationCreateRequest = { quotedAmount, advanceAmount, expiryAt: new Date(form.expiryAt).toISOString(), estimatedDeliveryDate: form.estimatedDeliveryDate || undefined, notesTerms: form.notesTerms.trim() || undefined }; void runAction('quotation', 'Create this quotation? The MVP permits only one quotation for this request.', () => customArtworkService.adminCreateQuotation(requestId, payload), 'Quotation created.') }
  const update = (field: keyof typeof form, value: string) => setForm((current) => ({ ...current, [field]: value }))
  return <section className="detail-panel"><h2>Create quotation</h2><p>Advance payment is a fixed amount defined by this quotation.</p><form className="admin-form-grid" onSubmit={submit}><label>Quoted amount<input type="number" min="0.01" step="0.01" required value={form.quotedAmount} onChange={(event) => update('quotedAmount', event.target.value)} /></label><label>Fixed advance amount<input type="number" min="0.01" step="0.01" required value={form.advanceAmount} onChange={(event) => update('advanceAmount', event.target.value)} /></label><label>Quotation expiry<input type="datetime-local" required value={form.expiryAt} onChange={(event) => update('expiryAt', event.target.value)} /></label><label>Estimated delivery (optional)<input type="date" value={form.estimatedDeliveryDate} onChange={(event) => update('estimatedDeliveryDate', event.target.value)} /></label><label className="full-field">Notes and terms (optional)<textarea rows={3} value={form.notesTerms} onChange={(event) => update('notesTerms', event.target.value)} /></label>{error && <p className="form-alert form-alert-error full-field" role="alert">{error}</p>}<button className="button button-primary" disabled={mutation !== null} type="submit">{mutation === 'quotation' ? 'Creating quotation…' : 'Create quotation'}</button></form></section>
}

function PaymentsPanel({ payments }: { payments: Payment[] }) { return <section className="detail-panel"><h2>Advance payments</h2>{payments.length === 0 ? <p>No payment attempts recorded.</p> : <ul className="payment-list">{payments.map((payment) => <li key={payment.paymentId}><div><StatusBadge kind="payment" value={payment.status} /><strong>{formatCurrency(payment.amount)}</strong></div><p>{humanizeStatus(payment.paymentPurpose)} · {payment.paymentMethod} · {formatDateTime(payment.initiatedAt)}</p>{payment.completedAt && <p>Completed {formatDateTime(payment.completedAt)}</p>}{payment.failureReason && <p>{payment.failureReason}</p>}</li>)}</ul>}</section> }

function ProductionPanel({ request, mutation, runAction }: { request: CustomArtworkRequest; mutation: Mutation; runAction: ActionRunner }) { return <section className="detail-panel"><h2>Production</h2><StatusBadge kind="request" value={request.status} /><p className="detail-note">Successful sandbox advance payment moves an approved request into production automatically.</p>{request.status === 'IN_PRODUCTION' && <button className="button button-primary" type="button" disabled={mutation !== null} onClick={() => void runAction('production', 'Mark production complete?', () => customArtworkService.adminUpdateStatus(request.id, 'COMPLETED'), 'Artwork marked complete.')}>{mutation === 'production' ? 'Updating…' : 'Mark production complete'}</button>}</section> }

function ShipmentPanel({ request, shipment, mutation, runAction }: { request: CustomArtworkRequest; shipment: Shipment | null; mutation: Mutation; runAction: ActionRunner }) {
  if (shipment) { const next = shipment.status === 'PENDING' ? 'SHIPPED' : shipment.status === 'SHIPPED' ? 'DELIVERED' : null; return <section className="detail-panel"><div className="section-heading-inline"><h2>Shipment</h2><StatusBadge kind="shipment" value={shipment.status} /></div><dl className="compact-details">{shipment.carrierName && <div><dt>Carrier</dt><dd>{shipment.carrierName}</dd></div>}{shipment.trackingReference && <div><dt>Tracking</dt><dd>{shipment.trackingReference}</dd></div>}{shipment.estimatedDeliveryDate && <div><dt>Estimated delivery</dt><dd>{formatDate(shipment.estimatedDeliveryDate)}</dd></div>}{shipment.shippedAt && <div><dt>Shipped</dt><dd>{formatDateTime(shipment.shippedAt)}</dd></div>}{shipment.deliveredAt && <div><dt>Delivered</dt><dd>{formatDateTime(shipment.deliveredAt)}</dd></div>}</dl>{next && <button className="button button-primary" type="button" disabled={mutation !== null} onClick={() => void runAction('shipment-status', `Mark this shipment ${humanizeStatus(next).toLowerCase()}?`, () => customArtworkService.adminUpdateShipmentStatus(shipment.id, next), `Shipment marked ${humanizeStatus(next).toLowerCase()}.`)}>{mutation === 'shipment-status' ? 'Updating…' : `Mark ${humanizeStatus(next)}`}</button>}</section> }
  if (request.status !== 'COMPLETED') return <section className="detail-panel"><h2>Shipment</h2><p>Create a shipment after production is complete.</p></section>
  return <ShipmentForm requestId={request.id} mutation={mutation} runAction={runAction} />
}

function ShipmentForm({ requestId, mutation, runAction }: { requestId: number; mutation: Mutation; runAction: ActionRunner }) { const [form, setForm] = useState({ carrierName: '', trackingReference: '', estimatedDeliveryDate: '' }); const submit = (event: FormEvent) => { event.preventDefault(); const payload: ShipmentCreateRequest = { customOrderRequestId: requestId, carrierName: form.carrierName.trim() || undefined, trackingReference: form.trackingReference.trim() || undefined, estimatedDeliveryDate: form.estimatedDeliveryDate || undefined }; void runAction('shipment', 'Create this internal shipment record?', () => customArtworkService.adminCreateShipment(payload), 'Shipment record created.') }; return <section className="detail-panel"><h2>Create shipment</h2><form className="stacked-form" onSubmit={submit}><label>Carrier (optional)<input value={form.carrierName} onChange={(event) => setForm({ ...form, carrierName: event.target.value })} /></label><label>Tracking reference (optional)<input value={form.trackingReference} onChange={(event) => setForm({ ...form, trackingReference: event.target.value })} /></label><label>Estimated delivery (optional)<input type="date" value={form.estimatedDeliveryDate} onChange={(event) => setForm({ ...form, estimatedDeliveryDate: event.target.value })} /></label><button className="button button-primary" disabled={mutation !== null} type="submit">{mutation === 'shipment' ? 'Creating…' : 'Create shipment'}</button></form></section> }

type ActionRunner = (kind: Exclude<Mutation, null>, confirmation: string, action: () => Promise<unknown>, success: string) => Promise<void>
