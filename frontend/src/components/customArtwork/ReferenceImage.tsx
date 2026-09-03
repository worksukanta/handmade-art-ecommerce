import { useEffect, useState } from 'react'
import { customArtworkService } from '../../services/customArtworkService'
import type { CustomOrderImage } from '../../types/customArtwork'
import { ImageLightbox } from '../common/ImageLightbox'

export function ReferenceImage({ image }: { image: CustomOrderImage }) {
  const [source, setSource] = useState<string | null>(null)
  const [failed, setFailed] = useState(false)
  const [lightboxOpen, setLightboxOpen] = useState(false)

  useEffect(() => {
    let active = true
    let objectUrl: string | null = null
    void customArtworkService.getImageContent(image.imageUrl).then((blob) => {
      if (!active) return
      objectUrl = URL.createObjectURL(blob)
      setSource(objectUrl)
    }).catch(() => { if (active) setFailed(true) })
    return () => { active = false; if (objectUrl) URL.revokeObjectURL(objectUrl) }
  }, [image.imageUrl])

  if (failed) return <div className="reference-image-fallback" role="img" aria-label={`${image.originalFilename} unavailable`}>Reference image unavailable</div>
  if (!source) return <div className="reference-image-fallback" role="status">Loading preview…</div>

  return (
    <>
      <button
        type="button"
        className="lightbox-trigger-button"
        onClick={() => setLightboxOpen(true)}
        aria-label={`Open enlarged preview of ${image.originalFilename}`}
      >
        <img className="reference-image-preview" src={source} alt={`Reference artwork: ${image.originalFilename}`} />
      </button>
      {lightboxOpen && (
        <ImageLightbox
          src={source}
          alt={`Reference artwork: ${image.originalFilename}`}
          onClose={() => setLightboxOpen(false)}
        />
      )}
    </>
  )
}
