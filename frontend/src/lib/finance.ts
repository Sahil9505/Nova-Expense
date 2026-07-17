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
  TrendingUp,
  Utensils,
  Wallet,
  type LucideIcon,
} from 'lucide-react';
import type { AccountType, BudgetPeriod, CategoryType, TransactionType } from '@/types';

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
