import { apiClient } from './apiClient'
import type { AdminCategory, AdminOrder, AdminOrderPage, AdminProductDetail, AdminProductPage, Inventory, InventoryPage, Payment, ProductImage, ProductRequest, ProductStatus, Shipment, ShipmentStatus } from '../types/admin'

export const adminCommerceService = {
  async listProducts(page = 0, size = 20): Promise<AdminProductPage> { return (await apiClient.get('/admin/products', { params: { page, size } })).data },
  async getProduct(id: number): Promise<AdminProductDetail> { return (await apiClient.get(`/admin/products/${id}`)).data },
  async createProduct(request: ProductRequest): Promise<AdminProductDetail> { return (await apiClient.post('/admin/products', request)).data },
  async updateProduct(id: number, request: ProductRequest): Promise<AdminProductDetail> { return (await apiClient.put(`/admin/products/${id}`, request)).data },
  async setProductStatus(id: number, status: ProductStatus): Promise<void> { await apiClient.patch(`/admin/products/${id}/status`, { status }) },
  async uploadProductImage(id: number, file: File): Promise<ProductImage> { const body = new FormData(); body.append('file', file); return (await apiClient.post(`/admin/products/${id}/images`, body)).data },
  async deleteProductImage(productId: number, imageId: number): Promise<void> { await apiClient.delete(`/admin/products/${productId}/images/${imageId}`) },
  async replaceRelatedProducts(id: number, productIds: number[]): Promise<void> { await apiClient.put(`/admin/products/${id}/related-products`, { productIds }) },
  async listCategories(): Promise<AdminCategory[]> { return (await apiClient.get('/admin/categories')).data },
  async createCategory(name: string, description: string | null): Promise<AdminCategory> { return (await apiClient.post('/admin/categories', { name, description })).data },
  async updateCategory(id: number, name: string, description: string | null): Promise<AdminCategory> { return (await apiClient.put(`/admin/categories/${id}`, { name, description })).data },
  async setCategoryStatus(id: number, status: 'ACTIVE' | 'INACTIVE'): Promise<void> { await apiClient.patch(`/admin/categories/${id}/status`, { status }) },
  async listInventory(page = 0, size = 20): Promise<InventoryPage> { return (await apiClient.get('/admin/inventory', { params: { page, size } })).data },
  async updateInventory(productId: number, availableQuantity: number): Promise<Inventory> { return (await apiClient.patch(`/admin/inventory/${productId}`, { availableQuantity })).data },
  async listOrders(page = 0, size = 20): Promise<AdminOrderPage> { return (await apiClient.get('/admin/orders', { params: { page, size } })).data },
  async getOrder(id: number): Promise<AdminOrder> { return (await apiClient.get(`/admin/orders/${id}`)).data },
  async getOrderPayments(id: number): Promise<Payment[]> { return (await apiClient.get(`/admin/orders/${id}/payments`)).data },
  async getOrderShipment(id: number): Promise<Shipment> { return (await apiClient.get(`/admin/orders/${id}/shipment`)).data },
  async setOrderStatus(id: number, status: AdminOrder['status']): Promise<void> { await apiClient.patch(`/admin/orders/${id}/status`, { status }) },
  async createOrderShipment(orderId: number, carrierName: string | null, trackingReference: string | null, estimatedDeliveryDate: string | null): Promise<Shipment> { return (await apiClient.post('/admin/shipments', { orderId, carrierName, trackingReference, estimatedDeliveryDate })).data },
  async setShipmentStatus(id: number, status: ShipmentStatus): Promise<void> { await apiClient.patch(`/admin/shipments/${id}/status`, { status }) },
}
