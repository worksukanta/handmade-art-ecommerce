import { type FormEvent, useEffect, useState } from 'react'
import { FormError } from '../components/forms/FormError'
import { FormField } from '../components/forms/FormField'
import { SubmitButton } from '../components/forms/SubmitButton'
import { LoadingState } from '../components/feedback/LoadingState'
import { accountService } from '../services/accountService'
import type { Profile } from '../types/commerce'
import { normalizeApiError } from '../utils/apiError'

export function ProfilePage() {
  const [profile, setProfile] = useState<Profile | null>(null); const [name, setName] = useState(''); const [phone, setPhone] = useState(''); const [error, setError] = useState<string | null>(null); const [saved, setSaved] = useState(false); const [submitting, setSubmitting] = useState(false)
  useEffect(() => { let current = true; void accountService.getProfile().then((data) => { if (current) { setProfile(data); setName(data.name); setPhone(data.phone ?? '') } }).catch((cause: unknown) => { if (current) setError(normalizeApiError(cause).message) }); return () => { current = false } }, [])
  const submit = async (event: FormEvent) => { event.preventDefault(); if (!name.trim() || submitting) return; setSubmitting(true); setError(null); setSaved(false); try { const updated = await accountService.updateProfile({ name: name.trim(), phone: phone.trim() || null }); setProfile(updated); setSaved(true) } catch (cause) { setError(normalizeApiError(cause).message) } finally { setSubmitting(false) } }
  if (!profile && !error) return <LoadingState label="Loading your profile" />
  return <section className="commerce-page narrow-page"><div className="page-heading"><p className="eyebrow">Your account</p><h1>Profile</h1></div>{error && <FormError message={error} />}{saved && <p className="form-alert form-alert-success" role="status">Profile updated.</p>}<form className="commerce-form" onSubmit={submit}><FormField id="profile-email" label="Email" type="email" value={profile?.email ?? ''} disabled /><FormField id="profile-name" label="Full name" value={name} maxLength={150} required onChange={(e) => setName(e.target.value)} /><FormField id="profile-phone" label="Phone" type="tel" value={phone} maxLength={20} onChange={(e) => setPhone(e.target.value)} /><SubmitButton isSubmitting={submitting} idleLabel="Save profile" submittingLabel="Saving…" disabled={!name.trim()} /></form></section>
}
