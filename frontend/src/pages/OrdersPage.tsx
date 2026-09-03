import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { EmptyState } from '../components/feedback/EmptyState'
import { ErrorState } from '../components/feedback/ErrorState'
import { LoadingState } from '../components/feedback/LoadingState'
import { StatusBadge } from '../components/commerce/StatusBadge'
import { orderService } from '../services/orderService'
import type { OrderPage } from '../types/commerce'
import { normalizeApiError } from '../utils/apiError'
import { formatCurrency, formatDateTime } from '../utils/format'

export function OrdersPage() {
  const [pageIndex, setPageIndex] = useState(0)
  const [orders, setOrders] = useState<OrderPage | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try { setOrders(await orderService.list(pageIndex)) }
    catch (cause) { setError(normalizeApiError(cause).message) }
    finally { setLoading(false) }
  }, [pageIndex])

  useEffect(() => { void Promise.resolve().then(load) }, [load])
  if (loading) return <LoadingState label="Loading your orders" cards />
  if (error) return <ErrorState title="We couldn't load your orders" message={error} onRetry={() => void load()} />
  if (!orders || orders.content.length === 0) return <EmptyState title="You haven't placed any orders yet" message="Your completed checkout orders will appear here." action={<Link className="button button-primary state-link" to="/">Browse the catalogue</Link>} />

  const changePage = (nextPage: number) => { setLoading(true); setPageIndex(nextPage) }
  return <section className="commerce-page"><div className="page-heading"><p className="eyebrow">Purchase history</p><h1>Your orders</h1><p>{orders.total_elements} {orders.total_elements === 1 ? 'order' : 'orders'}, with totals and status supplied by the store.</p></div><div className="order-list">{orders.content.map((order) => <article className="order-card" key={order.orderId}><div><p className="order-card-label">Order</p><h2><Link to={`/orders/${order.orderId}`}>#{order.orderId}</Link></h2><p>Placed <time dateTime={order.createdAt}>{formatDateTime(order.createdAt)}</time></p></div><div><p className="order-card-label">Deliver to</p><strong>{order.shipRecipientName}</strong><p>{order.shipCity}, {order.shipCountry}</p></div><div><p className="order-card-label">Total</p><strong>{formatCurrency(order.totalAmount)}</strong></div><div><p className="order-card-label">Status</p><StatusBadge kind="order" value={order.status} /></div><Link className="button button-secondary order-card-action" to={`/orders/${order.orderId}`} aria-label={`View order ${order.orderId}`}>View order</Link></article>)}</div>{orders.total_pages > 1 && <nav className="pagination" aria-label="Order history pages"><button className="button button-secondary" type="button" disabled={orders.page === 0} onClick={() => changePage(orders.page - 1)}>Previous</button><span>Page {orders.page + 1} of {orders.total_pages}</span><button className="button button-secondary" type="button" disabled={orders.page + 1 >= orders.total_pages} onClick={() => changePage(orders.page + 1)}>Next</button></nav>}</section>
}
