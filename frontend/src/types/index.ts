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

// ---------------------------------------------------------------------------
// Goal domain (Phase 4C)
// ---------------------------------------------------------------------------

export type GoalType = 'SAVINGS' | 'DEBT_PAYOFF' | 'CUSTOM';
export type GoalStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'ACHIEVED' | 'OVERDUE' | 'PAUSED';

/** Derived progress for a single goal, always present on a Goal response. */
export interface GoalProgress {
  targetAmount: number;
  currentAmount: number;
  remainingAmount: number;
  percentageComplete: number;
  status: GoalStatus;
  estimatedCompletionDate?: string | null;
}

export interface Goal {
  id: string;
  name: string;
  description?: string | null;
  type: GoalType;
  targetAmount: number;
  currentAmount: number;
  targetDate: string;
  active: boolean;
  paused: boolean;
  createdAt: string;
  updatedAt: string;
  progress: GoalProgress;
}

export interface GoalContribution {
  id: string;
  amount: number;
  note?: string | null;
  contributedAt: string;
}

export interface GoalDetail {
  goal: Goal;
  contributions: GoalContribution[];
}

export interface GoalWithProgress {
  goal: Goal;
  progress: GoalProgress;
}

/** Rolled-up goal health for the dashboard widgets. */
export interface GoalSummary {
  totalGoals: number;
  activeGoals: number;
  achievedGoals: number;
  pausedGoals: number;
  overdueGoals: number;
  totalTarget: number;
  totalCurrent: number;
  totalRemaining: number;
  overallPercent: number;
  currency: string;
  goals: GoalWithProgress[];
}

export interface CreateGoalPayload {
  name: string;
  type: GoalType;
  targetAmount: number;
  targetDate: string;
  currentAmount?: number;
  description?: string;
}

export interface UpdateGoalPayload {
  name?: string;
  type?: GoalType;
  targetAmount?: number;
  targetDate?: string;
  currentAmount?: number;
  description?: string;
  paused?: boolean;
  active?: boolean;
}

export interface AddGoalContributionPayload {
  amount: number;
  note?: string;
  contributedAt?: string;
}

// ---------------------------------------------------------------------------
// Analytics domain (Phase 5)
// ---------------------------------------------------------------------------

export type AnalyticsGranularity = 'WEEKLY' | 'MONTHLY' | 'YEARLY';
export type AnalyticsPeriod = 'weekly' | 'monthly' | 'yearly' | 'custom';
export type AnalyticsExportFormat = 'CSV' | 'PDF';

/** Filter applied across the Analytics domain. Any field may be omitted. */
export interface AnalyticsFilter {
  period?: AnalyticsPeriod;
  from?: string;
  to?: string;
  accountId?: string;
  categoryId?: string;
}

/** Headline income / expenses / net / savings rate for the applied window. */
export interface SpendingOverview {
  income: number;
  expenses: number;
  netCashFlow: number;
  savingsRatePct: number;
  transactionCount: number;
  currency: string;
}

/** One time bucket of the cash-flow trend. */
export interface CashFlowPoint {
  periodKey: string;
  label: string;
  income: number;
  expenses: number;
  net: number;
}

export interface CashFlowResponse {
  granularity: AnalyticsGranularity;
  currency: string;
  points: CashFlowPoint[];
}

/** One category's total within the analytics window. */
export interface CategoryBreakdownItem {
  name: string;
  color?: string | null;
  icon?: string | null;
  amount: number;
  type: 'INCOME' | 'EXPENSE';
}

export interface CategoryAnalysis {
  expenseTotal: number;
  incomeTotal: number;
  expenses: CategoryBreakdownItem[];
  incomes: CategoryBreakdownItem[];
  topCategories: CategoryBreakdownItem[];
  currency: string;
}

/** Budget health on top of the reusable BudgetSummary shape. */
export interface BudgetAnalytics {
  budgetSummary: BudgetSummary;
  healthDistribution: Record<string, number>;
  budgetEfficiencyPct: number;
  currency: string;
}

/** Goal progress on top of the reusable GoalSummary shape. */
export interface GoalAnalytics {
  goalSummary: GoalSummary;
  upcomingDeadlines: GoalWithProgress[];
  contributionTotal: number;
  currency: string;
}

/** The complete Analytics payload for the applied filter. */
export interface AnalyticsOverview {
  spendingOverview: SpendingOverview;
  cashFlow: CashFlowResponse;
  categoryAnalysis: CategoryAnalysis;
  budgetAnalytics: BudgetAnalytics;
  goalAnalytics: GoalAnalytics;
  currency: string;
  generatedAt: string;
  appliedFrom: string;
  appliedTo: string;
  accountId?: string | null;
  categoryId?: string | null;
}

/** Body for POST /api/analytics/reports/export. */
export interface AnalyticsExportRequest {
  format: AnalyticsExportFormat;
  from?: string;
  to?: string;
  accountId?: string;
  categoryId?: string;
}

// ---------------------------------------------------------------------------
// Receipt domain (Phase 6 — Smart Receipt Capture)
// ---------------------------------------------------------------------------

/** Lifecycle of a captured receipt. The backend drives these transitions. */
export type ReceiptStatus =
  | 'UPLOADED'
  | 'PROCESSING'
  | 'EXTRACTED'
  | 'FAILED'
  | 'FINALIZED';

/**
 * One extracted field: its value (string or number, or null when not found),
 * a 0–100 confidence score (null when missing), and a precomputed
 * `lowConfidence` flag the UI uses to highlight values worth a second look.
 * The pipeline never invents values, so a missing field is `null`, not a guess.
 */
export interface ReceiptField {
  value: string | number | null;
  confidence: number | null;
  lowConfidence: boolean;
}

/** A detected line item on a receipt. */
export interface ReceiptItem {
  name: string;
  amount: ReceiptField | null;
}

/** The structured, confidence-scored result of scanning a receipt. */
export interface ReceiptFields {
  merchant: ReceiptField | null;
  date: ReceiptField | null;
  time: ReceiptField | null;
  currency: ReceiptField | null;
  subtotal: ReceiptField | null;
  tax: ReceiptField | null;
  discount: ReceiptField | null;
  total: ReceiptField | null;
  paymentMethod: ReceiptField | null;
  receiptNumber: ReceiptField | null;
  items: ReceiptItem[];
  overallConfidence: number | null;
}

/** Full receipt projection, used by the detail/review screens. */
export interface Receipt {
  id: string;
  filename: string | null;
  contentType: string;
  fileSizeBytes: number;
  status: ReceiptStatus;
  statusMessage: string | null;
  ocrProvider: string | null;
  overallConfidence: number | null;
  currency: string | null;
  fields: ReceiptFields | null;
  linkedTransactionId: string | null;
  extractedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

/** Lightweight receipt projection for lists and the dashboard widget. */
export interface ReceiptSummary {
  id: string;
  filename: string | null;
  contentType: string;
  fileSizeBytes: number;
  status: ReceiptStatus;
  overallConfidence: number | null;
  currency: string | null;
  linkedTransactionId: string | null;
  extractedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

/**
 * The user's editable transaction draft, returned with the receipt so the
 * review form can be pre-filled. Account and category are intentionally absent
 * (the user must choose them); the rest default to the extracted values.
 */
export interface ReceiptDraft {
  receipt: Receipt;
  suggestion: {
    amount: number | null;
    type: TransactionType;
    accountId: string | null;
    destinationAccountId: string | null;
    categoryId: string | null;
    merchant: string | null;
    note: string | null;
    currency: string | null;
    tags: string | null;
    occurredAt: string;
  };
}

/** Payload sent when the user confirms a receipt's transaction. */
export interface FinalizeReceiptPayload {
  accountId: string;
  categoryId: string;
  type: TransactionType;
  amount: number;
  merchant?: string;
  note?: string;
  currency: string;
  occurredAt: string;
}
