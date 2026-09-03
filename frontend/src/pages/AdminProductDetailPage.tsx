import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { ErrorState } from '../components/feedback/ErrorState'
import { LoadingState } from '../components/feedback/LoadingState'
import { StatusBadge } from '../components/commerce/StatusBadge'
import { ProductImage } from '../components/catalogue/ProductImage'
import { ImageLightbox } from '../components/common/ImageLightbox'
import { adminCommerceService } from '../services/adminCommerceService'
import { resolveImageUrl } from '../services/catalogueService'
import type { AdminProductDetail, AdminProductSummary, ProductImage as ProductImageType } from '../types/admin'
import { normalizeApiError } from '../utils/apiError'
import { formatCurrency, humanizeStatus } from '../utils/format'

export function AdminProductDetailPage() {
  const id = Number(useParams().id)
  const location = useLocation()
  const created = Boolean((location.state as { productCreated?: boolean } | null)?.productCreated)
  const [product, setProduct] = useState<AdminProductDetail | null>(null)
  const [choices, setChoices] = useState<AdminProductSummary[]>([])
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<string | null>(
    created ? 'Product created successfully. You can now upload product images.' : null
  )
  const [file, setFile] = useState<File | null>(null)
  const [relatedId, setRelatedId] = useState('')
  const [previewImage, setPreviewImage] = useState<ProductImageType | null>(null)

  const load = useCallback(async () => {
    try {
      const [detail, page] = await Promise.all([
        adminCommerceService.getProduct(id),
        adminCommerceService.listProducts(0, 100),
      ])
      setProduct(detail)
      setChoices(page.content.filter((p) => p.id !== id))
      setError(null)
    } catch (e) {
      setError(normalizeApiError(e).message)
    }
  }, [id])

  useEffect(() => {
    void Promise.resolve().then(load)
  }, [load])

  const mutate = async (action: () => Promise<unknown>, success: string) => {
    setBusy(true)
    setError(null)
    setMessage(null)
    try {
      await action()
      setMessage(success)
      await load()
    } catch (e) {
      const n = normalizeApiError(e)
      setError(n.status === 409 ? `The product changed on the server. ${n.message}` : n.message)
      if (n.status === 409) await load()
    } finally {
      setBusy(false)
    }
  }

  if (error && !product) {
    return <ErrorState title="We couldn't load this product" message={error} onRetry={() => void load()} />
  }
  if (!product) return <LoadingState label="Loading product" />

  const relatedIds = product.related_products.map((p) => p.id)

  const upload = (e: FormEvent) => {
    e.preventDefault()
    if (file) {
      void mutate(() => adminCommerceService.uploadProductImage(id, file), 'Image uploaded.').then(() => setFile(null))
    }
  }

  const addRelated = () => {
    const value = Number(relatedId)
    if (value && !relatedIds.includes(value)) {
      void mutate(
        () => adminCommerceService.replaceRelatedProducts(id, [...relatedIds, value]),
        'Related products updated.'
      ).then(() => setRelatedId(''))
    }
  }

  return (
    <section className="commerce-page">
      <Link className="back-link" to="/admin/products">← Products</Link>
      <div className="heading-actions">
        <div className="page-heading">
          <p className="eyebrow">Product #{id}</p>
          <h1>{product.name}</h1>
          <StatusBadge kind="generic" value={product.status} />
        </div>
        <Link className="button" to={`/admin/products/${id}/edit`}>Edit product</Link>
      </div>
      {error && <p className="form-alert form-alert-error" role="alert">{error}</p>}
      {message && <p className="form-alert form-alert-success" role="status">{message}</p>}
      <div className="admin-detail-grid">
        <div className="detail-panel">
          <h2>Catalogue details</h2>
          <dl className="request-details">
            <div><dt>Type</dt><dd>{humanizeStatus(product.product_type)}</dd></div>
            <div><dt>Category</dt><dd>{product.category_name}</dd></div>
            <div><dt>Price</dt><dd>{formatCurrency(product.price)}</dd></div>
            <div><dt>Stock</dt><dd>{product.availability.quantity_on_hand ?? 'Not tracked'}</dd></div>
            <div><dt>Description</dt><dd>{product.description || '—'}</dd></div>
          </dl>
          <button
            className="button button-secondary"
            disabled={busy}
            onClick={() => void mutate(
              () => adminCommerceService.setProductStatus(id, product.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'),
              `Product ${product.status === 'ACTIVE' ? 'deactivated' : 'activated'}.`
            )}
          >
            {product.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
          </button>
        </div>
        <div className="detail-panel">
          <h2>Images</h2>
          <div className="admin-image-grid">
            {product.images.map((image) => (
              <article key={image.id}>
                <button
                  type="button"
                  className="lightbox-trigger-button"
                  onClick={() => setPreviewImage(image)}
                  aria-label={`Preview ${image.original_filename}`}
                >
                  <ProductImage image={image} className="admin-product-image" />
                </button>
                <p>{image.original_filename}</p>
                <small>{image.is_primary ? 'Primary · ' : ''}Order {image.display_order}</small>
                <button
                  className="text-button danger"
                  disabled={busy}
                  onClick={() => {
                    if (confirm('Delete this image?')) {
                      void mutate(() => adminCommerceService.deleteProductImage(id, image.id), 'Image deleted.')
                    }
                  }}
                >
                  Delete
                </button>
              </article>
            ))}
          </div>
          <form className="upload-form" onSubmit={upload}>
            <label htmlFor="product-image">Upload image</label>
            <input
              id="product-image"
              accept="image/*"
              type="file"
              disabled={busy}
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
            <button className="button button-primary" disabled={busy || !file} type="submit">
              {busy ? 'Uploading…' : 'Upload'}
            </button>
          </form>
        </div>
        <div className="detail-panel">
          <h2>Related products</h2>
          <ul className="simple-list">
            {product.related_products.map((item) => (
              <li key={item.id}>
                <span>{item.name} (#{item.id})</span>
                <button
                  className="text-button danger"
                  disabled={busy}
                  onClick={() => void mutate(
                    () => adminCommerceService.replaceRelatedProducts(id, relatedIds.filter((x) => x !== item.id)),
                    'Related product removed.'
                  )}
                >
                  Remove
                </button>
              </li>
            ))}
          </ul>
          <div className="inline-controls">
            <select
              aria-label="Select product to relate"
              value={relatedId}
              onChange={(e) => setRelatedId(e.target.value)}
            >
              <option value="">Select product to relate…</option>
              {choices.filter((c) => !relatedIds.includes(c.id)).map((c) => (
                <option key={c.id} value={c.id}>{c.name} (#{c.id})</option>
              ))}
            </select>
            <button className="button button-secondary" disabled={busy || !relatedId} onClick={addRelated}>
              Add related
            </button>
          </div>
        </div>
      </div>
      {previewImage && previewImage.imageUrl && (
        <ImageLightbox
          src={resolveImageUrl(previewImage.imageUrl)}
          alt={`${product.name} — ${previewImage.original_filename}`}
          onClose={() => setPreviewImage(null)}
        />
      )}
    </section>
  )
}
