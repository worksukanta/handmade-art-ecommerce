import { apiClient } from './apiClient'
import type {
  Category,
  PageResponse,
  ProductDetail,
  ProductListParams,
  ProductSummary,
} from '../types/catalogue'

export const catalogueService = {
  async listProducts(params: ProductListParams): Promise<PageResponse<ProductSummary>> {
    const response = await apiClient.get<PageResponse<ProductSummary>>('/products', { params })
    return response.data
  },

  async listCategories(): Promise<Category[]> {
    const response = await apiClient.get<Category[]>('/categories')
    return response.data
  },

  async getProduct(id: number): Promise<ProductDetail> {
    const response = await apiClient.get<ProductDetail>(`/products/${id}`)
    return response.data
  },
}

export function resolveImageUrl(imageUrl: string): string {
  const apiBaseUrl = apiClient.defaults.baseURL
  if (!apiBaseUrl) return imageUrl
  const apiOrigin = new URL(apiBaseUrl, window.location.origin).origin
  return new URL(imageUrl, apiOrigin).toString()
}
