import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { AlertTriangle, Loader2 } from 'lucide-react';
import { ApiError, getFieldErrors } from '@/lib/api';
import { applyFieldErrors } from '@/lib/formErrors';
import { useToast } from '@/components/ui/toast';
import { Button } from '@/components/ui/button';
import { Select } from '@/components/ui/select';
import { TextField } from '@/components/auth/TextField';
import { dateInputToIso, isoToDateInput, todayInputValue } from '@/lib/finance';
import { finalizeReceiptSchema, RECEIPT_TYPE_OPTIONS, type FinalizeReceiptValues } from '@/lib/validations';
import { useAccounts } from '@/hooks/useAccounts';
import { useCategories } from '@/hooks/useCategories';
import { useFinalizeReceipt } from '@/hooks/useReceipts';
import type { Receipt, ReceiptDraft } from '@/types';

interface ReceiptDraftFormProps {
  receipt: Receipt;
  draft: ReceiptDraft;
  onFinalized: (transactionId: string) => void;
}

/**
 * The user-facing half of the receipt pipeline: an editable transaction draft built
 * from the extracted values. The user stays in control — they can correct any field,
 * pick the account and category, and nothing is saved until they confirm. The
 * detected values are shown alongside the inputs as confidence-scored read-outs, so
 * the human can see what the machine found and overrule it. When extraction failed,
 * the form still works as a manual entry with empty defaults.
 */
export function ReceiptDraftForm({ receipt, draft, onFinalized }: ReceiptDraftFormProps) {
  const { toast } = useToast();
  const accounts = useAccounts();
  const categories = useCategories();
  const finalize = useFinalizeReceipt();

  const suggested = draft.suggestion;
  const failed = receipt.status === 'FAILED';

  const form = useForm<FinalizeReceiptValues>({
    resolver: zodResolver(finalizeReceiptSchema),
    defaultValues: {
      type: 'EXPENSE',
      accountId: '',
      categoryId: '',
      amount: suggested.amount ?? 0,
      merchant: suggested.merchant ?? '',
      note: '',
      currency: receipt.currency ?? suggested.currency ?? '',
      occurredAt: suggested.occurredAt ? isoToDateInput(suggested.occurredAt) : todayInputValue(),
    },
  });

  const type = form.watch('type');

  const accountOptions = (accounts.data ?? []).map((account) => ({
    value: account.id,
    label: account.name,
  }));

  const categoryOptions = (categories.data ?? [])
    .filter((category) => category.type === type)
    .map((category) => ({ value: category.id, label: category.name }));

  const onSubmit = form.handleSubmit(async (values) => {
    try {
      const result = await finalize.mutateAsync({
        id: receipt.id,
        payload: {
          accountId: values.accountId,
          categoryId: values.categoryId,
          type: values.type,
          amount: values.amount,
          merchant: values.merchant?.trim() || undefined,
          note: values.note?.trim() || undefined,
          currency: values.currency.trim().toUpperCase(),
          occurredAt: dateInputToIso(values.occurredAt),
        },
      });
      toast({ title: 'Transaction saved', description: 'The receipt is linked to your transaction.', tone: 'success' });
      onFinalized(result.id);
    } catch (error) {
      applyFieldErrors(form.setError, error, [
        'type',
        'accountId',
        'categoryId',
        'amount',
        'merchant',
        'note',
        'currency',
        'occurredAt',
      ]);
      if (getFieldErrors(error).length === 0) {
        toast({
          title: "Couldn't save the transaction",
          description: error instanceof ApiError ? error.message : 'Please try again.',
          tone: 'danger',
        });
      }
    }
  });

  const pending = finalize.isPending;

  return (
    <form className="flex flex-col gap-5" onSubmit={onSubmit} noValidate>
      {failed ? (
        <div className="flex items-start gap-3 rounded-lg border border-warning/40 bg-warning/5 px-4 py-3 text-sm">
          <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-warning" aria-hidden="true" />
          <p className="text-foreground">
            {receipt.statusMessage
              ? `${receipt.statusMessage} `
              : 'We could not read this receipt automatically. '}
            Enter the details below and save it as a normal transaction.
          </p>
        </div>
      ) : null}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <Select
          label="Type"
          options={RECEIPT_TYPE_OPTIONS}
          error={form.formState.errors.type?.message}
          {...form.register('type', {
            onChange: () => {
              form.setValue('categoryId', '');
            },
          })}
        />
        <Select
          label="Account"
          placeholder="Select account"
          options={accountOptions}
          error={form.formState.errors.accountId?.message}
          disabled={accounts.isLoading}
          {...form.register('accountId')}
        />
      </div>

      <Select
        label="Category"
        placeholder="Select category"
        options={categoryOptions}
        error={form.formState.errors.categoryId?.message}
        disabled={categories.isLoading}
        {...form.register('categoryId')}
      />

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <TextField
          label="Amount"
          type="number"
          step="0.01"
          min="0"
          inputMode="decimal"
          placeholder="0.00"
          error={form.formState.errors.amount?.message}
          {...form.register('amount')}
        />
        <TextField
          label="Currency"
          placeholder="USD"
          maxLength={8}
          error={form.formState.errors.currency?.message}
          {...form.register('currency')}
        />
      </div>

      <TextField
        label="Merchant"
        placeholder="e.g. Whole Foods"
        error={form.formState.errors.merchant?.message}
        {...form.register('merchant')}
      />

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <TextField
          label="Date"
          type="date"
          error={form.formState.errors.occurredAt?.message}
          {...form.register('occurredAt')}
        />
        <TextField
          label="Note"
          placeholder="Optional details"
          error={form.formState.errors.note?.message}
          {...form.register('note')}
        />
      </div>

      <div className="flex items-center justify-end gap-2">
        <Button type="submit" disabled={pending}>
          {pending ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" /> Saving…
            </>
          ) : (
            'Save transaction'
          )}
        </Button>
      </div>
    </form>
  );
}
