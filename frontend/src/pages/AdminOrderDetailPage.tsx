import { type FormEvent, useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { StatusBadge } from '../components/commerce/StatusBadge'
import { ErrorState } from '../components/feedback/ErrorState'
import { LoadingState } from '../components/feedback/LoadingState'
import { adminCommerceService } from '../services/adminCommerceService'
import type { AdminOrder, Payment, Shipment } from '../types/admin'
import type { OrderStatus, ShipmentStatus } from '../types/commerce'
import { normalizeApiError } from '../utils/apiError'
import { formatCurrency, formatDate, formatDateTime, humanizeStatus } from '../utils/format'

const nextOrder: Partial<Record<OrderStatus, OrderStatus>> = {
  PENDING_PAYMENT: 'CONFIRMED',
  CONFIRMED: 'PROCESSING',
  PROCESSING: 'SHIPPED',
  SHIPPED: 'DELIVERED',
}

const nextShipment: Partial<Record<ShipmentStatus, ShipmentStatus>> = {
  PENDING: 'SHIPPED',
  SHIPPED: 'DELIVERED',
}

export function AdminOrderDetailPage() {
  const id = Number(useParams().id)
  const [order, setOrder] = useState<AdminOrder | null>(null)
  const [payments, setPayments] = useState<Payment[]>([])
  const [shipment, setShipment] = useState<Shipment | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [carrier, setCarrier] = useState('')
  const [tracking, setTracking] = useState('')
  const [estimate, setEstimate] = useState('')

  const load = useCallback(async () => {
    try {
      const [o, p] = await Promise.all([
        adminCommerceService.getOrder(id),
        adminCommerceService.getOrderPayments(id),
      ])
      setOrder(o)
      setPayments(p)
      try {
        setShipment(await adminCommerceService.getOrderShipment(id))
      } catch (e) {
        if (normalizeApiError(e).status === 404) setShipment(null)
        else throw e
      }
      setError(null)
    } catch (e) {
      setError(normalizeApiError(e).message)
    }
  }, [id])

  useEffect(() => {
    void Promise.resolve().then(load)
  }, [load])

  const mutate = async (action: () => Promise<unknown>) => {
    setBusy(true)
    setError(null)
    try {
      await action()
      await load()
    } catch (e) {
      const n = normalizeApiError(e)
      setError(n.status === 409 ? `This order changed on the server. ${n.message}` : n.message)
      if (n.status === 409) await load()
    } finally {
      setBusy(false)
    }
  }

  const createShipment = (e: FormEvent) => {
    e.preventDefault()
    void mutate(() => adminCommerceService.createOrderShipment(id, carrier || null, tracking || null, estimate || null))
  }

  if (!order && !error) return <LoadingState label="Loading order" />
  if (!order) {
    return <ErrorState title="We couldn't load this order" message={error ?? 'Order unavailable'} onRetry={() => void load()} />
  }

  const orderNext = nextOrder[order.status]
  const shipmentNext = shipment ? nextShipment[shipment.status] : undefined

  return (
    <section className="commerce-page">
      <Link className="back-link" to="/admin/orders">← Orders</Link>
      <div className="order-detail-heading">
        <p className="eyebrow">Admin order</p>
        <h1>Order #{order.orderId}</h1>
        <StatusBadge kind="order" value={order.status} />
      </div>
      {error && <p className="form-alert form-alert-error" role="alert">{error}</p>}
      <div className="order-detail-grid">
        <div className="order-detail-main">
          <section className="detail-panel">
            <h2>Customer and items</h2>
            <p>{order.customerEmail} · Customer #{order.customerId}</p>
            <ul className="order-items">
              {order.items.map((i) => (
                <li key={i.itemId}>
                  <div>
                    <strong>{i.productName}</strong>
                    <span>{i.quantity} × {formatCurrency(i.unitPrice)}</span>
                  </div>
                  <strong>{formatCurrency(i.lineTotal)}</strong>
                </li>
              ))}
            </ul>
            <dl className="totals-list">
              <div>
                <dt>Subtotal</dt>
                <dd>{formatCurrency(order.subtotalAmount)}</dd>
              </div>
              <div className="total-row">
                <dt>Total</dt>
                <dd>{formatCurrency(order.totalAmount)}</dd>
              </div>
            </dl>
          </section>
          <section className="detail-panel">
            <h2>Payments</h2>
            {payments.length ? (
              <ul className="payment-list">
                {payments.map((p) => (
                  <li key={p.paymentId}>
                    <div>
                      <strong>{formatCurrency(p.amount)}</strong>
                      <StatusBadge kind="payment" value={p.status} />
                    </div>
                    <p>{humanizeStatus(p.paymentPurpose)} · {p.paymentMethod}</p>
                    <small>{formatDateTime(p.initiatedAt)}</small>
                  </li>
                ))}
              </ul>
            ) : (
              <p>No payment attempts recorded.</p>
            )}
          </section>
          <section className="detail-panel">
            <h2>Shipment</h2>
            {shipment ? (
              <>
                <StatusBadge kind="shipment" value={shipment.status} />
                <dl className="request-details">
                  <div>
                    <dt>Carrier</dt>
                    <dd>{shipment.carrierName || '—'}</dd>
                  </div>
                  <div>
                    <dt>Tracking</dt>
                    <dd>{shipment.trackingReference || '—'}</dd>
                  </div>
                  <div>
                    <dt>Estimated delivery</dt>
                    <dd>{shipment.estimatedDeliveryDate ? formatDate(shipment.estimatedDeliveryDate) : '—'}</dd>
                  </div>
                </dl>
                {shipmentNext && (
                  <button
                    className="button button-primary"
                    disabled={busy}
                    onClick={() => {
                      if (confirm(`Mark shipment ${humanizeStatus(shipmentNext)}?`)) {
                        void mutate(() => adminCommerceService.setShipmentStatus(shipment.id, shipmentNext))
                      }
                    }}
                  >
                    Mark {humanizeStatus(shipmentNext)}
                  </button>
                )}
              </>
            ) : (
              <form className="stacked-form" onSubmit={createShipment}>
                <label>Carrier<input value={carrier} onChange={(e) => setCarrier(e.target.value)} /></label>
                <label>Tracking reference<input value={tracking} onChange={(e) => setTracking(e.target.value)} /></label>
                <label>Estimated delivery<input type="date" value={estimate} onChange={(e) => setEstimate(e.target.value)} /></label>
                <button className="button button-primary" disabled={busy} type="submit">
                  Create shipment
                </button>
              </form>
            )}
          </section>
        </div>
        <aside className="detail-panel delivery-panel">
          <h2>Delivery</h2>
          <address>
            <strong>{order.shipRecipientName}</strong>
            <br />
            {order.shipLine1}
            <br />
            {order.shipLine2 && (
              <>
                {order.shipLine2}
                <br />
              </>
            )}
            {order.shipCity}, {order.shipStateProvince} {order.shipPostalCode}
            <br />
            {order.shipCountry}
            {order.shipPhone && (
              <>
                <br />
                {order.shipPhone}
              </>
            )}
          </address>
          <p>Placed {formatDateTime(order.createdAt)}</p>
          {orderNext && (
            <button
              className="button button-primary"
              disabled={busy}
              onClick={() => {
                if (confirm(`Move order to ${humanizeStatus(orderNext)}?`)) {
                  void mutate(() => adminCommerceService.setOrderStatus(id, orderNext))
                }
              }}
            >
              Mark {humanizeStatus(orderNext)}
            </button>
          )}
        </aside>
      </div>
    </section>
  )
}
