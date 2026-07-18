import { useNavigate } from 'react-router-dom';
import { Camera, ChevronRight, FileImage } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { Skeleton } from '@/components/ui/loading-state';
import { useRecentReceipts } from '@/hooks/useReceipts';
import { ReceiptUploadDialog } from '@/components/finance/ReceiptUploadDialog';
import { useState } from 'react';
import type { ReceiptStatus } from '@/types';

const STATUS_BADGE: Record<ReceiptStatus, { variant: 'success' | 'warning' | 'danger' | 'outline' | 'primary'; label: string }> = {
  UPLOADED: { variant: 'outline', label: 'Uploaded' },
  PROCESSING: { variant: 'outline', label: 'Processing' },
  EXTRACTED: { variant: 'primary', label: 'Ready' },
  FAILED: { variant: 'warning', label: 'Needs entry' },
  FINALIZED: { variant: 'success', label: 'Saved' },
};

interface RecentReceiptsWidgetProps {
  /** How many receipts to show. */
  limit?: number;
  title?: string;
}

/**
 * Lightweight dashboard panel for Smart Receipt Capture. Lists the user's most
 * recent uploads with their status and overall confidence, and offers a quick
 * upload action. Kept small so it slots into the existing dashboard grid without
 * disturbing the other sections.
 */
export function RecentReceiptsWidget({ limit = 6, title = 'Recent Receipts' }: RecentReceiptsWidgetProps) {
  const navigate = useNavigate();
  const [uploadOpen, setUploadOpen] = useState(false);
  const query = useRecentReceipts(limit);
  const receipts = query.data ?? [];

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between gap-2 space-y-0">
        <CardTitle>{title}</CardTitle>
        <div className="flex items-center gap-1">
          <Button variant="ghost" size="sm" onClick={() => setUploadOpen(true)}>
            <Camera className="h-4 w-4" aria-hidden="true" /> Upload
          </Button>
          <Button variant="ghost" size="sm" onClick={() => navigate('/receipts')}>
            View all <ChevronRight className="h-4 w-4" aria-hidden="true" />
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        {query.isLoading ? (
          <div className="flex flex-col gap-2">
            {Array.from({ length: Math.min(limit, 4) }).map((_, index) => (
              <Skeleton key={index} className="h-12 w-full rounded-lg" />
            ))}
          </div>
        ) : query.isError ? (
          <p className="py-4 text-center text-sm text-muted-foreground">Couldn't load recent receipts.</p>
        ) : receipts.length === 0 ? (
          <EmptyState
            icon={Camera}
            title="No receipts yet"
            description="Upload a receipt to automatically capture the details."
            action={
              <Button size="sm" onClick={() => setUploadOpen(true)}>
                <Camera className="h-4 w-4" aria-hidden="true" /> Upload receipt
              </Button>
            }
          />
        ) : (
          <ul className="flex flex-col">
            {receipts.map((receipt) => {
              const badge = STATUS_BADGE[receipt.status];
              return (
                <li key={receipt.id}>
                  <button
                    type="button"
                    onClick={() => navigate(`/receipts/${receipt.id}`)}
                    className="flex w-full items-center gap-3 border-b border-border py-2.5 text-left last:border-0"
                  >
                    <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-surface-2 text-muted-foreground">
                      <FileImage className="h-4 w-4" aria-hidden="true" />
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium">{receipt.filename ?? 'Receipt'}</p>
                      <div className="flex items-center gap-2 text-xs text-muted-foreground">
                        {receipt.overallConfidence !== null && receipt.overallConfidence !== undefined ? (
                          <span className="tabular-nums">{receipt.overallConfidence}% confidence</span>
                        ) : (
                          <span>
                            {new Date(receipt.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
                          </span>
                        )}
                        {receipt.currency ? <span>· {receipt.currency}</span> : null}
                      </div>
                    </div>
                    <Badge variant={badge.variant}>{badge.label}</Badge>
                  </button>
                </li>
              );
            })}
          </ul>
        )}

        <ReceiptUploadDialog
          open={uploadOpen}
          onClose={() => setUploadOpen(false)}
          onUploaded={(receipt) => {
            setUploadOpen(false);
            navigate(`/receipts/${receipt.id}`);
          }}
        />
      </CardContent>
    </Card>
  );
}
