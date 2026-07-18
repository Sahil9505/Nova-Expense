import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Camera, ChevronRight, FileImage, Receipt as ReceiptIcon } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { EmptyState } from '@/components/ui/empty-state';
import { Skeleton } from '@/components/ui/loading-state';
import { ReceiptUploadDialog } from '@/components/finance/ReceiptUploadDialog';
import { useRecentReceipts } from '@/hooks/useReceipts';
import type { Receipt, ReceiptStatus, ReceiptSummary } from '@/types';

const STATUS_BADGE: Record<ReceiptStatus, { variant: 'success' | 'warning' | 'danger' | 'outline' | 'primary'; label: string }> = {
  UPLOADED: { variant: 'outline', label: 'Uploaded' },
  PROCESSING: { variant: 'outline', label: 'Processing' },
  EXTRACTED: { variant: 'primary', label: 'Ready to review' },
  FAILED: { variant: 'warning', label: 'Needs entry' },
  FINALIZED: { variant: 'success', label: 'Saved' },
};

/**
 * Lists every receipt for the user, newest first, with a lightweight upload action.
 * Each row opens the review screen where extraction is shown and finalized.
 */
export function Receipts() {
  const navigate = useNavigate();
  const [uploadOpen, setUploadOpen] = useState(false);
  const query = useRecentReceipts(8);

  const openUpload = () => setUploadOpen(true);
  const onUploaded = (receipt: Receipt) => {
    setUploadOpen(false);
    navigate(`/receipts/${receipt.id}`);
  };

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-2xl font-bold tracking-tight">Receipts</h2>
          <p className="text-sm text-muted-foreground">
            Capture a receipt and we'll extract the details for you to review.
          </p>
        </div>
        <Button onClick={openUpload}>
          <Camera className="h-4 w-4" aria-hidden="true" /> Upload receipt
        </Button>
      </div>

      {query.isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, index) => (
            <Skeleton key={index} className="h-36 w-full rounded-lg" />
          ))}
        </div>
      ) : query.isError ? (
        <div className="rounded-lg border border-border bg-surface p-6 text-sm">
          <p className="font-medium">We couldn't load your receipts.</p>
          <Button variant="outline" size="sm" className="mt-3" onClick={() => query.refetch()}>
            Try again
          </Button>
        </div>
      ) : (query.data ?? []).length === 0 ? (
        <EmptyState
          icon={ReceiptIcon}
          title="No receipts yet"
          description="Upload a receipt photo or image to automatically extract the merchant, total, and date."
          action={
            <Button onClick={openUpload}>
              <Camera className="h-4 w-4" aria-hidden="true" /> Upload your first receipt
            </Button>
          }
        />
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {(query.data ?? []).map((receipt) => (
            <ReceiptCard key={receipt.id} receipt={receipt} onOpen={() => navigate(`/receipts/${receipt.id}`)} />
          ))}
        </div>
      )}

      <ReceiptUploadDialog open={uploadOpen} onClose={() => setUploadOpen(false)} onUploaded={onUploaded} />
    </div>
  );
}

function ReceiptCard({ receipt, onOpen }: { receipt: ReceiptSummary; onOpen: () => void }) {
  const badge = STATUS_BADGE[receipt.status];

  return (
    <Card className="group cursor-pointer transition-all duration-200 ease-premium hover:border-primary/40 hover:shadow-[0_10px_30px_-12px_rgb(var(--primary)/0.4)]" onClick={onOpen}>
      <CardContent className="flex items-center gap-4 p-4">
        <span className="flex h-14 w-14 shrink-0 items-center justify-center rounded-lg bg-surface-2 text-muted-foreground">
          <FileImage className="h-6 w-6" aria-hidden="true" />
        </span>
        <div className="min-w-0 flex-1">
          <div className="flex items-center justify-between gap-2">
            <p className="truncate text-sm font-semibold">
              {receipt.filename ?? 'Receipt'}
            </p>
            <Badge variant={badge.variant}>{badge.label}</Badge>
          </div>
          <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground">
            {receipt.overallConfidence !== null && receipt.overallConfidence !== undefined ? (
              <span className="tabular-nums">{receipt.overallConfidence}% confidence</span>
            ) : (
              <span>{new Date(receipt.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}</span>
            )}
            {receipt.currency ? (
              <span className="text-muted-foreground/70">· {receipt.currency}</span>
            ) : null}
          </div>
          {receipt.linkedTransactionId ? (
            <p className="mt-1 text-xs font-medium text-success">Linked to a transaction</p>
          ) : null}
        </div>
        <ChevronRight className="h-4 w-4 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-0.5" aria-hidden="true" />
      </CardContent>
    </Card>
  );
}
