import { Link, Outlet } from 'react-router-dom'

export function AppLayout() {
  return (
    <div className="app-shell">
      <header className="app-header">
        <Link className="brand" to="/">Handmade Art</Link>
      </header>
      <main className="app-main"><Outlet /></main>
      <footer className="app-footer">
        Handmade &amp; Custom Artwork E-Commerce Platform
      </footer>
    </div>
  )
}
