import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CataloguePage } from '../pages/CataloguePage'
import { ProductDetailPage } from '../pages/ProductDetailPage'
import { catalogueService } from '../services/catalogueService'
import type { ProductDetail } from '../types/catalogue'
import { authValue, customer, renderWithRouter } from './helpers'

vi.mock('../services/catalogueService', () => ({ catalogueService: { listProducts: vi.fn(), listCategories: vi.fn(), getProduct: vi.fn() }, resolveImageUrl: (url: string) => url }))
vi.mock('../hooks/useCart', () => ({ useCart: () => ({ addItem: vi.fn().mockResolvedValue(undefined) }) }))
const page = { content: [], page: 0, size: 12, total_elements: 0, total_pages: 0 }
const product = (product_type: ProductDetail['product_type']): ProductDetail => ({ id: 10, name: 'Ocean Study', description: 'Original', price: 125, product_type, category_id: 2, category_name: 'Paintings', images: [], availability: { in_stock: true }, related_products: [], created_at: '', updated_at: '' })

describe('catalogue search regression', () => {
  beforeEach(() => { vi.mocked(catalogueService.listProducts).mockResolvedValue(page); vi.mocked(catalogueService.listCategories).mockResolvedValue([]) })
  it('does not fetch again for equivalent empty Search or already-clear Clear', async () => {
    renderWithRouter(<CataloguePage />)
    await waitFor(() => expect(catalogueService.listProducts).toHaveBeenCalledTimes(1))
    await userEvent.click(screen.getByRole('button', { name: 'Search' }))
    await userEvent.click(screen.getAllByRole('button', { name: 'Clear filters' })[0])
    expect(catalogueService.listProducts).toHaveBeenCalledTimes(1)
  })
  it('fetches exactly once for an actual search change', async () => {
    renderWithRouter(<CataloguePage />)
    await waitFor(() => expect(catalogueService.listProducts).toHaveBeenCalledTimes(1))
    await userEvent.type(screen.getByLabelText('Search artwork'), 'ocean')
    await userEvent.click(screen.getByRole('button', { name: 'Search' }))
    await waitFor(() => expect(catalogueService.listProducts).toHaveBeenCalledTimes(2))
  })
})

describe('product purchase routing', () => {
  async function renderProduct(kind: ProductDetail['product_type']) {
    vi.mocked(catalogueService.getProduct).mockResolvedValue(product(kind))
    renderWithRouter(<Routes><Route path="/products/:id" element={<ProductDetailPage />} /></Routes>, { route: '/products/10', auth: authValue(customer) })
    await screen.findByRole('heading', { name: 'Ocean Study' })
  }
  it('offers add to cart for an in-stock READY_MADE product', async () => { await renderProduct('READY_MADE'); expect(screen.getByRole('button', { name: 'Add to cart' })).toBeInTheDocument() })
  it('routes CUSTOM_AVAILABLE to custom request and not cart', async () => { await renderProduct('CUSTOM_AVAILABLE'); expect(screen.getByRole('link', { name: 'Request custom artwork' })).toHaveAttribute('href', expect.stringContaining('/custom-requests/new')); expect(screen.queryByRole('button', { name: 'Add to cart' })).not.toBeInTheDocument() })
  it('does not expose purchase actions for PORTFOLIO_ONLY', async () => { await renderProduct('PORTFOLIO_ONLY'); expect(screen.queryByRole('button', { name: 'Add to cart' })).not.toBeInTheDocument(); expect(screen.getByText(/not available for standard purchase/i)).toBeInTheDocument() })
})
