import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ArrowDownLeft,
  ArrowLeftRight,
  ArrowUpRight,
  Pencil,
  Plus,
  Receipt,
  Trash2,
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { EmptyState } from '@/components/ui/empty-state';
import { Select } from '@/components/ui/select';
import { Skeleton } from '@/components/ui/loading-state';
import { TextField } from '@/components/auth/TextField';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { useToast } from '@/components/ui/toast';
import { useAccounts } from '@/hooks/useAccounts';
import { useCategories } from '@/hooks/useCategories';
import { useDeleteTransaction, useTransactions } from '@/hooks/useTransactions';
import { formatCurrency } from '@/lib/utils';
import { colorOf, formatTransactionDate, TRANSACTION_TYPE_OPTIONS } from '@/lib/finance';
import { ApiError } from '@/lib/api';
import type { Transaction, TransactionQuery, TransactionType } from '@/types';

export function Transactions() {
  const { toast } = useToast();
  const navigate = useNavigate();
  const accounts = useAccounts();
  const categories = useCategories();
  const deleteTransaction = useDeleteTransaction();

  const [type, setType] = useState<TransactionType | ''>('');
  const [accountId, setAccountId] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [confirming, setConfirming] = useState<Transaction | null>(null);

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(search), 300);
    return () => clearTimeout(timer);
  }, [search]);

  const query: TransactionQuery = useMemo(
    () => ({
      type: type || undefined,
      accountId: accountId || undefined,
      categoryId: categoryId || undefined,
      from: from ? `${from}T00:00:00.000Z` : undefined,
      to: to ? `${to}T23:59:59.999Z` : undefined,
      search: debouncedSearch.trim() || undefined,
    }),
    [type, accountId, categoryId, from, to, debouncedSearch],
  );

  const transactions = useTransactions(query);
  const hasActiveFilters = Boolean(type || accountId || categoryId || from || to || search);

  const accountOptions = (accounts.data ?? []).map((account) => ({
    value: account.id,
    label: account.name,
  }));
  const categoryOptions = (categories.data ?? []).map((category) => ({
    value: category.id,
    label: category.name,
  }));

  const clearFilters = () => {
    setType('');
    setAccountId('');
    setCategoryId('');
    setFrom('');
    setTo('');
    setSearch('');
    setDebouncedSearch('');
  };

  const confirmDelete = async () => {
    if (!confirming) return;
    try {
      await deleteTransaction.mutateAsync(confirming.id);
      toast({ title: 'Transaction deleted', tone: 'success' });
    } catch (error) {
      toast({
        title: 'Could not delete transaction',
        description: error instanceof ApiError ? error.message : 'Please try again.',
        tone: 'danger',
      });
    } finally {
      setConfirming(null);
    }
  };

  const titleFor = (tx: Transaction) =>
    tx.merchant || tx.note || (tx.type === 'TRANSFER' ? 'Transfer' : 'Transaction');

  const subtitleFor = (tx: Transaction) =>
    tx.type === 'TRANSFER'
      ? `${tx.account?.name ?? 'Account'} → ${tx.destinationAccount?.name ?? 'Account'}`
      : (tx.category?.name ?? '');

  const amountTone = (tx: Transaction) =>
    tx.type === 'INCOME' ? 'text-success' : 'text-foreground';

  const amountPrefix = (tx: Transaction) =>
    tx.type === 'INCOME' ? '+' : tx.type === 'EXPENSE' ? '-' : '';

  const typeIcon = (tx: Transaction) => {
    if (tx.type === 'INCOME') return ArrowDownLeft;
    if (tx.type === 'TRANSFER') return ArrowLeftRight;
    return ArrowUpRight;
  };

  const iconTone = (tx: Transaction) => {
    if (tx.type === 'INCOME') return 'bg-success/15 text-success';
    if (tx.type === 'TRANSFER') return 'bg-primary/15 text-primary';
    return 'bg-surface-2 text-muted-foreground';
  };

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-2xl font-bold tracking-tight">Transactions</h2>
          <p className="text-sm text-muted-foreground">
            Every income, expense, and transfer across your accounts.
          </p>
        </div>
        <Button onClick={() => navigate('/transactions/new')}>
          <Plus className="h-4 w-4" aria-hidden="true" />
          New transaction
        </Button>
      </div>

      <Card>
        <CardContent className="flex flex-col gap-3 pt-5">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <Select
              label="Type"
              placeholder="All types"
              options={TRANSACTION_TYPE_OPTIONS}
              value={type}
              onChange={(event) => setType(event.target.value as TransactionType | '')}
            />
            <Select
              label="Account"
              placeholder="All accounts"
              options={accountOptions}
              value={accountId}
              onChange={(event) => setAccountId(event.target.value)}
              disabled={accounts.isLoading}
            />
            <Select
              label="Category"
              placeholder="All categories"
              options={categoryOptions}
              value={categoryId}
              onChange={(event) => setCategoryId(event.target.value)}
              disabled={categories.isLoading}
            />
            <div className="grid grid-cols-2 gap-3">
              <TextField
                label="From"
                type="date"
                value={from}
                onChange={(event) => setFrom(event.target.value)}
              />
              <TextField
                label="To"
                type="date"
                value={to}
                onChange={(event) => setTo(event.target.value)}
              />
            </div>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
            <div className="flex-1">
              <TextField
                label="Search"
                placeholder="Search merchant or note"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
            </div>
            <Button variant="outline" type="button" onClick={clearFilters} disabled={!hasActiveFilters}>
              Clear filters
            </Button>
          </div>
        </CardContent>
      </Card>

      {transactions.isLoading ? (
        <ul className="flex flex-col gap-3" aria-hidden="true">
          {Array.from({ length: 5 }).map((_, index) => (
            <li key={index}>
              <Skeleton className="h-[72px] w-full rounded-lg" />
            </li>
          ))}
        </ul>
      ) : transactions.isError ? (
        <div className="rounded-lg border border-border bg-surface p-6 text-sm">
          <p className="font-medium">We couldn't load your transactions.</p>
          <Button variant="outline" size="sm" className="mt-3" onClick={() => transactions.refetch()}>
            Try again
          </Button>
        </div>
      ) : transactions.data && transactions.data.length === 0 ? (
        hasActiveFilters ? (
          <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed border-border px-6 py-12 text-center">
            <p className="text-sm font-medium text-foreground">No transactions match your filters.</p>
            <Button variant="outline" size="sm" onClick={clearFilters}>
              Clear filters
            </Button>
          </div>
        ) : (
          <EmptyState
            icon={Receipt}
            title="No transactions yet"
            description="Record your first income, expense, or transfer to see it here."
            action={
              <Button onClick={() => navigate('/transactions/new')}>
                <Plus className="h-4 w-4" aria-hidden="true" />
                New transaction
              </Button>
            }
          />
        )
      ) : (
        <ul className="flex flex-col gap-3">
          {transactions.data?.map((tx) => {
            const Icon = typeIcon(tx);
            return (
              <li
                key={tx.id}
                className="flex items-center gap-3 rounded-lg border border-border bg-surface px-3 py-3"
              >
                <span
                  className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-full ${iconTone(tx)}`}
                >
                  <Icon className="h-4 w-4" aria-hidden="true" />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium">{titleFor(tx)}</p>
                  <div className="mt-0.5 flex items-center gap-2 text-xs text-muted-foreground">
                    <span className="truncate">{subtitleFor(tx)}</span>
                    <span aria-hidden="true">·</span>
                    <span className="shrink-0">{formatTransactionDate(tx.occurredAt)}</span>
                  </div>
                </div>
                {tx.type === 'TRANSFER' ? (
                  <Badge variant="outline">Transfer</Badge>
                ) : tx.category?.color ? (
                  <span className="hidden items-center gap-1.5 text-xs text-muted-foreground sm:flex">
                    <span
                      className="h-2.5 w-2.5 rounded-full"
                      style={{ backgroundColor: colorOf(tx.category.color) }}
                    />
                    {tx.category.name}
                  </span>
                ) : null}
                <span className={`shrink-0 text-sm font-semibold tabular-nums ${amountTone(tx)}`}>
                  {amountPrefix(tx)}
                  {formatCurrency(Math.abs(tx.amount), tx.currency)}
                </span>
                <div className="flex shrink-0 items-center gap-1">
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label={`Edit ${titleFor(tx)}`}
                    onClick={() => navigate(`/transactions/${tx.id}/edit`)}
                  >
                    <Pencil className="h-4 w-4" aria-hidden="true" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="text-danger hover:bg-danger/10"
                    aria-label={`Delete ${titleFor(tx)}`}
                    onClick={() => setConfirming(tx)}
                  >
                    <Trash2 className="h-4 w-4" aria-hidden="true" />
                  </Button>
                </div>
              </li>
            );
          })}
        </ul>
      )}

      <ConfirmDialog
        open={Boolean(confirming)}
        title="Delete this transaction?"
        description={
          confirming
            ? `"${titleFor(confirming)}" will be removed and its effect on account balances reversed.`
            : undefined
        }
        confirmLabel="Delete"
        loading={deleteTransaction.isPending}
        onConfirm={confirmDelete}
        onClose={() => setConfirming(null)}
      />
    </div>
  );
}
