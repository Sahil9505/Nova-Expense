import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { ApiError, getFieldErrors } from '@/lib/api';
import { applyFieldErrors } from '@/lib/formErrors';
import { useToast } from '@/components/ui/toast';
import { Dialog } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { TextField } from '@/components/auth/TextField';
import { todayInputValue } from '@/lib/finance';
import { goalContributionSchema, type GoalContributionValues } from '@/lib/validations';
import { useAddGoalContribution } from '@/hooks/useGoals';
import { formatCurrency } from '@/lib/utils';
import type { Goal } from '@/types';

interface ContributionDialogProps {
  open: boolean;
  onClose: () => void;
  goal: Goal | null;
}

export function ContributionDialog({ open, onClose, goal }: ContributionDialogProps) {
  const { toast } = useToast();
  const addContribution = useAddGoalContribution();

  const form = useForm<GoalContributionValues>({
    resolver: zodResolver(goalContributionSchema),
    defaultValues: {
      amount: undefined,
      note: '',
      contributedAt: todayInputValue(),
    },
  });

  const onSubmit = form.handleSubmit(async (values) => {
    if (!goal) return;
    try {
      await addContribution.mutateAsync({
        id: goal.id,
        payload: {
          amount: Number(values.amount),
          note: values.note?.trim() || undefined,
          contributedAt: values.contributedAt,
        },
      });
      toast({
        title: 'Contribution added',
        description: `${formatCurrency(Number(values.amount), 'USD')} logged to ${goal.name}.`,
        tone: 'success',
      });
      onClose();
    } catch (error) {
      applyFieldErrors(form.setError, error, ['amount', 'note', 'contributedAt']);
      if (getFieldErrors(error).length === 0) {
        toast({
          title: 'Could not add contribution',
          description: error instanceof ApiError ? error.message : 'Please try again.',
          tone: 'danger',
        });
      }
    }
  });

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={`Add to ${goal?.name ?? 'goal'}`}
      description="Log a contribution toward this goal. Each one is saved to its history."
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={onClose} disabled={addContribution.isPending} type="button">
            Cancel
          </Button>
          <Button type="button" onClick={onSubmit} disabled={addContribution.isPending}>
            {addContribution.isPending ? 'Saving…' : 'Add contribution'}
          </Button>
        </div>
      }
    >
      <form className="flex flex-col gap-4" onSubmit={onSubmit} noValidate>
        <TextField
          label="Amount"
          type="number"
          step="0.01"
          placeholder="0.00"
          error={form.formState.errors.amount?.message}
          {...form.register('amount')}
        />
        <TextField
          label="Date"
          type="date"
          error={form.formState.errors.contributedAt?.message}
          {...form.register('contributedAt')}
        />
        <TextField
          label="Note (optional)"
          placeholder="e.g. Monthly transfer"
          error={form.formState.errors.note?.message}
          {...form.register('note')}
        />
      </form>
    </Dialog>
  );
}
