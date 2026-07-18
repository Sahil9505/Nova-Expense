import { z } from 'zod';

export const COMMON_CURRENCIES = ['USD', 'EUR', 'GBP', 'JPY', 'CAD', 'AUD', 'INR', 'SGD', 'CHF'] as const;

export const loginSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});

export type LoginValues = z.infer<typeof loginSchema>;

export const registerSchema = z
  .object({
    fullName: z.string().max(255, 'Name is too long').optional().or(z.literal('')),
    email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
    password: z
      .string()
      .min(8, 'Use at least 8 characters')
      .max(128, 'Password is too long'),
    confirmPassword: z.string().min(1, 'Please confirm your password'),
    preferredCurrency: z.enum(COMMON_CURRENCIES).optional(),
  })
  .refine((values) => values.password === values.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

export type RegisterValues = z.infer<typeof registerSchema>;

export const forgotPasswordSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
});

export type ForgotPasswordValues = z.infer<typeof forgotPasswordSchema>;

export const profileSchema = z.object({
  fullName: z.string().max(255, 'Name is too long').optional().or(z.literal('')),
  preferredCurrency: z.enum(COMMON_CURRENCIES).optional().or(z.literal('')),
  timezone: z.string().max(64, 'Timezone is too long').optional().or(z.literal('')),
  avatarUrl: z
    .string()
    .max(512, 'Avatar URL is too long')
    .url('Enter a valid URL')
    .optional()
    .or(z.literal('')),
});

export type ProfileValues = z.infer<typeof profileSchema>;

export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, 'Current password is required'),
    newPassword: z
      .string()
      .min(8, 'Use at least 8 characters')
      .max(128, 'Password is too long'),
    confirmNewPassword: z.string().min(1, 'Please confirm your new password'),
  })
  .refine((values) => values.newPassword === values.confirmNewPassword, {
    message: 'New passwords do not match',
    path: ['confirmNewPassword'],
  });

export type ChangePasswordValues = z.infer<typeof changePasswordSchema>;

// ---------------------------------------------------------------------------
// Finance validation (Phase 3)
// ---------------------------------------------------------------------------

export const ACCOUNT_TYPES = [
  'CASH',
  'CHECKING',
  'SAVINGS',
  'CREDIT_CARD',
  'WALLET',
] as const;

export const accountSchema = z.object({
  name: z.string().trim().min(1, 'Account name is required').max(120, 'Name is too long'),
  type: z.enum(ACCOUNT_TYPES),
  currency: z
    .string()
    .trim()
    .transform((value) => value.toUpperCase())
    .pipe(z.string().regex(/^[A-Z]{3}$/, 'Use a 3-letter currency code')),
  balance: z.coerce.number().optional().default(0),
  institution: z.string().trim().max(120, 'Institution name is too long').optional().or(z.literal('')),
  color: z.string().trim().max(32, 'Color is too long').optional().or(z.literal('')),
  icon: z.string().trim().max(64, 'Icon is too long').optional().or(z.literal('')),
  active: z.boolean().optional(),
});

export type AccountValues = z.infer<typeof accountSchema>;

export const accountUpdateSchema = z.object({
  name: z.string().trim().min(1).max(120).optional().or(z.literal('')),
  type: z.enum(ACCOUNT_TYPES).optional(),
  currency: z
    .string()
    .trim()
    .transform((value) => value.toUpperCase())
    .pipe(z.string().regex(/^[A-Z]{3}$/, 'Use a 3-letter currency code'))
    .optional()
    .or(z.literal('')),
  balance: z.coerce.number().optional(),
  active: z.boolean().optional(),
  institution: z.string().trim().max(120).optional().or(z.literal('')),
  color: z.string().trim().max(32).optional().or(z.literal('')),
  icon: z.string().trim().max(64).optional().or(z.literal('')),
});

export type AccountUpdateValues = z.infer<typeof accountUpdateSchema>;

export const categorySchema = z.object({
  name: z.string().trim().min(1, 'Category name is required').max(120, 'Name is too long'),
  type: z.enum(['INCOME', 'EXPENSE']),
  color: z.string().trim().max(32, 'Color is too long').optional().or(z.literal('')),
  icon: z.string().trim().max(64, 'Icon is too long').optional().or(z.literal('')),
});

export type CategoryValues = z.infer<typeof categorySchema>;

export const transactionSchema = z
  .object({
    type: z.enum(['INCOME', 'EXPENSE', 'TRANSFER']),
    accountId: z.string().optional(),
    destinationAccountId: z.string().optional(),
    categoryId: z.string().optional(),
    amount: z.coerce
      .number({ invalid_type_error: 'Amount is required' })
      .positive('Amount must be greater than zero'),
    merchant: z.string().trim().max(255).optional().or(z.literal('')),
    note: z.string().trim().max(255).optional().or(z.literal('')),
    occurredAt: z.string().min(1, 'Date is required'),
  })
  .superRefine((value, ctx) => {
    if (value.type === 'TRANSFER') {
      if (!value.accountId) {
        ctx.addIssue({ code: 'custom', path: ['accountId'], message: 'Source account is required' });
      }
      if (!value.destinationAccountId) {
        ctx.addIssue({
          code: 'custom',
          path: ['destinationAccountId'],
          message: 'Destination account is required',
        });
      }
      if (value.accountId && value.destinationAccountId && value.accountId === value.destinationAccountId) {
        ctx.addIssue({
          code: 'custom',
          path: ['destinationAccountId'],
          message: 'Source and destination must be different',
        });
      }
    } else {
      if (!value.accountId) {
        ctx.addIssue({ code: 'custom', path: ['accountId'], message: 'Account is required' });
      }
      if (!value.categoryId) {
        ctx.addIssue({ code: 'custom', path: ['categoryId'], message: 'Category is required' });
      }
    }
  });

export type TransactionValues = z.infer<typeof transactionSchema>;

// ---------------------------------------------------------------------------
// Budget validation (Phase 4A)
// ---------------------------------------------------------------------------

export const BUDGET_PERIODS = ['WEEKLY', 'MONTHLY', 'YEARLY', 'CUSTOM'] as const;

const budgetBaseSchema = z.object({
  name: z.string().trim().min(1, 'Budget name is required').max(120, 'Name is too long'),
  amount: z.coerce
    .number({ invalid_type_error: 'Amount is required' })
    .positive('Amount must be greater than zero'),
  period: z.enum(BUDGET_PERIODS),
  categoryId: z.string().optional().or(z.literal('')),
  description: z.string().trim().max(255, 'Description is too long').optional().or(z.literal('')),
  startDate: z.string().min(1, 'Start date is required'),
  endDate: z.string().optional().or(z.literal('')),
});

/** Enforces period-aware date rules for both create and update payloads. */
function refineBudgetDates(
  value: { period?: string; startDate?: string; endDate?: string },
  ctx: z.RefinementCtx,
) {
  if (value.period !== 'CUSTOM') {
    if (value.startDate && value.endDate && value.endDate < value.startDate) {
      ctx.addIssue({ code: 'custom', path: ['endDate'], message: 'End date cannot be before the start date' });
    }
    return;
  }
  if (!value.startDate) {
    ctx.addIssue({ code: 'custom', path: ['startDate'], message: 'Start date is required' });
  }
  if (!value.endDate) {
    ctx.addIssue({ code: 'custom', path: ['endDate'], message: 'End date is required for custom budgets' });
  }
  if (value.startDate && value.endDate && value.endDate < value.startDate) {
    ctx.addIssue({ code: 'custom', path: ['endDate'], message: 'End date cannot be before the start date' });
  }
}

export const budgetSchema = budgetBaseSchema.superRefine(refineBudgetDates);

export type BudgetValues = z.infer<typeof budgetSchema>;

export const budgetUpdateSchema = budgetBaseSchema.partial().superRefine(refineBudgetDates);

export type BudgetUpdateValues = z.infer<typeof budgetUpdateSchema>;

// ---------------------------------------------------------------------------
// Goal validation (Phase 4C)
// ---------------------------------------------------------------------------

export const GOAL_TYPES = ['SAVINGS', 'DEBT_PAYOFF', 'CUSTOM'] as const;

export const goalSchema = z.object({
  name: z.string().trim().min(1, 'Goal name is required').max(120, 'Name is too long'),
  type: z.enum(GOAL_TYPES),
  targetAmount: z.coerce
    .number({ invalid_type_error: 'Target amount is required' })
    .positive('Target amount must be greater than zero'),
  targetDate: z.string().min(1, 'Target date is required'),
  currentAmount: z.coerce.number().min(0, 'Current amount cannot be negative').optional(),
  description: z.string().trim().max(255, 'Description is too long').optional().or(z.literal('')),
});

export type GoalValues = z.infer<typeof goalSchema>;

export const goalUpdateSchema = goalSchema.partial();

export type GoalUpdateValues = z.infer<typeof goalUpdateSchema>;

export const goalContributionSchema = z.object({
  amount: z.coerce
    .number({ invalid_type_error: 'Amount is required' })
    .positive('Amount must be greater than zero'),
  note: z.string().trim().max(255, 'Note is too long').optional().or(z.literal('')),
  contributedAt: z.string().min(1, 'Date is required'),
});

export type GoalContributionValues = z.infer<typeof goalContributionSchema>;

// ---------------------------------------------------------------------------
// Receipt capture (Phase 6)
// ---------------------------------------------------------------------------

/** Receipts become a single income or expense transaction — transfers aren't supported. */
export const RECEIPT_TYPE_OPTIONS: { value: 'INCOME' | 'EXPENSE'; label: string }[] = [
  { value: 'EXPENSE', label: 'Expense' },
  { value: 'INCOME', label: 'Income' },
];

/**
 * Validation for confirming a captured receipt. Mirrors the backend's
 * FinalizeReceiptRequest so the same rules apply: account + category required,
 * a positive amount, a 3–8 char currency, and a date. The amount/merchant/currency
 * default to the extracted draft but can be corrected by the user.
 */
export const finalizeReceiptSchema = z.object({
  type: z.enum(['INCOME', 'EXPENSE']),
  accountId: z.string().min(1, 'An account is required'),
  categoryId: z.string().min(1, 'A category is required'),
  amount: z.coerce
    .number({ invalid_type_error: 'Amount is required' })
    .positive('Amount must be greater than zero'),
  merchant: z.string().trim().max(255).optional().or(z.literal('')),
  note: z.string().trim().max(255).optional().or(z.literal('')),
  currency: z
    .string()
    .min(1, 'Currency is required')
    .refine((value) => value.length >= 3 && value.length <= 8, 'Currency must be 3–8 characters'),
  occurredAt: z.string().min(1, 'A date is required'),
});

export type FinalizeReceiptValues = z.infer<typeof finalizeReceiptSchema>;

