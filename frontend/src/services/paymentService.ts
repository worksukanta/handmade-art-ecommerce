import { apiClient } from './apiClient'
import type { Payment, PaymentInitiationRequest } from '../types/commerce'

export const paymentService = {
  async listForOrder(orderId: number): Promise<Payment[]> {
    return (await apiClient.get<Payment[]>(`/orders/${orderId}/payments`)).data
  },
  async initiate(orderId: number, request: PaymentInitiationRequest): Promise<Payment> {
    return (await apiClient.post<Payment>(`/orders/${orderId}/payments`, request)).data
  },
}
