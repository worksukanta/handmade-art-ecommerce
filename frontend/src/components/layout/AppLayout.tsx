import { Link, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'

export function AppLayout() {
  const { isAuthenticated, isInitializing, signOut, user } = useAuth()
  const navigate = useNavigate()

  const handleSignOut = () => {
    signOut()
    navigate('/')
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <Link className="brand" to="/">Handmade Art</Link>
        <nav aria-label="Primary navigation">
          <ul className="nav-list">
            <li><Link to="/">Catalogue</Link></li>
            {!isInitializing && !isAuthenticated && (
              <>
                <li><Link to="/login">Sign in</Link></li>
                <li><Link to="/register">Register</Link></li>
              </>
            )}
            {!isInitializing && user?.role === 'CUSTOMER' && (
              <li><Link to="/account/profile">Customer area</Link></li>
            )}
            {!isInitializing && user?.role === 'ADMIN' && (
              <li><Link to="/admin/products">Admin area</Link></li>
            )}
            {!isInitializing && isAuthenticated && (
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
