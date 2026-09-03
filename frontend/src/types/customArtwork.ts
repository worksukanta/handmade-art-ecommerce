import type { Payment, Shipment } from './commerce'

export type CustomRequestStatus = 'REQUESTED' | 'UNDER_REVIEW' | 'QUOTED' | 'CUSTOMER_APPROVAL_PENDING' | 'APPROVED' | 'ADVANCE_PAYMENT_PENDING' | 'IN_PRODUCTION' | 'COMPLETED' | 'SHIPPED' | 'DELIVERED' | 'REJECTED' | 'QUOTATION_EXPIRED' | 'CANCELLED'
export type QuotationStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXPIRED'

export interface CustomArtworkRequestCreateRequest { productType: string; description: string; designTheme?: string; preferredColors?: string; dimensionsSize?: string; budgetRange?: string; requiredDeliveryDate?: string; additionalInstructions?: string }
export interface CustomOrderImage { id: number; customOrderRequestId: number; storageReference: string; originalFilename: string; contentType: string; fileSizeBytes: number; uploadedAt: string }
export interface CustomArtworkRequestSummary { id: number; userId: number; productType: string; description: string; status: CustomRequestStatus; createdAt: string; updatedAt: string }
export interface CustomArtworkRequest extends CustomArtworkRequestSummary { designTheme: string | null; preferredColors: string | null; dimensionsSize: string | null; budgetRange: string | null; requiredDeliveryDate: string | null; additionalInstructions: string | null; reviewedByUserId: number | null; reviewNotes: string | null; images: CustomOrderImage[] }
export interface CustomRequestPage { content: CustomArtworkRequestSummary[]; page: number; size: number; total_elements: number; total_pages: number }
export interface Quotation { id: number; customOrderRequestId: number; quotedAmount: number; advanceAmount: number | null; estimatedDeliveryDate: string | null; expiryAt: string; notesTerms: string | null; status: QuotationStatus; createdByUserId: number; createdAt: string; decidedAt: string | null }
export interface CustomRequestReviewRequest { decision: 'ACCEPT' | 'REJECT'; notes?: string }
export interface QuotationCreateRequest { quotedAmount: number; advanceAmount: number; estimatedDeliveryDate?: string; expiryAt: string; notesTerms?: string }
export interface ShipmentCreateRequest { customOrderRequestId: number; carrierName?: string; trackingReference?: string; estimatedDeliveryDate?: string }

export interface CustomRequestWorkflowData { request: CustomArtworkRequest; quotation: Quotation | null; payments: Payment[]; shipment: Shipment | null }
