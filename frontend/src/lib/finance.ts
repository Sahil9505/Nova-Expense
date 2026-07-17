import {
  Banknote,
  Briefcase,
  Car,
  CreditCard,
  Film,
  Gift,
  GraduationCap,
  HeartPulse,
  Landmark,
  PiggyBank,
  Plane,
  Receipt,
  RotateCcw,
  ShoppingBag,
  Tag,
  Target,
  TrendingDown,
  TrendingUp,
  Utensils,
  Wallet,
  type LucideIcon,
} from 'lucide-react';
import type {
  AccountType,
  BudgetPeriod,
  CategoryType,
  GoalType,
  TransactionType,
} from '@/types';

/** Human-readable labels for account types, shown in forms and tables. */
export const ACCOUNT_TYPE_LABELS: Record<AccountType, string> = {
  CASH: 'Cash',
  CHECKING: 'Checking',
  SAVINGS: 'Savings',
  CREDIT_CARD: 'Credit Card',
  WALLET: 'Wallet',
};

export const ACCOUNT_TYPE_OPTIONS = (
  Object.keys(ACCOUNT_TYPE_LABELS) as AccountType[]
).map((value) => ({ value, label: ACCOUNT_TYPE_LABELS[value] }));

/** Maps an account type to its icon. */
export const ACCOUNT_TYPE_ICONS: Record<AccountType, LucideIcon> = {
  CASH: Banknote,
  CHECKING: Landmark,
  SAVINGS: PiggyBank,
  CREDIT_CARD: CreditCard,
  WALLET: Wallet,
};

export function accountIcon(type: AccountType): LucideIcon {
  return ACCOUNT_TYPE_ICONS[type] ?? Wallet;
}

/** Maps seeded category icon names to Lucide components. */
const CATEGORY_ICONS: Record<string, LucideIcon> = {
  utensils: Utensils,
  car: Car,
  'shopping-bag': ShoppingBag,
  receipt: Receipt,
  'heart-pulse': HeartPulse,
  film: Film,
  plane: Plane,
  'graduation-cap': GraduationCap,
  tag: Tag,
  banknote: Banknote,
  briefcase: Briefcase,
  gift: Gift,
  'rotate-ccw': RotateCcw,
  'trending-up': TrendingUp,
};

export function categoryIcon(icon?: string | null): LucideIcon {
  if (icon && CATEGORY_ICONS[icon]) {
    return CATEGORY_ICONS[icon];
  }
  return Tag;
}

/** Curated icon choices offered in the category form, mapped to Lucide names. */
export const ICON_CHOICES: { value: string; label: string }[] = [
  { value: 'tag', label: 'Tag' },
  { value: 'utensils', label: 'Food' },
  { value: 'car', label: 'Transport' },
  { value: 'shopping-bag', label: 'Shopping' },
  { value: 'receipt', label: 'Bills' },
  { value: 'heart-pulse', label: 'Health' },
  { value: 'film', label: 'Entertainment' },
  { value: 'plane', label: 'Travel' },
  { value: 'graduation-cap', label: 'Education' },
  { value: 'banknote', label: 'Cash' },
  { value: 'briefcase', label: 'Work' },
  { value: 'gift', label: 'Gift' },
  { value: 'rotate-ccw', label: 'Refund' },
  { value: 'trending-up', label: 'Investment' },
];

export const TRANSACTION_TYPE_LABELS: Record<TransactionType, string> = {
  INCOME: 'Income',
  EXPENSE: 'Expense',
  TRANSFER: 'Transfer',
};

export const TRANSACTION_TYPE_OPTIONS = (
  Object.keys(TRANSACTION_TYPE_LABELS) as TransactionType[]
).map((value) => ({ value, label: TRANSACTION_TYPE_LABELS[value] }));

export const CATEGORY_TYPE_LABELS: Record<CategoryType, string> = {
  INCOME: 'Income',
  EXPENSE: 'Expense',
};

/** Human-readable labels for budget periods, shown in forms and budget cards. */
export const BUDGET_PERIOD_LABELS: Record<BudgetPeriod, string> = {
  WEEKLY: 'Weekly',
  MONTHLY: 'Monthly',
  YEARLY: 'Yearly',
  CUSTOM: 'Custom',
};

export const BUDGET_PERIOD_OPTIONS = (
  Object.keys(BUDGET_PERIOD_LABELS) as BudgetPeriod[]
).map((value) => ({ value, label: BUDGET_PERIOD_LABELS[value] }));

/** Maps a budget's health status to the badge variant matching the status palette. */
export function budgetStatusBadgeVariant(
  status: 'HEALTHY' | 'WARNING' | 'EXCEEDED',
): 'success' | 'warning' | 'danger' {
  switch (status) {
    case 'WARNING':
      return 'warning';
    case 'EXCEEDED':
      return 'danger';
    case 'HEALTHY':
    default:
      return 'success';
  }
}

/** Maps a budget's health status to the progress-fill tone. */
export function budgetStatusTone(
  status: 'HEALTHY' | 'WARNING' | 'EXCEEDED',
): 'success' | 'warning' | 'danger' {
  return budgetStatusBadgeVariant(status);
}

/** Short, human label for a budget's health status. */
export const BUDGET_STATUS_LABELS: Record<'HEALTHY' | 'WARNING' | 'EXCEEDED', string> = {
  HEALTHY: 'Healthy',
  WARNING: 'Warning',
  EXCEEDED: 'Exceeded',
};

/** A neutral fallback color for categories/accounts without one. */
export const FALLBACK_COLOR = '#94A3B8';

export function colorOf(color?: string | null): string {
  return color && color.trim().length > 0 ? color : FALLBACK_COLOR;
}

/** Formats an ISO timestamp to a short date, e.g. "Jul 15". */
export function formatTransactionDate(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

/** Today's date as YYYY-MM-DD, used to prefill transaction date inputs. */
export function todayInputValue(): string {
  const now = new Date();
  const offset = now.getTimezoneOffset() * 60000;
  return new Date(now.getTime() - offset).toISOString().slice(0, 10);
}

/** Converts a YYYY-MM-DD input into an ISO timestamp anchored at noon UTC. */
export function dateInputToIso(date: string): string {
  return `${date}T12:00:00.000Z`;
}

/** Extracts the YYYY-MM-DD portion of an ISO timestamp for a date input. */
export function isoToDateInput(iso: string): string {
  return iso.slice(0, 10);
}

// ---------------------------------------------------------------------------
// Goal helpers (Phase 4C)
// ---------------------------------------------------------------------------

/** Human-readable labels for goal types, shown in forms and goal cards. */
export const GOAL_TYPE_LABELS: Record<GoalType, string> = {
  SAVINGS: 'Savings',
  DEBT_PAYOFF: 'Debt Payoff',
  CUSTOM: 'Custom',
};

export const GOAL_TYPE_OPTIONS = (
  Object.keys(GOAL_TYPE_LABELS) as GoalType[]
).map((value) => ({ value, label: GOAL_TYPE_LABELS[value] }));

/** Maps a goal type to its icon. */
export const GOAL_TYPE_ICONS: Record<GoalType, LucideIcon> = {
  SAVINGS: PiggyBank,
  DEBT_PAYOFF: TrendingDown,
  CUSTOM: Target,
};

export function goalTypeIcon(type: GoalType): LucideIcon {
  return GOAL_TYPE_ICONS[type] ?? Target;
}

/** Short, human label for a goal's derived status. */
export const GOAL_STATUS_LABELS: Record<string, string> = {
  NOT_STARTED: 'Not started',
  IN_PROGRESS: 'In progress',
  ACHIEVED: 'Achieved',
  OVERDUE: 'Overdue',
  PAUSED: 'Paused',
};

/** Maps a goal's derived status to the badge variant matching the status palette. */
export function goalStatusBadgeVariant(
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'ACHIEVED' | 'OVERDUE' | 'PAUSED',
): 'success' | 'warning' | 'danger' | 'outline' | 'primary' {
  switch (status) {
    case 'ACHIEVED':
      return 'success';
    case 'OVERDUE':
      return 'danger';
    case 'PAUSED':
      return 'outline';
    case 'IN_PROGRESS':
      return 'primary';
    case 'NOT_STARTED':
    default:
      return 'outline';
  }
}

/** Maps a goal's derived status to the progress-fill tone. */
export function goalStatusTone(
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'ACHIEVED' | 'OVERDUE' | 'PAUSED',
): 'success' | 'warning' | 'danger' | 'primary' {
  switch (status) {
    case 'ACHIEVED':
      return 'success';
    case 'OVERDUE':
      return 'danger';
    case 'IN_PROGRESS':
      return 'primary';
    case 'PAUSED':
    case 'NOT_STARTED':
    default:
      return 'primary';
  }
}
