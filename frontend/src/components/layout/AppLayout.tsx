import { useRef, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { useCart } from '../../hooks/useCart'

import { useDismiss } from '../../hooks/useDismiss'
import { NavigationGroup } from './NavigationGroup'

export function AppLayout() {
  const { isAuthenticated, isInitializing, signOut, user } = useAuth()
  const navigate = useNavigate()
  const { itemCount } = useCart()
  const location = useLocation()
  const [openAt, setOpenAt] = useState<string | null>(null)
  const menuOpen = openAt === location.key
  const navRef = useRef<HTMLElement>(null)
  const toggleRef = useRef<HTMLButtonElement>(null)
  const closeMenu = () => {
    setOpenAt(null)
    navRef.current?.querySelectorAll('details[open]').forEach((group) => { (group as HTMLDetailsElement).open = false })
  }
  useDismiss([navRef, toggleRef], menuOpen, (escape) => {
    if (escape && navRef.current?.querySelector('details[open]')) return
    closeMenu()
    if (escape) toggleRef.current?.focus()
  })
  const inSection = (path: string) => location.pathname === path || location.pathname.startsWith(path + '/')

  const handleSignOut = () => {
    signOut()
    navigate('/')
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <Link className="brand" to="/">Handmade Art</Link>
        <div className="mobile-header-actions">
          {!isInitializing && user?.role === 'CUSTOMER' && <NavLink to="/cart" onClick={closeMenu}>Cart{itemCount > 0 ? ` (${itemCount})` : ''}</NavLink>}
          <button ref={toggleRef} className="button button-secondary menu-toggle" type="button" aria-label="Toggle navigation menu" aria-controls="primary-navigation" aria-expanded={menuOpen} onClick={() => setOpenAt(menuOpen ? null : location.key)}>☰ Menu</button>
        </div>
        <nav ref={navRef} id="primary-navigation" className={menuOpen ? 'primary-navigation is-open' : 'primary-navigation'} aria-label="Primary navigation" onClick={(event) => { if ((event.target as HTMLElement).closest('a')) closeMenu() }}>
          <ul className="nav-list">
            <li><NavLink to="/" end>Catalogue</NavLink></li>
            {!isInitializing && !isAuthenticated && (
              <>
                <li><NavLink to="/login">Sign in</NavLink></li>
                <li><NavLink to="/register">Register</NavLink></li>
              </>
            )}
            {!isInitializing && user?.role === 'CUSTOMER' && (
              <>
                <li><NavLink to="/cart">Cart{itemCount > 0 ? ` (${itemCount})` : ''}</NavLink></li>
                <li><NavLink to="/orders">Orders</NavLink></li>
                <li><NavLink to="/custom-requests">Custom requests</NavLink></li>
                <li><NavigationGroup key={location.key} label="Account" active={inSection("/account")}>
                  <li><NavLink to="/account/profile">Profile</NavLink></li>
                  <li><NavLink to="/account/addresses">Addresses</NavLink></li>
                  <li><button type="button" onClick={handleSignOut}>Sign out</button></li>
                </NavigationGroup></li>
              </>
            )}
            {!isInitializing && user?.role === 'ADMIN' && (
              <li><NavigationGroup key={location.key} label="Admin" active={inSection("/admin")}>
                <li><NavLink to="/admin/custom-requests">Custom requests</NavLink></li>
                <li><NavLink to="/admin/products">Products</NavLink></li>
                <li><NavLink to="/admin/categories">Categories</NavLink></li>
                <li><NavLink to="/admin/inventory">Inventory</NavLink></li>
                <li><NavLink to="/admin/orders">Orders</NavLink></li>
              </NavigationGroup></li>
            )}
            {!isInitializing && isAuthenticated && user?.role !== 'CUSTOMER' && (
              <li><button type="button" onClick={handleSignOut}>Sign out</button></li>
            )}
          </ul>
        </nav>
      </header>
      <main className="app-main"><Outlet /></main>
      <footer className="app-footer">
        Handmade &amp; Custom Artwork E-Commerce Platform
      </footer>
    </div>
  )
}
