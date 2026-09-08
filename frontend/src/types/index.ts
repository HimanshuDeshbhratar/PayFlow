export type UserRole = 'ADMIN' | 'MERCHANT_ADMIN' | 'MERCHANT_USER' | 'RISK_ANALYST'

export type PaymentStatus =
  | 'PENDING'
  | 'FRAUD_SCREENING'
  | 'APPROVED'
  | 'BLOCKED'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED'
  | 'REFUND_PENDING'
  | 'REFUNDED'
  | 'PARTIALLY_REFUNDED'

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type PaymentMethodType = 'CARD' | 'UPI' | 'NET_BANKING' | 'WALLET'
export type FraudAlertStatus = 'OPEN' | 'INVESTIGATING' | 'RESOLVED' | 'DISMISSED'
export type MerchantStatus = 'ACTIVE' | 'SUSPENDED' | 'PENDING' | 'CLOSED'
export type SettlementStatus = 'CURRENT' | 'PENDING' | 'OVERDUE' | 'HOLD' | 'COMPLETED' | 'FAILED'

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface User {
  id: string
  email: string
  fullName: string
  role: UserRole
  merchantId?: string | null
  active: boolean
  lastLoginAt?: string | null
  createdAt: string
}

export interface AuthTokens {
  accessToken: string
  refreshToken: string
  tokenType?: string
  expiresIn?: number
}

export interface AuthResponse extends AuthTokens {
  user: User
}

export interface DashboardKpis {
  totalVolumeCents: number
  successfulPayments: number
  blockedFraud: number
  activeMerchants: number
  volumeChangePercent?: number
  successChangePercent?: number
  blockedChangePercent?: number
  merchantsChangePercent?: number
}

export interface ChartPoint {
  label: string
  value: number
}

export interface PaymentStatusBreakdown {
  status: PaymentStatus | string
  count: number
  percentage?: number
}

export interface DashboardResponse {
  kpis: DashboardKpis
  volumeSeries: ChartPoint[]
  statusBreakdown: PaymentStatusBreakdown[]
  recentTransactions: Transaction[]
  fraudAlerts: FraudAlert[]
}

export interface Transaction {
  id: string
  paymentId: string
  merchantId: string
  customerId?: string | null
  reference: string
  amountCents: number
  currency: string
  status: PaymentStatus
  paymentMethodType?: PaymentMethodType | string | null
  locationCity?: string | null
  locationCountry?: string | null
  deviceInfo?: string | null
  ipAddress?: string | null
  isNewDevice?: boolean
  riskScore?: number | null
  riskLevel?: RiskLevel | null
  customerName?: string | null
  customerEmail?: string | null
  merchantName?: string | null
  createdAt: string
  updatedAt?: string
}

export interface RiskFactor {
  code: string
  name: string
  weight: number
  description?: string
}

export interface TransactionEvent {
  id: string
  eventType: string
  fromStatus?: string | null
  toStatus?: string | null
  message?: string | null
  createdAt: string
}

export interface RiskAssessment {
  id: string
  riskScore: number
  riskLevel: RiskLevel
  decision: 'APPROVE' | 'REVIEW' | 'BLOCK' | string
  factors: RiskFactor[]
  assessedAt: string
}

export interface TransactionDetail extends Transaction {
  events?: TransactionEvent[]
  timeline?: TransactionEvent[]
  riskAssessment?: RiskAssessment | null
  customer?: {
    id: string
    email: string
    fullName: string
    blocked?: boolean
    status?: string
  } | null
  customerActivity?: CustomerActivity[]
  newDevice?: boolean
}

export interface CustomerActivity {
  id: string
  label?: string
  reference?: string
  status?: string
  amountCents?: number
  createdAt: string
}

export interface FraudAlert {
  id: string
  transactionId: string
  merchantId: string
  severity: RiskLevel
  title: string
  description?: string | null
  status: FraudAlertStatus
  assignedTo?: string | null
  resolvedAt?: string | null
  createdAt: string
  transactionReference?: string
  amountCents?: number
}

export interface FraudRule {
  id: string
  code: string
  name: string
  description?: string | null
  weight: number
  thresholdValue?: number | null
  enabled: boolean
  createdAt: string
  updatedAt?: string
}

export interface FraudDashboard {
  fraudRatePercent: number
  blockedCount: number
  highRiskCount: number
  preventedLossCents: number
  alerts: FraudAlert[]
  rules: FraudRule[]
  trendSeries?: ChartPoint[]
}

export interface Payment {
  id: string
  merchantId: string
  customerId?: string | null
  reference: string
  amountCents: number
  currency: string
  status: PaymentStatus
  description?: string | null
  checkoutSessionId?: string | null
  customerName?: string | null
  customerEmail?: string | null
  paymentMethodType?: PaymentMethodType | string | null
  createdAt: string
  updatedAt?: string
}

export interface CheckoutSession {
  id: string
  paymentId: string
  reference: string
  amountCents: number
  currency: string
  status: PaymentStatus
  merchantName: string
  description?: string | null
  customerEmail?: string | null
}

export interface Customer {
  id: string
  merchantId: string
  email: string
  fullName: string
  phone?: string | null
  countryCode?: string | null
  status: string
  blocked: boolean
  blockedReason?: string | null
  totalSpendCents?: number
  transactionCount?: number
  createdAt: string
}

export interface Merchant {
  id: string
  businessName: string
  legalName?: string | null
  status: MerchantStatus
  countryCode: string
  currency: string
  settlementStatus: SettlementStatus | string
  volumeCents?: number
  transactionCount?: number
  createdAt: string
}

export interface Settlement {
  id: string
  merchantId: string
  merchantName?: string
  amountCents: number
  currency: string
  status: SettlementStatus | string
  periodStart: string
  periodEnd: string
  settledAt?: string | null
  createdAt: string
}

export interface LedgerAccount {
  id: string
  merchantId?: string | null
  customerId?: string | null
  accountType: string
  currency: string
  name: string
  balanceCents: number
  createdAt: string
}

export interface LedgerEntry {
  id: string
  accountId: string
  paymentId?: string | null
  transactionId?: string | null
  entryType: 'DEBIT' | 'CREDIT'
  amountCents: number
  currency: string
  description?: string | null
  createdAt: string
}

export interface NotificationItem {
  id: string
  title: string
  message: string
  type?: string
  read: boolean
  createdAt: string
  link?: string | null
}

export interface ApiKey {
  id: string
  name: string
  prefix: string
  key?: string
  lastUsedAt?: string | null
  createdAt: string
  revoked?: boolean
}

export interface WebhookEndpoint {
  id: string
  url: string
  events: string[]
  active: boolean
  secret?: string
  createdAt: string
}

export interface AuditLog {
  id: string
  actorEmail?: string
  actorId?: string
  action: string
  resourceType?: string
  resourceId?: string
  ipAddress?: string
  metadata?: Record<string, unknown>
  createdAt: string
}

export interface ReportSummary {
  id: string
  name: string
  description?: string
  type: string
  generatedAt?: string
}

export interface CreatePaymentRequest {
  amountCents: number
  currency: string
  customerEmail?: string
  customerName?: string
  description?: string
  paymentMethodType?: PaymentMethodType
}

export interface ProcessCheckoutRequest {
  paymentMethodType: PaymentMethodType
  cardNumber?: string
  cardExpiry?: string
  cardCvv?: string
  cardHolder?: string
  upiId?: string
  bankCode?: string
  walletProvider?: string
  saveMethod?: boolean
}
