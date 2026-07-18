import { useCallback, useEffect, useRef, useState } from 'react';
import { AlertCircle, FileImage, Loader2, UploadCloud, X } from 'lucide-react';
import { ApiError, getFieldErrors } from '@/lib/api';
import { useToast } from '@/components/ui/toast';
import { Button } from '@/components/ui/button';
import { Dialog } from '@/components/ui/dialog';
import { cn } from '@/lib/utils';
import { useUploadReceipt } from '@/hooks/useReceipts';
import type { Receipt } from '@/types';

interface ReceiptUploadDialogProps {
  open: boolean;
  onClose: () => void;
  /** Called with the stored receipt so the parent can route to the review screen. */
  onUploaded: (receipt: Receipt) => void;
  /** Max accepted size in bytes (defaults to 10 MB, matching the backend). */
  maxBytes?: number;
}

const ACCEPTED_TYPES = ['image/png', 'image/jpeg', 'image/jpg', 'image/webp'];
const ACCEPTED_EXTENSIONS = ['png', 'jpg', 'jpeg', 'webp'];
const DEFAULT_MAX_BYTES = 10 * 1024 * 1024;

function isAccepted(file: File): boolean {
  const type = file.type.toLowerCase();
  if (ACCEPTED_TYPES.includes(type)) return true;
  const ext = file.name.split('.').pop()?.toLowerCase() ?? '';
  return ACCEPTED_EXTENSIONS.includes(ext);
}

/**
 * Reusable upload entry point for Smart Receipt Capture. Supports drag-and-drop and
 * file picking, validates the file client-side (type + 10 MB) before the request so
 * the user gets instant feedback, and hands the stored receipt back to the caller on
 * success. The actual storage and OCR happen server-side.
 */
export function ReceiptUploadDialog({
  open,
  onClose,
  onUploaded,
  maxBytes = DEFAULT_MAX_BYTES,
}: ReceiptUploadDialogProps) {
  const { toast } = useToast();
  const upload = useUploadReceipt();
  const inputRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [dragging, setDragging] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reset = useCallback(() => {
    setFile(null);
    setError(null);
    if (preview) {
      window.URL.revokeObjectURL(preview);
      setPreview(null);
    }
  }, [preview]);

  // Revoke any preview URL when the dialog unmounts or closes.
  useEffect(() => {
    if (!open && preview) {
      window.URL.revokeObjectURL(preview);
      setPreview(null);
      setFile(null);
      setError(null);
    }
  }, [open, preview]);

  const selectFile = useCallback(
    (picked: File | undefined) => {
      if (!picked) return;
      setError(null);
      if (!isAccepted(picked)) {
        setError('Only PNG, JPEG, JPG, or WEBP images are supported.');
        return;
      }
      if (picked.size > maxBytes) {
        setError(`That file is larger than the ${Math.round(maxBytes / (1024 * 1024))} MB limit.`);
        return;
      }
      setFile(picked);
      if (preview) window.URL.revokeObjectURL(preview);
      setPreview(window.URL.createObjectURL(picked));
    },
    [maxBytes, preview],
  );

  const onUpload = async () => {
    if (!file) return;
    try {
      const receipt = await upload.mutateAsync(file);
      toast({ title: 'Receipt uploaded', description: 'Review the extracted details next.', tone: 'success' });
      reset();
      onUploaded(receipt);
    } catch (err) {
      if (getFieldErrors(err).length === 0) {
        toast({
          title: 'Upload failed',
          description: err instanceof ApiError ? err.message : 'Please try again.',
          tone: 'danger',
        });
      }
    }
  };

  const handleClose = () => {
    if (upload.isPending) return;
    reset();
    onClose();
  };

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      title="Upload a receipt"
      description="Snap a photo or choose an image. We'll extract the details for you to review."
      className="sm:max-w-md"
      footer={
        <div className="flex items-center justify-end gap-2">
          <Button type="button" variant="outline" onClick={handleClose} disabled={upload.isPending}>
            Cancel
          </Button>
          <Button type="button" onClick={onUpload} disabled={!file || upload.isPending}>
            {upload.isPending ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" /> Uploading…
              </>
            ) : (
              <>
                <UploadCloud className="h-4 w-4" aria-hidden="true" /> Upload receipt
              </>
            )}
          </Button>
        </div>
      }
    >
      <div className="flex flex-col gap-4">
        {!file ? (
          <button
            type="button"
            onClick={() => inputRef.current?.click()}
            onDragOver={(event) => {
              event.preventDefault();
              setDragging(true);
            }}
            onDragLeave={() => setDragging(false)}
            onDrop={(event) => {
              event.preventDefault();
              setDragging(false);
              selectFile(event.dataTransfer.files?.[0]);
            }}
            className={cn(
              'flex flex-col items-center justify-center gap-3 rounded-xl border-2 border-dashed px-6 py-10 text-center transition-colors',
              dragging
                ? 'border-primary bg-primary/5'
                : 'border-border hover:border-primary/40 hover:bg-surface-2/40',
            )}
          >
            <span className="flex h-12 w-12 items-center justify-center rounded-full bg-surface-2 text-primary">
              <UploadCloud className="h-6 w-6" aria-hidden="true" />
            </span>
            <span className="text-sm font-medium text-foreground">
              Drag &amp; drop or click to choose
            </span>
            <span className="text-xs text-muted-foreground">
              PNG, JPEG, JPG, or WEBP · up to {Math.round(maxBytes / (1024 * 1024))} MB
            </span>
          </button>
        ) : (
          <div className="flex items-center gap-3 rounded-lg border border-border bg-surface p-3">
            {preview ? (
              <img
                src={preview}
                alt="Selected receipt preview"
                className="h-16 w-16 shrink-0 rounded-md border border-border object-cover"
              />
            ) : (
              <span className="flex h-16 w-16 shrink-0 items-center justify-center rounded-md border border-border bg-surface-2 text-muted-foreground">
                <FileImage className="h-6 w-6" aria-hidden="true" />
              </span>
            )}
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium">{file.name}</p>
              <p className="text-xs text-muted-foreground">
                {(file.size / 1024).toFixed(0)} KB · {file.type || 'image'}
              </p>
            </div>
            <button
              type="button"
              onClick={reset}
              disabled={upload.isPending}
              aria-label="Remove selected file"
              className="rounded-lg p-1.5 text-muted-foreground transition-colors hover:bg-surface-2 hover:text-foreground"
            >
              <X className="h-4 w-4" aria-hidden="true" />
            </button>
          </div>
        )}

        <input
          ref={inputRef}
          type="file"
          accept="image/png,image/jpeg,image/jpg,image/webp"
          className="hidden"
          onChange={(event) => {
            selectFile(event.target.files?.[0]);
            event.target.value = '';
          }}
        />

        {error ? (
          <p className="flex items-center gap-2 text-sm font-medium text-danger" role="alert">
            <AlertCircle className="h-4 w-4 shrink-0" aria-hidden="true" /> {error}
          </p>
        ) : null}
      </div>
    </Dialog>
  );
}
