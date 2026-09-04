import { fireEvent, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CartPage } from '../pages/CartPage'
import { CheckoutPage } from '../pages/CheckoutPage'
import { accountService } from '../services/accountService'
import { checkoutService } from '../services/checkoutService'
import { renderWithRouter } from './helpers'

const updateItem = vi.fn().mockResolvedValue(undefined), removeItem = vi.fn().mockResolvedValue(undefined), clearCart = vi.fn().mockResolvedValue(undefined), resetCart = vi.fn()
let cart: any
vi.mock('../hooks/useCart', () => ({ useCart: () => ({ cart, isLoading: false, loadError: null, refresh: vi.fn(), updateItem, removeItem, clearCart, resetCart }) }))
vi.mock('../services/accountService', () => ({ accountService: { listAddresses: vi.fn() } }))
vi.mock('../services/checkoutService', () => ({ checkoutService: { validate: vi.fn(), createOrder: vi.fn() } }))
const fullCart = { cartId: 1, items: [{ itemId: 4, productId: 8, productName: 'Vase', unitPrice: 50, quantity: 1, subtotal: 50, addedAt: '' }], total: 50 }
const address = { id: 7, recipient_name: 'Asha', line1: '1 Main St', line2: null, city: 'Kolkata', state_province: 'WB', postal_code: '700001', country: 'India', phone: null, is_default: false, created_at: '', updated_at: '' }

describe('cart behavior', () => {
  beforeEach(() => { cart = fullCart })
  it('shows the empty state', () => { cart = { cartId: 1, items: [], total: 0 }; renderWithRouter(<CartPage />); expect(screen.getByRole('heading', { name: 'Your cart is empty' })).toBeInTheDocument() })
  it('updates quantity and removes an item', async () => { renderWithRouter(<CartPage />); fireEvent.change(screen.getByRole('spinbutton', { name: 'Quantity' }), { target: { value: '2' } }); await waitFor(() => expect(updateItem).toHaveBeenCalledWith(4, 2)); await userEvent.click(screen.getByRole('button', { name: 'Remove' })); expect(removeItem).toHaveBeenCalledWith(4) })
  it('does nothing when clear confirmation is declined', async () => { vi.spyOn(window, 'confirm').mockReturnValue(false); renderWithRouter(<CartPage />); await userEvent.click(screen.getByRole('button', { name: 'Clear cart' })); expect(clearCart).not.toHaveBeenCalled() })
})

describe('checkout behavior', () => {
  beforeEach(() => { cart = fullCart; vi.mocked(accountService.listAddresses).mockResolvedValue([address]); vi.mocked(checkoutService.validate).mockResolvedValue({ valid: true, items: [], subtotalAmount: 50, totalAmount: 50 }); vi.mocked(checkoutService.createOrder).mockResolvedValue({ orderId: 99 } as any) })
  function view() { return renderWithRouter(<Routes><Route path="/checkout" element={<CheckoutPage />} /><Route path="/checkout/success/:orderId" element={<p>Order created; payment pending</p>} /></Routes>, { route: '/checkout' }) }
  it('requires explicit address selection before validation', async () => { view(); expect(await screen.findByRole('button', { name: 'Validate checkout' })).toBeDisabled(); await userEvent.click(screen.getByRole('radio')); expect(screen.getByRole('button', { name: 'Validate checkout' })).toBeEnabled() })
  it('validates, enables order creation, and navigates without implying payment success', async () => { view(); await userEvent.click(await screen.findByRole('radio')); await userEvent.click(screen.getByRole('button', { name: 'Validate checkout' })); expect(checkoutService.validate).toHaveBeenCalledWith(7); await userEvent.click(await screen.findByRole('button', { name: 'Place order' })); expect(await screen.findByText('Order created; payment pending')).toBeInTheDocument(); expect(resetCart).toHaveBeenCalled() })
  it('shows a validation failure and keeps order creation disabled', async () => { vi.mocked(checkoutService.validate).mockRejectedValue(new Error('fail')); view(); await userEvent.click(await screen.findByRole('radio')); await userEvent.click(screen.getByRole('button', { name: 'Validate checkout' })); expect(await screen.findByRole('alert')).toHaveTextContent('unexpected error'); expect(screen.getByRole('button', { name: 'Place order' })).toBeDisabled() })
  it('prevents duplicate validation submissions while pending', async () => { vi.mocked(checkoutService.validate).mockReturnValue(new Promise(() => undefined)); view(); await userEvent.click(await screen.findByRole('radio')); const button = screen.getByRole('button', { name: 'Validate checkout' }); await userEvent.dblClick(button); expect(checkoutService.validate).toHaveBeenCalledTimes(1); expect(button).toBeDisabled() })
})
