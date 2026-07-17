import { forwardRef, type SelectHTMLAttributes } from 'react';
import { cn } from '@/lib/utils';

export interface SelectOption {
  value: string;
  label: string;
}

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
  hint?: string;
  options: SelectOption[];
  placeholder?: string;
}

const selectClass =
  'flex h-10 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground shadow-sm ' +
  'transition-all duration-200 ease-premium hover:border-foreground/20 ' +
  'focus-visible:border-primary/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 ' +
  'focus-visible:ring-offset-0 disabled:cursor-not-allowed disabled:opacity-50';

/**
 * Labeled native select styled to match Nova's inputs, for forms that need a
 * constrained choice (account, category, type, currency).
 */
export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, error, hint, options, placeholder, id, className, ...props }, ref) => {
    const fieldId = id ?? props.name;
    return (
      <div className="flex flex-col gap-1.5">
        {label ? (
          <label htmlFor={fieldId} className="text-sm font-medium text-foreground">
            {label}
          </label>
        ) : null}
        <select
          ref={ref}
          id={fieldId}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${fieldId}-error` : undefined}
          className={cn(selectClass, error && 'border-danger focus-visible:ring-danger/70', className)}
          {...props}
        >
          {placeholder ? (
            <option value="" disabled>
              {placeholder}
            </option>
          ) : null}
          {options.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        {error ? (
          <p id={`${fieldId}-error`} className="text-xs font-medium text-danger">
            {error}
          </p>
        ) : hint ? (
          <p className="text-xs text-muted-foreground">{hint}</p>
        ) : null}
      </div>
    );
  },
);

Select.displayName = 'Select';
