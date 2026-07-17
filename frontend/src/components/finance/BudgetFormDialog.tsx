import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { ApiError, getFieldErrors } from '@/lib/api';
import { applyFieldErrors } from '@/lib/formErrors';
import { useToast } from '@/components/ui/toast';
import { Dialog } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Select } from '@/components/ui/select';
import { TextField } from '@/components/auth/TextField';
import { BUDGET_PERIOD_OPTIONS, dateInputToIso, isoToDateInput } from '@/lib/finance';
import { budgetSchema, type BudgetValues } from '@/lib/validations';
import { useCreateBudget, useUpdateBudget } from '@/hooks/useBudgets';
import { useCategories } from '@/hooks/useCategories';
import type { Budget, Category } from '@/types';

interface BudgetFormDialogProps {
  open: boolean;
  onClose: () => void;
  budget?: Budget | null;
}

export function BudgetFormDialog({ open, onClose, budget }: BudgetFormDialogProps) {
  const { toast } = useToast();
  const isEdit = Boolean(budget);
  const createBudget = useCreateBudget();
  const updateBudget = useUpdateBudget();
  const { data: categories } = useCategories();

  const form = useForm<BudgetValues>({
    resolver: zodResolver(budgetSchema),
    defaultValues: {
      name: budget?.name ?? '',
      amount: budget ? budget.amount : 0,
      period: budget?.period ?? 'MONTHLY',
      categoryId: budget?.category?.id ?? '',
      description: budget?.description ?? '',
      startDate: budget ? isoToDateInput(budget.startDate) : '',
      endDate: budget?.endDate ? isoToDateInput(budget.endDate) : '',
    },
  });

  const watchedPeriod = form.watch('period');

  const onSubmit = form.handleSubmit(async (values) => {
    const categoryId = values.categoryId && values.categoryId.length > 0 ? values.categoryId : undefined;
    const base = {
      name: values.name.trim(),
      amount: Number(values.amount ?? 0),
      period: values.period,
      categoryId,
      description: values.description?.trim() || undefined,
      startDate: dateInputToIso(values.startDate),
      endDate:
        values.period === 'CUSTOM' && values.endDate ? dateInputToIso(values.endDate) : undefined,
    };
    try {
      if (isEdit && budget) {
        await updateBudget.mutateAsync({ id: budget.id, payload: base });
        toast({ title: 'Budget updated', tone: 'success' });
      } else {
        await createBudget.mutateAsync(base);
        toast({ title: 'Budget created', tone: 'success' });
      }
      onClose();
    } catch (error) {
      applyFieldErrors(form.setError, error, [
        'name',
        'amount',
        'period',
        'categoryId',
        'description',
        'startDate',
        'endDate',
      ]);
      if (getFieldErrors(error).length === 0) {
        toast({
          title: isEdit ? 'Could not update budget' : 'Could not create budget',
          description: error instanceof ApiError ? error.message : 'Please try again.',
          tone: 'danger',
        });
      }
    }
  });

  const pending = createBudget.isPending || updateBudget.isPending;
  const categoryOptions = [
    { value: '', label: 'Overall budget (no category)' },
    ...((categories ?? []) as Category[]).map((category) => ({
      value: category.id,
      label: `${category.name}${category.type === 'INCOME' ? ' (Income)' : ' (Expense)'}`,
    })),
  ];

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={isEdit ? 'Edit budget' : 'Create budget'}
      description={
        isEdit
          ? 'Adjust the limit, period, or scope of this budget.'
          : 'Set a spending limit for a category or your overall spending.'
      }
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={onClose} disabled={pending} type="button">
            Cancel
          </Button>
          <Button type="button" onClick={onSubmit} disabled={pending}>
            {pending ? 'Saving…' : isEdit ? 'Save changes' : 'Create budget'}
          </Button>
        </div>
      }
    >
      <form className="flex flex-col gap-4" onSubmit={onSubmit} noValidate>
        <TextField
          label="Budget name"
          placeholder="Monthly groceries"
          error={form.formState.errors.name?.message}
          {...form.register('name')}
        />

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <TextField
            label="Amount"
            type="number"
            step="0.01"
            placeholder="0.00"
            error={form.formState.errors.amount?.message}
            {...form.register('amount')}
          />
          <Select
            label="Period"
            options={BUDGET_PERIOD_OPTIONS}
            error={form.formState.errors.period?.message}
            {...form.register('period')}
          />
        </div>

        <Select
          label="Category"
          options={categoryOptions}
          hint="Leave as an overall budget to cap total spending."
          error={form.formState.errors.categoryId?.message}
          {...form.register('categoryId')}
        />

        <TextField
          label="Description"
          placeholder="Optional note about this budget"
          error={form.formState.errors.description?.message}
          {...form.register('description')}
        />

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <TextField
            label="Start date"
            type="date"
            error={form.formState.errors.startDate?.message}
            {...form.register('startDate')}
          />
          {watchedPeriod === 'CUSTOM' ? (
            <TextField
              label="End date"
              type="date"
              error={form.formState.errors.endDate?.message}
              {...form.register('endDate')}
            />
          ) : null}
        </div>
      </form>
    </Dialog>
  );
}
