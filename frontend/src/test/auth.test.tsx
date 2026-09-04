import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../context/AuthProvider'
import { AuthContext } from '../context/authContext'
import { RequireAuth } from '../components/routing/RequireAuth'
import { RequireRole } from '../components/routing/RequireRole'
import { LoginPage } from '../pages/LoginPage'
import { authService } from '../services/authService'
import { tokenStorage } from '../services/tokenStorage'
import { admin, authValue, customer } from './helpers'

vi.mock('../services/authService', () => ({ authService: { login: vi.fn(), register: vi.fn(), getCurrentUser: vi.fn() } }))

function LocationProbe() {
  const location = useLocation()
  return <p>{location.pathname}{location.search}</p>
}

function GuardApp({ user = null }: { user?: typeof customer | null }) {
  return <AuthContext.Provider value={authValue(user)}><MemoryRouter initialEntries={['/admin/products?from=test']}><Routes>
    <Route path="/login" element={<><p>Login</p><LocationProbe /></>} />
    <Route path="/unauthorized" element={<p>Unauthorized</p>} />
    <Route element={<RequireAuth />}><Route element={<RequireRole role="ADMIN" />}><Route path="/admin/products" element={<p>Admin products</p>} /></Route></Route>
  </Routes></MemoryRouter></AuthContext.Provider>
}

describe('authentication behavior', () => {
  beforeEach(() => localStorage.clear())

  it('redirects an unauthenticated protected route to login and preserves its location', () => {
    render(<GuardApp />)
    expect(screen.getByText('Login')).toBeInTheDocument()
  })

  it('rejects a CUSTOMER from an ADMIN route', () => {
    render(<GuardApp user={customer} />)
    expect(screen.getByText('Unauthorized')).toBeInTheDocument()
  })

  it('allows an ADMIN through the ADMIN guard', () => {
    render(<GuardApp user={admin} />)
    expect(screen.getByText('Admin products')).toBeInTheDocument()
  })

  it('returns to the protected path after login', async () => {
    const login = vi.fn().mockResolvedValue(customer)
    render(<AuthContext.Provider value={{ ...authValue(), login }}><MemoryRouter initialEntries={[{ pathname: '/login', state: { from: { pathname: '/orders', search: '?page=2', hash: '' } } }]}><Routes><Route path="/login" element={<LoginPage />} /><Route path="/orders" element={<LocationProbe />} /></Routes></MemoryRouter></AuthContext.Provider>)
    const user = userEvent.setup()
    await user.type(screen.getByLabelText('Email'), 'customer@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(await screen.findByText('/orders?page=2')).toBeInTheDocument()
  })

  it('clears token and user on logout', async () => {
    localStorage.setItem('handmade-art.access-token', 'token')
    vi.mocked(authService.getCurrentUser).mockResolvedValue({ ...customer, phone: null, created_at: '' })
    function Consumer() { const value = React.useContext(AuthContext)!; return <>{value.user && <p>{value.user.name}</p>}<button onClick={value.signOut}>Logout</button></> }
    const React = await import('react')
    render(<AuthProvider><Consumer /></AuthProvider>)
    await screen.findByText('Customer')
    await userEvent.click(screen.getByRole('button', { name: 'Logout' }))
    expect(tokenStorage.get()).toBeNull()
    expect(screen.queryByText('Customer')).not.toBeInTheDocument()
  })

  it('clears an invalid startup session', async () => {
    localStorage.setItem('handmade-art.access-token', 'invalid')
    vi.mocked(authService.getCurrentUser).mockRejectedValue(new Error('invalid'))
    render(<AuthProvider><p>App</p></AuthProvider>)
    await waitFor(() => expect(tokenStorage.get()).toBeNull())
  })
})
