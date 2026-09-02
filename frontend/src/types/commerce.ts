export interface CartItem { itemId: number; productId: number; productName: string; unitPrice: number; quantity: number; subtotal: number; addedAt: string }
export interface Cart { cartId: number | null; items: CartItem[]; total: number }
export interface Profile { id: number; name: string; email: string; phone: string | null; role: string; created_at: string; updated_at: string }
export interface UpdateProfileRequest { name: string; phone: string | null }
export interface AddressRequest { recipient_name: string; line1: string; line2: string | null; city: string; state_province: string; postal_code: string; country: string; phone: string | null; is_default: boolean }
export interface Address extends AddressRequest { id: number; created_at: string; updated_at: string }
export interface CheckoutValidationItem { productId: number; productName: string; quantity: number; unitPrice: number; lineTotal: number }
export interface CheckoutValidation { valid: boolean; items: CheckoutValidationItem[]; subtotalAmount: number; totalAmount: number }
export interface OrderItem { itemId: number; productId: number | null; productName: string; unitPrice: number; quantity: number; lineTotal: number }
export interface Order { orderId: number; status: string; shipRecipientName: string; shipLine1: string; shipLine2: string | null; shipCity: string; shipStateProvince: string; shipPostalCode: string; shipCountry: string; shipPhone: string | null; subtotalAmount: number; totalAmount: number; items: OrderItem[]; createdAt: string; updatedAt: string }
