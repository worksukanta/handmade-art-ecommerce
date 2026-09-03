import { useEffect, useState } from 'react'
import { customArtworkService } from '../../services/customArtworkService'
import type { CustomOrderImage } from '../../types/customArtwork'

export function ReferenceImage({ image }: { image: CustomOrderImage }) {
  const [source, setSource] = useState<string | null>(null)
  const [failed, setFailed] = useState(false)
  useEffect(() => {
    let active = true
    let objectUrl: string | null = null
    void customArtworkService.getImageContent(image.imageUrl).then((blob) => {
      if (!active) return
      objectUrl = URL.createObjectURL(blob); setSource(objectUrl)
    }).catch(() => { if (active) setFailed(true) })
    return () => { active = false; if (objectUrl) URL.revokeObjectURL(objectUrl) }
  }, [image.imageUrl])
  if (failed) return <div className="reference-image-fallback" role="img" aria-label={`${image.originalFilename} preview unavailable`}>Preview unavailable</div>
  if (!source) return <div className="reference-image-fallback" role="status">Loading preview…</div>
  return <a href={source} target="_blank" rel="noreferrer" aria-label={`View full-size ${image.originalFilename}`}><img className="reference-image-preview" src={source} alt={`Reference artwork: ${image.originalFilename}`} /></a>
}
