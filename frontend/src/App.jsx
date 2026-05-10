import { BrowserRouter, Navigate, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import { ToastProvider } from './components/Toast';
import ProtectedRoute from './components/ProtectedRoute';
import BackToTop from './components/BackToTop';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import ShopPage from './pages/ShopPage';
import ProductPage from './pages/ProductPage';
import CartPage from './pages/CartPage';
import CheckoutPage from './pages/CheckoutPage';
import TryOnPage from './pages/TryOnPage';
import AccountLayout from './pages/AccountLayout';
import AccountProfilePage from './pages/AccountProfilePage';
import AccountAddressesPage from './pages/AccountAddressesPage';
import AccountOrdersPage from './pages/AccountOrdersPage';
import AccountOrderDetailPage from './pages/AccountOrderDetailPage';

export default function App() {
  return (
    <AuthProvider>
      <CartProvider>
        <ToastProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/shop" element={<ShopPage />} />
            <Route path="/product/:id" element={<ProductPage />} />
            <Route path="/cart" element={<CartPage />} />
            <Route
              path="/checkout"
              element={
                <ProtectedRoute>
                  <CheckoutPage />
                </ProtectedRoute>
              }
            />
            <Route path="/try-on" element={<TryOnPage />} />
            <Route
              path="/account"
              element={
                <ProtectedRoute>
                  <AccountLayout />
                </ProtectedRoute>
              }
            >
              <Route index element={<Navigate to="profile" replace />} />
              <Route path="profile" element={<AccountProfilePage />} />
              <Route path="addresses" element={<AccountAddressesPage />} />
              <Route path="orders" element={<AccountOrdersPage />} />
              <Route path="orders/:orderNumber" element={<AccountOrderDetailPage />} />
            </Route>
          </Routes>
          <BackToTop />
        </BrowserRouter>
        </ToastProvider>
      </CartProvider>
    </AuthProvider>
  );
}
