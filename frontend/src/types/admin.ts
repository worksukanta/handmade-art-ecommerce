import type { PageResponse, ProductDetail, ProductImage, ProductSummary, ProductType } from './catalogue'
import type { OrderItem, OrderStatus, Payment, Shipment, ShipmentStatus } from './commerce'

export type ProductStatus = 'ACTIVE' | 'INACTIVE'
export type CategoryStatus = 'ACTIVE' | 'INACTIVE'
export interface AdminProductSummary extends ProductSummary { status: ProductStatus }
export interface AdminProductDetail extends Omit<ProductDetail, 'related_products'> { status: ProductStatus; related_products: AdminProductSummary[] }
export interface AdminCategory { id: number; name: string; description: string | null; status: CategoryStatus; created_at: string }
export interface ProductRequest { name: string; description: string | null; price: number; category_id: number; product_type: ProductType; status: ProductStatus }
export interface Inventory { product_id: number; quantity_on_hand: number; updated_at: string }
export interface AdminOrderSummary { orderId: number; customerId: number; customerEmail: string; status: OrderStatus; shipRecipientName: string; shipCity: string; shipCountry: string; subtotalAmount: number; totalAmount: number; createdAt: string; updatedAt: string }
export interface AdminOrder extends AdminOrderSummary { shipLine1: string; shipLine2: string | null; shipStateProvince: string; shipPostalCode: string; shipPhone: string | null; items: OrderItem[] }
export type AdminProductPage = PageResponse<AdminProductSummary>
export type InventoryPage = PageResponse<Inventory>
export type AdminOrderPage = PageResponse<AdminOrderSummary>
export type { Payment, ProductImage, Shipment, ShipmentStatus }
