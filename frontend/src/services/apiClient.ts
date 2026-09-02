import axios from 'axios'
import { tokenStorage } from './tokenStorage'

export const authUnauthorizedEvent = 'handmade-art:auth-unauthorized'

const defaultApiBaseUrl = 'http://localhost:8080/api/v1'
const configuredApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()

export const apiClient = axios.create({
  baseURL: (configuredApiBaseUrl || defaultApiBaseUrl).replace(/\/$/, ''),
  headers: { Accept: 'application/json' },
})

apiClient.interceptors.request.use((config) => {
  const token = tokenStorage.get()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      tokenStorage.clear()
      window.dispatchEvent(new Event(authUnauthorizedEvent))
    }

    return Promise.reject(error)
  },
)
