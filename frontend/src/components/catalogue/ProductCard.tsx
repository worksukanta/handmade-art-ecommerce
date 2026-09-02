import { Link } from 'react-router-dom'
import type { ProductSummary } from '../../types/catalogue'
import { ProductImage } from './ProductImage'

interface ProductCardProps {
  product: ProductSummary
}

function productTypeLabel(type: ProductSummary['product_type']) {
  if (type === 'PORTFOLIO_ONLY') return 'Display only'
  if (type === 'CUSTOM_AVAILABLE') return 'Custom order'
  return 'Ready-made'
}

export function ProductCard({ product }: ProductCardProps) {
  return (
    <article className="product-card">
      <Link className="product-card-link" to={`/products/${product.id}`}>
        <ProductImage
          className="product-card-image"
          imageUrl={product.primary_image?.imageUrl}
          alt={`${product.name} artwork`}
        />
        <div className="product-card-body">
          <p className="product-category">{product.category_name}</p>
          <h2>{product.name}</h2>
          <div className="product-card-meta">
            <span className="product-price">${Number(product.price).toFixed(2)}</span>
            <span className={`product-badge product-badge-${product.product_type.toLowerCase()}`}>
              {productTypeLabel(product.product_type)}
            </span>
          </div>
        </div>
      </Link>
    </article>
  )
}
