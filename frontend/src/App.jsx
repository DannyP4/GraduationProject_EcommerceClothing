import { BrowserRouter, Navigate, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import { ToastProvider } from './components/Toast';
import ProtectedRoute from './components/ProtectedRoute';
import AdminRoute from './components/AdminRoute';
import BackToTop from './components/BackToTop';
import ChatWidget from './components/ChatWidget';
import ScrollToTop from './components/ScrollToTop';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import AccountStatusPage from './pages/AccountStatusPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import VerifyEmailPage from './pages/VerifyEmailPage';
import AcceptInvitePage from './pages/AcceptInvitePage';
import OAuthCallbackPage from './pages/OAuthCallbackPage';
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
import VnpayReturnPage from './pages/VnpayReturnPage';
import StripeSuccessPage from './pages/StripeSuccessPage';
import StripeCancelPage from './pages/StripeCancelPage';
import AdminLayout from './pages/admin/AdminLayout';
import AdminDashboardPage from './pages/admin/AdminDashboardPage';
import AdminOrdersListPage from './pages/admin/AdminOrdersListPage';
import AdminOrderDetailPage from './pages/admin/AdminOrderDetailPage';
import AdminUsersPage from './pages/admin/AdminUsersPage';
import AdminUserDetailPage from './pages/admin/AdminUserDetailPage';
import AdminReviewsPage from './pages/admin/AdminReviewsPage';
import AdminStatsPage from './pages/admin/AdminStatsPage';
import AdminCategoriesPage from './pages/admin/AdminCategoriesPage';
import AdminBrandsPage from './pages/admin/AdminBrandsPage';
import AdminProductsPage from './pages/admin/AdminProductsPage';
import AdminProductEditPage from './pages/admin/AdminProductEditPage';
import AdminCouponsPage from './pages/admin/AdminCouponsPage';
import AdminForbiddenPage from './pages/admin/AdminForbiddenPage';

export default function App() {
  return (
    <AuthProvider>
      <CartProvider>
        <ToastProvider>
        <BrowserRouter>
          <ScrollToTop />
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/auth/account-status" element={<AccountStatusPage />} />
            <Route path="/auth/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/auth/reset-password" element={<ResetPasswordPage />} />
            <Route path="/auth/verify-email" element={<VerifyEmailPage />} />
            <Route path="/auth/accept-invite" element={<AcceptInvitePage />} />
            <Route path="/auth/oauth/callback" element={<OAuthCallbackPage />} />
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
            <Route
              path="/try-on"
              element={
                <ProtectedRoute>
                  <TryOnPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/try-on/:productId"
              element={
                <ProtectedRoute>
                  <TryOnPage />
                </ProtectedRoute>
              }
            />
            <Route path="/payment/vnpay/return" element={<VnpayReturnPage />} />
            <Route
              path="/payment/stripe/success"
              element={
                <ProtectedRoute>
                  <StripeSuccessPage />
                </ProtectedRoute>
              }
            />
            <Route path="/payment/stripe/cancel" element={<StripeCancelPage />} />
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

            <Route path="/admin/forbidden" element={<AdminForbiddenPage />} />
            <Route
              path="/admin"
              element={
                <AdminRoute>
                  <AdminLayout />
                </AdminRoute>
              }
            >
              <Route index element={<AdminDashboardPage />} />
              <Route path="orders" element={<AdminOrdersListPage />} />
              <Route path="orders/:orderNumber" element={<AdminOrderDetailPage />} />
              <Route path="users" element={<AdminUsersPage />} />
              <Route path="users/:id" element={<AdminUserDetailPage />} />
              <Route path="reviews" element={<AdminReviewsPage />} />
              <Route path="stats" element={<AdminStatsPage />} />
              <Route path="categories" element={<AdminCategoriesPage />} />
              <Route path="brands" element={<AdminBrandsPage />} />
              <Route path="products" element={<AdminProductsPage />} />
              <Route path="products/new" element={<AdminProductEditPage />} />
              <Route path="products/:id" element={<AdminProductEditPage />} />
              <Route path="coupons" element={<AdminCouponsPage />} />
            </Route>
          </Routes>
          <BackToTop />
          <ChatWidget />
        </BrowserRouter>
        </ToastProvider>
      </CartProvider>
    </AuthProvider>
  );
}
