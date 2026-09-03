export interface CartItem { itemId: number; productId: number; productName: string; unitPrice: number; quantity: number; subtotal: number; addedAt: string }
export interface Cart { cartId: number | null; items: CartItem[]; total: number }
export interface Profile { id: number; name: string; email: string; phone: string | null; role: string; created_at: string; updated_at: string }
export interface UpdateProfileRequest { name: string; phone: string | null }
export interface AddressRequest { recipient_name: string; line1: string; line2: string | null; city: string; state_province: string; postal_code: string; country: string; phone: string | null; is_default: boolean }
export interface Address extends AddressRequest { id: number; created_at: string; updated_at: string }
export interface CheckoutValidationItem { productId: number; productName: string; quantity: number; unitPrice: number; lineTotal: number }
export interface CheckoutValidation { valid: boolean; items: CheckoutValidationItem[]; subtotalAmount: number; totalAmount: number }
export type OrderStatus = 'PENDING_PAYMENT' | 'CONFIRMED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'
export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED'
export type PaymentPurpose = 'FULL' | 'ADVANCE' | 'REMAINING'
export type ShipmentStatus = 'PENDING' | 'SHIPPED' | 'DELIVERED'

export interface OrderItem { itemId: number; productId: number | null; productName: string; unitPrice: number; quantity: number; lineTotal: number }
export interface Order { orderId: number; status: OrderStatus; shipRecipientName: string; shipLine1: string; shipLine2: string | null; shipCity: string; shipStateProvince: string; shipPostalCode: string; shipCountry: string; shipPhone: string | null; subtotalAmount: number; totalAmount: number; items: OrderItem[]; createdAt: string; updatedAt: string }
export interface OrderSummary { orderId: number; status: OrderStatus; shipRecipientName: string; shipCity: string; shipCountry: string; subtotalAmount: number; totalAmount: number; createdAt: string; updatedAt: string }
export interface OrderPage { content: OrderSummary[]; page: number; size: number; total_elements: number; total_pages: number }
export interface PaymentInitiationRequest { paymentMethod: string }
export interface Payment { paymentId: number; orderId: number | null; customOrderRequestId: number | null; paymentPurpose: PaymentPurpose; amount: number; paymentMethod: string; status: PaymentStatus; providerTransactionReference: string | null; failureReason: string | null; initiatedAt: string; completedAt: string | null }
export interface Shipment { id: number; orderId: number | null; customOrderRequestId: number | null; carrierName: string | null; trackingReference: string | null; status: ShipmentStatus; estimatedDeliveryDate: string | null; shippedAt: string | null; deliveredAt: string | null; createdAt: string }
