import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { StatusBadge } from '../components/commerce/StatusBadge'
import { ErrorState } from '../components/feedback/ErrorState'
import { LoadingState } from '../components/feedback/LoadingState'
import { adminCommerceService } from '../services/adminCommerceService'
import type { AdminCategory } from '../types/admin'
import { normalizeApiError } from '../utils/apiError'

export function AdminCategoriesPage() {
  const [items, setItems] = useState<AdminCategory[]>([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [editing, setEditing] = useState<number | null>(null)
  const load = useCallback(async () => {
    try { setItems(await adminCommerceService.listCategories()); setError(null) }
    catch (cause) { setError(normalizeApiError(cause).message) }
    finally { setLoading(false) }
  }, [])
  useEffect(() => { void Promise.resolve().then(load) }, [load])
  const resetForm = () => { setEditing(null); setName(''); setDescription('') }
  const submit = async (event: FormEvent) => {
    event.preventDefault(); setBusy(true); setError(null)
    try {
      if (editing !== null) await adminCommerceService.updateCategory(editing, name, description || null)
      else await adminCommerceService.createCategory(name, description || null)
      resetForm(); await load()
    } catch (cause) { setError(normalizeApiError(cause).message) }
    finally { setBusy(false) }
  }
  const changeStatus = async (category: AdminCategory) => {
    setBusy(true); setError(null)
    try {
      await adminCommerceService.setCategoryStatus(category.id, category.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE')
      await load()
    } catch (cause) { setError(normalizeApiError(cause).message) }
    finally { setBusy(false) }
  }
  if (loading) return <LoadingState label="Loading categories" />
  return <section className="commerce-page">
    <Link className="back-link" to="/admin/products">← Products</Link>
    <div className="page-heading"><p className="eyebrow">Admin catalogue</p><h1>Categories</h1></div>
    {error && <ErrorState title="Category operation failed" message={error} onRetry={() => void load()} />}
    <form className="commerce-form admin-form-grid" onSubmit={submit}>
      <label>Name<input required maxLength={100} value={name} onChange={(event) => setName(event.target.value)} /></label>
      <label>Description<input value={description} onChange={(event) => setDescription(event.target.value)} /></label>
      <div className="form-actions"><button disabled={busy}>{editing !== null ? 'Save category' : 'Create category'}</button>{editing !== null && <button type="button" className="button button-secondary" onClick={resetForm}>Cancel</button>}</div>
    </form>
    <div className="admin-table-wrap"><table className="admin-table">
      <thead><tr><th>Name</th><th>Description</th><th>Status</th><th>Actions</th></tr></thead>
      <tbody>{items.map((category) => <tr key={category.id}>
        <td>{category.name}</td><td>{category.description || '—'}</td><td><StatusBadge kind="generic" value={category.status} /></td>
        <td><button className="text-button" disabled={busy} onClick={() => { setEditing(category.id); setName(category.name); setDescription(category.description || '') }}>Edit</button>{' '}<button className="text-button" disabled={busy} onClick={() => void changeStatus(category)}>{category.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}</button></td>
      </tr>)}</tbody>
    </table></div>
  </section>
}
