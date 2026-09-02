import { createContext } from 'react'
import type {
  LoginRequest,
  RegisterRequest,
  UserResponse,
  UserSummary,
} from '../types/auth'

export interface AuthContextValue {
  user: UserSummary | null
  isAuthenticated: boolean
  isInitializing: boolean
  login: (request: LoginRequest) => Promise<UserSummary>
  register: (request: RegisterRequest) => Promise<UserResponse>
  signOut: () => void
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)
