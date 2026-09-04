import { AxiosError, type AxiosResponse } from 'axios'
import { describe, expect, it } from 'vitest'
import { normalizeApiError } from '../utils/apiError'

const normalized = (status: number) => new AxiosError('failed', undefined, undefined, undefined, {
  status, statusText: '', headers: {}, config: { headers: {} },
  data: { timestamp: '', status, error: 'Error', message: `Backend ${status}`, path: '/x', details: ['Useful detail'] },
} as AxiosResponse)
const bare = (status: number) => new AxiosError('failed', undefined, undefined, undefined, { status, statusText: '', headers: {}, config: { headers: {} }, data: null } as AxiosResponse)

describe('normalizeApiError', () => {
  it.each([400, 401, 403, 404, 409, 413])('preserves normalized backend information for %i', (status) => {
    expect(normalizeApiError(normalized(status))).toEqual({ status, message: `Backend ${status}`, details: ['Useful detail'] })
  })
  it('provides a useful upload-size fallback for a bare 413', () => expect(normalizeApiError(bare(413))).toMatchObject({ status: 413, message: expect.stringContaining('too large') }))
  it('normalizes a network failure', () => expect(normalizeApiError(new AxiosError('Network'))).toEqual({ status: null, message: 'Unable to reach the server. Please try again.', details: [] }))
  it('normalizes an unexpected failure', () => expect(normalizeApiError(new Error('boom'))).toEqual({ status: null, message: 'An unexpected error occurred.', details: [] }))
})
