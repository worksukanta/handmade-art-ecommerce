import { fireEvent, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CustomRequestDetailPage } from '../pages/CustomRequestDetailPage'
import { AdminCustomRequestDetailPage } from '../pages/AdminCustomRequestDetailPage'
import { customArtworkService } from '../services/customArtworkService'
import type { CustomArtworkRequest, Quotation } from '../types/customArtwork'
import { renderWithRouter } from './helpers'

vi.mock('../components/customArtwork/ReferenceImageList', () => ({ ReferenceImageList: ({ images }: { images: unknown[] }) => <p>{images.length} safe reference preview</p> }))
vi.mock('../services/customArtworkService', () => ({ customArtworkService: {
  get: vi.fn(), listPayments: vi.fn(), getQuotation: vi.fn(), getShipment: vi.fn(), approveQuotation: vi.fn(), rejectQuotation: vi.fn(), initiateAdvancePayment: vi.fn(), uploadImage: vi.fn(),
  adminGet: vi.fn(), adminGetQuotation: vi.fn(), adminListPayments: vi.fn(), adminGetShipment: vi.fn(), adminReview: vi.fn(), adminCreateQuotation: vi.fn(), adminUpdateStatus: vi.fn(), adminCreateShipment: vi.fn(), adminUpdateShipmentStatus: vi.fn(),
} }))
const request = (status: CustomArtworkRequest['status']): CustomArtworkRequest => ({ id: 21, userId: 1, productType: 'Portrait', description: 'Family portrait', status, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', designTheme: null, preferredColors: null, dimensionsSize: null, budgetRange: null, requiredDeliveryDate: null, additionalInstructions: null, reviewedByUserId: null, reviewNotes: null, images: [] })
const quotation: Quotation = { id: 5, customOrderRequestId: 21, quotedAmount: 1000, advanceAmount: 250, estimatedDeliveryDate: null, expiryAt: '2099-01-01T00:00:00Z', notesTerms: null, status: 'PENDING', createdByUserId: 2, createdAt: '', decidedAt: null }

function customerView(status: CustomArtworkRequest['status'], payments: any[] = []) {
  vi.mocked(customArtworkService.get).mockResolvedValue(request(status)); vi.mocked(customArtworkService.getQuotation).mockResolvedValue(quotation); vi.mocked(customArtworkService.listPayments).mockResolvedValue(payments)
  return renderWithRouter(<Routes><Route path="/custom-requests/:id" element={<CustomRequestDetailPage />} /></Routes>, { route: '/custom-requests/21' })
}
function adminView(status: CustomArtworkRequest['status']) {
  vi.mocked(customArtworkService.adminGet).mockResolvedValue(request(status)); vi.mocked(customArtworkService.adminGetQuotation).mockRejectedValue(new Error('none')); vi.mocked(customArtworkService.adminListPayments).mockResolvedValue([]); vi.mocked(customArtworkService.adminGetShipment).mockRejectedValue(new Error('none'))
  return renderWithRouter(<Routes><Route path="/admin/custom-requests/:id" element={<AdminCustomRequestDetailPage />} /></Routes>, { route: '/admin/custom-requests/21' })
}

describe('customer custom artwork states', () => {
  beforeEach(() => { vi.mocked(customArtworkService.getShipment).mockRejectedValue(new Error('none')) })
  it('renders authoritative request state and no admin controls', async () => { customerView('REQUESTED'); expect(await screen.findByRole('heading', { name: 'Request #21' })).toBeInTheDocument(); expect(screen.getAllByText('Requested').length).toBeGreaterThan(0); expect(screen.queryByRole('button', { name: 'Begin review' })).not.toBeInTheDocument() })
  it('shows quotation decisions only in the actionable quoted state', async () => { customerView('QUOTED'); expect(await screen.findByRole('button', { name: 'Approve quotation' })).toBeInTheDocument(); expect(screen.getByRole('button', { name: 'Reject quotation' })).toBeInTheDocument() })
  it('shows backend fixed advance only when approved without successful advance', async () => { customerView('APPROVED'); expect(await screen.findByRole('button', { name: /Pay.*250/ })).toBeInTheDocument(); expect(screen.getByText(/amount is fixed/i)).toBeInTheDocument() })
  it('hides advance action after successful payment', async () => { customerView('APPROVED', [{ paymentId: 1, orderId: null, customOrderRequestId: 21, paymentPurpose: 'ADVANCE', status: 'SUCCESS', amount: 250, paymentMethod: 'SANDBOX', providerTransactionReference: null, failureReason: null, initiatedAt: '2026-01-01T00:00:00Z', completedAt: '2026-01-01T00:00:00Z' }]); await screen.findByRole('heading', { name: 'Request #21' }); expect(screen.queryByRole('button', { name: /Pay/ })).not.toBeInTheDocument() })
})

describe('admin custom artwork states and validation', () => {
  it('shows review only for REQUESTED', async () => { adminView('REQUESTED'); expect(await screen.findByRole('button', { name: 'Begin review' })).toBeInTheDocument(); expect(screen.queryByRole('button', { name: 'Mark production complete' })).not.toBeInTheDocument() })
  it('validates quotation amount boundaries and future expiry', async () => { adminView('UNDER_REVIEW'); const submit = await screen.findByRole('button', { name: 'Create quotation' }); const form = submit.closest('form')!; fireEvent.submit(form); expect(screen.getByText('Quoted amount must be greater than zero.')).toBeInTheDocument(); await userEvent.type(screen.getByLabelText(/^Quoted amount/), '100'); await userEvent.type(screen.getByLabelText(/^Fixed advance amount/), '200'); fireEvent.submit(form); expect(screen.getByText(/cannot exceed/)).toBeInTheDocument(); await userEvent.clear(screen.getByLabelText(/^Fixed advance amount/)); await userEvent.type(screen.getByLabelText(/^Fixed advance amount/), '50'); fireEvent.submit(form); expect(screen.getByText(/expiry date and time in the future/)).toBeInTheDocument() })
  it('shows production completion only while in production', async () => { adminView('IN_PRODUCTION'); expect(await screen.findByRole('button', { name: 'Mark production complete' })).toBeInTheDocument() })
  it('shows shipment creation only after completion', async () => { adminView('COMPLETED'); expect(await screen.findByRole('button', { name: 'Create shipment' })).toBeInTheDocument() })
})
