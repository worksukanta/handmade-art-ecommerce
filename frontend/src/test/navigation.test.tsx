import { StrictMode } from 'react'
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

describe('menu dismissal and active destinations', () => {
  it('dismisses outside, preserves inside clicks, toggles and restores focus on Escape under StrictMode', async () => {
    const view = renderWithRouter(<StrictMode><AppLayout /></StrictMode>, { auth: authValue(admin) })
    const trigger = screen.getByText('Admin')
    await userEvent.click(trigger)
    await userEvent.click(screen.getByRole('link', { name: 'Products' }).parentElement!.parentElement!)
    expect(screen.getByRole('link', { name: 'Products' })).toBeVisible()
    await userEvent.click(screen.getByText(/Handmade & Custom/))
    expect(screen.getByRole('link', { name: 'Products' })).not.toBeVisible()
    await userEvent.click(trigger)
    await userEvent.keyboard('{Escape}')
    expect(trigger).toHaveFocus()
    expect(screen.getByRole('link', { name: 'Products' })).not.toBeVisible()
    await userEvent.click(trigger)
    await userEvent.click(trigger)
    expect(screen.getByRole('link', { name: 'Products' })).not.toBeVisible()
    const toggle = screen.getByRole('button', { name: 'Toggle navigation menu' })
    await userEvent.click(toggle)
    await userEvent.click(screen.getByRole('navigation'))
    expect(toggle).toHaveAttribute('aria-expanded', 'true')
    await userEvent.click(screen.getByText(/Handmade & Custom/))
    expect(toggle).toHaveAttribute('aria-expanded', 'false')
    view.unmount()
    await userEvent.click(document.body)
  })

  it.each([
    ['/orders/12', customer, 'Orders', null],
    ['/custom-requests/12', customer, 'Custom requests', null],
    ['/admin/products/12/edit', admin, 'Products', 'Admin'],
    ['/admin/orders/12', admin, 'Orders', 'Admin'],
    ['/account/profile', customer, 'Profile', 'Account'],
    ['/account/addresses', customer, 'Addresses', 'Account'],
  ])('marks the destination for %s', async (route, user, label, group) => {
    renderWithRouter(<AppLayout />, { route, auth: authValue(user) })
    if (group) {
      expect(screen.getByText(group)).toHaveAttribute('aria-current', 'true')
      await userEvent.click(screen.getByText(group))
    }
    expect(screen.getByRole('link', { name: label })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('link', { name: 'Catalogue' })).not.toHaveAttribute('aria-current')
    expect(screen.getAllByRole('link').filter((link) => link.getAttribute('aria-current') === 'page')).toHaveLength(1)
  })

  it('does not match unrelated prefixes', async () => {
    renderWithRouter(<AppLayout />, { route: '/admin/products-extra', auth: authValue(admin) })
    await userEvent.click(screen.getByText('Admin'))
    expect(screen.getByRole('link', { name: 'Products' })).not.toHaveAttribute('aria-current')
  })

  it('closes a dropdown after navigating even to the current destination', async () => {
    renderWithRouter(<Routes><Route element={<AppLayout />}><Route path="*" element={<p>Page</p>} /></Route></Routes>, { route: '/admin/products', auth: authValue(admin) })
    await userEvent.click(screen.getByText('Admin'))
    await userEvent.click(screen.getByRole('link', { name: 'Products' }))
    expect(screen.getByRole('link', { name: 'Products' })).not.toBeVisible()
  })
})
