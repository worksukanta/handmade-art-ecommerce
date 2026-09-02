export type ProductType = 'READY_MADE' | 'CUSTOM_AVAILABLE' | 'PORTFOLIO_ONLY'

export interface ProductImage {
  id: number
  storage_reference: string
  imageUrl: string
  original_filename: string
  content_type: string
  file_size_bytes: number
  display_order: number
  is_primary: boolean
}

export interface ProductSummary {
  id: number
  name: string
  price: number
  product_type: ProductType
  category_id: number
  category_name: string
  primary_image: ProductImage | null
  created_at: string
}

export interface ProductAvailability {
  in_stock: boolean
  quantity_on_hand?: number
}

export interface ProductDetail {
  id: number
  name: string
  description: string | null
  price: number
  product_type: ProductType
  category_id: number
  category_name: string
  images: ProductImage[]
  availability: ProductAvailability
  related_products: ProductSummary[]
  created_at: string
  updated_at: string
}

export interface Category {
  id: number
  name: string
  description: string | null
  status: 'ACTIVE'
  created_at: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  total_elements: number
  total_pages: number
}

export interface ProductListParams {
  q?: string
  categoryId?: number
  minPrice?: number
  maxPrice?: number
  sort?: 'name' | 'price' | 'created_at'
  direction?: 'ASC' | 'DESC'
  page?: number
  size?: number
}
