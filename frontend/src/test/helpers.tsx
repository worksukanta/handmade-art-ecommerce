import type { ReactElement } from 'react'
import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AuthContext, type AuthContextValue } from '../context/authContext'
import type { UserSummary } from '../types/auth'

export const customer: UserSummary = { id: 1, name: 'Customer', email: 'customer@example.com', role: 'CUSTOMER' }
export const admin: UserSummary = { id: 2, name: 'Admin', email: 'admin@example.com', role: 'ADMIN' }

export function authValue(user: UserSummary | null = null): AuthContextValue {
  return {
    user,
    isAuthenticated: user !== null,
    isInitializing: false,
    login: async () => user ?? customer,
    register: async () => ({ id: 1, name: 'Customer', email: 'customer@example.com', phone: null, role: 'CUSTOMER', created_at: '' }),
    signOut: () => undefined,
  }
}

export function renderWithRouter(ui: ReactElement, options: { route?: string; auth?: AuthContextValue } = {}) {
  return render(
    <AuthContext.Provider value={options.auth ?? authValue()}>
      <MemoryRouter initialEntries={[options.route ?? '/']}>{ui}</MemoryRouter>
    </AuthContext.Provider>,
  )
}
