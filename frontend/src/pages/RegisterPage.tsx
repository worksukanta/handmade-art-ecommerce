import { useRef, useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { FormError } from '../components/forms/FormError'
import { FormField } from '../components/forms/FormField'
import { SubmitButton } from '../components/forms/SubmitButton'
import { useAuth } from '../hooks/useAuth'
import type { RegisterRequest } from '../types/auth'
import { normalizeApiError, type NormalizedApiError } from '../utils/apiError'

interface RegistrationErrors {
  name?: string
  email?: string
  password?: string
  confirmPassword?: string
  phone?: string
}

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function RegisterPage() {
  const { isAuthenticated, register, user } = useAuth()
  const navigate = useNavigate()
  const errorRef = useRef<HTMLDivElement>(null)
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [errors, setErrors] = useState<RegistrationErrors>({})
  const [apiError, setApiError] = useState<NormalizedApiError | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  if (isAuthenticated && user) {
    return <Navigate to={user.role === 'ADMIN' ? '/admin/products' : '/'} replace />
  }

  const validate = (): RegistrationErrors => {
    const nextErrors: RegistrationErrors = {}
    const normalizedName = name.trim()
    const normalizedEmail = email.trim()
    const normalizedPhone = phone.trim()

    if (!normalizedName) nextErrors.name = 'Full name is required.'
    else if (normalizedName.length > 150) nextErrors.name = 'Full name must not exceed 150 characters.'

    if (!normalizedEmail) nextErrors.email = 'Email is required.'
    else if (!emailPattern.test(normalizedEmail)) nextErrors.email = 'Enter a valid email address.'
    else if (normalizedEmail.length > 254) nextErrors.email = 'Email must not exceed 254 characters.'

    if (!password) nextErrors.password = 'Password is required.'
    else if (password.length < 8 || password.length > 100) {
      nextErrors.password = 'Password must be between 8 and 100 characters.'
    }

    if (!confirmPassword) nextErrors.confirmPassword = 'Confirm your password.'
    else if (confirmPassword !== password) nextErrors.confirmPassword = 'Passwords do not match.'

    if (normalizedPhone.length > 20) nextErrors.phone = 'Phone must not exceed 20 characters.'

    return nextErrors
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (isSubmitting) return

    const validationErrors = validate()
    setErrors(validationErrors)
    setApiError(null)
    if (Object.keys(validationErrors).length > 0) return

    const request: RegisterRequest = {
      name: name.trim(),
      email: email.trim(),
      password,
      ...(phone.trim() ? { phone: phone.trim() } : {}),
    }

    setIsSubmitting(true)
    try {
      await register(request)
      navigate('/login', { replace: true, state: { registrationComplete: true } })
    } catch (error) {
      const normalizedError = normalizeApiError(error)
      if (normalizedError.status === 409) {
        setErrors((current) => ({ ...current, email: normalizedError.message }))
      }
      setApiError(normalizedError)
      window.requestAnimationFrame(() => errorRef.current?.focus())
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="auth-page" aria-labelledby="register-heading">
      <div className="auth-card">
        <h1 id="register-heading">Create an account</h1>
        {apiError && <FormError ref={errorRef} message={apiError.message} details={apiError.details} />}
        <form noValidate onSubmit={handleSubmit}>
          <FormField id="register-name" label="Full name" autoComplete="name" value={name}
            error={errors.name} onChange={(event) => setName(event.target.value)} disabled={isSubmitting}
            maxLength={150} required />
          <FormField id="register-email" label="Email" type="email" autoComplete="email" value={email}
            error={errors.email} onChange={(event) => setEmail(event.target.value)} disabled={isSubmitting}
            maxLength={254} required />
          <FormField id="register-phone" label="Phone (optional)" type="tel" autoComplete="tel" value={phone}
            error={errors.phone} onChange={(event) => setPhone(event.target.value)} disabled={isSubmitting}
            maxLength={20} />
          <FormField id="register-password" label="Password" type="password" autoComplete="new-password"
            value={password} error={errors.password} onChange={(event) => setPassword(event.target.value)}
            disabled={isSubmitting} minLength={8} maxLength={100} required />
          <FormField id="register-confirm-password" label="Confirm password" type="password"
            autoComplete="new-password" value={confirmPassword} error={errors.confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)} disabled={isSubmitting} required />
          <SubmitButton isSubmitting={isSubmitting} idleLabel="Create account"
            submittingLabel="Creating account…" />
        </form>
        <p>Already have an account? <Link to="/login">Sign in</Link>.</p>
      </div>
    </section>
  )
}
