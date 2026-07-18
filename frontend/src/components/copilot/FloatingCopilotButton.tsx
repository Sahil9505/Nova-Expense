import { Sparkles } from 'lucide-react';
import { useCopilotStore } from '@/store/copilotStore';

/**
 * Persistent launcher for the AI copilot. Floats above the content on every page
 * and opens the chat panel. The subtle pulse draws attention without being noisy.
 */
export function FloatingCopilotButton() {
  const open = useCopilotStore((s) => s.open);
  const isOpen = useCopilotStore((s) => s.isOpen);

  if (isOpen) return null;

  return (
    <button
      type="button"
      onClick={() => open()}
      aria-label="Ask Nova AI"
      className="group fixed bottom-5 right-5 z-40 flex h-14 w-14 items-center justify-center rounded-full bg-primary text-primary-foreground shadow-glow-primary transition-all duration-200 ease-premium hover:-translate-y-0.5 hover:shadow-glow-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70 focus-visible:ring-offset-2 focus-visible:ring-offset-background"
    >
      <span className="absolute inset-0 animate-ping rounded-full bg-primary/30 [animation-duration:2.4s]" aria-hidden="true" />
      <Sparkles className="relative h-6 w-6 transition-transform duration-200 group-hover:scale-110" aria-hidden="true" />
    </button>
  );
}
