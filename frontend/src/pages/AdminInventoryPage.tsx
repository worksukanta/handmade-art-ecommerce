import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { EmptyState } from '../components/feedback/EmptyState'
import { ErrorState } from '../components/feedback/ErrorState'
import { LoadingState } from '../components/feedback/LoadingState'
import { adminCommerceService } from '../services/adminCommerceService'
import type { AdminProductSummary, InventoryPage } from '../types/admin'
import { normalizeApiError } from '../utils/apiError'
import { formatDateTime } from '../utils/format'

export function AdminInventoryPage() {
  const [data, setData] = useState<InventoryPage | null>(null)
  const [products, setProducts] = useState<Map<number, AdminProductSummary>>(new Map())
  const [page, setPage] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState<number | null>(null)
  const [values, setValues] = useState<Record<number, string>>({})
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    try {
      const [inventory, productPage] = await Promise.all([
        adminCommerceService.listInventory(page),
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
  }, [page])

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
      </div>
      {error && <ErrorState title="Inventory operation failed" message={error} onRetry={() => void load()} />}
      {!data?.content.length ? (
        <EmptyState title="No inventory records" message="Inventory records will appear when inventory-tracked products exist." />
      ) : (
        <>
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
                        value={values[i.product_id] ?? ''}
                        onChange={(e) => setValues({ ...values, [i.product_id]: e.target.value })}
                      />
                    </td>
                    <td>{formatDateTime(i.updated_at)}</td>
                    <td>
                      <button
                        className="button button-secondary"
                        disabled={busy !== null || !/^\d+$/.test(values[i.product_id] ?? '')}
                        onClick={() => {
                          setBusy(i.product_id)
                          void adminCommerceService
                            .updateInventory(i.product_id, Number(values[i.product_id]))
                            .then(load)
                            .catch((e) => setError(normalizeApiError(e).message))
                            .finally(() => setBusy(null))
                        }}
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
              <button disabled={page === 0} onClick={() => setPage(page - 1)}>
                Previous
              </button>
              <span>
                Page {page + 1} of {data.total_pages}
              </span>
              <button disabled={page + 1 >= data.total_pages} onClick={() => setPage(page + 1)}>
                Next
              </button>
            </nav>
          )}
        </>
      )}
    </section>
  )
}
