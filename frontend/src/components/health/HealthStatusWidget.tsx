import { Loader2, Wifi, WifiOff } from 'lucide-react';
import { useHealth } from '@/hooks/useHealth';
import { cn } from '@/lib/utils';

/**
 * Compact API connection indicator. Degrades gracefully to "Disconnected" when
 * the backend is unreachable, which is expected before Phase 2 wiring.
 */
export function HealthStatusWidget() {
  const { data, isLoading, isError } = useHealth();
  const connected = !isError && !!data && data.status === 'UP';

  const label = isLoading ? 'Checking…' : connected ? 'Connected' : 'Disconnected';
  const tone = isLoading ? 'muted' : connected ? 'success' : 'danger';

  return (
    <div
      role="status"
      aria-live="polite"
      title="Backend connection status"
      className={cn(
        'flex items-center gap-2 rounded-full border px-3 py-1.5 text-xs font-medium',
        tone === 'success' && 'border-success/30 bg-success/10 text-success',
        tone === 'danger' && 'border-danger/30 bg-danger/10 text-danger',
        tone === 'muted' && 'border-border bg-surface-2 text-muted-foreground',
      )}
    >
      {isLoading ? (
        <Loader2 className="h-3.5 w-3.5 animate-spin" aria-hidden="true" />
      ) : connected ? (
        <Wifi className="h-3.5 w-3.5" aria-hidden="true" />
      ) : (
        <WifiOff className="h-3.5 w-3.5" aria-hidden="true" />
      )}
      <span>{label}</span>
    </div>
  );
}
