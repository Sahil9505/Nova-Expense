import { useEffect, useState } from 'react';
import { ImageOff, Loader2 } from 'lucide-react';
import { receiptImageUrl } from '@/lib/api';
import { cn } from '@/lib/utils';

interface ReceiptPreviewProps {
  receiptId: string;
  /** Accessible label / alt text fallback. */
  alt?: string;
  className?: string;
}

/**
 * Streams a stored receipt image and renders it. The bytes come straight from the
 * image endpoint (raw, not JSON) and are turned into an object URL that is revoked
 * when the preview unmounts. Falls back to a friendly placeholder if the image is
 * missing or cannot be loaded.
 */
export function ReceiptPreview({ receiptId, alt = 'Receipt', className }: ReceiptPreviewProps) {
  const [url, setUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    let active = true;
    let created: string | null = null;

    setLoading(true);
    setError(false);
    receiptImageUrl(receiptId)
      .then((objectUrl) => {
        if (!active) {
          window.URL.revokeObjectURL(objectUrl);
          return;
        }
        created = objectUrl;
        setUrl(objectUrl);
        setLoading(false);
      })
      .catch(() => {
        if (active) {
          setError(true);
          setLoading(false);
        }
      });

    return () => {
      active = false;
      if (created) {
        window.URL.revokeObjectURL(created);
      }
    };
  }, [receiptId]);

  if (loading) {
    return (
      <div
        className={cn(
          'flex items-center justify-center rounded-lg border border-border bg-surface-2/40 text-muted-foreground',
          className,
        )}
      >
        <Loader2 className="h-6 w-6 animate-spin" aria-hidden="true" />
      </div>
    );
  }

  if (error || !url) {
    return (
      <div
        className={cn(
          'flex flex-col items-center justify-center gap-2 rounded-lg border border-dashed border-border bg-surface-2/40 px-4 py-10 text-center text-muted-foreground',
          className,
        )}
      >
        <ImageOff className="h-6 w-6" aria-hidden="true" />
        <p className="text-sm">We couldn't load this receipt image.</p>
      </div>
    );
  }

  return (
    <img
      src={url}
      alt={alt}
      className={cn('rounded-lg border border-border bg-surface object-contain', className)}
    />
  );
}
