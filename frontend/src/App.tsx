import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from '@/components/layout/AppLayout'
import { ProtectedRoute } from '@/components/layout/ProtectedRoute'
import { LandingPage } from '@/pages/landing/LandingPage'
import { LoginPage } from '@/pages/auth/LoginPage'
import { RegisterPage } from '@/pages/auth/RegisterPage'
import { CheckoutPage } from '@/pages/checkout/CheckoutPage'
import { DashboardPage } from '@/pages/dashboard/DashboardPage'
import { TransactionsPage } from '@/pages/transactions/TransactionsPage'
import { TransactionDetailPage } from '@/pages/transactions/TransactionDetailPage'
import { PaymentsPage } from '@/pages/payments/PaymentsPage'
import { RiskFraudPage } from '@/pages/fraud/RiskFraudPage'
import { CustomersPage } from '@/pages/customers/CustomersPage'
import { CustomerDetailPage } from '@/pages/customers/CustomerDetailPage'
import { MerchantsPage } from '@/pages/merchants/MerchantsPage'
import { MerchantDetailPage } from '@/pages/merchants/MerchantDetailPage'
import { SettlementsPage } from '@/pages/settlements/SettlementsPage'
import { LedgerPage } from '@/pages/ledger/LedgerPage'
import { LedgerDetailPage } from '@/pages/ledger/LedgerDetailPage'
import { ReportsPage } from '@/pages/reports/ReportsPage'
import { DevelopersPage } from '@/pages/developers/DevelopersPage'
import { ApiKeysPage } from '@/pages/developers/ApiKeysPage'
import { WebhooksPage } from '@/pages/developers/WebhooksPage'
import { NotificationsPage } from '@/pages/notifications/NotificationsPage'
import { SettingsPage } from '@/pages/settings/SettingsPage'
import { AuditLogsPage } from '@/pages/audit/AuditLogsPage'
import { useAuth } from '@/hooks/useAuth'

function PublicOnly({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return null
  if (user) return <Navigate to="/dashboard" replace />
  return <>{children}</>
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route
        path="/login"
        element={
          <PublicOnly>
            <LoginPage />
          </PublicOnly>
        }
      />
      <Route
        path="/register"
        element={
          <PublicOnly>
            <RegisterPage />
          </PublicOnly>
        }
      />
      <Route path="/checkout/:paymentId" element={<CheckoutPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/transactions" element={<TransactionsPage />} />
          <Route path="/transactions/:id" element={<TransactionDetailPage />} />
          <Route path="/payments" element={<PaymentsPage />} />
          <Route path="/risk-fraud" element={<RiskFraudPage />} />
          <Route path="/customers" element={<CustomersPage />} />
          <Route path="/customers/:id" element={<CustomerDetailPage />} />
          <Route path="/merchants" element={<MerchantsPage />} />
          <Route path="/merchants/:id" element={<MerchantDetailPage />} />
          <Route path="/settlements" element={<SettlementsPage />} />
          <Route path="/ledger" element={<LedgerPage />} />
          <Route path="/ledger/:id" element={<LedgerDetailPage />} />
          <Route path="/reports" element={<ReportsPage />} />
          <Route path="/developers" element={<DevelopersPage />} />
          <Route path="/developers/api-keys" element={<ApiKeysPage />} />
          <Route path="/developers/webhooks" element={<WebhooksPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/settings" element={<SettingsPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute roles={['ADMIN']} />}>
        <Route element={<AppLayout />}>
          <Route path="/audit-logs" element={<AuditLogsPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
