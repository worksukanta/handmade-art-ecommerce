import { type FormEvent, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { ProductCard } from '../components/catalogue/ProductCard'
import { EmptyState } from '../components/feedback/EmptyState'
import { ErrorState } from '../components/feedback/ErrorState'
import { LoadingState } from '../components/feedback/LoadingState'
import { catalogueService } from '../services/catalogueService'
import type { Category, PageResponse, ProductListParams, ProductSummary } from '../types/catalogue'
import { normalizeApiError } from '../utils/apiError'

const PAGE_SIZE = 12

function optionalNumber(value: string | null): number | undefined {
  if (!value) return undefined
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

export function CataloguePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [products, setProducts] = useState<PageResponse<ProductSummary> | null>(null)
  const [categories, setCategories] = useState<Category[]>([])
  const [categoryError, setCategoryError] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchInput, setSearchInput] = useState(searchParams.get('q') ?? '')
  const [requestVersion, setRequestVersion] = useState(0)

  const requestParams = useMemo<ProductListParams>(() => ({
    q: searchParams.get('q') || undefined,
    categoryId: optionalNumber(searchParams.get('categoryId')),
    minPrice: optionalNumber(searchParams.get('minPrice')),
    maxPrice: optionalNumber(searchParams.get('maxPrice')),
    sort: (searchParams.get('sort') as ProductListParams['sort']) || 'created_at',
    direction: (searchParams.get('direction') as ProductListParams['direction']) || 'DESC',
    page: optionalNumber(searchParams.get('page')) ?? 0,
    size: PAGE_SIZE,
  }), [searchParams])

  useEffect(() => {
    let isCurrent = true
    void catalogueService.listProducts(requestParams)
      .then((result) => { if (isCurrent) setProducts(result) })
      .catch((requestError: unknown) => { if (isCurrent) setError(normalizeApiError(requestError).message) })
      .finally(() => { if (isCurrent) setIsLoading(false) })
    return () => { isCurrent = false }
  }, [requestParams, requestVersion])
  useEffect(() => {
    void catalogueService.listCategories()
      .then(setCategories)
      .catch(() => setCategoryError(true))
  }, [])

  const updateFilters = (updates: Record<string, string | undefined>) => {
    setIsLoading(true)
    setError(null)
    const next = new URLSearchParams(searchParams)
    for (const [key, value] of Object.entries(updates)) {
      if (value) next.set(key, value)
      else next.delete(key)
    }
    if (!Object.hasOwn(updates, 'page')) next.delete('page')
    setSearchParams(next)
  }

  const submitSearch = (event: FormEvent) => {
    event.preventDefault()
    updateFilters({ q: searchInput.trim() || undefined })
  }

  const clearFilters = () => {
    setIsLoading(true)
    setError(null)
    setSearchInput('')
    setSearchParams({})
  }

  return (
    <section className="catalogue-page">
      <div className="page-heading">
        <p className="eyebrow">Original work, made by hand</p>
        <h1>Explore the catalogue</h1>
        <p>Browse ready-made pieces, customisable work, and artist portfolio highlights.</p>
      </div>

      <form className="catalogue-filters" onSubmit={submitSearch}>
        <div className="filter-search">
          <label htmlFor="catalogue-search">Search artwork</label>
          <div className="search-row">
            <input id="catalogue-search" type="search" value={searchInput} onChange={(event) => setSearchInput(event.target.value)} />
            <button className="button button-primary" type="submit">Search</button>
          </div>
        </div>
        <div className="filter-field">
          <label htmlFor="category-filter">Category</label>
          <select id="category-filter" value={searchParams.get('categoryId') ?? ''} onChange={(event) => updateFilters({ categoryId: event.target.value || undefined })}>
            <option value="">All categories</option>
            {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
          </select>
          {categoryError && <span className="filter-note" role="status">Categories are temporarily unavailable.</span>}
        </div>
        <div className="filter-field filter-price">
          <label htmlFor="minimum-price">Price range</label>
          <div className="price-row">
            <input id="minimum-price" type="number" min="0" step="0.01" placeholder="Min" aria-label="Minimum price" value={searchParams.get('minPrice') ?? ''} onChange={(event) => updateFilters({ minPrice: event.target.value || undefined })} />
            <span aria-hidden="true">to</span>
            <input type="number" min="0" step="0.01" placeholder="Max" aria-label="Maximum price" value={searchParams.get('maxPrice') ?? ''} onChange={(event) => updateFilters({ maxPrice: event.target.value || undefined })} />
          </div>
        </div>
        <div className="filter-field">
          <label htmlFor="sort-filter">Sort by</label>
          <select id="sort-filter" value={`${requestParams.sort}:${requestParams.direction}`} onChange={(event) => {
            const [sort, direction] = event.target.value.split(':')
            updateFilters({ sort, direction })
          }}>
            <option value="created_at:DESC">Newest</option>
            <option value="name:ASC">Name A–Z</option>
            <option value="price:ASC">Price low to high</option>
            <option value="price:DESC">Price high to low</option>
          </select>
        </div>
        <button className="button button-secondary filter-clear" type="button" onClick={clearFilters}>Clear filters</button>
      </form>

      {isLoading && <LoadingState label="Loading catalogue" cards />}
      {!isLoading && error && <ErrorState title="We couldn’t load the catalogue" message={error} onRetry={() => { setIsLoading(true); setError(null); setRequestVersion((version) => version + 1) }} />}
      {!isLoading && !error && products?.content.length === 0 && (
        <EmptyState title="No products match your filters" message="Try changing your search or clearing the current filters." action={<button className="button button-primary" type="button" onClick={clearFilters}>Clear filters</button>} />
      )}
      {!isLoading && !error && products && products.content.length > 0 && (
        <>
          <p className="results-count">{products.total_elements} {products.total_elements === 1 ? 'artwork' : 'artworks'}</p>
          <div className="product-grid">{products.content.map((product) => <ProductCard key={product.id} product={product} />)}</div>
          {products.total_pages > 1 && (
            <nav className="pagination" aria-label="Catalogue pages">
              <button className="button button-secondary" type="button" disabled={products.page === 0} onClick={() => updateFilters({ page: String(products.page - 1) })}>Previous</button>
              <span>Page {products.page + 1} of {products.total_pages}</span>
              <button className="button button-secondary" type="button" disabled={products.page + 1 >= products.total_pages} onClick={() => updateFilters({ page: String(products.page + 1) })}>Next</button>
            </nav>
          )}
        </>
      )}
    </section>
  )
}
