import { useState } from 'react'
import { Link } from 'react-router-dom'
import { EmptyState } from '../components/feedback/EmptyState'
import { ErrorState } from '../components/feedback/ErrorState'
import { LoadingState } from '../components/feedback/LoadingState'
import { useCart } from '../hooks/useCart'
import { normalizeApiError } from '../utils/apiError'

export function CartPage() {
  const { cart, clearCart, isLoading, loadError, refresh, removeItem, updateItem } = useCart()
  const [busyItem, setBusyItem] = useState<number | 'clear' | null>(null)
  const [error, setError] = useState<string | null>(null)
  const mutate = async (key: number | 'clear', action: () => Promise<void>) => { if (busyItem) return; setBusyItem(key); setError(null); try { await action() } catch (cause) { setError(normalizeApiError(cause).message) } finally { setBusyItem(null) } }
  if (isLoading && !cart) return <LoadingState label="Loading your cart" />
  if (!cart) return <ErrorState title="We couldn’t load your cart" message={error ?? loadError ?? 'Please try again.'} onRetry={() => void refresh()} />
  if (cart.items.length === 0) return <EmptyState title="Your cart is empty" message="Explore the catalogue to find a ready-made artwork." action={<Link className="button button-primary state-link" to="/">Browse catalogue</Link>} />
  return <section className="commerce-page"><div className="page-heading"><p className="eyebrow">Your selection</p><h1>Shopping cart</h1></div>{error && <p className="form-alert form-alert-error" role="alert">{error}</p>}<div className="cart-layout"><div className="cart-items">{cart.items.map((item) => <article className="cart-item" key={item.itemId}><div><Link to={`/products/${item.productId}`}><h2>{item.productName}</h2></Link><p>${Number(item.unitPrice).toFixed(2)} each</p></div><label>Quantity<input type="number" min="1" value={item.quantity} disabled={busyItem !== null} onChange={(event) => { const quantity = Number(event.target.value); if (Number.isInteger(quantity) && quantity >= 1) void mutate(item.itemId, () => updateItem(item.itemId, quantity)) }} /></label><strong>${Number(item.subtotal).toFixed(2)}</strong><button className="text-button" type="button" disabled={busyItem !== null} onClick={() => void mutate(item.itemId, () => removeItem(item.itemId))}>Remove</button></article>)}</div><aside className="order-summary"><h2>Order summary</h2><div><span>Total</span><strong>${Number(cart.total).toFixed(2)}</strong></div><p>Calculated by the server using current product prices.</p><Link className="button button-primary checkout-link" to="/checkout">Proceed to checkout</Link><button className="text-button" type="button" disabled={busyItem !== null} onClick={() => { if (window.confirm('Clear every item from your cart?')) void mutate('clear', clearCart) }}>Clear cart</button></aside></div></section>
}
