export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  errorCode?: string;
}

export interface Merchant {
  id: string;
  businessName: string;
  email: string;
  status: string;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  merchantId: string;
  email: string;
  businessName: string;
  createdAt: string;
}

export interface Order {
  id: string;
  merchantId: string;
  orderReference: string;
  amountMinor: number;
  currency: string;
  status: 'CREATED' | 'PROCESSING' | 'COMPLETED' | 'CANCELLED' | 'FAILED';
  description?: string;
  createdAt: string;
}

export interface Payment {
  id: string;
  orderId: string;
  merchantId: string;
  amountMinor: number;
  currency: string;
  status: 'CREATED' | 'PROCESSING' | 'AUTHORIZED' | 'CAPTURED' | 'FAILED' | 'CANCELLED' | 'PARTIALLY_REFUNDED' | 'REFUNDED';
  paymentMethod: string;
  providerReference?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Refund {
  id: string;
  paymentId: string;
  merchantId: string;
  amountMinor: number;
  currency: string;
  status: 'PENDING' | 'PROCESSING' | 'SUCCEEDED' | 'FAILED';
  reason?: string;
  createdAt: string;
}

export interface LedgerLine {
  id: string;
  accountCode: string;
  accountType: string;
  direction: 'DEBIT' | 'CREDIT';
  amountMinor: number;
  currency: string;
}

export interface JournalEntry {
  id: string;
  referenceType: string;
  referenceId: string;
  description: string;
  status: string;
  createdAt: string;
  lines: LedgerLine[];
  totalDebits: number;
  totalCredits: number;
  balanced: boolean;
}

export interface DailyVolume {
  date: string;
  volumeMinor: number;
  count: number;
}

export interface DashboardAnalytics {
  totalVolumeMinor: number;
  capturedCount: number;
  failedCount: number;
  pendingCount: number;
  refundedVolumeMinor: number;
  totalRefundCount: number;
  successRate: number;
  volumeByDay: DailyVolume[];
}

