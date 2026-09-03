import { useState } from 'react'
import { resolveImageUrl } from '../../services/catalogueService'
import type { ProductImage as ProductImageData } from '../../types/catalogue'

interface ProductImageProps {
  imageUrl?: string | null
  alt?: string
  className?: string
  image?: ProductImageData
}

export function ProductImage({ imageUrl, alt, className, image }: ProductImageProps) {
  const [hasError, setHasError] = useState(false)
  const source = image?.imageUrl ?? imageUrl
  const alternative = alt ?? image?.original_filename ?? 'Product image'

  if (!source || hasError) {
    return (
      <div className={`product-image-fallback ${className ?? ''}`} role="img" aria-label={`${alternative} unavailable`}>
        <span aria-hidden="true">Artwork image unavailable</span>
      </div>
    )
  }

  return (
    <img
      className={className}
      src={resolveImageUrl(source)}
      alt={alternative}
      loading="lazy"
      onError={() => setHasError(true)}
    />
  )
}
