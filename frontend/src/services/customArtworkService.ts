import { apiClient } from './apiClient'
import type { Payment, PaymentInitiationRequest, Shipment } from '../types/commerce'
import type { CustomArtworkRequest, CustomArtworkRequestCreateRequest, CustomOrderImage, CustomRequestPage, CustomRequestStatus, Quotation } from '../types/customArtwork'

export const customArtworkService = {
  async create(request: CustomArtworkRequestCreateRequest): Promise<CustomArtworkRequest> { return (await apiClient.post<CustomArtworkRequest>('/custom-requests', request)).data },
  async list(page = 0, size = 20, status?: CustomRequestStatus): Promise<CustomRequestPage> { return (await apiClient.get<CustomRequestPage>('/custom-requests', { params: { page, size, status } })).data },
  async get(id: number): Promise<CustomArtworkRequest> { return (await apiClient.get<CustomArtworkRequest>(`/custom-requests/${id}`)).data },
  async uploadImage(id: number, file: File): Promise<CustomOrderImage> { const data = new FormData(); data.append('file', file); return (await apiClient.post<CustomOrderImage>(`/custom-requests/${id}/images`, data)).data },
  async getQuotation(id: number): Promise<Quotation> { return (await apiClient.get<Quotation>(`/custom-requests/${id}/quotation`)).data },
  async approveQuotation(id: number): Promise<Quotation> { return (await apiClient.post<Quotation>(`/quotations/${id}/approve`)).data },
  async rejectQuotation(id: number): Promise<Quotation> { return (await apiClient.post<Quotation>(`/quotations/${id}/reject`)).data },
  async listPayments(id: number): Promise<Payment[]> { return (await apiClient.get<Payment[]>(`/custom-requests/${id}/payments`)).data },
  async initiateAdvancePayment(id: number, request: PaymentInitiationRequest): Promise<Payment> { return (await apiClient.post<Payment>(`/custom-requests/${id}/payments`, request)).data },
  async getShipment(id: number): Promise<Shipment> { return (await apiClient.get<Shipment>(`/custom-requests/${id}/shipment`)).data },
}
