import { apiClient } from './apiClient'
import type { CheckoutValidation, Order } from '../types/commerce'

export const checkoutService = {
  async validate(addressId: number): Promise<CheckoutValidation> { return (await apiClient.post<CheckoutValidation>('/checkout/validate', { addressId })).data },
  async createOrder(addressId: number): Promise<Order> { return (await apiClient.post<Order>('/orders', { addressId })).data },
}
