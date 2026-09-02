import { createContext } from 'react'
import type { Cart } from '../types/commerce'

export interface CartContextValue {
  cart: Cart | null
  isLoading: boolean
  loadError: string | null
  itemCount: number
  refresh: () => Promise<void>
  addItem: (productId: number, quantity: number) => Promise<void>
  updateItem: (itemId: number, quantity: number) => Promise<void>
  removeItem: (itemId: number) => Promise<void>
  clearCart: () => Promise<void>
  resetCart: () => void
}
export const CartContext = createContext<CartContextValue | undefined>(undefined)
