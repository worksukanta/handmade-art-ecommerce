import { apiClient } from './apiClient'
import type { Cart } from '../types/commerce'

export const cartService = {
  async get(): Promise<Cart> { return (await apiClient.get<Cart>('/cart')).data },
  async add(productId: number, quantity: number): Promise<Cart> { return (await apiClient.post<Cart>('/cart/items', { productId, quantity })).data },
  async update(itemId: number, quantity: number): Promise<Cart> { return (await apiClient.put<Cart>(`/cart/items/${itemId}`, { quantity })).data },
  async remove(itemId: number): Promise<Cart> { return (await apiClient.delete<Cart>(`/cart/items/${itemId}`)).data },
  async clear(): Promise<void> { await apiClient.delete('/cart/items') },
}
