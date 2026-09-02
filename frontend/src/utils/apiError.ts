import axios from 'axios'

export interface ApiErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
  details?: string[]
}

export interface NormalizedApiError {
  status: number | null
  message: string
  details: string[]
}

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  if (typeof value !== 'object' || value === null) {
    return false
  }

  const candidate = value as Record<string, unknown>
  return (
    typeof candidate.status === 'number' &&
    typeof candidate.message === 'string' &&
    (candidate.details === undefined ||
      (Array.isArray(candidate.details) &&
        candidate.details.every((detail) => typeof detail === 'string')))
  )
}

export function normalizeApiError(error: unknown): NormalizedApiError {
  if (axios.isAxiosError(error)) {
    const data: unknown = error.response?.data

    if (isApiErrorResponse(data)) {
      return {
        status: data.status,
        message: data.message,
        details: data.details ?? [],
      }
    }

    if (!error.response) {
      return {
        status: null,
        message: 'Unable to reach the server. Please try again.',
        details: [],
      }
    }

    return {
      status: error.response.status,
      message: 'The request could not be completed.',
      details: [],
    }
  }

  return {
    status: null,
    message: 'An unexpected error occurred.',
    details: [],
  }
}
