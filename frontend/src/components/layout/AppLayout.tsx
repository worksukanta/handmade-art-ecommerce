import { useState } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { useCart } from '../../hooks/useCart'

export function AppLayout() {
  const { isAuthenticated, isInitializing, signOut, user } = useAuth()
  const navigate = useNavigate()
  const { itemCount } = useCart()
  const location = useLocation()
  const [openAt, setOpenAt] = useState<string | null>(null)
  const menuOpen = openAt === location.key
  const closeMenu = () => setOpenAt(null)

  const handleSignOut = () => {
    signOut()
    navigate('/')
  }

  return (
    <div className="app-shell">
      <header className="app-header" onKeyDown={(event) => {
        if (event.key === 'Escape') {
          const group = (event.target as HTMLElement).closest('details')
          if (group?.open) { group.open = false; group.querySelector('summary')?.focus() }
          else { closeMenu(); event.currentTarget.querySelector<HTMLButtonElement>('.menu-toggle')?.focus() }
        }
      }}>
        <Link className="brand" to="/">Handmade Art</Link>
        <div className="mobile-header-actions">
          {!isInitializing && user?.role === 'CUSTOMER' && <Link to="/cart" onClick={closeMenu}>Cart{itemCount > 0 ? ` (${itemCount})` : ''}</Link>}
          <button className="button button-secondary menu-toggle" type="button" aria-label="Toggle navigation menu" aria-controls="primary-navigation" aria-expanded={menuOpen} onClick={() => setOpenAt(menuOpen ? null : location.key)}>☰ Menu</button>
        </div>
        <nav id="primary-navigation" className={menuOpen ? 'primary-navigation is-open' : 'primary-navigation'} aria-label="Primary navigation" onClick={(event) => { if ((event.target as HTMLElement).closest('a')) closeMenu() }}>
          <ul className="nav-list">
            <li><Link to="/">Catalogue</Link></li>
            {!isInitializing && !isAuthenticated && (
              <>
                <li><Link to="/login">Sign in</Link></li>
                <li><Link to="/register">Register</Link></li>
              </>
            )}
            {!isInitializing && user?.role === 'CUSTOMER' && (
              <>
                <li><Link to="/cart">Cart{itemCount > 0 ? ` (${itemCount})` : ''}</Link></li>
                <li><Link to="/orders">Orders</Link></li>
                <li><Link to="/custom-requests">Custom requests</Link></li>
                <li><details key={location.key} className="nav-group"><summary>Account</summary><ul>
                  <li><Link to="/account/profile">Profile</Link></li>
                  <li><Link to="/account/addresses">Addresses</Link></li>
                  <li><button type="button" onClick={handleSignOut}>Sign out</button></li>
                </ul></details></li>
              </>
            )}
            {!isInitializing && user?.role === 'ADMIN' && (
              <li><details key={location.key} className="nav-group"><summary>Admin</summary><ul>
                <li><Link to="/admin/custom-requests">Custom requests</Link></li>
                <li><Link to="/admin/products">Products</Link></li>
                <li><Link to="/admin/categories">Categories</Link></li>
                <li><Link to="/admin/inventory">Inventory</Link></li>
                <li><Link to="/admin/orders">Orders</Link></li>
              </ul></details></li>
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
