import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AdminProductsPage } from '../pages/AdminProductsPage'
import { AdminInventoryPage } from '../pages/AdminInventoryPage'
import { AdminProductFormPage } from '../pages/AdminProductFormPage'
import { AdminOrderDetailPage } from '../pages/AdminOrderDetailPage'
import { AdminProductDetailPage } from '../pages/AdminProductDetailPage'
import { adminCommerceService } from '../services/adminCommerceService'
import { admin, authValue, renderWithRouter } from './helpers'

vi.mock('../services/adminCommerceService', () => ({ adminCommerceService: { listProducts: vi.fn(), listInventory: vi.fn(), getInventory: vi.fn(), updateInventory: vi.fn(), listCategories: vi.fn(), createProduct: vi.fn(), getProduct: vi.fn(), updateProduct: vi.fn(), getOrder: vi.fn(), getOrderPayments: vi.fn(), getOrderShipment: vi.fn(), setOrderStatus: vi.fn(), createOrderShipment: vi.fn(), setShipmentStatus: vi.fn() } }))
const product = { id: 8, name: 'Clay Vase', price: 75, product_type: 'READY_MADE' as const, category_id: 2, category_name: 'Ceramics', primary_image: null, created_at: '', status: 'ACTIVE' as const }
const products = { content: [product], page: 0, size: 20, total_elements: 1, total_pages: 1 }

describe('admin commerce', () => {
  it('loads focused stock directly and prevents duplicate saves while pending', async () => {
    vi.mocked(adminCommerceService.getInventory).mockResolvedValue({ product_id: 8, quantity_on_hand: 6, updated_at: '2026-01-01T00:00:00Z' })
    let finish!: (value: { product_id: number; quantity_on_hand: number; updated_at: string }) => void
    vi.mocked(adminCommerceService.updateInventory).mockImplementation(() => new Promise((resolve) => { finish = resolve }))
    renderWithRouter(<AdminInventoryPage />, { route: '/admin/inventory?productId=8' })
    const input = await screen.findByLabelText('Quantity for product 8')
    expect(adminCommerceService.getInventory).toHaveBeenCalledWith(8)
    expect(adminCommerceService.listInventory).not.toHaveBeenCalled()
    await userEvent.clear(input)
    await userEvent.type(input, '12')
    const save = screen.getByRole('button', { name: 'Save changed' })
    await userEvent.dblClick(save)
    expect(adminCommerceService.updateInventory).toHaveBeenCalledTimes(1)
    expect(input).toBeDisabled()
    finish({ product_id: 8, quantity_on_hand: 12, updated_at: '2026-01-01T00:00:00Z' })
    await waitFor(() => expect(input).toBeEnabled())
    expect(save).toBeDisabled()
  })
  it('saves only dirty rows and retains partial successes and retryable failures', async () => {
    vi.mocked(adminCommerceService.listInventory).mockResolvedValue({ content: [8, 9, 10].map((id) => ({ product_id: id, quantity_on_hand: 6, updated_at: '2026-01-01T00:00:00Z' })), page: 0, size: 20, total_elements: 3, total_pages: 1 })
    vi.mocked(adminCommerceService.updateInventory).mockImplementation(async (id) => {
      if (id === 9) throw new Error('Stock update failed')
      return { product_id: id, quantity_on_hand: 7, updated_at: '2026-01-01T00:00:00Z' }
    })
    renderWithRouter(<AdminInventoryPage />)
    const first = await screen.findByLabelText('Quantity for product 8')
    const second = screen.getByLabelText('Quantity for product 9')
    const save = screen.getByRole('button', { name: 'Save changed' })
    expect(save).toBeDisabled()
    for (const input of [first, second]) { await userEvent.clear(input); await userEvent.type(input, '9') }
    expect(screen.getAllByText('Unsaved changes')).toHaveLength(2)
    await userEvent.click(save)
    await screen.findByText(/1 saved. 1 failed/)
    expect(adminCommerceService.updateInventory).toHaveBeenCalledTimes(2)
    expect(adminCommerceService.updateInventory).toHaveBeenCalledWith(8, 9)
    expect(adminCommerceService.updateInventory).toHaveBeenCalledWith(9, 9)
    expect(first).toHaveValue(7)
    expect(second).toHaveValue(9)
    expect(second).toBeEnabled()
    expect(screen.getAllByText('Unsaved changes')).toHaveLength(1)
    expect(screen.getByRole('alert')).toBeInTheDocument()
    vi.mocked(adminCommerceService.updateInventory).mockResolvedValue({ product_id: 9, quantity_on_hand: 9, updated_at: '2026-01-01T00:00:00Z' })
    await userEvent.click(save)
    await waitFor(() => expect(save).toBeDisabled())
    expect(screen.queryByText('Unsaved changes')).not.toBeInTheDocument()
  })
  it('shows ADMIN a focused inventory action from product detail', async () => {
    vi.mocked(adminCommerceService.getProduct).mockResolvedValue({ ...product, description: null, images: [], availability: { in_stock: false, quantity_on_hand: 0 }, related_products: [], updated_at: '2026-01-01T00:00:00Z' })
    renderWithRouter(<Routes><Route path="/admin/products/:id" element={<AdminProductDetailPage />} /></Routes>, { route: '/admin/products/8', auth: authValue(admin) })
    expect(await screen.findByRole('link', { name: 'Manage inventory' })).toHaveAttribute('href', '/admin/inventory?productId=8')
  })
  beforeEach(() => { vi.mocked(adminCommerceService.listProducts).mockResolvedValue(products); vi.mocked(adminCommerceService.listInventory).mockResolvedValue({ content: [{ product_id: 8, quantity_on_hand: 6, updated_at: '2026-01-01T00:00:00Z' }], page: 0, size: 20, total_elements: 1, total_pages: 1 }); vi.mocked(adminCommerceService.updateInventory).mockResolvedValue({ product_id: 8, quantity_on_hand: 9, updated_at: '2026-01-01T00:00:00Z' }); vi.mocked(adminCommerceService.listCategories).mockResolvedValue([{ id: 2, name: 'Ceramics', description: null, status: 'ACTIVE', created_at: '' }]); vi.mocked(adminCommerceService.createProduct).mockResolvedValue({ ...product, description: null, images: [], availability: { in_stock: false }, related_products: [], updated_at: '2026-01-01T00:00:00Z' }) })
  it('renders the ADMIN product list', async () => { renderWithRouter(<AdminProductsPage />); expect(await screen.findByText('Clay Vase')).toBeInTheDocument(); expect(screen.getByText('Active')).toBeInTheDocument() })
  it('renders authoritative inventory and submits an accepted value', async () => { renderWithRouter(<AdminInventoryPage />); const input = await screen.findByLabelText('Quantity for product 8'); expect(input).toHaveValue(6); await userEvent.clear(input); await userEvent.type(input, '9'); await userEvent.click(screen.getByRole('button', { name: 'Save' })); await waitFor(() => expect(adminCommerceService.updateInventory).toHaveBeenCalledWith(8, 9)) })
  it('rejects invalid inventory client-side', async () => { renderWithRouter(<AdminInventoryPage />); const input = await screen.findByLabelText('Quantity for product 8'); await userEvent.clear(input); await userEvent.type(input, '-1'); expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled() })
  it('navigates from successful creation to created product detail', async () => { renderWithRouter(<Routes><Route path="/admin/products/new" element={<AdminProductFormPage />} /><Route path="/admin/products/:id" element={<p>Created product detail with image management</p>} /></Routes>, { route: '/admin/products/new' }); await screen.findByRole('heading', { name: 'Create product' }); await userEvent.type(screen.getByLabelText('Name'), 'Clay Vase'); await userEvent.clear(screen.getByLabelText('Price')); await userEvent.type(screen.getByLabelText('Price'), '75'); await userEvent.click(screen.getByRole('button', { name: 'Create product' })); expect(await screen.findByText('Created product detail with image management')).toBeInTheDocument(); expect(adminCommerceService.createProduct).toHaveBeenCalled() })
  it('renders standard order and only supported forward controls', async () => { vi.mocked(adminCommerceService.getOrder).mockResolvedValue({ orderId: 44, customerId: 1, customerEmail: 'customer@example.com', status: 'CONFIRMED', shipRecipientName: 'Asha', shipLine1: '1 Main', shipLine2: null, shipCity: 'Kolkata', shipStateProvince: 'WB', shipPostalCode: '700001', shipCountry: 'India', shipPhone: null, subtotalAmount: 75, totalAmount: 75, items: [], createdAt: '2026-01-01T00:00:00Z', updatedAt: '' }); vi.mocked(adminCommerceService.getOrderPayments).mockResolvedValue([]); vi.mocked(adminCommerceService.getOrderShipment).mockResolvedValue({ id: 3, orderId: 44, customOrderRequestId: null, carrierName: 'DHL', trackingReference: 'DONE', status: 'DELIVERED', estimatedDeliveryDate: null, shippedAt: null, deliveredAt: '2026-01-02T00:00:00Z', createdAt: '' }); renderWithRouter(<Routes><Route path="/admin/orders/:id" element={<AdminOrderDetailPage />} /></Routes>, { route: '/admin/orders/44' }); expect(await screen.findByRole('heading', { name: 'Order #44' })).toBeInTheDocument(); expect(screen.getByRole('button', { name: 'Mark Processing' })).toBeInTheDocument(); expect(screen.queryByRole('button', { name: 'Mark Delivered' })).not.toBeInTheDocument(); expect(screen.queryByRole('button', { name: 'Create shipment' })).not.toBeInTheDocument() })
})
