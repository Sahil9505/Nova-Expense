import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { ApiError, getFieldErrors } from '@/lib/api';
import { applyFieldErrors } from '@/lib/formErrors';
import { useToast } from '@/components/ui/toast';
import { Dialog } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Select } from '@/components/ui/select';
import { TextField } from '@/components/auth/TextField';
import { GOAL_TYPE_OPTIONS, todayInputValue } from '@/lib/finance';
import { goalSchema, type GoalValues } from '@/lib/validations';
import { useCreateGoal, useUpdateGoal } from '@/hooks/useGoals';
import type { Goal } from '@/types';

interface GoalFormDialogProps {
  open: boolean;
  onClose: () => void;
  goal?: Goal | null;
}

export function GoalFormDialog({ open, onClose, goal }: GoalFormDialogProps) {
  const { toast } = useToast();
  const isEdit = Boolean(goal);
  const createGoal = useCreateGoal();
  const updateGoal = useUpdateGoal();

  const form = useForm<GoalValues>({
    resolver: zodResolver(goalSchema),
    defaultValues: {
      name: goal?.name ?? '',
      type: goal?.type ?? 'SAVINGS',
      targetAmount: goal ? goal.targetAmount : 0,
      targetDate: goal ? goal.targetDate.slice(0, 10) : todayInputValue(),
      currentAmount: goal ? goal.currentAmount : undefined,
      description: goal?.description ?? '',
    },
  });

  const onSubmit = form.handleSubmit(async (values) => {
    const base = {
      name: values.name.trim(),
      type: values.type,
      targetAmount: Number(values.targetAmount ?? 0),
      targetDate: values.targetDate,
      description: values.description?.trim() || undefined,
    };
    try {
      if (isEdit && goal) {
        // Never overwrite the running total via edit; contributions own currentAmount.
        await updateGoal.mutateAsync({ id: goal.id, payload: base });
        toast({ title: 'Goal updated', tone: 'success' });
      } else {
        await createGoal.mutateAsync({
          ...base,
          currentAmount: values.currentAmount != null ? Number(values.currentAmount) : undefined,
        });
        toast({ title: 'Goal created', tone: 'success' });
      }
      onClose();
    } catch (error) {
      applyFieldErrors(form.setError, error, [
        'name',
        'type',
        'targetAmount',
        'targetDate',
        'currentAmount',
        'description',
      ]);
      if (getFieldErrors(error).length === 0) {
        toast({
          title: isEdit ? 'Could not update goal' : 'Could not create goal',
          description: error instanceof ApiError ? error.message : 'Please try again.',
          tone: 'danger',
        });
      }
    }
  });

  const pending = createGoal.isPending || updateGoal.isPending;

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={isEdit ? 'Edit goal' : 'Create goal'}
      description={
        isEdit
          ? 'Adjust the target, type, or deadline of this goal.'
          : 'Set a long-term objective and track progress as you contribute.'
      }
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={onClose} disabled={pending} type="button">
            Cancel
          </Button>
          <Button type="button" onClick={onSubmit} disabled={pending}>
            {pending ? 'Saving…' : isEdit ? 'Save changes' : 'Create goal'}
          </Button>
        </div>
      }
    >
      <form className="flex flex-col gap-4" onSubmit={onSubmit} noValidate>
        <TextField
          label="Goal name"
          placeholder="Emergency fund"
          error={form.formState.errors.name?.message}
          {...form.register('name')}
        />

        <Select
          label="Type"
          options={GOAL_TYPE_OPTIONS}
          error={form.formState.errors.type?.message}
          {...form.register('type')}
        />

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <TextField
            label="Target amount"
            type="number"
            step="0.01"
            placeholder="0.00"
            error={form.formState.errors.targetAmount?.message}
            {...form.register('targetAmount')}
          />
          <TextField
            label="Target date"
            type="date"
            error={form.formState.errors.targetDate?.message}
            {...form.register('targetDate')}
          />
        </div>

        {!isEdit ? (
          <TextField
            label="Starting amount (optional)"
            type="number"
            step="0.01"
            placeholder="0.00"
            hint="Seed with what you've already saved toward this goal."
            error={form.formState.errors.currentAmount?.message}
            {...form.register('currentAmount')}
          />
        ) : null}

        <TextField
          label="Description"
          placeholder="Optional note about this goal"
          error={form.formState.errors.description?.message}
          {...form.register('description')}
        />
      </form>
    </Dialog>
  );
}
