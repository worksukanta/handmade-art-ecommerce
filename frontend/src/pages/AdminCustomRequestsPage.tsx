import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { StatusBadge } from '../components/commerce/StatusBadge'
import { EmptyState } from '../components/feedback/EmptyState'
import { ErrorState } from '../components/feedback/ErrorState'
import { LoadingState } from '../components/feedback/LoadingState'
import { customArtworkService } from '../services/customArtworkService'
import type { CustomRequestPage, CustomRequestStatus } from '../types/customArtwork'
import { normalizeApiError } from '../utils/apiError'
import { formatDateTime, humanizeStatus } from '../utils/format'

const statuses: CustomRequestStatus[] = ['REQUESTED', 'UNDER_REVIEW', 'QUOTED', 'CUSTOMER_APPROVAL_PENDING', 'APPROVED', 'ADVANCE_PAYMENT_PENDING', 'IN_PRODUCTION', 'COMPLETED', 'SHIPPED', 'DELIVERED', 'REJECTED', 'QUOTATION_EXPIRED', 'CANCELLED']

export function AdminCustomRequestsPage() {
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<CustomRequestStatus | ''>('')
  const [data, setData] = useState<CustomRequestPage | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const load = useCallback(async () => { setLoading(true); setError(null); try { setData(await customArtworkService.adminList(page, 20, status || undefined)) } catch (cause) { setError(normalizeApiError(cause).message) } finally { setLoading(false) } }, [page, status])
  useEffect(() => { void Promise.resolve().then(load) }, [load])
  const changeStatus = (value: string) => { setPage(0); setStatus(value as CustomRequestStatus | '') }

  return <section className="commerce-page"><div className="page-heading"><p className="eyebrow">Admin workflow</p><h1>Custom requests</h1><p>Review commissions, issue quotations, and manage production and delivery.</p></div><div className="request-filter"><label htmlFor="admin-request-status">Filter by status</label><select id="admin-request-status" value={status} onChange={(event) => changeStatus(event.target.value)}><option value="">All statuses</option>{statuses.map((value) => <option key={value} value={value}>{humanizeStatus(value)}</option>)}</select></div>{loading ? <LoadingState label="Loading customer requests" cards /> : error ? <ErrorState title="We couldn't load custom requests" message={error} onRetry={() => void load()} /> : !data?.content.length ? <EmptyState title="No custom requests found" message={status ? 'Choose another status or view all requests.' : 'Customer requests will appear here when submitted.'} /> : <><div className="request-list">{data.content.map((request) => <article className="request-card admin-request-card" key={request.id}><div><p className="order-card-label">Request #{request.id}</p><h2><Link to={`/admin/custom-requests/${request.id}`}>{request.productType}</Link></h2><p className="request-description">{request.description}</p></div><div><p className="order-card-label">Customer</p><p>User #{request.userId}</p></div><div><p className="order-card-label">Submitted</p><time dateTime={request.createdAt}>{formatDateTime(request.createdAt)}</time><p>Updated {formatDateTime(request.updatedAt)}</p></div><StatusBadge kind="request" value={request.status} /><Link className="button button-secondary" to={`/admin/custom-requests/${request.id}`}>Manage</Link></article>)}</div>{data.total_pages > 1 && <nav className="pagination" aria-label="Admin custom request pages"><button className="button button-secondary" disabled={data.page === 0} onClick={() => setPage(data.page - 1)}>Previous</button><span>Page {data.page + 1} of {data.total_pages}</span><button className="button button-secondary" disabled={data.page + 1 >= data.total_pages} onClick={() => setPage(data.page + 1)}>Next</button></nav>}</>}</section>
}
