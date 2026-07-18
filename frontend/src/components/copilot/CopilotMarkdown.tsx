import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { cn } from '@/lib/utils';

/**
 * Renders the assistant's reply as safe, styled markdown. The model is instructed
 * to return markdown and every figure it cites comes from Nova's own data, so this
 * is presentation only — no HTML is injected (react-markdown escapes by default).
 */
export function CopilotMarkdown({ content, className }: { content: string; className?: string }) {
  return (
    <div
      className={cn(
        'space-y-2 text-sm leading-relaxed text-foreground',
        'prose-nova',
        className,
      )}
    >
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          a: ({ ...props }) => (
            <a {...props} target="_blank" rel="noreferrer noopener" className="text-primary underline-offset-2 hover:underline" />
          ),
          ul: ({ ...props }) => <ul {...props} className="list-disc space-y-1 pl-5" />,
          ol: ({ ...props }) => <ol {...props} className="list-decimal space-y-1 pl-5" />,
          strong: ({ ...props }) => <strong {...props} className="font-semibold text-foreground" />,
          code: ({ ...props }) => (
            <code {...props} className="rounded bg-surface-2 px-1 py-0.5 font-mono text-[0.8em]" />
          ),
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
}
