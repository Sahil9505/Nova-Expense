import { AnimatePresence, motion } from 'framer-motion';
import { Sparkles, X } from 'lucide-react';
import { useEffect } from 'react';
import { useCopilotStore } from '@/store/copilotStore';
import { ConversationUI } from './ConversationUI';

/**
 * Right-side chat drawer. Mounted once at the app root so it is reachable from any
 * page; its visibility is driven by the copilot store. The conversation state lives
 * in the store, so navigating away and back keeps the thread.
 */
export function CopilotPanel() {
  const isOpen = useCopilotStore((s) => s.isOpen);
  const close = useCopilotStore((s) => s.close);

  // Close on Escape for keyboard users.
  useEffect(() => {
    if (!isOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') close();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [isOpen, close]);

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          <motion.div
            className="fixed inset-0 z-40 bg-black/50 backdrop-blur-sm"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15 }}
            onClick={close}
            aria-hidden="true"
          />
          <motion.aside
            role="dialog"
            aria-label="Nova AI Copilot"
            className="fixed inset-y-0 right-0 z-50 flex w-full max-w-md flex-col border-l border-border bg-surface shadow-2xl"
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={{ type: 'tween', duration: 0.22, ease: 'easeOut' }}
          >
            <header className="flex items-center justify-between gap-2 border-b border-border bg-surface/80 px-4 py-3">
              <div className="flex items-center gap-2.5">
                <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/15 text-primary ring-1 ring-primary/25">
                  <Sparkles className="h-4 w-4" aria-hidden="true" />
                </div>
                <div>
                  <p className="text-sm font-semibold leading-tight">Nova AI Copilot</p>
                  <p className="text-xs text-muted-foreground">Answers from your own data</p>
                </div>
              </div>
              <button
                type="button"
                onClick={close}
                aria-label="Close copilot"
                className="rounded-lg p-1.5 text-muted-foreground transition-colors hover:bg-surface-2 hover:text-foreground"
              >
                <X className="h-5 w-5" />
              </button>
            </header>

            <div className="min-h-0 flex-1">
              <ConversationUI variant="panel" />
            </div>
          </motion.aside>
        </>
      )}
    </AnimatePresence>
  );
}
