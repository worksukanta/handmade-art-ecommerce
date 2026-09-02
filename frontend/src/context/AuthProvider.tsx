import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react'
import { AuthContext, type AuthContextValue } from './authContext'
import { authUnauthorizedEvent } from '../services/apiClient'
import { authService } from '../services/authService'
import { tokenStorage } from '../services/tokenStorage'
import type {
  LoginRequest,
  RegisterRequest,
  UserResponse,
  UserSummary,
} from '../types/auth'

export function AuthProvider({ children }: PropsWithChildren) {
  const [initialToken] = useState(() => tokenStorage.get())
  const [user, setUser] = useState<UserSummary | null>(null)
  const [isInitializing, setIsInitializing] = useState(initialToken !== null)

  const signOut = useCallback(() => {
    tokenStorage.clear()
    setUser(null)
  }, [])

  const login = useCallback(async (request: LoginRequest) => {
    const response = await authService.login(request)
    tokenStorage.set(response.access_token)
    setUser(response.user)
    return response.user
  }, [])

  const register = useCallback((request: RegisterRequest): Promise<UserResponse> => {
    return authService.register(request)
  }, [])

  useEffect(() => {
    if (!initialToken) {
      return
    }

    let active = true

    authService
      .getCurrentUser()
      .then((currentUser) => {
        if (active) {
          setUser(currentUser)
        }
      })
      .catch(() => {
        if (active) {
          signOut()
        }
      })
      .finally(() => {
        if (active) {
          setIsInitializing(false)
        }
      })

    return () => {
      active = false
    }
  }, [initialToken, signOut])

  useEffect(() => {
    window.addEventListener(authUnauthorizedEvent, signOut)
    return () => window.removeEventListener(authUnauthorizedEvent, signOut)
  }, [signOut])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: user !== null,
      isInitializing,
      login,
      register,
      signOut,
    }),
    [isInitializing, login, register, signOut, user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
