import axios from 'axios'

const defaultApiBaseUrl = 'http://localhost:8080/api/v1'
const configuredApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()

export const apiClient = axios.create({
  baseURL: (configuredApiBaseUrl || defaultApiBaseUrl).replace(/\/$/, ''),
  headers: { Accept: 'application/json' },
})
