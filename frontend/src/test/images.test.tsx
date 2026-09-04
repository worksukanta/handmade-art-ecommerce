import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ImageLightbox } from '../components/common/ImageLightbox'
import { ReferenceImage } from '../components/customArtwork/ReferenceImage'
import { customArtworkService } from '../services/customArtworkService'

vi.mock('../services/customArtworkService', () => ({ customArtworkService: { getImageContent: vi.fn() } }))
const image = { id: 4, customOrderRequestId: 3, imageUrl: '/custom-requests/3/images/4/content', storageReference: 'private/never-render-this', originalFilename: 'sketch.png', contentType: 'image/png', fileSizeBytes: 42, uploadedAt: '2026-01-01T00:00:00Z' }

describe('ImageLightbox', () => {
  it('has dialog semantics, alt text, initial focus, and closes explicitly', async () => {
    const close = vi.fn()
    render(<ImageLightbox src="blob:preview" alt="Detailed artwork" onClose={close} />)
    expect(screen.getByRole('dialog', { name: 'Image preview' })).toBeInTheDocument()
    expect(screen.getByRole('img', { name: 'Detailed artwork' })).toHaveAttribute('src', 'blob:preview')
    expect(screen.getByRole('button', { name: 'Close image preview' })).toHaveFocus()
    await userEvent.click(screen.getByRole('button', { name: 'Close image preview' }))
    expect(close).toHaveBeenCalledOnce()
  })
  it('closes with Escape and the backdrop', async () => {
    const close = vi.fn()
    const { container } = render(<ImageLightbox src="x" alt="Artwork" onClose={close} />)
    await userEvent.keyboard('{Escape}')
    await userEvent.click(container.querySelector('.lightbox-overlay')!)
    expect(close).toHaveBeenCalledTimes(2)
  })
})

describe('ReferenceImage regression', () => {
  it('uses authenticated content retrieval, creates a preview, and revokes its URL', async () => {
    vi.mocked(customArtworkService.getImageContent).mockResolvedValue(new Blob(['x']))
    const create = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:safe')
    const revoke = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined)
    const { unmount } = render(<ReferenceImage image={image} />)
    expect(await screen.findByRole('img', { name: 'Reference artwork: sketch.png' })).toHaveAttribute('src', 'blob:safe')
    expect(customArtworkService.getImageContent).toHaveBeenCalledWith(image.imageUrl)
    expect(screen.queryByText(image.storageReference)).not.toBeInTheDocument()
    unmount()
    expect(create).toHaveBeenCalled()
    expect(revoke).toHaveBeenCalledWith('blob:safe')
  })
  it('renders a useful fallback when binary retrieval fails', async () => {
    vi.mocked(customArtworkService.getImageContent).mockRejectedValue(new Error('nope'))
    render(<ReferenceImage image={image} />)
    await waitFor(() => expect(screen.getByRole('img', { name: 'sketch.png unavailable' })).toHaveTextContent('Reference image unavailable'))
  })
})
