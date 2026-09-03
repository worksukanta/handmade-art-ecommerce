import { useEffect, useRef } from 'react'

interface ImageLightboxProps {
  src: string | null
  alt: string
  onClose: () => void
}

export function ImageLightbox({ src, alt, onClose }: ImageLightboxProps) {
  const dialogRef = useRef<HTMLDivElement>(null)
  const closeButtonRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (!src) return

    // Focus the close button when opened
    closeButtonRef.current?.focus()

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault()
        onClose()
      }
    }

    // Trap focus inside modal
    const handleFocusTrap = (event: KeyboardEvent) => {
      if (event.key !== 'Tab' || !dialogRef.current) return
      const focusable = dialogRef.current.querySelectorAll<HTMLElement>(
        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
      )
      if (focusable.length === 0) return
      const first = focusable[0]
      const last = focusable[focusable.length - 1]

      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault()
        last.focus()
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault()
        first.focus()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    document.addEventListener('keydown', handleFocusTrap)

    // Prevent body scrolling while modal is open
    const originalOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.removeEventListener('keydown', handleFocusTrap)
      document.body.style.overflow = originalOverflow
    }
  }, [src, onClose])

  if (!src) return null

  return (
    <div
      className="lightbox-overlay"
      onClick={(e) => {
        if (e.target === e.currentTarget) {
          onClose()
        }
      }}
      role="presentation"
    >
      <div
        ref={dialogRef}
        className="lightbox-dialog"
        role="dialog"
        aria-modal="true"
        aria-label="Image preview"
      >
        <button
          ref={closeButtonRef}
          type="button"
          className="lightbox-close-button"
          onClick={onClose}
          aria-label="Close image preview"
        >
          ✕
        </button>
        <div className="lightbox-content">
          <img src={src} alt={alt || 'Artwork preview'} className="lightbox-image" />
          {alt && <p className="lightbox-caption">{alt}</p>}
        </div>
      </div>
    </div>
  )
}
