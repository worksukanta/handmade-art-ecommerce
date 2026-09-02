import { Link } from 'react-router-dom'

export function UnauthorizedPage() {
  return (
    <section>
      <h1>Access denied</h1>
      <p>You do not have permission to view this page.</p>
      <Link to="/">Return home</Link>
    </section>
  )
}
