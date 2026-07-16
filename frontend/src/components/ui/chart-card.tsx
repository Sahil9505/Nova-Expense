import { type ReactNode } from 'react';
import { Card } from '@/components/ui/card';
import { cn } from '@/lib/utils';

interface ChartCardProps {
  title: string;
  description?: string;
  action?: ReactNode;
  className?: string;
  children: ReactNode;
}

/**
 * Consistent shell for dashboard charts. Provides a fixed-height plotting area so
 * Recharts' ResponsiveContainer always has measurable dimensions.
 */
export function ChartCard({ title, description, action, className, children }: ChartCardProps) {
  return (
    <Card className={cn('flex flex-col p-5', className)}>
      <div className="mb-4 flex items-start justify-between gap-3">
        <div>
          <h3 className="text-base font-semibold">{title}</h3>
          {description ? (
            <p className="mt-0.5 text-sm text-muted-foreground">{description}</p>
          ) : null}
        </div>
        {action}
      </div>
      <div className="h-64 w-full">{children}</div>
    </Card>
  );
}
