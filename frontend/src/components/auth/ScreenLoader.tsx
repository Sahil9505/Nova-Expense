import { Loader2 } from 'lucide-react';

/** Branded full-screen loader shown while the auth session is being restored. */
export function ScreenLoader() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4">
      <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-primary to-accent text-primary-foreground">
        <span className="text-xl font-extrabold">N</span>
      </div>
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
        Restoring your session…
      </div>
    </div>
  );
}
