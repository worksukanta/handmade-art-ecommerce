import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import { router } from './app/router'
import { AuthProvider } from './context/AuthProvider'
import { CartProvider } from './context/CartProvider'
import './styles/global.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <CartProvider><RouterProvider router={router} /></CartProvider>
    </AuthProvider>
  </StrictMode>,
)
