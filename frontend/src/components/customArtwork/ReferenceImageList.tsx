import { ReferenceImage } from './ReferenceImage'
import type { CustomOrderImage } from '../../types/customArtwork'
import { formatDateTime } from '../../utils/format'

export function ReferenceImageList({ images }: { images: CustomOrderImage[] }) {
  return <ul className="reference-file-list reference-image-list">{images.map((image) => <li key={image.id}><ReferenceImage image={image} /><div><strong>{image.originalFilename}</strong><span>{image.contentType} · {formatBytes(image.fileSizeBytes)} · uploaded {formatDateTime(image.uploadedAt)}</span></div></li>)}</ul>
}

function formatBytes(value: number) { if (value < 1024) return `${value} B`; return `${(value / 1024).toFixed(1)} KB` }
