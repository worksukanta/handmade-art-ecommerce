import { type PropsWithChildren, useCallback, useEffect, useMemo, useState } from 'react'
import { useAuth } from '../hooks/useAuth'
import { cartService } from '../services/cartService'
import type { Cart } from '../types/commerce'
import { CartContext, type CartContextValue } from './cartContext'
import { normalizeApiError } from '../utils/apiError'

export function CartProvider({ children }: PropsWithChildren) {
  const { user } = useAuth()
  const [cart, setCart] = useState<Cart | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)
  const isCustomer = user?.role === 'CUSTOMER'
  const refresh = useCallback(async () => { if (!isCustomer) return; setIsLoading(true); setLoadError(null); try { setCart(await cartService.get()) } catch (cause) { setLoadError(normalizeApiError(cause).message) } finally { setIsLoading(false) } }, [isCustomer])
  useEffect(() => { if (!isCustomer) return; let current = true; void cartService.get().then((result) => { if (current) setCart(result) }).catch((cause: unknown) => { if (current) setLoadError(normalizeApiError(cause).message) }); return () => { current = false } }, [isCustomer])
  const addItem = useCallback(async (productId: number, quantity: number) => { setCart(await cartService.add(productId, quantity)) }, [])
  const updateItem = useCallback(async (itemId: number, quantity: number) => { setCart(await cartService.update(itemId, quantity)) }, [])
  const removeItem = useCallback(async (itemId: number) => { setCart(await cartService.remove(itemId)) }, [])
  const clearCart = useCallback(async () => { await cartService.clear(); setCart((current) => ({ cartId: current?.cartId ?? null, items: [], total: 0 })) }, [])
  const resetCart = useCallback(() => setCart((current) => ({ cartId: current?.cartId ?? null, items: [], total: 0 })), [])
  const itemCount = cart?.items.reduce((count, item) => count + item.quantity, 0) ?? 0
  const cartIsLoading = isLoading || (isCustomer && cart === null && loadError === null)
  const value = useMemo<CartContextValue>(() => ({ cart, isLoading: cartIsLoading, loadError, itemCount, refresh, addItem, updateItem, removeItem, clearCart, resetCart }), [addItem, cart, cartIsLoading, clearCart, itemCount, loadError, refresh, removeItem, resetCart, updateItem])
  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}
