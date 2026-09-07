import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AppLayout } from '../components/layout/AppLayout'
import { admin, authValue, customer, renderWithRouter } from './helpers'
vi.mock('../hooks/useCart', () => ({ useCart: () => ({ itemCount: 2 }) }))

describe('responsive navigation', () => {
  it('opens and closes the menu by keyboard and closes after route selection', async () => {
    renderWithRouter(<Routes><Route element={<AppLayout />}><Route path="*" element={<p>Page</p>} /></Route></Routes>, { auth: authValue(customer) })
    const toggle = screen.getByRole('button', { name: 'Toggle navigation menu' })
    expect(toggle).toHaveAttribute('aria-expanded', 'false')
    toggle.focus()
    await userEvent.keyboard('{Enter}')
    expect(toggle).toHaveAttribute('aria-expanded', 'true')
    await userEvent.keyboard('{Escape}')
    expect(toggle).toHaveAttribute('aria-expanded', 'false')
    await userEvent.click(toggle)
    await userEvent.click(screen.getByRole('link', { name: 'Orders' }))
    expect(toggle).toHaveAttribute('aria-expanded', 'false')
    expect(screen.queryByText('Admin')).not.toBeInTheDocument()
    await userEvent.click(screen.getByText('Account'))
    expect(screen.getByRole('link', { name: 'Profile' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Addresses' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sign out' })).toBeInTheDocument()
  })
  it('preserves all ADMIN destinations without customer links', async () => {
    renderWithRouter(<AppLayout />, { auth: authValue(admin) })
    await userEvent.click(screen.getByText('Admin'))
    const nav = within(screen.getByRole('navigation'))
    for (const name of ['Products', 'Categories', 'Inventory', 'Orders', 'Custom requests']) expect(nav.getByRole('link', { name })).toBeInTheDocument()
    expect(nav.queryByRole('link', { name: /Cart|Profile|Addresses/ })).not.toBeInTheDocument()
  })
  it('preserves anonymous sign-in and registration', () => {
    renderWithRouter(<AppLayout />)
    expect(screen.getByRole('link', { name: 'Sign in' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Register' })).toBeInTheDocument()
    expect(screen.queryByText('Admin')).not.toBeInTheDocument()
  })
})
