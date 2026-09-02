import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ProductCard } from '../components/catalogue/ProductCard'
import { ProductImage } from '../components/catalogue/ProductImage'
import { ErrorState } from '../components/feedback/ErrorState'
import { LoadingState } from '../components/feedback/LoadingState'
import { catalogueService } from '../services/catalogueService'
import type { ProductDetail, ProductImage as ProductImageType } from '../types/catalogue'
import { normalizeApiError } from '../utils/apiError'

function orderedImages(images: ProductImageType[]) {
  return [...images].sort((left, right) => {
    if (left.is_primary !== right.is_primary) return left.is_primary ? -1 : 1
    return left.display_order - right.display_order
  })
}

function availabilityLabel(product: ProductDetail) {
  if (product.product_type === 'PORTFOLIO_ONLY') return 'Portfolio piece — display only'
  if (product.product_type === 'CUSTOM_AVAILABLE') return 'Available by custom request'
  return product.availability.in_stock ? 'In stock' : 'Currently unavailable'
}

export function ProductDetailPage() {
  const { id } = useParams()
  const productId = Number(id)
  const hasInvalidProductId = !Number.isSafeInteger(productId) || productId <= 0
  const [product, setProduct] = useState<ProductDetail | null>(null)
  const [selectedImageId, setSelectedImageId] = useState<number | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<{ status: number | null; message: string } | null>(null)
  const [requestVersion, setRequestVersion] = useState(0)

  useEffect(() => {
    let isCurrent = true
    if (hasInvalidProductId) return () => { isCurrent = false }
    void catalogueService.getProduct(productId)
      .then((result) => {
        if (!isCurrent) return
        setProduct(result)
        setSelectedImageId(orderedImages(result.images)[0]?.id ?? null)
      })
      .catch((requestError: unknown) => {
        if (!isCurrent) return
        const normalized = normalizeApiError(requestError)
        setError({ status: normalized.status, message: normalized.message })
      })
      .finally(() => { if (isCurrent) setIsLoading(false) })
    return () => { isCurrent = false }
  }, [hasInvalidProductId, productId, requestVersion])
  const images = useMemo(() => orderedImages(product?.images ?? []), [product])
  const selectedImage = images.find((image) => image.id === selectedImageId) ?? images[0]

  if (hasInvalidProductId) {
    return <ErrorState title="Product not available" message="This product address is invalid." action={<Link className="button button-secondary state-link" to="/">Return to catalogue</Link>} />
  }
  if (isLoading || product?.id !== productId) return <LoadingState label="Loading product details" />
  if (error?.status === 404) {
    return <ErrorState title="Product not available" message="This artwork may have been removed or is no longer available." action={<Link className="button button-secondary state-link" to="/">Return to catalogue</Link>} />
  }
  if (error || !product) {
    return <ErrorState title="We couldn’t load this product" message={error?.message ?? 'Please try again.'} onRetry={() => { setIsLoading(true); setError(null); setRequestVersion((version) => version + 1) }} />
  }

  return (
    <article className="product-detail-page">
      <Link className="back-link" to="/">← Back to catalogue</Link>
      <div className="product-detail-layout">
        <section className="product-gallery" aria-label={`${product.name} images`}>
          <ProductImage className="product-main-image" imageUrl={selectedImage?.imageUrl} alt={`${product.name}${selectedImage?.is_primary ? ', primary view' : ''}`} />
          {images.length > 1 && (
            <div className="product-thumbnails">
              {images.map((image, index) => (
                <button type="button" key={image.id} className={image.id === selectedImage?.id ? 'thumbnail-button selected' : 'thumbnail-button'} onClick={() => setSelectedImageId(image.id)} aria-label={`View ${product.name} image ${index + 1}`} aria-pressed={image.id === selectedImage?.id}>
                  <ProductImage imageUrl={image.imageUrl} alt="" />
                </button>
              ))}
            </div>
          )}
        </section>
        <section className="product-information">
          <p className="product-category"><Link to={`/?categoryId=${product.category_id}`}>{product.category_name}</Link></p>
          <h1>{product.name}</h1>
          <p className="product-price product-detail-price">${Number(product.price).toFixed(2)}</p>
          <p className={`availability ${product.availability.in_stock ? 'available' : ''}`}>{availabilityLabel(product)}</p>
          {product.description && <div className="product-description"><h2>About this artwork</h2><p>{product.description}</p></div>}
          <div className="purchase-note">
            <h2>Purchase options</h2>
            <p>{product.product_type === 'READY_MADE' ? 'Cart purchasing will be available in the next storefront phase.' : product.product_type === 'CUSTOM_AVAILABLE' ? 'This work is available through the custom artwork request process.' : 'This piece is presented as part of the artist’s portfolio and is not available for standard purchase.'}</p>
          </div>
        </section>
      </div>
      {product.related_products.length > 0 && (
        <section className="related-products" aria-labelledby="related-products-heading">
          <div className="section-heading"><p className="eyebrow">You may also like</p><h2 id="related-products-heading">Related artwork</h2></div>
          <div className="product-grid related-grid">{product.related_products.map((related) => <ProductCard key={related.id} product={related} />)}</div>
        </section>
      )}
    </article>
  )
}
