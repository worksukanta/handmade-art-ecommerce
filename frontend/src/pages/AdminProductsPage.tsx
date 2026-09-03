import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { EmptyState } from '../components/feedback/EmptyState'
import { ErrorState } from '../components/feedback/ErrorState'
import { LoadingState } from '../components/feedback/LoadingState'
import { StatusBadge } from '../components/commerce/StatusBadge'
import { adminCommerceService } from '../services/adminCommerceService'
import type { AdminProductPage } from '../types/admin'
import { normalizeApiError } from '../utils/apiError'
import { formatCurrency, humanizeStatus } from '../utils/format'

export function AdminProductsPage() {
  const [page, setPage] = useState(0); const [data, setData] = useState<AdminProductPage | null>(null); const [error, setError] = useState<string | null>(null); const [loading, setLoading] = useState(true)
  const load = useCallback(async () => { setLoading(true); setError(null); try { setData(await adminCommerceService.listProducts(page)) } catch (e) { setError(normalizeApiError(e).message) } finally { setLoading(false) } }, [page])
  useEffect(() => { void load() }, [load])
  return <section className="commerce-page"><div className="heading-actions"><div className="page-heading"><p className="eyebrow">Admin catalogue</p><h1>Products</h1><p>Manage all active and inactive catalogue records.</p></div><Link className="button" to="/admin/products/new">Create product</Link></div><div className="admin-section-links"><Link to="/admin/categories">Categories</Link><Link to="/admin/inventory">Inventory</Link></div>{loading ? <LoadingState label="Loading products" cards /> : error ? <ErrorState title="We couldn't load products" message={error} onRetry={() => void load()} /> : !data?.content.length ? <EmptyState title="No products" message="Create the first catalogue product." /> : <><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Product</th><th>Type</th><th>Category</th><th>Price</th><th>Status</th><th><span className="sr-only">Action</span></th></tr></thead><tbody>{data.content.map(p => <tr key={p.id}><td><strong>{p.name}</strong><br /><small>#{p.id}</small></td><td>{humanizeStatus(p.product_type)}</td><td>{p.category_name}</td><td>{formatCurrency(p.price)}</td><td><StatusBadge kind="generic" value={p.status} /></td><td><Link to={`/admin/products/${p.id}`}>Manage</Link></td></tr>)}</tbody></table></div>{data.total_pages > 1 && <nav className="pagination"><button disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button><span>Page {page + 1} of {data.total_pages}</span><button disabled={page + 1 >= data.total_pages} onClick={() => setPage(page + 1)}>Next</button></nav>}</>}</section>
}
