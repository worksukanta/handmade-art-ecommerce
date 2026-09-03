import { createBrowserRouter } from 'react-router-dom'
import { AppLayout } from '../components/layout/AppLayout'
import { RequireAuth } from '../components/routing/RequireAuth'
import { RequireRole } from '../components/routing/RequireRole'
import { AdminAreaPage } from '../pages/AdminAreaPage'
import { ProfilePage } from '../pages/ProfilePage'
import { AddressesPage } from '../pages/AddressesPage'
import { CartPage } from '../pages/CartPage'
import { CheckoutPage } from '../pages/CheckoutPage'
import { CheckoutSuccessPage } from '../pages/CheckoutSuccessPage'
import { CataloguePage } from '../pages/CataloguePage'
import { LoginPage } from '../pages/LoginPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { RegisterPage } from '../pages/RegisterPage'
import { UnauthorizedPage } from '../pages/UnauthorizedPage'
import { ProductDetailPage } from '../pages/ProductDetailPage'
import { OrderDetailPage } from '../pages/OrderDetailPage'
import { OrdersPage } from '../pages/OrdersPage'

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
              { path: 'cart', element: <CartPage /> },
              { path: 'account/profile', element: <ProfilePage /> },
              { path: 'account/addresses', element: <AddressesPage /> },
              { path: 'checkout', element: <CheckoutPage /> },
              { path: 'checkout/success/:orderId', element: <CheckoutSuccessPage /> },
              { path: 'orders', element: <OrdersPage /> },
              { path: 'orders/:id', element: <OrderDetailPage /> },
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
