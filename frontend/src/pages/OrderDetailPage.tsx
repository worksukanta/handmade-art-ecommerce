import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { StatusBadge } from '../components/commerce/StatusBadge'
import { ErrorState } from '../components/feedback/ErrorState'
import { LoadingState } from '../components/feedback/LoadingState'
import { orderService } from '../services/orderService'
import { paymentService } from '../services/paymentService'
import type { Order, Payment, Shipment } from '../types/commerce'
import { normalizeApiError } from '../utils/apiError'
import { formatCurrency, formatDate, formatDateTime, humanizeStatus } from '../utils/format'

type ShipmentState = Shipment | 'not-created' | null

export function OrderDetailPage() {
  const orderId = Number(useParams().id)
  const [order, setOrder] = useState<Order | null>(null)
  const [payments, setPayments] = useState<Payment[]>([])
  const [shipment, setShipment] = useState<ShipmentState>(null)
  const [loading, setLoading] = useState(true)
  const [paymentLoading, setPaymentLoading] = useState(true)
  const [shipmentLoading, setShipmentLoading] = useState(true)
  const [pageError, setPageError] = useState<string | null>(null)
  const [paymentError, setPaymentError] = useState<string | null>(null)
  const [shipmentError, setShipmentError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [paymentNotice, setPaymentNotice] = useState<string | null>(null)

  const loadOrder = useCallback(async () => {
    if (!Number.isInteger(orderId) || orderId <= 0) { setPageError('This order address is invalid.'); setLoading(false); return }
    setLoading(true); setPageError(null)
    try { setOrder(await orderService.get(orderId)) }
    catch (cause) { setPageError(normalizeApiError(cause).message) }
    finally { setLoading(false) }
  }, [orderId])

  const loadPayments = useCallback(async () => {
    if (!Number.isInteger(orderId) || orderId <= 0) return
    setPaymentLoading(true); setPaymentError(null)
    try { setPayments(await paymentService.listForOrder(orderId)) }
    catch (cause) { setPaymentError(normalizeApiError(cause).message) }
    finally { setPaymentLoading(false) }
  }, [orderId])

  const loadShipment = useCallback(async () => {
    if (!Number.isInteger(orderId) || orderId <= 0) return
    setShipmentLoading(true); setShipmentError(null)
    try { setShipment(await orderService.getShipment(orderId)) }
    catch (cause) { const error = normalizeApiError(cause); if (error.status === 404) setShipment('not-created'); else setShipmentError(error.message) }
    finally { setShipmentLoading(false) }
  }, [orderId])

  useEffect(() => { void Promise.resolve().then(() => { void loadOrder(); void loadPayments(); void loadShipment() }) }, [loadOrder, loadPayments, loadShipment])

  const pay = async () => {
    if (!order || order.status !== 'PENDING_PAYMENT' || submitting) return
    setSubmitting(true); setPaymentError(null); setPaymentNotice(null)
    try {
      const payment = await paymentService.initiate(order.orderId, { paymentMethod: 'SANDBOX' })
      setPaymentNotice(payment.status === 'SUCCESS' ? 'Payment succeeded and your order is confirmed.' : `Payment status: ${humanizeStatus(payment.status)}.`)
      await Promise.all([loadOrder(), loadPayments()])
    } catch (cause) {
      const error = normalizeApiError(cause)
      setPaymentError(error.status === 409 ? 'This order is no longer payable. Its status may already have changed; refresh the order details.' : error.message)
      if (error.status === 409) await Promise.all([loadOrder(), loadPayments()])
    } finally { setSubmitting(false) }
  }

  if (loading) return <LoadingState label="Loading order details" />
  if (pageError || !order) return <ErrorState title="We couldn't load this order" message={pageError ?? 'Order not found.'} onRetry={() => void loadOrder()} action={<Link className="state-link" to="/orders">Back to orders</Link>} />

  return <section className="commerce-page"><Link className="back-link" to="/orders">← Back to orders</Link><div className="page-heading order-detail-heading"><p className="eyebrow">Order details</p><h1>Order #{order.orderId}</h1><p>Placed <time dateTime={order.createdAt}>{formatDateTime(order.createdAt)}</time></p><StatusBadge kind="order" value={order.status} /></div><div className="order-detail-grid"><div className="order-detail-main"><section className="detail-panel"><h2>Items</h2><p className="detail-note">Names and prices below are purchase-time snapshots.</p><ul className="order-items">{order.items.map((item) => <li key={item.itemId}><div><strong>{item.productName}</strong><span>{formatCurrency(item.unitPrice)} × {item.quantity}</span></div><strong>{formatCurrency(item.lineTotal)}</strong></li>)}</ul><dl className="totals-list"><div><dt>Subtotal</dt><dd>{formatCurrency(order.subtotalAmount)}</dd></div><div className="total-row"><dt>Total</dt><dd>{formatCurrency(order.totalAmount)}</dd></div></dl></section><PaymentSection order={order} payments={payments} loading={paymentLoading} error={paymentError} notice={paymentNotice} submitting={submitting} onPay={pay} onRetry={loadPayments} /><ShipmentSection shipment={shipment} loading={shipmentLoading} error={shipmentError} onRetry={loadShipment} /></div><aside className="detail-panel delivery-panel"><h2>Delivery address</h2><address><strong>{order.shipRecipientName}</strong><br />{order.shipLine1}<br />{order.shipLine2 && <>{order.shipLine2}<br /></>}{order.shipCity}, {order.shipStateProvince} {order.shipPostalCode}<br />{order.shipCountry}{order.shipPhone && <><br />{order.shipPhone}</>}</address></aside></div></section>
}

function PaymentSection({ order, payments, loading, error, notice, submitting, onPay, onRetry }: { order: Order; payments: Payment[]; loading: boolean; error: string | null; notice: string | null; submitting: boolean; onPay: () => Promise<void>; onRetry: () => Promise<void> }) {
  return <section className="detail-panel"><div className="section-heading-inline"><div><p className="eyebrow">Server-authoritative amount</p><h2>Payment</h2></div>{order.status === 'PENDING_PAYMENT' && <button className="button button-primary" type="button" disabled={submitting} onClick={() => void onPay()}>{submitting ? 'Processing…' : `Pay ${formatCurrency(order.totalAmount)}`}</button>}</div><p>No card or banking details are collected. This uses the approved provider-agnostic sandbox flow.</p>{notice && <p className="form-alert form-alert-success" role="status">{notice}</p>}{error && <div className="form-alert form-alert-error" role="alert"><p>{error}</p><button className="text-button" type="button" onClick={() => void onRetry()}>Refresh payments</button></div>}{loading ? <p role="status">Loading payment status…</p> : payments.length === 0 ? <p>No payment attempts yet.</p> : <ol className="payment-list">{payments.map((payment) => <li key={payment.paymentId}><div><StatusBadge kind="payment" value={payment.status} /><strong>{formatCurrency(payment.amount)}</strong></div><dl className="compact-details"><div><dt>Method</dt><dd>{payment.paymentMethod}</dd></div><div><dt>Purpose</dt><dd>{humanizeStatus(payment.paymentPurpose)}</dd></div><div><dt>Initiated</dt><dd><time dateTime={payment.initiatedAt}>{formatDateTime(payment.initiatedAt)}</time></dd></div>{payment.completedAt && <div><dt>Completed</dt><dd><time dateTime={payment.completedAt}>{formatDateTime(payment.completedAt)}</time></dd></div>}{payment.providerTransactionReference && <div><dt>Reference</dt><dd>{payment.providerTransactionReference}</dd></div>}{payment.status === 'FAILED' && payment.failureReason && <div><dt>Failure reason</dt><dd>{payment.failureReason}</dd></div>}</dl></li>)}</ol>}</section>
}

function ShipmentSection({ shipment, loading, error, onRetry }: { shipment: ShipmentState; loading: boolean; error: string | null; onRetry: () => Promise<void> }) {
  return <section className="detail-panel"><h2>Shipment and tracking</h2>{loading ? <p role="status">Loading shipment information…</p> : error ? <div className="form-alert form-alert-error" role="alert"><p>{error}</p><button className="text-button" type="button" onClick={() => void onRetry()}>Try shipment again</button></div> : shipment === 'not-created' || !shipment ? <p>Not yet shipped. Tracking will appear here after a shipment is created.</p> : <><StatusBadge kind="shipment" value={shipment.status} /><dl className="compact-details shipment-details">{shipment.carrierName && <div><dt>Carrier</dt><dd>{shipment.carrierName}</dd></div>}{shipment.trackingReference && <div><dt>Tracking reference</dt><dd>{shipment.trackingReference}</dd></div>}{shipment.estimatedDeliveryDate && <div><dt>Estimated delivery</dt><dd><time dateTime={shipment.estimatedDeliveryDate}>{formatDate(shipment.estimatedDeliveryDate)}</time></dd></div>}{shipment.shippedAt && <div><dt>Shipped</dt><dd><time dateTime={shipment.shippedAt}>{formatDateTime(shipment.shippedAt)}</time></dd></div>}{shipment.deliveredAt && <div><dt>Delivered</dt><dd><time dateTime={shipment.deliveredAt}>{formatDateTime(shipment.deliveredAt)}</time></dd></div>}</dl></>}</section>
}
