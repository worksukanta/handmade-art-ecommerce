import { AxiosError, type AxiosResponse } from 'axios'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { OrderDetailPage } from '../pages/OrderDetailPage'
import { orderService } from '../services/orderService'
import { paymentService } from '../services/paymentService'
import type { Order, Shipment } from '../types/commerce'
import { renderWithRouter } from './helpers'

vi.mock('../services/orderService', () => ({ orderService: { get: vi.fn(), getShipment: vi.fn() } }))
vi.mock('../services/paymentService', () => ({ paymentService: { listForOrder: vi.fn(), initiate: vi.fn() } }))
const apiError = (status: number, message: string) => new AxiosError('x', undefined, undefined, undefined, { status, statusText: '', headers: {}, config: { headers: {} }, data: { status, message } } as AxiosResponse)
const base: Order = { orderId: 12, status: 'PENDING_PAYMENT', shipRecipientName: 'Asha', shipLine1: '1 Main', shipLine2: null, shipCity: 'Kolkata', shipStateProvince: 'WB', shipPostalCode: '700001', shipCountry: 'India', shipPhone: null, subtotalAmount: 90, totalAmount: 90, items: [{ itemId: 1, productId: 2, productName: 'Vase', unitPrice: 90, quantity: 1, lineTotal: 90 }], createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' }
const shipment: Shipment = { id: 2, orderId: 12, customOrderRequestId: null, carrierName: 'DHL', trackingReference: 'TRACK-1', status: 'SHIPPED', estimatedDeliveryDate: '2026-09-10', shippedAt: '2026-09-01T00:00:00Z', deliveredAt: null, createdAt: '' }

describe('order, payment, and shipment UI', () => {
  beforeEach(() => { vi.mocked(orderService.get).mockResolvedValue(base); vi.mocked(paymentService.listForOrder).mockResolvedValue([]); vi.mocked(orderService.getShipment).mockRejectedValue(apiError(404, 'missing')) })
  const view = () => renderWithRouter(<Routes><Route path="/orders/:id" element={<OrderDetailPage />} /></Routes>, { route: '/orders/12' })
  it('renders authoritative status, payment action, and absent-shipment state', async () => { view(); expect(await screen.findByRole('heading', { name: 'Order #12' })).toBeInTheDocument(); expect(screen.getByText(/Pending payment/)).toBeInTheDocument(); expect(screen.getByRole('button', { name: /Pay/ })).toBeInTheDocument(); expect(await screen.findByText(/Not yet shipped/)).toBeInTheDocument() })
  it('hides payment action when the order is not payable', async () => { vi.mocked(orderService.get).mockResolvedValue({ ...base, status: 'CONFIRMED' }); view(); await screen.findByRole('heading', { name: 'Order #12' }); expect(screen.queryByRole('button', { name: /Pay/ })).not.toBeInTheDocument() })
  it('renders shipment information when present', async () => { vi.mocked(orderService.getShipment).mockResolvedValue(shipment); view(); expect(await screen.findByText('TRACK-1')).toBeInTheDocument(); expect(screen.getByText('DHL')).toBeInTheDocument() })
  it('shows a useful 409 conflict and refreshes authoritative order/payment state', async () => { vi.mocked(paymentService.initiate).mockRejectedValue(apiError(409, 'not payable')); view(); await userEvent.click(await screen.findByRole('button', { name: /Pay/ })); expect(await screen.findByRole('alert')).toHaveTextContent('no longer payable'); await waitFor(() => expect(orderService.get).toHaveBeenCalledTimes(2)); expect(paymentService.listForOrder).toHaveBeenCalledTimes(2) })
})
