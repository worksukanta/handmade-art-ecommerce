import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { EmptyState } from '../components/feedback/EmptyState'
import { ErrorState } from '../components/feedback/ErrorState'
import { LoadingState } from '../components/feedback/LoadingState'
import { adminCommerceService } from '../services/adminCommerceService'
import type { AdminProductSummary, InventoryPage } from '../types/admin'
import { normalizeApiError } from '../utils/apiError'
import { formatDateTime } from '../utils/format'

export function AdminInventoryPage() {
  const [params] = useSearchParams()
  const focusedId = Number(params.get('productId')) || null
  const [data, setData] = useState<InventoryPage | null>(null)
  const [products, setProducts] = useState<Map<number, AdminProductSummary>>(new Map())
  const [page, setPage] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState<number | null>(null)
  const [values, setValues] = useState<Record<number, string>>({})
  const [loading, setLoading] = useState(true)
  const saving = useRef(false)
  const [rowErrors, setRowErrors] = useState<Record<number, string>>({})
  const [message, setMessage] = useState('')
  const dirty = (data?.content ?? []).filter((row) => values[row.product_id] === '' || Number(values[row.product_id]) !== row.quantity_on_hand)
  const valid = (id: number) => /^\d+$/.test(values[id] ?? '') && Number(values[id]) <= 2147483647

  const saveRows = async (ids: number[]) => {
    if (saving.current || !ids.length || ids.some((id) => !valid(id))) return
    saving.current = true
    setBusy(ids[0])
    setMessage('')
    const results = await Promise.allSettled(ids.map(async (id) => {
      const result = await adminCommerceService.updateInventory(id, Number(values[id]))
      setData((current) => current && ({ ...current, content: current.content.map((row) => row.product_id === id ? result : row) }))
      setValues((current) => ({ ...current, [id]: String(result.quantity_on_hand) }))
      setRowErrors((current) => { const next = { ...current }; delete next[id]; return next })
    }))
    results.forEach((result, index) => {
      if (result.status === 'rejected') setRowErrors((current) => ({ ...current, [ids[index]]: normalizeApiError(result.reason).message }))
    })
    const failed = results.filter((result) => result.status === 'rejected').length
    setMessage(`${ids.length - failed} saved. ${failed ? `${failed} failed; review the marked rows and retry.` : 'All submitted changes saved.'}`)
    setBusy(null)
    saving.current = false
  }

  const load = useCallback(async () => {
    try {
      const [inventory, productPage] = await Promise.all([
        focusedId ? adminCommerceService.getInventory(focusedId).then((row) => ({ content: [row], page: 0, size: 1, total_elements: 1, total_pages: 1 })) : adminCommerceService.listInventory(page),
        adminCommerceService.listProducts(0, 100),
      ])
      setData(inventory)
      setProducts(new Map(productPage.content.map((p) => [p.id, p])))
      setValues(Object.fromEntries(inventory.content.map((i) => [i.product_id, String(i.quantity_on_hand)])))
      setError(null)
    } catch (e) {
      setError(normalizeApiError(e).message)
    } finally {
      setLoading(false)
    }
  }, [page, focusedId])

  useEffect(() => {
    void Promise.resolve().then(load)
  }, [load])

  if (loading && !data && !error) return <LoadingState label="Loading inventory" />

  return (
    <section className="commerce-page">
      <Link className="back-link" to="/admin/products">← Products</Link>
      <div className="page-heading">
        <p className="eyebrow">Admin catalogue</p>
        <h1>Inventory</h1>
        <p>Set authoritative stock levels for inventory-tracked products.</p>
        {focusedId && <p>Showing product #{focusedId}. <Link to="/admin/inventory">View all inventory</Link></p>}
      </div>
      {error && <ErrorState title="Inventory operation failed" message={error} onRetry={() => void load()} />}
      {!data?.content.length ? (
        <EmptyState title="No inventory records" message="Inventory records will appear when inventory-tracked products exist." />
      ) : (
        <>
          <div className="form-actions">
            <button className="button button-primary" disabled={busy !== null || !dirty.length || dirty.some((row) => !valid(row.product_id))} onClick={() => void saveRows(dirty.map((row) => row.product_id))}>Save changed</button>
            <span>{dirty.length} changed on this page. Each row is saved independently.</span>
          </div>
          {message && <p role="status">{message}</p>}
          <div className="admin-table-wrap">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Quantity on hand</th>
                  <th>Updated</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((i) => (
                  <tr key={i.product_id}>
                    <td>
                      <Link to={`/admin/products/${i.product_id}`}>
                        {products.get(i.product_id)?.name ?? `Product #${i.product_id}`}
                      </Link>
                    </td>
                    <td>
                      <input
                        aria-label={`Quantity for product ${i.product_id}`}
                        min="0"
                        step="1"
                        type="number"
                        disabled={busy !== null}
                        aria-invalid={Boolean(rowErrors[i.product_id]) || !valid(i.product_id)}
                        aria-describedby={`stock-status-${i.product_id}`}
                        value={values[i.product_id] ?? ''}
                        onChange={(e) => setValues({ ...values, [i.product_id]: e.target.value })}
                      />
                      <div id={`stock-status-${i.product_id}`}>
                        {dirty.some((row) => row.product_id === i.product_id) && <span>Unsaved changes</span>}
                        {rowErrors[i.product_id] && <p role="alert">{rowErrors[i.product_id]}</p>}
                      </div>
                    </td>
                    <td>{formatDateTime(i.updated_at)}</td>
                    <td>
                      <button
                        className="button button-secondary"
                        disabled={busy !== null || !valid(i.product_id) || !dirty.some((row) => row.product_id === i.product_id)}
                        onClick={() => void saveRows([i.product_id])}
                      >
                        {busy === i.product_id ? 'Saving…' : 'Save'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {data.total_pages > 1 && (
            <nav className="pagination" aria-label="Admin inventory pagination">
              <button disabled={busy !== null || dirty.length > 0 || page === 0} onClick={() => setPage(page - 1)}>
                Previous
              </button>
              <span>
                Page {page + 1} of {data.total_pages}
              </span>
              <button disabled={busy !== null || dirty.length > 0 || page + 1 >= data.total_pages} onClick={() => setPage(page + 1)}>
                Next
              </button>
            </nav>
          )}
        </>
      )}
    </section>
  )
}
