import type { TooltipProps } from 'recharts';
import type { NameType, ValueType } from 'recharts/types/component/DefaultTooltipContent';
import { cn } from '@/lib/utils';

interface ChartTooltipProps extends TooltipProps<ValueType, NameType> {
  /** Optional formatter for each series value (e.g. currency). */
  valueFormatter?: (value: number) => string;
}

/**
 * Glass-styled Recharts tooltip. Replaces the flat default tooltip with a
 * premium, readable surface that matches Nova's glass system. Pass as the
 * `content` render-prop of a Recharts `<Tooltip>`.
 */
export function ChartTooltip({ active, payload, label, valueFormatter }: ChartTooltipProps) {
  if (!active || !payload || payload.length === 0) return null;

  return (
    <div className="min-w-[9rem] rounded-lg glass-strong px-3 py-2 text-xs shadow-glass-strong">
      {label != null && (
        <p className="mb-1.5 font-medium text-foreground">{String(label)}</p>
      )}
      <ul className="flex flex-col gap-1.5">
        {payload.map((entry, index) => {
          const raw = entry.value;
          const numeric = typeof raw === 'number' ? raw : Number(raw);
          const display =
            valueFormatter && Number.isFinite(numeric) ? valueFormatter(numeric) : String(raw);
          return (
            <li key={`${entry.name}-${index}`} className="flex items-center gap-2">
              <span
                className="h-2 w-2 shrink-0 rounded-full"
                style={{ backgroundColor: entry.color }}
                aria-hidden="true"
              />
              <span className="text-muted-foreground">{entry.name}</span>
              <span className="ml-auto font-semibold tabular-nums text-foreground">{display}</span>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

interface ChartLegendItem {
  label: string;
  color: string;
}

/** Compact inline legend used beside chart titles. */
export function ChartLegend({
  items,
  className,
}: {
  items: ChartLegendItem[];
  className?: string;
}) {
  return (
    <ul className={cn('flex items-center gap-3', className)}>
      {items.map((item) => (
        <li key={item.label} className="flex items-center gap-1.5 text-xs text-muted-foreground">
          <span
            className="h-2 w-2 rounded-full"
            style={{ backgroundColor: item.color }}
            aria-hidden="true"
          />
          {item.label}
        </li>
      ))}
    </ul>
  );
}
