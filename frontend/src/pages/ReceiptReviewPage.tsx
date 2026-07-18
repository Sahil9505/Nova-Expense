import { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, CheckCircle2, Loader2 } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { LoadingState } from '@/components/ui/loading-state';
import { ReceiptPreview } from '@/components/finance/ReceiptPreview';
import { ReceiptDraftForm } from '@/components/finance/ReceiptDraftForm';
import { ConfidenceField, ConfidenceIndicator } from '@/components/finance/ConfidenceIndicator';
import { useReceipt, useReceiptDraft, useProcessReceipt } from '@/hooks/useReceipts';
import { formatCurrency } from '@/lib/utils';
import type { ReceiptField, ReceiptStatus } from '@/types';

const STATUS_BADGE: Record<ReceiptStatus, { variant: 'success' | 'warning' | 'danger' | 'outline' | 'primary'; label: string }> = {
  UPLOADED: { variant: 'outline', label: 'Uploaded' },
  PROCESSING: { variant: 'outline', label: 'Processing' },
  EXTRACTED: { variant: 'primary', label: 'Ready to review' },
  FAILED: { variant: 'warning', label: 'Needs entry' },
  FINALIZED: { variant: 'success', label: 'Saved' },
};

interface FieldDef {
  label: string;
  field: ReceiptField | null | undefined;
  format?: (value: string | number | null) => string;
  emphasis?: boolean;
}

/**
 * The review screen: shows the captured image and the confidence-scored extraction,
 * then lets the user correct and finalize a transaction. Nothing is saved until the
 * user confirms — extraction only pre-fills the editable draft.
 */
export function ReceiptReviewPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const receiptQuery = useReceipt(id);
  const receipt = receiptQuery.data;

  const draftQuery = useReceiptDraft(id, receipt?.status === 'EXTRACTED' || receipt?.status === 'FAILED');
  const process = useProcessReceipt();

  // Kick off extraction automatically once the raw upload is available.
  useEffect(() => {
    if (receipt && receipt.status === 'UPLOADED' && !process.isPending) {
      process.mutate(receipt.id);
    }
  }, [receipt, process]);

  if (receiptQuery.isLoading) {
    return (
      <div className="flex flex-col gap-6">
        <div className="h-9 w-48">
          <LoadingState label="Loading receipt…" />
        </div>
      </div>
    );
  }

  if (receiptQuery.isError || !receipt) {
    return (
      <div className="flex flex-col gap-4">
        <Button variant="outline" size="sm" className="w-fit" onClick={() => navigate('/receipts')}>
          <ArrowLeft className="h-4 w-4" aria-hidden="true" /> Back to receipts
        </Button>
        <div className="rounded-lg border border-border bg-surface p-6 text-sm">
          <p className="font-medium">We couldn't find that receipt.</p>
          <Button variant="outline" size="sm" className="mt-3" onClick={() => receiptQuery.refetch()}>
            Try again
          </Button>
        </div>
      </div>
    );
  }

  const badge = STATUS_BADGE[receipt.status];
  const fields = receipt.fields;

  const fieldDefs: FieldDef[] = [
    { label: 'Merchant', field: fields?.merchant },
    { label: 'Date', field: fields?.date },
    { label: 'Time', field: fields?.time },
    { label: 'Currency', field: fields?.currency },
    { label: 'Subtotal', field: fields?.subtotal, format: money },
    { label: 'Tax', field: fields?.tax, format: money },
    { label: 'Discount', field: fields?.discount, format: money },
    { label: 'Total', field: fields?.total, format: money, emphasis: true },
    { label: 'Payment', field: fields?.paymentMethod },
    { label: 'Receipt #', field: fields?.receiptNumber },
  ];

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center gap-3">
        <Button variant="outline" size="sm" onClick={() => navigate('/receipts')}>
          <ArrowLeft className="h-4 w-4" aria-hidden="true" /> Receipts
        </Button>
        <div>
          <h2 className="text-2xl font-bold tracking-tight">{receipt.filename ?? 'Receipt'}</h2>
          <p className="text-sm text-muted-foreground">
            Captured {new Date(receipt.createdAt).toLocaleString('en-US', { dateStyle: 'medium', timeStyle: 'short' })}
          </p>
        </div>
        <div className="ml-auto flex items-center gap-2">
          {receipt.overallConfidence !== null && receipt.overallConfidence !== undefined ? (
            <Badge variant="outline">{receipt.overallConfidence}% overall</Badge>
          ) : null}
          <Badge variant={badge.variant}>{badge.label}</Badge>
        </div>
      </div>

      {receipt.status === 'PROCESSING' ? (
        <Card>
          <CardContent className="flex items-center gap-3 py-10 text-muted-foreground">
            <Loader2 className="h-5 w-5 animate-spin" aria-hidden="true" /> Scanning the receipt…
          </CardContent>
        </Card>
      ) : null}

      {receipt.status === 'FINALIZED' && receipt.linkedTransactionId ? (
        <div className="flex items-start gap-3 rounded-lg border border-success/40 bg-success/5 px-4 py-3 text-sm">
          <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-success" aria-hidden="true" />
          <div className="flex flex-1 items-center justify-between gap-2">
            <p className="text-foreground">This receipt is saved as a transaction.</p>
            <Button variant="outline" size="sm" onClick={() => navigate('/transactions')}>
              View transaction
            </Button>
          </div>
        </div>
      ) : null}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Receipt image</CardTitle>
          </CardHeader>
          <CardContent>
            <ReceiptPreview receiptId={receipt.id} alt={receipt.filename ?? 'Receipt'} className="max-h-[28rem] w-full" />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Extracted details</CardTitle>
            {fields ? (
              <p className="flex items-center gap-2 text-sm text-muted-foreground">
                <ConfidenceIndicator
                  field={
                    receipt.overallConfidence !== null && receipt.overallConfidence !== undefined
                      ? { value: 'x', confidence: receipt.overallConfidence, lowConfidence: receipt.overallConfidence <= 60 }
                      : null
                  }
                />
                Review the highlighted values before saving.
              </p>
            ) : null}
          </CardHeader>
          <CardContent>
            {fields ? (
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                {fieldDefs.map((def) => (
                  <ConfidenceField key={def.label} label={def.label} field={def.field} format={def.format} emphasis={def.emphasis} />
                ))}
                {fields.items.length > 0 ? (
                  <div className="sm:col-span-2 rounded-lg border border-border px-3 py-2.5">
                    <p className="mb-1 text-xs font-medium text-muted-foreground">Items</p>
                    <ul className="flex flex-col gap-1">
                      {fields.items.map((item, index) => (
                        <li key={index} className="flex items-center justify-between text-sm">
                          <span>{item.name}</span>
                          <span className="tabular-nums text-muted-foreground">
                            {item.amount?.value !== null && item.amount?.value !== undefined
                              ? formatCurrency(Number(item.amount.value), receipt.currency ?? 'USD')
                              : '—'}
                          </span>
                        </li>
                      ))}
                    </ul>
                  </div>
                ) : null}
              </div>
            ) : (
              <p className="py-6 text-center text-sm text-muted-foreground">
                No details were extracted. Enter them manually below.
              </p>
            )}
          </CardContent>
        </Card>
      </div>

      {(receipt.status === 'EXTRACTED' || receipt.status === 'FAILED' || receipt.status === 'UPLOADED') && draftQuery.data ? (
        <Card>
          <CardHeader>
            <CardTitle>Review &amp; save</CardTitle>
            <p className="text-sm text-muted-foreground">
              Confirm or correct the details, choose where it goes, then save the transaction.
            </p>
          </CardHeader>
          <CardContent>
            <ReceiptDraftForm
              receipt={receipt}
              draft={draftQuery.data}
              onFinalized={() => navigate('/transactions')}
            />
          </CardContent>
        </Card>
      ) : receipt.status === 'EXTRACTED' || receipt.status === 'FAILED' ? (
        <Card>
          <CardContent className="py-10 text-center text-muted-foreground">
            <Loader2 className="mx-auto h-5 w-5 animate-spin" aria-hidden="true" />
            <p className="mt-2 text-sm">Loading the draft…</p>
          </CardContent>
        </Card>
      ) : null}
    </div>
  );

  function money(value: string | number | null): string {
    if (value === null || value === undefined || value === '') return '—';
    const currency = receipt?.currency ?? 'USD';
    try {
      return formatCurrency(Number(value), currency);
    } catch {
      return String(value);
    }
  }
}
