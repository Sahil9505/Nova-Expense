import { ArrowLeft, Sparkles } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { ConversationUI } from '@/components/copilot/ConversationUI';
import { useCopilotStore } from '@/store/copilotStore';

/**
 * Full-screen copilot route (/copilot). Shares the chat surface with the floating
 * panel; reaching it from the sidebar opens the same conversation stored in the
 * copilot store. A back affordance returns to the dashboard.
 */
export function Copilot() {
  const navigate = useNavigate();
  const close = useCopilotStore((s) => s.close);

  return (
    <div className="mx-auto flex h-[calc(100vh-4rem)] max-w-3xl flex-col">
      <div className="mb-3 flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/15 text-primary ring-1 ring-primary/25">
            <Sparkles className="h-4 w-4" aria-hidden="true" />
          </div>
          <div>
            <h1 className="text-lg font-semibold leading-tight">Nova AI Copilot</h1>
            <p className="text-xs text-muted-foreground">Answers from your own financial data</p>
          </div>
        </div>
        <Button variant="ghost" size="sm" onClick={() => { close(); navigate('/'); }}>
          <ArrowLeft className="h-4 w-4" /> Dashboard
        </Button>
      </div>

      <div className="min-h-0 flex-1 overflow-hidden rounded-2xl border border-border bg-surface/60">
        <ConversationUI variant="page" />
      </div>
    </div>
  );
}
