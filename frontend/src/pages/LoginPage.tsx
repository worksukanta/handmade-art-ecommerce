import { useRef, useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate, type Location } from 'react-router-dom'
import { FormError } from '../components/forms/FormError'
import { FormField } from '../components/forms/FormField'
import { SubmitButton } from '../components/forms/SubmitButton'
import { useAuth } from '../hooks/useAuth'
import type { UserRole } from '../types/auth'
import { normalizeApiError, type NormalizedApiError } from '../utils/apiError'

interface LoginErrors {
  email?: string
  password?: string
}

interface LoginLocationState {
  from?: Location
  registrationComplete?: boolean
}

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function defaultDestination(role: UserRole): string {
  return role === 'ADMIN' ? '/admin/products' : '/'
}

export function LoginPage() {
  const { isAuthenticated, login, user } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const errorRef = useRef<HTMLDivElement>(null)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<LoginErrors>({})
  const [apiError, setApiError] = useState<NormalizedApiError | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const locationState = location.state as LoginLocationState | null

  if (isAuthenticated && user) {
    return <Navigate to={defaultDestination(user.role)} replace />
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (isSubmitting) return

    const nextErrors: LoginErrors = {}
    const normalizedEmail = email.trim()
    if (!normalizedEmail) nextErrors.email = 'Email is required.'
    else if (!emailPattern.test(normalizedEmail)) nextErrors.email = 'Enter a valid email address.'
    if (!password) nextErrors.password = 'Password is required.'

    setErrors(nextErrors)
    setApiError(null)
    if (Object.keys(nextErrors).length > 0) return

    setIsSubmitting(true)
    try {
      const authenticatedUser = await login({ email: normalizedEmail, password })
      const requestedLocation = locationState?.from
      const destination = requestedLocation
        ? `${requestedLocation.pathname}${requestedLocation.search}${requestedLocation.hash}`
        : defaultDestination(authenticatedUser.role)
      navigate(destination, { replace: true })
    } catch (error) {
      const normalizedError = normalizeApiError(error)
      setApiError(normalizedError.status === 401
        ? { ...normalizedError, message: 'Invalid email or password.' }
        : normalizedError)
      window.requestAnimationFrame(() => errorRef.current?.focus())
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="auth-page" aria-labelledby="login-heading">
      <div className="auth-card">
        <h1 id="login-heading">Sign in</h1>
        {locationState?.registrationComplete && (
          <p className="form-alert form-alert-success" role="status">
            Account created successfully. Sign in to continue.
          </p>
        )}
        {apiError && <FormError ref={errorRef} message={apiError.message} details={apiError.details} />}
        <form noValidate onSubmit={handleSubmit}>
          <FormField id="login-email" label="Email" type="email" autoComplete="email"
            value={email} error={errors.email} onChange={(event) => setEmail(event.target.value)}
            disabled={isSubmitting} required />
          <FormField id="login-password" label="Password" type="password" autoComplete="current-password"
            value={password} error={errors.password} onChange={(event) => setPassword(event.target.value)}
            disabled={isSubmitting} required />
          <SubmitButton isSubmitting={isSubmitting} idleLabel="Sign in" submittingLabel="Signing in…" />
        </form>
        <p>Need an account? <Link to="/register">Register</Link>.</p>
      </div>
    </section>
  )
}
