export type UserRole = 'CUSTOMER' | 'ADMIN'

export interface RegisterRequest {
  name: string
  email: string
  password: string
  phone?: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface UserSummary {
  id: number
  name: string
  email: string
  role: UserRole
}

export interface UserResponse extends UserSummary {
  phone: string | null
  created_at: string
}

export interface LoginResponse {
  access_token: string
  token_type: 'Bearer'
  user: UserSummary
}
