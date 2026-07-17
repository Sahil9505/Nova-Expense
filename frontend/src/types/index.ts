/** Health payload returned by GET /api/health. */
export interface HealthStatus {
  status: 'UP' | 'DOWN';
  service: string;
  timestamp: string;
}

export type TrendDirection = 'up' | 'down' | 'flat';

export interface StatMetric {
  id: string;
  label: string;
  value: number;
  format: 'currency' | 'percent' | 'number';
  currency?: string;
  trend: number;
  trendDirection: TrendDirection;
  caption: string;
}

export interface SpendingPoint {
  month: string;
  income: number;
  expenses: number;
}

export interface CategoryBreakdown {
  category: string;
  amount: number;
  color: string;
}

export interface TransactionItem {
  id: string;
  description: string;
  category: string;
  amount: number;
  date: string;
  type: 'income' | 'expense';
}

export type UserRole = 'USER' | 'ADMIN';
export type AccountStatus = 'ACTIVE' | 'DISABLED' | 'LOCKED' | 'PENDING';

/** The authenticated user as returned by the API. Never includes secrets. */
export interface CurrentUser {
  id: string;
  email: string;
  fullName: string | null;
  role: UserRole;
  accountStatus: AccountStatus;
  preferredCurrency: string;
  timezone: string | null;
  avatarUrl: string | null;
  lastLoginAt: string | null;
  createdAt: string;
  updatedAt: string;
}

/** Tokens issued on register, login, and refresh. */
export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: CurrentUser;
}

/** Field-level validation error returned by the API. */
export interface ApiFieldError {
  field: string;
  message: string;
}

// ---------------------------------------------------------------------------
// Finance domain (Phase 3)
// ---------------------------------------------------------------------------

export type AccountType = 'CASH' | 'CHECKING' | 'SAVINGS' | 'CREDIT_CARD' | 'WALLET';
export type CategoryType = 'INCOME' | 'EXPENSE';
export type TransactionType = 'INCOME' | 'EXPENSE' | 'TRANSFER';
export type BudgetPeriod = 'WEEKLY' | 'MONTHLY' | 'YEARLY' | 'CUSTOM';

export interface Account {
  id: string;
  name: string;
  type: AccountType;
  currency: string;
  balance: number;
  active: boolean;
  institution?: string | null;
  color?: string | null;
  icon?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Category {
  id: string;
  name: string;
  type: CategoryType;
  color?: string | null;
  icon?: string | null;
  system: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AccountRef {
  id: string;
  name: string;
  type: AccountType;
  currency: string;
}

export interface CategoryRef {
  id: string;
  name: string;
  type: CategoryType;
  color?: string | null;
  icon?: string | null;
}

export interface Transaction {
  id: string;
  amount: number;
  type: TransactionType;
  account: AccountRef | null;
  destinationAccount: AccountRef | null;
  category: CategoryRef | null;
  merchant?: string | null;
  note?: string | null;
  currency: string;
  tags?: string | null;
  occurredAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface CategoryBreakdownItem {
  name: string;
  color?: string | null;
  icon?: string | null;
  amount: number;
}

export interface MonthlyPoint {
  month: string;
  label: string;
  income: number;
  expenses: number;
}

export interface DashboardSummary {
  totalBalance: number;
  monthlyIncome: number;
  monthlyExpenses: number;
  netCashFlow: number;
  currency: string;
  accountsCount: number;
  activeAccountsCount: number;
  recentTransactions: Transaction[];
  categoryBreakdown: CategoryBreakdownItem[];
  monthlyTrend: MonthlyPoint[];
}

export interface CreateAccountPayload {
  name: string;
  type: AccountType;
  currency: string;
  balance?: number;
  institution?: string;
  color?: string;
  icon?: string;
}

export interface UpdateAccountPayload {
  name?: string;
  type?: AccountType;
  currency?: string;
  balance?: number;
  active?: boolean;
  institution?: string;
  color?: string;
  icon?: string;
}

export interface CreateCategoryPayload {
  name: string;
  type: CategoryType;
  color?: string;
  icon?: string;
}

export interface UpdateCategoryPayload {
  name?: string;
  color?: string;
  icon?: string;
}

export interface CreateTransactionPayload {
  amount: number;
  type: TransactionType;
  accountId?: string;
  destinationAccountId?: string;
  categoryId?: string;
  merchant?: string;
  note?: string;
  currency?: string;
  tags?: string;
  occurredAt: string;
}

export type UpdateTransactionPayload = Partial<CreateTransactionPayload>;

export interface TransactionQuery {
  type?: TransactionType;
  accountId?: string;
  categoryId?: string;
  from?: string;
  to?: string;
  search?: string;
}

// ---------------------------------------------------------------------------
// Budget domain (Phase 4A)
// ---------------------------------------------------------------------------

export interface Budget {
  id: string;
  name: string;
  description?: string | null;
  amount: number;
  period: BudgetPeriod;
  category: CategoryRef | null;
  active: boolean;
  startDate: string;
  endDate?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateBudgetPayload {
  name: string;
  amount: number;
  period: BudgetPeriod;
  categoryId?: string;
  description?: string;
  startDate: string;
  endDate?: string;
}

export interface UpdateBudgetPayload {
  name?: string;
  amount?: number;
  period?: BudgetPeriod;
  categoryId?: string;
  description?: string;
  startDate?: string;
  endDate?: string;
  active?: boolean;
}

// ---------------------------------------------------------------------------
// Budget Intelligence (Phase 4B)
// ---------------------------------------------------------------------------

export type BudgetStatus = 'HEALTHY' | 'WARNING' | 'EXCEEDED';

/** Live, derived figures for a single budget over its current period. */
export interface BudgetMetrics {
  amount: number;
  spent: number;
  remaining: number;
  percentageUsed: number;
  status: BudgetStatus;
}

/** A budget record paired with its live metrics. */
export interface BudgetWithMetrics {
  budget: Budget;
  metrics: BudgetMetrics;
}

/** Rolled-up budget health for the summary strip and dashboard widgets. */
export interface BudgetSummary {
  activeBudgets: number;
  totalBudgeted: number;
  totalSpent: number;
  totalRemaining: number;
  currency: string;
  warningCount: number;
  exceededCount: number;
  budgets: BudgetWithMetrics[];
}
