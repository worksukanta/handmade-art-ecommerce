import { apiClient } from './apiClient'
import type { Payment, PaymentInitiationRequest, Shipment } from '../types/commerce'
import type { CustomArtworkRequest, CustomArtworkRequestCreateRequest, CustomOrderImage, CustomRequestPage, CustomRequestReviewRequest, CustomRequestStatus, Quotation, QuotationCreateRequest, ShipmentCreateRequest } from '../types/customArtwork'

export const customArtworkService = {
  async create(request: CustomArtworkRequestCreateRequest): Promise<CustomArtworkRequest> { return (await apiClient.post<CustomArtworkRequest>('/custom-requests', request)).data },
  async list(page = 0, size = 20, status?: CustomRequestStatus): Promise<CustomRequestPage> { return (await apiClient.get<CustomRequestPage>('/custom-requests', { params: { page, size, status } })).data },
  async get(id: number): Promise<CustomArtworkRequest> { return (await apiClient.get<CustomArtworkRequest>(`/custom-requests/${id}`)).data },
  async uploadImage(id: number, file: File): Promise<CustomOrderImage> { const data = new FormData(); data.append('file', file); return (await apiClient.post<CustomOrderImage>(`/custom-requests/${id}/images`, data)).data },
  async getImageContent(imageUrl: string): Promise<Blob> {
    // imageUrl from the backend is an API-relative path like "/api/v1/custom-request-images/{id}/content".
    // apiClient.baseURL is already "http://localhost:8080/api/v1", so passing the full /api/v1/... path
    // would cause Axios to concatenate baseURL + path → ".../api/v1/api/v1/..." (double prefix).
    // Strip the base path prefix so Axios resolves against the correct baseURL root.
    const basePath = (apiClient.defaults.baseURL ? new URL(apiClient.defaults.baseURL, 'http://localhost').pathname : '/api/v1').replace(/\/$/, '')
    const relativePath = imageUrl.startsWith(basePath + '/') ? imageUrl.slice(basePath.length) : imageUrl
    return (await apiClient.get<Blob>(relativePath, { responseType: 'blob' })).data
  },
  async getQuotation(id: number): Promise<Quotation> { return (await apiClient.get<Quotation>(`/custom-requests/${id}/quotation`)).data },
  async approveQuotation(id: number): Promise<Quotation> { return (await apiClient.post<Quotation>(`/quotations/${id}/approve`)).data },
  async rejectQuotation(id: number): Promise<Quotation> { return (await apiClient.post<Quotation>(`/quotations/${id}/reject`)).data },
  async listPayments(id: number): Promise<Payment[]> { return (await apiClient.get<Payment[]>(`/custom-requests/${id}/payments`)).data },
  async initiateAdvancePayment(id: number, request: PaymentInitiationRequest): Promise<Payment> { return (await apiClient.post<Payment>(`/custom-requests/${id}/payments`, request)).data },
  async getShipment(id: number): Promise<Shipment> { return (await apiClient.get<Shipment>(`/custom-requests/${id}/shipment`)).data },
  async adminList(page = 0, size = 20, status?: CustomRequestStatus): Promise<CustomRequestPage> { return (await apiClient.get<CustomRequestPage>('/admin/custom-requests', { params: { page, size, status } })).data },
  async adminGet(id: number): Promise<CustomArtworkRequest> { return (await apiClient.get<CustomArtworkRequest>(`/admin/custom-requests/${id}`)).data },
  async adminReview(id: number, request: CustomRequestReviewRequest): Promise<CustomArtworkRequest> { return (await apiClient.patch<CustomArtworkRequest>(`/admin/custom-requests/${id}/review`, request)).data },
  async adminCreateQuotation(id: number, request: QuotationCreateRequest): Promise<Quotation> { return (await apiClient.post<Quotation>(`/admin/custom-requests/${id}/quotation`, request)).data },
  async adminGetQuotation(id: number): Promise<Quotation> { return (await apiClient.get<Quotation>(`/admin/custom-requests/${id}/quotation`)).data },
  async adminListPayments(id: number): Promise<Payment[]> { return (await apiClient.get<Payment[]>(`/admin/custom-requests/${id}/payments`)).data },
  async adminUpdateStatus(id: number, status: CustomRequestStatus): Promise<CustomArtworkRequest> { return (await apiClient.patch<CustomArtworkRequest>(`/admin/custom-requests/${id}/status`, { status })).data },
  async adminCreateShipment(request: ShipmentCreateRequest): Promise<Shipment> { return (await apiClient.post<Shipment>('/admin/shipments', request)).data },
  async adminGetShipment(id: number): Promise<Shipment> { return (await apiClient.get<Shipment>(`/admin/custom-requests/${id}/shipment`)).data },
  async adminUpdateShipmentStatus(id: number, status: Shipment['status']): Promise<Shipment> { return (await apiClient.patch<Shipment>(`/admin/shipments/${id}/status`, { status })).data },
}
