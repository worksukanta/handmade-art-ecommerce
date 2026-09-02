import { createBrowserRouter } from 'react-router-dom'
import { AppLayout } from '../components/layout/AppLayout'
import { RequireAuth } from '../components/routing/RequireAuth'
import { RequireRole } from '../components/routing/RequireRole'
import { AdminAreaPage } from '../pages/AdminAreaPage'
import { CustomerAreaPage } from '../pages/CustomerAreaPage'
import { CataloguePage } from '../pages/CataloguePage'
import { LoginPage } from '../pages/LoginPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { RegisterPage } from '../pages/RegisterPage'
import { UnauthorizedPage } from '../pages/UnauthorizedPage'
import { ProductDetailPage } from '../pages/ProductDetailPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <CataloguePage /> },
      { path: 'products/:id', element: <ProductDetailPage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'register', element: <RegisterPage /> },
      { path: 'unauthorized', element: <UnauthorizedPage /> },
      {
        element: <RequireAuth />,
        children: [
          {
            element: <RequireRole role="CUSTOMER" />,
            children: [
              { path: 'account/profile', element: <CustomerAreaPage /> },
            ],
          },
          {
            element: <RequireRole role="ADMIN" />,
            children: [
              { path: 'admin/products', element: <AdminAreaPage /> },
            ],
          },
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
