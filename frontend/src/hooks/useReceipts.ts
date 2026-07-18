import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { receiptsApi } from '@/lib/api';
import type {
  FinalizeReceiptPayload,
  Receipt,
  ReceiptDraft,
  ReceiptSummary,
} from '@/types';

/** Recent uploads for the dashboard widget and the receipts list. */
export function useRecentReceipts(limit = 6) {
  return useQuery<ReceiptSummary[]>({
    queryKey: ['receipts', 'recent', limit],
    queryFn: () => receiptsApi.recent(limit),
  });
}

/** A single receipt with its extracted fields, for the review screen. */
export function useReceipt(id: string | undefined) {
  return useQuery<Receipt>({
    queryKey: ['receipt', id],
    queryFn: () => receiptsApi.get(id as string),
    enabled: Boolean(id),
  });
}

/** The pre-filled, editable transaction draft for a processed receipt. */
export function useReceiptDraft(id: string | undefined, enabled = true) {
  return useQuery<ReceiptDraft>({
    queryKey: ['receipt', id, 'draft'],
    queryFn: () => receiptsApi.draft(id as string),
    enabled: Boolean(id) && enabled,
  });
}

/** Invalidates every receipt query so lists and the dashboard widget refresh. */
function useInvalidateReceipts() {
  const queryClient = useQueryClient();
  return (id?: string) => {
    queryClient.invalidateQueries({ queryKey: ['receipts'] });
    if (id) {
      queryClient.invalidateQueries({ queryKey: ['receipt', id] });
    }
  };
}

/** Uploads a receipt image; the result is stored but not yet processed. */
export function useUploadReceipt() {
  const invalidate = useInvalidateReceipts();
  return useMutation({
    mutationFn: (file: File) => receiptsApi.upload(file),
    onSuccess: () => invalidate(),
  });
}

/** Runs OCR + extraction for a receipt and refreshes its cached copy. */
export function useProcessReceipt() {
  const invalidate = useInvalidateReceipts();
  return useMutation({
    mutationFn: (id: string) => receiptsApi.process(id),
    onSuccess: (receipt) => invalidate(receipt.id),
  });
}

/**
 * Finalizes a receipt into a transaction. On success we invalidate the receipt
 * caches plus transactions, accounts, and the dashboard — exactly what creating a
 * transaction anywhere else in the app invalidates — so balances stay consistent.
 */
export function useFinalizeReceipt() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: FinalizeReceiptPayload }) =>
      receiptsApi.finalize(id, payload),
    onSuccess: (_transaction, { id }) => {
      queryClient.invalidateQueries({ queryKey: ['receipts'] });
      queryClient.invalidateQueries({ queryKey: ['receipt', id] });
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
      queryClient.invalidateQueries({ queryKey: ['accounts'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });
}
