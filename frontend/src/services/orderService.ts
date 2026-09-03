import { apiClient } from './apiClient'
import type { Order, OrderPage, Shipment } from '../types/commerce'

export const orderService = {
  async list(page = 0, size = 20): Promise<OrderPage> {
    return (await apiClient.get<OrderPage>('/orders', { params: { page, size } })).data
  },
  async get(orderId: number): Promise<Order> {
    return (await apiClient.get<Order>(`/orders/${orderId}`)).data
  },
  async getShipment(orderId: number): Promise<Shipment> {
    return (await apiClient.get<Shipment>(`/orders/${orderId}/shipment`)).data
  },
}
