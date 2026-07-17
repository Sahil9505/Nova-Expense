import type { ReactNode } from 'react';
import { LineChart, Lock, ShieldCheck, Sparkles } from 'lucide-react';

interface AuthLayoutProps {
  title: string;
  subtitle?: string;
  children: ReactNode;
}

const features = [
  { icon: LineChart, text: 'Clear dashboards that make sense of your spending.' },
  { icon: ShieldCheck, text: 'Your data is private and encrypted by design.' },
  { icon: Sparkles, text: 'Insights that help you plan, not just track.' },
];

/**
 * Premium split-screen shell for authentication pages: a branded marketing panel
 * on large screens and a focused form column everywhere. The brand panel collapses
 * to a compact header on mobile so the form stays front and center.
 */
export function AuthLayout({ title, subtitle, children }: AuthLayoutProps) {
  return (
    <div className="flex min-h-screen">
      <section
        className="glass relative hidden w-1/2 flex-col justify-between overflow-hidden border-r border-border p-10 xl:p-12 lg:flex"
        aria-hidden="true"
      >
        <div className="flex items-center gap-2">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-gradient-to-br from-primary to-accent text-primary-foreground">
            <span className="text-lg font-extrabold">N</span>
          </div>
          <span className="text-lg font-bold tracking-tight">Nova</span>
        </div>

        <div className="max-w-md">
          <span className="inline-flex items-center gap-1.5 rounded-full bg-primary/15 px-3 py-1 text-xs font-medium text-primary">
            <Sparkles className="h-3.5 w-3.5" />
            Personal finance, refined
          </span>
          <h1 className="mt-5 text-3xl font-bold leading-tight tracking-tight xl:text-4xl">
            Take control of every dollar.
          </h1>
          <p className="mt-3 text-sm leading-relaxed text-muted-foreground">
            Nova brings your accounts, budgets, and insights into one calm, confident
            workspace built for how you actually manage money.
          </p>
          <ul className="mt-8 space-y-4">
            {features.map((feature) => (
              <li key={feature.text} className="flex items-start gap-3">
                <span className="mt-0.5 flex h-8 w-8 items-center justify-center rounded-lg bg-surface-2 text-primary">
                  <feature.icon className="h-4 w-4" />
                </span>
                <span className="text-sm text-muted-foreground">{feature.text}</span>
              </li>
            ))}
          </ul>
        </div>

        <div className="flex items-center gap-2 text-xs text-muted-foreground">
          <Lock className="h-3.5 w-3.5" />
          Bank-grade security. Your password is hashed with BCrypt and all traffic is encrypted in transit over HTTPS.
        </div>
      </section>

      <section className="flex w-full flex-col items-center justify-center px-4 py-10 sm:px-6 lg:w-1/2">
        <div className="w-full max-w-sm">
          <div className="mb-8 flex items-center gap-2 lg:hidden">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-gradient-to-br from-primary to-accent text-primary-foreground">
              <span className="text-lg font-extrabold">N</span>
            </div>
            <span className="text-lg font-bold tracking-tight">Nova</span>
          </div>

          <h2 className="text-2xl font-bold tracking-tight">{title}</h2>
          {subtitle ? <p className="mt-1 text-sm text-muted-foreground">{subtitle}</p> : null}

          <div className="mt-6">{children}</div>

          <p className="mt-8 text-center text-xs text-muted-foreground">
            By continuing you agree to Nova's Terms and Privacy Policy.
          </p>
        </div>
      </section>
    </div>
  );
}
