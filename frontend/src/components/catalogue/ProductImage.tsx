import { useState } from 'react'
import { resolveImageUrl } from '../../services/catalogueService'

interface ProductImageProps {
  imageUrl?: string | null
  alt: string
  className?: string
}

export function ProductImage({ imageUrl, alt, className }: ProductImageProps) {
  const [hasError, setHasError] = useState(false)

  if (!imageUrl || hasError) {
    return (
      <div className={`product-image-fallback ${className ?? ''}`} role="img" aria-label={`${alt} unavailable`}>
        <span aria-hidden="true">Artwork image unavailable</span>
      </div>
    )
  }

  return (
    <img
      className={className}
      src={resolveImageUrl(imageUrl)}
      alt={alt}
      loading="lazy"
      onError={() => setHasError(true)}
    />
  )
}
