import { forwardRef, type ButtonHTMLAttributes } from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-lg text-sm font-semibold transition-all duration-200 ease-premium focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70 focus-visible:ring-offset-2 focus-visible:ring-offset-background disabled:pointer-events-none disabled:opacity-50 active:scale-[0.98] motion-reduce:transform-none motion-reduce:transition-none',
  {
    variants: {
      variant: {
        primary:
          'bg-primary text-primary-foreground shadow-[0_1px_2px_rgb(0_0_0/0.25),0_8px_20px_-8px_rgb(var(--primary)/0.55)] hover:bg-primary/90 hover:-translate-y-px hover:shadow-[0_10px_24px_-8px_rgb(var(--primary)/0.65)]',
        secondary:
          'bg-secondary text-primary-foreground hover:bg-secondary/90 hover:-translate-y-px',
        outline:
          'border border-border bg-transparent text-foreground hover:border-primary/30 hover:bg-surface-2/70 hover:-translate-y-px',
        ghost: 'bg-transparent text-foreground hover:bg-surface-2/70 hover:-translate-y-px',
        danger:
          'bg-danger text-white shadow-[0_8px_20px_-8px_rgb(var(--danger)/0.6)] hover:bg-danger/90 hover:-translate-y-px',
        success:
          'bg-success text-white shadow-[0_8px_20px_-8px_rgb(var(--success)/0.6)] hover:bg-success/90 hover:-translate-y-px',
      },
      size: {
        sm: 'h-8 px-3 text-xs',
        md: 'h-10 px-4',
        lg: 'h-11 px-6 text-base',
        icon: 'h-10 w-10',
      },
    },
    defaultVariants: {
      variant: 'primary',
      size: 'md',
    },
  },
);

export interface ButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, ...props }, ref) => (
    <button
      ref={ref}
      className={cn(buttonVariants({ variant, size }), className)}
      {...props}
    />
  ),
);
Button.displayName = 'Button';

export { buttonVariants };
